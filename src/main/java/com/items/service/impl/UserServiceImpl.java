// Package declaration — JDBC-based implementation of the UserService interface
package com.items.service.impl;

// Core JDBC classes for database interaction
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
// LocalDateTime is used to calculate OTP expiry (current time + 5 minutes)
import java.time.LocalDateTime;

// DataSource is the connection pool from Tomcat's JNDI
import javax.sql.DataSource;

// User is the domain model we're managing
import com.items.model.User;
// UserService is the interface we're implementing
import com.items.service.UserService;
// EmailUtil generates and sends OTP codes via SMTP (or prints to console)
import com.items.util.EmailUtil;
// PasswordUtil provides SHA-256 + salt hashing and verification
import com.items.util.PasswordUtil;

/**
 * JDBC implementation of UserService.
 *
 * This handles all user-related database operations:
 *   - Registration with OTP email verification
 *   - Login with password verification and OTP status check
 *   - OTP verification and resend
 *   - Profile updates (name, email, password)
 *   - Hard-delete and lookup by ID
 *
 * Workflow for new registration:
 *   1. INSERT the user with basic fields (id, name, username, email, password_hash, salt)
 *   2. Generate a 6-digit OTP and UPDATE the OTP columns
 *   3. Send the OTP via EmailUtil (gracefully handles missing columns or SMTP failure)
 *   4. Redirect the user to the OTP verification page
 *   5. On successful OTP entry, set is_verified = 1 and clear OTP fields
 *
 * Key design decisions:
 *   - OTP columns were added via ALTER TABLE — if they don't exist, the user
 *     is treated as "already verified" (backward compatibility)
 *   - The login method blocks unverified accounts (is_verified = 0)
 *   - Password is NEVER stored in plain text — only the SHA-256 hash + salt
 */
public class UserServiceImpl implements UserService {

    // The connection pool from Tomcat's JNDI context.xml
    private DataSource dataSource;

    /**
     * Constructor receiving a DataSource from the controller.
     *
     * @param dataSource the Tomcat connection pool
     */
    public UserServiceImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Borrows a single connection from the connection pool.
     *
     * @return a pooled java.sql.Connection
     * @throws SQLException if the pool is exhausted or the database is unreachable
     */
    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Maps the CURRENT ROW of a ResultSet into a complete User object.
     * OTP fields are read with try-catch because they may not exist in older
     * database schemas — if a column is missing, the catch block provides a safe default.
     *
     * @param resultSet the ResultSet positioned at the current row
     * @return a fully populated User object
     * @throws SQLException if a REQUIRED column access fails
     */
    private User extractUser(ResultSet resultSet) throws SQLException {
        User user = new User();
        // Standard columns that always exist
        user.setId(resultSet.getLong("id"));
        user.setFullName(resultSet.getString("full_name"));
        user.setUsername(resultSet.getString("username"));
        user.setEmail(resultSet.getString("email"));
        user.setPasswordHash(resultSet.getString("password_hash"));
        user.setSalt(resultSet.getString("salt"));
        user.setCreatedAt(resultSet.getTimestamp("created_at"));
        user.setUpdatedAt(resultSet.getTimestamp("updated_at"));
        user.setIsActive(resultSet.getInt("is_active"));

        // OTP columns — wrap in try-catch for backward compatibility.
        // If the column doesn't exist (ALTER TABLE not run), the user is treated as verified.
        try { user.setIsVerified(resultSet.getInt("is_verified")); } catch(SQLException e) { user.setIsVerified(1); }
        try { user.setOtpCode(resultSet.getString("otp_code")); } catch(SQLException e) { /* column may not exist — ignore */ }
        try { user.setOtpExpiresAt(resultSet.getTimestamp("otp_expires_at")); } catch(SQLException e) { /* column may not exist — ignore */ }
        return user;
    }

    /**
     * Registers a new user account.
     *
     * Step-by-step:
     *   1. INSERT into USERS with basic fields only (no OTP columns — backward compatible)
     *   2. Generate a random 6-digit OTP, set it to expire in 5 minutes
     *   3. Try to UPDATE the OTP columns (if they exist)
     *   4. If OTP update succeeded, send the code via email
     *   5. If OTP columns don't exist (SQLException), silently skip verification
     *
     * @param user the User with registration data (fullName, username, email, passwordHash, salt)
     * @return true on successful registration
     */
    @Override
    public boolean signup(User user) {
        // Insert without OTP columns first to maintain backward compatibility
        String query = "INSERT INTO USERS (full_name, username, email, password_hash, salt) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            // Bind the basic user fields
            preparedStatement.setString(1, user.getFullName());
            preparedStatement.setString(2, user.getUsername());
            preparedStatement.setString(3, user.getEmail());
            preparedStatement.setString(4, user.getPasswordHash());
            preparedStatement.setString(5, user.getSalt());
            preparedStatement.execute();

            // Generate OTP — a random 6-digit number that expires in 5 minutes
            String otpCode = EmailUtil.generateOtp();
            Timestamp expiresAt = Timestamp.valueOf(LocalDateTime.now().plusMinutes(5));

            // Try to store the OTP in the DB and send it via email
            // If the OTP columns don't exist, this silently fails (backward compatibility)
            try {
                // UPDATE the newly created user with OTP data
                String otpQuery = "UPDATE USERS SET otp_code = ?, otp_expires_at = ?, is_verified = 0 WHERE email = ?";
                try (PreparedStatement otpStmt = connection.prepareStatement(otpQuery)) {
                    otpStmt.setString(1, otpCode);
                    otpStmt.setTimestamp(2, expiresAt);
                    otpStmt.setString(3, user.getEmail());
                    otpStmt.executeUpdate();
                }
                // Send the OTP via email (also prints to console for debugging)
                EmailUtil.sendOtpEmail(user.getEmail(), otpCode, user.getFullName());
            } catch(SQLException e) {
                // OTP columns don't exist — user is treated as verified
                // The login method will see is_verified = 1 (from extractUser default)
                System.out.println("OTP columns not available — skipping email verification.");
            }
            return true;

        } catch (SQLException e) {
            // Wrap in RuntimeException so the controller catches it uniformly
            throw new RuntimeException(e);
        }
    }

    /**
     * Authenticates a user by username/email and password.
     *
     * The user can log in with either their username OR their email address.
     * After verifying the password hash, we also check:
     *   - is_active = 1 (account not soft-deleted)
     *   - is_verified != 0 (email verification completed)
     *
     * If the OTP column doesn't exist (backward compatibility), extractUser
     * defaults is_verified to 1, so the check passes.
     *
     * @param user a User with username (or email) and plain-text password
     * @return the authenticated User if valid, or null if credentials don't match
     *         or the account is not yet verified
     */
    @Override
    public User login(User user) {
        // Query accepts username OR email in the same field (loginId)
        // Only active accounts can log in
        String query = "SELECT * FROM USERS WHERE (USERNAME = ? OR EMAIL = ?) AND is_active = 1";
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            // Bind the login ID to both the USERNAME and EMAIL placeholders
            preparedStatement.setString(1, user.getUsername());
            preparedStatement.setString(2, user.getUsername());

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if(resultSet.next()) {
                    // Retrieve the stored password hash and salt for verification
                    String storedHash = resultSet.getString("password_hash");
                    String storedSalt = resultSet.getString("salt");

                    // Verify the entered password against the stored hash + salt
                    if(PasswordUtil.verify(user.getPasswordHash(), storedHash, storedSalt)) {
                        // Password matches — build the full user object
                        User found = extractUser(resultSet);

                        // Block login if the user hasn't verified their email
                        // extractUser defaults to 1 if the column doesn't exist (backward compat)
                        if(found.getIsVerified() == 0) return null;

                        return found;
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        // No matching user found or password didn't match
        return null;
    }

    /**
     * Verifies the user's email using the 6-digit OTP they received.
     *
     * Validation checks (all must pass):
     *   1. User exists with the given email
     *   2. User is active (not deleted)
     *   3. User is NOT yet verified (is_verified = 0)
     *   4. Stored OTP matches the entered code
     *   5. Stored OTP expiry is still in the future
     *
     * On success: sets is_verified = 1, clears OTP fields so the code cannot be reused.
     *
     * @param email   the user's email address
     * @param otpCode the 6-digit code the user entered
     * @return true if verification succeeded
     */
    @Override
    public boolean verifyOtp(String email, String otpCode) {
        // Find the user — must be active, unverified, and match the email
        String query = "SELECT * FROM USERS WHERE EMAIL = ? AND is_active = 1 AND is_verified = 0";
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setString(1, email);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if(resultSet.next()) {
                    // Get the stored OTP and its expiry
                    String storedOtp = resultSet.getString("otp_code");
                    Timestamp expiresAt = resultSet.getTimestamp("otp_expires_at");

                    // Validate the OTP
                    if(storedOtp == null || expiresAt == null) return false;  // No OTP issued
                    if(!storedOtp.equals(otpCode)) return false;               // Wrong code
                    if(expiresAt.before(new Timestamp(System.currentTimeMillis()))) return false; // Expired

                    // OTP is valid — mark user as verified and clear the OTP fields
                    String updateQuery = "UPDATE USERS SET is_verified = 1, otp_code = NULL, otp_expires_at = NULL, updated_at = CURRENT_TIMESTAMP WHERE EMAIL = ?";
                    try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
                        updateStmt.setString(1, email);
                        updateStmt.executeUpdate();
                    }
                    return true;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    /**
     * Generates a NEW OTP, updates it in the database, and sends it again.
     * Invalidates the previous OTP. The user must be active and unverified.
     *
     * @param email the user's email to resend the OTP to
     */
    @Override
    public void resendOtp(String email) {
        // Find the user — must be active and unverified
        String query = "SELECT * FROM USERS WHERE EMAIL = ? AND is_active = 1 AND is_verified = 0";
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setString(1, email);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if(resultSet.next()) {
                    // Generate a new OTP and new expiry (5 minutes from now)
                    String newOtp = EmailUtil.generateOtp();
                    Timestamp newExpires = Timestamp.valueOf(LocalDateTime.now().plusMinutes(5));
                    // Get the user's name for the email body
                    String fullName = resultSet.getString("full_name");

                    // Update the OTP in the database (replaces the old one)
                    String updateQuery = "UPDATE USERS SET otp_code = ?, otp_expires_at = ?, updated_at = CURRENT_TIMESTAMP WHERE EMAIL = ?";
                    try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
                        updateStmt.setString(1, newOtp);
                        updateStmt.setTimestamp(2, newExpires);
                        updateStmt.setString(3, email);
                        updateStmt.executeUpdate();
                    }

                    // Send the new OTP via email
                    EmailUtil.sendOtpEmail(email, newOtp, fullName);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Updates the user's profile information (full name and email).
     * Does NOT change the password — use updateUserPassword() for that.
     * The updated_at timestamp is set to the current time by the query.
     *
     * @param user a User with the new name and email (must have ID set)
     * @return true if at least one row was updated
     */
    @Override
    public boolean updateUserInfo(User user) {
        String query = "UPDATE USERS SET full_name = ?, email = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setString(1, user.getFullName());
            preparedStatement.setString(2, user.getEmail());
            preparedStatement.setLong(3, user.getId());

            // executeUpdate returns 1 if a row was affected, 0 if the ID wasn't found
            return preparedStatement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Changes the user's password by updating password_hash and salt.
     * The new password must already be hashed before calling — this method
     * does NOT hash the password. The controller handles hashing.
     *
     * @param user a User with the new password hash and salt (must have ID set)
     * @return true if the password was updated
     */
    @Override
    public boolean updateUserPassword(User user) {
        String query = "UPDATE USERS SET password_hash = ?, salt = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setString(1, user.getPasswordHash());
            preparedStatement.setString(2, user.getSalt());
            preparedStatement.setLong(3, user.getId());

            return preparedStatement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Hard-deletes a user account from the database by ID.
     * This is a permanent deletion — the record cannot be recovered.
     * For a soft delete, use the is_active flag instead.
     *
     * @param id the user's database ID to delete
     * @return true if a record was deleted
     */
    @Override
    public boolean deleteUser(long id) {
        String query = "DELETE FROM USERS WHERE id = ?";
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setLong(1, id);
            return preparedStatement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves a user by their database ID.
     * Used for the "remember me" feature — when a returning user has a valid
     * cookie, we look them up by ID and auto-login.
     *
     * @param id the user's database ID
     * @return the full User object, or null if not found
     */
    @Override
    public User selectUserById(long id) {
        String query = "SELECT * FROM USERS WHERE id = ?";
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setLong(1, id);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if(resultSet.next()) return extractUser(resultSet);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
