package com.nhom12.doangiaohang.service;

import com.nhom12.doangiaohang.model.*;
import com.nhom12.doangiaohang.repository.*;
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
    @Autowired private CustomUserHelper userHelper; // Dùng file tiện ích
    
    // (Các Service/Repository khác)
    // @Autowired private NhatKyVanHanhService nhatKyService; 

    // LUỒNG 1: KHÁCH HÀNG TẠO ĐƠN
    @Transactional // Đảm bảo tất cả cùng thành công hoặc thất bại
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

        // 5. Thiết lập liên kết hai chiều cho ThanhToan (Rất quan trọng)
        // Dữ liệu ThanhToan (COD) đã được gán vào donHang từ form
        if (donHang.getThanhToan() != null) {
            donHang.getThanhToan().setDonHang(donHang); 
        } else {
            // Nếu không có thông tin thanh toán, tạo một bản ghi mặc định
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
        // ID_NHAN_VIEN_THUC_HIEN là NULL vì đây là do khách tạo
        hanhTrinhDonHangRepository.save(hanhTrinhDauTien);

        return savedDonHang;
    }
    
    // (Các phương thức khác giữ nguyên như cũ)
    
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
        if (trangThaiHienTai == null || trangThaiHienTai.getIdTrangThai() != 1) { 
             throw new IllegalStateException("Chỉ có thể phân công đơn hàng đang ở trạng thái 'Chờ lấy hàng'.");
        }

        HanhTrinhDonHang hanhTrinhPhanCong = new HanhTrinhDonHang();
        hanhTrinhPhanCong.setDonHang(donHang);
        hanhTrinhPhanCong.setNhanVienThucHien(shipper); 
        hanhTrinhPhanCong.setTrangThai(trangThaiHienTai); 
        hanhTrinhPhanCong.setGhiChuNhanVien("Quản lý [" + quanLy.getMaNV() + "] đã phân công cho shipper [" + shipper.getMaNV() + "]");
        hanhTrinhPhanCong.setThoiGianCapNhat(new Date());
        hanhTrinhDonHangRepository.save(hanhTrinhPhanCong);

        // nhatKyService.logAction(quanLy.getTaiKhoan(), "Phân công đơn hàng", ...);
    }

    @Transactional
    public void capNhatTrangThai(Integer idDonHang, Integer idTrangThaiMoi, String ghiChu, Authentication authentication) {
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
        // Thêm các quy tắc khác...

        HanhTrinhDonHang hanhTrinhMoi = new HanhTrinhDonHang();
        hanhTrinhMoi.setDonHang(donHang);
        hanhTrinhMoi.setNhanVienThucHien(shipper);
        hanhTrinhMoi.setTrangThai(trangThaiMoi);
        hanhTrinhMoi.setGhiChuNhanVien(ghiChu);
        hanhTrinhMoi.setThoiGianCapNhat(new Date());

        hanhTrinhDonHangRepository.save(hanhTrinhMoi);
        
        // nhatKyService.logAction(shipper.getTaiKhoan(), "Cập nhật trạng thái đơn hàng", ...);
    }

    public List<DonHang> getDonHangCuaShipperHienTai(Authentication authentication) {
        NhanVien shipper = userHelper.getNhanVienHienTai(authentication);
        return donHangRepository.findDonHangDangXuLyCuaShipper(shipper.getId());
    }
}