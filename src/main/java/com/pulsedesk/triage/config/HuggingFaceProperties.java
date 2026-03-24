package com.pulsedesk.triage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pulsedesk.huggingface")
public record HuggingFaceProperties(
    String baseUrl,
    String model,
    String apiToken,
    long timeoutMs
) {}

