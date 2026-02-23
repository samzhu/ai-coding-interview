package com.interview.question.application;

import com.interview.question.QuestionDetail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("QuestionQueryService 業務邏輯")
class QuestionQueryServiceTest {

    @Test
    @DisplayName("查詢不存在題目應拋出 IllegalArgumentException")
    void shouldThrowWhenQuestionNotFound() {
        QuestionQueryService service = new QuestionQueryServiceTestHelper(List.of());

        UUID unknownId = UUID.randomUUID();

        assertThatThrownBy(() -> service.findById(unknownId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(unknownId.toString());
    }

    @Test
    @DisplayName("查詢所有題目應回傳所有載入的題目")
    void shouldReturnAllLoadedQuestions() {
        UUID id = UUID.randomUUID();
        QuestionDetail detail = new QuestionDetail(id, "Test Q", "desc", "EASY", "python", null, null, List.of(), List.of());
        QuestionQueryService service = new QuestionQueryServiceTestHelper(List.of(detail));

        List<QuestionDetail> result = service.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(id);
    }

    @Test
    @DisplayName("查詢存在的題目應回傳正確詳情")
    void shouldReturnQuestionWhenFound() {
        UUID id = UUID.randomUUID();
        QuestionDetail detail = new QuestionDetail(id, "Test Q", "desc", "EASY", "python", null, null, List.of(), List.of());
        QuestionQueryService service = new QuestionQueryServiceTestHelper(List.of(detail));

        QuestionDetail result = service.findById(id);

        assertThat(result.id()).isEqualTo(id);
        assertThat(result.title()).isEqualTo("Test Q");
    }

    /**
     * Test helper that pre-loads given questions into QuestionQueryService,
     * bypassing the @PostConstruct classpath scanning.
     */
    static class QuestionQueryServiceTestHelper extends QuestionQueryService {
        QuestionQueryServiceTestHelper(List<QuestionDetail> initialQuestions) {
            super(null);
            initialQuestions.forEach(q -> questions.put(q.id(), q));
        }
    }
}
