package com.nhom12.doangiaohang.service;

import com.nhom12.doangiaohang.model.*;
import com.nhom12.doangiaohang.repository.*;
import com.nhom12.doangiaohang.utils.EncryptionUtil;
import com.nhom12.doangiaohang.utils.RSAUtil;
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
    @Autowired private CustomUserHelper userHelper;
    @Autowired private NhatKyVanHanhService nhatKyVanHanhService;
    @Autowired private EncryptionUtil encryptionUtil;
    @Autowired private RSAUtil rsaUtil;

    /**
     * Giải mã dữ liệu (AES) để hiển thị.
     * Bao gồm giải mã thông tin đơn hàng và địa chỉ lấy hàng.
     */
    private void decryptDonHangPII(DonHang donHang) {
        if (donHang != null) {
            try {
                // 1. Giải mã Đơn hàng (App Level)
                donHang.setTenNguoiNhan(encryptionUtil.decrypt(donHang.getTenNguoiNhan()));
                donHang.setDiaChiGiaoHang(encryptionUtil.decrypt(donHang.getDiaChiGiaoHang()));
                
                // 2. Giải mã Địa chỉ lấy hàng (App Level)
                if (donHang.getDiaChiLayHang() != null) {
                    String soNhaDecrypted = encryptionUtil.decrypt(donHang.getDiaChiLayHang().getSoNhaDuong());
                    donHang.getDiaChiLayHang().setSoNhaDuong(soNhaDecrypted);
                }
                // SĐT và Quận Huyện được giải mã tự động bởi @Formula (DB Level)
            } catch (Exception e) {
                System.err.println("Lỗi giải mã PII DonHang " + donHang.getMaVanDon() + ": " + e.getMessage());
                donHang.setTenNguoiNhan("[Lỗi hiển thị]");
            }
        }
    }
    
    /**
     * Lấy Private Key RSA của user hiện tại (đã giải mã AES).
     */
    private String getMyPrivateKey(Authentication authentication) {
        TaiKhoan tk = userHelper.getTaiKhoanHienTai(authentication);
        if (tk == null || tk.getPrivateKey() == null) {
            throw new RuntimeException("Tài khoản chưa có Key RSA. Vui lòng liên hệ Admin.");
        }
        // Giải mã AES để lấy Private Key gốc
        return encryptionUtil.decrypt(tk.getPrivateKey());
    }

    /**
     * TẠO ĐƠN HÀNG MỚI
     * - Mã hóa AES cho Tên, Địa chỉ.
     * - Ký số RSA cho dữ liệu quan trọng.
     */
    @Transactional
    public DonHang taoDonHangMoi(DonHang donHang, Integer idDiaChiLayHang, Authentication authentication) {
        KhachHang khachHangGui = userHelper.getKhachHangHienTai(authentication);
        donHang.setKhachHangGui(khachHangGui);
        
        DiaChi diaChiLay = diaChiRepository.findById(idDiaChiLayHang)
                .orElseThrow(() -> new IllegalArgumentException("Địa chỉ không hợp lệ."));
        if(!diaChiLay.getKhachHangSoHuu().getId().equals(khachHangGui.getId())){
             throw new SecurityException("Truy cập trái phép.");
        }
        donHang.setDiaChiLayHang(diaChiLay);
        donHang.setMaVanDon("DH" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        donHang.setNgayTao(new Date());

        // 1. Chuẩn bị dữ liệu để Ký số (Trước khi mã hóa)
        String dataToSign = "CreateOrder|" + donHang.getMaVanDon() + "|" + donHang.getTenNguoiNhan() + "|" + 
                            (donHang.getThanhToan() != null ? donHang.getThanhToan().getTongTienCod() : "0");

        // 2. Mã hóa AES (Mức Ứng dụng)
        donHang.setTenNguoiNhan(encryptionUtil.encrypt(donHang.getTenNguoiNhan()));
        donHang.setDiaChiGiaoHang(encryptionUtil.encrypt(donHang.getDiaChiGiaoHang()));
        // SĐT để Trigger DB mã hóa

        if (donHang.getThanhToan() != null) {
            donHang.getThanhToan().setDonHang(donHang);
        } else {
            ThanhToan thanhToanMacDinh = new ThanhToan();
            thanhToanMacDinh.setDonHang(donHang);
            donHang.setThanhToan(thanhToanMacDinh);
        }

        // 3. Ký số RSA (Khách hàng ký xác nhận tạo đơn)
        try {
            String myPrivateKey = getMyPrivateKey(authentication);
            String signature = rsaUtil.sign(dataToSign, myPrivateKey);
            donHang.setChKyKhachHang(signature);
        } catch (Exception e) {
            System.err.println("Lỗi ký số Khách hàng: " + e.getMessage());
            // Không throw lỗi để quy trình vẫn chạy (nếu khóa chưa sẵn sàng)
        }

        DonHang savedDonHang = donHangRepository.save(donHang);

        // Tạo hành trình đầu
        TrangThaiDonHang trangThaiBanDau = trangThaiDonHangRepository.findById(1).orElseThrow();
        HanhTrinhDonHang hanhTrinhDauTien = new HanhTrinhDonHang();
        hanhTrinhDauTien.setDonHang(savedDonHang);
        hanhTrinhDauTien.setTrangThai(trangThaiBanDau);
        hanhTrinhDauTien.setThoiGianCapNhat(new Date());
        hanhTrinhDonHangRepository.save(hanhTrinhDauTien);

        TaiKhoan tk = userHelper.getTaiKhoanHienTai(authentication);
        if(tk != null) {
             nhatKyVanHanhService.logAction(tk, "Tạo đơn hàng", "DON_HANG", savedDonHang.getIdDonHang(), 
                     "Khách tạo đơn: " + savedDonHang.getMaVanDon());
        }
        
        return savedDonHang;
    }

    // === CÁC HÀM ĐỌC (READ) ===

    public List<DonHang> getDonHangCuaKhachHangHienTai(Authentication authentication) {
        KhachHang kh = userHelper.getKhachHangHienTai(authentication);
        List<DonHang> list = donHangRepository.findByKhachHangGui_IdOrderByIdDonHangDesc(kh.getId());
        list.forEach(this::decryptDonHangPII);
        return list;
    }
    
    public DonHang getChiTietDonHangCuaKhachHang(String maVanDon, Authentication authentication) {
        DonHang donHang = donHangRepository.findByMaVanDon(maVanDon).orElseThrow();
        KhachHang kh = userHelper.getKhachHangHienTai(authentication);
        if (!donHang.getKhachHangGui().getId().equals(kh.getId())) throw new SecurityException("Không có quyền.");
        decryptDonHangPII(donHang);
        return donHang;
    }
    
    public DonHang getDonHangByMaVanDon(String maVanDon) {
        DonHang donHang = donHangRepository.findByMaVanDon(maVanDon).orElseThrow();
        decryptDonHangPII(donHang);
        return donHang;
    }

    public List<DonHang> getAllDonHangForQuanLy() {
       List<DonHang> list = donHangRepository.findAllByOrderByIdDonHangDesc();
       list.forEach(this::decryptDonHangPII);
       return list;
    }
    
    public DonHang getChiTietDonHangChoQuanLy(Integer idDonHang) {
        DonHang donHang = donHangRepository.findById(idDonHang).orElseThrow();
        decryptDonHangPII(donHang);
        return donHang;
    }
    
    public List<DonHang> getDonHangCuaShipperHienTai(Authentication authentication) {
        NhanVien shipper = userHelper.getNhanVienHienTai(authentication);
        List<DonHang> list = donHangRepository.findDonHangDangXuLyCuaShipper(shipper.getId());
        list.forEach(this::decryptDonHangPII);
        return list;
    }

    // === CÁC HÀM NGHIỆP VỤ KHÁC ===
    
    @Transactional
    public void phanCongShipper(Integer idDonHang, Integer idShipper, Authentication authentication) {
        NhanVien quanLy = userHelper.getNhanVienHienTai(authentication);
        DonHang donHang = donHangRepository.findById(idDonHang).orElseThrow();
        NhanVien shipper = nhanVienRepository.findById(idShipper).orElseThrow();
        TrangThaiDonHang trangThaiMoi = (donHang.getTrangThaiHienTai().getIdTrangThai() == 7) 
                ? trangThaiDonHangRepository.findById(4).orElseThrow() 
                : donHang.getTrangThaiHienTai();

        HanhTrinhDonHang hanhTrinh = new HanhTrinhDonHang();
        hanhTrinh.setDonHang(donHang);
        hanhTrinh.setNhanVienThucHien(shipper);
        hanhTrinh.setTrangThai(trangThaiMoi);
        hanhTrinh.setGhiChuNhanVien("Quản lý phân công.");
        hanhTrinh.setThoiGianCapNhat(new Date());
        hanhTrinhDonHangRepository.save(hanhTrinh);
        
        nhatKyVanHanhService.logAction(quanLy.getTaiKhoan(), "Phân công đơn hàng", "DON_HANG", idDonHang, "Gán cho shipper: " + shipper.getMaNV());
    }

    @Transactional
    public void capNhatTrangThai(Integer idDonHang, Integer idTrangThaiMoi, String ghiChu, boolean daThanhToanCod, Authentication authentication) {
        NhanVien shipper = userHelper.getNhanVienHienTai(authentication);
        DonHang donHang = donHangRepository.findById(idDonHang).orElseThrow();
        TrangThaiDonHang trangThaiMoi = trangThaiDonHangRepository.findById(idTrangThaiMoi).orElseThrow();

        NhanVien shipperHienTai = donHang.getShipperHienTai();
        if (shipperHienTai == null || !shipperHienTai.getId().equals(shipper.getId())) {
             throw new SecurityException("Bạn không có quyền cập nhật đơn hàng này.");
        }

        if (idTrangThaiMoi == 5) {
            Optional<ThanhToan> ttOpt = thanhToanRepository.findByDonHang_IdDonHang(donHang.getIdDonHang());
            if (ttOpt.isPresent() && ttOpt.get().getTongTienCod() > 0) {
                ThanhToan tt = ttOpt.get();
                if (!daThanhToanCod) throw new IllegalStateException("Chưa xác nhận thu tiền COD.");
                tt.setDaThanhToanCod(true);
                thanhToanRepository.save(tt);
            }
        }
        
        HanhTrinhDonHang ht = new HanhTrinhDonHang();
        ht.setDonHang(donHang);
        ht.setNhanVienThucHien(shipper);
        ht.setTrangThai(trangThaiMoi);
        ht.setGhiChuNhanVien(ghiChu);
        ht.setThoiGianCapNhat(new Date());

        // --- KÝ SỐ RSA SHIPPER ---
        try {
            // Dữ liệu ký: ID Đơn + Trạng thái + ID Shipper + Ghi chú
            String dataToSign = "UpdateStatus|" + idDonHang + "|" + idTrangThaiMoi + "|" + shipper.getId() + "|" + (ghiChu != null ? ghiChu : "");
            String myPrivateKey = getMyPrivateKey(authentication);
            String signature = rsaUtil.sign(dataToSign, myPrivateKey);
            ht.setChKySo(signature);
        } catch (Exception e) {
            System.err.println("Lỗi ký số Shipper: " + e.getMessage());
        }
        // -------------------------

        hanhTrinhDonHangRepository.save(ht);
        
        if (idTrangThaiMoi == 6) {
             TrangThaiDonHang choXuLy = trangThaiDonHangRepository.findById(7).orElseThrow();
             HanhTrinhDonHang htAuto = new HanhTrinhDonHang();
             htAuto.setDonHang(donHang);
             htAuto.setTrangThai(choXuLy);
             htAuto.setGhiChuNhanVien("Hệ thống: Chuyển về chờ xử lý.");
             htAuto.setThoiGianCapNhat(new Date(System.currentTimeMillis() + 1000));
             hanhTrinhDonHangRepository.save(htAuto);
        }
        
        nhatKyVanHanhService.logAction(shipper.getTaiKhoan(), "Cập nhật trạng thái", "DON_HANG", idDonHang, "Trạng thái: " + trangThaiMoi.getTenTrangThai());
    }

    @Transactional
    public void hoanKhoDonHang(Integer idDonHang, Authentication authentication) {
        NhanVien quanLy = userHelper.getNhanVienHienTai(authentication);
        DonHang donHang = donHangRepository.findById(idDonHang).orElseThrow();
        
        if (donHang.getTrangThaiHienTai().getIdTrangThai() != 7) {
             throw new IllegalStateException("Chỉ có thể hoàn kho khi đơn hàng đang chờ xử lý lại.");
        }

        NhanVien lastShipper = null;
        if (donHang.getHanhTrinh() != null) {
            for (HanhTrinhDonHang ht : donHang.getHanhTrinh()) {
                if (ht.getTrangThai().getIdTrangThai() == 6 && ht.getNhanVienThucHien() != null) {
                    lastShipper = ht.getNhanVienThucHien();
                    break;
                }
            }
        }

        if (lastShipper == null) throw new IllegalStateException("Không tìm thấy shipper để hoàn kho.");

        TrangThaiDonHang ttHoanKho = trangThaiDonHangRepository.findById(8).orElseThrow();
        HanhTrinhDonHang ht = new HanhTrinhDonHang();
        ht.setDonHang(donHang);
        ht.setNhanVienThucHien(lastShipper);
        ht.setTrangThai(ttHoanKho);
        ht.setGhiChuNhanVien("Quản lý duyệt hoàn kho.");
        ht.setThoiGianCapNhat(new Date());
        hanhTrinhDonHangRepository.save(ht);
        
        nhatKyVanHanhService.logAction(quanLy.getTaiKhoan(), "Duyệt hoàn kho", "DON_HANG", idDonHang, "Gán shipper hoàn: " + lastShipper.getMaNV());
    }
}