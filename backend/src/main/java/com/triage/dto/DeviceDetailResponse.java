package com.triage.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DeviceDetailResponse(
        UUID id,
        String hostname,
        String osInfo,
        String agentVersion,
        OffsetDateTime registeredAt,
        OffsetDateTime lastSeenAt,
        TelemetrySnapshotDto latestSnapshot) {}
