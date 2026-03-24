package com.pulsedesk.triage.service.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.pulsedesk.triage.config.HuggingFaceProperties;

@Component
@Primary
public class CompositeCommentAnalyzer implements CommentAnalyzer {
  private static final Logger log = LoggerFactory.getLogger(CompositeCommentAnalyzer.class);

  private final HuggingFaceProperties props;
  private final HuggingFaceCommentAnalyzer hf;
  private final HeuristicCommentAnalyzer heuristic;

  public CompositeCommentAnalyzer(HuggingFaceProperties props, HuggingFaceCommentAnalyzer hf, HeuristicCommentAnalyzer heuristic) {
    this.props = props;
    this.hf = hf;
    this.heuristic = heuristic;
  }

  @Override
  public TicketDraft analyze(String commentText) {
    if (props.apiToken() != null && !props.apiToken().isBlank()) {
      try {
        return hf.analyze(commentText);
      } catch (Exception e) {
        String hfReason = rootMessage(e);
        // Strict AI mode when token is configured: do not fall back to heuristics.
        log.warn("Hugging Face analysis failed; no heuristic fallback in strict AI mode: {}", hfReason);
        throw new IllegalStateException("Hugging Face analysis failed: " + hfReason, e);
      }
    }

    // Heuristic mode is used only when HF token is not configured.
    return heuristic.analyze(commentText);
  }

  private String rootMessage(Throwable t) {
    Throwable cur = t;
    while (cur != null && cur.getCause() != null && cur.getCause() != cur) {
      cur = cur.getCause();
    }
    String msg = cur == null ? null : cur.getMessage();
    return (msg == null || msg.isBlank()) ? "unknown error" : msg;
  }
}

