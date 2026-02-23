import java.util.List;

/**
 * Checkpoint 2: Verify parse(), filterByLevel(), and averageDuration().
 * Pure Java test runner — no external JUnit jar required.
 */
public class TestCP2 {

    static int passed = 0;
    static int failed = 0;

    static void check(boolean condition, String name) {
        if (condition) {
            System.out.println("  PASS " + name);
            passed++;
        } else {
            System.out.println("  FAIL " + name);
            failed++;
        }
    }

    static void eq(Object expected, Object actual, String name) {
        boolean ok = (expected == null) ? (actual == null) : expected.equals(actual);
        if (ok) {
            System.out.println("  PASS " + name);
            passed++;
        } else {
            System.out.println("  FAIL " + name + " [expected=" + expected + ", got=" + actual + "]");
            failed++;
        }
    }

    public static void main(String[] args) {
        System.out.println("=== CP2: Log Parsing and Analysis Tests ===");
        LogParser p = new LogParser();

        String[] lines = {
            "08:00:00 [INFO] Server started",
            "08:30:00 [WARN] High memory usage",
            "09:00:00 [ERROR] Connection timeout",
            "09:30:00 [INFO] Request processed",
            "INVALID LINE - should be skipped",
            "10:00:00 [ERROR] Disk full",
        };

        // --- parse ---
        System.out.println("\n-- parse --");
        List<LogEntry> entries = p.parse(lines);
        eq(5, entries.size(), "parse() should return 5 entries (skip invalid line)");

        if (entries.size() >= 1) {
            eq("08:00:00", entries.get(0).getTimestamp(), "First entry timestamp");
            eq("INFO", entries.get(0).getLevel(), "First entry level");
            eq("Server started", entries.get(0).getMessage(), "First entry message");
        }

        // --- filterByLevel ---
        System.out.println("\n-- filterByLevel --");
        List<LogEntry> errors = p.filterByLevel(entries, "ERROR");
        eq(2, errors.size(), "filterByLevel('ERROR') returns 2 entries");

        List<LogEntry> infos = p.filterByLevel(entries, "INFO");
        eq(2, infos.size(), "filterByLevel('INFO') returns 2 entries");

        // Case-insensitive
        List<LogEntry> errorsLower = p.filterByLevel(entries, "error");
        eq(2, errorsLower.size(), "filterByLevel is case-insensitive ('error' == 'ERROR')");

        // --- averageDuration ---
        System.out.println("\n-- averageDuration --");
        // 5 entries: 08:00, 08:30, 09:00, 09:30, 10:00
        // Gaps: 1800, 1800, 1800, 1800 -> avg = 1800.0
        double avg = p.averageDuration(entries);
        check(Math.abs(avg - 1800.0) < 0.01,
            "averageDuration for 30-min intervals = 1800.0, got " + avg);

        System.out.println();
        System.out.println("Results: " + passed + " passed, " + failed + " failed");
        System.exit(failed > 0 ? 1 : 0);
    }
}
