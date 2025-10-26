package com.nhom12.doangiaohang.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

@Component
public class CustomLoginSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        for (GrantedAuthority grantedAuthority : authorities) {
            String authorityName = grantedAuthority.getAuthority();
            
            // Dựa vào vai trò (ROLE) để quyết định chuyển hướng
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
        
        // Mặc định nếu không tìm thấy vai trò phù hợp
        response.sendRedirect("/");
    }
}