package com.nhom12.doangiaohang.controller;

import com.nhom12.doangiaohang.model.DonHang;
import com.nhom12.doangiaohang.model.NhanVien;
import com.nhom12.doangiaohang.service.DonHangService;
import com.nhom12.doangiaohang.service.NhanVienService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/quan-ly")
public class QuanLyController {

    @Autowired private DonHangService donHangService;
    @Autowired private NhanVienService nhanVienService; // Dùng Service

    @GetMapping("/dashboard")
    public String dashboard() {
        return "quan-ly/dashboard";
    }

    @GetMapping("/don-hang")
    public String quanLyDonHang(Model model) {
        List<DonHang> allDonHang = donHangService.getAllDonHangForQuanLy();
        List<NhanVien> shippers = nhanVienService.getAllShippers(); // Lấy ds shipper từ Service

        model.addAttribute("donHangList", allDonHang);
        model.addAttribute("shipperList", shippers);
        return "quan-ly/don-hang";
    }

    @PostMapping("/don-hang/phan-cong")
    public String phanCongShipper(@RequestParam("idDonHang") Integer idDonHang,
                                  @RequestParam("idShipper") Integer idShipper,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        try {
            donHangService.phanCongShipper(idDonHang, idShipper, authentication);
            // Thông báo thành công kiểu "popup"
            redirectAttributes.addFlashAttribute("successMessage", "Phân công shipper thành công!");
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Thông báo lỗi
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi phân công: " + e.getMessage());
        } catch (Exception e) {
             redirectAttributes.addFlashAttribute("errorMessage", "Đã có lỗi hệ thống xảy ra. Vui lòng thử lại.");
        }
        return "redirect:/quan-ly/don-hang";
    }

    @GetMapping("/nhan-vien")
    public String quanLyNhanVien() {
        return "quan-ly/nhan-vien";
    }
    @GetMapping("/nhat-ky")
    public String quanLyNhatKy() {
        return "quan-ly/nhat-ky";
    }
}