import java.util.HashSet;
import java.util.Set;

/**
 * Core Hangman game logic.
 *
 * A player guesses letters one at a time. Each correct guess reveals
 * all occurrences of that letter in the word. Each wrong guess costs one life.
 * The game ends when the word is fully revealed (win) or lives reach zero (loss).
 */
public class Game {

    private final String word;
    private final int maxLives;
    private int remainingLives;
    private final Set<Character> guessedLetters;

    public Game(String word, int maxLives) {
        this.word = word.toLowerCase();
        this.maxLives = maxLives;
        this.remainingLives = maxLives;
        this.guessedLetters = new HashSet<>();
    }

    /**
     * Processes a single letter guess.
     * If the letter was already guessed, nothing changes.
     * If correct, the letter is revealed. If wrong, one life is deducted.
     *
     * @return true if the letter was found in the word, false otherwise
     */
    public boolean processGuess(char letter) {
        char lower = Character.toLowerCase(letter);

        // BUG 1: missing duplicate-guess check.
        // When the same wrong letter is guessed twice, remainingLives-- runs twice.
        // Fix: check if guessedLetters already contains 'lower' and return early.
        guessedLetters.add(lower);
        boolean found = word.indexOf(lower) >= 0;
        if (!found) {
            remainingLives--;
        }
        return found;
    }

    /**
     * Returns the masked word, with unguessed letters shown as underscores.
     * Example: word="hello", guessed={h,l} -> "h_ll_"
     */
    public String getMaskedWord() {
        StringBuilder sb = new StringBuilder();
        for (char c : word.toCharArray()) {
            sb.append(guessedLetters.contains(c) ? c : '_');
        }
        return sb.toString();
    }

    /**
     * Returns true if the player has won (all letters revealed).
     *
     * BUG 2: condition is inverted — currently returns true when lives == 0
     * (that's the LOSS condition). Should return true when every character
     * in the word has been guessed.
     */
    public boolean isGameWon() {
        return remainingLives <= 0;
    }

    /** Returns true if the player has lost (no lives remaining). */
    public boolean isGameLost() {
        return remainingLives <= 0;
    }

    public String getWord()            { return word; }
    public int getRemainingLives()     { return remainingLives; }
    public int getMaxLives()           { return maxLives; }
    public Set<Character> getGuessedLetters() { return new HashSet<>(guessedLetters); }
}
