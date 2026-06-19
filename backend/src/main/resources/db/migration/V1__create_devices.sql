CREATE TABLE devices (
    id            UUID         PRIMARY KEY,
    hostname      TEXT         NOT NULL UNIQUE,
    os_info       TEXT,
    agent_version TEXT,
    registered_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_seen_at  TIMESTAMPTZ
);
