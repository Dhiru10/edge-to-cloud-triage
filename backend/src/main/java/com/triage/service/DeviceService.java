package com.triage.service;

import com.triage.dto.DeviceDetailResponse;
import com.triage.dto.DeviceRegistrationRequest;
import com.triage.dto.DeviceResponse;
import com.triage.dto.TelemetrySnapshotDto;
import com.triage.entity.Device;
import com.triage.exception.ResourceNotFoundException;
import com.triage.repository.DeviceRepository;
import com.triage.repository.TelemetrySnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepo;
    private final TelemetrySnapshotRepository snapshotRepo;

    public DeviceService(DeviceRepository deviceRepo, TelemetrySnapshotRepository snapshotRepo) {
        this.deviceRepo = deviceRepo;
        this.snapshotRepo = snapshotRepo;
    }

    @Transactional
    public DeviceResponse register(DeviceRegistrationRequest req) {
        Device device = deviceRepo.findByHostname(req.hostname()).orElseGet(() -> {
            Device d = new Device();
            d.setId(UUID.randomUUID());
            d.setRegisteredAt(OffsetDateTime.now());
            return d;
        });
        device.setHostname(req.hostname());
        device.setOsInfo(req.osInfo());
        device.setAgentVersion(req.agentVersion());
        device.setLastSeenAt(OffsetDateTime.now());
        return toResponse(deviceRepo.save(device));
    }

    public List<DeviceResponse> listAll() {
        return deviceRepo.findAll().stream().map(this::toResponse).toList();
    }

    public DeviceDetailResponse getById(UUID id) {
        Device device = deviceRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Device " + id + " not found"));

        TelemetrySnapshotDto latest = snapshotRepo
                .findTopByDeviceIdOrderByCapturedAtDesc(id)
                .map(s -> new TelemetrySnapshotDto(s.getCapturedAt(), s.getCpuPct(),
                        s.getMemUsedMb(), s.getMemTotalMb(),
                        s.getDiskUsedGb(), s.getDiskTotalGb(), s.getLoadAvg1m()))
                .orElse(null);

        return new DeviceDetailResponse(device.getId(), device.getHostname(), device.getOsInfo(),
                device.getAgentVersion(), device.getRegisteredAt(), device.getLastSeenAt(), latest);
    }

    private DeviceResponse toResponse(Device d) {
        return new DeviceResponse(d.getId(), d.getHostname(), d.getOsInfo(),
                d.getAgentVersion(), d.getRegisteredAt(), d.getLastSeenAt());
    }
}
