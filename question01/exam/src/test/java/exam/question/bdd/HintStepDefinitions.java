package exam.question.bdd;

import exam.question.HintProvider;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class HintStepDefinitions {

    private HintProvider hintProvider;
    private String word;
    private Set<Character> guessedLetters;
    private Character hintLetter;
    private int hintPosition;
    private int score;

    @Before
    public void reset() {
        hintProvider = null;
        word = null;
        guessedLetters = new HashSet<>();
        hintLetter = null;
        hintPosition = 0;
        score = 0;
    }

    // --- Given ---

    @Given("a hint provider")
    public void aHintProvider() {
        hintProvider = new HintProvider();
    }

    @Given("the word is {string}")
    public void theWordIs(String word) {
        this.word = word;
    }

    @Given("the guessed letters are {string}")
    public void theGuessedLettersAre(String letters) {
        guessedLetters = new HashSet<>();
        for (char c : letters.toCharArray()) {
            guessedLetters.add(c);
        }
    }

    @Given("no letters have been guessed")
    public void noLettersHaveBeenGuessed() {
        guessedLetters = new HashSet<>();
    }

    // --- When ---

    @When("I request a letter frequency hint")
    public void iRequestALetterFrequencyHint() {
        hintLetter = hintProvider.getLetterFrequencyHint(word, guessedLetters);
    }

    @When("I request a letter frequency hint for null word")
    public void iRequestALetterFrequencyHintForNullWord() {
        hintLetter = hintProvider.getLetterFrequencyHint(null, guessedLetters);
    }

    @When("I request a position hint")
    public void iRequestAPositionHint() {
        hintPosition = hintProvider.getPositionHint(word, guessedLetters);
    }

    @When("I request a position hint for null word")
    public void iRequestAPositionHintForNullWord() {
        hintPosition = hintProvider.getPositionHint(null, guessedLetters);
    }

    @When("I calculate score for word length {int} with {int} wrong guesses and {int} hints")
    public void iCalculateScore(int wordLength, int wrongGuesses, int hints) {
        score = hintProvider.calculateScore(wordLength, wrongGuesses, hints);
    }

    // --- Then ---

    @Then("the hint letter should be {string}")
    public void theHintLetterShouldBe(String expected) {
        assertThat(hintLetter).isEqualTo(expected.charAt(0));
    }

    @Then("the hint letter should be null")
    public void theHintLetterShouldBeNull() {
        assertThat(hintLetter).isNull();
    }

    @Then("the position should be {int}")
    public void thePositionShouldBe(int expected) {
        assertThat(hintPosition).isEqualTo(expected);
    }

    @Then("the score should be {int}")
    public void theScoreShouldBe(int expected) {
        assertThat(score).isEqualTo(expected);
    }
}
