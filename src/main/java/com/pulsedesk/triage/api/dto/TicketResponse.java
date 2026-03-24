package com.pulsedesk.triage.api.dto;

import java.time.Instant;

import com.pulsedesk.triage.domain.TicketCategory;
import com.pulsedesk.triage.domain.TicketPriority;

public record TicketResponse(
    long id,
    String title,
    TicketCategory category,
    TicketPriority priority,
    String summary,
    String source,
    String author,
    Instant createdAt,
    long commentId
) {}

