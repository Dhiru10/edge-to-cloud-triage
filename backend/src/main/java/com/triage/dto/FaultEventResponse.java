package com.triage.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FaultEventResponse(
        UUID id,
        UUID deviceId,
        OffsetDateTime occurredAt,
        String faultType,
        String processName,
        Integer exitCode,
        String rawLog,
        String status,
        OffsetDateTime processingStartedAt,
        OffsetDateTime createdAt) {}
