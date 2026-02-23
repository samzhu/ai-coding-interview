import java.util.*;

/**
 * Checkpoint 3: searchBooks() tests.
 * DO NOT MODIFY THIS FILE.
 */
public class TestCP3 {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        // Test 1: Search with no filters returns all books sorted by title
        test("searchBooks no filters returns all sorted", () -> {
            BookRepository repo = new BookRepository();
            BookService service = new BookService(repo);
            List<Book> results = service.searchBooks(null, null, null, null);
            assertTrue(results.size() == 6, "expected 6 books but got " + results.size());
            // Check alphabetical sorting (case-insensitive)
            assertEqual("Clean Code", results.get(0).getTitle(), "first title");
            assertEqual("Deleted Book", results.get(1).getTitle(), "second title");
        });

        // Test 2: Filter by author
        test("searchBooks filter by author", () -> {
            BookRepository repo = new BookRepository();
            BookService service = new BookService(repo);
            List<Book> results = service.searchBooks("Robert Martin", null, null, null);
            assertTrue(results.size() == 1, "expected 1 book but got " + results.size());
            assertEqual("Clean Code", results.get(0).getTitle(), "title");
        });

        // Test 3: Filter by author (case-insensitive)
        test("searchBooks filter by author case-insensitive", () -> {
            BookRepository repo = new BookRepository();
            BookService service = new BookService(repo);
            List<Book> results = service.searchBooks("robert martin", null, null, null);
            assertTrue(results.size() == 1, "expected 1 book but got " + results.size());
        });

        // Test 4: Filter by status
        test("searchBooks filter by status", () -> {
            BookRepository repo = new BookRepository();
            BookService service = new BookService(repo);
            List<Book> results = service.searchBooks(null, "CHECKED_OUT", null, null);
            assertTrue(results.size() == 1, "expected 1 book but got " + results.size());
            assertEqual("Refactoring", results.get(0).getTitle(), "title");
        });

        // Test 5: Filter by price range
        test("searchBooks filter by price range", () -> {
            BookRepository repo = new BookRepository();
            BookService service = new BookService(repo);
            List<Book> results = service.searchBooks(null, null, 40.00, 50.00);
            // Effective Java (45.00), Refactoring (49.99), The Pragmatic Programmer (42.50)
            assertTrue(results.size() == 3, "expected 3 books but got " + results.size());
        });

        // Test 6: Filter by minPrice only
        test("searchBooks filter by minPrice only", () -> {
            BookRepository repo = new BookRepository();
            BookService service = new BookService(repo);
            List<Book> results = service.searchBooks(null, null, 50.00, null);
            // Design Patterns (54.99)
            assertTrue(results.size() == 1, "expected 1 book but got " + results.size());
            assertEqual("Design Patterns", results.get(0).getTitle(), "title");
        });

        // Test 7: Filter by maxPrice only
        test("searchBooks filter by maxPrice only", () -> {
            BookRepository repo = new BookRepository();
            BookService service = new BookService(repo);
            List<Book> results = service.searchBooks(null, null, null, 15.00);
            // Deleted Book (10.00)
            assertTrue(results.size() == 1, "expected 1 book but got " + results.size());
        });

        // Test 8: Combined filters — author + status
        test("searchBooks combined author + status", () -> {
            BookRepository repo = new BookRepository();
            BookService service = new BookService(repo);
            List<Book> results = service.searchBooks("Robert Martin", "AVAILABLE", null, null);
            assertTrue(results.size() == 1, "expected 1 book but got " + results.size());
        });

        // Test 9: No matching results returns empty list
        test("searchBooks no match returns empty list", () -> {
            BookRepository repo = new BookRepository();
            BookService service = new BookService(repo);
            List<Book> results = service.searchBooks("Nonexistent Author", null, null, null);
            assertNotNull(results, "results");
            assertTrue(results.size() == 0, "expected empty list but got " + results.size());
        });

        // Test 10: Results are sorted by title case-insensitively
        test("searchBooks results sorted case-insensitively", () -> {
            BookRepository repo = new BookRepository();
            BookService service = new BookService(repo);
            List<Book> results = service.searchBooks(null, "AVAILABLE", null, null);
            for (int i = 0; i < results.size() - 1; i++) {
                String a = results.get(i).getTitle().toLowerCase();
                String b = results.get(i + 1).getTitle().toLowerCase();
                assertTrue(a.compareTo(b) <= 0,
                    "Not sorted: '" + results.get(i).getTitle() + "' should come before '" + results.get(i + 1).getTitle() + "'");
            }
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

    private static void assertNotNull(Object obj, String name) {
        if (obj == null) {
            throw new AssertionError(name + " should not be null");
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
        System.out.println("CP3 Results: " + passed + " passed, " + failed + " failed");
        if (failed == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println("SOME TESTS FAILED");
        }
        System.out.println("========================================");
        System.exit(failed > 0 ? 1 : 0);
    }
}
