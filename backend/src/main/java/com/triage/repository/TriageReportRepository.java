package com.triage.repository;

import com.triage.entity.TriageReport;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TriageReportRepository extends JpaRepository<TriageReport, UUID> {

    Optional<TriageReport> findByFaultEventId(UUID faultEventId);

    boolean existsByFaultEventId(UUID faultEventId);

    List<TriageReport> findAllByOrderByAnalyzedAtDesc(Pageable pageable);
}
