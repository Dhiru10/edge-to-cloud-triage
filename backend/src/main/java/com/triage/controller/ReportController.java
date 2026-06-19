package com.triage.controller;

import com.triage.dto.TriageReportRequest;
import com.triage.dto.TriageReportResponse;
import com.triage.dto.TriageReportSummary;
import com.triage.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    public ResponseEntity<TriageReportResponse> create(@Valid @RequestBody TriageReportRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reportService.create(req));
    }

    @GetMapping
    public ResponseEntity<List<TriageReportSummary>> list(@RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(reportService.listAll(limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TriageReportResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(reportService.getById(id));
    }
}
