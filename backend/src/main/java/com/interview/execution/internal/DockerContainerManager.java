package com.interview.execution.internal;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.StreamType;
import com.interview.execution.CodeExecutionResult;
import com.interview.execution.ContainerFile;
import com.interview.execution.ContainerManager;
import com.interview.execution.ExecutionStatus;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Profile("!lab")
class DockerContainerManager implements ContainerManager {

    private static final Logger log = LoggerFactory.getLogger(DockerContainerManager.class);

    private final DockerClient dockerClient;

    DockerContainerManager(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    /**
     * Pulls the image from the registry if it is not present on the local Docker daemon.
     * Pull may take several minutes for large images; caller is expected to invoke this
     * from an async/background thread.
     */
    private void pullImageIfAbsent(String image) {
        try {
            dockerClient.inspectImageCmd(image).exec();
        } catch (NotFoundException e) {
            log.info("Image {} not found locally, pulling...", image);
            try {
                dockerClient.pullImageCmd(image)
                        .start()
                        .awaitCompletion(5, TimeUnit.MINUTES);
                log.info("Successfully pulled image {}", image);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Image pull interrupted: " + image, ie);
            }
        }
    }

    @Override
    public String startContainer(String image, int memoryMb, int cpuCount) {
        pullImageIfAbsent(image);

        HostConfig hostConfig = HostConfig.newHostConfig()
                .withMemory((long) memoryMb * 1024 * 1024)
                .withCpuCount((long) cpuCount);

        var container = dockerClient.createContainerCmd(image)
                .withCmd("tail", "-f", "/dev/null")
                .withHostConfig(hostConfig)
                .exec();
        String containerId = container.getId();
        dockerClient.startContainerCmd(containerId).exec();
        log.info("Started container {} from image {}", containerId, image);
        return containerId;
    }

    @Override
    public void stopContainer(String containerId) {
        try {
            dockerClient.stopContainerCmd(containerId).exec();
        } catch (Exception ignored) {
        }
        try {
            dockerClient.removeContainerCmd(containerId).withForce(true).exec();
        } catch (Exception ignored) {
        }
        log.info("Stopped and removed container {}", containerId);
    }

    @Override
    public boolean isRunning(String containerId) {
        try {
            var inspect = dockerClient.inspectContainerCmd(containerId).exec();
            return Boolean.TRUE.equals(inspect.getState().getRunning());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<ContainerFile> listFiles(String containerId, String dir) {
        try {
            String[] output = execRaw(containerId, "find " + dir + " -not -type d", 10);
            List<ContainerFile> files = new ArrayList<>();
            for (String line : output[0].split("\n")) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    files.add(new ContainerFile(trimmed, false, 0));
                }
            }
            return files;
        } catch (Exception e) {
            log.warn("listFiles failed for container {}: {}", containerId, e.getMessage());
            return List.of();
        }
    }

    @Override
    public String readFile(String containerId, String path) {
        String[] output = execRaw(containerId, "cat " + path, 10);
        return output[0];
    }

    @Override
    public void writeFile(String containerId, String path, String content) {
        try {
            // Build a single-file tar and copy it into the container
            byte[] tarBytes = createSingleFileTar(path, content);
            try (InputStream is = new ByteArrayInputStream(tarBytes)) {
                dockerClient.copyArchiveToContainerCmd(containerId)
                        .withTarInputStream(is)
                        .withRemotePath("/")
                        .exec();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public CodeExecutionResult execCommand(String containerId, String command, int timeoutSeconds) {
        long startMs = System.currentTimeMillis();
        try {
            ExecCreateCmdResponse exec = dockerClient.execCreateCmd(containerId)
                    .withCmd("bash", "-c", command)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .exec();

            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();
            CountDownLatch latch = new CountDownLatch(1);

            dockerClient.execStartCmd(exec.getId())
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
            long durationMs = System.currentTimeMillis() - startMs;

            if (!finished) {
                return new CodeExecutionResult(-1, "", "Execution timed out after " + timeoutSeconds + "s",
                        durationMs, ExecutionStatus.TIMEOUT);
            }

            var inspect = dockerClient.inspectExecCmd(exec.getId()).exec();
            int exitCode = inspect.getExitCodeLong() != null ? inspect.getExitCodeLong().intValue() : -1;
            ExecutionStatus status = exitCode == 0 ? ExecutionStatus.SUCCESS : ExecutionStatus.ERROR;
            return new CodeExecutionResult(exitCode, stdout.toString().trim(), stderr.toString().trim(),
                    durationMs, status);

        } catch (Exception e) {
            return new CodeExecutionResult(-1, "", e.getMessage(), System.currentTimeMillis() - startMs,
                    ExecutionStatus.ERROR);
        }
    }

    private String[] execRaw(String containerId, String command, int timeoutSeconds) {
        try {
            ExecCreateCmdResponse exec = dockerClient.execCreateCmd(containerId)
                    .withCmd("bash", "-c", command)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .exec();

            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();
            CountDownLatch latch = new CountDownLatch(1);

            dockerClient.execStartCmd(exec.getId())
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

                        @Override
                        public void onComplete() {
                            latch.countDown();
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            latch.countDown();
                        }
                    });

            latch.await(timeoutSeconds, TimeUnit.SECONDS);
            return new String[]{ stdout.toString(), stderr.toString() };
        } catch (Exception e) {
            return new String[]{ "", e.getMessage() };
        }
    }

    private byte[] createSingleFileTar(String absolutePath, String content) throws IOException {
        // absolutePath e.g. "/workspace/src/Game.java"
        // Strip leading slash for tar entry name
        String entryName = absolutePath.startsWith("/") ? absolutePath.substring(1) : absolutePath;

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tar = new TarArchiveOutputStream(bos)) {
            tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            TarArchiveEntry entry = new TarArchiveEntry(entryName);
            entry.setSize(bytes.length);
            tar.putArchiveEntry(entry);
            tar.write(bytes);
            tar.closeArchiveEntry();
        }
        return bos.toByteArray();
    }
}
