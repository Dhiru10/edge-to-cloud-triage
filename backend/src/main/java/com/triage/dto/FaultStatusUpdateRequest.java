package com.triage.dto;

import jakarta.validation.constraints.NotBlank;

public record FaultStatusUpdateRequest(@NotBlank String status) {}
