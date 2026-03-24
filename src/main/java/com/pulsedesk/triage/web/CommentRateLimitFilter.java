package com.pulsedesk.triage.web;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.pulsedesk.triage.config.RateLimitProperties;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CommentRateLimitFilter extends OncePerRequestFilter {
  private final RateLimitProperties props;
  private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();
  private final AtomicInteger cleanupTicker = new AtomicInteger(0);

  public CommentRateLimitFilter(RateLimitProperties props) {
    this.props = props;
  }

  @Override
    protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    if (!"POST".equalsIgnoreCase(request.getMethod()) || !"/comments".equals(request.getRequestURI())) {
      filterChain.doFilter(request, response);
      return;
    }

    long now = Instant.now().getEpochSecond();
    maybeCleanup(now);
    String key = clientKey(request);
    WindowCounter wc = counters.compute(key, (k, old) -> {
      if (old == null || now >= old.windowStartEpochSec + props.windowSeconds()) {
        return new WindowCounter(now);
      }
      return old;
    });

    int used = wc.count.incrementAndGet();
    if (used > props.requestsPerWindow()) {
      response.setStatus(429);
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded for POST /comments\"}");
      return;
    }

    filterChain.doFilter(request, response);
  }

  private String clientKey(HttpServletRequest request) {
    String fwd = request.getHeader("X-Forwarded-For");
    if (fwd != null && !fwd.isBlank()) {
      return fwd.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

  private void maybeCleanup(long nowEpochSec) {
    if (cleanupTicker.incrementAndGet() % 200 != 0) return;
    long expiry = nowEpochSec - (props.windowSeconds() * 2L);
    counters.entrySet().removeIf(e -> e.getValue().windowStartEpochSec < expiry);
  }

  private static final class WindowCounter {
    private final long windowStartEpochSec;
    private final AtomicInteger count = new AtomicInteger(0);

    private WindowCounter(long windowStartEpochSec) {
      this.windowStartEpochSec = windowStartEpochSec;
    }
  }
}

