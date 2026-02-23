/**
 * Validation utilities for book data.
 * DO NOT MODIFY THIS FILE.
 */
public class ValidationHelper {

    /**
     * Validates ISBN format. Must start with "978-" and be 14 characters long.
     * Returns null if valid, error message if invalid.
     */
    public static String validateIsbn(String isbn) {
        if (isbn == null || isbn.isEmpty()) {
            return "ISBN cannot be empty";
        }
        if (!isbn.startsWith("978-")) {
            return "ISBN must start with '978-'";
        }
        if (isbn.length() != 14) {
            return "ISBN must be 14 characters long (e.g., 978-0132350884)";
        }
        return null; // valid
    }

    /**
     * Validates price range. Must be between 0.01 and 9999.99 (inclusive).
     * Returns null if valid, error message if invalid.
     */
    public static String validatePrice(double price) {
        if (price < 0.01) {
            return "Price must be at least 0.01";
        }
        if (price > 9999.99) {
            return "Price must not exceed 9999.99";
        }
        return null; // valid
    }
}
