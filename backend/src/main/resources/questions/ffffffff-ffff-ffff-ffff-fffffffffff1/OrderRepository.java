import java.util.*;

/**
 * Simulated order repository with version-checking save.
 * DO NOT MODIFY THIS FILE.
 */
public class OrderRepository {
    private final Map<String, Order> store = new LinkedHashMap<>();
    private final Map<String, Long> versionStore = new HashMap<>();

    public Optional<Order> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    /**
     * Save an order. If the stored version doesn't match the order's version,
     * throws OptimisticLockException (simulating @Version behavior).
     */
    public Order save(Order order) {
        Long storedVersion = versionStore.get(order.getId());
        if (storedVersion != null && storedVersion != order.getVersion()) {
            throw new OptimisticLockException(
                "Order " + order.getId() + " was modified concurrently. " +
                "Expected version " + order.getVersion() + " but stored version is " + storedVersion);
        }
        store.put(order.getId(), order);
        versionStore.put(order.getId(), order.getVersion());
        return order;
    }

    // Test helper: seed an order directly (bypasses version check)
    public void seed(Order order) {
        store.put(order.getId(), order);
        versionStore.put(order.getId(), order.getVersion());
    }
}
