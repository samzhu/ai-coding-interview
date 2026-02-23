import java.util.*;

/**
 * Order entity with state machine.
 * You may edit this file.
 */
public class Order {
    private final String id;
    private OrderStatus status;
    private final List<String> items;
    private long version;

    public Order(String id, List<String> items) {
        this.id = id;
        this.status = OrderStatus.CREATED;
        this.items = new ArrayList<>(items);
        this.version = 0;
    }

    // ========== CP1: transition — HAS 2 BUGS ==========

    /**
     * Transition to a new status.
     * Bug 1: Does not support CANCELLED transitions (only linear flow).
     * Bug 2: No idempotent check — throws exception on same-state transition.
     */
    public void transition(OrderStatus target) {
        // BUG 1: Only checks linear flow, misses CANCELLED
        OrderStatus next = switch (status) {
            case CREATED -> OrderStatus.CONFIRMED;
            case CONFIRMED -> OrderStatus.SHIPPED;
            case SHIPPED -> OrderStatus.DELIVERED;
            default -> null;
        };
        // BUG 2: No idempotent check — if target == current status, should be no-op
        if (next == null || next != target) {
            throw new IllegalStateException(
                "Cannot transition from " + status + " to " + target);
        }
        this.status = target;
    }

    // ========== CP4: version management ==========

    /**
     * Check and increment version for optimistic locking.
     * Candidates should call this in transition() for CP4.
     */
    public void checkAndIncrementVersion() {
        // TODO (CP4): Call this method in transition() to enable version tracking
        this.version++;
    }

    public String getId() { return id; }
    public OrderStatus getStatus() { return status; }
    public List<String> getItems() { return Collections.unmodifiableList(items); }
    public long getVersion() { return version; }

    @Override
    public String toString() {
        return "Order{id='" + id + "', status=" + status + ", version=" + version + "}";
    }
}
