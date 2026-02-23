/**
 * Checkpoint 1: Bug Fix — findById() tests.
 * DO NOT MODIFY THIS FILE.
 */
public class TestCP1 {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        BookRepository repo = new BookRepository();
        BookService service = new BookService(repo);

        // Test 1: Find existing book returns correct result
        test("findById existing book", () -> {
            Book book = service.findById("1");
            assertEqual("Clean Code", book.getTitle(), "title");
            assertEqual("Robert Martin", book.getAuthor(), "author");
        });

        // Test 2: Find non-existing book throws BookNotFoundException
        test("findById non-existing throws BookNotFoundException", () -> {
            try {
                service.findById("999");
                fail("Expected BookNotFoundException but no exception was thrown");
            } catch (BookNotFoundException e) {
                // Expected
            } catch (Exception e) {
                fail("Expected BookNotFoundException but got " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        });

        // Test 3: Find with non-numeric ID throws IllegalArgumentException
        test("findById non-numeric throws IllegalArgumentException", () -> {
            try {
                service.findById("abc");
                fail("Expected IllegalArgumentException but no exception was thrown");
            } catch (IllegalArgumentException e) {
                // Expected
            } catch (Exception e) {
                fail("Expected IllegalArgumentException but got " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        });

        // Test 4: Find another existing book
        test("findById book 2", () -> {
            Book book = service.findById("2");
            assertEqual("Effective Java", book.getTitle(), "title");
            assertEqual("Joshua Bloch", book.getAuthor(), "author");
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

    private static void fail(String message) {
        throw new AssertionError(message);
    }

    private static void printSummary() {
        System.out.println("\n========================================");
        System.out.println("CP1 Results: " + passed + " passed, " + failed + " failed");
        if (failed == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println("SOME TESTS FAILED");
        }
        System.out.println("========================================");
        System.exit(failed > 0 ? 1 : 0);
    }
}

class AssertionError extends RuntimeException {
    public AssertionError(String message) { super(message); }
}
