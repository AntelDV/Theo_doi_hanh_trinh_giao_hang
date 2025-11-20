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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.KeyPair;
import java.util.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class TaiKhoanService {

    @Autowired private TaiKhoanRepository taiKhoanRepository;
    @Autowired private KhachHangRepository khachHangRepository;
    @Autowired private NhanVienRepository nhanVienRepository;
    @Autowired private VaiTroRepository vaiTroRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EncryptionUtil encryptionUtil; // Dùng để mã hóa AES
    @Autowired private RSAUtil rsaUtil; // Dùng để sinh khóa RSA

    // ============================================================
    // PHẦN 1: CÁC HÀM HỖ TRỢ GIẢI MÃ (READ)
    // ============================================================

    public KhachHang getDecryptedKhachHang(Integer id) {
         KhachHang kh = khachHangRepository.findById(id).orElse(null);
         if (kh != null) {
             try {
                // Giải mã thông tin PII của Khách hàng (App Level)
                kh.setHoTen(encryptionUtil.decrypt(kh.getHoTen()));
                kh.setSoDienThoai(encryptionUtil.decrypt(kh.getSoDienThoai()));
                kh.setEmail(encryptionUtil.decrypt(kh.getEmail())); 
             } catch (Exception e) { 
                 e.printStackTrace(); 
             }
         }
         return kh;
    }
    
    public NhanVien getDecryptedNhanVien(Integer id) {
         NhanVien nv = nhanVienRepository.findById(id).orElse(null);
          if (nv != null) {
             try {
                 // Giải mã App Level: Họ tên, SĐT
                 nv.setHoTen(encryptionUtil.decrypt(nv.getHoTen()));
                 nv.setSoDienThoai(encryptionUtil.decrypt(nv.getSoDienThoai()));
                 // Lưu ý: Email đã được giải mã tự động bởi @Formula trong Entity (DB Level)
             } catch (Exception e) { 
                 e.printStackTrace(); 
             }
         }
         return nv;
     }
     
    // ============================================================
    // PHẦN 2: CÁC HÀM ĐĂNG KÝ TÀI KHOẢN (WRITE)
    // ============================================================

    @Transactional
    public void dangKyKhachHang(DangKyForm form) {
        // 1. Kiểm tra trùng tên đăng nhập
        if (taiKhoanRepository.findByTenDangNhap(form.getTenDangNhap()).isPresent()) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại.");
        }
        
        // 2. Tạo Tài khoản
        TaiKhoan taiKhoan = new TaiKhoan();
        taiKhoan.setTenDangNhap(form.getTenDangNhap());
        taiKhoan.setMatKhau(passwordEncoder.encode(form.getMatKhau()));
        taiKhoan.setTrangThai(true); 
        
        // === LOGIC TUẦN 6: SINH CẶP KHÓA RSA CHO KHÁCH HÀNG ===
        try {
            KeyPair keyPair = rsaUtil.generateKeyPair();
            String publicKeyStr = rsaUtil.keyToString(keyPair.getPublic());
            String privateKeyStr = rsaUtil.keyToString(keyPair.getPrivate());
            
            // Lưu Public Key (Công khai, lưu dạng rõ Base64)
            taiKhoan.setPublicKey(publicKeyStr);
            
            // Lưu Private Key (Bí mật, MÃ HÓA BẰNG AES trước khi lưu)
            taiKhoan.setPrivateKey(encryptionUtil.encrypt(privateKeyStr));
            
        } catch (Exception e) {
            throw new RuntimeException("Lỗi sinh khóa RSA: " + e.getMessage());
        }
        // =====================================================

        VaiTro vaiTroKhachHang = vaiTroRepository.findById(3) 
                .orElseThrow(() -> new RuntimeException("Lỗi CSDL: Không tìm thấy Vai trò ID 3."));
        taiKhoan.setVaiTro(vaiTroKhachHang);
        
        // Lưu Tài khoản
        TaiKhoan savedTaiKhoan = taiKhoanRepository.save(taiKhoan);

        // 3. Tạo hồ sơ Khách hàng
        KhachHang khachHang = new KhachHang();
        khachHang.setTaiKhoan(savedTaiKhoan);
        
        // Mã hóa toàn bộ mức Ứng dụng (Do chưa làm Trigger cho bảng KHACH_HANG)
        khachHang.setHoTen(encryptionUtil.encrypt(form.getHoTen())); 
        khachHang.setEmail(encryptionUtil.encrypt(form.getEmail())); 
        khachHang.setSoDienThoai(encryptionUtil.encrypt(form.getSoDienThoai())); 
        
        khachHang.setNgayTao(new Date());
        
        // Lưu Khách hàng
        khachHangRepository.save(khachHang);
    }
    
    @Transactional
    public void dangKyNhanVien(NhanVienDangKyForm form) {
        // 1. Kiểm tra trùng lặp
        if (taiKhoanRepository.findByTenDangNhap(form.getTenDangNhap()).isPresent()) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại.");
        }
         if (nhanVienRepository.existsByMaNV(form.getMaNV())) {
            throw new IllegalArgumentException("Mã nhân viên này đã tồn tại.");
        }

        // 2. Tạo Tài khoản
        TaiKhoan taiKhoan = new TaiKhoan();
        taiKhoan.setTenDangNhap(form.getTenDangNhap());
        taiKhoan.setMatKhau(passwordEncoder.encode(form.getMatKhau()));
        taiKhoan.setTrangThai(true); 
        
        // === LOGIC TUẦN 6: SINH CẶP KHÓA RSA CHO NHÂN VIÊN ===
        try {
            KeyPair keyPair = rsaUtil.generateKeyPair();
            // Lưu Public Key
            taiKhoan.setPublicKey(rsaUtil.keyToString(keyPair.getPublic()));
            // Mã hóa Private Key bằng AES trước khi lưu
            taiKhoan.setPrivateKey(encryptionUtil.encrypt(rsaUtil.keyToString(keyPair.getPrivate())));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi sinh khóa RSA: " + e.getMessage());
        }
        // =====================================================

        VaiTro vaiTroNhanVien = vaiTroRepository.findById(form.getIdVaiTro())
                .orElseThrow(() -> new RuntimeException("Lỗi: Vai trò không hợp lệ."));
        if (vaiTroNhanVien.getIdVaiTro() == 3) { 
            throw new IllegalArgumentException("Vai trò không hợp lệ.");
        }
        taiKhoan.setVaiTro(vaiTroNhanVien);
        
        // Lưu Tài khoản
        TaiKhoan savedTaiKhoan = taiKhoanRepository.save(taiKhoan);

        // 3. Tạo hồ sơ Nhân viên
        NhanVien nhanVien = new NhanVien();
        nhanVien.setTaiKhoan(savedTaiKhoan);
        nhanVien.setMaNV(form.getMaNV());

        // --- LOGIC TUẦN 5: MÃ HÓA LAI (APP + DB) ---
        
        // A. Mã hóa App (Java): Họ tên & SĐT
        nhanVien.setHoTen(encryptionUtil.encrypt(form.getHoTen()));
        nhanVien.setSoDienThoai(encryptionUtil.encrypt(form.getSoDienThoai())); 
        
        // B. Mã hóa DB (Trigger): Email
        // Gửi email gốc (Plaintext) xuống, Trigger trg_encrypt_email_nhanvien sẽ tự bắt và mã hóa.
        nhanVien.setEmail(form.getEmail()); 
        
        // ------------------------------------------
        
        nhanVien.setNgayVaoLam(new Date());
        
        // Lưu Nhân viên
        nhanVienRepository.save(nhanVien);
    }
    
    // ============================================================
    // PHẦN 3: CÁC HÀM QUẢN TRỊ TÀI KHOẢN (KHÓA/MỞ KHÓA)
    // ============================================================

    @Transactional
    public void khoaTaiKhoan(Integer idTaiKhoan) {
        TaiKhoan taiKhoan = taiKhoanRepository.findById(idTaiKhoan)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản để khóa."));
        taiKhoan.setTrangThai(false); // false = 0 (Bị khóa)
        taiKhoanRepository.save(taiKhoan);
    }

    @Transactional
    public void moKhoaTaiKhoan(Integer idTaiKhoan) {
        TaiKhoan taiKhoan = taiKhoanRepository.findById(idTaiKhoan)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản để mở khóa."));
        taiKhoan.setTrangThai(true); // true = 1 (Hoạt động)
        taiKhoanRepository.save(taiKhoan);
    }
    
    // ============================================================
    // PHẦN 4: CÁC HÀM QUÊN MẬT KHẨU & ĐỔI MẬT KHẨU
    // ============================================================
    
    @Transactional
    public void processForgotPassword(String usernameOrEmail) {
        // Tìm tài khoản
        TaiKhoan taiKhoan = taiKhoanRepository.findByTenDangNhap(usernameOrEmail)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản: " + usernameOrEmail));

        // Sinh Token ngẫu nhiên
        String token = UUID.randomUUID().toString().substring(0, 6).toUpperCase(); // Lấy 6 ký tự cho gọn
        taiKhoan.setMaDatLaiMk(token);
        
        // Token hết hạn sau 15 phút
        Instant expiryInstant = Instant.now().plus(15, ChronoUnit.MINUTES);
        taiKhoan.setThoiHanMa(Date.from(expiryInstant));
        
        taiKhoanRepository.save(taiKhoan);
        
        // === GIẢ LẬP GỬI EMAIL (IN RA CONSOLE) ===
        System.out.println("==================================================");
        System.out.println(" [MÔ PHỎNG EMAIL] Gửi đến user: " + usernameOrEmail);
        System.out.println(" >> MÃ XÁC NHẬN (OTP) CỦA BẠN LÀ: " + token);
        System.out.println("==================================================");
    }
    
    public TaiKhoan validatePasswordResetToken(String token) {
        TaiKhoan taiKhoan = taiKhoanRepository.findByMaDatLaiMk(token)
            .orElseThrow(() -> new IllegalArgumentException("Token không hợp lệ."));

        if (taiKhoan.getThoiHanMa() == null || taiKhoan.getThoiHanMa().before(new Date())) {
            // Token hết hạn -> Xóa đi
            taiKhoan.setMaDatLaiMk(null);
            taiKhoan.setThoiHanMa(null);
            taiKhoanRepository.save(taiKhoan);
            throw new IllegalArgumentException("Token đã hết hạn. Vui lòng thực hiện lại.");
        }
        return taiKhoan;
    }

    public void changeUserPassword(TaiKhoan taiKhoan, String newPassword) {
        taiKhoan.setMatKhau(passwordEncoder.encode(newPassword));
        // Xóa token sau khi đổi pass thành công
        taiKhoan.setMaDatLaiMk(null);
        taiKhoan.setThoiHanMa(null);
        taiKhoanRepository.save(taiKhoan);
    }
}