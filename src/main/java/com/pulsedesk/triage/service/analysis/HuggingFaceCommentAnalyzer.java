package com.pulsedesk.triage.service.analysis;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsedesk.triage.config.HuggingFaceProperties;
import com.pulsedesk.triage.domain.TicketCategory;
import com.pulsedesk.triage.domain.TicketPriority;

@Component
public class HuggingFaceCommentAnalyzer implements CommentAnalyzer {
  private static final Logger log = LoggerFactory.getLogger(HuggingFaceCommentAnalyzer.class);
  private static final Duration MODEL_404_RETRY_DELAY = Duration.ofMinutes(15);
  private static final int REQUEST_ATTEMPTS = 2;
  private static final long MAX_REQUEST_TIMEOUT_MS = 45_000;
  private static final List<String> DEFAULT_FALLBACK_MODELS = List.of(
      "openai/gpt-oss-20b:novita",
      "openai/gpt-oss-20b:fireworks-ai",
      "openai/gpt-oss-20b:nscale",
      "openai/gpt-oss-20b:ovhcloud",
      "mistralai/Mistral-7B-Instruct-v0.2",
      "google/flan-t5-base",
      "HuggingFaceH4/zephyr-7b-beta"
  );

  private final HuggingFaceProperties props;
  private final ObjectMapper om;
  private final HttpClient http;
  private final ConcurrentMap<String, Instant> modelRetryAfter;

  public HuggingFaceCommentAnalyzer(HuggingFaceProperties props, ObjectMapper om) {
    this.props = props;
    this.om = om;
    this.http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(Math.max(1000, props.timeoutMs())))
        .build();
    this.modelRetryAfter = new ConcurrentHashMap<>();
  }

  @Override
  public TicketDraft analyze(String commentText) {
    if (props.apiToken() == null || props.apiToken().isBlank()) {
      throw new IllegalStateException("Hugging Face API token is not configured");
    }

    String prompt = """
You are an API that triages user comments into support tickets for a product called PulseDesk.
Return ONLY valid JSON (no markdown, no extra text).

Given the comment, decide if it should become a support ticket.

Rules:
- createTicket: true only if the comment indicates a problem, request, question needing support follow-up, billing/account issue, bug, or feature request.
- If it is praise/compliment with no actionable request, createTicket must be false.
- category must be one of: INCIDENT, SECURITY, PERFORMANCE, INTEGRATION, DATA_ISSUE, ACCESS_REQUEST, COMPLIANCE_PRIVACY, CONTENT_ABUSE, UX_USABILITY, QUESTION_SUPPORT, BUG, FEATURE, BILLING, ACCOUNT, OTHER
- priority must be one of: CRITICAL, HIGH, MEDIUM, LOW
- Priority policy follows Risk-Based Testing principles (impact x likelihood):
  - CRITICAL: severe impact or high-risk failure mode. Includes serious operational/system failures (e.g., system crash, outage, production down, data loss/corruption), payment-related failures (payment/checkout/billing failures, duplicate charges, charged twice, failed purchase flow), and exploitable injection vulnerabilities (e.g., SQL injection)
  - HIGH: likely revenue loss, or strong compliance/legal/security exposure that is serious but not yet clearly catastrophic (e.g., fraud risk, legal/compliance exposure, risk of fines)
  - MEDIUM: meaningful customer impact but not clearly catastrophic (e.g., account access/login issues, degraded performance, intermittent failures)
  - LOW: minor issues/requests with low immediate product/business risk
- title: short (max 120 chars)
- summary: short (max 400 chars), do not include secrets
- reason: 1 sentence explaining the decision

Output schema:
{"createTicket": boolean, "title": string|null, "category": "INCIDENT|SECURITY|PERFORMANCE|INTEGRATION|DATA_ISSUE|ACCESS_REQUEST|COMPLIANCE_PRIVACY|CONTENT_ABUSE|UX_USABILITY|QUESTION_SUPPORT|BUG|FEATURE|BILLING|ACCOUNT|OTHER"|null, "priority":"CRITICAL|HIGH|MEDIUM|LOW"|null, "summary": string|null, "reason": string}

Comment:
""".strip() + "\n" + commentText;

    try {
      Map<String, Object> payload = Map.of(
          "inputs", prompt,
          "parameters", Map.of("max_new_tokens", 220, "temperature", 0.2),
          "options", Map.of("wait_for_model", true)
      );

      String body = om.writeValueAsString(payload);
      String selectedModel = null;
      HttpResponse<String> resp = null;
      boolean usedChatCompletions = false;
      String lastModel404Error = null;
      String lastTransientError = null;
      List<String> triedModels = new ArrayList<>();

      for (String model : candidateModels()) {
        triedModels.add(model);
        URI uri = buildModelUri(model);
        try {
          resp = sendWithRetry(uri, body, model, "hf-inference");
        } catch (IOException e) {
          String ioReason = messageOf(e);
          lastTransientError = "HF transport error for model '" + model + "': " + ioReason;
          log.warn("HF request transport error for model '{}': {}. Trying next fallback model.", model, ioReason);
          continue;
        }

        if (resp.statusCode() == 404) {
          URI chatUri = buildChatCompletionsUri();
          String chatBody = buildChatCompletionsPayload(model, prompt);
          HttpResponse<String> chatResp;
          try {
            chatResp = sendWithRetry(chatUri, chatBody, model, "chat-completions");
          } catch (IOException e) {
            String ioReason = messageOf(e);
            lastTransientError = "HF chat transport error for model '" + model + "': " + ioReason;
            log.warn("HF chat transport error for model '{}': {}. Trying next fallback model.", model, ioReason);
            continue;
          }

          if (chatResp.statusCode() >= 200 && chatResp.statusCode() < 300) {
            selectedModel = model;
            resp = chatResp;
            usedChatCompletions = true;
            modelRetryAfter.remove(model);
            break;
          }

          if (chatResp.statusCode() == 404) {
            lastModel404Error = buildHttpErrorMessage(chatResp, model, chatUri);
            modelRetryAfter.put(model, Instant.now().plus(MODEL_404_RETRY_DELAY));
            log.warn(
                "Configured HF model unavailable on both endpoints: {}. Skipping this model for {} minute(s). Trying next fallback model.",
                model,
                MODEL_404_RETRY_DELAY.toMinutes());
            continue;
          }

          if (isTransientOrProviderStatus(chatResp.statusCode())) {
            String chatError = buildHttpErrorMessage(chatResp, model, chatUri);
            lastTransientError = chatError;
            log.warn("HF chat endpoint failed for model '{}': {}. Trying next fallback model.", model, chatError);
            continue;
          }

          throw new IllegalStateException(buildHttpErrorMessage(chatResp, model, chatUri));
        }

        if (resp.statusCode() >= 400) {
          if (isTransientOrProviderStatus(resp.statusCode())) {
            String endpointError = buildHttpErrorMessage(resp, model, uri);
            lastTransientError = endpointError;
            log.warn("HF endpoint failed for model '{}': {}. Trying next fallback model.", model, endpointError);
            continue;
          }
          throw new IllegalStateException(buildHttpErrorMessage(resp, model, uri));
        }

        selectedModel = model;
        modelRetryAfter.remove(model);
        break;
      }

      if (resp == null || selectedModel == null) {
        if (triedModels.isEmpty()) {
          throw new IllegalStateException(
              "All configured Hugging Face models are temporarily skipped after recent 404 responses. "
                  + "Wait for cooldown or configure a different model in HUGGINGFACE_MODEL.");
        }
        if (lastModel404Error != null) {
          throw new IllegalStateException(
              lastModel404Error + "; tried models=" + triedModels
                  + ". Configure a model your Hugging Face token can access (HUGGINGFACE_MODEL)."
          );
        }
        if (lastTransientError != null) {
          throw new IllegalStateException(lastTransientError + "; tried models=" + triedModels);
        }
        throw new IllegalStateException("Hugging Face request failed before receiving a response");
      }

      if (!selectedModel.equals(props.model())) {
        log.info("Using fallback Hugging Face model '{}' (configured '{}').", selectedModel, props.model());
      }

      String generated = extractGeneratedText(resp.body(), usedChatCompletions);

      Map<String, Object> parsed = parseDecisionJson(generated);

      boolean createTicket = Boolean.TRUE.equals(parsed.get("createTicket"));
      String title = asNullableString(parsed.get("title"));
      String summary = asNullableString(parsed.get("summary"));
      String reason = asNullableString(parsed.get("reason"));

      TicketCategory category = null;
      TicketPriority priority = null;
      if (createTicket) {
        category = TicketCategory.valueOf(String.valueOf(parsed.get("category")));
        priority = TicketPriority.valueOf(String.valueOf(parsed.get("priority")));
      }

      if (reason == null || reason.isBlank()) reason = "AI decision";
      if (title != null && title.length() > 160) title = title.substring(0, 160);
      if (summary != null && summary.length() > 1200) summary = summary.substring(0, 1200);

      return new TicketDraft(createTicket, title, category, priority, summary, reason);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      String detail = e.getMessage();
      if (detail == null || detail.isBlank()) {
        detail = e.getClass().getSimpleName();
      }
      throw new IllegalStateException("AI analysis failed: " + detail, e);
    } catch (IOException | RuntimeException e) {
      String detail = e.getMessage();
      if (detail == null || detail.isBlank()) {
        detail = e.getClass().getSimpleName();
      }
      throw new IllegalStateException("AI analysis failed: " + detail, e);
    }
  }

  private HttpRequest buildInferenceRequest(URI uri, String body, long timeoutMs) {
    return HttpRequest.newBuilder()
        .uri(uri)
        .timeout(Duration.ofMillis(Math.max(1000, timeoutMs)))
        .header("Authorization", "Bearer " + props.apiToken())
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();
  }

  private HttpResponse<String> sendWithRetry(URI uri, String body, String model, String endpoint)
      throws IOException, InterruptedException {
    long timeoutMs = Math.max(1000, props.timeoutMs());

    for (int attempt = 1; attempt <= REQUEST_ATTEMPTS; attempt++) {
      try {
        return http.send(buildInferenceRequest(uri, body, timeoutMs), HttpResponse.BodyHandlers.ofString());
      } catch (HttpTimeoutException e) {
        if (attempt >= REQUEST_ATTEMPTS) {
          throw e;
        }
        long nextTimeout = Math.min(MAX_REQUEST_TIMEOUT_MS, Math.max(timeoutMs + 2000, timeoutMs * 2));
        log.warn(
            "HF request timed out for model '{}' on {} (attempt {}/{} at {}ms). Retrying with {}ms.",
            model,
            endpoint,
            attempt,
            REQUEST_ATTEMPTS,
            timeoutMs,
            nextTimeout);
        timeoutMs = nextTimeout;
      }
    }

    throw new IOException("HF request failed after retry");
  }

  private boolean isTransientOrProviderStatus(int status) {
    return status == 403 || status == 408 || status == 429 || status >= 500;
  }

  private String messageOf(Throwable t) {
    String msg = t == null ? null : t.getMessage();
    if (msg == null || msg.isBlank()) {
      return t == null ? "unknown error" : t.getClass().getSimpleName();
    }
    return msg;
  }

  private String buildChatCompletionsPayload(String model, String prompt) throws IOException {
    Map<String, Object> payload = Map.of(
        "model", model,
        "messages", List.of(Map.of("role", "user", "content", prompt)),
        "temperature", 0.2,
        "max_tokens", 420
    );
    return om.writeValueAsString(payload);
  }

  private String extractGeneratedText(String responseBody, boolean chatCompletions) throws IOException {
    JsonNode root = om.readTree(responseBody);

    if (chatCompletions) {
      if (root.has("choices") && root.get("choices").isArray() && root.get("choices").size() > 0) {
        JsonNode message = root.get("choices").get(0).path("message");
        String content = message.path("content").asText("");
        if (content != null && !content.isBlank()) {
          return content;
        }
        String reasoning = message.path("reasoning_content").asText("");
        if (reasoning != null && !reasoning.isBlank()) {
          return reasoning;
        }
      }
      return responseBody;
    }

    // HF text2text returns: [{"generated_text":"..."}]
    if (root.isArray() && root.size() > 0 && root.get(0).has("generated_text")) {
      return root.get(0).get("generated_text").asText();
    }
    if (root.has("generated_text")) {
      return root.get("generated_text").asText();
    }
    return responseBody;
  }

  private String asNullableString(Object o) {
    if (o == null) return null;
    String s = String.valueOf(o);
    if ("null".equalsIgnoreCase(s)) return null;
    return s;
  }

  private List<String> candidateModels() {
    Set<String> ordered = new LinkedHashSet<>();
    if (props.model() != null && !props.model().isBlank()) {
      ordered.add(props.model().trim());
    }
    for (String model : DEFAULT_FALLBACK_MODELS) {
      if (model != null && !model.isBlank()) {
        ordered.add(model.trim());
      }
    }

    Instant now = Instant.now();
    List<String> available = new ArrayList<>();
    for (String model : ordered) {
      Instant retryAt = modelRetryAfter.get(model);
      if (retryAt != null && retryAt.isAfter(now)) {
        continue;
      }
      if (retryAt != null) {
        modelRetryAfter.remove(model, retryAt);
      }
      available.add(model);
    }
    return available;
  }

  private String buildHttpErrorMessage(HttpResponse<String> resp, String model, URI uri) {
    String responseBody = resp.body() == null ? "" : resp.body().trim();
    if (responseBody.length() > 280) {
      responseBody = responseBody.substring(0, 280) + "...";
    }
    String detail = responseBody.isEmpty() ? "" : (": " + responseBody);
    return "Hugging Face returned " + resp.statusCode()
        + " for model '" + model + "'"
        + " at " + uri
        + detail;
  }

  private URI buildModelUri(String model) {
    String base = props.baseUrl() == null ? "" : props.baseUrl().trim();
    while (base.endsWith("/")) {
      base = base.substring(0, base.length() - 1);
    }
    String encodedModel = URLEncoder.encode(model, StandardCharsets.UTF_8);
    return URI.create(base + "/models/" + encodedModel);
  }

  private URI buildChatCompletionsUri() {
    String base = props.baseUrl() == null ? "" : props.baseUrl().trim();
    while (base.endsWith("/")) {
      base = base.substring(0, base.length() - 1);
    }
    if (base.endsWith("/hf-inference")) {
      base = base.substring(0, base.length() - "/hf-inference".length());
    }
    if (base.endsWith("/v1")) {
      return URI.create(base + "/chat/completions");
    }
    return URI.create(base + "/v1/chat/completions");
  }

  private Map<String, Object> parseDecisionJson(String generated) {
    if (generated == null || generated.isBlank()) {
      throw new IllegalArgumentException("Empty model output");
    }

    String trimmed = generated.trim();
    try {
      Map<String, Object> parsed = om.readValue(trimmed, new TypeReference<>() {});
      if (parsed.containsKey("createTicket")) {
        return parsed;
      }
    } catch (IOException | RuntimeException ignored) {
      // Continue to extract embedded JSON objects from mixed model output.
    }

    int depth = 0;
    int start = -1;
    java.util.List<String> candidates = new java.util.ArrayList<>();
    for (int i = 0; i < generated.length(); i++) {
      char c = generated.charAt(i);
      if (c == '{') {
        if (depth == 0) {
          start = i;
        }
        depth++;
      } else if (c == '}') {
        if (depth > 0) {
          depth--;
          if (depth == 0 && start >= 0) {
            candidates.add(generated.substring(start, i + 1));
          }
        }
      }
    }

    for (int i = candidates.size() - 1; i >= 0; i--) {
      String candidate = candidates.get(i).trim();
      try {
        Map<String, Object> parsed = om.readValue(candidate, new TypeReference<>() {});
        if (parsed.containsKey("createTicket")) {
          return parsed;
        }
      } catch (IOException | RuntimeException ignored) {
        // Try previous candidate.
      }
    }

    throw new IllegalArgumentException("No valid decision JSON found in model output");
  }
}

