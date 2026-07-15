// Package declaration — utility class for SHA-256 password hashing with per-user salt
package com.items.util;

// MessageDigest provides the SHA-256 cryptographic hash function
import java.security.MessageDigest;
// Thrown if the requested algorithm is not available (should never happen for SHA-256)
import java.security.NoSuchAlgorithmException;
// SecureRandom generates cryptographically strong random bytes for the salt
import java.security.SecureRandom;
// Base64 encodes/decodes binary salt and hash values into URL-safe strings
import java.util.Base64;

/**
 * Password hashing utility using SHA-256 with a per-user random salt.
 *
 * Why salt is important:
 *   If two users have the same password, without salt their hashes would be identical.
 *   Salt ensures every hash is unique, even for identical passwords.
 *   Salt also prevents rainbow-table attacks.
 *
 * Storage strategy:
 *   - salt (base64-encoded, 16 random bytes) is stored in its own column
 *   - password_hash (base64-encoded SHA-256 digest) is stored in its own column
 *   - The original password is NEVER persisted — only the hash
 *
 * Verification flow:
 *   user enters password → hash(password + stored_salt) → compare with stored_hash
 *
 * NOTE: SHA-256 is used here for simplicity. In production, consider bcrypt or argon2.
 */
public class PasswordUtil {

    /** Private constructor — utility class should not be instantiated. */
    private PasswordUtil() {}

    /**
     * Generates a cryptographically random salt using SecureRandom.
     * The salt is 16 bytes (128 bits) long, which is sufficient for modern security.
     *
     * @return a base64-encoded string of 16 random bytes (about 24 characters)
     */
    public static String generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Hashes a plain-text password with a given salt using SHA-256.
     *
     * @param password   the plain-text password to hash
     * @param saltBase64 the base64-encoded salt to mix into the hash
     * @return a base64-encoded SHA-256 hash string (about 44 characters)
     */
    public static String hash(String password, String saltBase64) {
        byte[] salt = Base64.getDecoder().decode(saltBase64);
        byte[] hash = hashWithSalt(password, salt);
        return Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Verifies a plain-text password against a stored hash and salt.
     * Recomputes the hash with the stored salt and compares it to the stored hash.
     *
     * @param password   the plain-text password entered by the user
     * @param storedHash the base64-encoded hash from the database
     * @param saltBase64 the base64-encoded salt from the database
     * @return true if the computed hash matches the stored hash
     */
    public static boolean verify(String password, String storedHash, String saltBase64) {
        // Recompute the hash with the same salt and compare
        String computedHash = hash(password, saltBase64);
        return computedHash.equals(storedHash);
    }

    /**
     * Core hashing logic: feeds the salt + password bytes through SHA-256.
     *
     * The algorithm:
     *   1. Get a SHA-256 MessageDigest instance
     *   2. Feed the salt bytes first (this mixes the salt into the hash)
     *   3. Feed the password bytes
     *   4. digest() computes the final 32-byte (256-bit) hash
     *
     * @param password the plain-text password bytes
     * @param salt     the decoded salt bytes
     * @return the 32-byte SHA-256 digest
     */
    private static byte[] hashWithSalt(String password, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);                                    // Mix in the salt first
            return md.digest(password.getBytes());              // Add password and compute
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to be available on all Java platforms
            // This exception should never happen in practice
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
