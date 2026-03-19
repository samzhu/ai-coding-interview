package exam.question.bdd;

import exam.question.Difficulty;
import exam.question.Game;
import exam.question.WordBank;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

public class GameStepDefinitions {

    private Game game;
    private Boolean lastGuessResult;
    private Exception caughtException;
    private String randomWord;

    @Before
    public void reset() {
        game = null;
        lastGuessResult = null;
        caughtException = null;
        randomWord = null;
    }

    // --- Given ---

    @Given("a new game with word {string}")
    public void aNewGameWithWord(String word) {
        game = new Game(word);
    }

    @Given("a new game with word {string} and difficulty {word}")
    public void aNewGameWithWordAndDifficulty(String word, String difficulty) {
        game = new Game(word, Difficulty.valueOf(difficulty));
    }

    // --- When ---

    @When("the player guesses {string}")
    public void thePlayerGuesses(String letter) {
        lastGuessResult = game.processGuess(letter.charAt(0));
    }

    @When("I create a game with null word")
    public void iCreateAGameWithNullWord() {
        try {
            game = new Game(null);
        } catch (Exception e) {
            caughtException = e;
        }
    }

    @When("I create a game with empty word {string}")
    public void iCreateAGameWithEmptyWord(String word) {
        try {
            game = new Game(word);
        } catch (Exception e) {
            caughtException = e;
        }
    }

    @When("I attempt to guess {string} on a finished game")
    public void iAttemptToGuessOnAFinishedGame(String letter) {
        try {
            game.processGuess(letter.charAt(0));
        } catch (Exception e) {
            caughtException = e;
        }
    }

    @When("I attempt to guess {string} with validation")
    public void iAttemptToGuessWithValidation(String letter) {
        try {
            game.processGuess(letter.charAt(0));
        } catch (Exception e) {
            caughtException = e;
        }
    }

    @When("I request a random word for difficulty {word}")
    public void iRequestARandomWordForDifficulty(String difficulty) {
        randomWord = WordBank.getRandomWord(Difficulty.valueOf(difficulty));
    }

    // --- Then ---

    @Then("the guess result should be true")
    public void theGuessResultShouldBeTrue() {
        assertThat(lastGuessResult).isTrue();
    }

    @Then("the guess result should be false")
    public void theGuessResultShouldBeFalse() {
        assertThat(lastGuessResult).isFalse();
    }

    @Then("the masked word should be {string}")
    public void theMaskedWordShouldBe(String expected) {
        assertThat(game.getMaskedWord()).isEqualTo(expected);
    }

    @Then("the remaining lives should be {int}")
    public void theRemainingLivesShouldBe(int expected) {
        assertThat(game.getRemainingLives()).isEqualTo(expected);
    }

    @Then("the game should be won")
    public void theGameShouldBeWon() {
        assertThat(game.isGameWon()).isTrue();
    }

    @Then("the game should not be won")
    public void theGameShouldNotBeWon() {
        assertThat(game.isGameWon()).isFalse();
    }

    @Then("the game should be lost")
    public void theGameShouldBeLost() {
        assertThat(game.isGameLost()).isTrue();
    }

    @Then("an IllegalArgumentException should be thrown")
    public void anIllegalArgumentExceptionShouldBeThrown() {
        assertThat(caughtException).isInstanceOf(IllegalArgumentException.class);
    }

    @Then("an IllegalStateException should be thrown")
    public void anIllegalStateExceptionShouldBeThrown() {
        assertThat(caughtException).isInstanceOf(IllegalStateException.class);
    }

    @Then("the word should not be null")
    public void theWordShouldNotBeNull() {
        assertThat(randomWord).isNotNull();
    }
}
