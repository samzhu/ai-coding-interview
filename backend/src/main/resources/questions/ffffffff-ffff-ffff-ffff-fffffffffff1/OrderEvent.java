/**
 * Order event record for event publishing.
 * DO NOT MODIFY THIS FILE.
 */
public record OrderEvent(String orderId, String type, boolean releaseFailure) {

    public static OrderEvent confirmed(String orderId) {
        return new OrderEvent(orderId, "CONFIRMED", false);
    }

    public static OrderEvent cancelled(String orderId, boolean releaseFailure) {
        return new OrderEvent(orderId, "CANCELLED", releaseFailure);
    }
}
