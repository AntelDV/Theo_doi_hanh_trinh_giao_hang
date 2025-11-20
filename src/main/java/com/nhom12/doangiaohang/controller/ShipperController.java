package com.nhom12.doangiaohang.controller;

import com.nhom12.doangiaohang.model.DonHang;
import com.nhom12.doangiaohang.model.TrangThaiDonHang;
import com.nhom12.doangiaohang.repository.TrangThaiDonHangRepository;
import com.nhom12.doangiaohang.service.DonHangService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/shipper")
public class ShipperController {

    @Autowired private DonHangService donHangService;
    @Autowired private TrangThaiDonHangRepository trangThaiDonHangRepository; 

    /**
     * Hiển thị trang chủ (dashboard) của Shipper.
     */
    @GetMapping("/dashboard")
    public String dashboard() {
        return "shipper/dashboard";
    }

    /**
     * Hiển thị danh sách đơn hàng được gán cho Shipper hiện tại.
     */
    @GetMapping("/don-hang")
    public String donHangCanXuLy(Authentication authentication, Model model) {
        List<DonHang> donHangList = donHangService.getDonHangCuaShipperHienTai(authentication);
        
        // SỬA LỖI LOGIC: Thêm trạng thái 9 (Đã hoàn kho)
        // Shipper cần quyền này để hoàn tất quy trình "Hoàn kho" (từ trạng thái 8 -> 9)
        List<TrangThaiDonHang> trangThaiList = trangThaiDonHangRepository.findAllById(
                List.of(2, 4, 5, 6, 9) // 2:Đã lấy, 4:Đang giao, 5:Thành công, 6:Thất bại, 9:Đã hoàn kho
        );

        model.addAttribute("donHangList", donHangList);
        model.addAttribute("trangThaiList", trangThaiList); 
        return "shipper/don-hang";
    }

    /**
     * Xử lý việc Shipper cập nhật trạng thái đơn hàng (lấy hàng, giao hàng, thất bại, hoàn kho).
     */
    @PostMapping("/don-hang/cap-nhat")
    public String capNhatTrangThai(@RequestParam("idDonHang") Integer idDonHang,
                                   @RequestParam("idTrangThaiMoi") Integer idTrangThaiMoi,
                                   @RequestParam(value = "ghiChu", required = false) String ghiChu,
                                   @RequestParam(value = "daThanhToanCod", defaultValue = "false") boolean daThanhToanCod,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        try {
            donHangService.capNhatTrangThai(idDonHang, idTrangThaiMoi, ghiChu, daThanhToanCod, authentication);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái đơn hàng thành công!");
        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi cập nhật: " + e.getMessage());
        } catch (Exception e) {
             redirectAttributes.addFlashAttribute("errorMessage", "Đã có lỗi hệ thống xảy ra.");
             e.printStackTrace(); // Giúp debug
        }
        return "redirect:/shipper/don-hang";
    }
    
    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        List<DonHang> list = donHangService.getDonHangCuaShipperHienTai(authentication);
        
        long canLay = list.stream().filter(d -> d.getTrangThaiHienTai().getIdTrangThai() == 1 || d.getTrangThaiHienTai().getIdTrangThai() == 2).count();
        long canGiao = list.stream().filter(d -> d.getTrangThaiHienTai().getIdTrangThai() == 4).count();
        // Tính tổng COD của các đơn đang giữ (Đang giao)
        double codDangGiu = list.stream()
                .filter(d -> d.getTrangThaiHienTai().getIdTrangThai() == 4 && d.getThanhToan() != null && !d.getThanhToan().isDaThanhToanCod())
                .mapToDouble(d -> d.getThanhToan().getTongTienCod())
                .sum();
        
        model.addAttribute("countCanLay", canLay);
        model.addAttribute("countCanGiao", canGiao);
        model.addAttribute("codDangGiu", codDangGiu);
        
        // List 5 đơn cần xử lý gấp
        model.addAttribute("donHangGap", list.size() > 5 ? list.subList(0, 5) : list);

        return "shipper/dashboard";
    }
}