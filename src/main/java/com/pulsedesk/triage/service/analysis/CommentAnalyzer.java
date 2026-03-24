package com.pulsedesk.triage.service.analysis;

public interface CommentAnalyzer {
  TicketDraft analyze(String commentText);
}

