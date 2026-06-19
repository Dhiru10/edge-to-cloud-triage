package com.triage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FaultEventRequest(
        @NotNull UUID deviceId,
        @NotNull OffsetDateTime occurredAt,
        @NotBlank String faultType,
        String processName,
        Integer exitCode,
        String rawLog) {}
