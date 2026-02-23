public class TestCP1 {

    static int passed = 0, failed = 0;

    public static void main(String[] args) {
        GradeManager manager = new GradeManager();
        manager.addStudent(new Student("Alice",   95));  // A
        manager.addStudent(new Student("Bob",     85));  // B
        manager.addStudent(new Student("Charlie", 75));  // C
        manager.addStudent(new Student("Diana",   65));  // D
        manager.addStudent(new Student("Eve",     50));  // F

        // Use new String(...) to bypass String Pool interning.
        // This is exactly the scenario where == silently returns false
        // even though the content matches.
        check("Filter A → 1 student",  1, manager.filterByGrade(new String("A")).size());
        check("Filter B → 1 student",  1, manager.filterByGrade(new String("B")).size());
        check("Filter C → 1 student",  1, manager.filterByGrade(new String("C")).size());
        check("Filter D → 1 student",  1, manager.filterByGrade(new String("D")).size());
        check("Filter F → 1 student",  1, manager.filterByGrade(new String("F")).size());

        // Verify grade boundary logic is correct
        check("Score 100 → A", "A", manager.getGrade(100));
        check("Score  90 → A", "A", manager.getGrade(90));
        check("Score  89 → B", "B", manager.getGrade(89));
        check("Score  80 → B", "B", manager.getGrade(80));
        check("Score  79 → C", "C", manager.getGrade(79));
        check("Score  70 → C", "C", manager.getGrade(70));
        check("Score  69 → D", "D", manager.getGrade(69));
        check("Score  60 → D", "D", manager.getGrade(60));
        check("Score  59 → F", "F", manager.getGrade(59));
        check("Score   0 → F", "F", manager.getGrade(0));

        System.out.println("\n=== CP1 Results: " + passed + " passed, " + failed + " failed ===");
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
}
