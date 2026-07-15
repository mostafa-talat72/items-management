// Package declaration — this servlet handles ALL user-related HTTP requests
package com.items.controller;

// Standard Java I/O and web packages
import java.io.IOException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;

// JNDI resource injection for the database connection pool
import javax.annotation.Resource;
// Servlet API classes
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
// DataSource from the connection pool (Tomcat JNDI)
import javax.sql.DataSource;

// Domain and service classes
import com.items.model.User;
import com.items.service.UserService;
import com.items.service.impl.UserServiceImpl;
import com.items.util.PasswordUtil;
import com.items.util.UserValidator;

/**
 * Servlet handling ALL user-related operations:
 *
 *   Login/Logout
 *   Registration (signup) with OTP email verification
 *   OTP verification and resend
 *   Profile management (update info, change password)
 *   Account deletion
 *   "Remember me" cookie-based auto-login
 *
 * URL pattern: /userController?action=[actionName]
 *
 * This servlet uses the Post/Redirect/Get (PRG) pattern:
 *   - POST requests are routed through doGet() after a redirect
 *   - This prevents duplicate form submissions on page refresh
 *
 * Session management:
 *   - "IsLoggedIn" boolean attribute indicates authentication status
 *   - "user" attribute holds the full User object
 *   - Cookies are used for the "remember me" feature (rememberUserId)
 */
@WebServlet("/userController")
public class UserController extends HttpServlet {

    // Database connection pool injected by Tomcat (configured in META-INF/context.xml)
    @Resource(name = "jdbc/items/connection")
    private DataSource dataSource;

    // The user service handles all business logic and database operations
    private UserService userService;

    // Default redirect target after login/logout/etc.
    private static final String HOME = "/login.jsp";

    /**
     * Initializes the servlet — called once when the servlet is first loaded.
     * Creates the UserServiceImpl with the injected DataSource.
     */
    @Override
    public void init() throws ServletException {
        userService = new UserServiceImpl(dataSource);
    }

    /**
     * Routes ALL requests based on the "action" parameter.
     * Handles session checking and "remember me" cookie auto-login.
     * GET and POST are both handled here (PRG pattern).
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Read the action parameter — default to "login" if not provided
        String action = request.getParameter("action");
        if(Objects.isNull(action)) action = "login";

        // For the "selectUser" action, we DON'T check the session because this action
        // is used by the "remember me" cookie auto-login flow where no session exists yet.
        if(!"selectUser".equals(action)) {
            HttpSession session = request.getSession(false);
            // If user is not logged in, check for a "remember me" cookie
            if(session == null || session.getAttribute("user") == null) {
                Cookie[] cookies = request.getCookies();
                if(cookies != null) {
                    for(Cookie cookie : cookies) {
                        if("rememberUserId".equals(cookie.getName())) {
                            // Save the current URL so we can redirect back after auto-login
                            String currentUri = request.getRequestURI();
                            String qs = request.getQueryString();
                            String fullPath = currentUri + (qs != null ? "?" + qs : "");
                            String encodedPath = URLEncoder.encode(fullPath, "UTF-8");
                            // Auto-login: look up user by cookie ID, then redirect back
                            response.sendRedirect(request.getContextPath() + "/userController?action=selectUser&id=" + cookie.getValue() + "&redirectPath=" + encodedPath);
                            return;
                        }
                    }
                }
            }
        }

        // Route to the appropriate handler method based on the action parameter
        switch(action) {
            case "signup":             signup(request, response);             break;
            case "verifyOtp":          verifyOtp(request, response);          break;
            case "resendOtp":          resendOtp(request, response);          break;
            case "selectUser":         selectUser(request, response);         break;
            case "updateUserInfo":     updateUserInfo(request, response);     break;
            case "updateUserPassword": updateUserPassword(request, response); break;
            case "deleteUser":         deleteUser(request, response);         break;
            case "logout":             logout(request, response);             break;
            default:                   login(request, response);
        }
    }

    /**
     * POST requests are handled identically to GET (PRG pattern).
     * The actual form processing happens in the same methods.
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    /**
     * Handles user login.
     *
     * Accepts a username OR email ("loginId") and a password.
     * On success:
     *   - Creates an HTTP session
     *   - Stores IsLoggedIn and user attributes
     *   - Sets a "rememberUserId" cookie (1-hour expiry)
     *   - Redirects to the items dashboard
     * On failure:
     *   - Shows an error message on the login page
     */
    private void login(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String userLoginId = request.getParameter("loginId");
        String userPassword = request.getParameter("password");

        // Create a User object with the entered credentials for the service call
        User user = new User(userLoginId, userPassword);
        // Attempt login — returns null if credentials are invalid or email not verified
        user = userService.login(user);

        if(Objects.nonNull(user)) {
            // Login successful — create session and set attributes
            HttpSession session = request.getSession();
            session.setAttribute("IsLoggedIn", true);
            session.setAttribute("user", user);

            // Set a "remember me" cookie so the user doesn't have to log in again
            Cookie cookie = new Cookie("rememberUserId", Long.toString(user.getId()));
            cookie.setMaxAge(60 * 60);  // 1 hour expiry
            cookie.setPath("/");         // Available across the entire application
            cookie.setHttpOnly(true);    // Not accessible via JavaScript (security)
            response.addCookie(cookie);

            // Redirect to the items dashboard
            response.sendRedirect("itemController?action=showItems");
        } else {
            // Login failed — show error on the login page
            request.setAttribute("message", "Invalid login credentials or email not verified.");
            request.getRequestDispatcher("login.jsp").forward(request, response);
        }
    }

    /**
     * Handles new user registration.
     *
     * Workflow:
     *   1. Read form fields (fullName, username, email, password, confirmPassword)
     *   2. Validate fields — if errors exist, re-display the form with field-level errors
     *   3. Hash the password (SHA-256 + random salt)
     *   4. Call userService.signup() which inserts the user and sends an OTP email
     *   5. On success, redirect to the OTP verification page
     *   6. On SQL error (unique constraint, check violation), merge with validator errors
     *      and re-display the form
     */
    private void signup(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        // Read form fields from the request
        String fullName = request.getParameter("fullName");
        String username = request.getParameter("username");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");

        // Phase 1: Client-side validation (format, length, required fields)
        Map<String, String> errors = UserValidator.validateForSignup(fullName, username, email, password, confirmPassword);

        if(!errors.isEmpty()) {
            // Validation failed — re-display the form with errors and submitted values
            request.setAttribute("errors", errors);
            request.setAttribute("submitted", new User(fullName, username, email, null, null));
            request.getRequestDispatcher("/register.jsp").forward(request, response);
            return;
        }

        // Hash the password — generate salt, then SHA-256 hash
        String salt = PasswordUtil.generateSalt();
        String passwordHash = PasswordUtil.hash(password, salt);

        // Create the user object for the service layer
        User user = new User(fullName, username, email, passwordHash, salt);
        try {
            if(userService.signup(user)) {
                // Registration successful — redirect to OTP verification page
                response.sendRedirect(request.getContextPath() + "/verifyOtp.jsp?email=" + URLEncoder.encode(email, "UTF-8"));
            }
        } catch(RuntimeException e) {
            // Phase 2: Database error — parse SQL constraint violations
            handleSqlError(e, errors);
            if(!errors.isEmpty()) {
                // Known constraint — re-display form with error messages
                request.setAttribute("errors", errors);
                request.setAttribute("submitted", new User(fullName, username, email, null, null));
                request.getRequestDispatcher("/register.jsp").forward(request, response);
                return;
            }
            // Unknown/unexpected error — let the ErrorFilter handle it
            throw new ServletException("Failed to sign up: " + username, e);
        }
    }

    /**
     * Handles OTP verification form submission.
     * Validates the 6-digit code entered by the user.
     */
    private void verifyOtp(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String email = request.getParameter("email");
        String otpCode = request.getParameter("otpCode");

        // Basic validation — ensure the user entered something
        if(otpCode == null || otpCode.trim().isEmpty()) {
            request.setAttribute("error", "Please enter the OTP code.");
            request.setAttribute("email", email);
            request.getRequestDispatcher("/verifyOtp.jsp").forward(request, response);
            return;
        }

        // Call the service to verify the OTP
        if(userService.verifyOtp(email, otpCode.trim())) {
            // Success — redirect to login with a success message
            response.sendRedirect(request.getContextPath() + HOME + "?successMessage=Email verified successfully! Please log in.");
        } else {
            // Invalid or expired OTP — show error
            request.setAttribute("error", "Invalid or expired OTP code. Please try again.");
            request.setAttribute("email", email);
            request.getRequestDispatcher("/verifyOtp.jsp").forward(request, response);
        }
    }

    /**
     * Handles OTP resend request.
     * Generates a new OTP, stores it in the DB, and sends it via email.
     */
    private void resendOtp(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String email = request.getParameter("email");
        userService.resendOtp(email);
        response.sendRedirect(request.getContextPath() + "/verifyOtp.jsp?email=" + URLEncoder.encode(email, "UTF-8") + "&resent=true");
    }

    /**
     * Auto-login via "remember me" cookie.
     * Looks up the user by ID (stored in the cookie) and creates a session.
     * Then redirects to the originally requested page (stored in redirectPath).
     */
    private void selectUser(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            long id = Long.parseLong(request.getParameter("id"));
            User user = userService.selectUserById(id);
            if(Objects.nonNull(user)) {
                // User found — create session
                HttpSession session = request.getSession();
                session.setAttribute("IsLoggedIn", true);
                session.setAttribute("user", user);

                // Redirect to the original page (or dashboard if no redirectPath)
                String redirectPath = request.getParameter("redirectPath");
                if(redirectPath == null || redirectPath.isEmpty()) {
                    redirectPath = request.getContextPath() + "/itemController?action=showItems";
                }
                response.sendRedirect(redirectPath);
            } else {
                // User not found — send to login
                response.sendRedirect(request.getContextPath() + "/login.jsp");
            }
        } catch(NumberFormatException e) {
            // Invalid cookie value — send to login
            response.sendRedirect(request.getContextPath() + "/login.jsp");
        }
    }

    /**
     * Updates the user's profile information (full name and email).
     * Validates before updating, handles SQL constraint violations.
     */
    private void updateUserInfo(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        long id = Long.parseLong(request.getParameter("id"));
        String fullName = request.getParameter("fullName");
        String email = request.getParameter("email");

        // Validate the form fields
        Map<String, String> errors = UserValidator.validateForUpdate(fullName, email);

        if(!errors.isEmpty()) {
            request.setAttribute("errors", errors);
            request.getRequestDispatcher("/profile.jsp").forward(request, response);
            return;
        }

        // Build the User object with updated values
        User user = new User();
        user.setId(id);
        user.setFullName(fullName);
        user.setEmail(email);
        try {
            if(userService.updateUserInfo(user)) {
                // Success — re-load the user and redirect to profile page with success message
                String redirectPath = request.getContextPath() + "/profile.jsp?success=Profile+updated+successfully.";
                String encodedPath = URLEncoder.encode(redirectPath, "UTF-8");
                response.sendRedirect(request.getContextPath() + "/userController?action=selectUser&id=" + id + "&redirectPath=" + encodedPath);
            } else {
                response.sendRedirect(request.getContextPath() + "/profile.jsp");
            }
        } catch(RuntimeException e) {
            // Handle SQL constraint violations (unique email, etc.)
            handleSqlError(e, errors);
            if(!errors.isEmpty()) {
                request.setAttribute("errors", errors);
                request.getRequestDispatcher("/profile.jsp").forward(request, response);
                return;
            }
            throw new ServletException("Failed to update user: " + id, e);
        }
    }

    /**
     * Changes the user's password.
     * Validates the current password, hashes the new one, and updates the DB.
     */
    private void updateUserPassword(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        long id = Long.parseLong(request.getParameter("id"));
        String userName = request.getParameter("username");
        String currentPassword = request.getParameter("currentPassword");
        String newPassword = request.getParameter("newPassword");
        String confirmNewPassword = request.getParameter("confirmNewPassword");

        // Validate the form fields
        Map<String, String> errors = UserValidator.validatePasswordChange(currentPassword, newPassword, confirmNewPassword);

        if(!errors.isEmpty()) {
            request.setAttribute("errors", errors);
            request.getRequestDispatcher("/profile.jsp").forward(request, response);
            return;
        }

        // Verify the current password by attempting a login
        User existingUser = userService.login(new User(userName, currentPassword));
        if(existingUser == null) {
            errors.put("currentPassword", "Current password is incorrect.");
            request.setAttribute("errors", errors);
            request.getRequestDispatcher("/profile.jsp").forward(request, response);
            return;
        }

        // Hash the new password with a fresh salt
        String salt = PasswordUtil.generateSalt();
        existingUser.setSalt(salt);
        existingUser.setPasswordHash(PasswordUtil.hash(newPassword, salt));

        try {
            if(userService.updateUserPassword(existingUser)) {
                // Success — re-load the user and redirect with success message
                String redirectPath = request.getContextPath() + "/profile.jsp?success=Password+updated+successfully.";
                String encodedPath = URLEncoder.encode(redirectPath, "UTF-8");
                response.sendRedirect(request.getContextPath() + "/userController?action=selectUser&id=" + id + "&redirectPath=" + encodedPath);
            } else {
                response.sendRedirect(request.getContextPath() + "/profile.jsp");
            }
        } catch(RuntimeException e) {
            throw new ServletException("Failed to update password for user: " + id, e);
        }
    }

    /**
     * Deletes the user's account (hard delete).
     * After deletion, logs the user out and redirects to the login page.
     */
    private void deleteUser(HttpServletRequest request, HttpServletResponse response) throws IOException {
        long id = Long.parseLong(request.getParameter("id"));
        try {
            if(userService.deleteUser(id)) {
                // Account deleted — log out the user
                logout(request, response);
            } else {
                response.sendRedirect(request.getContextPath() + "/profile.jsp");
            }
        } catch(Exception e) {
            throw new RuntimeException("Error deleting user: " + e.getMessage(), e);
        }
    }

    /**
     * Logs out the user by:
     *   1. Invalidating the HTTP session
     *   2. Clearing the "rememberUserId" cookie (setting maxAge to 0)
     *   3. Redirecting to the login page
     */
    private void logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if(session != null) {
            session.invalidate();
        }

        // Clear the remember-me cookie by setting its max age to 0
        Cookie cookie = new Cookie("rememberUserId", "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);

        response.sendRedirect(request.getContextPath() + "/login.jsp");
    }

    /**
     * Walks the exception cause chain to find a SQLException and maps its
     * message to field-level errors using UserValidator.parseSqlError().
     *
     * @param e      the RuntimeException that wraps the SQLException
     * @param errors the error map to add field-level messages to
     */
    private void handleSqlError(RuntimeException e, Map<String, String> errors) {
        String msg = extractSqlMessage(e);
        if(msg == null) return;
        errors.putAll(UserValidator.parseSqlError(msg));
    }

    /**
     * Walks the nested exception cause chain to find the first SQLException message.
     * This is needed because the service wraps SQLException in RuntimeException,
     * and there may be multiple layers of wrapping.
     *
     * @param e the RuntimeException to analyze
     * @return the SQL error message string, or null if no SQLException is in the chain
     */
    private String extractSqlMessage(RuntimeException e) {
        Throwable cause = e;
        while(cause != null) {
            if(cause instanceof SQLException) {
                return cause.getMessage();
            }
            cause = cause.getCause();
        }
        return null;
    }
}
