package com.interview.ai.interfaces.rest;

import com.interview.ai.AiModelInfo;
import com.interview.ai.AvailableModelsProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai/models")
class AiModelController {

    private final AvailableModelsProvider modelsProvider;

    AiModelController(AvailableModelsProvider modelsProvider) {
        this.modelsProvider = modelsProvider;
    }

    @GetMapping
    List<AiModelInfo> listModels() {
        return modelsProvider.getAvailableModels();
    }
}
