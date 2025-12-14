package com.nhom12.doangiaohang.controller;

import com.nhom12.doangiaohang.model.DiaChi;
import com.nhom12.doangiaohang.model.DonHang;
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
    public String dashboard(Authentication authentication, Model model) {
        List<DonHang> list = donHangService.getDonHangCuaKhachHangHienTai(authentication);
        
        long choLay = list.stream()
                .filter(d -> d.getTrangThaiHienTai() != null && d.getTrangThaiHienTai().getIdTrangThai() == 1)
                .count();
                
        long dangGiao = list.stream()
                .filter(d -> d.getTrangThaiHienTai() != null && (d.getTrangThaiHienTai().getIdTrangThai() == 3 || d.getTrangThaiHienTai().getIdTrangThai() == 4))
                .count();
                
        long hoanThanh = list.stream()
                .filter(d -> d.getTrangThaiHienTai() != null && d.getTrangThaiHienTai().getIdTrangThai() == 5)
                .count();
                
        double tongTien = list.stream()
                .filter(d -> d.getTrangThaiHienTai() != null && d.getTrangThaiHienTai().getIdTrangThai() == 5 && d.getThanhToan() != null)
                .mapToDouble(d -> d.getThanhToan().getTongTienCod() + (d.getThanhToan().getPhiVanChuyen() != null ? d.getThanhToan().getPhiVanChuyen() : 0))
                .sum();

        model.addAttribute("countChoLay", choLay);
        model.addAttribute("countDangGiao", dangGiao);
        model.addAttribute("countHoanThanh", hoanThanh);
        model.addAttribute("tongTien", tongTien);
        
        if(!list.isEmpty()) {
            model.addAttribute("donMoiNhat", list.get(0));
        }

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
            if (donHang.getThanhToan() != null) {
                donHang.getThanhToan().setDonHang(donHang); 
            } else {
                ThanhToan tt = new ThanhToan();
                tt.setDonHang(donHang);
                donHang.setThanhToan(tt);
            }
             
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
        } catch (Exception e) { 
             model.addAttribute("errorMessage", "Đã có lỗi hệ thống xảy ra khi tạo đơn hàng.");
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
    
    @PostMapping("/don-hang/huy/{id}")
    public String huyDonHang(@PathVariable("id") Integer idDonHang,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            donHangService.huyDonHang(idDonHang, authentication);
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy đơn hàng thành công.");
        } catch (IllegalStateException | SecurityException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống: " + e.getMessage());
            e.printStackTrace();
        }
        return "redirect:/khach-hang/danh-sach-don-hang";
    }

    @GetMapping("/so-dia-chi")
    public String soDiaChi(Authentication authentication, Model model) {
         List<DiaChi> diaChiList = diaChiService.getDiaChiByCurrentUser(authentication);
         model.addAttribute("diaChiList", diaChiList);
         if (!model.containsAttribute("diaChiMoi")) {
             model.addAttribute("diaChiMoi", new DiaChi());
         }
          if (!model.containsAttribute("diaChiSua")) {
             model.addAttribute("diaChiSua", new DiaChi());
         }
        return "khach-hang/so-dia-chi";
    }
    
    @PostMapping("/so-dia-chi/them")
    public String processThemDiaChi(@Valid @ModelAttribute("diaChiMoi") DiaChi diaChiMoi,
                                     BindingResult bindingResult,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.diaChiMoi", bindingResult);
            redirectAttributes.addFlashAttribute("diaChiMoi", diaChiMoi);
            redirectAttributes.addFlashAttribute("showThemModal", true); 
            return "redirect:/khach-hang/so-dia-chi";
        }
        try {
            diaChiService.themDiaChiMoi(diaChiMoi, authentication);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm địa chỉ mới thành công!");
        } catch (Exception e) {
             redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi thêm địa chỉ: " + e.getMessage());
        }
        return "redirect:/khach-hang/so-dia-chi";
    }
    
    @GetMapping("/so-dia-chi/sua/{id}")
    public String showSuaDiaChiForm(@PathVariable("id") Integer idDiaChi,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        try {
            DiaChi diaChi = diaChiService.findByIdAndCheckOwnership(idDiaChi, authentication);
            redirectAttributes.addFlashAttribute("diaChiSua", diaChi);
            redirectAttributes.addFlashAttribute("showSuaModal", true); 
        } catch (IllegalArgumentException | SecurityException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/khach-hang/so-dia-chi";
    }
    
    @PostMapping("/so-dia-chi/sua")
    public String processSuaDiaChi(@Valid @ModelAttribute("diaChiSua") DiaChi diaChiSua,
                                    BindingResult bindingResult,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
         if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.diaChiSua", bindingResult);
            redirectAttributes.addFlashAttribute("diaChiSua", diaChiSua);
            redirectAttributes.addFlashAttribute("showSuaModal", true); 
            return "redirect:/khach-hang/so-dia-chi";
        }
        try {
            diaChiService.capNhatDiaChi(diaChiSua, authentication);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật địa chỉ thành công!");
        } catch (Exception e) {
             redirectAttributes.addFlashAttribute("errorMessage", "Đã có lỗi: " + e.getMessage());
             redirectAttributes.addFlashAttribute("diaChiSua", diaChiSua);
             redirectAttributes.addFlashAttribute("showSuaModal", true);
        }
         return "redirect:/khach-hang/so-dia-chi";
    }
    
    @PostMapping("/so-dia-chi/xoa/{id}")
    public String processXoaDiaChi(@PathVariable("id") Integer idDiaChi,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        try {
            diaChiService.xoaDiaChi(idDiaChi, authentication);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa địa chỉ thành công!");
        } catch (Exception e) {
             redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xóa: " + e.getMessage());
        }
        return "redirect:/khach-hang/so-dia-chi";
    }
    
    @PostMapping("/so-dia-chi/mac-dinh/{id}")
    public String processDatMacDinh(@PathVariable("id") Integer idDiaChi,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
         try {
            diaChiService.datLamMacDinh(idDiaChi, authentication);
            redirectAttributes.addFlashAttribute("successMessage", "Đặt làm địa chỉ mặc định thành công!");
        } catch (Exception e) {
             redirectAttributes.addFlashAttribute("errorMessage", "Đã có lỗi hệ thống xảy ra.");
        }
        return "redirect:/khach-hang/so-dia-chi";
    }
}