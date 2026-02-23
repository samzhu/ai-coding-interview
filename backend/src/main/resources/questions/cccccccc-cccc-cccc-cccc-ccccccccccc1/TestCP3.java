import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class TestCP3 {

    static int passed = 0, failed = 0;

    public static void main(String[] args) throws Exception {
        testDuplicateSubscribers();
        testUnsubscribeNonExistentHandler();
        testPublishToEmptyTopic();
        testHandlerUnsubscribesDuringDispatch();
        testConcurrentSubscribeAndPublish();
        testPublishAsyncAfterShutdown();

        System.out.println("\n=== CP3 Results: " + passed + " passed, " + failed + " failed ===");
        if (failed > 0) System.exit(1);
    }

    static void testDuplicateSubscribers() {
        EventBus bus = new EventBus();
        AtomicInteger count = new AtomicInteger(0);
        Consumer<String> handler = e -> count.incrementAndGet();

        // Subscribe same handler twice
        bus.subscribe("dup", handler);
        bus.subscribe("dup", handler);
        bus.publish("dup", "ping");

        check("Duplicate subscribe: called twice per publish", 2, count.get());
        check("Duplicate subscribe: subscriberCount = 2", 2, bus.subscriberCount("dup"));
    }

    static void testUnsubscribeNonExistentHandler() {
        EventBus bus = new EventBus();
        boolean threw = false;
        try {
            bus.unsubscribe("ghost-topic", e -> {});   // topic doesn't exist
        } catch (Exception e) {
            threw = true;
        }
        check("Unsubscribe non-existent: no exception", false, threw);
    }

    static void testPublishToEmptyTopic() {
        EventBus bus = new EventBus();
        boolean threw = false;
        try {
            bus.publish("empty-topic", "hello");
        } catch (Exception e) {
            threw = true;
        }
        check("Publish to empty topic: no exception", false, threw);
    }

    static void testHandlerUnsubscribesDuringDispatch() {
        EventBus bus = new EventBus();
        AtomicInteger firstCount  = new AtomicInteger(0);
        AtomicInteger secondCount = new AtomicInteger(0);

        Consumer<String>[] self = new Consumer[1];
        self[0] = e -> {
            firstCount.incrementAndGet();
            bus.unsubscribe("ch", self[0]);   // remove self during dispatch
        };
        bus.subscribe("ch", self[0]);
        bus.subscribe("ch", e -> secondCount.incrementAndGet());

        bus.publish("ch", "round-1");
        bus.publish("ch", "round-2");

        check("Unsubscribe during dispatch: self called once", 1, firstCount.get());
        check("Unsubscribe during dispatch: other called twice", 2, secondCount.get());
    }

    static void testConcurrentSubscribeAndPublish() throws Exception {
        EventBus bus = new EventBus();
        AtomicInteger errors    = new AtomicInteger(0);
        AtomicInteger received  = new AtomicInteger(0);

        // Pre-subscribe so publishers have something to iterate
        for (int i = 0; i < 5; i++) {
            bus.subscribe("concurrent", e -> received.incrementAndGet());
        }

        int N = 10;
        CyclicBarrier barrier = new CyclicBarrier(N);
        Thread[] threads = new Thread[N];

        for (int i = 0; i < N; i++) {
            int idx = i;
            threads[i] = new Thread(() -> {
                try {
                    barrier.await();   // all threads start simultaneously
                    if (idx % 2 == 0) {
                        bus.subscribe("concurrent", e -> {});
                    } else {
                        bus.publish("concurrent", "e" + idx);
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                }
            });
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join(5000);

        check("Concurrent access: no exceptions", 0, errors.get());
    }

    static void testPublishAsyncAfterShutdown() throws InterruptedException {
        EventBus bus = new EventBus();
        AtomicInteger count = new AtomicInteger(0);
        bus.subscribe("s", e -> count.incrementAndGet());

        bus.publishAsync("s", "before-shutdown");
        bus.shutdown(3000);

        boolean threw = false;
        try {
            bus.publishAsync("s", "after-shutdown");  // should be silently ignored
        } catch (Exception e) {
            threw = true;
        }
        check("publishAsync after shutdown: no exception",   false, threw);
        check("publishAsync after shutdown: event not delivered to handler",
              1, count.get());
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
