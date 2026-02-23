/**
 * Checkpoint 2: Bug 2 — markDone() should use .equals() for String comparison.
 * DO NOT MODIFY THIS FILE.
 *
 * NOTE: These tests assume Bug 1 is already fixed (IDs auto-increment).
 */
public class TestCP2 {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        // Test 1: markDone returns true for existing TODO
        test("markDone returns true for existing todo", () -> {
            TodoRepository repo = new TodoRepository();
            TodoService service = new TodoService(repo);
            TodoItem item = service.addTodo("Task", "desc");
            // Use a NEW String object (not the same reference) to test .equals() vs ==
            String idCopy = new String(item.getId());
            boolean result = service.markDone(idCopy);
            assertTrue(result, "markDone should return true");
        });

        // Test 2: markDone actually changes the done status
        test("markDone changes done status", () -> {
            TodoRepository repo = new TodoRepository();
            TodoService service = new TodoService(repo);
            TodoItem item = service.addTodo("Task", "desc");
            assertTrue(!item.isDone(), "should start as not done");
            service.markDone(new String(item.getId()));
            assertTrue(item.isDone(), "should be done after markDone");
        });

        // Test 3: markDone returns false for non-existing ID
        test("markDone returns false for non-existing id", () -> {
            TodoRepository repo = new TodoRepository();
            TodoService service = new TodoService(repo);
            service.addTodo("Task", "desc");
            boolean result = service.markDone("nonexistent");
            assertTrue(!result, "markDone should return false for non-existing id");
        });

        // Test 4: markDone with null id does not throw NPE
        test("markDone with null id returns false without NPE", () -> {
            TodoRepository repo = new TodoRepository();
            TodoService service = new TodoService(repo);
            service.addTodo("Task", "desc");
            try {
                boolean result = service.markDone(null);
                assertTrue(!result, "markDone(null) should return false");
            } catch (NullPointerException e) {
                throw new AssertionError("markDone(null) should not throw NPE");
            }
        });

        // Test 5: markDone second item (not just first)
        test("markDone second item by id", () -> {
            TodoRepository repo = new TodoRepository();
            TodoService service = new TodoService(repo);
            service.addTodo("First", "d1");
            TodoItem second = service.addTodo("Second", "d2");
            service.markDone(new String(second.getId()));
            assertTrue(second.isDone(), "second item should be marked done");
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

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void printSummary() {
        System.out.println("\n========================================");
        System.out.println("CP2 Results: " + passed + " passed, " + failed + " failed");
        if (failed == 0) System.out.println("ALL TESTS PASSED");
        else System.out.println("SOME TESTS FAILED");
        System.out.println("========================================");
        System.exit(failed > 0 ? 1 : 0);
    }
}
