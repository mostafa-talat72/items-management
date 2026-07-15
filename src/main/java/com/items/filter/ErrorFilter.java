// Package declaration — this filter catches ALL unhandled exceptions across the entire application
package com.items.filter;

// IOException is thrown when forwarding to error.jsp fails
import java.io.IOException;
// SQLException is detected in the exception cause chain and mapped to friendly messages
import java.sql.SQLException;

// Servlet filter interfaces
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
// WebFilter annotation registers this filter without web.xml
import javax.servlet.annotation.WebFilter;
// HttpServletRequest/Response provide HTTP-specific features (URI, sendError)
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Global error-handling filter that catches ALL exceptions thrown by any servlet, JSP,
 * or service method and forwards them to a styled error.jsp page.
 *
 * This is the last line of defense. The controllers handle KNOWN errors (validation failures,
 * constraint violations) and re-display the form with field-level messages. But if an
 * UNEXPECTED error slips through (NullPointerException, NumberFormatException, unknown SQL error),
 * this filter catches it and shows a user-friendly error page instead of a raw stack trace
 * or Tomcat's default error page.
 *
 * The filter also converts common exception types into plain-language messages:
 *   - NumberFormatException → "Invalid number format"
 *   - NullPointerException → "Required information is missing"
 *   - SQLException → Human-readable message based on Oracle error code
 *   - Other exceptions → The exception's own message (or a generic fallback)
 *
 * The filter excludes /error.jsp to prevent infinite redirect loops
 * (if error.jsp itself throws an exception, we don't want to loop).
 */
@WebFilter("/*")  // Intercept every request to the application
public class ErrorFilter implements Filter {

    /**
     * Main filter method — called for every request matching "/*".
     *
     * @param request  the incoming servlet request
     * @param response the outgoing servlet response
     * @param chain    the filter chain (next filter or the target servlet)
     * @throws IOException      if forwarding to error.jsp fails
     * @throws ServletException if forwarding fails
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // Skip the filter for error.jsp itself to prevent infinite redirect loops.
        // If an error happens while rendering error.jsp, Tomcat will handle it natively.
        String path = req.getRequestURI().substring(req.getContextPath().length());
        if(path.equals("/error.jsp")) {
            chain.doFilter(request, response);
            return;
        }

        try {
            // Pass the request down the filter chain to the servlet/JSP
            chain.doFilter(request, response);
        } catch (Exception e) {
            // Any exception that wasn't caught by the controller lands here.
            // Convert it to a user-friendly message.
            String message = resolveMessage(e);
            try {
                // Set error attributes for error.jsp to display
                req.setAttribute("javax.servlet.error.status_code", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                req.setAttribute("javax.servlet.error.message", message);
                // Forward to the styled error page
                req.getRequestDispatcher("/error.jsp").forward(req, res);
            } catch (Exception ignored) {
                // If even the forward fails, send a plain HTTP 500 error as a fallback
                res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message);
            }
        }
    }

    /**
     * Converts a caught exception into a user-friendly error message.
     * Checks specific exception types first, then walks the cause chain
     * looking for SQLException at any nesting level.
     *
     * @param e the exception that was thrown
     * @return a human-readable error message
     */
    private String resolveMessage(Exception e) {
        // Specific exception types — detected directly
        if(e instanceof NumberFormatException) {
            return "Invalid number format. Please enter valid numeric values.";
        }
        if(e instanceof NullPointerException) {
            return "Required information is missing. Please fill all fields.";
        }

        // Walk the full cause chain — SQLException could be nested deep inside
        // a RuntimeException wrapping another RuntimeException wrapping the SQLException
        Throwable current = e;
        do {
            if(current instanceof SQLException) {
                return resolveSqlMessage((SQLException) current);
            }
            current = current.getCause();
        } while(current != null);

        // Fall back to the exception's own message
        String msg = e.getMessage();
        if(msg != null && !msg.isEmpty()) {
            return msg;
        }
        // Absolute last resort
        return "An unexpected error occurred. Please try again.";
    }

    /**
     * Maps Oracle error codes to user-friendly messages.
     *
     * This filters catches errors that escape the controller (unexpected/unknown).
     * Known constraint violations are handled INSIDE the controller by
     * Validator.parseSqlError() for field-level error display.
     *
     * @param e the SQLException to analyze
     * @return a user-friendly message string
     */
    private String resolveSqlMessage(SQLException e) {
        String msg = e.getMessage();
        if(msg == null) return "A database error occurred. Please try again.";

        String upper = msg.toUpperCase();

        // ORA-00001: Unique constraint violation — item name already exists
        if(upper.contains("ORA-00001") || upper.contains("UQ_ITEMS_NAME")) {
            return "An item with this name already exists.";
        }

        // ORA-01400: NOT NULL constraint — a required field was empty
        if(upper.contains("ORA-01400")) {
            if(upper.contains("NAME")) return "Item name is required.";
            if(upper.contains("PRICE")) return "Item price is required.";
            if(upper.contains("TOTAL_NUMBER")) return "Item quantity is required.";
            return "Required field cannot be empty.";
        }

        // ORA-02290: CHECK constraint — value violates a business rule
        if(upper.contains("ORA-02290") || upper.contains("CHECK CONSTRAINT")) {
            if(upper.contains("CHK_ITEMS_PRICE")) return "Price must be greater than zero.";
            if(upper.contains("CHK_ITEMS_TOTAL_NUMBER")) return "Quantity must be greater than zero.";
            if(upper.contains("CHK_ITEMS_NAME_LENGTH")) return "Item name must be at least 1 character.";
            return "A validation constraint was violated. Check your input.";
        }

        // ORA-02292: Foreign key violation — child records exist
        if(upper.contains("ORA-02292")) {
            return "Cannot delete this item. It is linked to other records.";
        }

        // ORA-20000: Database connection issue
        if(upper.contains("ORA-20000") || upper.contains("CONNECT")) {
            return "Unable to connect to the database. Please try again later.";
        }

        return "A database error occurred. Please try again.";
    }

    @Override public void init(FilterConfig filterConfig) throws ServletException {}
    @Override public void destroy() {}
}
