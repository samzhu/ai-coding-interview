import java.util.Set;

/**
 * Provides hints and scoring for the Hangman game.
 */
public class HintProvider {

    /**
     * Returns the unguessed letter that appears most frequently in the word.
     *
     * If multiple letters share the top frequency, return the one that
     * comes first alphabetically. Returns null if all letters are guessed
     * or the word is empty.
     *
     * @param word    the secret word (lowercase)
     * @param guessed the set of already-guessed letters
     * @return the highest-frequency unguessed letter, or null if none remain
     */
    public Character getLetterFrequencyHint(String word, Set<Character> guessed) {
        // TODO: Implement letter frequency hint
        // Steps:
        // 1. Count occurrences of each character in word
        // 2. Filter out already-guessed characters
        // 3. Return the character with the highest count
        //    (tie-break: alphabetically first)
        // 4. Return null if no unguessed letters remain
        return null;
    }

    /**
     * Reveals the position of the first unguessed letter in the word.
     *
     * Returns a hint string like "Position 2 is 'e'" (1-based index).
     * Returns "No hints available" if all letters are already guessed or word is empty.
     *
     * @param word    the secret word (lowercase)
     * @param guessed the set of already-guessed letters
     * @return a position hint string
     */
    public String getPositionHint(String word, Set<Character> guessed) {
        // TODO: Implement position hint
        // Steps:
        // 1. Iterate word characters with their index
        // 2. Find the first index whose character is NOT in guessed
        // 3. If none found, return "No hints available"
        // 4. Return format: "Position X is 'c'" (X is 1-based)
        return "No hints available";
    }

    /**
     * Calculates a performance score for the game session.
     *
     * Formula:
     *   baseScore    = wordLength * 100
     *   wrongPenalty = wrongGuesses * 10
     *   hintPenalty  = hintsUsed * 20
     *   score        = max(0, baseScore - wrongPenalty - hintPenalty)
     *
     * @param totalGuesses total number of guesses made
     * @param wrongGuesses number of wrong guesses
     * @param hintsUsed    number of hints requested
     * @param wordLength   length of the secret word
     * @return the final score (minimum 0)
     */
    public int calculateScore(int totalGuesses, int wrongGuesses, int hintsUsed, int wordLength) {
        // TODO: Implement scoring formula
        // baseScore = wordLength * 100
        // wrongPenalty = wrongGuesses * 10
        // hintPenalty = hintsUsed * 20
        // return Math.max(0, baseScore - wrongPenalty - hintPenalty)
        return 0;
    }
}
