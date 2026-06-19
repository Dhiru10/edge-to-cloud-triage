package com.triage.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FaultEventSummary(
        UUID id,
        UUID deviceId,
        OffsetDateTime occurredAt,
        String faultType,
        String processName,
        Integer exitCode,
        String status,
        OffsetDateTime createdAt) {}
