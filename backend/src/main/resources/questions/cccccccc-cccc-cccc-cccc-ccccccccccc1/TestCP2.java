import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TestCP2 {

    static int passed = 0, failed = 0;

    public static void main(String[] args) throws InterruptedException {
        testSubscriberCount();
        testActiveTopics();
        testPublishAsync();
        testShutdown();

        System.out.println("\n=== CP2 Results: " + passed + " passed, " + failed + " failed ===");
        if (failed > 0) System.exit(1);
    }

    static void testSubscriberCount() {
        EventBus bus = new EventBus();
        check("No subscribers → count 0", 0, bus.subscriberCount("x"));

        bus.subscribe("x", e -> {});
        check("1 subscriber → count 1", 1, bus.subscriberCount("x"));

        bus.subscribe("x", e -> {});
        check("2 subscribers → count 2", 2, bus.subscriberCount("x"));
    }

    static void testActiveTopics() {
        EventBus bus = new EventBus();
        check("No subscriptions → 0 active topics", 0, bus.activeTopics().size());

        bus.subscribe("alpha", e -> {});
        bus.subscribe("beta",  e -> {});
        Set<String> topics = bus.activeTopics();
        check("2 topics → 2 active", 2, topics.size());
        check("alpha is active", true, topics.contains("alpha"));
        check("beta is active",  true, topics.contains("beta"));
    }

    static void testPublishAsync() throws InterruptedException {
        EventBus bus = new EventBus();
        AtomicInteger received = new AtomicInteger(0);

        bus.subscribe("async-topic", e -> received.incrementAndGet());
        bus.publishAsync("async-topic", "event-1");
        bus.publishAsync("async-topic", "event-2");
        bus.publishAsync("async-topic", "event-3");

        // Wait for async events to be processed
        bus.shutdown(3000);

        check("Async: all 3 events received", 3, received.get());
    }

    static void testShutdown() throws InterruptedException {
        EventBus bus = new EventBus();
        AtomicInteger received = new AtomicInteger(0);

        bus.subscribe("s", e -> {
            try { Thread.sleep(10); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            received.incrementAndGet();
        });

        bus.publishAsync("s", "a");
        bus.publishAsync("s", "b");
        bus.shutdown(5000);

        // After shutdown, publishAsync should be silently ignored (no exception)
        boolean threw = false;
        try {
            bus.publishAsync("s", "ignored");
        } catch (Exception e) {
            threw = true;
        }
        check("Shutdown: no exception on publishAsync after shutdown", false, threw);
        check("Shutdown: events submitted before shutdown were processed", 2, received.get());
    }

    static void check(String name, int expected, int actual) {
        if (expected == actual) {
            System.out.println("[PASS] " + name);
            passed++;
        } else {
            System.out.println("[FAIL] " + name + " — expected: " + expected + ", got: " + actual);
            failed++;
        }
    }

    static void check(String name, boolean expected, boolean actual) {
        if (expected == actual) {
            System.out.println("[PASS] " + name);
            passed++;
        } else {
            System.out.println("[FAIL] " + name + " — expected: " + expected + ", got: " + actual);
            failed++;
        }
    }
}
