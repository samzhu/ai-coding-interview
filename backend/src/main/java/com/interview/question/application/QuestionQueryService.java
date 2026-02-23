package com.interview.question.application;

import com.interview.question.CheckpointDetail;
import com.interview.question.ProjectFileDetail;
import com.interview.question.QuestionDetail;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class QuestionQueryService {

    final Map<UUID, QuestionDetail> questions = new LinkedHashMap<>();
    private final ResourcePatternResolver resolver;

    public QuestionQueryService(ResourcePatternResolver resolver) {
        this.resolver = resolver;
    }

    @PostConstruct
    void loadQuestions() throws IOException {
        ObjectMapper yamlMapper = YAMLMapper.builder().build();
        Resource[] yamls = resolver.getResources("classpath:questions/*/question.yml");
        for (Resource yaml : yamls) {
            QuestionDefinition def = yamlMapper.readValue(yaml.getInputStream(), QuestionDefinition.class);
            String basePath = "questions/" + def.id() + "/";

            List<ProjectFileDetail> files = new ArrayList<>();
            for (QuestionDefinition.ProjectFileDef pf : def.projectFiles()) {
                String content = loadResource(basePath + pf.filePath());
                files.add(new ProjectFileDetail(
                        UUID.randomUUID(),
                        pf.filePath(),
                        content,
                        pf.editable(),
                        pf.sortOrder()));
            }

            List<CheckpointDetail> checkpoints = new ArrayList<>();
            for (QuestionDefinition.CheckpointDef cp : def.checkpoints()) {
                checkpoints.add(new CheckpointDetail(
                        UUID.fromString(cp.id()),
                        cp.sequenceNumber(),
                        cp.title(),
                        cp.description(),
                        null,
                        cp.testCommand(),
                        cp.aiEnabled(),
                        List.of()));
            }

            QuestionDetail detail = new QuestionDetail(
                    UUID.fromString(def.id()),
                    def.title(),
                    def.description(),
                    def.difficulty(),
                    def.language(),
                    def.type(),
                    def.level(),
                    files,
                    checkpoints);

            questions.put(detail.id(), detail);
        }
    }

    private String loadResource(String path) throws IOException {
        Resource resource = resolver.getResource("classpath:" + path);
        try (InputStream is = resource.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public List<QuestionDetail> findAll() {
        return new ArrayList<>(questions.values());
    }

    public QuestionDetail findById(UUID id) {
        QuestionDetail detail = questions.get(id);
        if (detail == null) {
            throw new IllegalArgumentException("Question not found: " + id);
        }
        return detail;
    }
}
