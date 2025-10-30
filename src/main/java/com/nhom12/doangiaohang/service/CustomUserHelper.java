package com.nhom12.doangiaohang.service;

import com.nhom12.doangiaohang.model.KhachHang;
import com.nhom12.doangiaohang.model.NhanVien;
import com.nhom12.doangiaohang.model.TaiKhoan;
import com.nhom12.doangiaohang.repository.KhachHangRepository;
import com.nhom12.doangiaohang.repository.NhanVienRepository;
import com.nhom12.doangiaohang.repository.TaiKhoanRepository;
// import com.nhom12.doangiaohang.utils.EncryptionUtil; // Gỡ bỏ import
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.security.authentication.AnonymousAuthenticationToken; 

@Component
public class CustomUserHelper {

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;
    @Autowired 
    private KhachHangRepository khachHangRepository; 
    @Autowired 
    private NhanVienRepository nhanVienRepository; 
    // @Autowired 
    // private EncryptionUtil encryptionUtil; // Gỡ bỏ Autowired

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
     * Lấy đối tượng KhachHang của người dùng đang đăng nhập (dữ liệu thô).
     */
    public KhachHang getKhachHangHienTai(Authentication authentication) {
        TaiKhoan taiKhoan = getTaiKhoanHienTai(authentication);
        if (taiKhoan == null) {
             throw new IllegalStateException("Người dùng chưa được xác thực.");
        }
        KhachHang kh = khachHangRepository.findByTaiKhoan_Id(taiKhoan.getId())
             .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ khách hàng cho tài khoản: " + taiKhoan.getTenDangNhap()));
        
        // Không giải mã
        return kh;
    }

    /**
     * Lấy đối tượng NhanVien của người dùng đang đăng nhập (dữ liệu thô).
     */
    public NhanVien getNhanVienHienTai(Authentication authentication) {
        TaiKhoan taiKhoan = getTaiKhoanHienTai(authentication);
         if (taiKhoan == null) {
             throw new IllegalStateException("Người dùng chưa được xác thực.");
        }
        NhanVien nv = nhanVienRepository.findByTaiKhoan_Id(taiKhoan.getId())
              .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ nhân viên cho tài khoản: " + taiKhoan.getTenDangNhap()));
              
        // Không giải mã
        return nv;
    }
}