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
import com.nhom12.doangiaohang.utils.EncryptionUtil;
import com.nhom12.doangiaohang.utils.RSAUtil; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.KeyPair;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class TaiKhoanService {

    @Autowired private TaiKhoanRepository taiKhoanRepository;
    @Autowired private KhachHangRepository khachHangRepository;
    @Autowired private NhanVienRepository nhanVienRepository;
    @Autowired private VaiTroRepository vaiTroRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EncryptionUtil encryptionUtil;
    @Autowired private RSAUtil rsaUtil;
    
    @Autowired private NhatKyVanHanhService nhatKyService;
    @Autowired private CustomUserHelper userHelper;

    public KhachHang getDecryptedKhachHang(Integer id) {
         KhachHang kh = khachHangRepository.findById(id).orElse(null);
         if (kh != null) {
             try {
                kh.setHoTen(encryptionUtil.decrypt(kh.getHoTen()));
                kh.setSoDienThoai(encryptionUtil.decrypt(kh.getSoDienThoai()));
                kh.setEmail(encryptionUtil.decrypt(kh.getEmail())); 
             } catch (Exception e) { e.printStackTrace(); }
         }
         return kh;
    }
    
    public NhanVien getDecryptedNhanVien(Integer id) {
         NhanVien nv = nhanVienRepository.findById(id).orElse(null);
          if (nv != null) {
             try {
                 nv.setHoTen(encryptionUtil.decrypt(nv.getHoTen()));
                 nv.setSoDienThoai(encryptionUtil.decrypt(nv.getSoDienThoai()));
             } catch (Exception e) { e.printStackTrace(); }
         }
         return nv;
     }

    @Transactional
    public void dangKyKhachHang(DangKyForm form) {
        if (taiKhoanRepository.findByTenDangNhap(form.getTenDangNhap()).isPresent()) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại.");
        }
        
        TaiKhoan taiKhoan = new TaiKhoan();
        taiKhoan.setTenDangNhap(form.getTenDangNhap());
        taiKhoan.setMatKhau(passwordEncoder.encode(form.getMatKhau()));
        taiKhoan.setTrangThai(true); 
        
        try {
            KeyPair keyPair = rsaUtil.generateKeyPair();
            taiKhoan.setPublicKey(rsaUtil.keyToString(keyPair.getPublic()));
            taiKhoan.setPrivateKey(encryptionUtil.encrypt(rsaUtil.keyToString(keyPair.getPrivate())));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi sinh khóa RSA: " + e.getMessage());
        }

        VaiTro vaiTroKhachHang = vaiTroRepository.findById(3).orElseThrow();
        taiKhoan.setVaiTro(vaiTroKhachHang);
        TaiKhoan savedTaiKhoan = taiKhoanRepository.save(taiKhoan);

        KhachHang khachHang = new KhachHang();
        khachHang.setTaiKhoan(savedTaiKhoan);
        khachHang.setHoTen(encryptionUtil.encrypt(form.getHoTen())); 
        khachHang.setEmail(encryptionUtil.encrypt(form.getEmail())); 
        khachHang.setSoDienThoai(encryptionUtil.encrypt(form.getSoDienThoai())); 
        khachHang.setNgayTao(new Date());
        
        khachHangRepository.save(khachHang);
    }
    
    @Transactional
    public void dangKyNhanVien(NhanVienDangKyForm form) {
        if (taiKhoanRepository.findByTenDangNhap(form.getTenDangNhap()).isPresent()) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại.");
        }
         if (nhanVienRepository.existsByMaNV(form.getMaNV())) {
            throw new IllegalArgumentException("Mã nhân viên này đã tồn tại.");
        }

        TaiKhoan taiKhoan = new TaiKhoan();
        taiKhoan.setTenDangNhap(form.getTenDangNhap());
        taiKhoan.setMatKhau(passwordEncoder.encode(form.getMatKhau()));
        taiKhoan.setTrangThai(true); 
        
        try {
            KeyPair keyPair = rsaUtil.generateKeyPair();
            taiKhoan.setPublicKey(rsaUtil.keyToString(keyPair.getPublic()));
            taiKhoan.setPrivateKey(encryptionUtil.encrypt(rsaUtil.keyToString(keyPair.getPrivate())));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi sinh khóa RSA: " + e.getMessage());
        }

        VaiTro vaiTroNhanVien = vaiTroRepository.findById(form.getIdVaiTro()).orElseThrow();
        if (vaiTroNhanVien.getIdVaiTro() == 3) throw new IllegalArgumentException("Lỗi vai trò.");
        
        taiKhoan.setVaiTro(vaiTroNhanVien);
        TaiKhoan savedTaiKhoan = taiKhoanRepository.save(taiKhoan);

        NhanVien nhanVien = new NhanVien();
        nhanVien.setTaiKhoan(savedTaiKhoan);
        nhanVien.setMaNV(form.getMaNV());
        nhanVien.setHoTen(encryptionUtil.encrypt(form.getHoTen()));
        nhanVien.setSoDienThoai(encryptionUtil.encrypt(form.getSoDienThoai())); 
        nhanVien.setEmail(form.getEmail()); 
        nhanVien.setNgayVaoLam(new Date());
        
        nhanVienRepository.save(nhanVien);

        ghiLogHeThong("THEM_NHAN_VIEN", "NHAN_VIEN", savedTaiKhoan.getId(), 
                "Đã tạo nhân viên mới: " + form.getMaNV() + " - " + form.getTenDangNhap());
    }

    @Transactional
    public void khoaTaiKhoan(Integer idTaiKhoan) {
        TaiKhoan taiKhoan = taiKhoanRepository.findById(idTaiKhoan)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));
        taiKhoan.setTrangThai(false);
        taiKhoanRepository.save(taiKhoan);
        
        String tenDoiTuong = layTenNguoiDung(taiKhoan);
        ghiLogHeThong("KHOA_TAI_KHOAN", "TAI_KHOAN", idTaiKhoan, "Đã khóa tài khoản: " + tenDoiTuong + " (" + taiKhoan.getTenDangNhap() + ")");
    }

    @Transactional
    public void moKhoaTaiKhoan(Integer idTaiKhoan) {
        TaiKhoan taiKhoan = taiKhoanRepository.findById(idTaiKhoan)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));
        taiKhoan.setTrangThai(true);
        taiKhoanRepository.save(taiKhoan);
        
        String tenDoiTuong = layTenNguoiDung(taiKhoan);
        ghiLogHeThong("MO_KHOA_TAI_KHOAN", "TAI_KHOAN", idTaiKhoan, "Đã mở khóa tài khoản: " + tenDoiTuong + " (" + taiKhoan.getTenDangNhap() + ")");
    }
    
    private String layTenNguoiDung(TaiKhoan tk) {
        try {
            if (tk.getVaiTro().getIdVaiTro() == 3) { // Khách hàng
                return khachHangRepository.findByTaiKhoan_Id(tk.getId())
                        .map(kh -> {
                            try { return encryptionUtil.decrypt(kh.getHoTen()); } catch (Exception e) { return kh.getHoTen(); }
                        }).orElse("Unknown");
            } else { // Nhân viên
                return nhanVienRepository.findByTaiKhoan_Id(tk.getId())
                        .map(nv -> {
                            try { return encryptionUtil.decrypt(nv.getHoTen()); } catch (Exception e) { return nv.getHoTen(); }
                        }).orElse("Unknown");
            }
        } catch (Exception e) {
            return tk.getTenDangNhap();
        }
    }
    
    private void ghiLogHeThong(String hanhDong, String doiTuong, Integer idDoiTuong, String moTa) {
        try {
            TaiKhoan admin = userHelper.getTaiKhoanHienTai(
                SecurityContextHolder.getContext().getAuthentication()
            );
            if (admin != null) {
                nhatKyService.logAction(admin, hanhDong, doiTuong, idDoiTuong, moTa);
            }
        } catch (Exception e) {
            System.err.println("Không thể ghi log: " + e.getMessage());
        }
    }
    
    @Transactional
    public void processForgotPassword(String username) {
        TaiKhoan taiKhoan = taiKhoanRepository.findByTenDangNhap(username)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản: " + username));
        String token = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        taiKhoan.setMaDatLaiMk(token);
        taiKhoan.setThoiHanMa(Date.from(Instant.now().plus(15, ChronoUnit.MINUTES)));
        taiKhoanRepository.save(taiKhoan);
        System.out.println(">> OTP RESET PASS CHO " + username + ": " + token);
    }
    
    public TaiKhoan validatePasswordResetToken(String token) {
        TaiKhoan tk = taiKhoanRepository.findByMaDatLaiMk(token).orElseThrow(() -> new IllegalArgumentException("Token sai."));
        if (tk.getThoiHanMa() == null || tk.getThoiHanMa().before(new Date())) {
            throw new IllegalArgumentException("Token hết hạn.");
        }
        return tk;
    }

    public void changeUserPassword(TaiKhoan taiKhoan, String newPassword) {
        taiKhoan.setMatKhau(passwordEncoder.encode(newPassword));
        taiKhoan.setMaDatLaiMk(null);
        taiKhoan.setThoiHanMa(null);
        taiKhoanRepository.save(taiKhoan);
    }
}