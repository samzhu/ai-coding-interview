package com.interview.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 從容器讀取並快取 exam.yml 配置。
 *
 * 約定：exam.yml 放在容器的 /workspace/exam.yml（預設路徑）。
 * exam.yml 是面試官準備映像檔的必要約定，找不到時拋出 ExamConfigNotFoundException。
 * 每個 containerId 只讀取一次，結束後呼叫 evict() 清除快取。
 */
@Service
public class ExamConfigService {

    private static final Logger log = LoggerFactory.getLogger(ExamConfigService.class);

    // exam.yml 始終從預設 workspace 路徑讀取，與 YAML 內的 workspace 欄位無關
    private static final String DEFAULT_EXAM_CONFIG_PATH =
            ExamConfig.DEFAULT_WORKSPACE + "/" + ExamConfig.CONFIG_FILENAME;

    private final ContainerService containerService;
    private final ObjectMapper yamlMapper;

    // 每個 containerId 只解析一次 exam.yml
    private final ConcurrentHashMap<String, ExamConfig> cache = new ConcurrentHashMap<>();

    public ExamConfigService(ContainerService containerService) {
        this.containerService = containerService;
        this.yamlMapper = YAMLMapper.builder().build();
    }

    /**
     * 取得容器的 exam.yml 配置（優先從快取，首次讀取並解析）。
     *
     * @throws ExamConfigNotFoundException 容器內找不到 exam.yml 或解析失敗
     */
    public ExamConfig getExamConfig(String containerId) {
        return cache.computeIfAbsent(containerId, this::loadExamConfig);
    }

    /**
     * 容器停止時清除快取（由 InterviewService 呼叫）。
     */
    public void evict(String containerId) {
        cache.remove(containerId);
        log.debug("Evicted exam config cache for container {}", containerId);
    }

    private ExamConfig loadExamConfig(String containerId) {
        String path = DEFAULT_EXAM_CONFIG_PATH;
        try {
            String yaml = containerService.readFile(containerId, path);
            if (yaml == null || yaml.isBlank()) {
                throw new ExamConfigNotFoundException(containerId, path);
            }
            ExamConfigYaml raw = yamlMapper.readValue(yaml, ExamConfigYaml.class);
            ExamConfig config = toExamConfig(raw);
            log.info("Loaded exam config for container {} from {}: {} checkpoints",
                    containerId, path, config.checkpoints().size());
            return config;
        } catch (ExamConfigNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new ExamConfigNotFoundException(containerId, path, e);
        }
    }

    private ExamConfig toExamConfig(ExamConfigYaml raw) {
        String workspace = raw.workspace() != null ? raw.workspace() : ExamConfig.DEFAULT_WORKSPACE;

        // 自動將 exam.yml 加入 exclude 清單（即使映像檔作者忘了排除）
        List<String> exclude = new ArrayList<>();
        if (raw.exclude() != null) {
            exclude.addAll(raw.exclude());
        }
        if (!exclude.contains(ExamConfig.CONFIG_FILENAME)) {
            exclude.add(ExamConfig.CONFIG_FILENAME);
        }

        List<ExamConfig.ExamCheckpoint> checkpoints = raw.checkpoints() == null
                ? List.of()
                : raw.checkpoints().stream()
                        .map(cp -> new ExamConfig.ExamCheckpoint(
                                cp.id(), cp.title(), cp.description(), cp.testCommand()))
                        .toList();

        return new ExamConfig(workspace, List.copyOf(exclude), checkpoints);
    }

    /**
     * YAML 反序列化用的內部 record（與 ExamConfig 分離，允許欄位為 null）。
     */
    record ExamConfigYaml(
            String workspace,
            List<String> exclude,
            List<CheckpointYaml> checkpoints) {

        record CheckpointYaml(
                String id,
                String title,
                String description,
                String testCommand) {}
    }
}
