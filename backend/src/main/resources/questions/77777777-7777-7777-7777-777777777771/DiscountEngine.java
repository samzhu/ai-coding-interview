/**
 * Discount calculation engine for an e-commerce platform.
 *
 * Supports three discount types: PERCENTAGE, BOGO, and FLAT.
 * All monetary results are rounded to 2 decimal places.
 */
public class DiscountEngine {

    /**
     * Calculate total price after BOGO (Buy One Get One) discount.
     *
     * BOGO rule: for every 2 items purchased, 1 is free.
     * Free item count = quantity / 2  (integer division, rounds down)
     *
     * Examples:
     *   price=10, quantity=2 -> 1 free -> total = 10.0
     *   price=10, quantity=3 -> 1 free -> total = 20.0
     *   price=10, quantity=4 -> 2 free -> total = 20.0
     *   price=10, quantity=5 -> 2 free -> total = 30.0
     *
     * BUG: the free item count calculation is wrong.
     * Currently returns incorrect values for quantities >= 2.
     *
     * @param price    unit price (>= 0)
     * @param quantity number of items (>= 0)
     * @return total price after BOGO discount, rounded to 2 decimal places
     */
    public double calculateBogo(double price, int quantity) {
        // BUG: freeItems should be quantity / 2 (integer division)
        // Current code uses wrong formula
        int freeItems = quantity / 3;  // BUG: should be quantity / 2
        int paidItems = quantity - freeItems;
        double total = price * paidItems;
        return roundToTwoDecimalPlaces(total);
    }

    /**
     * Calculate total price after PERCENTAGE discount.
     *
     * Formula: price * quantity * (1 - percent / 100.0)
     * Result rounded to 2 decimal places.
     *
     * @param price    unit price (>= 0)
     * @param quantity number of items (>= 0)
     * @param percent  discount percentage (0-100)
     * @return total price after percentage discount
     */
    public double calculatePercentage(double price, int quantity, double percent) {
        // TODO: Implement percentage discount
        // double total = price * quantity * (1 - percent / 100.0);
        // return roundToTwoDecimalPlaces(total);
        return 0.0;
    }

    /**
     * Calculate total price after FLAT discount per item.
     *
     * Each item's price is reduced by flatOff, but cannot go below 0.
     * Formula: Math.max(0, price - flatOff) * quantity
     * Result rounded to 2 decimal places.
     *
     * @param price    unit price (>= 0)
     * @param quantity number of items (>= 0)
     * @param flatOff  fixed discount per item (>= 0)
     * @return total price after flat discount
     */
    public double calculateFlat(double price, int quantity, double flatOff) {
        // TODO: Implement flat discount
        // double effectivePrice = Math.max(0, price - flatOff);
        // return roundToTwoDecimalPlaces(effectivePrice * quantity);
        return 0.0;
    }

    private double roundToTwoDecimalPlaces(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
