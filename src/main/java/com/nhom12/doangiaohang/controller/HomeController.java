package com.nhom12.doangiaohang.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(HttpServletRequest request) {
        // Lấy thông tin xác thực của người dùng hiện tại
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            // Kiểm tra vai trò và chuyển hướng
            // Sử dụng request.isUserInRole() là cách chuẩn của Spring Security
            if (request.isUserInRole("ROLE_QUANLY")) {
                return "redirect:/quan-ly/dashboard";
            } else if (request.isUserInRole("ROLE_SHIPPER")) {
                return "redirect:/shipper/dashboard";
            } else if (request.isUserInRole("ROLE_KHACHHANG")) {
                return "redirect:/khach-hang/dashboard";
            }
        }
        
        // Nếu không có vai trò phù hợp hoặc chưa đăng nhập, về trang login
        return "redirect:/login";
    }

    // Trang truy cập bị từ chối (Lỗi 403)
    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied"; 
    }
}