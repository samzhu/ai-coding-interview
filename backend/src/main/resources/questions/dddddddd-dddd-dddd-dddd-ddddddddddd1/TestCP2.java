/**
 * Checkpoint 2: createBook() tests.
 * DO NOT MODIFY THIS FILE.
 */
public class TestCP2 {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        // Test 1: Create a valid book
        test("createBook valid book", () -> {
            BookRepository repo = new BookRepository();
            BookService service = new BookService(repo);
            Book book = service.createBook("New Book", "Author", "978-1234567890", 29.99);
            assertNotNull(book, "created book");
            assertEqual("New Book", book.getTitle(), "title");
            assertEqual("Author", book.getAuthor(), "author");
            assertEqual("978-1234567890", book.getIsbn(), "isbn");
            assertEqual("AVAILABLE", book.getStatus(), "status");
            assertNotNull(book.getId(), "id");
        });

        // Test 2: Invalid ISBN throws ValidationException
        test("createBook invalid ISBN throws ValidationException", () -> {
            BookRepository repo = new BookRepository();
            BookService service = new BookService(repo);
            try {
                service.createBook("Book", "Author", "invalid-isbn", 29.99);
                fail("Expected ValidationException");
            } catch (ValidationException e) {
                // Expected
            }
        });

        // Test 3: Invalid price throws ValidationException
        test("createBook negative price throws ValidationException", () -> {
            BookRepository repo = new BookRepository();
            BookService service = new BookService(repo);
            try {
                service.createBook("Book", "Author", "978-1234567890", -5.00);
                fail("Expected ValidationException");
            } catch (ValidationException e) {
                // Expected
            }
        });

        // Test 4: Duplicate ISBN throws DuplicateIsbnException
        test("createBook duplicate ISBN throws DuplicateIsbnException", () -> {
            BookRepository repo = new BookRepository();
            BookService service = new BookService(repo);
            try {
                // "978-0132350884" is pre-loaded in repository (Clean Code)
                service.createBook("Another Book", "Author", "978-0132350884", 19.99);
                fail("Expected DuplicateIsbnException");
            } catch (DuplicateIsbnException e) {
                // Expected
            }
        });

        // Test 5: Created book is persisted
        test("createBook persists to repository", () -> {
            BookRepository repo = new BookRepository();
            BookService service = new BookService(repo);
            Book created = service.createBook("Persisted", "Author", "978-9999999999", 15.00);
            Book found = repo.findById(created.getId()).orElse(null);
            assertNotNull(found, "persisted book");
            assertEqual("Persisted", found.getTitle(), "title");
        });

        // Test 6: Price at upper boundary
        test("createBook price at upper boundary", () -> {
            BookRepository repo = new BookRepository();
            BookService service = new BookService(repo);
            Book book = service.createBook("Expensive", "Author", "978-1111111111", 9999.99);
            assertNotNull(book, "book at max price");
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

    private static void fail(String message) {
        throw new AssertionError(message);
    }

    private static void printSummary() {
        System.out.println("\n========================================");
        System.out.println("CP2 Results: " + passed + " passed, " + failed + " failed");
        if (failed == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println("SOME TESTS FAILED");
        }
        System.out.println("========================================");
        System.exit(failed > 0 ? 1 : 0);
    }
}
