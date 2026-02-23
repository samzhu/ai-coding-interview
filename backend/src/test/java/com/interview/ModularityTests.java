package com.interview;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

@DisplayName("Spring Modulith 模組邊界驗證")
class ModularityTests {

    ApplicationModules modules = ApplicationModules.of(InterviewPlatformApplication.class);

    @Test
    @DisplayName("驗證所有模組邊界符合 Spring Modulith 規範")
    void verifyModularity() {
        modules.verify();
    }

    @Test
    @DisplayName("產生模組文件")
    void writeDocumentationSnippets() {
        new Documenter(modules)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml();
    }
}
