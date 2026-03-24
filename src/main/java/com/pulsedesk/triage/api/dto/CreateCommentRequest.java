package com.pulsedesk.triage.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
    @NotBlank @Size(max = 8000) String text,
    @Size(max = 64) String source,
    @Size(max = 128) String author,
    @Size(max = 256) String externalRef
) {}

