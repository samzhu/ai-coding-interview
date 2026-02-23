import java.util.HashSet;
import java.util.Set;

/**
 * Checkpoint 3: Edge cases and defensive validation.
 * Pure Java test runner — no external JUnit jar required.
 *
 * Tests cover:
 *   - Single-letter word
 *   - All-same-letter word ("aaa")
 *   - Case insensitivity consistency (A == a)
 *   - Repeated wrong guess idempotency
 *   - Multiple distinct wrong guesses
 *   - HintProvider with empty word
 *   - Score with zero word length
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

        // --- Game edge cases ---
        System.out.println("\n-- Game edge cases --");
        {
            // Single-letter word: guess correctly wins immediately
            Game g = new Game("a", 6);
            g.processGuess('a');
            check(g.isGameWon(), "Single-letter word: win immediately on correct guess");
        }
        {
            // All-same-letter word: one guess reveals all
            Game g = new Game("aaa", 6);
            g.processGuess('a');
            eq("aaa", g.getMaskedWord(), "All-same-letter: one guess reveals all positions");
            check(g.isGameWon(), "All-same-letter: game won after one unique letter guessed");
        }
        {
            // Case insensitivity: H and h count as the same guess, no extra life lost
            Game g = new Game("hello", 6);
            g.processGuess('H');   // uppercase correct
            g.processGuess('h');   // same letter lowercase — must be a duplicate, no life lost
            eq(6, g.getRemainingLives(),
                "Case insensitivity: H and h are same guess, no life deducted for either");
        }
        {
            // Repeated wrong guess is idempotent
            Game g = new Game("hello", 6);
            g.processGuess('z');                     // wrong
            int livesAfterFirst = g.getRemainingLives();
            g.processGuess('z');                     // duplicate wrong
            eq(livesAfterFirst, g.getRemainingLives(),
                "Duplicate wrong guess is idempotent: no extra life deducted");
        }
        {
            // Multiple distinct wrong guesses each deduct one life
            Game g = new Game("hello", 6);
            g.processGuess('z');
            g.processGuess('x');
            g.processGuess('q');
            eq(3, g.getRemainingLives(),
                "Three distinct wrong guesses leave 3 lives from 6");
        }

        // --- HintProvider edge cases ---
        System.out.println("\n-- HintProvider edge cases --");
        HintProvider p = new HintProvider();
        {
            // Empty word -> null frequency hint
            Character hint = p.getLetterFrequencyHint("", new HashSet<>());
            eq(null, hint, "Empty word returns null frequency hint");
        }
        {
            // Empty word -> no hints available
            String hint = p.getPositionHint("", new HashSet<>());
            eq("No hints available", hint, "Empty word returns 'No hints available'");
        }
        {
            // Zero word length -> score is 0
            int score = p.calculateScore(0, 0, 0, 0);
            eq(0, score, "Zero word length: score is 0");
        }
        {
            // Single unique letter already guessed -> null
            Set<Character> guessed = new HashSet<>();
            guessed.add('a');
            Character hint = p.getLetterFrequencyHint("aaa", guessed);
            eq(null, hint, "All-same-letter word: if only letter is guessed, return null");
        }

        System.out.println();
        System.out.println("Results: " + passed + " passed, " + failed + " failed");
        System.exit(failed > 0 ? 1 : 0);
    }
}
