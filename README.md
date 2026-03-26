# PulseDesk Comment-to-Ticket Triage

PulseDesk is a small app that turns user comments into support tickets.

For full technical and architecture documentation, see [SYSTEM_DOCUMENTATION.md].

## Quick Setup

### Requirements

- Java 21+
- Maven 3.9+

### 1) Configure environment variables

Create a `.env.local` file in the project root folder:

```dotenv
HUGGINGFACE_API_TOKEN=hf_...
HUGGINGFACE_MODEL=openai/gpt-oss-20b:novita
HUGGINGFACE_BASE_URL=https://router.huggingface.co/hf-inference
HUGGINGFACE_TIMEOUT_MS=25000
```

Notes:

- If `HUGGINGFACE_API_TOKEN` has a value, the app uses AI for analysis.
- If `HUGGINGFACE_API_TOKEN` is empty, the app uses built-in rules instead of AI.

### 2) Start the application

```powershell
mvn spring-boot:run
```

App URL:

- `http://localhost:8080`

## How To Use

### UI

Open `http://localhost:8080` in your browser, fill in the form, and submit a comment.

### API

- `POST /comments`
- `GET /comments?page=0&size=20`
- `GET /tickets?page=0&size=20`
- `GET /tickets/{ticketId}`

## Common Configuration

Main settings are in `src/main/resources/application.yml`.

- `pulsedesk.processing.poll-delay-ms` (default: `250`)
- `pulsedesk.processing.retry-max-attempts` (default: `3`)
- `pulsedesk.processing.retry-delay-ms` (default: `10000`)
- `pulsedesk.rate-limit.requests-per-window` (default: `60`)
- `pulsedesk.rate-limit.window-seconds` (default: `60`)

## Troubleshooting

### AI analyzer not active

Set `HUGGINGFACE_API_TOKEN` and restart the app.

### Frequent AI timeouts

Increase `HUGGINGFACE_TIMEOUT_MS` (for example: `30000` or `45000`).

### H2 startup issues

Make sure the JDBC URL in `application.yml` is unchanged.
