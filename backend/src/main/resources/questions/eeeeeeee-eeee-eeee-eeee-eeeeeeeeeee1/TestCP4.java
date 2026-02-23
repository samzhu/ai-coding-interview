/**
 * Checkpoint 4: Bug 4 — deleteTodo() should also remove from the map.
 * DO NOT MODIFY THIS FILE.
 *
 * NOTE: These tests assume Bugs 1, 2, and 3 are already fixed.
 */
public class TestCP4 {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        // Test 1: deleteTodo removes from both list and map
        test("deleteTodo removes from both list and map", () -> {
            TodoRepository repo = new TodoRepository();
            TodoService service = new TodoService(repo);
            TodoItem item = service.addTodo("Delete Me", "desc");
            String id = item.getId();
            service.deleteTodo(new String(id));
            // findById uses the map — should return null after deletion
            TodoItem found = service.findById(id);
            assertNull(found, "deleted item should not be found by findById");
        });

        // Test 2: deleteTodo decreases count
        test("deleteTodo decreases count", () -> {
            TodoRepository repo = new TodoRepository();
            TodoService service = new TodoService(repo);
            service.addTodo("A", "d1");
            TodoItem item2 = service.addTodo("B", "d2");
            assertEqual(2, service.count(), "before delete");
            service.deleteTodo(new String(item2.getId()));
            assertEqual(1, service.count(), "after delete");
        });

        // Test 3: deleteTodo returns true for existing item
        test("deleteTodo returns true for existing item", () -> {
            TodoRepository repo = new TodoRepository();
            TodoService service = new TodoService(repo);
            TodoItem item = service.addTodo("Task", "desc");
            boolean result = service.deleteTodo(new String(item.getId()));
            assertTrue(result, "deleteTodo should return true");
        });

        // Test 4: deleteTodo returns false for non-existing id
        test("deleteTodo returns false for non-existing id", () -> {
            TodoRepository repo = new TodoRepository();
            TodoService service = new TodoService(repo);
            boolean result = service.deleteTodo("nonexistent");
            assertTrue(!result, "deleteTodo should return false for non-existing id");
        });

        // Test 5: After delete, list and map sizes are consistent
        test("list and map sizes consistent after delete", () -> {
            TodoRepository repo = new TodoRepository();
            TodoService service = new TodoService(repo);
            service.addTodo("A", "d1");
            TodoItem item2 = service.addTodo("B", "d2");
            service.addTodo("C", "d3");
            service.deleteTodo(new String(item2.getId()));
            assertEqual(repo.listSize(), repo.mapSize(),
                "list size (" + repo.listSize() + ") and map size (" + repo.mapSize() + ")");
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

    private static void assertNull(Object obj, String name) {
        if (obj != null) throw new AssertionError(name + " — expected null but got: " + obj);
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void printSummary() {
        System.out.println("\n========================================");
        System.out.println("CP4 Results: " + passed + " passed, " + failed + " failed");
        if (failed == 0) System.out.println("ALL TESTS PASSED");
        else System.out.println("SOME TESTS FAILED");
        System.out.println("========================================");
        System.exit(failed > 0 ? 1 : 0);
    }
}
