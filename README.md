# PulseDesk Comment-to-Ticket Triage

PulseDesk is a Spring Boot REST API that ingests user comments and triages them into support tickets.

The service stores comments and tickets in an embedded H2 database, analyzes comments with a Hugging Face-backed classifier, and creates tickets when the analysis decides follow-up is needed.

## What It Does

- Accepts comments through `POST /comments`
- Persists comments in H2 immediately
- Processes comments from the database queue in FIFO order
- Retries transient analysis failures automatically
- Creates tickets with category, priority, title, and summary
- Exposes paginated comment/ticket APIs and a basic UI (`/`)

## Runtime Flow

1. A comment is created with status `PENDING`.
2. A scheduled worker polls the database and processes one item at a time.
3. If analysis succeeds, status becomes `ANALYZED` and a ticket may be created.
4. If analysis fails but retries remain, status becomes `RETRYING`.
5. After max retry attempts, status becomes `FAILED`.

## Requirements

- Java 21+
- Maven 3.9+

## Quick Setup (Windows PowerShell)

### 1) Configure environment variables

Create a `.env` file in the project root:

```dotenv
HUGGINGFACE_API_TOKEN=hf_...
HUGGINGFACE_MODEL=openai/gpt-oss-20b:novita
HUGGINGFACE_BASE_URL=https://router.huggingface.co/hf-inference
HUGGINGFACE_TIMEOUT_MS=25000
```

Notes:

- If `HUGGINGFACE_API_TOKEN` is set, the app runs in strict AI mode (no heuristic fallback).
- If `HUGGINGFACE_API_TOKEN` is empty, the app falls back to heuristic analysis.

### 2) Start the application

```powershell
mvn spring-boot:run
```

App URL:

- `http://localhost:8080`

## API Endpoints

- `POST /comments`
- `GET /comments?page=0&size=20`
- `GET /tickets?page=0&size=20`
- `GET /tickets/{ticketId}`

### Example: Create a comment

```powershell
Invoke-RestMethod -Method Post "http://localhost:8080/comments" `
  -ContentType "application/json" `
  -Body (@{
    text = "Checkout fails and users are charged twice"
    source = "web"
    author = "jane@example.com"
    externalRef = "zendesk:12345"
  } | ConvertTo-Json)
```

## Configuration

Main settings are in `src/main/resources/application.yml`.

### Hugging Face

- `HUGGINGFACE_API_TOKEN`
- `HUGGINGFACE_MODEL`
- `HUGGINGFACE_BASE_URL`
- `HUGGINGFACE_TIMEOUT_MS`

### Processing Queue and Retry

- `pulsedesk.processing.poll-delay-ms`: scheduler polling interval (default `250` ms)
- `pulsedesk.processing.retry-max-attempts`: max analysis attempts per comment (default `3`)
- `pulsedesk.processing.retry-delay-ms`: delay between retries (default `10000` ms)

### Rate Limit

- `pulsedesk.rate-limit.requests-per-window`
- `pulsedesk.rate-limit.window-seconds`

## Data Storage

The app uses file-based embedded H2:

- JDBC URL: `jdbc:h2:file:./data/pulsedesk;MODE=PostgreSQL;DB_CLOSE_DELAY=-1`

This means data persists between restarts.

## Troubleshooting

### App fails at startup with H2 URL errors

Use the current JDBC URL in `application.yml`. H2 2.3.x does not support every old URL option combination.

### `No valid decision JSON found in model output`

The model response did not contain a parseable decision JSON with `createTicket`.
Typical causes are provider response format drift, partial outputs, or non-JSON content.

### Frequent timeouts

- Increase `HUGGINGFACE_TIMEOUT_MS`
- Keep `retry-max-attempts` and `retry-delay-ms` tuned for your traffic pattern
- Use a provider/model combination known to work in your region/network

## Development Notes

- Keep code comments and documentation in English.
- Do not commit real secrets in `.env`.
- `target/` build output is not part of runtime source.
