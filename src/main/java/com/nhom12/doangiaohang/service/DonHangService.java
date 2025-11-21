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
    @Autowired private HybridEncryptionService hybridService; 

    private String getMyPrivateKey(Authentication authentication) {
        TaiKhoan tk = userHelper.getTaiKhoanHienTai(authentication);
        if (tk == null || tk.getPrivateKey() == null) {
            throw new RuntimeException("Tài khoản chưa có Key RSA.");
        }
        return encryptionUtil.decrypt(tk.getPrivateKey());
    }

    // === HÀM GIẢI MÃ ĐÃ ĐƯỢC NÂNG CẤP (FIX LỖI HIỂN THỊ TÊN SHIPPER) ===
    private void decryptDonHangPII(DonHang donHang) {
        if (donHang != null) {
            try {
                // 1. Thông tin khách hàng
                donHang.setTenNguoiNhan(encryptionUtil.decrypt(donHang.getTenNguoiNhan()));
                donHang.setDiaChiGiaoHang(encryptionUtil.decrypt(donHang.getDiaChiGiaoHang()));
                if (donHang.getDiaChiLayHang() != null) {
                    donHang.getDiaChiLayHang().setSoNhaDuong(encryptionUtil.decrypt(donHang.getDiaChiLayHang().getSoNhaDuong()));
                }
                
                // 2. Tên Shipper trong lịch sử hành trình
                if (donHang.getHanhTrinh() != null) {
                    for (HanhTrinhDonHang ht : donHang.getHanhTrinh()) {
                        if (ht.getNhanVienThucHien() != null) {
                            String tenMaHoa = ht.getNhanVienThucHien().getHoTen();
                            ht.getNhanVienThucHien().setHoTen(encryptionUtil.decrypt(tenMaHoa));
                        }
                    }
                }
            } catch (Exception e) { /* Ignore */ }
        }
    }

    @Transactional
    public DonHang taoDonHangMoi(DonHang donHang, Integer idDiaChiLayHang, Authentication authentication) {
        KhachHang khachHangGui = userHelper.getKhachHangHienTai(authentication);
        donHang.setKhachHangGui(khachHangGui);
        
        DiaChi diaChiLay = diaChiRepository.findById(idDiaChiLayHang).orElseThrow();
        donHang.setDiaChiLayHang(diaChiLay);
        donHang.setMaVanDon("DH" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        donHang.setNgayTao(new Date());

        donHang.setTenNguoiNhan(encryptionUtil.encrypt(donHang.getTenNguoiNhan()));
        donHang.setDiaChiGiaoHang(encryptionUtil.encrypt(donHang.getDiaChiGiaoHang()));

        if (donHang.getMoTaHangHoa() != null && !donHang.getMoTaHangHoa().isEmpty()) {
            TaiKhoan admin = taiKhoanRepository.findById(1).orElse(null);
            if (admin != null && admin.getPublicKey() != null) {
                HybridResult res = hybridService.encrypt(donHang.getMoTaHangHoa(), admin.getPublicKey());
                donHang.setMoTaHangHoa(res.encryptedData);
                donHang.setMaKhoaHangHoa(res.encryptedSessionKey);
            }
        }

        try {
            String dataToSign = "CreateOrder|" + donHang.getMaVanDon();
            String signature = rsaUtil.sign(dataToSign, getMyPrivateKey(authentication));
            donHang.setChKyKhachHang(signature);
        } catch (Exception e) { e.printStackTrace(); }

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

    @Transactional
    public void capNhatTrangThai(Integer idDonHang, Integer idTrangThaiMoi, String ghiChu, boolean daThanhToanCod, Authentication authentication) {
        NhanVien shipper = userHelper.getNhanVienHienTai(authentication);
        DonHang donHang = donHangRepository.findById(idDonHang).orElseThrow();
        TrangThaiDonHang trangThaiMoi = trangThaiDonHangRepository.findById(idTrangThaiMoi).orElseThrow();

        if (idTrangThaiMoi == 5 && donHang.getThanhToan().getTongTienCod() > 0) {
             if (!daThanhToanCod) throw new IllegalStateException("Chưa xác nhận thu tiền COD.");
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

        if ((idTrangThaiMoi == 6 || idTrangThaiMoi == 8) && ghiChuFinal.length() > 10) {
            TaiKhoan admin = taiKhoanRepository.findById(1).orElse(null);
            if (admin != null) {
                HybridResult res = hybridService.encrypt(ghiChuFinal, admin.getPublicKey());
                ht.setChiTietSuCo(res.encryptedData);
                ht.setMaKhoaSuCo(res.encryptedSessionKey);
                ht.setGhiChuNhanVien("Đã gửi báo cáo mật."); 
            }
        }

        try {
            String dataToSign = "UpdateStatus|" + idDonHang + "|" + idTrangThaiMoi + "|" + shipper.getId() + "|" + ht.getGhiChuNhanVien();
            String signature = rsaUtil.sign(dataToSign, getMyPrivateKey(authentication));
            ht.setChKySo(signature);
        } catch (Exception e) {
             System.err.println("Lỗi ký số: " + e.getMessage());
        }

        hanhTrinhDonHangRepository.save(ht);
        
        if (idTrangThaiMoi == 6) {
             HanhTrinhDonHang htAuto = new HanhTrinhDonHang();
             htAuto.setDonHang(donHang);
             htAuto.setTrangThai(trangThaiDonHangRepository.findById(7).orElseThrow());
             htAuto.setGhiChuNhanVien("Hệ thống: Chuyển về chờ xử lý.");
             htAuto.setThoiGianCapNhat(new Date(System.currentTimeMillis() + 1000));
             hanhTrinhDonHangRepository.save(htAuto);
        }
    }
    
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
    
    // === ĐÃ SỬA: Dùng orElseThrow để báo lỗi rõ ràng ===
    public DonHang getDonHangByMaVanDon(String ma) {
        DonHang dh = donHangRepository.findByMaVanDon(ma)
                .orElseThrow(() -> new IllegalArgumentException("Mã vận đơn không tồn tại"));
        decryptDonHangPII(dh);
        return dh;
    }

    public String giaiMaLai(String encryptedData, String encryptedKey, Authentication auth) {
        return hybridService.decrypt(encryptedData, encryptedKey, getMyPrivateKey(auth));
    }

    @Transactional
    public void phanCongShipper(Integer idDon, Integer idShip, Authentication auth) {
        DonHang dh = donHangRepository.findById(idDon).orElseThrow();
        NhanVien ship = nhanVienRepository.findById(idShip).orElseThrow();
        HanhTrinhDonHang ht = new HanhTrinhDonHang();
        ht.setDonHang(dh); ht.setNhanVienThucHien(ship);
        ht.setTrangThai(trangThaiDonHangRepository.findById(4).orElseThrow());
        ht.setGhiChuNhanVien("Quản lý phân công");
        ht.setThoiGianCapNhat(new Date());
        hanhTrinhDonHangRepository.save(ht);
    }
    
    @Transactional
    public void hoanKhoDonHang(Integer idDon, Authentication auth) {
        DonHang dh = donHangRepository.findById(idDon).orElseThrow();
        HanhTrinhDonHang ht = new HanhTrinhDonHang();
        ht.setDonHang(dh); 
        // (Có thể thêm logic tìm shipper cũ ở đây nếu cần)
        ht.setTrangThai(trangThaiDonHangRepository.findById(8).orElseThrow());
        ht.setGhiChuNhanVien("Duyệt hoàn kho");
        ht.setThoiGianCapNhat(new Date());
        hanhTrinhDonHangRepository.save(ht);
    }
}