package com.pulsedesk.triage.service;

public class NotFoundException extends RuntimeException {
  public NotFoundException(String message) {
    super(message);
  }
}

