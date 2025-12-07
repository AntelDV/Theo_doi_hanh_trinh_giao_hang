package com.nhom12.doangiaohang.security;

import com.nhom12.doangiaohang.model.TaiKhoan;
import com.nhom12.doangiaohang.repository.TaiKhoanRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
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

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @PersistenceContext
    private EntityManager entityManager; 

    @Override
    @Transactional 
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        String username = authentication.getName();
        
        // Lấy thông tin tài khoản từ DB
        TaiKhoan tk = taiKhoanRepository.findByTenDangNhap(username).orElse(null);
        
        if (tk != null) {
            try {
                // CHUYỂN ĐỔI ROLE ID SANG CHUỖI (Khớp với logic DB)
                String dbRole = "KHACHHANG"; 
                int roleId = tk.getVaiTro().getIdVaiTro();
                
                if (roleId == 1) dbRole = "QUANLY";
                else if (roleId == 2) dbRole = "SHIPPER";

                // GỌI THỦ TỤC ORACLE ĐỂ THIẾT LẬP CONTEXT
                String sql = "BEGIN CSDL_NHOM12.PKG_SECURITY.SET_APP_USER(:uid, :uname, :urole); END;";
                Query query = entityManager.createNativeQuery(sql);
                query.setParameter("uid", tk.getId());
                query.setParameter("uname", username);
                query.setParameter("urole", dbRole);
                
                query.executeUpdate();

                System.out.println(">> [ORACLE CONTEXT SET] User: " + username + " | Role: " + dbRole);

            } catch (Exception e) {
                System.err.println(">> [ERROR] Lỗi thiết lập Context Oracle: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // 4. Chuyển hướng trang như cũ
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