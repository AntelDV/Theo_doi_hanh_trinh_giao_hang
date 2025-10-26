package com.nhom12.doangiaohang.service;

import com.nhom12.doangiaohang.model.*;
import com.nhom12.doangiaohang.repository.*;
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
    
    // LUỒNG 1: TẠO ĐƠN HÀNG
    @Transactional 
    public DonHang taoDonHangMoi(DonHang donHang, Integer idDiaChiLayHang, Authentication authentication) {
        
        // 1. Lấy thông tin người gửi (khách hàng đang đăng nhập)
        KhachHang khachHangGui = userHelper.getKhachHangHienTai(authentication);
        donHang.setKhachHangGui(khachHangGui);
        
        // 2. Lấy thông tin địa chỉ lấy hàng
        DiaChi diaChiLay = diaChiRepository.findById(idDiaChiLayHang)
                .orElseThrow(() -> new IllegalArgumentException("Địa chỉ lấy hàng không hợp lệ."));
        
        // 3. Kiểm tra bảo mật: Đảm bảo địa chỉ lấy hàng thuộc sở hữu của khách hàng
        if(!diaChiLay.getKhachHangSoHuu().getId().equals(khachHangGui.getId())){
             throw new SecurityException("Phát hiện truy cập trái phép: Địa chỉ lấy hàng không thuộc sở hữu của bạn.");
        }
        donHang.setDiaChiLayHang(diaChiLay);

        // 4. Thiết lập các thông tin mặc định
        donHang.setMaVanDon("DH" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        donHang.setNgayTao(new Date());

        // 5. Thiết lập liên kết hai chiều cho ThanhToan 
        if (donHang.getThanhToan() != null) {
            donHang.getThanhToan().setDonHang(donHang); 
        } else {
            ThanhToan thanhToanMacDinh = new ThanhToan();
            thanhToanMacDinh.setDonHang(donHang);
            donHang.setThanhToan(thanhToanMacDinh);
        }

        // 6. Lưu đơn hàng (CascadeType.ALL sẽ tự động lưu cả ThanhToan)
        DonHang savedDonHang = donHangRepository.save(donHang);

        // 7. Tạo trạng thái đầu tiên: "Chờ lấy hàng"
        TrangThaiDonHang trangThaiBanDau = trangThaiDonHangRepository.findById(1) // ID 1 = "Chờ lấy hàng"
                .orElseThrow(() -> new RuntimeException("Lỗi CSDL: Không tìm thấy trạng thái ID=1."));

        HanhTrinhDonHang hanhTrinhDauTien = new HanhTrinhDonHang();
        hanhTrinhDauTien.setDonHang(savedDonHang);
        hanhTrinhDauTien.setTrangThai(trangThaiBanDau);
        hanhTrinhDauTien.setThoiGianCapNhat(new Date());
        hanhTrinhDonHangRepository.save(hanhTrinhDauTien);

        return savedDonHang;
    }
    
    public List<DonHang> getDonHangCuaKhachHangHienTai(Authentication authentication) {
        KhachHang kh = userHelper.getKhachHangHienTai(authentication);
        return donHangRepository.findByKhachHangGui_IdOrderByIdDonHangDesc(kh.getId());
    }
    
    public DonHang getChiTietDonHangCuaKhachHang(String maVanDon, Authentication authentication) {
        DonHang donHang = donHangRepository.findByMaVanDon(maVanDon)
               .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng với mã: " + maVanDon));
        KhachHang kh = userHelper.getKhachHangHienTai(authentication);
        if (!donHang.getKhachHangGui().getId().equals(kh.getId())) {
             throw new SecurityException("Bạn không có quyền xem đơn hàng này.");
        }
        return donHang;
    }
    
    public DonHang getDonHangByMaVanDon(String maVanDon) {
        return donHangRepository.findByMaVanDon(maVanDon)
               .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng với mã: " + maVanDon));
    }

    public List<DonHang> getAllDonHangForQuanLy() {
        return donHangRepository.findAllByOrderByIdDonHangDesc();
    }

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
            trangThaiMoi = trangThaiDonHangRepository.findById(4) // ID 4 = "Đang giao hàng"
                    .orElseThrow(() -> new RuntimeException("Lỗi CSDL: Không tìm thấy trạng thái ID=4."));
        } else {
            trangThaiMoi = trangThaiHienTai; // Giữ nguyên trạng thái "Chờ lấy hàng" (1)
        }

        HanhTrinhDonHang hanhTrinhPhanCong = new HanhTrinhDonHang();
        hanhTrinhPhanCong.setDonHang(donHang);
        hanhTrinhPhanCong.setNhanVienThucHien(shipper); 
        hanhTrinhPhanCong.setTrangThai(trangThaiMoi); 
        hanhTrinhPhanCong.setGhiChuNhanVien("Quản lý [" + quanLy.getMaNV() + "] đã phân công cho shipper [" + shipper.getMaNV() + "]");
        hanhTrinhPhanCong.setThoiGianCapNhat(new Date());
        hanhTrinhDonHangRepository.save(hanhTrinhPhanCong);
    }

    @Transactional
    public void capNhatTrangThai(Integer idDonHang, Integer idTrangThaiMoi, String ghiChu, boolean daThanhToanCod, Authentication authentication) {
        NhanVien shipper = userHelper.getNhanVienHienTai(authentication); 
        
        DonHang donHang = donHangRepository.findById(idDonHang)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại."));
        TrangThaiDonHang trangThaiMoi = trangThaiDonHangRepository.findById(idTrangThaiMoi)
                 .orElseThrow(() -> new IllegalArgumentException("Trạng thái mới không hợp lệ."));

        NhanVien shipperHienTai = donHang.getShipperHienTai();
        if (shipperHienTai == null || !shipperHienTai.getId().equals(shipper.getId())) {
            throw new SecurityException("Bạn không được phân công cho đơn hàng này.");
        }

        int ttHienTaiId = donHang.getTrangThaiHienTai().getIdTrangThai();
        if (ttHienTaiId == 1 && idTrangThaiMoi != 2) { 
             throw new IllegalStateException("Thao tác không hợp lệ: Cần cập nhật thành 'Đã lấy hàng'.");
        }
        // (Thêm các quy tắc khác...)

        if (idTrangThaiMoi == 5) { // 5 = Giao thành công
            
            // Dùng repository để truy vấn trực tiếp
            Optional<ThanhToan> ttOpt = thanhToanRepository.findByDonHang_IdDonHang(donHang.getIdDonHang());
            
            if (ttOpt.isPresent() && ttOpt.get().getTongTienCod() > 0) {
                ThanhToan tt = ttOpt.get(); // Lấy đối tượng ThanhToan
                if (!daThanhToanCod) {
                    throw new IllegalStateException("Bạn phải xác nhận đã thu tiền COD để hoàn tất đơn hàng này.");
                }
                tt.setDaThanhToanCod(true);
                thanhToanRepository.save(tt); 
            }
        }
        
        if (idTrangThaiMoi == 6 && (ghiChu == null || ghiChu.trim().isEmpty())) { // 6 = Giao thất bại
            throw new IllegalStateException("Bạn phải nhập Ghi chú (lý do) khi báo giao hàng thất bại.");
        }

        HanhTrinhDonHang hanhTrinhMoi = new HanhTrinhDonHang();
        hanhTrinhMoi.setDonHang(donHang);
        hanhTrinhMoi.setNhanVienThucHien(shipper);
        hanhTrinhMoi.setTrangThai(trangThaiMoi);
        hanhTrinhMoi.setGhiChuNhanVien(ghiChu);
        hanhTrinhMoi.setThoiGianCapNhat(new Date());
        hanhTrinhDonHangRepository.save(hanhTrinhMoi);
        
        if (idTrangThaiMoi == 6) {
            TrangThaiDonHang trangThaiChoXuLy = trangThaiDonHangRepository.findById(7) // ID 7
                    .orElseThrow(() -> new RuntimeException("Lỗi CSDL: Không tìm thấy trạng thái ID=7."));
            
            HanhTrinhDonHang hanhTrinhChoXuLy = new HanhTrinhDonHang();
            hanhTrinhChoXuLy.setDonHang(donHang);
            hanhTrinhChoXuLy.setNhanVienThucHien(null); // Quay về cho Quản lý
            hanhTrinhChoXuLy.setTrangThai(trangThaiChoXuLy);
            hanhTrinhChoXuLy.setGhiChuNhanVien("Hệ thống tự động chuyển về chờ xử lý.");
            hanhTrinhChoXuLy.setThoiGianCapNhat(new Date(System.currentTimeMillis() + 1000)); // +1 giây
            hanhTrinhDonHangRepository.save(hanhTrinhChoXuLy);
        }
    }
    
    @Transactional
    public void hoanKhoDonHang(Integer idDonHang, Authentication authentication) {
        NhanVien quanLy = userHelper.getNhanVienHienTai(authentication);
        DonHang donHang = donHangRepository.findById(idDonHang)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại."));
        
        TrangThaiDonHang ttHienTai = donHang.getTrangThaiHienTai();
        if (ttHienTai == null || ttHienTai.getIdTrangThai() != 7) { // 7 = Chờ xử lý lại
             throw new IllegalStateException("Chỉ có thể hoàn kho đơn hàng ở trạng thái 'Chờ xử lý lại'.");
        }

        TrangThaiDonHang ttHoanKho = trangThaiDonHangRepository.findById(8) // 8 = Đang hoàn kho
                 .orElseThrow(() -> new RuntimeException("Lỗi CSDL: Không tìm thấy trạng thái ID=8."));

        NhanVien shipperDaGiao = donHang.getShipperHienTai();
        if (shipperDaGiao == null) {
            throw new IllegalStateException("Không tìm thấy shipper đã giao đơn này để thực hiện hoàn kho.");
        }

        HanhTrinhDonHang hanhTrinhMoi = new HanhTrinhDonHang();
        hanhTrinhMoi.setDonHang(donHang);
        hanhTrinhMoi.setNhanVienThucHien(shipperDaGiao); 
        hanhTrinhMoi.setTrangThai(ttHoanKho);
        hanhTrinhMoi.setGhiChuNhanVien("Quản lý [" + quanLy.getMaNV() + "] đã duyệt hoàn kho.");
        hanhTrinhMoi.setThoiGianCapNhat(new Date());
        hanhTrinhDonHangRepository.save(hanhTrinhMoi);
    }

    public List<DonHang> getDonHangCuaShipperHienTai(Authentication authentication) {
        NhanVien shipper = userHelper.getNhanVienHienTai(authentication);
        return donHangRepository.findDonHangDangXuLyCuaShipper(shipper.getId());
    }
}