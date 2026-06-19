package com.triage.service;

import com.triage.dto.FaultEventRequest;
import com.triage.dto.FaultEventResponse;
import com.triage.dto.FaultEventSummary;
import com.triage.dto.FaultStatusUpdateRequest;
import com.triage.entity.Device;
import com.triage.entity.FaultEvent;
import com.triage.exception.InvalidTransitionException;
import com.triage.exception.ResourceNotFoundException;
import com.triage.repository.DeviceRepository;
import com.triage.repository.FaultEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class FaultService {

    private static final Map<String, Set<String>> VALID_TRANSITIONS = Map.of(
            "pending",    Set.of("processing"),
            "processing", Set.of("done", "failed", "pending")
    );

    private final FaultEventRepository faultRepo;
    private final DeviceRepository deviceRepo;

    public FaultService(FaultEventRepository faultRepo, DeviceRepository deviceRepo) {
        this.faultRepo = faultRepo;
        this.deviceRepo = deviceRepo;
    }

    @Transactional
    public FaultEventSummary create(FaultEventRequest req) {
        Device device = deviceRepo.findById(req.deviceId())
                .orElseThrow(() -> new ResourceNotFoundException("Device " + req.deviceId() + " not found"));

        FaultEvent event = new FaultEvent();
        event.setId(UUID.randomUUID());
        event.setDevice(device);
        event.setOccurredAt(req.occurredAt());
        event.setFaultType(req.faultType());
        event.setProcessName(req.processName());
        event.setExitCode(req.exitCode());
        event.setRawLog(req.rawLog());
        event.setStatus("pending");
        event.setCreatedAt(OffsetDateTime.now());

        return toSummary(faultRepo.save(event));
    }

    public List<FaultEventSummary> list(String status, UUID deviceId, OffsetDateTime staleSince, int limit) {
        PageRequest page = PageRequest.of(0, limit);

        if (staleSince != null && "processing".equals(status)) {
            return faultRepo.findStale("processing", staleSince, page)
                    .stream().map(this::toSummary).toList();
        }
        if (deviceId != null && status != null) {
            return faultRepo.findByDeviceAndStatus(deviceId, status, page)
                    .stream().map(this::toSummary).toList();
        }
        if (deviceId != null) {
            return faultRepo.findByDeviceIdOrderByCreatedAtDesc(deviceId, page)
                    .stream().map(this::toSummary).toList();
        }
        if (status != null) {
            return faultRepo.findByStatusOrderByCreatedAtAsc(status, page)
                    .stream().map(this::toSummary).toList();
        }
        return faultRepo.findAll(page).stream().map(this::toSummary).toList();
    }

    public FaultEventResponse getById(UUID id) {
        FaultEvent event = faultRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FaultEvent " + id + " not found"));
        return toResponse(event);
    }

    @Transactional
    public FaultEventSummary updateStatus(UUID id, FaultStatusUpdateRequest req) {
        FaultEvent event = faultRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FaultEvent " + id + " not found"));

        String newStatus = req.status();
        Set<String> allowed = VALID_TRANSITIONS.getOrDefault(event.getStatus(), Set.of());
        if (!allowed.contains(newStatus)) {
            throw new InvalidTransitionException(event.getStatus(), newStatus);
        }

        event.setStatus(newStatus);
        if ("processing".equals(newStatus)) {
            event.setProcessingStartedAt(OffsetDateTime.now());
        }

        return toSummary(faultRepo.save(event));
    }

    private FaultEventSummary toSummary(FaultEvent e) {
        return new FaultEventSummary(e.getId(), e.getDevice().getId(), e.getOccurredAt(),
                e.getFaultType(), e.getProcessName(), e.getExitCode(), e.getStatus(), e.getCreatedAt());
    }

    private FaultEventResponse toResponse(FaultEvent e) {
        return new FaultEventResponse(e.getId(), e.getDevice().getId(), e.getOccurredAt(),
                e.getFaultType(), e.getProcessName(), e.getExitCode(), e.getRawLog(),
                e.getStatus(), e.getProcessingStartedAt(), e.getCreatedAt());
    }
}
