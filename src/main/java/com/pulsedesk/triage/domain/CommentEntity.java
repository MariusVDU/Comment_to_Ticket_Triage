package com.pulsedesk.triage.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "comments")
public class CommentEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 8000)
  private String text;

  @Column(nullable = false)
  private Instant createdAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private CommentAnalysisStatus analysisStatus;

  @Column(length = 64)
  private String source;

  @Column(length = 128)
  private String author;

  @Column(length = 256)
  private String externalRef;

  @Column
  private Long ticketId;

  @Column(length = 2000)
  private String analysisReason;

  @Column(nullable = false)
  private int analysisAttempts;

  @Column
  private Instant nextAttemptAt;

  protected CommentEntity() {}

  public CommentEntity(String text, String source, String author, String externalRef) {
    this.text = text;
    this.source = source;
    this.author = author;
    this.externalRef = externalRef;
    this.createdAt = Instant.now();
    this.analysisStatus = CommentAnalysisStatus.PENDING;
    this.analysisAttempts = 0;
    this.nextAttemptAt = null;
  }

  public Long getId() {
    return id;
  }

  public String getText() {
    return text;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public CommentAnalysisStatus getAnalysisStatus() {
    return analysisStatus;
  }

  public void setAnalysisStatus(CommentAnalysisStatus analysisStatus) {
    this.analysisStatus = analysisStatus;
  }

  public String getSource() {
    return source;
  }

  public String getAuthor() {
    return author;
  }

  public String getExternalRef() {
    return externalRef;
  }

  public Long getTicketId() {
    return ticketId;
  }

  public void setTicketId(Long ticketId) {
    this.ticketId = ticketId;
  }

  public String getAnalysisReason() {
    return analysisReason;
  }

  public void setAnalysisReason(String analysisReason) {
    this.analysisReason = analysisReason;
  }

  public int getAnalysisAttempts() {
    return analysisAttempts;
  }

  public void setAnalysisAttempts(int analysisAttempts) {
    this.analysisAttempts = analysisAttempts;
  }

  public Instant getNextAttemptAt() {
    return nextAttemptAt;
  }

  public void setNextAttemptAt(Instant nextAttemptAt) {
    this.nextAttemptAt = nextAttemptAt;
  }
}

