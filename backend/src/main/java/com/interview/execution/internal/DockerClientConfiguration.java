package com.interview.execution.internal;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.net.URI;
import java.time.Duration;

@Configuration
@Profile("!lab")
class DockerClientConfiguration {

    @Value("${execution.docker.host:tcp://localhost:2375}")
    private String dockerHost;

    @Value("${execution.docker.tls-verify:false}")
    private boolean tlsVerify;

    @Value("${execution.docker.cert-path:}")
    private String certPath;

    @Bean
    DockerClient dockerClient() {
        var configBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerHost)
                .withDockerTlsVerify(tlsVerify);

        if (certPath != null && !certPath.isBlank()) {
            configBuilder.withDockerCertPath(certPath);
        }

        var config = configBuilder.build();

        var httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(URI.create(dockerHost))
                .sslConfig(config.getSSLConfig())
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();

        return DockerClientImpl.getInstance(config, httpClient);
    }
}
