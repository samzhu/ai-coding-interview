import java.util.List;

/**
 * Checkpoint 3: Edge cases for the log parser.
 * Pure Java test runner — no external JUnit jar required.
 */
public class TestCP3 {

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
        System.out.println("=== CP3: Edge Cases Tests ===");
        LogParser p = new LogParser();

        // --- Empty input ---
        System.out.println("\n-- Empty input --");
        List<LogEntry> empty = p.parse(new String[]{});
        eq(0, empty.size(), "parse([]) returns empty list");
        check(Math.abs(p.averageDuration(empty) - 0.0) < 0.001,
            "averageDuration([]) = 0.0");

        // --- Single entry ---
        System.out.println("\n-- Single entry --");
        List<LogEntry> single = p.parse(new String[]{"12:00:00 [INFO] only one"});
        eq(1, single.size(), "parse() with 1 valid line returns 1 entry");
        check(Math.abs(p.averageDuration(single) - 0.0) < 0.001,
            "averageDuration with 1 entry = 0.0");

        // --- Invalid lines skipped ---
        System.out.println("\n-- Invalid lines skipped --");
        String[] mixed = {
            "00:00:01 [INFO] valid",
            "not a log line",
            "",
            "also bad",
            "00:01:00 [ERROR] also valid",
        };
        List<LogEntry> parsed = p.parse(mixed);
        eq(2, parsed.size(), "parse() skips 3 invalid lines, returns 2");

        // --- Case-insensitive filter ---
        System.out.println("\n-- Case-insensitive filter --");
        List<LogEntry> errUpper = p.filterByLevel(parsed, "ERROR");
        List<LogEntry> errLower = p.filterByLevel(parsed, "error");
        eq(errUpper.size(), errLower.size(), "filterByLevel is case-insensitive");

        // --- Same timestamp: gap = 0 ---
        System.out.println("\n-- Same timestamp --");
        String[] sameTime = {
            "10:00:00 [INFO] first",
            "10:00:00 [INFO] second",
        };
        List<LogEntry> sameEntries = p.parse(sameTime);
        check(Math.abs(p.averageDuration(sameEntries) - 0.0) < 0.001,
            "Two entries at same time: averageDuration = 0.0");

        // --- Integer division trap ---
        System.out.println("\n-- Double precision (no integer division) --");
        // 3 entries with gaps of 1 second each -> avg = 1.0
        String[] precise = {
            "10:00:00 [INFO] a",
            "10:00:01 [INFO] b",
            "10:00:02 [INFO] c",
        };
        List<LogEntry> preciseEntries = p.parse(precise);
        double avgPrecise = p.averageDuration(preciseEntries);
        check(Math.abs(avgPrecise - 1.0) < 0.001,
            "averageDuration with 1-second gaps = 1.0 (not 0 from int division)");

        System.out.println();
        System.out.println("Results: " + passed + " passed, " + failed + " failed");
        System.exit(failed > 0 ? 1 : 0);
    }
}
