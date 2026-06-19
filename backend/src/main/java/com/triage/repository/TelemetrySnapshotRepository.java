package com.triage.repository;

import com.triage.entity.TelemetrySnapshot;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TelemetrySnapshotRepository extends JpaRepository<TelemetrySnapshot, Long> {

    Optional<TelemetrySnapshot> findTopByDeviceIdOrderByCapturedAtDesc(UUID deviceId);

    @Query("SELECT t FROM TelemetrySnapshot t WHERE t.device.id = :deviceId " +
           "AND t.capturedAt >= :from AND t.capturedAt <= :to ORDER BY t.capturedAt ASC")
    List<TelemetrySnapshot> findByDeviceAndRange(@Param("deviceId") UUID deviceId,
                                                  @Param("from") OffsetDateTime from,
                                                  @Param("to") OffsetDateTime to,
                                                  Pageable pageable);
}
