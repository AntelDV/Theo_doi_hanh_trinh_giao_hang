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
import jakarta.persistence.EntityManager;
import java.util.List;

@Controller
@RequestMapping("/shipper")
public class ShipperController {

    @Autowired private DonHangService donHangService;
    @Autowired private TrangThaiDonHangRepository trangThaiDonHangRepository; 
    @Autowired private EntityManager entityManager;
    
    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
    	entityManager.clear();
        List<DonHang> list = donHangService.getDonHangCuaShipperHienTai(authentication);
        
        long canLay = list.stream().filter(d -> d.getTrangThaiHienTai().getIdTrangThai() == 1 || d.getTrangThaiHienTai().getIdTrangThai() == 2).count();
        long canGiao = list.stream().filter(d -> d.getTrangThaiHienTai().getIdTrangThai() == 4).count();
        double codDangGiu = list.stream()
                .filter(d -> d.getTrangThaiHienTai().getIdTrangThai() == 4 && d.getThanhToan() != null && !d.getThanhToan().isDaThanhToanCod())
                .mapToDouble(d -> d.getThanhToan().getTongTienCod())
                .sum();
        
        model.addAttribute("countCanLay", canLay);
        model.addAttribute("countCanGiao", canGiao);
        model.addAttribute("codDangGiu", codDangGiu);
        model.addAttribute("donHangGap", list.size() > 5 ? list.subList(0, 5) : list);

        return "shipper/dashboard";
    }

    @GetMapping("/don-hang")
    public String donHangCanXuLy(Authentication authentication, Model model) {
    	
    	entityManager.clear();
        List<DonHang> donHangList = donHangService.getDonHangCuaShipperHienTai(authentication);
        
        // Danh sách trạng thái Shipper được chọn:
        // 2: Đã lấy, 4: Đang giao, 5: Thành công, 6: Thất bại, 9: Đã hoàn kho
        List<TrangThaiDonHang> trangThaiList = trangThaiDonHangRepository.findAllById(
                List.of(2, 4, 5, 6, 9) 
        );

        model.addAttribute("donHangList", donHangList);
        model.addAttribute("trangThaiList", trangThaiList); 
        return "shipper/don-hang";
    }

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
             redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống: " + e.getMessage());
             e.printStackTrace(); 
        }
        return "redirect:/shipper/don-hang";
    }
}