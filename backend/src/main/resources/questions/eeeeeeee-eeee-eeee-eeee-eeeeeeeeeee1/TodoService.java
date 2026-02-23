import java.util.*;

/**
 * TODO management service — has 4 bugs to fix.
 * This is the ONLY file you should edit.
 */
public class TodoService {
    private final TodoRepository repository;

    public TodoService(TodoRepository repository) {
        this.repository = repository;
    }

    // ========== Bug 1: addTodo — nextId is local, not a field ==========

    /**
     * Add a new TODO item.
     * Returns the created TodoItem with a unique auto-incrementing ID.
     */
    public TodoItem addTodo(String title, String description) {
        int nextId = 1; // BUG: should be an instance field, not local variable
        String id = String.valueOf(nextId);
        nextId++;
        TodoItem item = new TodoItem(id, title, description);
        repository.addToList(item);
        repository.addToMap(id, item);
        return item;
    }

    // ========== Bug 2: markDone — uses == instead of .equals() ==========

    /**
     * Mark a TODO item as done.
     * Returns true if the item was found and marked, false otherwise.
     */
    public boolean markDone(String id) {
        for (TodoItem todo : repository.getList()) {
            if (todo.getId() == id) { // BUG: == compares references, not values
                todo.setDone(true);
                return true;
            }
        }
        return false;
    }

    // ========== Bug 3: getByStatus — ConcurrentModificationException ==========

    /**
     * Get all TODO items matching the given done status.
     * Returns a list of matching items.
     */
    public List<TodoItem> getByStatus(boolean done) {
        List<TodoItem> items = repository.getList();
        // BUG: Modifying list during iteration causes ConcurrentModificationException
        for (TodoItem item : items) {
            if (item.isDone() != done) {
                items.remove(item);
            }
        }
        return items;
    }

    // ========== Bug 4: deleteTodo — only removes from list, not map ==========

    /**
     * Delete a TODO item by ID.
     * Returns true if the item was found and deleted, false otherwise.
     */
    public boolean deleteTodo(String id) {
        TodoItem item = repository.findInMap(id);
        if (item == null) {
            return false;
        }
        repository.removeFromList(item);
        // BUG: Missing repository.removeFromMap(id);
        return true;
    }

    /**
     * Find a TODO item by ID (uses the map for fast lookup).
     */
    public TodoItem findById(String id) {
        return repository.findInMap(id);
    }

    /**
     * Get total count of TODO items (from the list).
     */
    public int count() {
        return repository.listSize();
    }
}
