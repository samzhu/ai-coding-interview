import java.util.ArrayList;
import java.util.List;

/**
 * Log parser and analyzer for timestamped log entries.
 *
 * Log format: "HH:MM:SS [LEVEL] message"
 * Example:    "14:30:45 [ERROR] Connection timeout"
 */
public class LogParser {

    /**
     * Parse timestamp string "HH:MM:SS" into total seconds since midnight.
     *
     * @param timestamp in format "HH:MM:SS"
     * @return total seconds (0-86399)
     */
    public int parseToSeconds(String timestamp) {
        String[] parts = timestamp.split(":");
        int h = Integer.parseInt(parts[0]);
        int m = Integer.parseInt(parts[1]);
        int s = Integer.parseInt(parts[2]);
        return h * 3600 + m * 60 + s;
    }

    /**
     * Sort log entries by timestamp in ascending order.
     *
     * BUG: Currently uses String.compareTo() for timestamp comparison.
     * String comparison is lexicographic. For HH:MM:SS format this usually
     * works, but it should use parseToSeconds() for correct numeric comparison.
     *
     * Fix: Use Integer.compare(parseToSeconds(a), parseToSeconds(b))
     * instead of a.getTimestamp().compareTo(b.getTimestamp())
     *
     * @param entries list of log entries to sort (in-place)
     */
    public void sortByTime(List<LogEntry> entries) {
        // BUG: string comparison instead of numeric comparison
        entries.sort((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()));
    }

    /**
     * Parse an array of log lines into LogEntry objects.
     *
     * Each line must match: "HH:MM:SS [LEVEL] message"
     * Skip any lines that don't match this format.
     *
     * TODO: Implement this method.
     *
     * @param lines array of raw log strings
     * @return list of parsed LogEntry objects
     */
    public List<LogEntry> parse(String[] lines) {
        // TODO: Implement parse()
        // For each line:
        //   1. Split on space: parts[0]=timestamp, parts[1]=[LEVEL], parts[2..]=message
        //   2. Validate format (parts.length >= 3, parts[1] starts with '[' and ends with ']')
        //   3. Extract level from "[LEVEL]" -> "LEVEL"
        //   4. Join remaining parts as message
        //   5. Create LogEntry and add to result
        //   6. Skip (continue) on any format error
        return new ArrayList<>();
    }

    /**
     * Filter log entries by log level (case-insensitive).
     *
     * TODO: Implement this method.
     *
     * @param entries list of log entries
     * @param level   target level (e.g. "ERROR", "WARN", "INFO")
     * @return filtered list containing only entries with matching level
     */
    public List<LogEntry> filterByLevel(List<LogEntry> entries, String level) {
        // TODO: Implement filterByLevel()
        // Return entries where entry.getLevel().equalsIgnoreCase(level)
        return new ArrayList<>();
    }

    /**
     * Calculate the average duration (in seconds) between consecutive log entries.
     *
     * Steps:
     * 1. Sort entries by time using sortByTime()
     * 2. Calculate time difference between each consecutive pair
     * 3. Return average of all differences
     * 4. Return 0.0 if fewer than 2 entries
     *
     * TODO: Implement this method.
     *
     * IMPORTANT: Use (double) division to avoid integer truncation!
     *
     * @param entries list of log entries
     * @return average gap in seconds (0.0 if fewer than 2 entries)
     */
    public double averageDuration(List<LogEntry> entries) {
        // TODO: Implement averageDuration()
        // BUG TRAP: Do NOT use int division: totalGap / count
        // Use: (double) totalGap / count
        return 0.0;
    }
}
