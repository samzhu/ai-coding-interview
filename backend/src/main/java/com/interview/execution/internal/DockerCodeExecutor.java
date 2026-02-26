package com.interview.execution.internal;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.StreamType;
import com.github.dockerjava.api.model.WaitResponse;
import com.interview.execution.CodeExecutionRequest;
import com.interview.execution.CodeExecutionResult;
import com.interview.execution.CodeExecutor;
import com.interview.execution.ExecutionStatus;
import com.interview.execution.ProjectExecutionRequest;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Profile("!lab")
class DockerCodeExecutor implements CodeExecutor {

    private static final Map<String, String> FILE_NAMES = Map.of(
            "java", "Solution.java",
            "python", "solution.py",
            "javascript", "solution.js"
    );

    private static final Map<String, String> RUN_COMMANDS = Map.of(
            "java", "cp /code/Solution.java /tmp/ && cd /tmp && javac Solution.java && java Solution",
            "python", "python3 /code/solution.py",
            "javascript", "node /code/solution.js"
    );

    private final DockerClient dockerClient;
    private final String dockerImage;

    DockerCodeExecutor(DockerClient dockerClient,
                       @Value("${execution.docker.image:eclipse-temurin:25-jdk}") String dockerImage) {
        this.dockerClient = dockerClient;
        this.dockerImage = dockerImage;
    }

    @Override
    public CodeExecutionResult execute(CodeExecutionRequest request) {
        String fileName = FILE_NAMES.get(request.language());
        if (fileName == null) {
            return new CodeExecutionResult(-1, "", "Unsupported language: " + request.language(), 0, ExecutionStatus.ERROR);
        }

        Path tempDir = null;
        String containerId = null;
        try {
            tempDir = Files.createTempDirectory("exec-" + UUID.randomUUID());
            Files.writeString(tempDir.resolve(fileName), request.code());

            String runCmd = RUN_COMMANDS.get(request.language());
            if (request.stdin() != null && !request.stdin().isBlank()) {
                Files.writeString(tempDir.resolve("stdin.txt"), request.stdin());
                runCmd = runCmd + " < /code/stdin.txt";
            }

            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withMemory(128 * 1024 * 1024L)
                    .withCpuCount(1L)
                    .withTmpFs(Map.of("/tmp", "rw,nosuid,size=64m"));

            var container = dockerClient.createContainerCmd(dockerImage)
                    .withCmd("bash", "-c", runCmd)
                    .withHostConfig(hostConfig)
                    .exec();
            containerId = container.getId();

            copyDirectoryToContainer(containerId, tempDir, "/code");

            long startMs = System.currentTimeMillis();
            dockerClient.startContainerCmd(containerId).exec();

            Integer exitCode = waitForContainer(containerId, request.timeoutSeconds());
            long durationMs = System.currentTimeMillis() - startMs;

            if (exitCode == null) {
                try { dockerClient.killContainerCmd(containerId).exec(); } catch (Exception ignored) {}
                return new CodeExecutionResult(-1, "", "Execution timed out after " + request.timeoutSeconds() + "s",
                        durationMs, ExecutionStatus.TIMEOUT);
            }

            String[] logs = collectLogs(containerId);
            ExecutionStatus status = exitCode == 0 ? ExecutionStatus.SUCCESS : ExecutionStatus.ERROR;
            return new CodeExecutionResult(exitCode, logs[0], logs[1], durationMs, status);

        } catch (Exception e) {
            return new CodeExecutionResult(-1, "", e.getMessage(), 0, ExecutionStatus.ERROR);
        } finally {
            removeContainer(containerId);
            deleteDirectory(tempDir);
        }
    }

    @Override
    public CodeExecutionResult executeProject(ProjectExecutionRequest request) {
        Path tempDir = null;
        String containerId = null;
        try {
            tempDir = Files.createTempDirectory("proj-" + UUID.randomUUID());
            for (Map.Entry<String, String> entry : request.files().entrySet()) {
                Path filePath = tempDir.resolve(entry.getKey());
                Files.createDirectories(filePath.getParent());
                Files.writeString(filePath, entry.getValue());
            }

            String runCmd = "cp -r /code/. /tmp/workspace/ && cd /tmp/workspace && " + request.testCommand();

            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withMemory(256 * 1024 * 1024L)
                    .withCpuCount(1L)
                    .withTmpFs(Map.of("/tmp", "rw,nosuid,size=128m"));

            String image = request.dockerImage() != null ? request.dockerImage() : this.dockerImage;
            var container = dockerClient.createContainerCmd(image)
                    .withCmd("bash", "-c", runCmd)
                    .withHostConfig(hostConfig)
                    .exec();
            containerId = container.getId();

            copyDirectoryToContainer(containerId, tempDir, "/code");

            long startMs = System.currentTimeMillis();
            dockerClient.startContainerCmd(containerId).exec();

            Integer exitCode = waitForContainer(containerId, request.timeoutSeconds());
            long durationMs = System.currentTimeMillis() - startMs;

            if (exitCode == null) {
                try { dockerClient.killContainerCmd(containerId).exec(); } catch (Exception ignored) {}
                return new CodeExecutionResult(-1, "", "Execution timed out after " + request.timeoutSeconds() + "s",
                        durationMs, ExecutionStatus.TIMEOUT);
            }

            String[] logs = collectLogs(containerId);
            ExecutionStatus status = exitCode == 0 ? ExecutionStatus.SUCCESS : ExecutionStatus.ERROR;
            return new CodeExecutionResult(exitCode, logs[0], logs[1], durationMs, status);

        } catch (Exception e) {
            return new CodeExecutionResult(-1, "", e.getMessage(), 0, ExecutionStatus.ERROR);
        } finally {
            removeContainer(containerId);
            deleteDirectory(tempDir);
        }
    }

    private Integer waitForContainer(String containerId, int timeoutSeconds) throws InterruptedException {
        AtomicInteger exitCode = new AtomicInteger(-1);
        CountDownLatch latch = new CountDownLatch(1);

        dockerClient.waitContainerCmd(containerId).exec(new ResultCallback.Adapter<WaitResponse>() {
            @Override
            public void onNext(WaitResponse response) {
                exitCode.set(response.getStatusCode());
            }

            @Override
            public void onComplete() {
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                latch.countDown();
            }
        });

        boolean finished = latch.await(timeoutSeconds, TimeUnit.SECONDS);
        return finished ? exitCode.get() : null;
    }

    private String[] collectLogs(String containerId) throws InterruptedException {
        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();

        dockerClient.logContainerCmd(containerId)
                .withFollowStream(false)
                .withStdOut(true)
                .withStdErr(true)
                .exec(new ResultCallback.Adapter<Frame>() {
                    @Override
                    public void onNext(Frame frame) {
                        String text = new String(frame.getPayload(), StandardCharsets.UTF_8);
                        if (frame.getStreamType() == StreamType.STDOUT) {
                            stdout.append(text);
                        } else if (frame.getStreamType() == StreamType.STDERR) {
                            stderr.append(text);
                        }
                    }
                })
                .awaitCompletion();

        return new String[]{ stdout.toString().trim(), stderr.toString().trim() };
    }

    private void copyDirectoryToContainer(String containerId, Path sourceDir, String remotePath) throws IOException {
        // Build tar with remotePath as the top-level directory name so Docker creates it automatically.
        // e.g. remotePath="/code" → tar entries are "code/Solution.java" extracted to container root "/".
        String dirName = remotePath.startsWith("/") ? remotePath.substring(1) : remotePath;
        byte[] tarBytes = createTarWithPrefix(sourceDir, dirName + "/");
        try (InputStream is = new ByteArrayInputStream(tarBytes)) {
            dockerClient.copyArchiveToContainerCmd(containerId)
                    .withTarInputStream(is)
                    .withRemotePath("/")
                    .exec();
        }
    }

    private byte[] createTarWithPrefix(Path directory, String prefix) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tar = new TarArchiveOutputStream(bos)) {
            tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            // Add the directory entry so Docker creates the target directory
            TarArchiveEntry dirEntry = new TarArchiveEntry(prefix);
            tar.putArchiveEntry(dirEntry);
            tar.closeArchiveEntry();
            // Add all files with the prefix
            try (var walk = Files.walk(directory)) {
                walk.filter(Files::isRegularFile).forEach(file -> {
                    try {
                        String entryName = prefix + directory.relativize(file).toString();
                        TarArchiveEntry entry = new TarArchiveEntry(file.toFile(), entryName);
                        tar.putArchiveEntry(entry);
                        Files.copy(file, tar);
                        tar.closeArchiveEntry();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            }
        }
        return bos.toByteArray();
    }

    private void removeContainer(String containerId) {
        if (containerId == null) return;
        try { dockerClient.removeContainerCmd(containerId).withForce(true).exec(); } catch (Exception ignored) {}
    }

    private String detectLanguage(Map<String, String> files) {
        for (String filePath : files.keySet()) {
            if (filePath.endsWith(".py")) return "python";
            if (filePath.endsWith(".js")) return "javascript";
            if (filePath.endsWith(".java")) return "java";
        }
        return "python";
    }

    private void deleteDirectory(Path dir) {
        if (dir == null) return;
        try {
            Files.walk(dir)
                    .sorted(java.util.Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException ignored) {
        }
    }
}
