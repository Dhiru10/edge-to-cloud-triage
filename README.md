# Edge-to-Cloud Telemetry & AI Triage System

![Backend CI](https://github.com/<your-username>/edge-to-cloud-triage/actions/workflows/backend-ci.yml/badge.svg)
![Edge Client CI](https://github.com/<your-username>/edge-to-cloud-triage/actions/workflows/edge-client-ci.yml/badge.svg)

A polyglot, multi-service platform that monitors edge hosts in real time, ships telemetry and crash signals to a cloud backend, and automatically triages fault events into structured root-cause reports — all deployable to AWS with a single `terraform apply`.

---

## The Problem It Solves

In distributed systems where software runs on many hosts — edge devices, servers, containers — two things go wrong constantly:

1. **Silent degradation** — CPU spikes, memory pressure, and disk exhaustion build up unnoticed until something crashes.
2. **Slow incident response** — when a crash does happen, engineers manually dig through logs to find the root cause.

This platform automates both: a lightweight daemon collects continuous telemetry from each host and immediately reports crashes; a backend persists and exposes the data; a Python agent parses the crash log and produces a structured root-cause report with a confidence score — all within seconds of the fault occurring.

---

## How It Works

```
Edge Host (C++ daemon)
  │  collects CPU/mem/disk/load every 15s
  │  detects process crashes and signals
  └──► REST POST ──► Spring Boot Backend (Java)
                          │  persists to PostgreSQL
                          │  exposes 13 REST endpoints
                          │
             ┌────────────┴──────────────┐
             ▼                           ▼
  Python Triage Agent            React UI Dashboard
  polls pending faults           devices · telemetry charts
  parses crash logs              fault events · triage reports
  produces root-cause report
             │
             └──► POST report back to backend
                          │
             ┌────────────┘
             ▼
  C# Watchdog Lambda (.NET 8)
  runs every 5 min via EventBridge
  resets stale triage jobs
  publishes CloudWatch metrics + SNS alerts
```

---

## Key Capabilities

| Capability | Implementation |
|---|---|
| **Real-time telemetry** | C++ daemon reads `/proc/stat`, `/proc/meminfo`, `statvfs` every 15s, batches over HTTP |
| **Crash detection** | Scans `/proc/<pid>/status` for zombie processes; captures signal name, exit code, raw log |
| **Automated triage** | Python agent parses GDB-style stack traces, detects null deref / OOM / heap corruption / use-after-free |
| **Confidence scoring** | Reports rated `high`, `medium`, or `low` based on indicator strength |
| **Stale job recovery** | C# Lambda resets orphaned `processing` events if the triage agent stalls |
| **Visual dashboard** | React SPA with Chart.js time-series charts, status-filtered fault table, expandable reports |
| **Cloud deployment** | ECS Fargate + RDS PostgreSQL Multi-AZ + Lambda — fully Terraform-managed |

---

## Tech Stack

| Layer | Technology | Role |
|---|---|---|
| Edge daemon | **C++17**, libcurl, nlohmann/json | Telemetry collection + fault detection |
| Backend API | **Java 21**, Spring Boot 3, Flyway | REST gateway + persistence |
| Database | **PostgreSQL 15** | Telemetry, fault events, triage reports |
| Triage agent | **Python 3.11**, requests | Crash log parsing + root-cause analysis |
| Ops watchdog | **C# .NET 8**, AWS Lambda | Scheduled stale-job recovery + alerting |
| Dashboard | **TypeScript**, React 18, Chart.js, Tailwind | Real-time monitoring UI |
| Infrastructure | **Terraform**, Docker, AWS ECS/Fargate | Cloud deployment + local dev stack |

---

## Repository Structure

```
edge-to-cloud-triage/
├── backend/            Java Spring Boot — REST API + DB persistence
├── edge-client/        C++ daemon — telemetry collector + fault watcher
├── ai-triage-agent/    Python — crash log parser + report generator
├── watchdog-lambda/    C# .NET 8 Lambda — stale job recovery
├── ui/                 React/TypeScript — monitoring dashboard
├── infra/              Terraform modules + docker-compose dev stack
└── docs/               Architecture, API reference, DB schema, runbook
```

---

## Running Locally

**Prerequisites:** Docker 24+, Docker Compose v2

```bash
cd infra
docker compose up --build
```

| Service | URL |
|---|---|
| UI | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| PostgreSQL | localhost:5432 |

All API calls require the header: `X-Api-Key: dev-api-key-change-in-prod`

---

## AWS Deployment

```bash
# 1. Push container images to ECR
# 2. Deploy infrastructure
cd infra/terraform && terraform init && terraform apply

# 3. Deploy watchdog Lambda
cd watchdog-lambda && sam build && sam deploy
```

See [`docs/runbook.md`](docs/runbook.md) for full deployment steps and secret management.

---

## Documentation

| Doc | Description |
|---|---|
| [`docs/architecture.md`](docs/architecture.md) | Component design, data flow, key decisions |
| [`docs/api-reference.md`](docs/api-reference.md) | All 13 REST endpoints with request/response shapes |
| [`docs/db-schema.md`](docs/db-schema.md) | Table definitions, indexes, fault status lifecycle |
| [`docs/runbook.md`](docs/runbook.md) | AWS deployment, secrets, monitoring, incident playbooks |

---

## Project Status

| Phase | Description | Status |
|---|---|---|
| 1 | Repo scaffold + architecture docs | ✅ Done |
| 2 | Spring Boot backend + PostgreSQL | ✅ Done |
| 3 | C++ edge client | ✅ Done |
| 4 | Python AI triage agent | ✅ Done |
| 5 | React UI dashboard | ✅ Done |
| 6 | AWS infra + C# watchdog + CI | ✅ Done |

---

## License

MIT
