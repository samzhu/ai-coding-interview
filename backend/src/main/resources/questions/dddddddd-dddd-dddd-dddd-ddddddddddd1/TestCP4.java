import java.util.*;

/**
 * Checkpoint 4: Edge cases and input validation.
 * DO NOT MODIFY THIS FILE.
 */
public class TestCP4 {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        // Test 1: findById(null) throws IllegalArgumentException
        test("findById null throws IllegalArgumentException", () -> {
            BookRepository repo = new BookRepository();
            BookService service = new BookService(repo);
            try {
                service.findById(null);
                fail("Expected IllegalArgumentException");
            } catch (IllegalArgumentException e) {
                // Expected
            }
        });

        // Test 2: findById deleted book throws BookNotFoundException
        test("findById deleted book throws BookNotFoundException", () -> {
            BookRepository repo = new BookRepository();
            BookService service = new BookService(repo);
            try {
                service.findById("6"); // Book 6 has status "DELETED"
                fail("Expected BookNotFoundException");
            } catch (BookNotFoundException e) {
                // Expected
            }
        });

        // Test 3: createBook with empty title throws ValidationException
        test("createBook empty title throws ValidationException", () -> {
            BookRepository repo = new BookRepository();
            BookService service = new BookService(repo);
            try {
                service.createBook("", "Author", "978-1234567890", 29.99);
                fail("Expected ValidationException");
            } catch (ValidationException e) {
                // Expected
            }
        });

        // Test 4: createBook with null title throws ValidationException
        test("createBook null title throws ValidationException", () -> {
            BookRepository repo = new BookRepository();
            BookService service = new BookService(repo);
            try {
                service.createBook(null, "Author", "978-1234567890", 29.99);
                fail("Expected ValidationException");
            } catch (ValidationException e) {
                // Expected
            }
        });

        // Test 5: createBook with empty author throws ValidationException
        test("createBook empty author throws ValidationException", () -> {
            BookRepository repo = new BookRepository();
            BookService service = new BookService(repo);
            try {
                service.createBook("Book", "", "978-1234567890", 29.99);
                fail("Expected ValidationException");
            } catch (ValidationException e) {
                // Expected
            }
        });

        // Test 6: createBook with null author throws ValidationException
        test("createBook null author throws ValidationException", () -> {
            BookRepository repo = new BookRepository();
            BookService service = new BookService(repo);
            try {
                service.createBook("Book", null, "978-1234567890", 29.99);
                fail("Expected ValidationException");
            } catch (ValidationException e) {
                // Expected
            }
        });

        // Test 7: createBook with zero price throws ValidationException
        test("createBook zero price throws ValidationException", () -> {
            BookRepository repo = new BookRepository();
            BookService service = new BookService(repo);
            try {
                service.createBook("Book", "Author", "978-1234567890", 0.00);
                fail("Expected ValidationException");
            } catch (ValidationException e) {
                // Expected
            }
        });

        // Test 8: searchBooks results are sorted case-insensitively
        test("searchBooks sorted case-insensitively with mixed case", () -> {
            BookRepository repo = new BookRepository();
            // Add a book with lowercase title
            repo.save(new Book(repo.nextId(), "a lowercase title", "Test", "978-9876543210", 10.00, "AVAILABLE"));
            BookService service = new BookService(repo);
            List<Book> results = service.searchBooks(null, "AVAILABLE", null, null);
            // "a lowercase title" should come before "Clean Code"
            assertEqual("a lowercase title", results.get(0).getTitle(), "first sorted title");
        });

        printSummary();
    }

    private static void test(String name, Runnable testCase) {
        try {
            testCase.run();
            passed++;
            System.out.println("  PASS: " + name);
        } catch (AssertionError e) {
            failed++;
            System.out.println("  FAIL: " + name + " — " + e.getMessage());
        } catch (Exception e) {
            failed++;
            System.out.println("  FAIL: " + name + " — " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static void assertEqual(Object expected, Object actual, String field) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected " + field + "='" + expected + "' but got '" + actual + "'");
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void fail(String message) {
        throw new AssertionError(message);
    }

    private static void printSummary() {
        System.out.println("\n========================================");
        System.out.println("CP4 Results: " + passed + " passed, " + failed + " failed");
        if (failed == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println("SOME TESTS FAILED");
        }
        System.out.println("========================================");
        System.exit(failed > 0 ? 1 : 0);
    }
}
