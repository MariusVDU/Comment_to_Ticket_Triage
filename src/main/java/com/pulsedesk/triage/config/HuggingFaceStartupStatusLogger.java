package com.pulsedesk.triage.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class HuggingFaceStartupStatusLogger {
  private static final Logger log = LoggerFactory.getLogger(HuggingFaceStartupStatusLogger.class);

  private final HuggingFaceProperties props;

  public HuggingFaceStartupStatusLogger(HuggingFaceProperties props) {
    this.props = props;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void logStatus() {
    boolean enabled = props.apiToken() != null && !props.apiToken().isBlank();
    if (enabled) {
      log.info(
          "Hugging Face analyzer ENABLED (model='{}', baseUrl='{}', timeoutMs={})",
          props.model(),
          props.baseUrl(),
          props.timeoutMs());
      return;
    }

    log.warn(
        "Hugging Face analyzer DISABLED: pulsedesk.huggingface.api-token is empty. "
            + "Set HUGGINGFACE_API_TOKEN in environment or .env and restart the app.");
  }
}
