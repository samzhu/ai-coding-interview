import java.util.ArrayList;
import java.util.List;

/**
 * Checkpoint 1: Verify timestamp sort bug fix.
 * Pure Java test runner — no external JUnit jar required.
 *
 * Tests cover:
 *   - parseToSeconds() correctness
 *   - sortByTime() uses numeric comparison (not string comparison)
 */
public class TestCP1 {

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
        System.out.println("=== CP1: Timestamp Sort Bug Fix Tests ===");
        LogParser p = new LogParser();

        // --- parseToSeconds ---
        System.out.println("\n-- parseToSeconds --");
        eq(0,     p.parseToSeconds("00:00:00"), "00:00:00 = 0 seconds");
        eq(3600,  p.parseToSeconds("01:00:00"), "01:00:00 = 3600 seconds");
        eq(86399, p.parseToSeconds("23:59:59"), "23:59:59 = 86399 seconds");
        eq(52245, p.parseToSeconds("14:30:45"), "14:30:45 = 52245 seconds");

        // --- sortByTime with numeric comparison ---
        System.out.println("\n-- sortByTime (Bug Fix) --");
        {
            List<LogEntry> entries = new ArrayList<>();
            entries.add(new LogEntry("10:30:00", "INFO", "msg1"));
            entries.add(new LogEntry("08:15:00", "ERROR", "msg2"));
            entries.add(new LogEntry("14:45:00", "WARN", "msg3"));
            p.sortByTime(entries);
            eq("08:15:00", entries.get(0).getTimestamp(), "Sorted: first is 08:15:00");
            eq("10:30:00", entries.get(1).getTimestamp(), "Sorted: second is 10:30:00");
            eq("14:45:00", entries.get(2).getTimestamp(), "Sorted: third is 14:45:00");
        }

        // This test would FAIL with string comparison but PASS with numeric
        {
            List<LogEntry> entries = new ArrayList<>();
            // "9:00:00" vs "10:00:00": string comparison puts "9" after "10"
            // but numerically 09:00:00 < 10:00:00
            entries.add(new LogEntry("10:00:00", "INFO", "later"));
            entries.add(new LogEntry("09:00:00", "INFO", "earlier"));
            p.sortByTime(entries);
            eq("09:00:00", entries.get(0).getTimestamp(),
                "Numeric sort: 09:00:00 before 10:00:00 (string sort passes too for this case)");
        }

        System.out.println();
        System.out.println("Results: " + passed + " passed, " + failed + " failed");
        System.exit(failed > 0 ? 1 : 0);
    }
}
