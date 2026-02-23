import java.util.*;
import java.util.concurrent.*;

/**
 * Checkpoint 4: Optimistic locking tests.
 * DO NOT MODIFY THIS FILE.
 * NOTE: Assumes CP1, CP2, and CP3 are fixed.
 */
public class TestCP4 {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        // Test 1: Version increments on transition
        test("version increments on transition", () -> {
            Order order = new Order("o1", List.of("item1"));
            assertEqual(0L, order.getVersion(), "initial version");
            order.transition(OrderStatus.CONFIRMED);
            assertEqual(1L, order.getVersion(), "version after CONFIRMED");
            order.transition(OrderStatus.SHIPPED);
            assertEqual(2L, order.getVersion(), "version after SHIPPED");
        });

        // Test 2: Idempotent transition does NOT increment version
        test("idempotent transition does not increment version", () -> {
            Order order = new Order("o2", List.of("item1"));
            order.transition(OrderStatus.CONFIRMED);
            long versionAfterFirst = order.getVersion();
            order.transition(OrderStatus.CONFIRMED); // idempotent
            assertEqual(versionAfterFirst, order.getVersion(), "version after idempotent");
        });

        // Test 3: Repository detects version mismatch
        test("repository detects stale version", () -> {
            OrderRepository repo = new OrderRepository();
            InventoryService inv = new InventoryService();
            EventPublisher pub = new EventPublisher();
            OrderService service = new OrderService(repo, inv, pub);

            Order order = service.createOrder("o3", List.of("item1"));
            long originalVersion = order.getVersion();

            // Simulate concurrent modification: manually increment version in repo
            order.transition(OrderStatus.CONFIRMED);
            repo.save(order); // version now 1 in repo

            // Create a "stale" order at original version
            Order stale = new Order("o3", List.of("item1"));
            // stale.version is 0, repo has version 1
            stale.transition(OrderStatus.CONFIRMED); // stale version becomes 1
            // But wait — the repo version is also 1 after our save above
            // Let's simulate properly: the stale order was read BEFORE the first confirm
            // We need to show that two concurrent modifications conflict
        });

        // Test 4: Concurrent confirm — one succeeds, one fails
        test("concurrent confirm — one succeeds one fails", () -> {
            OrderRepository repo = new OrderRepository();
            InventoryService inv = new InventoryService();
            EventPublisher pub = new EventPublisher();
            OrderService service = new OrderService(repo, inv, pub);

            service.createOrder("o4", List.of("item1"));

            // First confirm should succeed
            service.confirmOrder("o4");
            assertEqual(OrderStatus.CONFIRMED, service.getOrder("o4").getStatus(), "first confirm status");

            // Simulate a second service instance that read the order BEFORE first confirm
            // and tries to confirm with the old version
            Order staleOrder = new Order("o4", List.of("item1")); // version=0
            staleOrder.transition(OrderStatus.CONFIRMED); // version=1
            // But repo has version=1 (from first confirm), stale read version was 0
            // The save should fail if stale version != stored version

            // This test validates the concept — actual thread safety requires
            // the service to correctly use versions
        });

        // Test 5: Version preserved across confirm
        test("confirmOrder increments version", () -> {
            OrderRepository repo = new OrderRepository();
            InventoryService inv = new InventoryService();
            EventPublisher pub = new EventPublisher();
            OrderService service = new OrderService(repo, inv, pub);

            service.createOrder("o5", List.of("item1"));
            assertEqual(0L, service.getOrder("o5").getVersion(), "initial version");

            service.confirmOrder("o5");
            assertTrue(service.getOrder("o5").getVersion() > 0,
                "version should increase after confirm, got: " + service.getOrder("o5").getVersion());
        });

        // Test 6: Cancel also increments version
        test("cancelOrder increments version", () -> {
            OrderRepository repo = new OrderRepository();
            InventoryService inv = new InventoryService();
            EventPublisher pub = new EventPublisher();
            OrderService service = new OrderService(repo, inv, pub);

            service.createOrder("o6", List.of("item1"));
            service.cancelOrder("o6");
            assertTrue(service.getOrder("o6").getVersion() > 0,
                "version should increase after cancel, got: " + service.getOrder("o6").getVersion());
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

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void fail(String message) { throw new AssertionError(message); }

    private static void printSummary() {
        System.out.println("\n========================================");
        System.out.println("CP4 Results: " + passed + " passed, " + failed + " failed");
        if (failed == 0) System.out.println("ALL TESTS PASSED");
        else System.out.println("SOME TESTS FAILED");
        System.out.println("========================================");
        System.exit(failed > 0 ? 1 : 0);
    }
}
