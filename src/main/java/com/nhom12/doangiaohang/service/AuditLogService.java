package com.nhom12.doangiaohang.service;

import com.nhom12.doangiaohang.model.AuditLog;
import com.nhom12.doangiaohang.repository.AuditLogRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @PersistenceContext
    private EntityManager entityManager;
    @Transactional
    public List<AuditLog> getAllAuditLogs() {
        try {
            entityManager.createNativeQuery("BEGIN DBMS_AUDIT_MGMT.FLUSH_UNIFIED_AUDIT_TRAIL; END;").executeUpdate();
        } catch (Exception e) {
            System.err.println("Warning: Không thể flush audit trail. " + e.getMessage());
        }

        return auditLogRepository.findAllByOrderByThoiGianDesc();
    }
}