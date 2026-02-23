/**
 * TODO item model.
 * DO NOT MODIFY THIS FILE.
 */
public class TodoItem {
    private final String id;
    private final String title;
    private final String description;
    private boolean done;

    public TodoItem(String id, String title, String description) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.done = false;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public boolean isDone() { return done; }
    public void setDone(boolean done) { this.done = done; }

    @Override
    public String toString() {
        return "TodoItem{id='" + id + "', title='" + title + "', done=" + done + "}";
    }
}
