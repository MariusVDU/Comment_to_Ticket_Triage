package com.pulsedesk.triage.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "tickets")
public class TicketEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 160)
  private String title;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private TicketCategory category;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private TicketPriority priority;

  @Column(nullable = false, length = 1200)
  private String summary;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Long commentId;

  protected TicketEntity() {}

  public TicketEntity(String title, TicketCategory category, TicketPriority priority, String summary, Long commentId) {
    this.title = title;
    this.category = category;
    this.priority = priority;
    this.summary = summary;
    this.commentId = commentId;
    this.createdAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public TicketCategory getCategory() {
    return category;
  }

  public TicketPriority getPriority() {
    return priority;
  }

  public String getSummary() {
    return summary;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Long getCommentId() {
    return commentId;
  }
}

