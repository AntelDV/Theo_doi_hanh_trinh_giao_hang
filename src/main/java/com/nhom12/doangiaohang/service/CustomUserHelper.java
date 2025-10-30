package com.nhom12.doangiaohang.service;

import com.nhom12.doangiaohang.model.KhachHang;
import com.nhom12.doangiaohang.model.NhanVien;
import com.nhom12.doangiaohang.model.TaiKhoan;
import com.nhom12.doangiaohang.repository.KhachHangRepository;
import com.nhom12.doangiaohang.repository.NhanVienRepository;
import com.nhom12.doangiaohang.repository.TaiKhoanRepository;
import com.nhom12.doangiaohang.utils.EncryptionUtil; // SỬA LỖI: KÍCH HOẠT IMPORT
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.security.authentication.AnonymousAuthenticationToken; 

/**
 * Lớp tiện ích giúp lấy thông tin (đã giải mã) của người dùng đang đăng nhập.
 */
@Component
public class CustomUserHelper {

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;
    @Autowired 
    private KhachHangRepository khachHangRepository; 
    @Autowired 
    private NhanVienRepository nhanVienRepository; 
    @Autowired 
    private EncryptionUtil encryptionUtil; // SỬA LỖI: KÍCH HOẠT AUTOWIRED

    /**
     * Lấy đối tượng TaiKhoan của người dùng đang đăng nhập.
     */
    public TaiKhoan getTaiKhoanHienTai(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
             return null;
        }
        String username = authentication.getName();
        return taiKhoanRepository.findByTenDangNhap(username).orElse(null); 
    }

    /**
     * Lấy đối tượng KhachHang (đã giải mã PII) của người dùng đang đăng nhập.
     */
    public KhachHang getKhachHangHienTai(Authentication authentication) {
        TaiKhoan taiKhoan = getTaiKhoanHienTai(authentication);
        if (taiKhoan == null) {
             throw new IllegalStateException("Người dùng chưa được xác thực.");
        }
        KhachHang kh = khachHangRepository.findByTaiKhoan_Id(taiKhoan.getId())
             .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ khách hàng cho tài khoản: " + taiKhoan.getTenDangNhap()));
        
        // KÍCH HOẠT GIẢI MÃ PII (Mức Ứng dụng)
        kh.setHoTen(encryptionUtil.decrypt(kh.getHoTen()));
        kh.setEmail(encryptionUtil.decrypt(kh.getEmail()));
        kh.setSoDienThoai(encryptionUtil.decrypt(kh.getSoDienThoai()));
        
        return kh;
    }

    /**
     * Lấy đối tượng NhanVien (đã giải mã PII) của người dùng đang đăng nhập.
     */
    public NhanVien getNhanVienHienTai(Authentication authentication) {
        TaiKhoan taiKhoan = getTaiKhoanHienTai(authentication);
         if (taiKhoan == null) {
             throw new IllegalStateException("Người dùng chưa được xác thực.");
        }
        NhanVien nv = nhanVienRepository.findByTaiKhoan_Id(taiKhoan.getId())
              .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ nhân viên cho tài khoản: " + taiKhoan.getTenDangNhap()));
              
        // KÍCH HOẠT GIẢI MÃ PII (Mức Ứng dụng)
        nv.setHoTen(encryptionUtil.decrypt(nv.getHoTen()));
        nv.setEmail(encryptionUtil.decrypt(nv.getEmail()));
        nv.setSoDienThoai(encryptionUtil.decrypt(nv.getSoDienThoai()));
        
        return nv;
    }
}