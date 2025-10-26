package com.nhom12.doangiaohang.service;

import com.nhom12.doangiaohang.model.KhachHang;
import com.nhom12.doangiaohang.model.NhanVien;
import com.nhom12.doangiaohang.model.TaiKhoan;
import com.nhom12.doangiaohang.repository.KhachHangRepository;
import com.nhom12.doangiaohang.repository.NhanVienRepository;
import com.nhom12.doangiaohang.repository.TaiKhoanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class CustomUserHelper {

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;
    @Autowired
    private KhachHangRepository khachHangRepository;
    @Autowired
    private NhanVienRepository nhanVienRepository;

    public TaiKhoan getTaiKhoanHienTai(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Người dùng chưa được xác thực.");
        }
        String username = authentication.getName();
        return taiKhoanRepository.findByTenDangNhap(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản: " + username));
    }

    public KhachHang getKhachHangHienTai(Authentication authentication) {
        TaiKhoan taiKhoan = getTaiKhoanHienTai(authentication);
        return khachHangRepository.findByTaiKhoan_Id(taiKhoan.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ khách hàng cho tài khoản: " + taiKhoan.getTenDangNhap()));
    }

    public NhanVien getNhanVienHienTai(Authentication authentication) {
        TaiKhoan taiKhoan = getTaiKhoanHienTai(authentication);
        return nhanVienRepository.findByTaiKhoan_Id(taiKhoan.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ nhân viên cho tài khoản: " + taiKhoan.getTenDangNhap()));
    }
}