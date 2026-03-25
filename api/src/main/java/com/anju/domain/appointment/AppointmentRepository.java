package com.anju.domain.appointment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.time.LocalDateTime;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByStatusAndCreatedAtBefore(String status, LocalDateTime threshold);

        List<Appointment> findByStaffIdOrderByStartTimeDesc(Long staffId);

    boolean existsByNumber(String number);

    @Query("""
            SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
            FROM Appointment a
            WHERE (a.staffId = :staffId OR a.resourceId = :resourceId)
              AND a.status <> :excludedStatus
              AND :startTime < a.endTime
              AND :endTime > a.startTime
            """)
    boolean existsConflict(
            @Param("staffId") Long staffId,
            @Param("resourceId") Long resourceId,
            @Param("startTime") java.time.LocalDateTime startTime,
            @Param("endTime") java.time.LocalDateTime endTime,
            @Param("excludedStatus") String excludedStatus
    );
}
