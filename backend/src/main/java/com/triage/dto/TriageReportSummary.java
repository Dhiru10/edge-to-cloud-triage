package com.triage.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TriageReportSummary(
        UUID id,
        UUID faultEventId,
        OffsetDateTime analyzedAt,
        String rootCause,
        String confidence,
        String affectedModule) {}
