# API Reference

Base URL (local): `http://localhost:8080`
Base URL (AWS):   `https://<alb-dns>/`

All requests require the header:
```
X-Api-Key: <configured-key>
```

Timestamps are ISO-8601 UTC strings. UUIDs are v4.

---

## Devices

### POST /api/devices/register
Register an edge device. Called by the edge client on startup.

**Request body**
```json
{
  "hostname": "prod-host-01",
  "osInfo": "Linux 6.1.0 x86_64",
  "agentVersion": "1.0.0"
}
```

**Response 201**
```json
{
  "id": "a3f2...",
  "hostname": "prod-host-01",
  "registeredAt": "2025-01-15T10:00:00Z"
}
```

---

### GET /api/devices
List all registered devices.

**Response 200**
```json
[
  {
    "id": "a3f2...",
    "hostname": "prod-host-01",
    "osInfo": "Linux 6.1.0 x86_64",
    "agentVersion": "1.0.0",
    "lastSeenAt": "2025-01-15T10:45:00Z"
  }
]
```

---

### GET /api/devices/{id}
Device detail with latest telemetry snapshot.

**Response 200**
```json
{
  "id": "a3f2...",
  "hostname": "prod-host-01",
  "lastSeenAt": "2025-01-15T10:45:00Z",
  "latestSnapshot": {
    "cpuPct": 34.2,
    "memUsedMb": 3120,
    "memTotalMb": 8192,
    "diskUsedGb": 42.1,
    "diskTotalGb": 200.0,
    "loadAvg1m": 1.45,
    "capturedAt": "2025-01-15T10:45:00Z"
  }
}
```

---

## Telemetry

### POST /api/telemetry
Ingest a batch of telemetry snapshots. Called by the edge client on each collection cycle.

**Request body**
```json
{
  "deviceId": "a3f2...",
  "snapshots": [
    {
      "capturedAt": "2025-01-15T10:45:00Z",
      "cpuPct": 34.2,
      "memUsedMb": 3120,
      "memTotalMb": 8192,
      "diskUsedGb": 42.1,
      "diskTotalGb": 200.0,
      "loadAvg1m": 1.45
    }
  ]
}
```

**Response 202** — accepted, no body.

---

### GET /api/telemetry/{deviceId}
Retrieve telemetry for a device within a time range.

**Query params**

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `from` | ISO timestamp | 1 hour ago | Start of window |
| `to`   | ISO timestamp | now        | End of window  |
| `limit`| integer       | 200        | Max records    |

**Response 200** — array of snapshot objects (same shape as POST body items).

---

## Faults

### POST /api/faults
Report a fault or crash event. Called by the edge client when a fault is detected.

**Request body**
```json
{
  "deviceId": "a3f2...",
  "occurredAt": "2025-01-15T10:46:12Z",
  "faultType": "SIGSEGV",
  "processName": "sensor-reader",
  "exitCode": 139,
  "rawLog": "Program received signal SIGSEGV\n#0  0x00007f... in parse_frame()\n..."
}
```

Supported `faultType` values: `SIGSEGV`, `SIGABRT`, `SIGBUS`, `OOM`, `PROCESS_EXIT`, `TIMEOUT`, `UNKNOWN`

**Response 201**
```json
{
  "id": "b9c1...",
  "status": "pending",
  "createdAt": "2025-01-15T10:46:12Z"
}
```

---

### GET /api/faults
List fault events with optional filters.

**Query params**

| Param | Type | Description |
|-------|------|-------------|
| `status` | string | Filter by status: `pending`, `processing`, `done`, `failed` |
| `deviceId` | UUID | Filter by device |
| `staleSince` | ISO timestamp | Return `processing` events with `processingStartedAt` before this time (used by watchdog) |
| `limit` | integer (default 50) | Max records |

**Response 200** — array of fault event summaries (no `rawLog` field).

---

### GET /api/faults/{id}
Full fault event detail, including `rawLog`.

**Response 200**
```json
{
  "id": "b9c1...",
  "deviceId": "a3f2...",
  "occurredAt": "2025-01-15T10:46:12Z",
  "faultType": "SIGSEGV",
  "processName": "sensor-reader",
  "exitCode": 139,
  "rawLog": "...",
  "status": "done",
  "processingStartedAt": "2025-01-15T10:46:15Z",
  "createdAt": "2025-01-15T10:46:12Z"
}
```

---

### PATCH /api/faults/{id}/status
Update the processing status of a fault event. Used by the triage agent and watchdog Lambda.

**Request body**
```json
{
  "status": "processing"
}
```

Valid transitions: `pending → processing`, `processing → done`, `processing → failed`, `processing → pending` (watchdog reset).

**Response 200** — updated fault event summary.

---

## Reports

### POST /api/reports
Submit a completed triage report. Called by the Python triage agent.

**Request body**
```json
{
  "faultEventId": "b9c1...",
  "rootCause": "Null pointer dereference in parse_frame() due to missing bounds check on input buffer.",
  "confidence": "high",
  "affectedModule": "sensor-reader/src/parser.c",
  "recommendation": "Add null check before dereferencing frame pointer at line 84. Add input length validation.",
  "rawAnalysis": {
    "signalName": "SIGSEGV",
    "faultAddress": "0x0000000000000000",
    "topFrame": "parse_frame",
    "matchedPattern": "null_deref"
  }
}
```

`confidence`: `high` | `medium` | `low`

**Response 201** — report object with assigned `id` and `analyzedAt`.

---

### GET /api/reports
List all triage reports.

**Query params**: `limit` (default 50), `faultEventId` (UUID, optional).

**Response 200** — array of report summaries (no `rawAnalysis` field).

---

### GET /api/reports/{id}
Full report detail including `rawAnalysis` JSON.

**Response 200** — full report object.

---

## Error Responses

All errors follow this shape:
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "FaultEvent b9c1... not found"
}
```

Common codes: `400` bad request / validation, `401` missing or invalid API key, `404` not found, `409` conflict (e.g. report already exists for fault), `500` internal error.
