package com.nhom12.doangiaohang.controller;

import com.nhom12.doangiaohang.model.KhachHang;
import com.nhom12.doangiaohang.model.NhanVien;
import com.nhom12.doangiaohang.service.CustomUserHelper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Lớp "Global Helper" (Trình trợ giúp Toàn cục).
 * Tự động cung cấp dữ liệu chung (URL, Họ tên user) cho TẤT CẢ các trang giao diện.
 */
@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private CustomUserHelper userHelper;

    /**
     * Hàm lấy URL hiện tại (để tô màu menu active).
     */
    @ModelAttribute("currentURI")
    public String getCurrentURI(HttpServletRequest request) {
        return request.getRequestURI();
    }

    /**
     * Hàm lấy HỌ TÊN NGƯỜI DÙNG (Thay vì hiện username).
     * Tự động chạy mỗi khi user load trang.
     */
    @ModelAttribute("userFullName")
    public String getUserFullName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }

        try {
            // Nếu là Khách hàng
            if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_KHACHHANG"))) {
                KhachHang kh = userHelper.getKhachHangHienTai(auth);
                return kh.getHoTen();
            } 
            // Nếu là Quản lý hoặc Shipper
            else {
                NhanVien nv = userHelper.getNhanVienHienTai(auth);
                return nv.getHoTen();
            }
        } catch (Exception e) {
            // Nếu lỗi (ví dụ chưa có profile), trả về tên đăng nhập tạm thời
            return auth.getName();
        }
    }
}