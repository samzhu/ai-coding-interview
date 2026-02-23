import java.util.*;

/**
 * Order processing service.
 * You may edit this file.
 */
public class OrderService {
    private final OrderRepository repository;
    private final InventoryService inventoryService;
    private final EventPublisher eventPublisher;

    public OrderService(OrderRepository repository, InventoryService inventoryService,
                        EventPublisher eventPublisher) {
        this.repository = repository;
        this.inventoryService = inventoryService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Create a new order.
     */
    public Order createOrder(String orderId, List<String> items) {
        Order order = new Order(orderId, items);
        repository.save(order);
        return order;
    }

    // ========== CP2: confirmOrder — TODO ==========

    /**
     * Confirm an order: reserve inventory, transition state, publish event.
     * - Find order (throw OrderNotFoundException if not found)
     * - Reserve inventory (may throw InsufficientStockException)
     * - If reserve succeeds: transition to CONFIRMED, save, publish event
     * - If reserve fails: do NOT change state, rethrow exception
     */
    public void confirmOrder(String orderId) {
        // TODO: Implement this method
        throw new UnsupportedOperationException("confirmOrder not implemented yet");
    }

    // ========== CP3: cancelOrder — TODO ==========

    /**
     * Cancel an order with best-effort inventory release.
     * - Find order (throw OrderNotFoundException if not found)
     * - If CONFIRMED or SHIPPED: release inventory (best-effort)
     * - Transition to CANCELLED
     * - Save and publish event (with releaseFailure flag if release failed)
     */
    public void cancelOrder(String orderId) {
        // TODO: Implement this method
        throw new UnsupportedOperationException("cancelOrder not implemented yet");
    }

    /**
     * Get an order by ID.
     */
    public Order getOrder(String orderId) {
        return repository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
    }
}
