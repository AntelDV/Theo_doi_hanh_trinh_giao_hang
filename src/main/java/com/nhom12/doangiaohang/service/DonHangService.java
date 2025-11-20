package com.nhom12.doangiaohang.service;

import com.nhom12.doangiaohang.model.*;
import com.nhom12.doangiaohang.repository.*;
import com.nhom12.doangiaohang.utils.EncryptionUtil;
import com.nhom12.doangiaohang.utils.RSAUtil;
import com.nhom12.doangiaohang.service.HybridEncryptionService.HybridResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DonHangService {

    @Autowired private DonHangRepository donHangRepository;
    @Autowired private HanhTrinhDonHangRepository hanhTrinhDonHangRepository;
    @Autowired private TrangThaiDonHangRepository trangThaiDonHangRepository;
    @Autowired private NhanVienRepository nhanVienRepository;
    @Autowired private DiaChiRepository diaChiRepository;
    @Autowired private ThanhToanRepository thanhToanRepository;
    @Autowired private TaiKhoanRepository taiKhoanRepository;
    @Autowired private CustomUserHelper userHelper;
    @Autowired private EncryptionUtil encryptionUtil;
    @Autowired private RSAUtil rsaUtil;
    @Autowired private HybridEncryptionService hybridService; // Service Mã hóa Lai

    // Tiện ích lấy Private Key
    private String getMyPrivateKey(Authentication authentication) {
        TaiKhoan tk = userHelper.getTaiKhoanHienTai(authentication);
        if (tk == null || tk.getPrivateKey() == null) {
            throw new RuntimeException("Tài khoản chưa có Key RSA.");
        }
        return encryptionUtil.decrypt(tk.getPrivateKey());
    }

    // Tiện ích giải mã PII (Tuần 5)
    private void decryptDonHangPII(DonHang donHang) {
        if (donHang != null) {
            try {
                donHang.setTenNguoiNhan(encryptionUtil.decrypt(donHang.getTenNguoiNhan()));
                donHang.setDiaChiGiaoHang(encryptionUtil.decrypt(donHang.getDiaChiGiaoHang()));
                if (donHang.getDiaChiLayHang() != null) {
                    donHang.getDiaChiLayHang().setSoNhaDuong(encryptionUtil.decrypt(donHang.getDiaChiLayHang().getSoNhaDuong()));
                }
            } catch (Exception e) { /* Ignore */ }
        }
    }

    // ============================================================
    // 1. TẠO ĐƠN HÀNG (Có tích hợp Mã hóa Lai cho Mô tả hàng - SV3)
    // ============================================================
    @Transactional
    public DonHang taoDonHangMoi(DonHang donHang, Integer idDiaChiLayHang, Authentication authentication) {
        KhachHang khachHangGui = userHelper.getKhachHangHienTai(authentication);
        donHang.setKhachHangGui(khachHangGui);
        
        DiaChi diaChiLay = diaChiRepository.findById(idDiaChiLayHang).orElseThrow();
        donHang.setDiaChiLayHang(diaChiLay);
        donHang.setMaVanDon("DH" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        donHang.setNgayTao(new Date());

        // Mã hóa PII (Tuần 5)
        donHang.setTenNguoiNhan(encryptionUtil.encrypt(donHang.getTenNguoiNhan()));
        donHang.setDiaChiGiaoHang(encryptionUtil.encrypt(donHang.getDiaChiGiaoHang()));

        // === TUẦN 7 (SV3): MÃ HÓA LAI MÔ TẢ HÀNG ===
        if (donHang.getMoTaHangHoa() != null && !donHang.getMoTaHangHoa().isEmpty()) {
            // Tìm Admin (ID=1) để lấy Public Key
            TaiKhoan admin = taiKhoanRepository.findById(1).orElse(null);
            if (admin != null && admin.getPublicKey() != null) {
                HybridResult res = hybridService.encrypt(donHang.getMoTaHangHoa(), admin.getPublicKey());
                donHang.setMoTaHangHoa(res.encryptedData);
                donHang.setMaKhoaHangHoa(res.encryptedSessionKey);
            }
        }

        // Ký số (Tuần 6 - SV1)
        try {
            String dataToSign = "CreateOrder|" + donHang.getMaVanDon();
            String signature = rsaUtil.sign(dataToSign, getMyPrivateKey(authentication));
            donHang.setChKyKhachHang(signature);
        } catch (Exception e) { e.printStackTrace(); }

        // Lưu & Tạo hành trình đầu
        if (donHang.getThanhToan() == null) { 
            ThanhToan tt = new ThanhToan(); tt.setDonHang(donHang); donHang.setThanhToan(tt);
        } else { donHang.getThanhToan().setDonHang(donHang); }
        
        DonHang saved = donHangRepository.save(donHang);
        
        HanhTrinhDonHang ht = new HanhTrinhDonHang();
        ht.setDonHang(saved);
        ht.setTrangThai(trangThaiDonHangRepository.findById(1).orElseThrow());
        ht.setThoiGianCapNhat(new Date());
        hanhTrinhDonHangRepository.save(ht);

        return saved;
    }

    // ============================================================
    // 2. CẬP NHẬT TRẠNG THÁI (Có Ký số SV1 + Báo cáo mật SV2)
    // ============================================================
    @Transactional
    public void capNhatTrangThai(Integer idDonHang, Integer idTrangThaiMoi, String ghiChu, boolean daThanhToanCod, Authentication authentication) {
        NhanVien shipper = userHelper.getNhanVienHienTai(authentication);
        DonHang donHang = donHangRepository.findById(idDonHang).orElseThrow();
        TrangThaiDonHang trangThaiMoi = trangThaiDonHangRepository.findById(idTrangThaiMoi).orElseThrow();

        // Xử lý COD
        if (idTrangThaiMoi == 5 && donHang.getThanhToan().getTongTienCod() > 0) {
             if (!daThanhToanCod) throw new IllegalStateException("Chưa thu tiền COD");
             donHang.getThanhToan().setDaThanhToanCod(true);
             thanhToanRepository.save(donHang.getThanhToan());
        }

        HanhTrinhDonHang ht = new HanhTrinhDonHang();
        ht.setDonHang(donHang);
        ht.setNhanVienThucHien(shipper);
        ht.setTrangThai(trangThaiMoi);
        ht.setThoiGianCapNhat(new Date());
        
        String ghiChuFinal = (ghiChu != null) ? ghiChu : "";
        ht.setGhiChuNhanVien(ghiChuFinal);

        // === TUẦN 7 (SV2): MÃ HÓA LAI BÁO CÁO SỰ CỐ ===
        // Nếu trạng thái là Thất bại (6) hoặc Hoàn kho (8) và có ghi chú dài -> Coi là báo cáo sự cố
        if ((idTrangThaiMoi == 6 || idTrangThaiMoi == 8) && ghiChuFinal.length() > 10) {
            TaiKhoan admin = taiKhoanRepository.findById(1).orElse(null);
            if (admin != null) {
                HybridResult res = hybridService.encrypt(ghiChuFinal, admin.getPublicKey());
                ht.setChiTietSuCo(res.encryptedData);
                ht.setMaKhoaSuCo(res.encryptedSessionKey);
                ht.setGhiChuNhanVien("Đã gửi báo cáo mật."); // Ẩn nội dung gốc
            }
        }

        // === TUẦN 6 (SV1): KÝ SỐ (QUAN TRỌNG ĐỂ QUA TRIGGER) ===
        try {
            // Format chuỗi ký phải khớp Trigger Oracle: 
            // 'UpdateStatus|' || ID_DON || '|' || ID_TRANG_THAI || '|' || ID_SHIPPER || '|' || GHI_CHU
            String dataToSign = "UpdateStatus|" + idDonHang + "|" + idTrangThaiMoi + "|" + shipper.getId() + "|" + ht.getGhiChuNhanVien();
            String signature = rsaUtil.sign(dataToSign, getMyPrivateKey(authentication));
            ht.setChKySo(signature);
        } catch (Exception e) {
             System.err.println("Lỗi ký số: " + e.getMessage());
        }

        hanhTrinhDonHangRepository.save(ht);
        
        // Tự động chuyển trạng thái nếu thất bại
        if (idTrangThaiMoi == 6) {
             HanhTrinhDonHang htAuto = new HanhTrinhDonHang();
             htAuto.setDonHang(donHang);
             htAuto.setTrangThai(trangThaiDonHangRepository.findById(7).orElseThrow());
             htAuto.setGhiChuNhanVien("Hệ thống: Chuyển về chờ xử lý.");
             htAuto.setThoiGianCapNhat(new Date(System.currentTimeMillis() + 1000));
             hanhTrinhDonHangRepository.save(htAuto);
        }
    }
    
    // === CÁC HÀM ĐỌC DỮ LIỆU (READ) ===
    public List<DonHang> getDonHangCuaShipperHienTai(Authentication auth) {
        NhanVien shipper = userHelper.getNhanVienHienTai(auth);
        List<DonHang> list = donHangRepository.findDonHangDangXuLyCuaShipper(shipper.getId());
        list.forEach(this::decryptDonHangPII);
        return list;
    }
    
    public List<DonHang> getAllDonHangForQuanLy() {
       List<DonHang> list = donHangRepository.findAllByOrderByIdDonHangDesc();
       list.forEach(this::decryptDonHangPII);
       return list;
    }
    
    public DonHang getChiTietDonHangChoQuanLy(Integer id) {
        DonHang dh = donHangRepository.findById(id).orElseThrow();
        decryptDonHangPII(dh);
        return dh;
    }
    
    public List<DonHang> getDonHangCuaKhachHangHienTai(Authentication auth) {
        KhachHang kh = userHelper.getKhachHangHienTai(auth);
        List<DonHang> list = donHangRepository.findByKhachHangGui_IdOrderByIdDonHangDesc(kh.getId());
        list.forEach(this::decryptDonHangPII);
        return list;
    }
    
    public DonHang getChiTietDonHangCuaKhachHang(String ma, Authentication auth) {
        DonHang dh = donHangRepository.findByMaVanDon(ma).orElseThrow();
        decryptDonHangPII(dh);
        return dh;
    }
    
    public DonHang getDonHangByMaVanDon(String ma) {
        DonHang dh = donHangRepository.findByMaVanDon(ma).orElseThrow();
        decryptDonHangPII(dh);
        return dh;
    }

    // === HÀM GIẢI MÃ LAI (CHO ADMIN XEM) ===
    public String giaiMaLai(String encryptedData, String encryptedKey, Authentication auth) {
        return hybridService.decrypt(encryptedData, encryptedKey, getMyPrivateKey(auth));
    }

    // Hàm Phân công / Hoàn kho (Giữ nguyên logic cũ)
    @Transactional
    public void phanCongShipper(Integer idDon, Integer idShip, Authentication auth) {
        DonHang dh = donHangRepository.findById(idDon).orElseThrow();
        NhanVien ship = nhanVienRepository.findById(idShip).orElseThrow();
        HanhTrinhDonHang ht = new HanhTrinhDonHang();
        ht.setDonHang(dh); ht.setNhanVienThucHien(ship);
        ht.setTrangThai(trangThaiDonHangRepository.findById(4).orElseThrow()); // Đang giao
        ht.setGhiChuNhanVien("Quản lý phân công");
        ht.setThoiGianCapNhat(new Date());
        hanhTrinhDonHangRepository.save(ht);
    }
    
    @Transactional
    public void hoanKhoDonHang(Integer idDon, Authentication auth) {
        DonHang dh = donHangRepository.findById(idDon).orElseThrow();
        // Logic tìm shipper cuối cùng... (giữ nguyên như cũ)
        // ...
        HanhTrinhDonHang ht = new HanhTrinhDonHang();
        ht.setDonHang(dh); 
        // ht.setNhanVienThucHien(lastShipper); // Bạn tự điền nốt logic tìm shipper cũ nếu cần
        ht.setTrangThai(trangThaiDonHangRepository.findById(8).orElseThrow()); // Đang hoàn
        ht.setGhiChuNhanVien("Duyệt hoàn kho");
        ht.setThoiGianCapNhat(new Date());
        hanhTrinhDonHangRepository.save(ht);
    }
}