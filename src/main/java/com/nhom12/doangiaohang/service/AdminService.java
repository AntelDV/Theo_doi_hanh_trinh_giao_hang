package com.nhom12.doangiaohang.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhom12.doangiaohang.model.AuditLog;
import com.nhom12.doangiaohang.model.DonHang;
import com.nhom12.doangiaohang.model.NhatKyVanHanh;
import com.nhom12.doangiaohang.model.TaiKhoan;
import com.nhom12.doangiaohang.repository.AuditLogRepository;
import com.nhom12.doangiaohang.repository.DonHangRepository;
import com.nhom12.doangiaohang.repository.NhatKyVanHanhRepository;
import com.nhom12.doangiaohang.utils.EncryptionUtil;
import com.nhom12.doangiaohang.utils.RSAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
public class AdminService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private NhatKyVanHanhRepository nhatKyRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private DonHangRepository donHangRepository;

    @Autowired
    private EncryptionUtil encryptionUtil;

    @Autowired
    private RSAUtil rsaUtil;

    @Autowired
    private CustomUserHelper userHelper;

    @Autowired
    private SessionRegistry sessionRegistry;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemLogDTO {
        private Date thoiGian;
        private String user;
        private String hanhDong;
        private String chiTiet;
        private String styleClass;
    }

    public List<SystemLogDTO> getUnifiedLogs() {
        List<SystemLogDTO> unifiedList = new ArrayList<>();
        String adminPrivateKey = null;

        try {
            auditLogRepository.flushAuditLogs();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            TaiKhoan me = userHelper.getTaiKhoanHienTai(SecurityContextHolder.getContext().getAuthentication());
            if (me != null && me.getPrivateKey() != null) {
                adminPrivateKey = encryptionUtil.decrypt(me.getPrivateKey());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<NhatKyVanHanh> appLogs = nhatKyRepository.findAll(Sort.by(Sort.Direction.DESC, "thoiGianThucHien"));
        for (NhatKyVanHanh log : appLogs) {
            SystemLogDTO dto = new SystemLogDTO();
            dto.setThoiGian(log.getThoiGianThucHien());
            dto.setUser(log.getTaiKhoanThucHien() != null ? log.getTaiKhoanThucHien().getTenDangNhap() : "Unknown");
            dto.setHanhDong(log.getHanhDong());

            String chiTiet = log.getMoTaChiTiet();

            if (chiTiet != null && chiTiet.length() > 50 && adminPrivateKey != null) {
                try {
                    String decrypted = rsaUtil.decrypt(chiTiet, adminPrivateKey);
                    if (decrypted != null && !decrypted.equals(chiTiet)) {
                        chiTiet = decrypted;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            dto.setChiTiet(chiTiet);
            dto.setStyleClass("text-primary");
            unifiedList.add(dto);
        }

        try {
            List<AuditLog> dbLogs = auditLogRepository.findAll();
            for (AuditLog log : dbLogs) {
                SystemLogDTO dto = new SystemLogDTO();
                dto.setThoiGian(log.getThoiGian());
                dto.setUser(log.getUserDb());
                dto.setHanhDong(log.getHanhDong());
                dto.setChiTiet(log.getChiTiet() + " [Object: " + log.getDoiTuong() + "]");
                
                String style = "text-info";
                if (log.getHanhDong() != null) {
                    if (log.getHanhDong().contains("DELETE") || log.getHanhDong().contains("DROP")) {
                        style = "text-danger font-weight-bold";
                    } else if (log.getHanhDong().contains("UPDATE") || log.getHanhDong().contains("ALTER")) {
                        style = "text-warning";
                    }
                }
                dto.setStyleClass(style);
                unifiedList.add(dto);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        unifiedList.sort((o1, o2) -> {
            if (o1.getThoiGian() == null) return 1;
            if (o2.getThoiGian() == null) return -1;
            return o2.getThoiGian().compareTo(o1.getThoiGian());
        });

        return unifiedList;
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> getActiveSessions() {
        try {
            String sql = "SELECT SESSION_ID, USERNAME, HO_TEN, THOI_GIAN_LOGIN, IP_ADDRESS FROM THEO_DOI_ONLINE ORDER BY THOI_GIAN_LOGIN DESC";
            return entityManager.createNativeQuery(sql).getResultList();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Transactional
    public void killSession(String sessionId) {
        entityManager.createNativeQuery("DELETE FROM THEO_DOI_ONLINE WHERE SESSION_ID = :sid")
                .setParameter("sid", sessionId)
                .executeUpdate();

        List<Object> principals = sessionRegistry.getAllPrincipals();
        for (Object principal : principals) {
            if (principal instanceof User) {
                List<SessionInformation> sessions = sessionRegistry.getAllSessions(principal, false);
                for (SessionInformation sessionInfo : sessions) {
                    if (sessionInfo.getSessionId().equals(sessionId)) {
                        sessionInfo.expireNow();
                    }
                }
            }
        }
    }

    @Transactional
    public void backupData() {
        try {
            TaiKhoan admin = userHelper.getTaiKhoanHienTai(SecurityContextHolder.getContext().getAuthentication());
            Integer adminId = (admin != null) ? admin.getId() : 1;
            entityManager.createNativeQuery("BEGIN CSDL_NHOM12.PR_BACKUP_DATABASE(:uid); END;")
                    .setParameter("uid", adminId).executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public String exportDataToJson() {
        try {
            List<DonHang> allOrders = donHangRepository.findAll();
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(allOrders);
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    @Transactional
    public void restoreData(Integer minutes) {
        try {
            entityManager.createNativeQuery("BEGIN CSDL_NHOM12.PR_FLASHBACK_DATA(:minutes); END;")
                    .setParameter("minutes", minutes)
                    .executeUpdate();

            entityManager.clear();
            entityManager.getEntityManagerFactory().getCache().evictAll();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}