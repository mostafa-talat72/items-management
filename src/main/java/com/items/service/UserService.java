// Package declaration — service-layer interface for all user-related business logic
package com.items.service;

// User is the domain model that represents a registered account
import com.items.model.User;

/**
 * Service-layer interface that defines ALL operations for user accounts.
 *
 * This covers the full user lifecycle:
 *   1. Registration (signup) with OTP email verification
 *   2. Login with credential validation and OTP status check
 *   3. OTP verification and resend
 *   4. Profile updates (info + password change)
 *   5. Account deletion (soft delete via is_active flag, or hard delete)
 *   6. User lookup by ID
 *
 * The implementation (UserServiceImpl) handles all JDBC interactions
 * and wraps SQLException in RuntimeException for consistent error handling.
 */
public interface UserService {

    /**
     * Registers a NEW user account.
     * Inserts the user record, generates an OTP, stores it in the DB,
     * and sends the OTP via email. If the OTP columns don't exist yet
     * (backward compatibility), the user is treated as verified immediately.
     *
     * @param user the User object with registration data (name, username, email, password)
     * @return true if registration was successful
     */
    boolean signup(User user);

    /**
     * Authenticates a user by username/email and password.
     * Also checks whether the user's email is verified (is_verified flag).
     * Unverified users cannot log in — they must complete OTP verification first.
     *
     * @param user a User with the entered loginId (username or email) and plain-text password
     * @return the full User object if credentials match AND email is verified, or null otherwise
     */
    User login(User user);

    /**
     * Verifies the 6-digit OTP code entered by the user.
     * Checks that:
     *   1. The user exists and is active but NOT yet verified
     *   2. The OTP code matches the stored value
     *   3. The OTP has not expired (5-minute window)
     * On success, sets is_verified = 1 and clears the OTP fields.
     *
     * @param email   the user's email address
     * @param otpCode the 6-digit code the user entered
     * @return true if the OTP is valid and the user is now verified
     */
    boolean verifyOtp(String email, String otpCode);

    /**
     * Generates a NEW OTP code, updates it in the database, and re-sends it via email.
     * The old OTP is invalidated when this is called.
     * Only works for unverified, active users.
     *
     * @param email the user's email address to resend the OTP to
     */
    void resendOtp(String email);

    /**
     * Updates the user's profile information (full name and email).
     * Does NOT change the password — that is handled by updateUserPassword().
     *
     * @param user a User with the updated name and email (ID must be set)
     * @return true if at least one row was updated
     */
    boolean updateUserInfo(User user);

    /**
     * Changes the user's password by updating the password_hash and salt.
     * The new password must already be hashed before calling this method.
     *
     * @param user a User with the new password hash and salt (ID must be set)
     * @return true if the password was updated
     */
    boolean updateUserPassword(User user);

    /**
     * HARD-deletes a user account from the database.
     * This permanently removes the user record (not a soft delete).
     *
     * @param id the user's database ID
     * @return true if a user was deleted
     */
    boolean deleteUser(long id);

    /**
     * Retrieves a user by their database ID.
     * Used for "remember me" auto-login — the cookie stores the user's ID,
     * and this method looks up the full user record.
     *
     * @param id the user's database ID
     * @return the full User object, or null if not found
     */
    User selectUserById(long id);
}
