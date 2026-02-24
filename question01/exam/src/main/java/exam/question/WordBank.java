package exam.question;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Word bank that provides words categorized by difficulty.
 */
public class WordBank {

    private static final Random RANDOM = new Random();

    private static final Map<Difficulty, List<String>> WORDS = Map.of(
            Difficulty.EASY, List.of("cat", "dog", "sun", "hat", "run", "big", "red", "cup"),
            Difficulty.MEDIUM, List.of("python", "bridge", "castle", "garden", "forest", "rocket"),
            Difficulty.HARD, List.of("algorithm", "hypothesis", "xylophone", "labyrinth", "chrysalis")
    );

    /**
     * Returns a random word for the given difficulty level.
     */
    public static String getRandomWord(Difficulty difficulty) {
        List<String> wordList = WORDS.get(difficulty);
        return wordList.get(RANDOM.nextInt(wordList.size()));
    }
}
