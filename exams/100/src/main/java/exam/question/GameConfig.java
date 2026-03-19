package exam.question;

/**
 * Game configuration constants.
 */
public final class GameConfig {

    public static final int DEFAULT_MAX_LIVES = 6;
    public static final int EASY_MAX_LIVES = 8;
    public static final int MEDIUM_MAX_LIVES = 6;
    public static final int HARD_MAX_LIVES = 4;

    public static final int SCORE_BASE_PER_LETTER = 100;
    public static final int SCORE_PENALTY_WRONG_GUESS = 10;
    public static final int SCORE_PENALTY_HINT = 20;

    private GameConfig() {
        // utility class
    }
}
