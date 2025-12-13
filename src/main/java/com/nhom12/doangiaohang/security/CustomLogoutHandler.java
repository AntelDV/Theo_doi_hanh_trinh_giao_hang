package com.nhom12.doangiaohang.security;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CustomLogoutHandler implements LogoutHandler {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            try {
                // Gọi thủ tục Oracle để xóa Context bảo mật
                // Điều này ngăn chặn lỗi Connection Pool (Người sau dùng lại kết nối của người trước)
                String sql = "BEGIN CSDL_NHOM12.PKG_SECURITY.CLEAR_CONTEXT; END;";
                Query query = entityManager.createNativeQuery(sql);
                query.executeUpdate();
                
                System.out.println(">> [LOGOUT SUCCESS] Đã dọn dẹp phiên làm việc an toàn cho user: " + authentication.getName());
            } catch (Exception e) {
                System.err.println(">> [LOGOUT ERROR] Lỗi dọn dẹp Context: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}