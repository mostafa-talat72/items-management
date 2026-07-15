// Package declaration — validates Item form fields and maps DB constraint errors to field-level messages
package com.items.util;

// HashMap and Map are used to return key-value error pairs (fieldName → errorMessage)
import java.util.HashMap;
import java.util.Map;

/**
 * Server-side validator for the Item entity form fields.
 *
 * Validation is performed in TWO phases:
 *
 * Phase 1 — validateEmptyAndFormat() — runs BEFORE any database call.
 *   Checks:
 *     - All required fields are present (not empty)
 *     - Numeric fields (price, totalNumber) are parseable as numbers
 *   If this phase fails, the form is re-displayed with field-level errors
 *   and NO database operation is attempted.
 *
 * Phase 2 — validateBusinessRules() — runs INSIDE the catch block AFTER
 *   a SQLException. The business rules (negative/zero values) are merged
 *   with the parsed SQL error message. This way ALL errors appear on the form
 *   simultaneously, even though some could only be detected at the DB level.
 *
 * Why two phases?
 *   The database enforces CHECK constraints (CHK_ITEMS_PRICE, CHK_ITEMS_TOTAL_NUMBER).
 *   If we submitted a negative price, the DB would reject it with ORA-02290.
 *   But we also want to check negative/zero values ourselves so we can show
 *   a friendlier error message. The merge strategy gives us the best of both:
 *   pre-DB validation for format + post-DB merging for business rules.
 */
public class ItemValidator {

    /** Private constructor — utility class. */
    private ItemValidator() {}

    /**
     * Phase 1: Checks that fields are present and parseable.
     * Called BEFORE attempting the database operation.
     *
     * @param name           the item name string
     * @param priceStr       the price string (raw from form)
     * @param totalNumberStr the total number string (raw from form)
     * @return a map of field→error (empty map = no errors)
     */
    public static Map<String, String> validateEmptyAndFormat(String name, String priceStr, String totalNumberStr) {
        Map<String, String> errors = new HashMap<>();
        checkName(name, errors);
        checkPriceFormat(priceStr, errors);
        checkTotalNumberFormat(totalNumberStr, errors);
        return errors;
    }

    /**
     * Phase 2: Checks business rules (negative/zero values).
     * Called INSIDE the catch block after a SQLException.
     * Results are merged with parseSqlError() output.
     *
     * @param priceStr       the price string from the form
     * @param totalNumberStr the total number string from the form
     * @return a map of field→error for business rule violations
     */
    public static Map<String, String> validateBusinessRules(String priceStr, String totalNumberStr) {
        Map<String, String> errors = new HashMap<>();
        checkPriceNegative(priceStr, errors);
        checkTotalNumberNegative(totalNumberStr, errors);
        return errors;
    }

    /**
     * Converts an Oracle SQL error message into a user-friendly field-level error.
     * Returns an empty map if the message doesn't match any known constraint.
     *
     * Known constraint names from the database:
     *   UQ_ITEMS_NAME          — unique item name violation
     *   CHK_ITEMS_PRICE        — price must be > 0
     *   CHK_ITEMS_TOTAL_NUMBER — total number must be > 0
     *   CHK_ITEMS_NAME_LENGTH  — name must be at least 1 character
     *   ORA-01400              — NOT NULL constraint (required field missing)
     *
     * @param sqlMessage the Oracle error message from the SQLException
     * @return a one-entry map with the field name and user-friendly message
     */
    public static Map<String, String> parseSqlError(String sqlMessage) {
        Map<String, String> error = new HashMap<>();
        if(sqlMessage == null) return error;

        String m = sqlMessage.toUpperCase();

        // Unique constraint violation — item name already exists in the database
        if(m.contains("UQ_ITEMS_NAME")) {
            error.put("name", "An item with this name already exists.");
        }
        // CHECK constraint on price — value must be > 0
        else if(m.contains("CHK_ITEMS_PRICE")) {
            error.put("price", "Price must be greater than zero.");
        }
        // CHECK constraint on total_number — value must be > 0
        else if(m.contains("CHK_ITEMS_TOTAL_NUMBER")) {
            error.put("totalNumber", "Total number must be greater than zero.");
        }
        // CHECK constraint on name length — must be at least 1 character
        else if(m.contains("CHK_ITEMS_NAME_LENGTH")) {
            error.put("name", "Item name must be at least 1 character.");
        }
        // NOT NULL violation (ORA-01400) — a required column was not provided
        else if(m.contains("ORA-01400")) {
            if(m.contains("NAME"))         error.put("name", "Item name is required.");
            else if(m.contains("PRICE"))   error.put("price", "Price is required.");
            else if(m.contains("TOTAL_NUMBER")) error.put("totalNumber", "Total number is required.");
        }

        return error;
    }

    /**
     * Validates the item name is not empty.
     *
     * @param name   the raw name string
     * @param errors the error map to add to
     */
    private static void checkName(String name, Map<String, String> errors) {
        if(name == null || name.trim().isEmpty())
            errors.put("name", "Item name is required.");
    }

    /**
     * Validates that the price string is not empty and can be parsed as a double.
     *
     * @param priceStr the raw price string from the form
     * @param errors   the error map to add to
     */
    private static void checkPriceFormat(String priceStr, Map<String, String> errors) {
        if(priceStr == null || priceStr.trim().isEmpty()) {
            errors.put("price", "Price is required.");
            return;
        }
        try { Double.parseDouble(priceStr); }
        catch(NumberFormatException e) { errors.put("price", "Invalid price format."); }
    }

    /**
     * Validates that the total number string is not empty and can be parsed as an integer.
     *
     * @param totalNumberStr the raw total number string from the form
     * @param errors         the error map to add to
     */
    private static void checkTotalNumberFormat(String totalNumberStr, Map<String, String> errors) {
        if(totalNumberStr == null || totalNumberStr.trim().isEmpty()) {
            errors.put("totalNumber", "Total number is required.");
            return;
        }
        try { Integer.parseInt(totalNumberStr); }
        catch(NumberFormatException e) { errors.put("totalNumber", "Invalid total number format."); }
    }

    /**
     * Checks if the price is ≤ 0 (negative or zero — not allowed).
     * This is called after a SQL error because the DB constraint would also
     * catch this, but we want a friendlier message.
     *
     * @param priceStr the raw price string
     * @param errors   the error map to add to
     */
    private static void checkPriceNegative(String priceStr, Map<String, String> errors) {
        if(priceStr == null || priceStr.trim().isEmpty()) return;
        try {
            double price = Double.parseDouble(priceStr);
            if(price <= 0) errors.put("price", "Price must be greater than zero.");
        } catch(NumberFormatException ignored) {
            // If it's not a valid number, the format validation already caught this
        }
    }

    /**
     * Checks if the quantity is ≤ 0 (negative or zero — not allowed).
     * Called after a SQL error for the same reason as checkPriceNegative.
     *
     * @param totalNumberStr the raw total number string
     * @param errors         the error map to add to
     */
    private static void checkTotalNumberNegative(String totalNumberStr, Map<String, String> errors) {
        if(totalNumberStr == null || totalNumberStr.trim().isEmpty()) return;
        try {
            int qty = Integer.parseInt(totalNumberStr);
            if(qty <= 0) errors.put("totalNumber", "Total number must be greater than zero.");
        } catch(NumberFormatException ignored) {
            // If it's not a valid number, the format validation already caught this
        }
    }
}
