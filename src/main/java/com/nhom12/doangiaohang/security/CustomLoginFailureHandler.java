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
                // GỌI THỦ TỤC ĐẾM LỖI (DB sẽ tự khóa nếu >= 3 lần)
                entityManager.createNativeQuery("BEGIN CSDL_NHOM12.SP_LOGIN_FAIL(:u); END;")
                        .setParameter("u", username)
                        .executeUpdate();
            } catch (Exception e) {
                System.err.println("Lỗi gọi SP_LOGIN_FAIL: " + e.getMessage());
            }
        }
        
        // Về trang login kèm lỗi
        setDefaultFailureUrl("/login?error");
        super.onAuthenticationFailure(request, response, exception);
    }
}