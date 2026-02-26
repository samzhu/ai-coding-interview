package com.interview.execution;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExamConfigService 快取與 YAML 解析")
class ExamConfigServiceTest {

    @Mock
    private ContainerService containerService;

    private ExamConfigService examConfigService;

    private static final String CONTAINER_ID = "test-container-abc";
    private static final String EXAM_YML_PATH = "/workspace/exam.yml";

    private static final String VALID_EXAM_YML = """
            workspace: "/workspace"
            exclude:
              - ".gradle/"
              - "build/"
              - "PRD.md"
            checkpoints:
              - id: "1"
                title: "Bug Fix"
                description: "Fix the bugs"
                testCommand: "./gradlew test --tests 'CP1Test'"
              - id: "2"
                title: "Feature"
                description: "Add feature"
                testCommand: "./gradlew test --tests 'CP2Test'"
            """;

    @BeforeEach
    void setUp() {
        examConfigService = new ExamConfigService(containerService);
    }

    @Test
    @DisplayName("成功解析有效的 exam.yml 並回傳 ExamConfig")
    void shouldParseValidExamYml() {
        when(containerService.readFile(CONTAINER_ID, EXAM_YML_PATH)).thenReturn(VALID_EXAM_YML);

        ExamConfig config = examConfigService.getExamConfig(CONTAINER_ID);

        assertThat(config.workspace()).isEqualTo("/workspace");
        assertThat(config.checkpoints()).hasSize(2);
        assertThat(config.checkpoints().get(0).id()).isEqualTo("1");
        assertThat(config.checkpoints().get(0).title()).isEqualTo("Bug Fix");
        assertThat(config.checkpoints().get(0).testCommand()).isEqualTo("./gradlew test --tests 'CP1Test'");
        assertThat(config.checkpoints().get(1).id()).isEqualTo("2");
    }

    @Test
    @DisplayName("exam.yml 自動加入 exclude 清單（即使 YAML 中沒有列出）")
    void shouldAutoExcludeExamYml() {
        when(containerService.readFile(CONTAINER_ID, EXAM_YML_PATH)).thenReturn(VALID_EXAM_YML);

        ExamConfig config = examConfigService.getExamConfig(CONTAINER_ID);

        assertThat(config.exclude()).contains("exam.yml");
    }

    @Test
    @DisplayName("即使 YAML 的 exclude 已包含 exam.yml，不重複加入")
    void shouldNotDuplicateExamYmlInExclude() {
        String yamlWithExamYmlExcluded = """
                workspace: "/workspace"
                exclude:
                  - "exam.yml"
                  - "build/"
                checkpoints:
                  - id: "1"
                    title: "CP1"
                    testCommand: "test"
                """;
        when(containerService.readFile(CONTAINER_ID, EXAM_YML_PATH)).thenReturn(yamlWithExamYmlExcluded);

        ExamConfig config = examConfigService.getExamConfig(CONTAINER_ID);

        long examYmlCount = config.exclude().stream().filter("exam.yml"::equals).count();
        assertThat(examYmlCount).isEqualTo(1);
    }

    @Test
    @DisplayName("同一 containerId 只讀取一次（快取生效）")
    void shouldCacheExamConfigPerContainer() {
        when(containerService.readFile(CONTAINER_ID, EXAM_YML_PATH)).thenReturn(VALID_EXAM_YML);

        ExamConfig first = examConfigService.getExamConfig(CONTAINER_ID);
        ExamConfig second = examConfigService.getExamConfig(CONTAINER_ID);

        assertThat(first).isSameAs(second);
        // 只呼叫一次 readFile
        verify(containerService, times(1)).readFile(CONTAINER_ID, EXAM_YML_PATH);
    }

    @Test
    @DisplayName("evict 後重新讀取 exam.yml")
    void shouldReloadAfterEvict() {
        when(containerService.readFile(CONTAINER_ID, EXAM_YML_PATH)).thenReturn(VALID_EXAM_YML);

        examConfigService.getExamConfig(CONTAINER_ID);
        examConfigService.evict(CONTAINER_ID);
        examConfigService.getExamConfig(CONTAINER_ID);

        verify(containerService, times(2)).readFile(CONTAINER_ID, EXAM_YML_PATH);
    }

    @Test
    @DisplayName("exam.yml 內容為空時拋出 ExamConfigNotFoundException")
    void shouldThrowWhenExamYmlIsEmpty() {
        when(containerService.readFile(CONTAINER_ID, EXAM_YML_PATH)).thenReturn("");

        assertThatThrownBy(() -> examConfigService.getExamConfig(CONTAINER_ID))
                .isInstanceOf(ExamConfigNotFoundException.class)
                .hasMessageContaining(CONTAINER_ID)
                .hasMessageContaining(EXAM_YML_PATH);
    }

    @Test
    @DisplayName("readFile 拋出例外時包裝為 ExamConfigNotFoundException")
    void shouldWrapReadFileExceptionAsExamConfigNotFound() {
        when(containerService.readFile(CONTAINER_ID, EXAM_YML_PATH))
                .thenThrow(new RuntimeException("container not found"));

        assertThatThrownBy(() -> examConfigService.getExamConfig(CONTAINER_ID))
                .isInstanceOf(ExamConfigNotFoundException.class)
                .hasMessageContaining(CONTAINER_ID);
    }

    @Test
    @DisplayName("workspace 未設定時使用預設值 /workspace")
    void shouldUseDefaultWorkspaceWhenNotSpecified() {
        String yamlWithoutWorkspace = """
                checkpoints:
                  - id: "1"
                    title: "CP1"
                    testCommand: "test"
                """;
        when(containerService.readFile(CONTAINER_ID, EXAM_YML_PATH)).thenReturn(yamlWithoutWorkspace);

        ExamConfig config = examConfigService.getExamConfig(CONTAINER_ID);

        assertThat(config.effectiveWorkspace()).isEqualTo("/workspace");
    }

    @Test
    @DisplayName("不同 containerId 分別快取")
    void shouldCacheSeparatelyPerContainer() {
        String otherContainerId = "other-container-xyz";
        when(containerService.readFile(CONTAINER_ID, EXAM_YML_PATH)).thenReturn(VALID_EXAM_YML);
        when(containerService.readFile(otherContainerId, EXAM_YML_PATH)).thenReturn(VALID_EXAM_YML);

        examConfigService.getExamConfig(CONTAINER_ID);
        examConfigService.getExamConfig(otherContainerId);

        verify(containerService, times(1)).readFile(CONTAINER_ID, EXAM_YML_PATH);
        verify(containerService, times(1)).readFile(otherContainerId, EXAM_YML_PATH);
    }
}
