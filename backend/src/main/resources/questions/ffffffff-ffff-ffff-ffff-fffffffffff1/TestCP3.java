import java.util.*;

/**
 * Checkpoint 3: cancelOrder() tests with compensation logic.
 * DO NOT MODIFY THIS FILE.
 * NOTE: Assumes CP1 and CP2 are fixed.
 */
public class TestCP3 {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        // Test 1: Cancel CREATED order — no inventory release needed
        test("cancel CREATED order skips release", () -> {
            OrderRepository repo = new OrderRepository();
            InventoryService inv = new InventoryService();
            EventPublisher pub = new EventPublisher();
            OrderService service = new OrderService(repo, inv, pub);

            service.createOrder("o1", List.of("itemA"));
            service.cancelOrder("o1");

            assertEqual(OrderStatus.CANCELLED, service.getOrder("o1").getStatus(), "status");
            assertEqual(0, inv.getReleasedItems().size(), "no release for CREATED");
        });

        // Test 2: Cancel CONFIRMED order — releases inventory
        test("cancel CONFIRMED order releases inventory", () -> {
            OrderRepository repo = new OrderRepository();
            InventoryService inv = new InventoryService();
            EventPublisher pub = new EventPublisher();
            OrderService service = new OrderService(repo, inv, pub);

            service.createOrder("o2", List.of("itemA", "itemB"));
            service.confirmOrder("o2");
            pub.clear();
            service.cancelOrder("o2");

            assertEqual(OrderStatus.CANCELLED, service.getOrder("o2").getStatus(), "status");
            assertEqual(2, inv.getReleasedItems().size(), "released items count");
        });

        // Test 3: Cancel publishes event
        test("cancel publishes OrderCancelledEvent", () -> {
            OrderRepository repo = new OrderRepository();
            InventoryService inv = new InventoryService();
            EventPublisher pub = new EventPublisher();
            OrderService service = new OrderService(repo, inv, pub);

            service.createOrder("o3", List.of("itemA"));
            service.cancelOrder("o3");

            assertEqual(1, pub.getPublishedEvents().size(), "event count");
            assertEqual("CANCELLED", pub.getPublishedEvents().get(0).type(), "event type");
            assertEqual(false, pub.getPublishedEvents().get(0).releaseFailure(), "releaseFailure");
        });

        // Test 4: Cancel non-existing order throws OrderNotFoundException
        test("cancel non-existing throws OrderNotFoundException", () -> {
            OrderRepository repo = new OrderRepository();
            InventoryService inv = new InventoryService();
            EventPublisher pub = new EventPublisher();
            OrderService service = new OrderService(repo, inv, pub);
            try {
                service.cancelOrder("nonexistent");
                fail("Expected OrderNotFoundException");
            } catch (OrderNotFoundException e) {
                // Expected
            }
        });

        // Test 5: Cancel with release failure — still cancels (best-effort)
        test("cancel with release failure still cancels", () -> {
            OrderRepository repo = new OrderRepository();
            InventoryService inv = new InventoryService();
            EventPublisher pub = new EventPublisher();
            OrderService service = new OrderService(repo, inv, pub);

            service.createOrder("o5", List.of("itemA"));
            service.confirmOrder("o5");
            pub.clear();

            inv.setFailRelease(true);
            service.cancelOrder("o5"); // should NOT throw

            assertEqual(OrderStatus.CANCELLED, service.getOrder("o5").getStatus(), "status");
        });

        // Test 6: Cancel with release failure sets releaseFailure flag
        test("cancel with release failure sets releaseFailure=true", () -> {
            OrderRepository repo = new OrderRepository();
            InventoryService inv = new InventoryService();
            EventPublisher pub = new EventPublisher();
            OrderService service = new OrderService(repo, inv, pub);

            service.createOrder("o6", List.of("itemA"));
            service.confirmOrder("o6");
            pub.clear();

            inv.setFailRelease(true);
            service.cancelOrder("o6");

            assertEqual(1, pub.getPublishedEvents().size(), "event count");
            assertEqual(true, pub.getPublishedEvents().get(0).releaseFailure(), "releaseFailure flag");
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
        System.out.println("CP3 Results: " + passed + " passed, " + failed + " failed");
        if (failed == 0) System.out.println("ALL TESTS PASSED");
        else System.out.println("SOME TESTS FAILED");
        System.out.println("========================================");
        System.exit(failed > 0 ? 1 : 0);
    }
}
