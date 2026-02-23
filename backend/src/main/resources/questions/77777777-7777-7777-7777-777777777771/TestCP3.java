/**
 * Checkpoint 3: Edge cases for all discount types.
 * Pure Java test runner — no external JUnit jar required.
 */
public class TestCP3 {

    static int passed = 0;
    static int failed = 0;

    static void eq(double expected, double actual, String name) {
        if (Math.abs(expected - actual) < 0.01) {
            System.out.println("  PASS " + name);
            passed++;
        } else {
            System.out.println("  FAIL " + name + " [expected=" + expected + ", got=" + actual + "]");
            failed++;
        }
    }

    public static void main(String[] args) {
        System.out.println("=== CP3: Edge Cases Tests ===");
        DiscountEngine e = new DiscountEngine();

        System.out.println("\n-- Zero quantity --");
        eq(0.0, e.calculateBogo(10.0, 0),       "BOGO qty=0: total 0.0");
        eq(0.0, e.calculatePercentage(10.0, 0, 20.0), "PERCENTAGE qty=0: total 0.0");
        eq(0.0, e.calculateFlat(10.0, 0, 5.0),  "FLAT qty=0: total 0.0");

        System.out.println("\n-- 100% discount --");
        eq(0.0, e.calculatePercentage(100.0, 5, 100.0),
            "PERCENTAGE 100%: total is 0.0");

        System.out.println("\n-- FLAT exceeds price --");
        eq(0.0, e.calculateFlat(3.0, 4, 10.0),
            "FLAT $10 off $3 item: price cannot go negative, total is 0.0");

        System.out.println("\n-- BOGO edge cases --");
        eq(10.0, e.calculateBogo(10.0, 1),
            "BOGO qty=1: no free item, pay full price");
        eq(10.0, e.calculateBogo(10.0, 2),
            "BOGO qty=2: 1 free, pay for 1");

        System.out.println("\n-- Float precision --");
        // 0.1 + 0.2 precision issue: price=0.1, qty=3, 0% -> 0.30
        eq(0.30, e.calculatePercentage(0.1, 3, 0.0),
            "Float precision: 0.1*3 = 0.30 (not 0.30000000000000004)");

        System.out.println();
        System.out.println("Results: " + passed + " passed, " + failed + " failed");
        System.exit(failed > 0 ? 1 : 0);
    }
}
