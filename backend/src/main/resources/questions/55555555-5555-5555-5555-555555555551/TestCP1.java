/**
 * Checkpoint 1: Verify Game class bug fixes.
 * Pure Java test runner — no external JUnit jar required.
 *
 * Tests cover:
 *   Bug 1 — Duplicate guess must NOT deduct an extra life
 *   Bug 2 — isGameWon() must return true when all letters are revealed, not when lives == 0
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
        System.out.println("=== CP1: Game Bug Fix Tests ===");

        // --- Basic guess mechanics ---
        {
            Game g = new Game("hello", GameConfig.DEFAULT_MAX_LIVES);
            int before = g.getRemainingLives();
            g.processGuess('h');
            eq(before, g.getRemainingLives(), "Correct guess does not deduct a life");
        }
        {
            Game g = new Game("hello", GameConfig.DEFAULT_MAX_LIVES);
            int before = g.getRemainingLives();
            g.processGuess('z');
            eq(before - 1, g.getRemainingLives(), "Wrong guess deducts one life");
        }

        // --- Bug 1: Duplicate guess should NOT deduct a life ---
        {
            Game g = new Game("hello", GameConfig.DEFAULT_MAX_LIVES);
            g.processGuess('z');                     // wrong, costs 1 life
            int livesAfterFirst = g.getRemainingLives();
            g.processGuess('z');                     // duplicate — must NOT cost another life
            eq(livesAfterFirst, g.getRemainingLives(),
                "Duplicate wrong guess (Bug 1 fix): must not deduct another life");
        }
        {
            Game g = new Game("hello", GameConfig.DEFAULT_MAX_LIVES);
            g.processGuess('h');                     // correct
            boolean result = g.processGuess('h');    // duplicate correct
            check(result, "Duplicate correct guess still returns true");
        }

        // --- Bug 2: Win condition should check all letters guessed, not lives == 0 ---
        {
            Game g = new Game("hello", GameConfig.DEFAULT_MAX_LIVES);
            check(!g.isGameWon(), "Game is NOT won at start");
            g.processGuess('h');
            g.processGuess('e');
            g.processGuess('l');
            g.processGuess('o');
            check(g.isGameWon(),
                "Game IS won after all unique letters guessed (Bug 2 fix)");
        }
        {
            Game g = new Game("hello", GameConfig.DEFAULT_MAX_LIVES);
            g.processGuess('h');
            g.processGuess('e');
            check(!g.isGameWon(), "Game is NOT won when only some letters guessed");
        }

        // --- Loss condition ---
        {
            Game g = new Game("ab", 2);
            g.processGuess('z');
            g.processGuess('x');
            check(g.isGameLost(), "Game is lost when lives reach zero");
        }

        // --- getMaskedWord ---
        {
            Game g = new Game("hello", GameConfig.DEFAULT_MAX_LIVES);
            eq("_____", g.getMaskedWord(), "All letters masked initially");
            g.processGuess('h');
            eq("h____", g.getMaskedWord(), "Masked word shows guessed letters");
            g.processGuess('l');
            eq("h_ll_", g.getMaskedWord(), "Masked word shows multiple occurrences");
        }

        // --- Case insensitivity ---
        {
            Game g = new Game("hello", GameConfig.DEFAULT_MAX_LIVES);
            g.processGuess('H');    // uppercase
            eq("h____", g.getMaskedWord(), "Uppercase guess works same as lowercase");
        }

        System.out.println();
        System.out.println("Results: " + passed + " passed, " + failed + " failed");
        System.exit(failed > 0 ? 1 : 0);
    }
}
