package com.triage.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record TelemetryIngestRequest(
        @NotNull UUID deviceId,
        @NotEmpty List<TelemetrySnapshotDto> snapshots) {}
