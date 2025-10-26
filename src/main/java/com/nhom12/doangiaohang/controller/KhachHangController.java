package com.nhom12.doangiaohang.controller;

import com.nhom12.doangiaohang.model.DiaChi;
import com.nhom12.doangiaohang.model.DonHang;
import com.nhom12.doangiaohang.model.KhachHang;
import com.nhom12.doangiaohang.model.ThanhToan;
import com.nhom12.doangiaohang.service.DiaChiService;
import com.nhom12.doangiaohang.service.DonHangService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/khach-hang")
public class KhachHangController {

    @Autowired private DonHangService donHangService;
    @Autowired private DiaChiService diaChiService;

    @GetMapping("/dashboard")
    public String dashboard() {
        return "khach-hang/dashboard";
    }

    @GetMapping("/tao-don-hang")
    public String taoDonHangForm(Authentication authentication, Model model) {
        try {
            List<DiaChi> diaChiList = diaChiService.getDiaChiByCurrentUser(authentication);
            DonHang donHang = new DonHang();
            donHang.setThanhToan(new ThanhToan()); 
            model.addAttribute("donHang", donHang);
            model.addAttribute("diaChiList", diaChiList);
            return "khach-hang/tao-don-hang";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Không thể tải thông tin khách hàng.");
            return "khach-hang/dashboard";
        }
    }

    @PostMapping("/tao-don-hang")
    public String processTaoDonHang(@ModelAttribute("donHang") DonHang donHang, 
                                   @RequestParam("idDiaChiLayHang") Integer idDiaChiLayHang,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes,
                                   Model model) {
        try {
            donHang.getThanhToan().setDonHang(donHang); 
            DonHang donHangMoi = donHangService.taoDonHangMoi(donHang, idDiaChiLayHang, authentication);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo đơn hàng thành công! Mã vận đơn: " + donHangMoi.getMaVanDon());
            return "redirect:/khach-hang/danh-sach-don-hang";
        } catch (SecurityException e) { 
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi bảo mật: " + e.getMessage());
            return "redirect:/khach-hang/dashboard";
        } catch (IllegalArgumentException e) { 
            model.addAttribute("errorMessage", "Lỗi: " + e.getMessage());
            List<DiaChi> diaChiList = diaChiService.getDiaChiByCurrentUser(authentication);
            model.addAttribute("diaChiList", diaChiList);
            return "khach-hang/tao-don-hang";
        }
    }

    @GetMapping("/danh-sach-don-hang")
    public String danhSachDonHang(Authentication authentication, Model model) {
        List<DonHang> donHangList = donHangService.getDonHangCuaKhachHangHienTai(authentication);
        model.addAttribute("donHangList", donHangList);
        return "khach-hang/danh-sach-don-hang";
    }

    @GetMapping("/chi-tiet-don-hang/{maVanDon}")
    public String chiTietDonHang(@PathVariable("maVanDon") String maVanDon, Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
         try {
            DonHang donHang = donHangService.getChiTietDonHangCuaKhachHang(maVanDon, authentication);
            model.addAttribute("donHang", donHang);
            return "khach-hang/chi-tiet-don-hang";
         } catch(IllegalArgumentException | SecurityException e) { 
             redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
             return "redirect:/khach-hang/danh-sach-don-hang";
         }
    }
    
    // === CÁC HÀM SỬA LỖI CHO SỔ ĐỊA CHỈ ===
    
    @GetMapping("/so-dia-chi")
    public String soDiaChi(Authentication authentication, Model model) {
         List<DiaChi> diaChiList = diaChiService.getDiaChiByCurrentUser(authentication);
         model.addAttribute("diaChiList", diaChiList);
         
         // Chuẩn bị form để thêm địa chỉ mới
         if (!model.containsAttribute("diaChiMoi")) {
             model.addAttribute("diaChiMoi", new DiaChi());
         }
         
        return "khach-hang/so-dia-chi";
    }
    
    @PostMapping("/so-dia-chi/them")
    public String processThemDiaChi(@Valid @ModelAttribute("diaChiMoi") DiaChi diaChiMoi,
                                     BindingResult bindingResult,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            // Nếu lỗi, gửi lại form và lỗi
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.diaChiMoi", bindingResult);
            redirectAttributes.addFlashAttribute("diaChiMoi", diaChiMoi);
            return "redirect:/khach-hang/so-dia-chi";
        }
        
        try {
            diaChiService.themDiaChiMoi(diaChiMoi, authentication);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm địa chỉ mới thành công!");
        } catch (Exception e) {
             redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/khach-hang/so-dia-chi";
    }
}