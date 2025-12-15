package com.nhom12.doangiaohang.security;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;

@Component
public class CustomLoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @PersistenceContext private EntityManager entityManager;

    @Override
    @Transactional 
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String username = request.getParameter("username");
        if (username != null) {
            try {
                // Gọi thủ tục đếm lỗi 
                entityManager.createNativeQuery("BEGIN CSDL_NHOM12.SP_LOGIN_FAIL(:u); END;")
                        .setParameter("u", username)
                        .executeUpdate();
                
                // Insert trực tiếp, không qua Trigger RSA
                String sqlLog = "INSERT INTO LICH_SU_HOAT_DONG (ID_HOAT_DONG, ID_TAI_KHOAN_THUC_HIEN, HANH_DONG, MO_TA_CHI_TIET, THOI_GIAN_THUC_HIEN, DIA_CHI_IP) " +
                                "VALUES (LICH_SU_HOAT_DONG_SEQ.NEXTVAL, 1, 'LOGIN_FAIL', :desc, CURRENT_TIMESTAMP, :ip)";
                
                entityManager.createNativeQuery(sqlLog)
                        .setParameter("desc", "Đăng nhập thất bại: " + username)
                        .setParameter("ip", request.getRemoteAddr())
                        .executeUpdate();

            } catch (Exception e) {
                System.err.println("Lỗi ghi log: " + e.getMessage());
            }
        }
        
        setDefaultFailureUrl("/login?error");
        super.onAuthenticationFailure(request, response, exception);
    }
}