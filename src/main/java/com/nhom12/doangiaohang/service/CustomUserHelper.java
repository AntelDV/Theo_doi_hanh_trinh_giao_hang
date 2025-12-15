package com.nhom12.doangiaohang.service;

import com.nhom12.doangiaohang.model.KhachHang;
import com.nhom12.doangiaohang.model.NhanVien;
import com.nhom12.doangiaohang.model.TaiKhoan;
import com.nhom12.doangiaohang.repository.KhachHangRepository;
import com.nhom12.doangiaohang.repository.NhanVienRepository;
import com.nhom12.doangiaohang.repository.TaiKhoanRepository;
import com.nhom12.doangiaohang.utils.EncryptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.security.authentication.AnonymousAuthenticationToken;

@Component
public class CustomUserHelper {

    @Autowired private TaiKhoanRepository taiKhoanRepository;
    @Autowired private KhachHangRepository khachHangRepository;
    @Autowired private NhanVienRepository nhanVienRepository;
    @Autowired private EncryptionUtil encryptionUtil;

    public TaiKhoan getTaiKhoanHienTai(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
             return null;
        }
        return taiKhoanRepository.findByTenDangNhap(authentication.getName()).orElse(null); 
    }

    public KhachHang getKhachHangHienTai(Authentication authentication) {
        TaiKhoan taiKhoan = getTaiKhoanHienTai(authentication);
        if (taiKhoan == null) throw new IllegalStateException("Chưa đăng nhập.");
        
        KhachHang kh = khachHangRepository.findByTaiKhoan_Id(taiKhoan.getId())
             .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ khách hàng."));
        
        // GIẢI MÃ: Chỉ giải mã các trường APP-LEVEL (chưa có @Formula)
        try {
            kh.setHoTen(encryptionUtil.decrypt(kh.getHoTen()));
            kh.setEmail(encryptionUtil.decrypt(kh.getEmail())); 
            kh.setSoDienThoai(encryptionUtil.decrypt(kh.getSoDienThoai()));
        } catch (Exception e) { e.printStackTrace(); }
        
        return kh;
    }

    public NhanVien getNhanVienHienTai(Authentication authentication) {
        TaiKhoan taiKhoan = getTaiKhoanHienTai(authentication);
         if (taiKhoan == null) throw new IllegalStateException("Chưa đăng nhập.");
         
        NhanVien nv = nhanVienRepository.findByTaiKhoan_Id(taiKhoan.getId())
              .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ nhân viên."));
              
        // GIẢI MÃ: Lưu ý EMAIL của Nhân viên dùng @Formula (DB-Level) nên KHÔNG giải mã ở đây
        try {
            nv.setHoTen(encryptionUtil.decrypt(nv.getHoTen()));
            nv.setSoDienThoai(encryptionUtil.decrypt(nv.getSoDienThoai()));
            // nv.setEmail(...) -> KHÔNG CẦN, vì @Formula đã trả về Plaintext rồi
        } catch (Exception e) { e.printStackTrace(); }
        
        return nv;
    }
}