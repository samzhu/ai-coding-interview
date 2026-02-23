import java.util.*;

/**
 * Checkpoint 2: confirmOrder() tests.
 * DO NOT MODIFY THIS FILE.
 * NOTE: Assumes CP1 bugs are fixed.
 */
public class TestCP2 {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        // Test 1: Confirm order successfully
        test("confirmOrder success", () -> {
            OrderRepository repo = new OrderRepository();
            InventoryService inv = new InventoryService();
            EventPublisher pub = new EventPublisher();
            OrderService service = new OrderService(repo, inv, pub);

            Order order = service.createOrder("o1", List.of("itemA", "itemB"));
            service.confirmOrder("o1");

            assertEqual(OrderStatus.CONFIRMED, service.getOrder("o1").getStatus(), "status");
        });

        // Test 2: Confirm order reserves inventory
        test("confirmOrder reserves inventory", () -> {
            OrderRepository repo = new OrderRepository();
            InventoryService inv = new InventoryService();
            EventPublisher pub = new EventPublisher();
            OrderService service = new OrderService(repo, inv, pub);

            service.createOrder("o2", List.of("itemA", "itemB"));
            service.confirmOrder("o2");

            assertEqual(2, inv.getReservedItems().size(), "reserved items count");
        });

        // Test 3: Confirm order publishes event
        test("confirmOrder publishes OrderConfirmedEvent", () -> {
            OrderRepository repo = new OrderRepository();
            InventoryService inv = new InventoryService();
            EventPublisher pub = new EventPublisher();
            OrderService service = new OrderService(repo, inv, pub);

            service.createOrder("o3", List.of("itemA"));
            service.confirmOrder("o3");

            assertEqual(1, pub.getPublishedEvents().size(), "event count");
            assertEqual("CONFIRMED", pub.getPublishedEvents().get(0).type(), "event type");
            assertEqual("o3", pub.getPublishedEvents().get(0).orderId(), "event orderId");
        });

        // Test 4: Confirm non-existing order throws OrderNotFoundException
        test("confirmOrder non-existing throws OrderNotFoundException", () -> {
            OrderRepository repo = new OrderRepository();
            InventoryService inv = new InventoryService();
            EventPublisher pub = new EventPublisher();
            OrderService service = new OrderService(repo, inv, pub);
            try {
                service.confirmOrder("nonexistent");
                fail("Expected OrderNotFoundException");
            } catch (OrderNotFoundException e) {
                // Expected
            }
        });

        // Test 5: Confirm with insufficient stock — order stays CREATED
        test("confirmOrder insufficient stock keeps CREATED state", () -> {
            OrderRepository repo = new OrderRepository();
            InventoryService inv = new InventoryService();
            inv.setFailReserve(true);
            EventPublisher pub = new EventPublisher();
            OrderService service = new OrderService(repo, inv, pub);

            service.createOrder("o5", List.of("itemA"));
            try {
                service.confirmOrder("o5");
                fail("Expected InsufficientStockException");
            } catch (InsufficientStockException e) {
                // Expected
            }
            assertEqual(OrderStatus.CREATED, service.getOrder("o5").getStatus(), "status after failed confirm");
            assertEqual(0, pub.getPublishedEvents().size(), "no events on failure");
        });

        // Test 6: Idempotent confirm (already CONFIRMED)
        test("confirmOrder already confirmed is idempotent", () -> {
            OrderRepository repo = new OrderRepository();
            InventoryService inv = new InventoryService();
            EventPublisher pub = new EventPublisher();
            OrderService service = new OrderService(repo, inv, pub);

            service.createOrder("o6", List.of("itemA"));
            service.confirmOrder("o6");
            service.confirmOrder("o6"); // should not throw

            assertEqual(OrderStatus.CONFIRMED, service.getOrder("o6").getStatus(), "status");
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
        System.out.println("CP2 Results: " + passed + " passed, " + failed + " failed");
        if (failed == 0) System.out.println("ALL TESTS PASSED");
        else System.out.println("SOME TESTS FAILED");
        System.out.println("========================================");
        System.exit(failed > 0 ? 1 : 0);
    }
}
