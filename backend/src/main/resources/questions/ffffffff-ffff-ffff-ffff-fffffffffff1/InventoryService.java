import java.util.*;

/**
 * Simulated inventory service.
 * DO NOT MODIFY THIS FILE.
 */
public class InventoryService {
    private boolean shouldFailReserve = false;
    private boolean shouldFailRelease = false;
    private final List<String> reservedItems = new ArrayList<>();
    private final List<String> releasedItems = new ArrayList<>();

    public void reserve(List<String> items) {
        if (shouldFailReserve) {
            throw new InsufficientStockException("Not enough stock for items: " + items);
        }
        reservedItems.addAll(items);
    }

    public void release(List<String> items) {
        if (shouldFailRelease) {
            throw new RuntimeException("Release failed: external service unavailable");
        }
        releasedItems.addAll(items);
    }

    // Test helpers
    public void setFailReserve(boolean fail) { this.shouldFailReserve = fail; }
    public void setFailRelease(boolean fail) { this.shouldFailRelease = fail; }
    public List<String> getReservedItems() { return Collections.unmodifiableList(reservedItems); }
    public List<String> getReleasedItems() { return Collections.unmodifiableList(releasedItems); }
}

class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) { super(message); }
}

class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String message) { super(message); }
}

class OptimisticLockException extends RuntimeException {
    public OptimisticLockException(String message) { super(message); }
}
