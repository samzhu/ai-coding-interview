package exam.question;

import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

/**
 * Interactive CLI demo for Hangman.
 * Lets candidates experience the game (including bugs) before diving into the code.
 */
public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        HintProvider hintProvider = new HintProvider();
        int hintsUsed = 0;

        System.out.println("=== Hangman ===");
        System.out.println("Type a letter to guess, 'hint' for a hint, or 'quit' to exit.");
        System.out.println();

        String word = WordBank.getRandomWord();
        Game game = new Game(word);

        while (!game.isGameOver()) {
            printStatus(game, hintProvider, hintsUsed);

            System.out.print("Enter a letter (or 'hint' / 'quit'): ");
            String input = scanner.nextLine().trim().toLowerCase();

            if (input.equals("quit")) {
                System.out.println("Quitting. The word was: " + game.getWord());
                return;
            }

            if (input.equals("hint")) {
                hintsUsed++;
                Character letterHint = hintProvider.getLetterFrequencyHint(game.getWord(), game.getGuessedLetters());
                int posHint = hintProvider.getPositionHint(game.getWord(), game.getGuessedLetters());
                if (letterHint != null) {
                    System.out.println("Hint: Try the letter '" + letterHint + "' (position hint: " + posHint + ")");
                } else {
                    System.out.println("Hint: No hints available — all letters guessed!");
                }
                System.out.println();
                continue;
            }

            if (input.length() != 1 || !Character.isLetter(input.charAt(0))) {
                System.out.println("Please enter a single letter.");
                System.out.println();
                continue;
            }

            char letter = input.charAt(0);

            if (game.getGuessedLetters().contains(letter)) {
                System.out.println("You already guessed '" + letter + "'. Try a different letter.");
                System.out.println();
                continue;
            }

            boolean correct = game.processGuess(letter);
            if (correct) {
                System.out.println("Correct! '" + letter + "' is in the word.");
            } else {
                System.out.println("Wrong! '" + letter + "' is not in the word.");
            }
            System.out.println();
        }

        printStatus(game, hintProvider, hintsUsed);

        if (game.isGameWon()) {
            int wrongGuesses = game.getMaxLives() - game.getRemainingLives();
            int score = hintProvider.calculateScore(game.getWord().length(), wrongGuesses, hintsUsed);
            System.out.println("You won! The word was: " + game.getWord());
            System.out.println("Score: " + score);
        } else {
            System.out.println("Game over! The word was: " + game.getWord());
        }

        scanner.close();
    }

    private static void printStatus(Game game, HintProvider hintProvider, int hintsUsed) {
        String masked = game.getMaskedWord();
        // Format: "_ _ l l _"
        String display = masked.replace("", " ").trim();

        // Sorted guessed letters for readable display
        Set<Character> sorted = new TreeSet<>(game.getGuessedLetters());

        System.out.printf("Word: %-20s  Lives: %d/%d    Guessed: %s%n",
                display,
                game.getRemainingLives(),
                game.getMaxLives(),
                sorted);
    }
}
