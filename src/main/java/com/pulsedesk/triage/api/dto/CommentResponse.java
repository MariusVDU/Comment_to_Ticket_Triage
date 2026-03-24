package com.pulsedesk.triage.api.dto;

import com.pulsedesk.triage.domain.CommentAnalysisStatus;
import java.time.Instant;

public record CommentResponse(
    long id,
    String text,
    String source,
    String author,
    String externalRef,
    Instant createdAt,
    CommentAnalysisStatus analysisStatus,
    String analysisReason,
    Long ticketId
) {}

