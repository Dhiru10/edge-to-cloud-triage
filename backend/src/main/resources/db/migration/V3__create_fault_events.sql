CREATE TABLE fault_events (
    id                    UUID         PRIMARY KEY,
    device_id             UUID         NOT NULL REFERENCES devices(id),
    occurred_at           TIMESTAMPTZ  NOT NULL,
    fault_type            TEXT         NOT NULL,
    process_name          TEXT,
    exit_code             INTEGER,
    raw_log               TEXT,
    status                TEXT         NOT NULL DEFAULT 'pending',
    processing_started_at TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_fault_status   ON fault_events (status);
CREATE INDEX idx_fault_device   ON fault_events (device_id);
