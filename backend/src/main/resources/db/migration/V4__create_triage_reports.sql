CREATE TABLE triage_reports (
    id              UUID         PRIMARY KEY,
    fault_event_id  UUID         NOT NULL UNIQUE REFERENCES fault_events(id),
    analyzed_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    root_cause      TEXT,
    confidence      TEXT,
    affected_module TEXT,
    recommendation  TEXT,
    raw_analysis    JSONB
);
