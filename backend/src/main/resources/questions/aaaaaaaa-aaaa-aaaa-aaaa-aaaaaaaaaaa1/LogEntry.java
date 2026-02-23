/**
 * Represents a single parsed log entry. Do not modify this file.
 */
public class LogEntry {
    private final String timestamp;
    private final String level;
    private final String message;

    public LogEntry(String timestamp, String level, String message) {
        this.timestamp = timestamp;
        this.level = level;
        this.message = message;
    }

    public String getTimestamp() { return timestamp; }
    public String getLevel()     { return level; }
    public String getMessage()   { return message; }

    @Override
    public String toString() {
        return timestamp + " [" + level + "] " + message;
    }
}
