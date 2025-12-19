package com.nhom12.doangiaohang.service;

import com.nhom12.doangiaohang.model.AuditLog;
import com.nhom12.doangiaohang.repository.AuditLogRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public List<AuditLog> getFullAuditLogs() {
        try {
            entityManager.createNativeQuery("BEGIN PR_FLUSH_AUDIT; END;").executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return auditLogRepository.findAll();
    }
}