import java.util.*;

/**
 * Simulated repository — stores TodoItems.
 * DO NOT MODIFY THIS FILE.
 */
public class TodoRepository {
    private final List<TodoItem> todoList = new ArrayList<>();
    private final Map<String, TodoItem> todoMap = new HashMap<>();

    public void addToList(TodoItem item) {
        todoList.add(item);
    }

    public void addToMap(String id, TodoItem item) {
        todoMap.put(id, item);
    }

    public List<TodoItem> getList() {
        return todoList;
    }

    public Map<String, TodoItem> getMap() {
        return todoMap;
    }

    public TodoItem findInMap(String id) {
        return todoMap.get(id);
    }

    public boolean removeFromList(TodoItem item) {
        return todoList.remove(item);
    }

    public TodoItem removeFromMap(String id) {
        return todoMap.remove(id);
    }

    public int listSize() {
        return todoList.size();
    }

    public int mapSize() {
        return todoMap.size();
    }
}
