package com.nhom12.doangiaohang.security;

import com.nhom12.doangiaohang.model.TaiKhoan;
import com.nhom12.doangiaohang.repository.TaiKhoanRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

    @Autowired private TaiKhoanRepository taiKhoanRepository;
    @PersistenceContext private EntityManager entityManager;

    @Override
    @Transactional 
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String username = authentication.getName();
        
        try {
            // 1. GỌI THỦ TỤC RESET SỐ LẦN SAI (DB Logic)
            entityManager.createNativeQuery("BEGIN CSDL_NHOM12.SP_LOGIN_SUCCESS(:u); END;")
                    .setParameter("u", username)
                    .executeUpdate();

            // 2. THIẾT LẬP CONTEXT VPD & OLS (Như cũ)
            TaiKhoan tk = taiKhoanRepository.findByTenDangNhap(username).orElse(null);
            if (tk != null) {
                String dbRole = "KHACHHANG"; 
                if (tk.getVaiTro().getIdVaiTro() == 1) dbRole = "QUANLY";
                else if (tk.getVaiTro().getIdVaiTro() == 2) dbRole = "SHIPPER";

                entityManager.createNativeQuery("BEGIN CSDL_NHOM12.PKG_SECURITY.SET_APP_USER(:uid, :uname, :urole); END;")
                        .setParameter("uid", tk.getId())
                        .setParameter("uname", username)
                        .setParameter("urole", dbRole)
                        .executeUpdate();

                // 3. Ghi Session Online
                String sqlOnline = "MERGE INTO THEO_DOI_ONLINE t USING DUAL ON (t.USERNAME = :u) " +
                                   "WHEN MATCHED THEN UPDATE SET SESSION_ID = :sid, THOI_GIAN_LOGIN = CURRENT_TIMESTAMP, IP_ADDRESS = :ip " +
                                   "WHEN NOT MATCHED THEN INSERT (SESSION_ID, USERNAME, HO_TEN, IP_ADDRESS) VALUES (:sid, :u, :name, :ip)";
                entityManager.createNativeQuery(sqlOnline)
                        .setParameter("sid", request.getSession().getId())
                        .setParameter("u", username)
                        .setParameter("name", username)
                        .setParameter("ip", request.getRemoteAddr())
                        .executeUpdate();
            }
        } catch (Exception e) {
            System.err.println("Lỗi Login Handler: " + e.getMessage());
        }

        // Redirect
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        for (GrantedAuthority authority : authorities) {
            if (authority.getAuthority().equals("ROLE_QUANLY")) { response.sendRedirect("/quan-ly/dashboard"); return; }
            if (authority.getAuthority().equals("ROLE_SHIPPER")) { response.sendRedirect("/shipper/dashboard"); return; }
            if (authority.getAuthority().equals("ROLE_KHACHHANG")) { response.sendRedirect("/khach-hang/dashboard"); return; }
        }
        response.sendRedirect("/");
    }
}