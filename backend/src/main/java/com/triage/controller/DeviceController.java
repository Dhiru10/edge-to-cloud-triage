package com.triage.controller;

import com.triage.dto.DeviceDetailResponse;
import com.triage.dto.DeviceRegistrationRequest;
import com.triage.dto.DeviceResponse;
import com.triage.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping("/register")
    public ResponseEntity<DeviceResponse> register(@Valid @RequestBody DeviceRegistrationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(deviceService.register(req));
    }

    @GetMapping
    public ResponseEntity<List<DeviceResponse>> list() {
        return ResponseEntity.ok(deviceService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeviceDetailResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(deviceService.getById(id));
    }
}
