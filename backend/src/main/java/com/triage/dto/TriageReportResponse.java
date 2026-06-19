package com.triage.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TriageReportResponse(
        UUID id,
        UUID faultEventId,
        OffsetDateTime analyzedAt,
        String rootCause,
        String confidence,
        String affectedModule,
        String recommendation,
        @JsonRawValue String rawAnalysis) {}
