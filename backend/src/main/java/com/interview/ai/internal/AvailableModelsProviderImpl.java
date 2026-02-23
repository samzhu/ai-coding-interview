package com.interview.ai.internal;

import com.interview.ai.AiModelInfo;
import com.interview.ai.AvailableModelsProvider;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
class AvailableModelsProviderImpl implements AvailableModelsProvider {

    private final AiModelRegistry registry;

    AvailableModelsProviderImpl(AiModelRegistry registry) {
        this.registry = registry;
    }

    @Override
    public List<AiModelInfo> getAvailableModels() {
        return registry.getAvailableModels();
    }

    @Override
    public boolean isValidModel(String modelId) {
        return registry.getChatClient(modelId).isPresent();
    }
}
