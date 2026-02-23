/**
 * Checkpoint 2: Verify PERCENTAGE and FLAT discount implementations.
 * Pure Java test runner — no external JUnit jar required.
 */
public class TestCP2 {

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
        System.out.println("=== CP2: PERCENTAGE and FLAT Discount Tests ===");
        DiscountEngine e = new DiscountEngine();

        // --- PERCENTAGE ---
        System.out.println("\n-- calculatePercentage --");

        // price=100, qty=1, 10% off -> 90.0
        eq(90.0, e.calculatePercentage(100.0, 1, 10.0),
            "price=100, qty=1, 10% off -> 90.0");

        // price=100, qty=2, 10% off -> 180.0
        eq(180.0, e.calculatePercentage(100.0, 2, 10.0),
            "price=100, qty=2, 10% off -> 180.0");

        // price=50, qty=3, 20% off -> 120.0
        eq(120.0, e.calculatePercentage(50.0, 3, 20.0),
            "price=50, qty=3, 20% off -> 120.0");

        // price=10, qty=1, 0% off -> 10.0
        eq(10.0, e.calculatePercentage(10.0, 1, 0.0),
            "0% discount: full price");

        // --- FLAT ---
        System.out.println("\n-- calculateFlat --");

        // price=20, qty=2, flat=5 -> (20-5)*2 = 30.0
        eq(30.0, e.calculateFlat(20.0, 2, 5.0),
            "price=20, qty=2, flat=5 -> 30.0");

        // price=10, qty=3, flat=3 -> (10-3)*3 = 21.0
        eq(21.0, e.calculateFlat(10.0, 3, 3.0),
            "price=10, qty=3, flat=3 -> 21.0");

        // price=5, qty=2, flat=5 -> max(0, 5-5)*2 = 0.0
        eq(0.0, e.calculateFlat(5.0, 2, 5.0),
            "flat equals price: total is 0.0");

        // price=5, qty=2, flat=10 -> max(0, 5-10)*2 = 0.0
        eq(0.0, e.calculateFlat(5.0, 2, 10.0),
            "flat exceeds price: total is 0.0 (no negative items)");

        System.out.println();
        System.out.println("Results: " + passed + " passed, " + failed + " failed");
        System.exit(failed > 0 ? 1 : 0);
    }
}
