package com.nhom12.doangiaohang.security;

import com.nhom12.doangiaohang.model.TaiKhoan;
import com.nhom12.doangiaohang.repository.TaiKhoanRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Collection;

@Component
public class CustomLoginSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional 
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        String username = authentication.getName();
        TaiKhoan tk = taiKhoanRepository.findByTenDangNhap(username).orElse(null);
        
        if (tk != null) {
            try {
                // Set Context Oracle
                String dbRole = "KHACHHANG"; 
                if (tk.getVaiTro().getIdVaiTro() == 1) dbRole = "QUANLY";
                else if (tk.getVaiTro().getIdVaiTro() == 2) dbRole = "SHIPPER";

                String sqlCtx = "BEGIN CSDL_NHOM12.PKG_SECURITY.SET_APP_USER(:uid, :uname, :urole); END;";
                entityManager.createNativeQuery(sqlCtx)
                        .setParameter("uid", tk.getId())
                        .setParameter("uname", username)
                        .setParameter("urole", dbRole)
                        .executeUpdate();

                //  Ghi nhận User Online vào bảng riêng 
                HttpSession session = request.getSession();
                String sessionId = session.getId();
                String ip = request.getRemoteAddr();
                
                String sqlOnline = "MERGE INTO THEO_DOI_ONLINE t USING DUAL ON (t.USERNAME = :u) " +
                                   "WHEN MATCHED THEN UPDATE SET SESSION_ID = :sid, THOI_GIAN_LOGIN = CURRENT_TIMESTAMP, IP_ADDRESS = :ip " +
                                   "WHEN NOT MATCHED THEN INSERT (SESSION_ID, USERNAME, HO_TEN, IP_ADDRESS) VALUES (:sid, :u, :name, :ip)";
                
                entityManager.createNativeQuery(sqlOnline)
                        .setParameter("sid", sessionId)
                        .setParameter("u", username)
                        .setParameter("name", username) 
                        .setParameter("ip", ip)
                        .executeUpdate();

                System.out.println(">> [LOGIN SUCCESS] User: " + username + " | Session: " + sessionId);

            } catch (Exception e) {
                System.err.println(">> [ERROR] Lỗi thiết lập Context/Online: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Chuyển hướng trang
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        for (GrantedAuthority grantedAuthority : authorities) {
            String authorityName = grantedAuthority.getAuthority();
            if (authorityName.equals("ROLE_QUANLY")) {
                response.sendRedirect("/quan-ly/dashboard");
                return;
            } else if (authorityName.equals("ROLE_SHIPPER")) {
                response.sendRedirect("/shipper/dashboard");
                return;
            } else if (authorityName.equals("ROLE_KHACHHANG")) {
                response.sendRedirect("/khach-hang/dashboard");
                return;
            }
        }
        response.sendRedirect("/");
    }
}