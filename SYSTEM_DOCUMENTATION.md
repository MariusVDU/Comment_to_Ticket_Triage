# PulseDesk System Documentation (Simple Version)

This document explains how PulseDesk works in simple words.

If you are new to backend systems, this guide is for you.

## 1. What This System Does

PulseDesk accepts user comments and decides if a support ticket should be created.

Example:

- User comment: "Checkout failed and I was charged twice"
- System result: create a ticket with category and priority

In short:

- You send a comment
- The app analyzes it
- The app may create a ticket

## 2. Main Idea (Easy Flow)

1. A user sends a comment to the app.
2. The app saves it immediately.
3. A background worker picks comments one by one.
4. The worker analyzes each comment.
5. If needed, it creates a support ticket.
6. If analysis fails, it retries automatically.

## 3. Two Analysis Modes

PulseDesk can work in two modes.

### Mode A: AI Mode

- Used when `HUGGINGFACE_API_TOKEN` is set.
- The app asks Hugging Face to analyze comments.
- If AI fails in this mode, the app does not switch to rule mode.

### Mode B: Rule Mode

- Used when `HUGGINGFACE_API_TOKEN` is empty.
- The app uses built-in keyword and risk rules.
- No external AI call is needed.

## 4. Important Terms

- Comment: user text sent to the app.
- Ticket: a support item created by the app.
- Queue: waiting list of comments to process.
- Retry: try again after a failure.
- Category: ticket type (for example BUG or BILLING).
- Priority: urgency level (CRITICAL, HIGH, MEDIUM, LOW).

## 5. Comment Status (What You Will See)

Each comment has a status:

- `PENDING`: waiting for analysis.
- `RETRYING`: failed once, will try again.
- `ANALYZED`: analysis finished.
- `FAILED`: all retries were used and still failed.

## 6. API Endpoints (Simple Reference)

Base URL: `http://localhost:8080`

### Create a comment

- `POST /comments`

### List comments

- `GET /comments?page=0&size=20`

### List tickets

- `GET /tickets?page=0&size=20`

### Get one ticket

- `GET /tickets/{ticketId}`

## 7. Example Request

```json
{
  "text": "Checkout fails and users are charged twice",
  "source": "web",
  "author": "jane@example.com",
  "externalRef": "zendesk:12345"
}
```

## 8. What Happens After You Post a Comment

- API returns quickly with `201 Created`.
- The returned comment has status `PENDING`.
- Background worker analyzes it later.
- If the comment is actionable, a ticket is created.

This means ticket creation is asynchronous (not instant in the same request).

## 9. Categories Used for Tickets

The system uses these categories:

- INCIDENT
- SECURITY
- PERFORMANCE
- INTEGRATION
- DATA_ISSUE
- ACCESS_REQUEST
- COMPLIANCE_PRIVACY
- CONTENT_ABUSE
- UX_USABILITY
- QUESTION_SUPPORT
- BUG
- FEATURE
- BILLING
- ACCOUNT
- OTHER

## 10. Priority Levels

- `CRITICAL`: very urgent and high risk (for example outages, severe payment issues).
- `HIGH`: serious risk (security/legal/revenue impact).
- `MEDIUM`: important but not catastrophic.
- `LOW`: minor impact.

## 11. Retry Behavior

If analysis fails:

- The app retries automatically.
- Default max attempts: 3.
- Default retry delay: 10 seconds.

After max attempts:

- Status becomes `FAILED`.

## 12. Rate Limit (Protection)

To protect the app, `POST /comments` is limited.

Default:

- 60 requests per 60 seconds per client IP.

If limit is exceeded:

- API returns `429 Too Many Requests`.

## 13. Data Storage

The app uses an embedded H2 database in file mode.

Path:

- `./data/pulsedesk`

This means your data stays after restart.

## 14. UI Page

Open:

- `http://localhost:8080`

The page lets you:

- Submit new comments
- See latest comments
- See latest tickets
- Watch status changes live

## 15. Configuration (Most Important)

In `.env.local`:

```dotenv
HUGGINGFACE_API_TOKEN=hf_...
HUGGINGFACE_MODEL=openai/gpt-oss-20b:novita
HUGGINGFACE_BASE_URL=https://router.huggingface.co/hf-inference
HUGGINGFACE_TIMEOUT_MS=25000
```

In `src/main/resources/application.yml` (common settings):

- `pulsedesk.processing.poll-delay-ms`
- `pulsedesk.processing.retry-max-attempts`
- `pulsedesk.processing.retry-delay-ms`
- `pulsedesk.rate-limit.requests-per-window`
- `pulsedesk.rate-limit.window-seconds`

## 16. Errors You May See

### Validation errors (`400`)

Usually means request body is invalid.

Common reasons:

- `text` is empty
- `text` is too long

### Not found (`404`)

Usually means endpoint or ticket id does not exist.

### Too many requests (`429`)

You sent too many `POST /comments` requests quickly.

### Internal error (`500`)

Unexpected server issue.

## 17. Troubleshooting (Quick)

### AI not working

- Check `HUGGINGFACE_API_TOKEN` is set.
- Restart app after changing environment values.

### AI times out often

- Increase `HUGGINGFACE_TIMEOUT_MS`.

### H2 startup problems

- Keep JDBC URL as configured in `application.yml`.

### No ticket created

Possible reasons:

- Comment is not actionable.
- Comment is still pending.
- Analysis failed and is retrying.

Check comment status in `GET /comments`.

## 18. Security Notes

- Never commit real API tokens to Git.
- Keep `.env.local` private.
- Do not put secrets in comment text.

## 19. Where to Look in Code

Main locations:

- `src/main/java/com/pulsedesk/triage/api`
- `src/main/java/com/pulsedesk/triage/service`
- `src/main/java/com/pulsedesk/triage/service/analysis`
- `src/main/java/com/pulsedesk/triage/domain`
- `src/main/resources/application.yml`
- `src/main/resources/static/index.html`
