import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
    java
    id("org.springframework.boot") version "4.0.3"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.interview"
version = "0.0.1-SNAPSHOT"
description = "AI Coding Interview Platform"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
}

extra["springModulithVersion"] = "2.0.2"
extra["springAiVersion"] = "2.0.0-M2"
extra["cucumberVersion"] = "7.20.1"

dependencies {
    // Spring Boot Web
    implementation("org.springframework.boot:spring-boot-starter-webmvc")

    // WebSocket — 互動式終端機需要雙向即時通訊，HTTP 長輪詢無法滿足
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    // Spring Data JDBC
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")

    // Validation
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Actuator
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // OpenTelemetry — 自動 traceId/spanId MDC 注入，log 可追蹤 request
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")

    // Spring Modulith
    implementation("org.springframework.modulith:spring-modulith-starter-core")
    implementation("org.springframework.modulith:spring-modulith-starter-jdbc")

    // Liquibase
    implementation("org.springframework.boot:spring-boot-starter-liquibase")

    // Database
    runtimeOnly("org.postgresql:postgresql")

    // Spring AI (non-starter: manual config, no auto-config compatibility issues with Spring Boot 4)
    implementation("org.springframework.ai:spring-ai-google-genai")
    implementation("org.springframework.ai:spring-ai-anthropic")
    implementation("org.springframework.ai:spring-ai-openai")
    implementation("org.springframework.ai:spring-ai-client-chat")

    // YAML support for question resource files
    implementation("tools.jackson.dataformat:jackson-dataformat-yaml")

    // docker-java (replace ProcessBuilder Docker CLI calls)
    implementation("com.github.docker-java:docker-java-core:3.5.3")
    implementation("com.github.docker-java:docker-java-transport-httpclient5:3.5.3")

    // Spring Security — OAuth2 resource server (JWT 驗證)
    // SPA 直接持有 access token，後端純 Resource Server，不需要 oauth2-client
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    // Development only
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    // Test — Spring Boot
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jdbc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-liquibase-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")

    // Test — Spring Modulith
    testImplementation("org.springframework.modulith:spring-modulith-starter-test")

    // Test — Testcontainers
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")

    // Test — Cucumber BDD
    testImplementation("io.cucumber:cucumber-java:${property("cucumberVersion")}")
    testImplementation("io.cucumber:cucumber-junit-platform-engine:${property("cucumberVersion")}")
    testImplementation("io.cucumber:cucumber-spring:${property("cucumberVersion")}")
    testImplementation("org.junit.platform:junit-platform-suite")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.modulith:spring-modulith-bom:${property("springModulithVersion")}")
        mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.jar { enabled = false }


tasks.named<BootBuildImage>("bootBuildImage") {
    imageName.set("ai-coding-interview-backend:latest")
    environment.put("BP_JVM_VERSION", "25")
}
