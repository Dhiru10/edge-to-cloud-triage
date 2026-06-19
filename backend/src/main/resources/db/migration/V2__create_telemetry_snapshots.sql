CREATE TABLE telemetry_snapshots (
    id            BIGSERIAL    PRIMARY KEY,
    device_id     UUID         NOT NULL REFERENCES devices(id),
    captured_at   TIMESTAMPTZ  NOT NULL,
    cpu_pct       NUMERIC(5,2),
    mem_used_mb   INTEGER,
    mem_total_mb  INTEGER,
    disk_used_gb  NUMERIC(8,2),
    disk_total_gb NUMERIC(8,2),
    load_avg_1m   NUMERIC(6,3)
);

CREATE INDEX idx_telemetry_device_time ON telemetry_snapshots (device_id, captured_at DESC);
