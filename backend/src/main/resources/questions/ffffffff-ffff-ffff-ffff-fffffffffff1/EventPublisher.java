import java.util.*;

/**
 * Simulated event publisher (like ApplicationEventPublisher).
 * DO NOT MODIFY THIS FILE.
 */
public class EventPublisher {
    private final List<OrderEvent> publishedEvents = new ArrayList<>();

    public void publish(OrderEvent event) {
        publishedEvents.add(event);
    }

    public List<OrderEvent> getPublishedEvents() {
        return Collections.unmodifiableList(publishedEvents);
    }

    public void clear() {
        publishedEvents.clear();
    }
}
