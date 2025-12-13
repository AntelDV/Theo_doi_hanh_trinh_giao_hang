package com.nhom12.doangiaohang.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhom12.doangiaohang.model.DonHang;
import com.nhom12.doangiaohang.model.NhatKyVanHanh;
import com.nhom12.doangiaohang.model.TaiKhoan;
import com.nhom12.doangiaohang.repository.DonHangRepository;
import com.nhom12.doangiaohang.repository.NhatKyVanHanhRepository;
import com.nhom12.doangiaohang.utils.EncryptionUtil;
import com.nhom12.doangiaohang.utils.RSAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class AdminService {

    @PersistenceContext private EntityManager entityManager;
    @Autowired private NhatKyVanHanhRepository nhatKyRepository;
    @Autowired private DonHangRepository donHangRepository;
    @Autowired private EncryptionUtil encryptionUtil;
    @Autowired private RSAUtil rsaUtil;
    @Autowired private CustomUserHelper userHelper;
    @Autowired private SessionRegistry sessionRegistry; 
    @Autowired private NhatKyVanHanhService nhatKyService;

    @Data
    public static class SystemLogDTO {
        private Date thoiGian;
        private String user;
        private String hanhDong;
        private String nguon; 
        private String chiTiet;
        private String styleClass;
    }

    // --- CHỨC NĂNG LẤY LOG GỘP ---
    public List<SystemLogDTO> getUnifiedLogs() {

        List<SystemLogDTO> unifiedList = new ArrayList<>();
        String adminPrivateKey = null;
        try {
            TaiKhoan me = userHelper.getTaiKhoanHienTai(SecurityContextHolder.getContext().getAuthentication());
            if(me != null && me.getPrivateKey() != null) 
                adminPrivateKey = encryptionUtil.decrypt(me.getPrivateKey());
        } catch (Exception e) {}

        List<NhatKyVanHanh> appLogs = nhatKyRepository.findAll(Sort.by(Sort.Direction.DESC, "thoiGianThucHien"));
        for (NhatKyVanHanh log : appLogs) {
            SystemLogDTO dto = new SystemLogDTO();
            dto.setThoiGian(log.getThoiGianThucHien());
            dto.setUser(log.getTaiKhoanThucHien() != null ? log.getTaiKhoanThucHien().getTenDangNhap() : "Unknown");
            dto.setHanhDong(log.getHanhDong());
            dto.setNguon("WEB APP");
            String chiTiet = log.getMoTaChiTiet();
            if (chiTiet != null && chiTiet.length() > 50 && adminPrivateKey != null) {
                try { chiTiet = rsaUtil.decrypt(chiTiet, adminPrivateKey); } catch (Exception e) {}
            }
            dto.setChiTiet(chiTiet);
            dto.setStyleClass("text-primary");
            unifiedList.add(dto);
        }
        
        try {
            Query query = entityManager.createNativeQuery("SELECT THOI_GIAN, USER_DB, HANH_DONG, CHI_TIET FROM V_AUDIT_LOG_FULL FETCH FIRST 50 ROWS ONLY");
            List<Object[]> dbLogs = query.getResultList();
            for (Object[] row : dbLogs) {
                SystemLogDTO dto = new SystemLogDTO();
                dto.setThoiGian((Date) row[0]);
                dto.setUser((String) row[1]); 
                dto.setHanhDong((String) row[2]);
                dto.setNguon("ORACLE DB");
                dto.setChiTiet((String) row[3]); 
                dto.setStyleClass("text-danger font-weight-bold");
                unifiedList.add(dto);
            }
        } catch (Exception e) {}
        
        unifiedList.sort((o1, o2) -> {
            if (o1.getThoiGian() == null || o2.getThoiGian() == null) return 0;
            return o2.getThoiGian().compareTo(o1.getThoiGian());
        });
        return unifiedList;
    }

    // --- GIÁM SÁT & KICK  ---
    @SuppressWarnings("unchecked")
    public List<Object[]> getActiveSessions() {
        try {
            // Vẫn lấy từ DB để hiển thị danh sách
            String sql = "SELECT SESSION_ID, USERNAME, HO_TEN, THOI_GIAN_LOGIN, IP_ADDRESS FROM THEO_DOI_ONLINE ORDER BY THOI_GIAN_LOGIN DESC";
            return entityManager.createNativeQuery(sql).getResultList();
        } catch (Exception e) { return new ArrayList<>(); }
    }

    @Transactional
    public void killSession(String sessionId) {
        //  Xóa khỏi DB 
        entityManager.createNativeQuery("DELETE FROM THEO_DOI_ONLINE WHERE SESSION_ID = :sid")
                .setParameter("sid", sessionId)
                .executeUpdate();

        // Duyệt qua tất cả user đang login
        List<Object> principals = sessionRegistry.getAllPrincipals();
        for (Object principal : principals) {
            if (principal instanceof User) {
                List<SessionInformation> sessions = sessionRegistry.getAllSessions(principal, false);
                for (SessionInformation sessionInfo : sessions) {
                    if (sessionInfo.getSessionId().equals(sessionId)) {
                        sessionInfo.expireNow(); // Đánh dấu session hết hạn -> User bị đá ra ngay
                    }
                }
            }
        }
        
        // Ghi log hành động KICK
        try {
            TaiKhoan admin = userHelper.getTaiKhoanHienTai(SecurityContextHolder.getContext().getAuthentication());
            nhatKyService.logAction(admin, "KICK_USER", "SESSION", 0, "Đã ngắt kết nối session: " + sessionId);
        } catch (Exception e) {}
    }

    @Transactional
    public void backupData() {
        try {
            entityManager.createNativeQuery("BEGIN CSDL_NHOM12.PR_BACKUP_DATABASE; END;").executeUpdate();
        } catch (Exception e) { System.err.println("Lỗi gọi thủ tục backup: " + e.getMessage()); }
    }

    public String exportDataToJson() {
        try {
            List<DonHang> allOrders = donHangRepository.findAll();
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(allOrders);
        } catch (Exception e) { return "{\"error\": \"" + e.getMessage() + "\"}"; }
    }
    
    @Transactional
    public void restoreData(Integer minutes) {
        entityManager.createNativeQuery("BEGIN CSDL_NHOM12.PR_FLASHBACK_DATA(:minutes); END;")
                .setParameter("minutes", minutes).executeUpdate();
    }
}