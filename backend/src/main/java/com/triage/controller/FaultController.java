package com.triage.controller;

import com.triage.dto.FaultEventRequest;
import com.triage.dto.FaultEventResponse;
import com.triage.dto.FaultEventSummary;
import com.triage.dto.FaultStatusUpdateRequest;
import com.triage.service.FaultService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/faults")
public class FaultController {

    private final FaultService faultService;

    public FaultController(FaultService faultService) {
        this.faultService = faultService;
    }

    @PostMapping
    public ResponseEntity<FaultEventSummary> create(@Valid @RequestBody FaultEventRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(faultService.create(req));
    }

    @GetMapping
    public ResponseEntity<List<FaultEventSummary>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID deviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime staleSince,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(faultService.list(status, deviceId, staleSince, limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FaultEventResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(faultService.getById(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<FaultEventSummary> updateStatus(@PathVariable UUID id,
                                                           @Valid @RequestBody FaultStatusUpdateRequest req) {
        return ResponseEntity.ok(faultService.updateStatus(id, req));
    }
}
