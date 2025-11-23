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

    // Hàm hỗ trợ lấy Private Key của người dùng hiện tại
    private String getMyPrivateKey(Authentication authentication) {
        TaiKhoan tk = userHelper.getTaiKhoanHienTai(authentication);
        if (tk == null || tk.getPrivateKey() == null) {
            throw new RuntimeException("Tài khoản chưa có Key RSA. Vui lòng đăng ký tài khoản mới.");
        }
        return encryptionUtil.decrypt(tk.getPrivateKey());
    }

    // Hàm giải mã thông tin PII cơ bản (Tên, Địa chỉ) để hiển thị lên Web
    private void decryptDonHangPII(DonHang donHang) {
        if (donHang != null) {
            try {
                donHang.setTenNguoiNhan(encryptionUtil.decrypt(donHang.getTenNguoiNhan()));
                donHang.setDiaChiGiaoHang(encryptionUtil.decrypt(donHang.getDiaChiGiaoHang()));
                
                if (donHang.getDiaChiLayHang() != null) {
                    donHang.getDiaChiLayHang().setSoNhaDuong(encryptionUtil.decrypt(donHang.getDiaChiLayHang().getSoNhaDuong()));
                }
                
                // Giải mã tên Shipper trong lịch sử hành trình
                if (donHang.getHanhTrinh() != null) {
                    for (HanhTrinhDonHang ht : donHang.getHanhTrinh()) {
                        if (ht.getNhanVienThucHien() != null) {
                            String tenMaHoa = ht.getNhanVienThucHien().getHoTen();
                            ht.getNhanVienThucHien().setHoTen(encryptionUtil.decrypt(tenMaHoa));
                        }
                    }
                }
            } catch (Exception e) { 
                // Bỏ qua lỗi giải mã nếu dữ liệu không phải là ciphertext hợp lệ
            }
        }
    }

    // === CHỨC NĂNG 1: TẠO ĐƠN HÀNG MỚI (KHÁCH HÀNG) ===
    @Transactional
    public DonHang taoDonHangMoi(DonHang donHang, Integer idDiaChiLayHang, Authentication authentication) {
        KhachHang khachHangGui = userHelper.getKhachHangHienTai(authentication);
        donHang.setKhachHangGui(khachHangGui);
        
        DiaChi diaChiLay = diaChiRepository.findById(idDiaChiLayHang)
                .orElseThrow(() -> new IllegalArgumentException("Địa chỉ lấy hàng không tồn tại"));
        donHang.setDiaChiLayHang(diaChiLay);
        
        // Tạo mã vận đơn ngẫu nhiên
        donHang.setMaVanDon("DH" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        donHang.setNgayTao(new Date());

        // --- BƯỚC 1: LƯU LẠI GIÁ TRỊ GỐC (PLAINTEXT) ---
        // Để sau khi save() xong có thể gán lại, tránh hiển thị chuỗi mã hóa lên màn hình
        String tenGoc = donHang.getTenNguoiNhan();
        String diaChiGoc = donHang.getDiaChiGiaoHang();
        String moTaHangHoaGoc = donHang.getMoTaHangHoa();

        // --- BƯỚC 2: MÃ HÓA DỮ LIỆU ĐỂ LƯU XUỐNG DB ---
        donHang.setTenNguoiNhan(encryptionUtil.encrypt(tenGoc));
        donHang.setDiaChiGiaoHang(encryptionUtil.encrypt(diaChiGoc));

        // Xử lý Mã hóa Lai cho "Hàng giá trị cao" (Chỉ mã hóa nếu có nhập)
        if (donHang.getMoTaHangHoa() != null && !donHang.getMoTaHangHoa().trim().isEmpty()) {
            // Lấy Public Key của Admin (ID=1) để mã hóa
            TaiKhoan admin = taiKhoanRepository.findById(1).orElse(null);
            if (admin != null && admin.getPublicKey() != null) {
                HybridResult res = hybridService.encrypt(donHang.getMoTaHangHoa(), admin.getPublicKey());
                donHang.setMoTaHangHoa(res.encryptedData);      // Dữ liệu (AES)
                donHang.setMaKhoaHangHoa(res.encryptedSessionKey); // Khóa phiên (RSA)
            }
        } else {
            donHang.setMoTaHangHoa(null);
            donHang.setMaKhoaHangHoa(null);
        }

        // --- BƯỚC 3: TẠO CHỮ KÝ SỐ RSA (CHỐNG CHỐI BỎ) ---
        try {
            // Chuỗi ký phải khớp định dạng với Trigger Oracle: 'CreateOrder|MA_VAN_DON'
            String dataToSign = "CreateOrder|" + donHang.getMaVanDon();
            String signature = rsaUtil.sign(dataToSign, getMyPrivateKey(authentication));
            donHang.setChKyKhachHang(signature);
        } catch (Exception e) { 
            e.printStackTrace();
            throw new RuntimeException("Lỗi tạo chữ ký số: " + e.getMessage());
        }

        // --- BƯỚC 4: XỬ LÝ THANH TOÁN (TRÁNH NULL POINTER) ---
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
        
        // Lưu đơn hàng xuống DB
        DonHang saved = donHangRepository.save(donHang);
        
        // Tạo hành trình đầu tiên: "Chờ lấy hàng"
        HanhTrinhDonHang ht = new HanhTrinhDonHang();
        ht.setDonHang(saved);
        ht.setTrangThai(trangThaiDonHangRepository.findById(1).orElseThrow());
        ht.setThoiGianCapNhat(new Date());
        hanhTrinhDonHangRepository.save(ht);

        // --- BƯỚC 5: KHÔI PHỤC DỮ LIỆU GỐC CHO OBJECT TRẢ VỀ ---
        // Giúp giao diện hiển thị tiếng Việt ngay sau khi tạo
        saved.setTenNguoiNhan(tenGoc);
        saved.setDiaChiGiaoHang(diaChiGoc);
        saved.setMoTaHangHoa(moTaHangHoaGoc);

        return saved;
    }

    // === CHỨC NĂNG 2: CẬP NHẬT TRẠNG THÁI (SHIPPER) ===
    @Transactional
    public void capNhatTrangThai(Integer idDonHang, Integer idTrangThaiMoi, String ghiChu, boolean daThanhToanCod, Authentication authentication) {
        NhanVien shipper = userHelper.getNhanVienHienTai(authentication);
        DonHang donHang = donHangRepository.findById(idDonHang)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại"));
        TrangThaiDonHang trangThaiMoi = trangThaiDonHangRepository.findById(idTrangThaiMoi)
                .orElseThrow(() -> new IllegalArgumentException("Trạng thái không hợp lệ"));

        // Logic xác nhận thu tiền COD
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

        // Mã hóa lai Báo cáo sự cố (Nếu ghi chú dài > 10 ký tự và là trạng thái thất bại/hoàn kho)
        if ((idTrangThaiMoi == 6 || idTrangThaiMoi == 8) && ghiChuFinal.length() > 10) {
            TaiKhoan admin = taiKhoanRepository.findById(1).orElse(null);
            if (admin != null) {
                HybridResult res = hybridService.encrypt(ghiChuFinal, admin.getPublicKey());
                ht.setChiTietSuCo(res.encryptedData);
                ht.setMaKhoaSuCo(res.encryptedSessionKey);
                ht.setGhiChuNhanVien("Đã gửi báo cáo mật."); // Thay nội dung công khai
            }
        }

        // Tạo chữ ký số cho Shipper
        try {
            // Chuỗi ký: 'UpdateStatus|ID_DON|ID_TRANG_THAI|ID_SHIPPER|GHI_CHU'
            String dataToSign = "UpdateStatus|" + idDonHang + "|" + idTrangThaiMoi + "|" + shipper.getId() + "|" + ht.getGhiChuNhanVien();
            String signature = rsaUtil.sign(dataToSign, getMyPrivateKey(authentication));
            ht.setChKySo(signature);
        } catch (Exception e) {
             System.err.println("Lỗi ký số Shipper: " + e.getMessage());
             // Có thể ném ngoại lệ nếu yêu cầu bắt buộc ký thành công
        }

        hanhTrinhDonHangRepository.save(ht);
        
        // Tự động chuyển trạng thái phụ (Ví dụ: Thất bại -> Chờ xử lý lại)
        if (idTrangThaiMoi == 6) {
             HanhTrinhDonHang htAuto = new HanhTrinhDonHang();
             htAuto.setDonHang(donHang);
             htAuto.setTrangThai(trangThaiDonHangRepository.findById(7).orElseThrow());
             htAuto.setGhiChuNhanVien("Hệ thống: Chuyển về chờ xử lý.");
             htAuto.setThoiGianCapNhat(new Date(System.currentTimeMillis() + 1000)); // +1 giây
             hanhTrinhDonHangRepository.save(htAuto);
        }
    }

    // === CÁC HÀM TRUY VẤN (READ) ===

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
    
    // Hàm đặc biệt cho Admin: Giải mã cả "Hàng giá trị cao"
    public DonHang getChiTietDonHangChoQuanLy(Integer id) {
        DonHang dh = donHangRepository.findById(id).orElseThrow();
        
        // A. Giải mã thông tin cơ bản (Tên, Địa chỉ)
        decryptDonHangPII(dh); 
        
        // Lấy Private Key của Admin (ID=1) dùng chung cho các việc giải mã RSA
        TaiKhoan admin = taiKhoanRepository.findById(1).orElse(null);
        String adminPrivKey = null;
        if (admin != null && admin.getPrivateKey() != null) {
            try {
                adminPrivKey = encryptionUtil.decrypt(admin.getPrivateKey());
            } catch (Exception e) { /* Ignore */ }
        }

        if (adminPrivKey != null) {
            // B. Giải mã "Hàng giá trị cao" (Nếu có)
            if (dh.getMoTaHangHoa() != null && dh.getMaKhoaHangHoa() != null) {
                try {
                    String moTaDecrypted = hybridService.decrypt(dh.getMoTaHangHoa(), dh.getMaKhoaHangHoa(), adminPrivKey);
                    dh.setMoTaHangHoa(moTaDecrypted);
                } catch (Exception e) {
                    dh.setMoTaHangHoa("[Không thể giải mã hàng hóa]");
                }
            }

            // C. Giải mã "Báo cáo sự cố" trong Lịch sử hành trình (MỚI THÊM)
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
        // Khách hàng xem lại đơn của mình -> Có thể giải mã Mô tả hàng hóa nếu cần (Logic mở rộng)
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
        
        HanhTrinhDonHang ht = new HanhTrinhDonHang();
        ht.setDonHang(dh); 
        ht.setNhanVienThucHien(ship);
        ht.setTrangThai(trangThaiDonHangRepository.findById(4).orElseThrow()); // Chuyển sang Đang giao
        ht.setGhiChuNhanVien("Quản lý phân công");
        ht.setThoiGianCapNhat(new Date());
        
        hanhTrinhDonHangRepository.save(ht);
    }
    
    @Transactional
    public void hoanKhoDonHang(Integer idDon, Authentication auth) {
        DonHang dh = donHangRepository.findById(idDon).orElseThrow();
        
        HanhTrinhDonHang ht = new HanhTrinhDonHang();
        ht.setDonHang(dh); 
        // Trạng thái 8: Đang hoàn kho (Shipper báo) -> Trạng thái 9: Đã hoàn kho (Admin xác nhận)
        ht.setTrangThai(trangThaiDonHangRepository.findById(9).orElseThrow()); 
        ht.setGhiChuNhanVien("Admin xác nhận đã nhận lại hàng về kho.");
        ht.setThoiGianCapNhat(new Date());
        
        hanhTrinhDonHangRepository.save(ht);
    }
}