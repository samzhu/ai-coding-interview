package exam.question;

import java.util.HashSet;
import java.util.Set;

/**
 * Core Hangman game logic.
 *
 * Known issues to fix (CP1):
 * - processGuess() does not check for duplicate guesses
 * - isGameWon() has incorrect win condition
 */
public class Game {

    private final String word;
    private final int maxLives;
    private final Set<Character> guessedLetters = new HashSet<>();
    private int remainingLives;

    public Game(String word) {
        this.word = word.toLowerCase();
        this.maxLives = GameConfig.DEFAULT_MAX_LIVES;
        this.remainingLives = maxLives;
    }

    // CP3: Add constructor that accepts Difficulty
    public Game(String word, Difficulty difficulty) {
        throw new UnsupportedOperationException("CP3: Implement difficulty-based constructor");
    }

    /**
     * Process a letter guess.
     *
     * @param letter the letter to guess
     * @return true if the letter is in the word, false otherwise
     */
    public boolean processGuess(char letter) {
        char lower = Character.toLowerCase(letter);

        // BUG: Missing duplicate guess check — same wrong letter deducts life twice

        guessedLetters.add(lower);

        if (word.indexOf(lower) >= 0) {
            return true;
        } else {
            remainingLives--;
            return false;
        }
    }

    /**
     * Returns the word with unguessed letters replaced by underscores.
     * Example: word="hello", guessed={h,l} → "h_ll_"
     */
    public String getMaskedWord() {
        StringBuilder masked = new StringBuilder();
        for (char c : word.toCharArray()) {
            if (guessedLetters.contains(c)) {
                masked.append(c);
            } else {
                masked.append('_');
            }
        }
        return masked.toString();
    }

    /**
     * Check if the game is won.
     * BUG: Currently returns wrong condition.
     */
    public boolean isGameWon() {
        // BUG: should check if all letters are guessed, not if lives <= 0
        return remainingLives <= 0;
    }

    /**
     * Check if the game is lost (no remaining lives).
     */
    public boolean isGameLost() {
        return remainingLives <= 0;
    }

    /**
     * Check if the game is over (won or lost).
     */
    public boolean isGameOver() {
        return isGameWon() || isGameLost();
    }

    public String getWord() {
        return word;
    }

    public int getRemainingLives() {
        return remainingLives;
    }

    public int getMaxLives() {
        return maxLives;
    }

    public Set<Character> getGuessedLetters() {
        return Set.copyOf(guessedLetters);
    }
}
