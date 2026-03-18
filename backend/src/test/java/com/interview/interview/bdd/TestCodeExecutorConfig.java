package com.interview.interview.bdd;

import com.interview.execution.CodeExecutionRequest;
import com.interview.execution.CodeExecutionResult;
import com.interview.execution.CodeExecutor;
import com.interview.execution.ContainerFile;
import com.interview.execution.ContainerManager;
import com.interview.execution.ExecutionStatus;
import com.interview.execution.ProjectExecutionRequest;
import com.interview.execution.TerminalSession;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

@TestConfiguration
public class TestCodeExecutorConfig {

    @Bean
    @Primary
    CodeExecutor controllableCodeExecutor(
            @Qualifier("dockerCodeExecutor") CodeExecutor realExecutor) {
        return new ControllableCodeExecutor(realExecutor);
    }

    @Bean
    @Primary
    ContainerManager testContainerManager() {
        return new TestContainerManager();
    }

    static class ControllableCodeExecutor implements CodeExecutor {

        enum Mode { PASS_THROUGH, FORCE_PASS, FORCE_FAIL }

        static volatile Mode currentMode = Mode.PASS_THROUGH;

        private final CodeExecutor delegate;

        ControllableCodeExecutor(CodeExecutor delegate) {
            this.delegate = delegate;
        }

        static void reset()     { currentMode = Mode.PASS_THROUGH; }
        static void forcePass() { currentMode = Mode.FORCE_PASS; }
        static void forceFail() { currentMode = Mode.FORCE_FAIL; }

        @Override
        public CodeExecutionResult execute(CodeExecutionRequest request) {
            return switch (currentMode) {
                case PASS_THROUGH -> delegate.execute(request);
                case FORCE_PASS   -> successResult();
                case FORCE_FAIL   -> failResult();
            };
        }

        @Override
        public CodeExecutionResult executeProject(ProjectExecutionRequest request) {
            return switch (currentMode) {
                case PASS_THROUGH -> delegate.executeProject(request);
                case FORCE_PASS   -> successResult();
                case FORCE_FAIL   -> failResult();
            };
        }

        private static CodeExecutionResult successResult() {
            return new CodeExecutionResult(0, "All tests passed", "", 10L, ExecutionStatus.SUCCESS);
        }

        private static CodeExecutionResult failResult() {
            return new CodeExecutionResult(1, "", "Tests failed", 10L, ExecutionStatus.SUCCESS);
        }
    }

    static class TestContainerManager implements ContainerManager {

        enum Mode { FORCE_PASS, FORCE_FAIL }

        static volatile Mode currentMode = Mode.FORCE_PASS;

        static void reset()     { currentMode = Mode.FORCE_PASS; }
        static void forcePass() { currentMode = Mode.FORCE_PASS; }
        static void forceFail() { currentMode = Mode.FORCE_FAIL; }

        // exam.yml 從 test classpath 載入，確保 checkpoint title 與 feature 檔斷言完全一致
        // 對應檔案：backend/src/test/resources/test-exam.yml
        private static final String EXAM_YML_CONTENT = loadTestExamYml();

        private static String loadTestExamYml() {
            try (InputStream is = TestContainerManager.class
                    .getClassLoader().getResourceAsStream("test-exam.yml")) {
                if (is == null) throw new IllegalStateException("test-exam.yml not found on test classpath");
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public String startContainer(String image, int memoryMb, int cpuCount) {
            return "test-container-" + System.nanoTime();
        }

        @Override
        public void stopContainer(String containerId) {
            // no-op
        }

        @Override
        public boolean isRunning(String containerId) {
            return containerId != null && containerId.startsWith("test-container-");
        }

        @Override
        public List<ContainerFile> listDirectory(String containerId, String dir) {
            return List.of();
        }

        @Override
        public List<ContainerFile> directoryTree(String containerId, String dir, int maxDepth) {
            return List.of();
        }

        @Override
        public String readFile(String containerId, String path) {
            // BDD 測試中，凡路徑以 exam.yml 結尾，回傳測試用的 exam config
            if (path != null && path.endsWith("exam.yml")) {
                return EXAM_YML_CONTENT;
            }
            return "";
        }

        @Override
        public void writeFile(String containerId, String path, String content) {
            // no-op
        }

        @Override
        public CodeExecutionResult execCommand(String containerId, String command, int timeoutSeconds) {
            return switch (currentMode) {
                case FORCE_PASS -> new CodeExecutionResult(0, "All tests passed", "", 10L, ExecutionStatus.SUCCESS);
                case FORCE_FAIL -> new CodeExecutionResult(1, "", "Tests failed", 10L, ExecutionStatus.ERROR);
            };
        }

        @Override
        public CodeExecutionResult execCommand(String containerId, String command,
                                                int timeoutSeconds, Consumer<String> outputConsumer) {
            CodeExecutionResult result = execCommand(containerId, command, timeoutSeconds);
            outputConsumer.accept(result.stdout().isBlank() ? result.stderr() : result.stdout());
            return result;
        }

        @Override
        public TerminalSession createTerminalSession(String containerId, String workDir) {
            // BDD 測試不需要真實終端，回傳 no-op stub 即可通過 ContainerManager 介面實作
            return new TerminalSession() {
                @Override public String getId() { return "test-terminal"; }
                @Override public void write(byte[] data) {}
                @Override public void onOutput(java.util.function.Consumer<byte[]> callback) {}
                @Override public void resize(int cols, int rows) {}
                @Override public boolean isAlive() { return false; }
                @Override public void close() {}
            };
        }
    }
}
