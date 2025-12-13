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

    // Lấy danh sách Session đang online (Từ View V_GIAM_SAT_SESSION đã có trong SQL)
    @SuppressWarnings("unchecked")
    public List<Object[]> getActiveSessions() {
        try {
            // Lấy các cột: SID, SERIAL#, USER_WEB (Tên App), LOGON_TIME, STATUS
            String sql = "SELECT SID, SERIAL#, USER_WEB, LOGON_TIME, STATUS FROM CSDL_NHOM12.V_GIAM_SAT_SESSION";
            Query query = entityManager.createNativeQuery(sql);
            return query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // Chức năng: Đá user (Kill Session)
    @Transactional
    public void killSession(Integer sid, Integer serial) {
        // Gọi thủ tục PR_KILL_SESSION đã có trong SQL của bạn
        String sql = "BEGIN CSDL_NHOM12.PR_KILL_SESSION(:sid, :serial); END;";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("sid", sid);
        query.setParameter("serial", serial);
        query.executeUpdate();
    }

    // Chức năng: Khôi phục dữ liệu (Flashback)
    @Transactional
    public void restoreData(Integer minutes) {
        // Gọi thủ tục PR_FLASHBACK_DATA đã có trong SQL của bạn
        String sql = "BEGIN CSDL_NHOM12.PR_FLASHBACK_DATA(:minutes); END;";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("minutes", minutes);
        query.executeUpdate();
    }
}