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
            throw new RuntimeException("Tài khoản chưa có Key RSA. Vui lòng đăng ký tài khoản mới.");
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

        if (donHang.getMoTaHangHoa() != null && !donHang.getMoTaHangHoa().trim().isEmpty()) {
            TaiKhoan admin = taiKhoanRepository.findById(1).orElse(null);
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
            throw new RuntimeException("Lỗi tạo chữ ký số: " + e.getMessage());
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

        // 1. Kiểm tra IDOR: Shipper có được giao đơn này không?
        if (idHienTai != 1 && idHienTai != 7) { 
             NhanVien shipperDangGiu = donHang.getShipperHienTai();
             if (shipperDangGiu != null && !shipperDangGiu.getId().equals(shipper.getId())) {
                 throw new SecurityException("BÁO ĐỘNG: Bạn không có quyền can thiệp vào đơn hàng của Shipper khác!");
             }
        }

        // 2. Kiểm tra Logic chuyển trạng thái (State Machine)
        boolean isValidTransition = false;
        if (idHienTai == 1 && idTrangThaiMoi == 2) isValidTransition = true; // Chờ lấy -> Đã lấy
        else if (idHienTai == 2 && idTrangThaiMoi == 4) isValidTransition = true; // Đã lấy -> Đang giao
        else if (idHienTai == 4 && (idTrangThaiMoi == 5 || idTrangThaiMoi == 6)) isValidTransition = true; // Đang giao -> Xong/Lỗi
        else if (idHienTai == 6 && idTrangThaiMoi == 8) isValidTransition = true; // Lỗi -> Đang hoàn
        else if (idHienTai == 8 && idTrangThaiMoi == 9) isValidTransition = true; // Đang hoàn -> Đã hoàn
        else if (idHienTai == 7 && idTrangThaiMoi == 4) isValidTransition = true; // Chờ xử lý -> Giao lại
        
        // Admin phân công (ID=1 có Shipper) -> Shipper bấm Đã lấy (ID=2)
        if (idHienTai == 1 && donHang.getShipperHienTai() != null && idTrangThaiMoi == 2) isValidTransition = true;

        if (!isValidTransition) {
            throw new IllegalArgumentException("Lỗi Logic: Không thể chuyển từ trạng thái [" 
                    + ttHienTai.getTenTrangThai() + "] sang trạng thái ID [" + idTrangThaiMoi + "]");
        }
        
        // 3. Kiểm tra Race Condition
        if (idHienTai == 1 && idTrangThaiMoi == 2) {
            NhanVien assignedShipper = donHang.getShipperHienTai();
            if (assignedShipper != null && !assignedShipper.getId().equals(shipper.getId())) {
                throw new IllegalStateException("Đơn này đã được Admin phân công cho Shipper khác.");
            }
        }

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
        TaiKhoan admin = taiKhoanRepository.findById(1).orElse(null);
        String adminPrivKey = null;
        if (admin != null && admin.getPrivateKey() != null) {
            try {
                adminPrivKey = encryptionUtil.decrypt(admin.getPrivateKey());
            } catch (Exception e) { }
        }
        if (adminPrivKey != null) {
            if (dh.getMoTaHangHoa() != null && dh.getMaKhoaHangHoa() != null) {
                try {
                    String moTaDecrypted = hybridService.decrypt(dh.getMoTaHangHoa(), dh.getMaKhoaHangHoa(), adminPrivKey);
                    dh.setMoTaHangHoa(moTaDecrypted);
                } catch (Exception e) {
                    dh.setMoTaHangHoa("[Không thể giải mã hàng hóa]");
                }
            }
            if (dh.getHanhTrinh() != null) {
                for (HanhTrinhDonHang ht : dh.getHanhTrinh()) {
                    if (ht.getChiTietSuCo() != null && ht.getMaKhoaSuCo() != null) {
                        try {
                            String suCoDecrypted = hybridService.decrypt(ht.getChiTietSuCo(), ht.getMaKhoaSuCo(), adminPrivKey);
                            ht.setChiTietSuCo(suCoDecrypted);
                        } catch (Exception e) {
                            ht.setChiTietSuCo("[Không thể giải mã sự cố]");
                        }
                    }
                }
            }
        }
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
        } catch (Exception e) {
        }
        
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

    // === TÍNH NĂNG MỚI: KHÁCH HÀNG HỦY ĐƠN ===
    @Transactional
    public void huyDonHang(Integer idDonHang, Authentication authentication) {
        // 1. Lấy thông tin khách hàng và đơn hàng
        KhachHang kh = userHelper.getKhachHangHienTai(authentication);
        DonHang donHang = donHangRepository.findById(idDonHang)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại."));

        // 2. Bảo mật: Kiểm tra chính chủ (IDOR)
        if (!donHang.getKhachHangGui().getId().equals(kh.getId())) {
            throw new SecurityException("Bạn không có quyền hủy đơn hàng của người khác.");
        }

        // 3. Logic: Chỉ cho phép hủy khi trạng thái là "Chờ lấy hàng" (ID = 1)
        if (donHang.getTrangThaiHienTai().getIdTrangThai() != 1) {
            throw new IllegalStateException("Đơn hàng đã được tiếp nhận hoặc đang xử lý, không thể hủy.");
        }

        // 4. Tạo hành trình "Đã hủy" (ID = 10)
        HanhTrinhDonHang ht = new HanhTrinhDonHang();
        ht.setDonHang(donHang);
        // ID 10: Đã hủy (Phải insert trong DB trước)
        ht.setTrangThai(trangThaiDonHangRepository.findById(10)
                .orElseThrow(() -> new IllegalArgumentException("Lỗi hệ thống: Chưa cấu hình trạng thái Hủy (ID 10)"))); 
        ht.setGhiChuNhanVien("Khách hàng chủ động hủy đơn.");
        ht.setThoiGianCapNhat(new Date());

        // 5. Bảo mật: Tạo chữ ký số cho hành động Hủy (Non-repudiation)
        try {
            // Chuỗi ký: "CancelOrder|ID_DON|ID_KHACH"
            String dataToSign = "CancelOrder|" + idDonHang + "|" + kh.getId();
            String signature = rsaUtil.sign(dataToSign, getMyPrivateKey(authentication));
            ht.setChKySo(signature);
        } catch (Exception e) {
            // Log lỗi nhưng vẫn cho phép hủy (hoặc throw tùy policy)
            System.err.println("Lỗi ký số khi hủy đơn: " + e.getMessage());
        }

        hanhTrinhDonHangRepository.save(ht);
    }
}