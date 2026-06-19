package com.triage.service;

import com.triage.dto.TelemetryIngestRequest;
import com.triage.dto.TelemetrySnapshotDto;
import com.triage.entity.Device;
import com.triage.entity.TelemetrySnapshot;
import com.triage.exception.ResourceNotFoundException;
import com.triage.repository.DeviceRepository;
import com.triage.repository.TelemetrySnapshotRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TelemetryService {

    private final TelemetrySnapshotRepository snapshotRepo;
    private final DeviceRepository deviceRepo;

    public TelemetryService(TelemetrySnapshotRepository snapshotRepo, DeviceRepository deviceRepo) {
        this.snapshotRepo = snapshotRepo;
        this.deviceRepo = deviceRepo;
    }

    @Transactional
    public void ingest(TelemetryIngestRequest req) {
        Device device = deviceRepo.findById(req.deviceId())
                .orElseThrow(() -> new ResourceNotFoundException("Device " + req.deviceId() + " not found"));

        List<TelemetrySnapshot> snapshots = req.snapshots().stream().map(dto -> {
            TelemetrySnapshot s = new TelemetrySnapshot();
            s.setDevice(device);
            s.setCapturedAt(dto.capturedAt());
            s.setCpuPct(dto.cpuPct());
            s.setMemUsedMb(dto.memUsedMb());
            s.setMemTotalMb(dto.memTotalMb());
            s.setDiskUsedGb(dto.diskUsedGb());
            s.setDiskTotalGb(dto.diskTotalGb());
            s.setLoadAvg1m(dto.loadAvg1m());
            return s;
        }).toList();

        snapshotRepo.saveAll(snapshots);
        device.setLastSeenAt(OffsetDateTime.now());
        deviceRepo.save(device);
    }

    public List<TelemetrySnapshotDto> query(UUID deviceId, OffsetDateTime from, OffsetDateTime to, int limit) {
        if (!deviceRepo.existsById(deviceId)) {
            throw new ResourceNotFoundException("Device " + deviceId + " not found");
        }
        return snapshotRepo.findByDeviceAndRange(deviceId, from, to, PageRequest.of(0, limit))
                .stream()
                .map(s -> new TelemetrySnapshotDto(s.getCapturedAt(), s.getCpuPct(),
                        s.getMemUsedMb(), s.getMemTotalMb(),
                        s.getDiskUsedGb(), s.getDiskTotalGb(), s.getLoadAvg1m()))
                .toList();
    }
}
