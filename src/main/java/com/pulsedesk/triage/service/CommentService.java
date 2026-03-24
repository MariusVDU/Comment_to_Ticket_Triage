package com.pulsedesk.triage.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pulsedesk.triage.domain.CommentEntity;
import com.pulsedesk.triage.repo.CommentRepository;

@Service
public class CommentService {
  private final CommentRepository comments;

  public CommentService(CommentRepository comments) {
    this.comments = comments;
  }

  @Transactional
  public CommentEntity createAndQueue(String text, String source, String author, String externalRef) {
    return comments.save(new CommentEntity(text, source, author, externalRef));
  }
}

