package com.interview.execution.internal;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.net.URI;
import java.time.Duration;

@Configuration
@Profile("!lab")
class DockerClientConfiguration {

    @Bean
    DockerClient dockerClient(ExecutionDockerProperties properties) {
        String host = properties.host();
        // 部署架構為 Cloud Run → 內部網路 → VM Docker，不需 TLS
        var config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(host)
                .withDockerTlsVerify(false)
                .build();

        var httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(URI.create(host))
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();

        return DockerClientImpl.getInstance(config, httpClient);
    }
}
