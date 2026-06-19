package com.triage.controller;

import com.triage.dto.TelemetryIngestRequest;
import com.triage.dto.TelemetrySnapshotDto;
import com.triage.service.TelemetryService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/telemetry")
public class TelemetryController {

    private final TelemetryService telemetryService;

    public TelemetryController(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @PostMapping
    public ResponseEntity<Void> ingest(@Valid @RequestBody TelemetryIngestRequest req) {
        telemetryService.ingest(req);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @GetMapping("/{deviceId}")
    public ResponseEntity<List<TelemetrySnapshotDto>> query(
            @PathVariable UUID deviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "200") int limit) {

        OffsetDateTime end = to != null ? to : OffsetDateTime.now();
        OffsetDateTime start = from != null ? from : end.minusHours(1);
        return ResponseEntity.ok(telemetryService.query(deviceId, start, end, limit));
    }
}
