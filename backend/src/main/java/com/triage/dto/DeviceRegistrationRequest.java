package com.triage.dto;

import jakarta.validation.constraints.NotBlank;

public record DeviceRegistrationRequest(
        @NotBlank String hostname,
        String osInfo,
        String agentVersion) {}
