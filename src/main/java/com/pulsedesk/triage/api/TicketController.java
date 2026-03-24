package com.pulsedesk.triage.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pulsedesk.triage.api.dto.TicketResponse;
import com.pulsedesk.triage.api.mapper.ApiMappers;
import com.pulsedesk.triage.domain.CommentEntity;
import com.pulsedesk.triage.domain.TicketEntity;
import com.pulsedesk.triage.repo.CommentRepository;
import com.pulsedesk.triage.repo.TicketRepository;
import com.pulsedesk.triage.service.NotFoundException;

@RestController
@RequestMapping("/tickets")
public class TicketController {
  private final TicketRepository tickets;
  private final CommentRepository comments;

  public TicketController(TicketRepository tickets, CommentRepository comments) {
    this.tickets = tickets;
    this.comments = comments;
  }

  @GetMapping
  public Page<TicketResponse> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size
  ) {
    int boundedSize = Math.min(Math.max(1, size), 200);
    return tickets.findAll(PageRequest.of(Math.max(page, 0), boundedSize, Sort.by(Sort.Direction.DESC, "id")))
      .map(this::toResponseWithCommentContext);
  }

  @GetMapping("/{ticketId}")
  public TicketResponse get(@PathVariable long ticketId) {
    return tickets.findById(ticketId)
        .map(this::toResponseWithCommentContext)
        .orElseThrow(() -> new NotFoundException("Ticket not found: " + ticketId));
  }

  private TicketResponse toResponseWithCommentContext(TicketEntity t) {
    CommentEntity comment = comments.findById(t.getCommentId()).orElse(null);
    String source = comment == null ? null : comment.getSource();
    String author = comment == null ? null : comment.getAuthor();
    return ApiMappers.toResponse(t, source, author);
  }
}

