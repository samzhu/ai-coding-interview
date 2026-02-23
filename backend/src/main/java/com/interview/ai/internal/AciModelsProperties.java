package com.interview.ai.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "aci")
public record AciModelsProperties(List<ModelEntry> models) {

    public AciModelsProperties {
        if (models == null) {
            models = List.of();
        }
    }

    public record ModelEntry(
            String id,
            String name,
            String provider,
            String apiKey,
            Double temperature
    ) {}
}
