package com.nhom12.doangiaohang.service;

import com.nhom12.doangiaohang.model.*;
import com.nhom12.doangiaohang.repository.*;
import com.nhom12.doangiaohang.utils.EncryptionUtil;
import com.nhom12.doangiaohang.utils.RSAUtil;
import com.nhom12.doangiaohang.service.HybridEncryptionService.HybridResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
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

    private void decryptDonHangPII(DonHang donHang) {
        if (donHang != null) {
            try {
                donHang.setTenNguoiNhan(encryptionUtil.decrypt(donHang.getTenNguoiNhan()));
                donHang.setDiaChiGiaoHang(encryptionUtil.decrypt(donHang.getDiaChiGiaoHang()));
                if (donHang.getDiaChiLayHang() != null) {
                    donHang.getDiaChiLayHang().setSoNhaDuong(encryptionUtil.decrypt(donHang.getDiaChiLayHang().getSoNhaDuong()));
                }
                if (donHang.getHanhTrinh() != null) {
                    for (HanhTrinhDonHang ht : donHang.getHanhTrinh()) {
                        if (ht.getNhanVienThucHien() != null) {
                            String tenMaHoa = ht.getNhanVienThucHien().getHoTen();
                            ht.getNhanVienThucHien().setHoTen(encryptionUtil.decrypt(tenMaHoa));
                        }
                    }
                }
            } catch (Exception e) { }
        }
    }

    // Hàm phụ trợ tìm Admin động (Thay vì fix cứng ID=1)
    private TaiKhoan findSystemAdmin() {
        return taiKhoanRepository.findAll().stream()
                .filter(t -> t.getVaiTro().getIdVaiTro() == 1) // 1 = Role Quản lý
                .findFirst()
                .orElse(null);
    }

    @Transactional
    public DonHang taoDonHangMoi(DonHang donHang, Integer idDiaChiLayHang, Authentication authentication) {
        KhachHang khachHangGui = userHelper.getKhachHangHienTai(authentication);
        donHang.setKhachHangGui(khachHangGui);
        
        DiaChi diaChiLay = diaChiRepository.findById(idDiaChiLayHang)
                .orElseThrow(() -> new IllegalArgumentException("Địa chỉ lấy hàng không tồn tại"));
        donHang.setDiaChiLayHang(diaChiLay);
        
        donHang.setMaVanDon("DH" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        donHang.setNgayTao(new Date());

        String tenGoc = donHang.getTenNguoiNhan();
        String diaChiGoc = donHang.getDiaChiGiaoHang();
        String moTaHangHoaGoc = donHang.getMoTaHangHoa();

        donHang.setTenNguoiNhan(encryptionUtil.encrypt(tenGoc));
        donHang.setDiaChiGiaoHang(encryptionUtil.encrypt(diaChiGoc));

        // MÃ HÓA LAI (SỬA LỖI: Tìm Admin động)
        if (donHang.getMoTaHangHoa() != null && !donHang.getMoTaHangHoa().trim().isEmpty()) {
            TaiKhoan admin = findSystemAdmin();
            if (admin != null && admin.getPublicKey() != null) {
                HybridResult res = hybridService.encrypt(donHang.getMoTaHangHoa(), admin.getPublicKey());
                donHang.setMoTaHangHoa(res.encryptedData);
                donHang.setMaKhoaHangHoa(res.encryptedSessionKey);
            }
        } else {
            donHang.setMoTaHangHoa(null);
            donHang.setMaKhoaHangHoa(null);
        }

        try {
            String dataToSign = "CreateOrder|" + donHang.getMaVanDon();
            String signature = rsaUtil.sign(dataToSign, getMyPrivateKey(authentication));
            donHang.setChKyKhachHang(signature);
        } catch (Exception e) { 
            throw new RuntimeException("Lỗi ký số: " + e.getMessage());
        }

        if (donHang.getThanhToan() == null) { 
            ThanhToan tt = new ThanhToan();
            tt.setDonHang(donHang);
            tt.setTongTienCod(0.0);
            donHang.setThanhToan(tt);
        } else {
            donHang.getThanhToan().setDonHang(donHang); 
            if(donHang.getThanhToan().getTongTienCod() == null) {
                donHang.getThanhToan().setTongTienCod(0.0);
            }
        }
        
        DonHang saved = donHangRepository.save(donHang);
        
        HanhTrinhDonHang ht = new HanhTrinhDonHang();
        ht.setDonHang(saved);
        ht.setTrangThai(trangThaiDonHangRepository.findById(1).orElseThrow());
        ht.setThoiGianCapNhat(new Date());
        hanhTrinhDonHangRepository.save(ht);

        saved.setTenNguoiNhan(tenGoc);
        saved.setDiaChiGiaoHang(diaChiGoc);
        saved.setMoTaHangHoa(moTaHangHoaGoc);

        return saved;
    }

    @Transactional
    public void capNhatTrangThai(Integer idDonHang, Integer idTrangThaiMoi, String ghiChu, boolean daThanhToanCod, Authentication authentication) {
        NhanVien shipper = userHelper.getNhanVienHienTai(authentication);
        DonHang donHang = donHangRepository.findById(idDonHang)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại"));
        
        TrangThaiDonHang ttHienTai = donHang.getTrangThaiHienTai();
        int idHienTai = ttHienTai.getIdTrangThai();

        if (idHienTai != 1 && idHienTai != 7) { 
             NhanVien shipperDangGiu = donHang.getShipperHienTai();
             if (shipperDangGiu != null && !shipperDangGiu.getId().equals(shipper.getId())) {
                 throw new SecurityException("BÁO ĐỘNG: Bạn không có quyền can thiệp vào đơn hàng của Shipper khác!");
             }
        }

        // ... (Logic chuyển trạng thái giữ nguyên) ...
        // Để ngắn gọn, tôi lược bỏ phần kiểm tra trạng thái hợp lệ ở đây (bạn giữ nguyên code cũ phần này)

        TrangThaiDonHang trangThaiMoi = trangThaiDonHangRepository.findById(idTrangThaiMoi)
                .orElseThrow(() -> new IllegalArgumentException("Trạng thái mới không hợp lệ"));

        if (idTrangThaiMoi == 5 && donHang.getThanhToan().getTongTienCod() > 0) {
             if (!daThanhToanCod) throw new IllegalStateException("Bạn chưa xác nhận đã thu đủ tiền COD.");
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

        // MÃ HÓA LAI BÁO CÁO SỰ CỐ (SỬA LỖI: Tìm Admin động)
        // Điều kiện: Trạng thái Thất bại (6) hoặc Hoàn kho (8) VÀ Ghi chú dài > 10 ký tự
        if ((idTrangThaiMoi == 6 || idTrangThaiMoi == 8) && ghiChuFinal.length() > 10) {
            TaiKhoan admin = findSystemAdmin();
            if (admin != null) {
                HybridResult res = hybridService.encrypt(ghiChuFinal, admin.getPublicKey());
                ht.setChiTietSuCo(res.encryptedData);
                ht.setMaKhoaSuCo(res.encryptedSessionKey);
                ht.setGhiChuNhanVien("Đã gửi báo cáo mật (Chỉ Admin xem được).");
            }
        }

        try {
            String dataToSign = "UpdateStatus|" + idDonHang + "|" + idTrangThaiMoi + "|" + shipper.getId() + "|" + ghiChuFinal;
            String signature = rsaUtil.sign(dataToSign, getMyPrivateKey(authentication));
            ht.setChKySo(signature);
        } catch (Exception e) {
             System.err.println("Lỗi ký số Shipper: " + e.getMessage());
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
        
        // GIẢI MÃ LAI (SỬA LỖI: Dùng Key của chính Admin đang đăng nhập)
        TaiKhoan currentAdmin = userHelper.getTaiKhoanHienTai(SecurityContextHolder.getContext().getAuthentication());
        String adminPrivKey = null;
        if (currentAdmin != null && currentAdmin.getPrivateKey() != null) {
            try {
                adminPrivKey = encryptionUtil.decrypt(currentAdmin.getPrivateKey());
            } catch (Exception e) { }
        }
        
        if (adminPrivKey != null) {
            if (dh.getMoTaHangHoa() != null && dh.getMaKhoaHangHoa() != null) {
                try {
                    String moTaDecrypted = hybridService.decrypt(dh.getMoTaHangHoa(), dh.getMaKhoaHangHoa(), adminPrivKey);
                    dh.setMoTaHangHoa(moTaDecrypted);
                } catch (Exception e) {
                    dh.setMoTaHangHoa("[Không thể giải mã]");
                }
            }
            if (dh.getHanhTrinh() != null) {
                for (HanhTrinhDonHang ht : dh.getHanhTrinh()) {
                    if (ht.getChiTietSuCo() != null && ht.getMaKhoaSuCo() != null) {
                        try {
                            String suCoDecrypted = hybridService.decrypt(ht.getChiTietSuCo(), ht.getMaKhoaSuCo(), adminPrivKey);
                            ht.setChiTietSuCo(suCoDecrypted);
                        } catch (Exception e) {
                            ht.setChiTietSuCo("[Không thể giải mã]");
                        }
                    }
                }
            }
        }
        return dh;
    }
    
    // ... (Các hàm khác giữ nguyên: getDonHangCuaKhachHangHienTai, getDonHangByMaVanDon, phanCongShipper, huyDonHang...)
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
        DonHang dh = donHangRepository.findByMaVanDon(ma)
                .orElseThrow(() -> new IllegalArgumentException("Mã vận đơn không tồn tại"));
        decryptDonHangPII(dh);
        return dh;
    }

    @Transactional
    public void phanCongShipper(Integer idDon, Integer idShip, Authentication auth) {
        DonHang dh = donHangRepository.findById(idDon).orElseThrow();
        NhanVien ship = nhanVienRepository.findById(idShip).orElseThrow();
        
        String tenShipperRo = ship.getHoTen(); 
        try {
            tenShipperRo = encryptionUtil.decrypt(ship.getHoTen());
        } catch (Exception e) { }
        
        HanhTrinhDonHang ht = new HanhTrinhDonHang();
        ht.setDonHang(dh); 
        ht.setNhanVienThucHien(ship);
        ht.setTrangThai(trangThaiDonHangRepository.findById(1).orElseThrow()); 
        
        ht.setGhiChuNhanVien("Quản lý đã phân công cho Shipper: " + tenShipperRo);
        ht.setThoiGianCapNhat(new Date());
        
        hanhTrinhDonHangRepository.save(ht);
    }
    @Transactional
    public void hoanKhoDonHang(Integer idDon, Authentication auth) {
        DonHang dh = donHangRepository.findById(idDon).orElseThrow();
        HanhTrinhDonHang ht = new HanhTrinhDonHang();
        ht.setDonHang(dh); 
        ht.setTrangThai(trangThaiDonHangRepository.findById(9).orElseThrow()); 
        ht.setGhiChuNhanVien("Admin xác nhận đã nhận lại hàng về kho.");
        ht.setThoiGianCapNhat(new Date());
        hanhTrinhDonHangRepository.save(ht);
    }

    @Transactional
    public void huyDonHang(Integer idDonHang, Authentication authentication) {
        KhachHang kh = userHelper.getKhachHangHienTai(authentication);
        DonHang donHang = donHangRepository.findById(idDonHang)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại."));

        if (!donHang.getKhachHangGui().getId().equals(kh.getId())) {
            throw new SecurityException("Bạn không có quyền hủy đơn hàng của người khác.");
        }

        if (donHang.getTrangThaiHienTai().getIdTrangThai() != 1) {
            throw new IllegalStateException("Đơn hàng đã được tiếp nhận hoặc đang xử lý, không thể hủy.");
        }

        HanhTrinhDonHang ht = new HanhTrinhDonHang();
        ht.setDonHang(donHang);
        ht.setTrangThai(trangThaiDonHangRepository.findById(10)
                .orElseThrow(() -> new IllegalArgumentException("Lỗi hệ thống: Chưa cấu hình trạng thái Hủy (ID 10)"))); 
        ht.setGhiChuNhanVien("Khách hàng chủ động hủy đơn.");
        ht.setThoiGianCapNhat(new Date());

        try {
            String dataToSign = "CancelOrder|" + idDonHang + "|" + kh.getId();
            String signature = rsaUtil.sign(dataToSign, getMyPrivateKey(authentication));
            ht.setChKySo(signature);
        } catch (Exception e) {
            System.err.println("Lỗi ký số khi hủy đơn: " + e.getMessage());
        }

        hanhTrinhDonHangRepository.save(ht);
    }
}