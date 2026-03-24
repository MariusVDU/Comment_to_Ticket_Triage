package com.pulsedesk.triage.api.mapper;

import com.pulsedesk.triage.api.dto.CommentResponse;
import com.pulsedesk.triage.api.dto.TicketResponse;
import com.pulsedesk.triage.domain.CommentEntity;
import com.pulsedesk.triage.domain.TicketEntity;

public final class ApiMappers {
  private ApiMappers() {}

  public static CommentResponse toResponse(CommentEntity c) {
    return new CommentResponse(
        c.getId(),
        c.getText(),
        c.getSource(),
        c.getAuthor(),
        c.getExternalRef(),
        c.getCreatedAt(),
        c.getAnalysisStatus(),
        c.getAnalysisReason(),
        c.getTicketId()
    );
  }

  public static TicketResponse toResponse(TicketEntity t) {
    return toResponse(t, null, null);
  }

  public static TicketResponse toResponse(TicketEntity t, String source, String author) {
    return new TicketResponse(
        t.getId(),
        t.getTitle(),
        t.getCategory(),
        t.getPriority(),
        t.getSummary(),
        source,
        author,
        t.getCreatedAt(),
        t.getCommentId()
    );
  }
}

