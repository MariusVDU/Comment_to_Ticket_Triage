package com.pulsedesk.triage.api;

import com.pulsedesk.triage.api.dto.CommentResponse;
import com.pulsedesk.triage.api.dto.CreateCommentRequest;
import com.pulsedesk.triage.api.mapper.ApiMappers;
import com.pulsedesk.triage.repo.CommentRepository;
import com.pulsedesk.triage.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/comments")
public class CommentController {
  private final CommentService commentService;
  private final CommentRepository commentRepository;

  public CommentController(CommentService commentService, CommentRepository commentRepository) {
    this.commentService = commentService;
    this.commentRepository = commentRepository;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CommentResponse create(@Valid @RequestBody CreateCommentRequest req) {
    return ApiMappers.toResponse(commentService.createAndQueue(req.text(), req.source(), req.author(), req.externalRef()));
  }

  @GetMapping
  public Page<CommentResponse> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size
  ) {
    int boundedSize = Math.min(Math.max(1, size), 200);
    return commentRepository.findAll(PageRequest.of(Math.max(page, 0), boundedSize, Sort.by(Sort.Direction.DESC, "id")))
        .map(ApiMappers::toResponse);
  }
}

