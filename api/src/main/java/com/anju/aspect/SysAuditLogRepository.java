package com.anju.aspect;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SysAuditLogRepository extends JpaRepository<SysAuditLog, Long> {
}
