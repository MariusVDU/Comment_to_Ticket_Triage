package com.pulsedesk.triage.service.analysis;

import java.util.Locale;

import org.springframework.stereotype.Component;

import com.pulsedesk.triage.domain.TicketCategory;
import com.pulsedesk.triage.domain.TicketPriority;

@Component
public class HeuristicCommentAnalyzer implements CommentAnalyzer {
  @Override
  public TicketDraft analyze(String commentText) {
    String t = (commentText == null ? "" : commentText).toLowerCase(Locale.ROOT);

    boolean securityIssue =
      containsAny(t, "security", "vulnerability", "exploit", "breach", "hacked", "xss", "sql injection", "rce", "privilege escalation", "malware", "phishing");
    boolean incidentIssue =
      containsAny(t, "outage", "service down", "production down", "sev1", "sev 1", "p0", "critical incident", "system down", "major incident", "unavailable");
    boolean performanceIssue =
      containsAny(t, "slow", "latency", "timeout", "timed out", "lag", "degraded", "performance", "sluggish");
    boolean integrationIssue =
      containsAny(t, "webhook", "api integration", "integration", "sync failed", "oauth", "sso", "third-party", "third party", "connector", "zapier", "slack", "salesforce", "stripe api");
    boolean dataIssue =
      containsAny(t, "data loss", "lost data", "missing data", "wrong data", "duplicate data", "duplicated", "corrupt data", "data mismatch", "inconsistent data");
    boolean accessRequestIssue =
      containsAny(t, "grant access", "need access", "permission request", "role", "rbac", "invite user", "add user", "api key", "whitelist", "white list");
    boolean compliancePrivacyIssue =
      containsAny(t, "gdpr", "ccpa", "data deletion", "delete my data", "data export", "privacy request", "dpo", "compliance", "audit request", "consent");
    boolean contentAbuseIssue =
      containsAny(t, "spam", "abuse", "harassment", "hate speech", "scam", "fraud content", "report user", "impersonation", "inappropriate content");
    boolean usabilityIssue =
      containsAny(t, "confusing", "hard to use", "usability", "ux", "cannot find", "unclear", "workflow is confusing", "poor experience");
    boolean questionSupportIssue =
      containsAny(t, "how do i", "how to", "where can i", "what is the best way", "help me understand", "documentation", "setup help", "configuration help");

    boolean looksLikeIssue =
        containsAny(t, "bug", "error", "broken", "crash", "crashes", "cannot", "can't", "unable", "fails", "failure", "issue", "problem") ||
        containsAny(t, "refund", "charged", "billing", "invoice", "payment", "subscription") ||
      containsAny(t, "login", "sign in", "password", "account", "locked", "2fa") ||
      securityIssue || incidentIssue || performanceIssue || integrationIssue || dataIssue || accessRequestIssue
        || compliancePrivacyIssue || contentAbuseIssue || usabilityIssue || questionSupportIssue;

    if (!looksLikeIssue) {
      return new TicketDraft(false, null, null, null, null, "Heuristic: does not look like a support issue");
    }

    TicketCategory category =
      incidentIssue ? TicketCategory.INCIDENT :
      securityIssue ? TicketCategory.SECURITY :
      performanceIssue ? TicketCategory.PERFORMANCE :
      integrationIssue ? TicketCategory.INTEGRATION :
      dataIssue ? TicketCategory.DATA_ISSUE :
      accessRequestIssue ? TicketCategory.ACCESS_REQUEST :
      compliancePrivacyIssue ? TicketCategory.COMPLIANCE_PRIVACY :
      contentAbuseIssue ? TicketCategory.CONTENT_ABUSE :
      usabilityIssue ? TicketCategory.UX_USABILITY :
      questionSupportIssue ? TicketCategory.QUESTION_SUPPORT :
        containsAny(t, "refund", "charged", "billing", "invoice", "payment", "subscription") ? TicketCategory.BILLING :
        containsAny(t, "login", "sign in", "password", "account", "locked", "2fa") ? TicketCategory.ACCOUNT :
        containsAny(t, "feature", "request", "wish", "please add") ? TicketCategory.FEATURE :
        containsAny(t, "bug", "error", "broken", "crash", "fails", "failure") ? TicketCategory.BUG :
        TicketCategory.OTHER;

    boolean paymentFailureRisk =
        containsAny(t, "payment", "checkout", "purchase", "card", "charged", "billing", "invoice", "refund", "subscription") &&
            containsAny(t, "fail", "fails", "failed", "broken", "error", "declined", "not working", "cannot", "can't", "unable", "double charge", "charged twice")
      || containsAny(t, "charged twice", "double charge", "checkout failed", "payment failed", "card declined");

    boolean catastrophicOperationalRisk =
      containsAny(t,
        "system crash", "crash loop", "kernel panic", "panic", "fatal", "critical", "sev1", "sev 1", "p0", "priority 0",
        "outage", "service down", "production down", "down", "unavailable", "cannot start", "won't start",
        "data loss", "lost data", "data corruption", "corrupt data", "database corruption", "security vulnerability",
        "rce", "remote code execution", "privilege escalation", "incident")
        || (containsAny(t, "crash", "crashes", "crashed")
          && containsAny(t, "system", "service", "server", "production", "prod", "database", "application", "app"));

    boolean criticalSecurityExploitRisk =
        containsAny(t, "sql injection", "sqli");

    boolean criticalRisk = paymentFailureRisk
        || catastrophicOperationalRisk
        || criticalSecurityExploitRisk
        || category == TicketCategory.INCIDENT;

    boolean highRisk = containsAny(
      t,
      "fraud", "data leak", "breach", "security incident", "security vulnerability", "unauthorized access",
      "gdpr", "pci", "fine", "legal", "compliance", "regulatory", "lawsuit",
      "revenue loss", "lost revenue", "business risk", "churn"
    )
      || category == TicketCategory.SECURITY
      || category == TicketCategory.COMPLIANCE_PRIVACY;

    boolean moderateBusinessRisk =
        containsAny(t, "cannot login", "can't login", "unable to login", "account locked", "password reset", "2fa", "locked out") ||
      containsAny(t, "slow", "intermittent", "sometimes", "lag")
        || category == TicketCategory.PERFORMANCE
        || category == TicketCategory.INTEGRATION
        || category == TicketCategory.DATA_ISSUE;

    TicketPriority priority =
      criticalRisk ? TicketPriority.CRITICAL :
        highRisk ? TicketPriority.HIGH :
        moderateBusinessRisk ? TicketPriority.MEDIUM :
        TicketPriority.LOW;

    String title = switch (category) {
      case INCIDENT -> "Critical incident reported";
      case SECURITY -> "Security issue reported";
      case PERFORMANCE -> "Performance issue reported";
      case INTEGRATION -> "Integration issue reported";
      case DATA_ISSUE -> "Data issue reported";
      case ACCESS_REQUEST -> "Access request";
      case COMPLIANCE_PRIVACY -> "Compliance/privacy request";
      case CONTENT_ABUSE -> "Content abuse report";
      case UX_USABILITY -> "Usability issue reported";
      case QUESTION_SUPPORT -> "Support question";
      case BILLING -> "Billing issue reported";
      case ACCOUNT -> "Account access issue reported";
      case FEATURE -> "Feature request";
      case BUG -> "Bug reported";
      case OTHER -> "Support issue reported";
    };

    String sourceText = commentText == null ? "" : commentText;
    String summary = sourceText.length() > 400 ? sourceText.substring(0, 400) + "…" : sourceText;
    return new TicketDraft(true, title, category, priority, summary, "Heuristic: keyword-based classification");
  }

  private boolean containsAny(String haystack, String... needles) {
    for (String n : needles) {
      if (haystack.contains(n)) return true;
    }
    return false;
  }
}

