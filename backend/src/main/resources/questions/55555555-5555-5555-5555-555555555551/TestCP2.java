import java.util.HashSet;
import java.util.Set;

/**
 * Checkpoint 2: Verify HintProvider implementation.
 * Pure Java test runner — no external JUnit jar required.
 *
 * Tests cover:
 *   getLetterFrequencyHint — highest-frequency unguessed letter
 *   getPositionHint        — reveal first position of an unguessed letter
 *   calculateScore         — scoring formula correctness
 */
public class TestCP2 {

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
        System.out.println("=== CP2: HintProvider Implementation Tests ===");
        HintProvider p = new HintProvider();

        // --- getLetterFrequencyHint ---
        System.out.println("\n-- getLetterFrequencyHint --");
        {
            // "banana" -> b(1), a(3), n(2) -- 'a' is most frequent, unguessed
            Character hint = p.getLetterFrequencyHint("banana", new HashSet<>());
            eq('a', hint, "Returns highest-frequency unguessed letter ('a' in banana)");
        }
        {
            // "banana" with 'a' guessed -> n(2) > b(1)
            Set<Character> guessed = new HashSet<>();
            guessed.add('a');
            Character hint = p.getLetterFrequencyHint("banana", guessed);
            eq('n', hint, "Skips already-guessed letters ('a' guessed -> returns 'n')");
        }
        {
            // All letters guessed -> null
            Set<Character> guessed = Set.of('h', 'e', 'l', 'o');
            Character hint = p.getLetterFrequencyHint("hello", guessed);
            eq(null, hint, "Returns null when all letters are guessed");
        }
        {
            // "abcd" -> all appear once; tie-break alphabetically -> 'a'
            Character hint = p.getLetterFrequencyHint("abcd", new HashSet<>());
            eq('a', hint, "Tie-breaks alphabetically ('a' first in abcd)");
        }

        // --- getPositionHint ---
        System.out.println("\n-- getPositionHint --");
        {
            // word="hello", guessed={h} -> first unguessed is 'e' at index 1 -> "Position 2 is 'e'"
            Set<Character> guessed = new HashSet<>();
            guessed.add('h');
            String hint = p.getPositionHint("hello", guessed);
            eq("Position 2 is 'e'", hint, "Returns position hint for first unguessed letter");
        }
        {
            // All letters guessed -> "No hints available"
            Set<Character> guessed = Set.of('h', 'e', 'l', 'o');
            String hint = p.getPositionHint("hello", guessed);
            eq("No hints available", hint, "Returns 'No hints available' when all guessed");
        }
        {
            // word="apple", nothing guessed -> first unguessed index 0 -> "Position 1 is 'a'"
            String hint = p.getPositionHint("apple", new HashSet<>());
            eq("Position 1 is 'a'", hint, "First position hint when nothing guessed");
        }

        // --- calculateScore ---
        System.out.println("\n-- calculateScore --");
        {
            // wordLength=5, wrong=0, hints=0 -> base=500, penalty=0 -> 500
            int score = p.calculateScore(5, 0, 0, 5);
            eq(500, score, "Perfect score: no wrong guesses, no hints");
        }
        {
            // wordLength=5, wrong=2, hints=1 -> base=500, wrong=20, hint=20 -> 460
            int score = p.calculateScore(7, 2, 1, 5);
            eq(460, score, "Score reduced by wrong guesses (2x10) and hints (1x20)");
        }
        {
            // Heavy penalties: wordLength=1, wrong=100, hints=100 -> clamped to 0
            int score = p.calculateScore(200, 100, 100, 1);
            eq(0, score, "Score never goes below zero");
        }

        System.out.println();
        System.out.println("Results: " + passed + " passed, " + failed + " failed");
        System.exit(failed > 0 ? 1 : 0);
    }
}
