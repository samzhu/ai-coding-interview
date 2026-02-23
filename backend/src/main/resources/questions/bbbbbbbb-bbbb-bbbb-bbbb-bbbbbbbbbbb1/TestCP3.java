import java.util.*;

public class TestCP3 {

    static int passed = 0, failed = 0;

    public static void main(String[] args) {

        // --- Test 1: Empty class ---
        GradeManager empty = new GradeManager();
        checkApprox("Empty: average = 0.0",    0.0, empty.getAverageScore(), 0.001);
        check("Empty: top 5 returns []",        0,   empty.getTopStudents(5).size());
        check("Empty: distribution is empty",   0,   empty.getGradeDistribution().size());
        check("Empty: filterByGrade returns []", 0,  empty.filterByGrade(new String("A")).size());

        // --- Test 2: Single student ---
        GradeManager single = new GradeManager();
        single.addStudent(new Student("Solo", 88));
        checkApprox("Single: average = 88.0",  88.0, single.getAverageScore(), 0.001);
        check("Single: top 1 size = 1",  1, single.getTopStudents(1).size());
        check("Single: top 10 size = 1", 1, single.getTopStudents(10).size());

        // --- Test 3: Exact grade boundary scores ---
        GradeManager boundary = new GradeManager();
        boundary.addStudent(new Student("A-edge", 90));
        boundary.addStudent(new Student("B-edge", 80));
        boundary.addStudent(new Student("C-edge", 70));
        boundary.addStudent(new Student("D-edge", 60));
        boundary.addStudent(new Student("F-edge", 59));
        Map<String, Integer> dist = boundary.getGradeDistribution();
        check("Boundary A count = 1", 1, dist.getOrDefault("A", 0));
        check("Boundary B count = 1", 1, dist.getOrDefault("B", 0));
        check("Boundary C count = 1", 1, dist.getOrDefault("C", 0));
        check("Boundary D count = 1", 1, dist.getOrDefault("D", 0));
        check("Boundary F count = 1", 1, dist.getOrDefault("F", 0));

        // --- Test 4: Tied scores ---
        GradeManager ties = new GradeManager();
        ties.addStudent(new Student("T1", 100));
        ties.addStudent(new Student("T2", 100));
        ties.addStudent(new Student("T3",  50));
        List<Student> top2 = ties.getTopStudents(2);
        check("Ties: top 2 size = 2",      2,   top2.size());
        check("Ties: rank 1 score = 100",  100, top2.get(0).score());
        check("Ties: rank 2 score = 100",  100, top2.get(1).score());

        // --- Test 5: Filter on absent grade ---
        GradeManager allA = new GradeManager();
        allA.addStudent(new Student("Perfect", 100));
        check("Filter F in all-A class = []", 0, allA.filterByGrade(new String("F")).size());

        // --- Test 6: Non-integer average ---
        GradeManager nonInt = new GradeManager();
        nonInt.addStudent(new Student("P1", 100));
        nonInt.addStudent(new Student("P2",  99));
        nonInt.addStudent(new Student("P3",  98));
        // Average: 297 / 3 = 99.0 — integer and double give same result here
        // Use a case where integer truncation matters: (100 + 99) / 2 = 99 (int) vs 99.5 (double)
        GradeManager nonInt2 = new GradeManager();
        nonInt2.addStudent(new Student("X", 100));
        nonInt2.addStudent(new Student("Y",  99));
        checkApprox("Non-integer average = 99.5", 99.5, nonInt2.getAverageScore(), 0.001);

        System.out.println("\n=== CP3 Results: " + passed + " passed, " + failed + " failed ===");
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
