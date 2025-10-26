package com.nhom12.doangiaohang.service;

import com.nhom12.doangiaohang.dto.DangKyForm;
import com.nhom12.doangiaohang.dto.NhanVienDangKyForm; // Sẽ tạo ở file sau
import com.nhom12.doangiaohang.model.KhachHang;
import com.nhom12.doangiaohang.model.NhanVien;
import com.nhom12.doangiaohang.model.TaiKhoan;
import com.nhom12.doangiaohang.model.VaiTro;
import com.nhom12.doangiaohang.repository.KhachHangRepository;
import com.nhom12.doangiaohang.repository.NhanVienRepository;
import com.nhom12.doangiaohang.repository.TaiKhoanRepository;
import com.nhom12.doangiaohang.repository.VaiTroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    
    // @Autowired private EmailService emailService; // Tạm đóng

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

        VaiTro vaiTroKhachHang = vaiTroRepository.findById(3) // ID 3 = Khách hàng
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
    
    // === THÊM PHƯƠNG THỨC MỚI ĐỂ TẠO NHÂN VIÊN ===
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

        // Lấy vai trò Quản lý (1) hoặc Shipper (2)
        VaiTro vaiTroNhanVien = vaiTroRepository.findById(form.getIdVaiTro())
                .orElseThrow(() -> new RuntimeException("Lỗi: Vai trò không hợp lệ."));
        if (vaiTroNhanVien.getIdVaiTro() == 3) { // Không cho phép tạo Khách hàng bằng form này
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
    
    
    // === CÁC HÀM QUÊN MẬT KHẨU (Tạm thời không dùng nhưng vẫn giữ) ===
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

        // emailService.sendPasswordResetEmail(email, token); // Tạm đóng
    }
    
    public TaiKhoan validatePasswordResetToken(String token) {
        TaiKhoan taiKhoan = taiKhoanRepository.findByMaDatLaiMk(token)
            .orElseThrow(() -> new IllegalArgumentException("Token không hợp lệ."));

        if (taiKhoan.getThoiHanMa() == null || taiKhoan.getThoiHanMa().before(new Date())) {
            throw new IllegalArgumentException("Token đã hết hạn. Vui lòng gửi lại yêu cầu mới.");
        }
        return taiKhoan;
    }

    public void changeUserPassword(TaiKhoan taiKhoan, String newPassword) {
        taiKhoan.setMatKhau(passwordEncoder.encode(newPassword));
        taiKhoan.setMaDatLaiMk(null);
        taiKhoan.setThoiHanMa(null);
        taiKhoanRepository.save(taiKhoan);
    }
}