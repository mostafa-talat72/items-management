// Package declaration — validates User form fields and maps DB constraint errors to field-level messages
package com.items.util;

// HashMap and Map are used to return key-value error pairs (fieldName → errorMessage)
import java.util.HashMap;
import java.util.Map;

/**
 * Server-side validator for all User-related forms.
 *
 * Validates the following forms:
 *   1. Registration (signup) — fullName, username, email, password, confirmPassword
 *   2. Profile update — fullName, email
 *   3. Password change — currentPassword, newPassword, confirmNewPassword
 *
 * Validation rules match the database CHECK constraints:
 *   CHK_USERS_FULL_NAME_LENGTH          — 3 to 100 characters
 *   CHK_USERS_USERNAME_LENGTH           — 3 to 30 characters
 *   CHK_USERS_USERNAME_FORMAT           — must start with a letter, alphanumeric + . _ only
 *   CHK_USERS_EMAIL_LENGTH              — 5 to 255 characters
 *   CHK_USERS_EMAIL_FORMAT              — must match email regex pattern
 *   ORA-00001 (UQ_USERS_USERNAME/EAMIL) — unique values only
 *
 * Password check is done BEFORE hashing: the raw password must be >= 6 characters.
 * After that, PasswordUtil hashes it and stores the hash — the hash length
 * check happens at the DB level (CHK_USERS_PASSWORD_HASH_LENGTH).
 */
public class UserValidator {

    /** Private constructor — utility class. */
    private UserValidator() {}

    /**
     * Validates ALL registration fields.
     * Runs BEFORE the database INSERT is attempted.
     *
     * @param fullName        the user's full name
     * @param username        the desired username
     * @param email           the email address
     * @param password        the chosen password (plain text — will be hashed later)
     * @param confirmPassword the repeated password for confirmation
     * @return a map of field→error (empty map = no errors)
     */
    public static Map<String, String> validateForSignup(String fullName, String username, String email, String password, String confirmPassword) {
        Map<String, String> errors = new HashMap<>();
        checkFullName(fullName, errors);
        checkUsername(username, errors);
        checkEmail(email, errors);
        checkPassword(password, errors);
        checkConfirmPassword(password, confirmPassword, errors);
        return errors;
    }

    /**
     * Validates the profile update form (only fullName and email can be updated).
     *
     * @param fullName the new full name
     * @param email    the new email address
     * @return a map of field→error (empty map = no errors)
     */
    public static Map<String, String> validateForUpdate(String fullName, String email) {
        Map<String, String> errors = new HashMap<>();
        checkFullName(fullName, errors);
        checkEmail(email, errors);
        return errors;
    }

    /**
     * Validates the password change form.
     *
     * @param currentPassword   the user's current password (for identity verification)
     * @param newPassword       the desired new password
     * @param confirmNewPassword repeated new password for confirmation
     * @return a map of field→error (empty map = no errors)
     */
    public static Map<String, String> validatePasswordChange(String currentPassword, String newPassword, String confirmNewPassword) {
        Map<String, String> errors = new HashMap<>();
        // Current password must not be empty
        if(currentPassword == null || currentPassword.trim().isEmpty()) {
            errors.put("currentPassword", "Current password is required.");
        }
        // Validate new password (length >= 6)
        checkPassword(newPassword, errors);
        // Confirm passwords match
        checkConfirmPassword(newPassword, confirmNewPassword, errors);
        // Rename keys to match JSP field names ("newPassword" / "confirmNewPassword")
        if(errors.containsKey("password")) {
            errors.put("newPassword", errors.remove("password"));
        }
        if(errors.containsKey("confirmPassword")) {
            errors.put("confirmNewPassword", errors.remove("confirmPassword"));
        }
        return errors;
    }

    /**
     * Converts an Oracle SQL error message into a user-friendly field-level error.
     * Handles both UNIQUE and CHECK constraint violations for the USERS table.
     *
     * Known constraint names:
     *   UQ_USERS_USERNAME              — unique username violation
     *   UQ_USERS_EMAIL                 — unique email violation
     *   CHK_USERS_FULL_NAME_LENGTH     — full name length out of range
     *   CHK_USERS_USERNAME_LENGTH      — username length out of range
     *   CHK_USERS_USERNAME_FORMAT      — username format invalid
     *   CHK_USERS_EMAIL_LENGTH         — email length out of range
     *   CHK_USERS_EMAIL_FORMAT         — email format invalid
     *   ORA-01400                      — NOT NULL constraint violation
     *
     * @param sqlMessage the Oracle error message from the SQLException
     * @return a map mapping the field name to a user-friendly message
     */
    public static Map<String, String> parseSqlError(String sqlMessage) {
        Map<String, String> error = new HashMap<>();
        if(sqlMessage == null) return error;

        String m = sqlMessage.toUpperCase();

        // UNIQUE constraints (ORA-00001)
        if(m.contains("UQ_USERS_USERNAME")) {
            error.put("username", "This username is already taken.");
        } else if(m.contains("UQ_USERS_EMAIL")) {
            error.put("email", "This email is already registered.");
        }
        // CHECK constraints
        else if(m.contains("CHK_USERS_FULL_NAME_LENGTH")) {
            error.put("fullName", "Full name must be between 3 and 100 characters.");
        } else if(m.contains("CHK_USERS_USERNAME_LENGTH")) {
            error.put("username", "Username must be between 3 and 30 characters.");
        } else if(m.contains("CHK_USERS_USERNAME_FORMAT")) {
            error.put("username", "Username must start with a letter and contain only letters, numbers, dots, or underscores.");
        } else if(m.contains("CHK_USERS_EMAIL_LENGTH")) {
            error.put("email", "Email must be between 5 and 255 characters.");
        } else if(m.contains("CHK_USERS_EMAIL_FORMAT")) {
            error.put("email", "Please enter a valid email address.");
        }
        // NOT NULL violation (required field missing)
        else if(m.contains("ORA-01400")) {
            if(m.contains("FULL_NAME"))      error.put("fullName", "Full name is required.");
            else if(m.contains("USERNAME"))  error.put("username", "Username is required.");
            else if(m.contains("EMAIL"))     error.put("email", "Email is required.");
            else if(m.contains("PASSWORD_HASH")) error.put("password", "Password is required.");
        }

        return error;
    }

    /**
     * Validates the full name: required, 3–100 characters.
     *
     * @param fullName the raw full name string
     * @param errors   the error map to add to
     */
    private static void checkFullName(String fullName, Map<String, String> errors) {
        if(fullName == null || fullName.trim().isEmpty()) {
            errors.put("fullName", "Full name is required.");
            return;
        }
        String trimmed = fullName.trim();
        if(trimmed.length() < 3) errors.put("fullName", "Full name must be at least 3 characters.");
        else if(trimmed.length() > 100) errors.put("fullName", "Full name must not exceed 100 characters.");
    }

    /**
     * Validates the username: required, 3–30 characters, must start with a letter,
     * and contain only letters, numbers, periods, or underscores.
     *
     * @param username the raw username string
     * @param errors   the error map to add to
     */
    private static void checkUsername(String username, Map<String, String> errors) {
        if(username == null || username.trim().isEmpty()) {
            errors.put("username", "Username is required.");
            return;
        }
        String u = username.trim();
        if(u.length() < 3) errors.put("username", "Username must be at least 3 characters.");
        else if(u.length() > 30) errors.put("username", "Username must not exceed 30 characters.");
        // Regex: starts with a letter, then any number of letters, digits, dots, or underscores
        else if(!u.matches("^[A-Za-z][A-Za-z0-9_.]*$"))
            errors.put("username", "Username must start with a letter and contain only letters, numbers, dots, or underscores.");
    }

    /**
     * Validates the email: required, 5–255 characters, basic email format.
     *
     * @param email  the raw email string
     * @param errors the error map to add to
     */
    private static void checkEmail(String email, Map<String, String> errors) {
        if(email == null || email.trim().isEmpty()) {
            errors.put("email", "Email is required.");
            return;
        }
        String e = email.trim();
        if(e.length() < 5) errors.put("email", "Email must be at least 5 characters.");
        else if(e.length() > 255) errors.put("email", "Email must not exceed 255 characters.");
        // Basic email regex: local-part@domain.tld (must have at least 2-letter TLD)
        else if(!e.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
            errors.put("email", "Please enter a valid email address.");
    }

    /**
     * Validates the password: required, at least 6 characters (BEFORE hashing).
     *
     * @param password the raw password string (plain text)
     * @param errors   the error map to add to
     */
    public static void checkPassword(String password, Map<String, String> errors) {
        if(password == null || password.trim().isEmpty()) {
            errors.put("password", "Password is required.");
            return;
        }
        if(password.length() < 6) errors.put("password", "Password must be at least 6 characters.");
    }

    /**
     * Validates that confirmPassword matches password.
     *
     * @param password        the original password
     * @param confirmPassword the repeated password for confirmation
     * @param errors          the error map to add to
     */
    private static void checkConfirmPassword(String password, String confirmPassword, Map<String, String> errors) {
        if(confirmPassword == null || confirmPassword.trim().isEmpty()) {
            errors.put("confirmPassword", "Please confirm your password.");
            return;
        }
        if(!confirmPassword.equals(password))
            errors.put("confirmPassword", "Passwords do not match.");
    }
}
