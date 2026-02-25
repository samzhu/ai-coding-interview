package com.interview.question.application;

import com.interview.question.CheckpointDetail;
import com.interview.question.QuestionDetail;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class QuestionQueryService {

    final Map<String, QuestionDetail> questions = new LinkedHashMap<>();
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

            List<CheckpointDetail> checkpoints = new ArrayList<>();
            List<QuestionDefinition.CheckpointDef> cpDefs = def.checkpoints();
            if (cpDefs != null) {
                for (int i = 0; i < cpDefs.size(); i++) {
                    QuestionDefinition.CheckpointDef cp = cpDefs.get(i);
                    checkpoints.add(new CheckpointDetail(
                            cp.id(),
                            i + 1,
                            cp.title(),
                            cp.description(),
                            null,
                            cp.testCommand(),
                            List.of()));
                }
            }

            QuestionDetail detail = new QuestionDetail(
                    def.id(),
                    def.title(),
                    def.description(),
                    def.difficulty(),
                    def.language(),
                    def.type(),
                    def.level(),
                    def.image(),
                    def.workspace(),
                    checkpoints);

            questions.put(detail.id(), detail);
        }
    }

    public List<QuestionDetail> findAll() {
        return new ArrayList<>(questions.values());
    }

    public QuestionDetail findById(String id) {
        QuestionDetail detail = questions.get(id);
        if (detail == null) {
            throw new IllegalArgumentException("Question not found: " + id);
        }
        return detail;
    }
}
