import java.util.*;

/**
 * Checkpoint 3: Bug 3 — getByStatus() should not throw ConcurrentModificationException.
 * DO NOT MODIFY THIS FILE.
 *
 * NOTE: These tests assume Bugs 1 and 2 are already fixed.
 */
public class TestCP3 {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        // Test 1: getByStatus(false) returns only undone items
        test("getByStatus false returns undone items", () -> {
            TodoRepository repo = new TodoRepository();
            TodoService service = new TodoService(repo);
            service.addTodo("Task 1", "d1");
            service.addTodo("Task 2", "d2");
            TodoItem item3 = service.addTodo("Task 3", "d3");
            service.markDone(new String(item3.getId()));

            List<TodoItem> undone = service.getByStatus(false);
            assertEqual(2, undone.size(), "undone count");
        });

        // Test 2: getByStatus(true) returns only done items
        test("getByStatus true returns done items", () -> {
            TodoRepository repo = new TodoRepository();
            TodoService service = new TodoService(repo);
            TodoItem item1 = service.addTodo("Task 1", "d1");
            service.addTodo("Task 2", "d2");
            service.markDone(new String(item1.getId()));

            List<TodoItem> done = service.getByStatus(true);
            assertEqual(1, done.size(), "done count");
            assertEqual("Task 1", done.get(0).getTitle(), "done item title");
        });

        // Test 3: getByStatus with no matches returns empty list
        test("getByStatus no matches returns empty list", () -> {
            TodoRepository repo = new TodoRepository();
            TodoService service = new TodoService(repo);
            service.addTodo("Task 1", "d1");
            service.addTodo("Task 2", "d2");

            List<TodoItem> done = service.getByStatus(true);
            assertNotNull(done, "result list");
            assertEqual(0, done.size(), "done count");
        });

        // Test 4: getByStatus does not modify the original repository list
        test("getByStatus does not modify original list", () -> {
            TodoRepository repo = new TodoRepository();
            TodoService service = new TodoService(repo);
            service.addTodo("Task 1", "d1");
            service.addTodo("Task 2", "d2");
            TodoItem item3 = service.addTodo("Task 3", "d3");
            service.markDone(new String(item3.getId()));

            service.getByStatus(false); // should not modify repo
            assertEqual(3, service.count(), "total count after getByStatus");
        });

        // Test 5: getByStatus with empty repository
        test("getByStatus with empty repo returns empty list", () -> {
            TodoRepository repo = new TodoRepository();
            TodoService service = new TodoService(repo);
            List<TodoItem> result = service.getByStatus(false);
            assertNotNull(result, "result");
            assertEqual(0, result.size(), "count");
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
        if (obj == null) throw new AssertionError(name + " should not be null");
    }

    private static void printSummary() {
        System.out.println("\n========================================");
        System.out.println("CP3 Results: " + passed + " passed, " + failed + " failed");
        if (failed == 0) System.out.println("ALL TESTS PASSED");
        else System.out.println("SOME TESTS FAILED");
        System.out.println("========================================");
        System.exit(failed > 0 ? 1 : 0);
    }
}
