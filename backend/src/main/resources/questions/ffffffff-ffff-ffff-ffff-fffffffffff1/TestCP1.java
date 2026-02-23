import java.util.*;

/**
 * Checkpoint 1: State transition bug fixes.
 * DO NOT MODIFY THIS FILE.
 */
public class TestCP1 {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        // Test 1: Normal linear transition CREATED -> CONFIRMED
        test("CREATED -> CONFIRMED works", () -> {
            Order order = new Order("o1", List.of("item1"));
            order.transition(OrderStatus.CONFIRMED);
            assertEqual(OrderStatus.CONFIRMED, order.getStatus(), "status");
        });

        // Test 2: Normal linear transition CONFIRMED -> SHIPPED
        test("CONFIRMED -> SHIPPED works", () -> {
            Order order = new Order("o2", List.of("item1"));
            order.transition(OrderStatus.CONFIRMED);
            order.transition(OrderStatus.SHIPPED);
            assertEqual(OrderStatus.SHIPPED, order.getStatus(), "status");
        });

        // Test 3: CREATED -> CANCELLED should work (Bug 1)
        test("CREATED -> CANCELLED works", () -> {
            Order order = new Order("o3", List.of("item1"));
            order.transition(OrderStatus.CANCELLED);
            assertEqual(OrderStatus.CANCELLED, order.getStatus(), "status");
        });

        // Test 4: CONFIRMED -> CANCELLED should work (Bug 1)
        test("CONFIRMED -> CANCELLED works", () -> {
            Order order = new Order("o4", List.of("item1"));
            order.transition(OrderStatus.CONFIRMED);
            order.transition(OrderStatus.CANCELLED);
            assertEqual(OrderStatus.CANCELLED, order.getStatus(), "status");
        });

        // Test 5: SHIPPED -> CANCELLED should work (Bug 1)
        test("SHIPPED -> CANCELLED works", () -> {
            Order order = new Order("o5", List.of("item1"));
            order.transition(OrderStatus.CONFIRMED);
            order.transition(OrderStatus.SHIPPED);
            order.transition(OrderStatus.CANCELLED);
            assertEqual(OrderStatus.CANCELLED, order.getStatus(), "status");
        });

        // Test 6: Idempotent — CONFIRMED -> CONFIRMED is no-op (Bug 2)
        test("CONFIRMED -> CONFIRMED is idempotent (no-op)", () -> {
            Order order = new Order("o6", List.of("item1"));
            order.transition(OrderStatus.CONFIRMED);
            order.transition(OrderStatus.CONFIRMED); // should not throw
            assertEqual(OrderStatus.CONFIRMED, order.getStatus(), "status");
        });

        // Test 7: DELIVERED -> CANCELLED should throw (DELIVERED is terminal)
        test("DELIVERED -> CANCELLED throws exception", () -> {
            Order order = new Order("o7", List.of("item1"));
            order.transition(OrderStatus.CONFIRMED);
            order.transition(OrderStatus.SHIPPED);
            order.transition(OrderStatus.DELIVERED);
            try {
                order.transition(OrderStatus.CANCELLED);
                fail("Expected IllegalStateException");
            } catch (IllegalStateException e) {
                // Expected
            }
        });

        // Test 8: Invalid transition CREATED -> SHIPPED should throw
        test("CREATED -> SHIPPED throws exception", () -> {
            Order order = new Order("o8", List.of("item1"));
            try {
                order.transition(OrderStatus.SHIPPED);
                fail("Expected IllegalStateException");
            } catch (IllegalStateException e) {
                // Expected
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
        if (!expected.equals(actual))
            throw new AssertionError("Expected " + field + "='" + expected + "' but got '" + actual + "'");
    }

    private static void fail(String message) { throw new AssertionError(message); }

    private static void printSummary() {
        System.out.println("\n========================================");
        System.out.println("CP1 Results: " + passed + " passed, " + failed + " failed");
        if (failed == 0) System.out.println("ALL TESTS PASSED");
        else System.out.println("SOME TESTS FAILED");
        System.out.println("========================================");
        System.exit(failed > 0 ? 1 : 0);
    }
}

class AssertionError extends RuntimeException {
    public AssertionError(String message) { super(message); }
}
