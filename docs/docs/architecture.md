# Architecture

## System Overview

The platform has five active components and one infrastructure layer:

```
┌────────────────────────────────────────────────────────────────────┐
│                          Edge Host(s)                              │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                  C++ Edge Client (daemon)                    │  │
│  │  Thread 1 — Telemetry collector  (CPU / mem / disk / net)   │  │
│  │  Thread 2 — Fault watcher        (crash signals, exit codes) │  │
│  │  Thread 3 — HTTP sender          (batches queue over REST)   │  │
│  └────────────────────────────┬─────────────────────────────────┘  │
└───────────────────────────────┼────────────────────────────────────┘
                                │ HTTPS  POST /api/telemetry
                                │        POST /api/faults
                                ▼
┌────────────────────────────────────────────────────────────────────┐
│                   Java Spring Boot Backend                         │
│                                                                    │
│  DeviceController   TelemetryController   FaultController          │
│  ReportController                                                  │
│             │                                                      │
│       ┌─────▼──────────────────────────────────┐                  │
│       │          PostgreSQL (RDS)               │                  │
│       │  devices  telemetry_snapshots           │                  │
│       │  fault_events  triage_reports           │                  │
│       └─────────────────────────────────────────┘                  │
└──────────┬─────────────────────────────▲──────────────────────────┘
           │  GET /api/faults             │  PATCH /api/faults/{id}/status
           │  ?status=pending             │  POST  /api/reports
           ▼                             │
┌──────────────────────┐    ┌────────────┴──────────────────────────┐
│  Python Triage Agent │    │  C# Watchdog Lambda (.NET 8)          │
│                      │    │  Trigger: EventBridge every 5 min     │
│  1. Poll pending     │    │  1. GET faults stuck in 'processing'   │
│  2. Parse crash log  │    │  2. PATCH → 'pending' (reset)          │
│  3. Analyze signals  │    │  3. CloudWatch metric + SNS alert      │
│  4. POST report      │    └───────────────────────────────────────┘
└──────────────────────┘
           ▲
           │  REST (reads only)
┌──────────┴─────────────────────────────────────────────────────────┐
│                     React UI Dashboard                             │
│   /devices   /telemetry/:deviceId   /faults   /reports/:id        │
└────────────────────────────────────────────────────────────────────┘
```

---

## Component Responsibilities

### edge-client (C++)
- Runs as a long-lived daemon on the monitored host
- Collects telemetry on a configurable interval (default: 15s)
- Watches for POSIX signals, abnormal process exits, OOM events
- Maintains an in-process send queue with retry logic
- Sends batched telemetry and individual fault events to the backend over HTTP

### backend (Java / Spring Boot)
- Single deployable JAR, exposed behind an ALB in production
- Owns device registration and last-seen tracking
- Receives and persists all telemetry and fault data
- Exposes read APIs for the UI and triage agent
- Does not call the triage agent directly — agent polls independently

### ai-triage-agent (Python)
- Stateless polling service; safe to restart at any time
- Claims fault events by PATCH-ing status to `processing`
- Parses raw crash logs: stack traces, signal names, OOM markers
- Applies pattern-matching rules to determine root cause and confidence
- Posts a structured report back to the backend and marks the event `done`
- On unhandled errors: marks the event `failed` and logs the reason

### watchdog-lambda (C# / .NET 8)
- Triggered by EventBridge on a 5-minute schedule
- Queries `GET /api/faults?status=processing&staleSince=<threshold>`
- Any event stuck in `processing` longer than the configured window (default: 10 min)
  is PATCH-ed back to `pending` so the triage agent retries it
- Publishes a `TriageSystem/StaleEvents` CloudWatch custom metric on every run
- Sends an SNS notification when the stale count exceeds a configurable threshold

### ui (React / TypeScript)
- Static SPA served by nginx in production
- Reads all data from the backend REST API
- No direct DB access; no backend-for-frontend layer

---

## Key Design Decisions

**No message broker.**
Fault events use a `status` column (`pending → processing → done/failed`) as a lightweight
work queue. This is sufficient at the expected event rate and removes an operational
dependency. A migration to SQS would be straightforward if throughput requirements grow.

**Pull-based triage agent.**
The agent polls rather than being pushed to. This keeps it stateless and decoupled.
The backend does not need to know the agent's address.

**Watchdog as a separate Lambda.**
Stale-job recovery is an operational concern, not a business logic concern. Keeping it in
a scheduled Lambda means it runs independently of both the backend and the triage agent,
and it can alert even if either of those services is degraded.

**Static API key auth.**
Sufficient for a controlled environment. The key is stored in Secrets Manager and injected
at runtime. JWT or mTLS are the natural next steps for a multi-tenant deployment.

**Raw logs in PostgreSQL.**
Crash logs are stored inline in `fault_events.raw_log` (TEXT). This simplifies Phase 2-4
development. At high volume, the raw_log column would be replaced with an S3 reference.

---

## Upgrade Paths (documented, not implemented)

| Current | Production upgrade |
|---------|-------------------|
| DB status-column queue | AWS SQS with dead-letter queue |
| Static API key | JWT with short expiry + refresh |
| Inline raw_log in Postgres | S3 object store with presigned URL in fault_events |
| Single-region ECS | Multi-region with Route 53 failover |
