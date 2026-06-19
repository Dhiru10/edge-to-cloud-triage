package com.triage.repository;

import com.triage.entity.FaultEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface FaultEventRepository extends JpaRepository<FaultEvent, UUID> {

    List<FaultEvent> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);

    List<FaultEvent> findByDeviceIdOrderByCreatedAtDesc(UUID deviceId, Pageable pageable);

    @Query("SELECT f FROM FaultEvent f WHERE f.status = :status " +
           "AND f.processingStartedAt < :before ORDER BY f.processingStartedAt ASC")
    List<FaultEvent> findStale(@Param("status") String status,
                                @Param("before") OffsetDateTime before,
                                Pageable pageable);

    @Query("SELECT f FROM FaultEvent f WHERE f.device.id = :deviceId " +
           "AND f.status = :status ORDER BY f.createdAt DESC")
    List<FaultEvent> findByDeviceAndStatus(@Param("deviceId") UUID deviceId,
                                            @Param("status") String status,
                                            Pageable pageable);
}
