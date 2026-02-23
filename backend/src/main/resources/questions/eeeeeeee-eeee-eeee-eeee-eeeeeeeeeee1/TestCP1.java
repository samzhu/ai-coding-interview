/**
 * Checkpoint 1: Bug 1 — addTodo() ID should auto-increment.
 * DO NOT MODIFY THIS FILE.
 */
public class TestCP1 {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        // Test 1: First TODO gets id "1"
        test("first todo gets id 1", () -> {
            TodoRepository repo = new TodoRepository();
            TodoService service = new TodoService(repo);
            TodoItem item = service.addTodo("Task 1", "Description 1");
            assertEqual("1", item.getId(), "id");
        });

        // Test 2: Second TODO gets id "2" (not "1" again)
        test("second todo gets id 2", () -> {
            TodoRepository repo = new TodoRepository();
            TodoService service = new TodoService(repo);
            service.addTodo("Task 1", "Description 1");
            TodoItem item2 = service.addTodo("Task 2", "Description 2");
            assertEqual("2", item2.getId(), "id");
        });

        // Test 3: Three TODOs have unique IDs
        test("three todos have unique IDs", () -> {
            TodoRepository repo = new TodoRepository();
            TodoService service = new TodoService(repo);
            TodoItem item1 = service.addTodo("A", "a");
            TodoItem item2 = service.addTodo("B", "b");
            TodoItem item3 = service.addTodo("C", "c");
            assertTrue(!item1.getId().equals(item2.getId()), "id 1 and 2 should be different");
            assertTrue(!item2.getId().equals(item3.getId()), "id 2 and 3 should be different");
            assertTrue(!item1.getId().equals(item3.getId()), "id 1 and 3 should be different");
        });

        // Test 4: Count increases with each add
        test("count increases with each add", () -> {
            TodoRepository repo = new TodoRepository();
            TodoService service = new TodoService(repo);
            assertEqual(0, service.count(), "initial count");
            service.addTodo("Task 1", "D1");
            assertEqual(1, service.count(), "after first add");
            service.addTodo("Task 2", "D2");
            assertEqual(2, service.count(), "after second add");
        });

        // Test 5: findById works after add
        test("findById works after add", () -> {
            TodoRepository repo = new TodoRepository();
            TodoService service = new TodoService(repo);
            TodoItem added = service.addTodo("Find Me", "desc");
            TodoItem found = service.findById(added.getId());
            assertNotNull(found, "found item");
            assertEqual("Find Me", found.getTitle(), "title");
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

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void printSummary() {
        System.out.println("\n========================================");
        System.out.println("CP1 Results: " + passed + " passed, " + failed + " failed");
        if (failed == 0) System.out.println("ALL TESTS PASSED");
        else System.out.println("SOME TESTS FAILED");
        System.out.println("========================================");
        System.exit(failed > 0 ? 1 : 0);
    }
}
