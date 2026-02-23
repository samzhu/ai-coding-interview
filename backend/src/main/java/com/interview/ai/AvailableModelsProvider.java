package com.interview.ai;

import java.util.List;

public interface AvailableModelsProvider {

    List<AiModelInfo> getAvailableModels();

    boolean isValidModel(String modelId);
}
