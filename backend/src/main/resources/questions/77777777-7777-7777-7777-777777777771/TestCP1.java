/**
 * Checkpoint 1: Verify BOGO bug fix.
 * Pure Java test runner — no external JUnit jar required.
 *
 * BOGO rule: free items = quantity / 2 (integer division, rounds down)
 * paid items = quantity - freeItems
 * total = price * paidItems
 */
public class TestCP1 {

    static int passed = 0;
    static int failed = 0;

    static void eq(double expected, double actual, String name) {
        if (Math.abs(expected - actual) < 0.001) {
            System.out.println("  PASS " + name);
            passed++;
        } else {
            System.out.println("  FAIL " + name + " [expected=" + expected + ", got=" + actual + "]");
            failed++;
        }
    }

    public static void main(String[] args) {
        System.out.println("=== CP1: BOGO Bug Fix Tests ===");
        DiscountEngine e = new DiscountEngine();

        // price=10, qty=0: free=0, pay=0, total=0.0
        eq(0.0,  e.calculateBogo(10.0, 0), "qty=0: total is 0.0");

        // price=10, qty=1: free=0, pay=1, total=10.0
        eq(10.0, e.calculateBogo(10.0, 1), "qty=1: no BOGO, pay full price 10.0");

        // price=10, qty=2: free=1, pay=1, total=10.0
        eq(10.0, e.calculateBogo(10.0, 2), "qty=2: 1 free, pay for 1 -> 10.0");

        // price=10, qty=3: free=1, pay=2, total=20.0
        eq(20.0, e.calculateBogo(10.0, 3), "qty=3: 1 free, pay for 2 -> 20.0");

        // price=10, qty=4: free=2, pay=2, total=20.0
        eq(20.0, e.calculateBogo(10.0, 4), "qty=4: 2 free, pay for 2 -> 20.0");

        // price=10, qty=5: free=2, pay=3, total=30.0
        eq(30.0, e.calculateBogo(10.0, 5), "qty=5: 2 free, pay for 3 -> 30.0");

        // price=10, qty=6: free=3, pay=3, total=30.0
        eq(30.0, e.calculateBogo(10.0, 6), "qty=6: 3 free, pay for 3 -> 30.0");

        // price=5, qty=4: free=2, pay=2, total=10.0
        eq(10.0, e.calculateBogo(5.0, 4), "price=5, qty=4: 2 free, pay 2 -> 10.0");

        System.out.println();
        System.out.println("Results: " + passed + " passed, " + failed + " failed");
        System.exit(failed > 0 ? 1 : 0);
    }
}
