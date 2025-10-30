package com.nhom12.doangiaohang.service;

import com.nhom12.doangiaohang.dto.DangKyForm;
import com.nhom12.doangiaohang.dto.NhanVienDangKyForm; 
import com.nhom12.doangiaohang.model.KhachHang;
import com.nhom12.doangiaohang.model.NhanVien;
import com.nhom12.doangiaohang.model.TaiKhoan;
import com.nhom12.doangiaohang.model.VaiTro;
import com.nhom12.doangiaohang.repository.KhachHangRepository;
import com.nhom12.doangiaohang.repository.NhanVienRepository;
import com.nhom12.doangiaohang.repository.TaiKhoanRepository;
import com.nhom12.doangiaohang.repository.VaiTroRepository;
// import com.nhom12.doangiaohang.utils.EncryptionUtil; // Gỡ bỏ import
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication; 

import java.util.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class TaiKhoanService {

    @Autowired private TaiKhoanRepository taiKhoanRepository;
    @Autowired private KhachHangRepository khachHangRepository;
    @Autowired private NhanVienRepository nhanVienRepository;
    @Autowired private VaiTroRepository vaiTroRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    // @Autowired private EncryptionUtil encryptionUtil; // Gỡ bỏ Autowired

    /**
     * Xử lý đăng ký tài khoản khách hàng mới (lưu plaintext).
     */
    @Transactional
    public void dangKyKhachHang(DangKyForm form) {
        
        if (taiKhoanRepository.findByTenDangNhap(form.getTenDangNhap()).isPresent()) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại.");
        }
        if (khachHangRepository.existsByEmail(form.getEmail())) {
            throw new IllegalArgumentException("Email này đã được sử dụng.");
        }
        if (khachHangRepository.existsBySoDienThoai(form.getSoDienThoai())) {
            throw new IllegalArgumentException("Số điện thoại này đã được sử dụng.");
        }

        TaiKhoan taiKhoan = new TaiKhoan();
        taiKhoan.setTenDangNhap(form.getTenDangNhap());
        taiKhoan.setMatKhau(passwordEncoder.encode(form.getMatKhau()));
        taiKhoan.setTrangThai(true); 

        VaiTro vaiTroKhachHang = vaiTroRepository.findById(3) 
                .orElseThrow(() -> new RuntimeException("Lỗi CSDL: Không tìm thấy Vai trò ID 3."));
        taiKhoan.setVaiTro(vaiTroKhachHang);
        TaiKhoan savedTaiKhoan = taiKhoanRepository.save(taiKhoan);

        KhachHang khachHang = new KhachHang();
        khachHang.setTaiKhoan(savedTaiKhoan);
        khachHang.setHoTen(form.getHoTen()); 
        khachHang.setEmail(form.getEmail()); 
        khachHang.setSoDienThoai(form.getSoDienThoai()); 
        khachHang.setNgayTao(new Date());
        khachHangRepository.save(khachHang);
    }
    
    /**
     * Xử lý đăng ký tài khoản nhân viên mới (lưu plaintext).
     */
    @Transactional
    public void dangKyNhanVien(NhanVienDangKyForm form) {
        if (taiKhoanRepository.findByTenDangNhap(form.getTenDangNhap()).isPresent()) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại.");
        }
         if (nhanVienRepository.existsByEmail(form.getEmail())) {
            throw new IllegalArgumentException("Email này đã được sử dụng.");
        }
        if (nhanVienRepository.existsBySoDienThoai(form.getSoDienThoai())) {
            throw new IllegalArgumentException("Số điện thoại này đã được sử dụng.");
        }
         if (nhanVienRepository.existsByMaNV(form.getMaNV())) {
            throw new IllegalArgumentException("Mã nhân viên này đã tồn tại.");
        }

        TaiKhoan taiKhoan = new TaiKhoan();
        taiKhoan.setTenDangNhap(form.getTenDangNhap());
        taiKhoan.setMatKhau(passwordEncoder.encode(form.getMatKhau()));
        taiKhoan.setTrangThai(true); 

        VaiTro vaiTroNhanVien = vaiTroRepository.findById(form.getIdVaiTro())
                .orElseThrow(() -> new RuntimeException("Lỗi: Vai trò không hợp lệ."));
        if (vaiTroNhanVien.getIdVaiTro() == 3) { 
            throw new IllegalArgumentException("Vai trò không hợp lệ.");
        }
        taiKhoan.setVaiTro(vaiTroNhanVien);
        TaiKhoan savedTaiKhoan = taiKhoanRepository.save(taiKhoan);

        NhanVien nhanVien = new NhanVien();
        nhanVien.setTaiKhoan(savedTaiKhoan);
        nhanVien.setMaNV(form.getMaNV());
        nhanVien.setHoTen(form.getHoTen());
        nhanVien.setEmail(form.getEmail()); 
        nhanVien.setSoDienThoai(form.getSoDienThoai()); 
        nhanVien.setNgayVaoLam(new Date());
        nhanVienRepository.save(nhanVien);
    }
    
    /**
     * Lấy thông tin KhachHang (dữ liệu thô).
     */
    public KhachHang getDecryptedKhachHang(Integer id) {
         return khachHangRepository.findById(id).orElse(null);
    }
    
     /**
      * Lấy thông tin NhanVien (dữ liệu thô).
      */
     public NhanVien getDecryptedNhanVien(Integer id) {
         return nhanVienRepository.findById(id).orElse(null);
     }

     /**
      * Lấy thông tin KhachHang của người dùng đang đăng nhập (dữ liệu thô).
      */
     public KhachHang getDecryptedKhachHangHienTai(Authentication authentication) {
         if (authentication == null || !authentication.isAuthenticated()) {
             throw new IllegalStateException("Người dùng chưa được xác thực.");
         }
         return khachHangRepository.findByTaiKhoan_TenDangNhap(authentication.getName())
             .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ khách hàng cho tài khoản: " + authentication.getName()));
     }
     
      /**
       * Lấy thông tin NhanVien của người dùng đang đăng nhập (dữ liệu thô).
       */
      public NhanVien getDecryptedNhanVienHienTai(Authentication authentication) {
          if (authentication == null || !authentication.isAuthenticated()) {
             throw new IllegalStateException("Người dùng chưa được xác thực.");
         }
         return nhanVienRepository.findByTaiKhoan_TenDangNhap(authentication.getName())
              .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ nhân viên cho tài khoản: " + authentication.getName()));
      }
    
    /**
     * Khóa tài khoản người dùng.
     */
    @Transactional
    public void khoaTaiKhoan(Integer idTaiKhoan) {
        TaiKhoan taiKhoan = taiKhoanRepository.findById(idTaiKhoan)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản để khóa."));
        taiKhoan.setTrangThai(false); 
        taiKhoanRepository.save(taiKhoan);
    }

    /**
     * Mở khóa tài khoản người dùng.
     */
    @Transactional
    public void moKhoaTaiKhoan(Integer idTaiKhoan) {
        TaiKhoan taiKhoan = taiKhoanRepository.findById(idTaiKhoan)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản để mở khóa."));
        taiKhoan.setTrangThai(true); 
        taiKhoanRepository.save(taiKhoan);
    }
    
    /**
     * Xử lý yêu cầu quên mật khẩu.
     */
    public void processForgotPassword(String email) {
        TaiKhoan taiKhoan = null;
        Optional<KhachHang> khachHangOpt = khachHangRepository.findByEmail(email);
        if (khachHangOpt.isPresent()) {
            taiKhoan = khachHangOpt.get().getTaiKhoan();
        } else {
            Optional<NhanVien> nhanVienOpt = nhanVienRepository.findByEmail(email);
            if (nhanVienOpt.isPresent()) {
                taiKhoan = nhanVienOpt.get().getTaiKhoan();
            } else {
                throw new IllegalArgumentException("Không tìm thấy tài khoản nào được đăng ký với email này.");
            }
        }

        String token = UUID.randomUUID().toString();
        taiKhoan.setMaDatLaiMk(token);
        Instant expiryInstant = Instant.now().plus(15, ChronoUnit.MINUTES);
        taiKhoan.setThoiHanMa(Date.from(expiryInstant));
        taiKhoanRepository.save(taiKhoan);
    }
    
    /**
     * Xác thực token đặt lại mật khẩu.
     */
    public TaiKhoan validatePasswordResetToken(String token) {
        TaiKhoan taiKhoan = taiKhoanRepository.findByMaDatLaiMk(token)
            .orElseThrow(() -> new IllegalArgumentException("Token không hợp lệ."));

        if (taiKhoan.getThoiHanMa() == null || taiKhoan.getThoiHanMa().before(new Date())) {
            throw new IllegalArgumentException("Token đã hết hạn. Vui lòng gửi lại yêu cầu mới.");
        }
        return taiKhoan;
    }

    /**
     * Thay đổi mật khẩu người dùng sau khi xác thực token.
     */
    public void changeUserPassword(TaiKhoan taiKhoan, String newPassword) {
        taiKhoan.setMatKhau(passwordEncoder.encode(newPassword));
        taiKhoan.setMaDatLaiMk(null);
        taiKhoan.setThoiHanMa(null);
        taiKhoanRepository.save(taiKhoan);
    }
}