package com.triage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record TriageReportRequest(
        @NotNull UUID faultEventId,
        @NotBlank String rootCause,
        @NotBlank String confidence,
        String affectedModule,
        String recommendation,
        Map<String, Object> rawAnalysis) {}
