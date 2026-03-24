package com.pulsedesk.triage.service;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pulsedesk.triage.domain.CommentAnalysisStatus;
import com.pulsedesk.triage.domain.CommentEntity;
import com.pulsedesk.triage.domain.TicketEntity;
import com.pulsedesk.triage.repo.CommentRepository;
import com.pulsedesk.triage.repo.TicketRepository;
import com.pulsedesk.triage.service.analysis.CommentAnalyzer;
import com.pulsedesk.triage.service.analysis.TicketDraft;

@Service
public class CommentAnalysisProcessor {
  private final CommentRepository comments;
  private final TicketRepository tickets;
  private final CommentAnalyzer analyzer;

  @Value("${pulsedesk.processing.retry-max-attempts:3}")
  private int retryMaxAttempts;

  @Value("${pulsedesk.processing.retry-delay-ms:30000}")
  private long retryDelayMs;

  public CommentAnalysisProcessor(CommentRepository comments, TicketRepository tickets, CommentAnalyzer analyzer) {
    this.comments = comments;
    this.tickets = tickets;
    this.analyzer = analyzer;
  }

  @Transactional
  @Scheduled(fixedDelayString = "${pulsedesk.processing.poll-delay-ms:250}")
  public void processNextPending() {
    Instant now = Instant.now();
    CommentEntity c = comments.findFirstByAnalysisStatusOrderByCreatedAtAscIdAsc(CommentAnalysisStatus.PENDING)
        .orElseGet(() -> comments
            .findFirstByAnalysisStatusAndAnalysisAttemptsLessThanAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAscIdAsc(
          CommentAnalysisStatus.RETRYING,
                Math.max(1, retryMaxAttempts),
                now
            )
            .orElse(null));

    if (c == null) {
      return;
    }

    int attempt = c.getAnalysisAttempts() + 1;
    c.setAnalysisAttempts(attempt);
    c.setNextAttemptAt(null);

    try {
      TicketDraft draft = analyzer.analyze(c.getText());
      c.setAnalysisStatus(CommentAnalysisStatus.ANALYZED);
      c.setAnalysisReason(draft.reason());

      if (draft.createTicket()) {
        TicketEntity t = tickets.save(new TicketEntity(
            safeNonBlank(draft.title(), "Support request"),
            draft.category(),
            draft.priority(),
            safeNonBlank(draft.summary(), summarize(c.getText())),
            c.getId()
        ));
        c.setTicketId(t.getId());
      }
    } catch (Exception e) {
      String reason = e.getMessage();
      if (reason == null || reason.isBlank()) {
        reason = "Analysis failed";
      }
      if (reason.length() > 300) {
        reason = reason.substring(0, 300) + "...";
      }
      if (attempt >= Math.max(1, retryMaxAttempts)) {
        c.setAnalysisStatus(CommentAnalysisStatus.FAILED);
        c.setAnalysisReason("Analysis failed after " + attempt + " attempts: " + reason);
        c.setNextAttemptAt(null);
      } else {
        c.setAnalysisStatus(CommentAnalysisStatus.RETRYING);
        long delayMs = Math.max(1000, retryDelayMs);
        c.setAnalysisReason(
            "Analysis failed (attempt " + attempt + "/" + retryMaxAttempts + "): " + reason
                + ". Will retry in " + delayMs + " ms."
        );
        c.setNextAttemptAt(Instant.now().plusMillis(delayMs));
      }
    }

    comments.save(c);
  }

  private String summarize(String text) {
    if (text == null) return "";
    return text.length() > 400 ? text.substring(0, 400) + "…" : text;
  }

  private String safeNonBlank(String value, String fallback) {
    if (value == null) return fallback;
    String s = value.trim();
    return s.isEmpty() ? fallback : s;
  }
}

