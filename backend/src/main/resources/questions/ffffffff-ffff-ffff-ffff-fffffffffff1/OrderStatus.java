import java.util.*;

/**
 * Order status enum with transition rules.
 * DO NOT MODIFY THIS FILE.
 */
public enum OrderStatus {
    CREATED,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    /**
     * Returns the set of valid target statuses from this status.
     * Linear flow: CREATED->CONFIRMED->SHIPPED->DELIVERED
     * Cancel: any non-DELIVERED status -> CANCELLED
     */
    public Set<OrderStatus> validTransitions() {
        return switch (this) {
            case CREATED -> Set.of(CONFIRMED, CANCELLED);
            case CONFIRMED -> Set.of(SHIPPED, CANCELLED);
            case SHIPPED -> Set.of(DELIVERED, CANCELLED);
            case DELIVERED -> Set.of();
            case CANCELLED -> Set.of();
        };
    }
}
