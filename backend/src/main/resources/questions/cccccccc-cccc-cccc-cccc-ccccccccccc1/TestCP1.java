import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class TestCP1 {

    static int passed = 0, failed = 0;

    public static void main(String[] args) {
        testBasicSubscribeAndPublish();
        testMultipleHandlers();
        testUnsubscribeStopsDelivery();
        testSelfUnsubscribeDuringDispatch();

        System.out.println("\n=== CP1 Results: " + passed + " passed, " + failed + " failed ===");
        if (failed > 0) System.exit(1);
    }

    static void testBasicSubscribeAndPublish() {
        EventBus bus = new EventBus();
        AtomicInteger count = new AtomicInteger(0);
        bus.subscribe("topic", e -> count.incrementAndGet());

        bus.publish("topic", "hello");
        bus.publish("topic", "world");
        check("Basic: handler called twice", 2, count.get());
    }

    static void testMultipleHandlers() {
        EventBus bus = new EventBus();
        AtomicInteger a = new AtomicInteger(0);
        AtomicInteger b = new AtomicInteger(0);
        bus.subscribe("news", e -> a.incrementAndGet());
        bus.subscribe("news", e -> b.incrementAndGet());

        bus.publish("news", "event");
        check("Multiple handlers: A called once", 1, a.get());
        check("Multiple handlers: B called once", 1, b.get());
    }

    static void testUnsubscribeStopsDelivery() {
        EventBus bus = new EventBus();
        AtomicInteger count = new AtomicInteger(0);
        Consumer<String> handler = e -> count.incrementAndGet();

        bus.subscribe("alerts", handler);
        bus.publish("alerts", "first");
        bus.unsubscribe("alerts", handler);
        bus.publish("alerts", "second");

        check("Unsubscribe: handler received only first event", 1, count.get());
    }

    /**
     * THE BUG TEST: A handler unsubscribes itself during dispatch.
     * With the buggy ArrayList iteration, this throws ConcurrentModificationException.
     * After the fix (defensive copy or CopyOnWriteArrayList), it must succeed.
     */
    static void testSelfUnsubscribeDuringDispatch() {
        EventBus bus = new EventBus();
        AtomicInteger count = new AtomicInteger(0);

        // Handler that removes itself from the bus on first call
        Consumer<String>[] ref = new Consumer[1];
        ref[0] = e -> {
            count.incrementAndGet();
            bus.unsubscribe("chat", ref[0]);   // modifies subscription list mid-dispatch
        };

        Consumer<String> other = e -> count.incrementAndGet();

        bus.subscribe("chat", ref[0]);
        bus.subscribe("chat", other);

        boolean threw = false;
        try {
            bus.publish("chat", "msg1");  // self-unsubscribes during iteration
            bus.publish("chat", "msg2");  // ref[0] should NOT be called again
        } catch (java.util.ConcurrentModificationException e) {
            threw = true;
        }

        check("Self-unsubscribe: no ConcurrentModificationException", false, threw);
        // ref[0] receives msg1 (1) + other receives msg1 (1) + other receives msg2 (1) = 3
        check("Self-unsubscribe: total call count = 3", 3, count.get());
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
