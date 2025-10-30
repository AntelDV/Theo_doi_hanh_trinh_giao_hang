package com.nhom12.doangiaohang.service;

import com.nhom12.doangiaohang.model.*;
import com.nhom12.doangiaohang.repository.*;
// import com.nhom12.doangiaohang.utils.EncryptionUtil; // Gỡ bỏ import
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// import java.util.Base64; // Gỡ bỏ import
import java.util.Date;
import java.util.List;
import java.util.Optional; 
import java.util.UUID;
import java.util.stream.Collectors; 

@Service
public class DonHangService {

    @Autowired private DonHangRepository donHangRepository;
    @Autowired private HanhTrinhDonHangRepository hanhTrinhDonHangRepository;
    @Autowired private TrangThaiDonHangRepository trangThaiDonHangRepository;
    @Autowired private NhanVienRepository nhanVienRepository;
    @Autowired private DiaChiRepository diaChiRepository;
    @Autowired private ThanhToanRepository thanhToanRepository; 
    @Autowired private CustomUserHelper userHelper; 
    // @Autowired private EncryptionUtil encryptionUtil; // Gỡ bỏ Autowired
    @Autowired private TaiKhoanService taiKhoanService; 
    @Autowired private NhatKyVanHanhService nhatKyVanHanhService; 

    /**
     * Tạo đơn hàng mới (lưu plaintext).
     */
    @Transactional 
    public DonHang taoDonHangMoi(DonHang donHang, Integer idDiaChiLayHang, Authentication authentication) {
        
        KhachHang khachHangGui = userHelper.getKhachHangHienTai(authentication); 
        donHang.setKhachHangGui(khachHangGui);
        
        DiaChi diaChiLay = diaChiRepository.findById(idDiaChiLayHang)
                .orElseThrow(() -> new IllegalArgumentException("Địa chỉ lấy hàng không hợp lệ."));
        
        if(!diaChiLay.getKhachHangSoHuu().getId().equals(khachHangGui.getId())){
             throw new SecurityException("Phát hiện truy cập trái phép: Địa chỉ lấy hàng không thuộc sở hữu của bạn.");
        }
        donHang.setDiaChiLayHang(diaChiLay);
        
        donHang.setMaVanDon("DH" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        donHang.setNgayTao(new Date());

        if (donHang.getThanhToan() != null) {
            donHang.getThanhToan().setDonHang(donHang); 
        } else {
            ThanhToan thanhToanMacDinh = new ThanhToan();
            thanhToanMacDinh.setDonHang(donHang);
            donHang.setThanhToan(thanhToanMacDinh);
        }

        DonHang savedDonHang = donHangRepository.save(donHang);

        TrangThaiDonHang trangThaiBanDau = trangThaiDonHangRepository.findById(1) 
                .orElseThrow(() -> new RuntimeException("Lỗi CSDL: Không tìm thấy trạng thái ID=1."));

        HanhTrinhDonHang hanhTrinhDauTien = new HanhTrinhDonHang();
        hanhTrinhDauTien.setDonHang(savedDonHang);
        hanhTrinhDauTien.setTrangThai(trangThaiBanDau);
        hanhTrinhDauTien.setThoiGianCapNhat(new Date());
        hanhTrinhDonHangRepository.save(hanhTrinhDauTien);

        TaiKhoan tk = userHelper.getTaiKhoanHienTai(authentication);
        if(tk != null) {
             nhatKyVanHanhService.logAction(tk, "Tạo đơn hàng", "DON_HANG", savedDonHang.getIdDonHang(), "Khách hàng tạo đơn hàng mới: " + savedDonHang.getMaVanDon());
        }

        return savedDonHang;
    }
    
    /**
     * Lấy danh sách đơn hàng của khách hàng (plaintext).
     */
    public List<DonHang> getDonHangCuaKhachHangHienTai(Authentication authentication) {
        KhachHang kh = userHelper.getKhachHangHienTai(authentication); 
        List<DonHang> list = donHangRepository.findByKhachHangGui_IdOrderByIdDonHangDesc(kh.getId());
        return list; 
    }
    
    /**
     * Lấy chi tiết đơn hàng cho Khách hàng (plaintext).
     */
    public DonHang getChiTietDonHangCuaKhachHang(String maVanDon, Authentication authentication) {
        DonHang donHang = donHangRepository.findByMaVanDon(maVanDon)
               .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng với mã: " + maVanDon));
        KhachHang kh = userHelper.getKhachHangHienTai(authentication); 
        if (!donHang.getKhachHangGui().getId().equals(kh.getId())) {
             throw new SecurityException("Bạn không có quyền xem đơn hàng này.");
        }
        return donHang; 
    }
    
    /**
     * Lấy chi tiết đơn hàng cho tra cứu công khai (plaintext).
     */
    public DonHang getDonHangByMaVanDon(String maVanDon) {
        DonHang donHang = donHangRepository.findByMaVanDon(maVanDon)
               .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng với mã: " + maVanDon));
        return donHang; 
    }

    /**
     * Lấy tất cả đơn hàng cho Quản lý (plaintext).
     */
    public List<DonHang> getAllDonHangForQuanLy() {
       List<DonHang> list = donHangRepository.findAllByOrderByIdDonHangDesc();
       return list;
    }
    
    /**
     * Lấy đơn hàng cần xử lý của Shipper (plaintext).
     */
    public List<DonHang> getDonHangCuaShipperHienTai(Authentication authentication) {
        NhanVien shipper = userHelper.getNhanVienHienTai(authentication); 
        List<DonHang> list = donHangRepository.findDonHangDangXuLyCuaShipper(shipper.getId());
        return list;
    }
    
    /**
     * Phân công shipper cho đơn hàng, ghi log.
     */
    @Transactional
    public void phanCongShipper(Integer idDonHang, Integer idShipper, Authentication authentication) {
        NhanVien quanLy = userHelper.getNhanVienHienTai(authentication); 
        
        DonHang donHang = donHangRepository.findById(idDonHang)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại."));
        NhanVien shipper = nhanVienRepository.findById(idShipper)
                 .orElseThrow(() -> new IllegalArgumentException("Shipper không tồn tại hoặc không hợp lệ."));
        
        if (shipper.getTaiKhoan().getVaiTro().getIdVaiTro() != 2) { 
             throw new IllegalArgumentException("Nhân viên được chọn không phải là Shipper.");
        }

        TrangThaiDonHang trangThaiHienTai = donHang.getTrangThaiHienTai();
        if (trangThaiHienTai == null || (trangThaiHienTai.getIdTrangThai() != 1 && trangThaiHienTai.getIdTrangThai() != 7)) { 
             throw new IllegalStateException("Chỉ có thể phân công đơn hàng đang ở trạng thái 'Chờ lấy hàng' hoặc 'Chờ xử lý lại'.");
        }

        TrangThaiDonHang trangThaiMoi;
        if (trangThaiHienTai.getIdTrangThai() == 7) {
            trangThaiMoi = trangThaiDonHangRepository.findById(4) 
                    .orElseThrow(() -> new RuntimeException("Lỗi CSDL: Không tìm thấy trạng thái ID=4."));
        } else {
            trangThaiMoi = trangThaiHienTai; 
        }

        HanhTrinhDonHang hanhTrinhPhanCong = new HanhTrinhDonHang();
        hanhTrinhPhanCong.setDonHang(donHang);
        hanhTrinhPhanCong.setNhanVienThucHien(shipper); 
        hanhTrinhPhanCong.setTrangThai(trangThaiMoi); 
        String ghiChuLog = "Quản lý [" + quanLy.getMaNV() + "] đã phân công cho shipper [" + shipper.getMaNV() + "]";
        hanhTrinhPhanCong.setGhiChuNhanVien(ghiChuLog);
        hanhTrinhPhanCong.setThoiGianCapNhat(new Date());
        hanhTrinhDonHangRepository.save(hanhTrinhPhanCong);

        TaiKhoan tkQuanLy = userHelper.getTaiKhoanHienTai(authentication);
        if(tkQuanLy != null) {
             nhatKyVanHanhService.logAction(tkQuanLy, "Phân công đơn hàng", "DON_HANG", idDonHang, ghiChuLog);
        }
    }

    /**
     * Shipper cập nhật trạng thái đơn hàng, xử lý COD, ghi log.
     */
    @Transactional
    public void capNhatTrangThai(Integer idDonHang, Integer idTrangThaiMoi, String ghiChu, boolean daThanhToanCod, Authentication authentication) {
        NhanVien shipper = userHelper.getNhanVienHienTai(authentication); 
        
        DonHang donHang = donHangRepository.findById(idDonHang)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại."));
        TrangThaiDonHang trangThaiMoi = trangThaiDonHangRepository.findById(idTrangThaiMoi)
                 .orElseThrow(() -> new IllegalArgumentException("Trạng thái mới không hợp lệ."));

        NhanVien shipperHienTaiCuaDon = findShipperHienTaiCuaDon(donHang); 
        if (shipperHienTaiCuaDon == null || !shipperHienTaiCuaDon.getId().equals(shipper.getId())) {
             throw new SecurityException("Bạn không được phân công cho đơn hàng này hoặc đơn hàng đã chuyển trạng thái.");
        }

        int ttHienTaiId = donHang.getTrangThaiHienTai().getIdTrangThai();
        if (!isTransitionAllowed(ttHienTaiId, idTrangThaiMoi)) {
             throw new IllegalStateException("Thao tác cập nhật trạng thái không hợp lệ.");
        }

        if (idTrangThaiMoi == 5) { // Giao thành công
            Optional<ThanhToan> ttOpt = thanhToanRepository.findByDonHang_IdDonHang(donHang.getIdDonHang());
            if (ttOpt.isPresent() && ttOpt.get().getTongTienCod() > 0) {
                ThanhToan tt = ttOpt.get(); 
                if (!daThanhToanCod) {
                    throw new IllegalStateException("Bạn phải xác nhận đã thu tiền COD để hoàn tất đơn hàng này.");
                }
                tt.setDaThanhToanCod(true);
                thanhToanRepository.save(tt); 
            }
        }
        
        if (idTrangThaiMoi == 6 && (ghiChu == null || ghiChu.trim().isEmpty())) { // Giao thất bại
            throw new IllegalStateException("Bạn phải nhập Ghi chú (lý do) khi báo giao hàng thất bại.");
        }

        HanhTrinhDonHang hanhTrinhMoi = new HanhTrinhDonHang();
        hanhTrinhMoi.setDonHang(donHang);
        hanhTrinhMoi.setNhanVienThucHien(shipper);
        hanhTrinhMoi.setTrangThai(trangThaiMoi);
        hanhTrinhMoi.setGhiChuNhanVien(ghiChu);
        hanhTrinhMoi.setThoiGianCapNhat(new Date());
        hanhTrinhDonHangRepository.save(hanhTrinhMoi);
        
        String ghiChuLog = "Shipper [" + shipper.getMaNV() + "] cập nhật trạng thái thành: " + trangThaiMoi.getTenTrangThai();
        if(ghiChu != null && !ghiChu.isEmpty()) ghiChuLog += ". Ghi chú: " + ghiChu;
        
        if (idTrangThaiMoi == 6) { 
            TrangThaiDonHang trangThaiChoXuLy = trangThaiDonHangRepository.findById(7) 
                    .orElseThrow(() -> new RuntimeException("Lỗi CSDL: Không tìm thấy trạng thái ID=7."));
            
            HanhTrinhDonHang hanhTrinhChoXuLy = new HanhTrinhDonHang();
            hanhTrinhChoXuLy.setDonHang(donHang);
            hanhTrinhChoXuLy.setNhanVienThucHien(null); 
            hanhTrinhChoXuLy.setTrangThai(trangThaiChoXuLy);
            hanhTrinhChoXuLy.setGhiChuNhanVien("Hệ thống tự động chuyển về chờ xử lý.");
            hanhTrinhChoXuLy.setThoiGianCapNhat(new Date(System.currentTimeMillis() + 1000)); 
            hanhTrinhDonHangRepository.save(hanhTrinhChoXuLy);
            ghiChuLog += ". Hệ thống tự động chuyển về chờ xử lý.";
        }
        
         TaiKhoan tkShipper = userHelper.getTaiKhoanHienTai(authentication);
         if(tkShipper != null) {
              nhatKyVanHanhService.logAction(tkShipper, "Cập nhật trạng thái", "DON_HANG", idDonHang, ghiChuLog);
         }
    }
    
    /**
     * Tìm shipper hiện tại đang được gán xử lý đơn hàng.
     */
    private NhanVien findShipperHienTaiCuaDon(DonHang donHang) {
        if (donHang.getHanhTrinh() == null || donHang.getHanhTrinh().isEmpty()) {
            return null;
        }
        HanhTrinhDonHang htMoiNhat = donHang.getHanhTrinh().get(0);
        if (htMoiNhat.getNhanVienThucHien() != null) {
             int currentStatusId = htMoiNhat.getTrangThai().getIdTrangThai();
             if (List.of(1, 2, 4, 6, 8).contains(currentStatusId)) { 
                 return htMoiNhat.getNhanVienThucHien();
             }
        }
        return null; 
    }

    /**
     * Kiểm tra logic chuyển trạng thái của Shipper.
     */
    private boolean isTransitionAllowed(int currentStatusId, int nextStatusId) {
        switch (currentStatusId) {
            case 1: return nextStatusId == 2; 
            case 2: return nextStatusId == 4; 
            case 4: return nextStatusId == 5 || nextStatusId == 6; 
            case 8: return nextStatusId == 9; 
            default: return false; 
        }
    }

    /**
     * Quản lý duyệt hoàn kho đơn hàng, ghi log.
     */
    @Transactional
    public void hoanKhoDonHang(Integer idDonHang, Authentication authentication) {
        NhanVien quanLy = userHelper.getNhanVienHienTai(authentication); 
        DonHang donHang = donHangRepository.findById(idDonHang)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại."));
        
        TrangThaiDonHang ttHienTai = donHang.getTrangThaiHienTai();
        if (ttHienTai == null || ttHienTai.getIdTrangThai() != 7) { 
             throw new IllegalStateException("Chỉ có thể hoàn kho đơn hàng ở trạng thái 'Chờ xử lý lại'.");
        }

        TrangThaiDonHang ttHoanKho = trangThaiDonHangRepository.findById(8) 
                 .orElseThrow(() -> new RuntimeException("Lỗi CSDL: Không tìm thấy trạng thái ID=8."));

        NhanVien shipperDaGiao = findLastFailedShipper(donHang);
        if (shipperDaGiao == null) {
            throw new IllegalStateException("Không tìm thấy shipper đã giao đơn này để thực hiện hoàn kho.");
        }

        HanhTrinhDonHang hanhTrinhMoi = new HanhTrinhDonHang();
        hanhTrinhMoi.setDonHang(donHang);
        hanhTrinhMoi.setNhanVienThucHien(shipperDaGiao); 
        hanhTrinhMoi.setTrangThai(ttHoanKho);
        String ghiChuLog = "Quản lý [" + quanLy.getMaNV() + "] đã duyệt hoàn kho, gán cho shipper [" + shipperDaGiao.getMaNV() + "]";
        hanhTrinhMoi.setGhiChuNhanVien(ghiChuLog);
        hanhTrinhMoi.setThoiGianCapNhat(new Date());
        hanhTrinhDonHangRepository.save(hanhTrinhMoi);
        
        TaiKhoan tkQuanLy = userHelper.getTaiKhoanHienTai(authentication);
         if(tkQuanLy != null) {
             nhatKyVanHanhService.logAction(tkQuanLy, "Duyệt hoàn kho", "DON_HANG", idDonHang, ghiChuLog);
         }
    }
    
    /**
     * Tìm shipper gần nhất đã thực hiện hành động Giao thất bại trên đơn hàng.
     */
    private NhanVien findLastFailedShipper(DonHang donHang) {
         if (donHang.getHanhTrinh() == null || donHang.getHanhTrinh().isEmpty()) {
            return null;
        }
         for (HanhTrinhDonHang ht : donHang.getHanhTrinh()) {
             if (ht.getTrangThai().getIdTrangThai() == 6 && ht.getNhanVienThucHien() != null) {
                 return ht.getNhanVienThucHien();
             }
         }
         return null; 
    }
}