package com.nhom12.doangiaohang.repository;

import com.nhom12.doangiaohang.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    List<AuditLog> findAllByOrderByThoiGianDesc();
    List<AuditLog> findByLoaiLogOrderByThoiGianDesc(String loaiLog);
    List<AuditLog> findByLoaiLogNotOrderByThoiGianDesc(String loaiLog);
    
    @Modifying
    @Transactional
    @Query(value = "BEGIN DBMS_AUDIT_MGMT.FLUSH_UNIFIED_AUDIT_TRAIL; END;", nativeQuery = true)
    void flushAuditLogs();
}