package com.triage.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TelemetrySnapshotDto(
        OffsetDateTime capturedAt,
        BigDecimal cpuPct,
        Integer memUsedMb,
        Integer memTotalMb,
        BigDecimal diskUsedGb,
        BigDecimal diskTotalGb,
        BigDecimal loadAvg1m) {}
