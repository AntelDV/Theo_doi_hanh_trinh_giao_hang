package com.nhom12.doangiaohang.repository;

import com.nhom12.doangiaohang.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Modifying
    @Transactional
    @Query(value = "BEGIN PR_FLUSH_AUDIT; END;", nativeQuery = true)
    void flushAuditLogs();
}