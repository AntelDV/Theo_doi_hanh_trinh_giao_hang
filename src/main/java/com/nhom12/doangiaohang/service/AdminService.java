package com.nhom12.doangiaohang.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdminService {

    @PersistenceContext
    private EntityManager entityManager;

    // Lấy Session Online
    @SuppressWarnings("unchecked")
    public List<Object[]> getActiveSessions() {
        try {
            String sql = "SELECT SID, SERIAL#, USER_WEB, LOGON_TIME, STATUS FROM CSDL_NHOM12.V_GIAM_SAT_SESSION";
            Query query = entityManager.createNativeQuery(sql);
            return query.getResultList();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // Kill Session
    @Transactional
    public void killSession(Integer sid, Integer serial) {
        String sql = "BEGIN CSDL_NHOM12.PR_KILL_SESSION(:sid, :serial); END;";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("sid", sid);
        query.setParameter("serial", serial);
        query.executeUpdate();
    }

    // Restore Data
    @Transactional
    public void restoreData(Integer minutes) {
        String sql = "BEGIN CSDL_NHOM12.PR_FLASHBACK_DATA(:minutes); END;";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("minutes", minutes);
        query.executeUpdate();
    }
    
    @SuppressWarnings("unchecked")
    public List<Object[]> getUnifiedAuditLog() {
        try {
            String sql = "SELECT THOI_GIAN, USER_WEB, HANH_DONG, DOI_TUONG, CHI_TIET FROM CSDL_NHOM12.V_AUDIT_LOG_FULL FETCH FIRST 100 ROWS ONLY";
            Query query = entityManager.createNativeQuery(sql);
            return query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Transactional
    public void generateDummyData() {
        String sql = "BEGIN CSDL_NHOM12.PR_GENERATE_DUMMY_DATA; END;";
        Query query = entityManager.createNativeQuery(sql);
        query.executeUpdate();
    }
}