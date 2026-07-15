// Package declaration — validates ItemDetails form fields and maps DB constraint errors
package com.items.util;

// HashMap and Map are used to return key-value error pairs (fieldName → errorMessage)
import java.util.HashMap;
import java.util.Map;

/**
 * Server-side validator for the ItemDetails form field (description).
 *
 * Validation follows the same two-phase approach as ItemValidator:
 *   1. validateEmptyAndFormat() — runs BEFORE the DB call
 *   2. parseSqlError() — runs AFTER a SQLException to map known constraints
 *
 * The description field must:
 *   - Not be empty (required)
 *   - Be at least 3 characters (CHECK constraint CHK_ITEM_DETAILS_DESCRIPTION_LENGTH)
 *
 * Known DB constraint names:
 *   FK_ITEM_DETAILS_ITEMS              — foreign key to ITEMS table
 *   CHK_ITEM_DETAILS_DESCRIPTION_LENGTH — description must be >= 3 characters
 *   ORA-01400                           — NOT NULL constraint
 */
public class ItemDetailsValidator {

    /** Private constructor — utility class. */
    private ItemDetailsValidator() {}

    /**
     * Phase 1: Checks that the description is not empty and meets minimum length.
     * Called BEFORE attempting the database INSERT/UPDATE.
     *
     * @param description the raw description text from the form
     * @return a map of field→error (empty map = no errors)
     */
    public static Map<String, String> validateEmptyAndFormat(String description) {
        Map<String, String> errors = new HashMap<>();
        checkDescription(description, errors);
        return errors;
    }

    /**
     * Phase 2: Converts an Oracle SQL error message into a user-friendly field-level error.
     * Called INSIDE the catch block after a SQLException.
     *
     * @param sqlMessage the Oracle error message from the SQLException
     * @return a one-entry map mapping field name to user-friendly message
     */
    public static Map<String, String> parseSqlError(String sqlMessage) {
        Map<String, String> error = new HashMap<>();
        if(sqlMessage == null) return error;

        String m = sqlMessage.toUpperCase();

        // Foreign key violation — the referenced item doesn't exist
        if(m.contains("FK_ITEM_DETAILS_ITEMS")) {
            error.put("itemId", "Invalid item reference.");
        }
        // CHECK constraint on description length (must be >= 3)
        else if(m.contains("CHK_ITEM_DETAILS_DESCRIPTION_LENGTH")) {
            error.put("description", "Description must be at least 3 characters.");
        }
        // NOT NULL violation (ORA-01400) — a required column was not provided
        else if(m.contains("ORA-01400")) {
            if(m.contains("DESCRIPTION")) error.put("description", "Description is required.");
        }

        return error;
    }

    /**
     * Validates that the description is present and meets length requirements.
     *
     * @param description the raw description text
     * @param errors      the error map to add to
     */
    private static void checkDescription(String description, Map<String, String> errors) {
        if(description == null || description.trim().isEmpty()) {
            errors.put("description", "Description is required.");
            return;
        }
        // Must be at least 3 characters (matching the DB CHECK constraint)
        if(description.trim().length() < 3)
            errors.put("description", "Description must be at least 3 characters.");
    }
}
