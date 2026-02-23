import java.util.*;

public class TestCP2 {

    static int passed = 0, failed = 0;

    public static void main(String[] args) {
        GradeManager manager = new GradeManager();
        manager.addStudent(new Student("Alice",   95));  // A
        manager.addStudent(new Student("Bob",     85));  // B
        manager.addStudent(new Student("Charlie", 75));  // C
        manager.addStudent(new Student("Diana",   65));  // D
        manager.addStudent(new Student("Eve",     50));  // F

        // Average: (95 + 85 + 75 + 65 + 50) / 5 = 370 / 5 = 74.0
        checkApprox("Average score = 74.0", 74.0, manager.getAverageScore(), 0.001);

        // Top 3 — sorted by score descending
        List<Student> top3 = manager.getTopStudents(3);
        check("Top 3 size = 3",      3,         top3.size());
        check("Rank 1 = Alice",      "Alice",   top3.get(0).name());
        check("Rank 2 = Bob",        "Bob",     top3.get(1).name());
        check("Rank 3 = Charlie",    "Charlie", top3.get(2).name());

        // n exceeds total — should return all
        check("getTopStudents(10) returns all 5", 5, manager.getTopStudents(10).size());
        check("getTopStudents(0)  returns 0",     0, manager.getTopStudents(0).size());

        // Grade distribution
        Map<String, Integer> dist = manager.getGradeDistribution();
        check("Distribution A = 1", 1, dist.getOrDefault("A", 0));
        check("Distribution B = 1", 1, dist.getOrDefault("B", 0));
        check("Distribution C = 1", 1, dist.getOrDefault("C", 0));
        check("Distribution D = 1", 1, dist.getOrDefault("D", 0));
        check("Distribution F = 1", 1, dist.getOrDefault("F", 0));
        check("Distribution size = 5", 5, dist.size());

        System.out.println("\n=== CP2 Results: " + passed + " passed, " + failed + " failed ===");
        if (failed > 0) System.exit(1);
    }

    static void check(String name, int expected, int actual) {
        if (expected == actual) {
            System.out.println("[PASS] " + name);
            passed++;
        } else {
            System.out.println("[FAIL] " + name + " — expected: " + expected + ", got: " + actual);
            failed++;
        }
    }

    static void check(String name, String expected, String actual) {
        if (expected.equals(actual)) {
            System.out.println("[PASS] " + name);
            passed++;
        } else {
            System.out.println("[FAIL] " + name + " — expected: " + expected + ", got: " + actual);
            failed++;
        }
    }

    static void checkApprox(String name, double expected, double actual, double delta) {
        if (Math.abs(expected - actual) <= delta) {
            System.out.println("[PASS] " + name);
            passed++;
        } else {
            System.out.println("[FAIL] " + name + " — expected: " + expected + ", got: " + actual);
            failed++;
        }
    }
}
