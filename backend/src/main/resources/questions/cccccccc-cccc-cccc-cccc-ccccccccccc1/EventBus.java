import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * A topic-based event bus for decoupled, asynchronous communication.
 *
 * Common AI-generated pitfalls in this file:
 * 1. publish() iterates the subscriber list without a defensive copy.
 *    If a handler calls subscribe() or unsubscribe() during dispatch,
 *    ArrayList throws ConcurrentModificationException.
 *
 * 2. HashMap is not thread-safe for concurrent subscribe + publish
 *    from multiple threads.
 */
public class EventBus {

    // BUG (secondary): HashMap is not thread-safe.
    // Under concurrent subscribe + publish, structural modifications to the map
    // can cause infinite loops or data corruption.
    // Fix: use ConcurrentHashMap.
    private final Map<String, List<Consumer<String>>> subscribers = new HashMap<>();

    /**
     * Subscribes a handler to the given topic.
     * The same handler may be subscribed multiple times.
     */
    public synchronized void subscribe(String topic, Consumer<String> handler) {
        subscribers.computeIfAbsent(topic, k -> new ArrayList<>()).add(handler);
    }

    /**
     * Removes the first occurrence of the given handler from the topic.
     * Does nothing if the handler is not subscribed.
     */
    public synchronized void unsubscribe(String topic, Consumer<String> handler) {
        List<Consumer<String>> handlers = subscribers.get(topic);
        if (handlers != null) {
            handlers.remove(handler);
        }
    }

    /**
     * Publishes an event synchronously to all subscribers of the topic.
     *
     * BUG: Iterates the live subscriber list without a defensive copy.
     * If any handler modifies the subscription list (e.g., calls unsubscribe
     * or subscribe on this bus), Java's ArrayList iterator will throw
     * ConcurrentModificationException.
     *
     * Fix option A (simple): snapshot the list before iterating
     *   List<Consumer<String>> snapshot = new ArrayList<>(handlers);
     *
     * Fix option B (structural): store subscribers in CopyOnWriteArrayList,
     *   which produces a fresh copy on every write, making iteration always safe.
     */
    public void publish(String topic, String event) {
        List<Consumer<String>> handlers = subscribers.get(topic);
        if (handlers == null) return;
        for (Consumer<String> handler : handlers) {   // BUG: no defensive copy
            handler.accept(event);
        }
    }

    /**
     * Returns the number of subscribers currently registered for the topic.
     * Returns 0 if the topic does not exist or has no subscribers.
     *
     * TODO CP2: Implement this method.
     */
    public int subscriberCount(String topic) {
        return 0;
    }

    /**
     * Returns all topics that currently have at least one subscriber.
     *
     * TODO CP2: Implement this method.
     */
    public Set<String> activeTopics() {
        return new HashSet<>();
    }

    /**
     * Publishes an event asynchronously using an internal thread pool.
     * The caller returns immediately; handlers run on a background thread.
     * If the bus has been shut down, this call is silently ignored.
     *
     * TODO CP2: Implement this method.
     * Hint: use an ExecutorService field; submit(() -> publish(topic, event)).
     */
    public void publishAsync(String topic, String event) {
        // TODO
    }

    /**
     * Gracefully shuts down the event bus.
     * Stops accepting new async tasks and waits up to timeoutMs milliseconds
     * for in-flight tasks to complete.
     *
     * TODO CP2: Implement this method.
     * Hint: executor.shutdown() + executor.awaitTermination(timeoutMs, MILLISECONDS)
     */
    public void shutdown(long timeoutMs) throws InterruptedException {
        // TODO
    }
}
