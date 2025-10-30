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
import com.nhom12.doangiaohang.utils.EncryptionUtil; // SỬA LỖI: KÍCH HOẠT IMPORT
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
    @Autowired private EncryptionUtil encryptionUtil; // SỬA LỖI: KÍCH HOẠT AUTOWIRED

    /**
     * Xử lý đăng ký tài khoản khách hàng mới (Mã hóa PII ở mức Ứng dụng).
     */
    @Transactional
    public void dangKyKhachHang(DangKyForm form) {
        
        // Lưu ý: Việc kiểm tra trùng lặp (existsByEmail) sẽ không hiệu quả 
        // khi dữ liệu đã được mã hóa. Cần một cơ chế tìm kiếm mã hóa (Searchable Encryption) 
        // phức tạp hơn, nằm ngoài phạm vi đồ án này.
        if (taiKhoanRepository.findByTenDangNhap(form.getTenDangNhap()).isPresent()) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại.");
        }
        
        // if (khachHangRepository.existsByEmail(encryptionUtil.encrypt(form.getEmail()))) { ... }

        TaiKhoan taiKhoan = new TaiKhoan();
        taiKhoan.setTenDangNhap(form.getTenDangNhap());
        taiKhoan.setMatKhau(passwordEncoder.encode(form.getMatKhau())); // Hash mật khẩu
        taiKhoan.setTrangThai(true); 

        VaiTro vaiTroKhachHang = vaiTroRepository.findById(3) 
                .orElseThrow(() -> new RuntimeException("Lỗi CSDL: Không tìm thấy Vai trò ID 3."));
        taiKhoan.setVaiTro(vaiTroKhachHang);
        TaiKhoan savedTaiKhoan = taiKhoanRepository.save(taiKhoan);

        KhachHang khachHang = new KhachHang();
        khachHang.setTaiKhoan(savedTaiKhoan);
        
        // KÍCH HOẠT MÃ HÓA MỨC ỨNG DỤNG (TUẦN 5)
        khachHang.setHoTen(encryptionUtil.encrypt(form.getHoTen())); 
        khachHang.setEmail(encryptionUtil.encrypt(form.getEmail())); 
        khachHang.setSoDienThoai(encryptionUtil.encrypt(form.getSoDienThoai())); 
        
        khachHang.setNgayTao(new Date());
        khachHangRepository.save(khachHang);
    }
    
    /**
     * Xử lý đăng ký tài khoản nhân viên mới (Mã hóa PII ở mức Ứng dụng).
     */
    @Transactional
    public void dangKyNhanVien(NhanVienDangKyForm form) {
        if (taiKhoanRepository.findByTenDangNhap(form.getTenDangNhap()).isPresent()) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại.");
        }
         if (nhanVienRepository.existsByMaNV(form.getMaNV())) {
            throw new IllegalArgumentException("Mã nhân viên này đã tồn tại.");
        }
        // Bỏ qua kiểm tra trùng lặp Email/SĐT vì đã mã hóa

        TaiKhoan taiKhoan = new TaiKhoan();
        taiKhoan.setTenDangNhap(form.getTenDangNhap());
        taiKhoan.setMatKhau(passwordEncoder.encode(form.getMatKhau())); // Hash mật khẩu
        taiKhoan.setTrangThai(true); 

        VaiTro vaiTroNhanVien = vaiTroRepository.findById(form.getIdVaiTro())
                .orElseThrow(() -> new RuntimeException("Lỗi: Vai trò không hợp lệ."));
        if (vaiTroNhanVien.getIdVaiTro() == 3) { // Không cho phép tạo Khách hàng ở đây
            throw new IllegalArgumentException("Vai trò không hợp lệ.");
        }
        taiKhoan.setVaiTro(vaiTroNhanVien);
        TaiKhoan savedTaiKhoan = taiKhoanRepository.save(taiKhoan);

        NhanVien nhanVien = new NhanVien();
        nhanVien.setTaiKhoan(savedTaiKhoan);
        nhanVien.setMaNV(form.getMaNV()); // Mã NV là unique, không mã hóa

        // KÍCH HOẠT MÃ HÓA MỨC ỨNG DỤNG (TUẦN 5)
        nhanVien.setHoTen(encryptionUtil.encrypt(form.getHoTen()));
        nhanVien.setEmail(encryptionUtil.encrypt(form.getEmail())); 
        nhanVien.setSoDienThoai(encryptionUtil.encrypt(form.getSoDienThoai())); 
        
        nhanVien.setNgayVaoLam(new Date());
        nhanVienRepository.save(nhanVien);
    }
    
    /**
     * Lấy thông tin KhachHang và GIẢI MÃ PII.
     */
    public KhachHang getDecryptedKhachHang(Integer id) {
         KhachHang kh = khachHangRepository.findById(id).orElse(null);
         if (kh != null) {
             kh.setHoTen(encryptionUtil.decrypt(kh.getHoTen()));
             kh.setEmail(encryptionUtil.decrypt(kh.getEmail()));
             kh.setSoDienThoai(encryptionUtil.decrypt(kh.getSoDienThoai()));
         }
         return kh;
    }
    
     /**
      * Lấy thông tin NhanVien và GIẢI MÃ PII.
      */
     public NhanVien getDecryptedNhanVien(Integer id) {
         NhanVien nv = nhanVienRepository.findById(id).orElse(null);
          if (nv != null) {
             nv.setHoTen(encryptionUtil.decrypt(nv.getHoTen()));
             nv.setEmail(encryptionUtil.decrypt(nv.getEmail()));
             nv.setSoDienThoai(encryptionUtil.decrypt(nv.getSoDienThoai()));
         }
         return nv;
     }

     /**
      * Lấy thông tin KhachHang (đã giải mã) của người dùng đang đăng nhập.
      */
     public KhachHang getDecryptedKhachHangHienTai(Authentication authentication) {
         if (authentication == null || !authentication.isAuthenticated()) {
             throw new IllegalStateException("Người dùng chưa được xác thực.");
         }
         KhachHang kh = khachHangRepository.findByTaiKhoan_TenDangNhap(authentication.getName())
             .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ khách hàng cho tài khoản: " + authentication.getName()));
         
         // GIẢI MÃ PII
         kh.setHoTen(encryptionUtil.decrypt(kh.getHoTen()));
         kh.setEmail(encryptionUtil.decrypt(kh.getEmail()));
         kh.setSoDienThoai(encryptionUtil.decrypt(kh.getSoDienThoai()));
         return kh;
     }
     
      /**
       * Lấy thông tin NhanVien (đã giải mã) của người dùng đang đăng nhập.
       */
      public NhanVien getDecryptedNhanVienHienTai(Authentication authentication) {
          if (authentication == null || !authentication.isAuthenticated()) {
             throw new IllegalStateException("Người dùng chưa được xác thực.");
         }
         NhanVien nv = nhanVienRepository.findByTaiKhoan_TenDangNhap(authentication.getName())
              .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ nhân viên cho tài khoản: " + authentication.getName()));
              
         // GIẢI MÃ PII
         nv.setHoTen(encryptionUtil.decrypt(nv.getHoTen()));
         nv.setEmail(encryptionUtil.decrypt(nv.getEmail()));
         nv.setSoDienThoai(encryptionUtil.decrypt(nv.getSoDienThoai()));
         return nv;
      }
    
    /**
     * Khóa tài khoản người dùng (Đặt TRANG_THAI = 0).
     */
    @Transactional
    public void khoaTaiKhoan(Integer idTaiKhoan) {
        TaiKhoan taiKhoan = taiKhoanRepository.findById(idTaiKhoan)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản để khóa."));
        taiKhoan.setTrangThai(false); // false = 0 (Bị khóa)
        taiKhoanRepository.save(taiKhoan);
    }

    /**
     * Mở khóa tài khoản người dùng (Đặt TRANG_THAI = 1).
     */
    @Transactional
    public void moKhoaTaiKhoan(Integer idTaiKhoan) {
        TaiKhoan taiKhoan = taiKhoanRepository.findById(idTaiKhoan)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản để mở khóa."));
        taiKhoan.setTrangThai(true); // true = 1 (Hoạt động)
        taiKhoanRepository.save(taiKhoan);
    }
    
    /**
     * Xử lý yêu cầu quên mật khẩu.
     * Lưu ý: Tìm kiếm bằng Email sẽ thất bại do mã hóa.
     * Tạm thời yêu cầu người dùng nhập TÊN ĐĂNG NHẬP thay vì Email.
     */
    public void processForgotPassword(String usernameOrEmail) {
        // Do Email đã bị mã hóa (Mức Ứng dụng), chúng ta không thể tìm kiếm
        // trực tiếp. Chúng ta sẽ tìm bằng Tên Đăng Nhập.
        TaiKhoan taiKhoan = taiKhoanRepository.findByTenDangNhap(usernameOrEmail)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));

        String token = UUID.randomUUID().toString();
        taiKhoan.setMaDatLaiMk(token);
        // Đặt thời hạn 15 phút
        Instant expiryInstant = Instant.now().plus(15, ChronoUnit.MINUTES);
        taiKhoan.setThoiHanMa(Date.from(expiryInstant));
        taiKhoanRepository.save(taiKhoan);
        
        // (Trong dự án thực tế, bạn sẽ gửi email chứa token này)
    }
    
    /**
     * Xác thực token đặt lại mật khẩu.
     */
    public TaiKhoan validatePasswordResetToken(String token) {
        TaiKhoan taiKhoan = taiKhoanRepository.findByMaDatLaiMk(token)
            .orElseThrow(() -> new IllegalArgumentException("Token không hợp lệ."));

        if (taiKhoan.getThoiHanMa() == null || taiKhoan.getThoiHanMa().before(new Date())) {
            // Xóa token hết hạn
            taiKhoan.setMaDatLaiMk(null);
            taiKhoan.setThoiHanMa(null);
            taiKhoanRepository.save(taiKhoan);
            throw new IllegalArgumentException("Token đã hết hạn. Vui lòng gửi lại yêu cầu mới.");
        }
        return taiKhoan;
    }

    /**
     * Thay đổi mật khẩu người dùng và vô hiệu hóa token.
     */
    public void changeUserPassword(TaiKhoan taiKhoan, String newPassword) {
        taiKhoan.setMatKhau(passwordEncoder.encode(newPassword));
        taiKhoan.setMaDatLaiMk(null);
        taiKhoan.setThoiHanMa(null);
        taiKhoanRepository.save(taiKhoan);
    }
}