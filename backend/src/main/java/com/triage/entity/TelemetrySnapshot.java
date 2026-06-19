package com.triage.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "telemetry_snapshots")
public class TelemetrySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    private OffsetDateTime capturedAt;
    private BigDecimal cpuPct;
    private Integer memUsedMb;
    private Integer memTotalMb;
    private BigDecimal diskUsedGb;
    private BigDecimal diskTotalGb;
    private BigDecimal loadAvg1m;

    public Long getId() { return id; }
    public Device getDevice() { return device; }
    public void setDevice(Device device) { this.device = device; }
    public OffsetDateTime getCapturedAt() { return capturedAt; }
    public void setCapturedAt(OffsetDateTime capturedAt) { this.capturedAt = capturedAt; }
    public BigDecimal getCpuPct() { return cpuPct; }
    public void setCpuPct(BigDecimal cpuPct) { this.cpuPct = cpuPct; }
    public Integer getMemUsedMb() { return memUsedMb; }
    public void setMemUsedMb(Integer memUsedMb) { this.memUsedMb = memUsedMb; }
    public Integer getMemTotalMb() { return memTotalMb; }
    public void setMemTotalMb(Integer memTotalMb) { this.memTotalMb = memTotalMb; }
    public BigDecimal getDiskUsedGb() { return diskUsedGb; }
    public void setDiskUsedGb(BigDecimal diskUsedGb) { this.diskUsedGb = diskUsedGb; }
    public BigDecimal getDiskTotalGb() { return diskTotalGb; }
    public void setDiskTotalGb(BigDecimal diskTotalGb) { this.diskTotalGb = diskTotalGb; }
    public BigDecimal getLoadAvg1m() { return loadAvg1m; }
    public void setLoadAvg1m(BigDecimal loadAvg1m) { this.loadAvg1m = loadAvg1m; }
}
