package example.question;

import org.springframework.boot.SpringApplication;

public class TestQuestionApplication {

	public static void main(String[] args) {
		SpringApplication.from(QuestionApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
