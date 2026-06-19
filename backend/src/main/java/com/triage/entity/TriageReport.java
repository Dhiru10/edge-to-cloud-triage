package com.triage.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "triage_reports")
public class TriageReport {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fault_event_id", nullable = false)
    private FaultEvent faultEvent;

    private OffsetDateTime analyzedAt;
    private String rootCause;
    private String confidence;
    private String affectedModule;
    private String recommendation;

    @Column(columnDefinition = "jsonb")
    private String rawAnalysis;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public FaultEvent getFaultEvent() { return faultEvent; }
    public void setFaultEvent(FaultEvent faultEvent) { this.faultEvent = faultEvent; }
    public OffsetDateTime getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(OffsetDateTime analyzedAt) { this.analyzedAt = analyzedAt; }
    public String getRootCause() { return rootCause; }
    public void setRootCause(String rootCause) { this.rootCause = rootCause; }
    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }
    public String getAffectedModule() { return affectedModule; }
    public void setAffectedModule(String affectedModule) { this.affectedModule = affectedModule; }
    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
    public String getRawAnalysis() { return rawAnalysis; }
    public void setRawAnalysis(String rawAnalysis) { this.rawAnalysis = rawAnalysis; }
}
