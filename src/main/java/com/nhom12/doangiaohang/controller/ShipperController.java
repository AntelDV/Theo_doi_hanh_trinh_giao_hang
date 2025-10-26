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

    @GetMapping("/dashboard")
    public String dashboard() {
        return "shipper/dashboard";
    }

    @GetMapping("/don-hang")
    public String donHangCanXuLy(Authentication authentication, Model model) {
        List<DonHang> donHangList = donHangService.getDonHangCuaShipperHienTai(authentication);
        // Lấy các trạng thái mà shipper được phép cập nhật
        List<TrangThaiDonHang> trangThaiList = trangThaiDonHangRepository.findAllById(
                List.of(2, 4, 5, 6) // 2:Đã lấy, 4:Đang giao, 5:Thành công, 6:Thất bại
        );

        model.addAttribute("donHangList", donHangList);
        model.addAttribute("trangThaiList", trangThaiList); 
        return "shipper/don-hang";
    }

    @PostMapping("/don-hang/cap-nhat")
    public String capNhatTrangThai(@RequestParam("idDonHang") Integer idDonHang,
                                   @RequestParam("idTrangThaiMoi") Integer idTrangThaiMoi,
                                   @RequestParam(value = "ghiChu", required = false) String ghiChu,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        try {
            donHangService.capNhatTrangThai(idDonHang, idTrangThaiMoi, ghiChu, authentication);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái đơn hàng thành công!");
        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi cập nhật: " + e.getMessage());
        } catch (Exception e) {
             redirectAttributes.addFlashAttribute("errorMessage", "Đã có lỗi hệ thống xảy ra.");
        }
        return "redirect:/shipper/don-hang";
    }
}