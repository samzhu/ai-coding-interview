package exam.question;

/**
 * Difficulty levels for the Hangman game.
 * Each level determines the maximum number of incorrect guesses allowed.
 */
public enum Difficulty {

    EASY(GameConfig.EASY_MAX_LIVES),
    MEDIUM(GameConfig.MEDIUM_MAX_LIVES),
    HARD(GameConfig.HARD_MAX_LIVES);

    private final int maxLives;

    Difficulty(int maxLives) {
        this.maxLives = maxLives;
    }

    public int getMaxLives() {
        return maxLives;
    }
}
