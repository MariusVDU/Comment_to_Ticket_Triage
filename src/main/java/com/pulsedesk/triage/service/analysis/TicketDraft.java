package com.pulsedesk.triage.service.analysis;

import com.pulsedesk.triage.domain.TicketCategory;
import com.pulsedesk.triage.domain.TicketPriority;

public record TicketDraft(
    boolean createTicket,
    String title,
    TicketCategory category,
    TicketPriority priority,
    String summary,
    String reason
) {}

