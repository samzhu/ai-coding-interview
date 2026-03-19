package exam.question;

import java.util.Set;

/**
 * Provides hints for the Hangman game.
 *
 * CP2: Implement all three TODO methods.
 */
public class HintProvider {

    /**
     * Returns the most frequent unguessed letter in the word.
     * If there is a tie, return the letter that comes first alphabetically.
     * If all letters have been guessed, return null.
     *
     * @param word           the target word
     * @param guessedLetters letters already guessed
     * @return the most frequent unguessed letter, or null if none
     */
    public Character getLetterFrequencyHint(String word, Set<Character> guessedLetters) {
        // TODO: Implement letter frequency hint
        return null;
    }

    /**
     * Returns the 1-based position of the first unguessed character in the word.
     * If all characters have been guessed, return -1.
     *
     * @param word           the target word
     * @param guessedLetters letters already guessed
     * @return 1-based position of first unguessed character, or -1
     */
    public int getPositionHint(String word, Set<Character> guessedLetters) {
        // TODO: Implement position hint
        return -1;
    }

    /**
     * Calculate the player's score.
     * Formula: max(0, wordLength * 100 - wrongGuesses * 10 - hintsUsed * 20)
     *
     * @param wordLength   length of the target word
     * @param wrongGuesses number of incorrect guesses
     * @param hintsUsed    number of hints used
     * @return the calculated score (minimum 0)
     */
    public int calculateScore(int wordLength, int wrongGuesses, int hintsUsed) {
        // TODO: Implement score calculation
        return 0;
    }
}
