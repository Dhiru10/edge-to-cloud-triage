package com.triage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.triage.dto.TriageReportRequest;
import com.triage.dto.TriageReportResponse;
import com.triage.dto.TriageReportSummary;
import com.triage.entity.FaultEvent;
import com.triage.entity.TriageReport;
import com.triage.exception.ResourceNotFoundException;
import com.triage.repository.FaultEventRepository;
import com.triage.repository.TriageReportRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ReportService {

    private final TriageReportRepository reportRepo;
    private final FaultEventRepository faultRepo;
    private final ObjectMapper objectMapper;

    public ReportService(TriageReportRepository reportRepo,
                         FaultEventRepository faultRepo,
                         ObjectMapper objectMapper) {
        this.reportRepo = reportRepo;
        this.faultRepo = faultRepo;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TriageReportResponse create(TriageReportRequest req) {
        FaultEvent fault = faultRepo.findById(req.faultEventId())
                .orElseThrow(() -> new ResourceNotFoundException("FaultEvent " + req.faultEventId() + " not found"));

        if (reportRepo.existsByFaultEventId(req.faultEventId())) {
            throw new IllegalArgumentException("Report already exists for fault event " + req.faultEventId());
        }

        String rawJson = null;
        if (req.rawAnalysis() != null) {
            try {
                rawJson = objectMapper.writeValueAsString(req.rawAnalysis());
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Invalid rawAnalysis JSON");
            }
        }

        TriageReport report = new TriageReport();
        report.setId(UUID.randomUUID());
        report.setFaultEvent(fault);
        report.setAnalyzedAt(OffsetDateTime.now());
        report.setRootCause(req.rootCause());
        report.setConfidence(req.confidence());
        report.setAffectedModule(req.affectedModule());
        report.setRecommendation(req.recommendation());
        report.setRawAnalysis(rawJson);

        fault.setStatus("done");
        faultRepo.save(fault);

        return toResponse(reportRepo.save(report));
    }

    public List<TriageReportSummary> listAll(int limit) {
        return reportRepo.findAllByOrderByAnalyzedAtDesc(PageRequest.of(0, limit))
                .stream().map(this::toSummary).toList();
    }

    public TriageReportResponse getById(UUID id) {
        TriageReport report = reportRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TriageReport " + id + " not found"));
        return toResponse(report);
    }

    private TriageReportSummary toSummary(TriageReport r) {
        return new TriageReportSummary(r.getId(), r.getFaultEvent().getId(),
                r.getAnalyzedAt(), r.getRootCause(), r.getConfidence(), r.getAffectedModule());
    }

    private TriageReportResponse toResponse(TriageReport r) {
        return new TriageReportResponse(r.getId(), r.getFaultEvent().getId(),
                r.getAnalyzedAt(), r.getRootCause(), r.getConfidence(),
                r.getAffectedModule(), r.getRecommendation(), r.getRawAnalysis());
    }
}
