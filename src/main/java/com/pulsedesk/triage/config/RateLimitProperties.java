package com.pulsedesk.triage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pulsedesk.rate-limit")
public record RateLimitProperties(
    int requestsPerWindow,
    int windowSeconds
) {}

