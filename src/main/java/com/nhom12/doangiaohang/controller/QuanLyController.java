package com.nhom12.doangiaohang.controller;

import com.nhom12.doangiaohang.dto.NhanVienDangKyForm; 
import com.nhom12.doangiaohang.model.DonHang;
import com.nhom12.doangiaohang.model.NhanVien;
import com.nhom12.doangiaohang.model.NhatKyVanHanh; 
import com.nhom12.doangiaohang.model.TaiKhoan; 
import com.nhom12.doangiaohang.service.AdminService; 
import com.nhom12.doangiaohang.service.DonHangService;
import com.nhom12.doangiaohang.service.NhanVienService;
import com.nhom12.doangiaohang.service.NhatKyVanHanhService; 
import com.nhom12.doangiaohang.service.TaiKhoanService; 

import jakarta.validation.Valid; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat; 
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult; 
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Date; 
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/quan-ly")
public class QuanLyController {

    @Autowired private DonHangService donHangService;
    @Autowired private NhanVienService nhanVienService; 
    @Autowired private TaiKhoanService taiKhoanService; 
    @Autowired private NhatKyVanHanhService nhatKyVanHanhService; 
    @Autowired private AdminService adminService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<DonHang> all = donHangService.getAllDonHangForQuanLy();
        long totalOrders = all.size();
        double totalRevenue = all.stream()
             .filter(d -> d.getTrangThaiHienTai().getIdTrangThai() == 5 && d.getThanhToan() != null)
             .mapToDouble(d -> d.getThanhToan().getTongTienCod() + (d.getThanhToan().getPhiVanChuyen()!=null?d.getThanhToan().getPhiVanChuyen():0))
             .sum();
        long totalShippers = nhanVienService.getAllShippers().size();
        long waitingOrders = all.stream().filter(d -> d.getTrangThaiHienTai().getIdTrangThai() == 1).count();

        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalShippers", totalShippers);
        model.addAttribute("waitingOrders", waitingOrders);

        Map<String, Long> statusCounts = all.stream()
            .collect(Collectors.groupingBy(d -> d.getTrangThaiHienTai().getTenTrangThai(), Collectors.counting()));
        List<String> chartLabels = statusCounts.keySet().stream().collect(Collectors.toList());
        List<Long> chartData = statusCounts.values().stream().collect(Collectors.toList());
        model.addAttribute("chartLabels", chartLabels);
        model.addAttribute("chartData", chartData);

        return "quan-ly/dashboard"; 
    }

    @GetMapping("/don-hang")
    public String quanLyDonHang(Model model) {
        List<DonHang> allDonHang = donHangService.getAllDonHangForQuanLy();
        List<NhanVien> shippers = nhanVienService.getAllShippers(); 
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
            redirectAttributes.addFlashAttribute("successMessage", "Phân công/Giao lại thành công!");
        } catch (Exception e) {
             redirectAttributes.addFlashAttribute("errorMessage", "Lỗi phân công: " + e.getMessage());
        }
        return "redirect:/quan-ly/don-hang"; 
    }
    
    @PostMapping("/don-hang/hoan-kho")
    public String hoanKhoDonHang(@RequestParam("idDonHang") Integer idDonHang,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
         try {
            donHangService.hoanKhoDonHang(idDonHang, authentication);
            redirectAttributes.addFlashAttribute("successMessage", "Duyệt hoàn kho thành công!");
        } catch (Exception e) {
             redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/quan-ly/don-hang"; 
    }

    @GetMapping("/nhan-vien")
    public String quanLyNhanVien(Model model) {
        List<NhanVien> nhanVienList = nhanVienService.getAllNhanVien(); 
        model.addAttribute("nhanVienList", nhanVienList);
        if (!model.containsAttribute("nhanVienForm")) {
            model.addAttribute("nhanVienForm", new NhanVienDangKyForm());
        }
        return "quan-ly/nhan-vien"; 
    }
    
    @PostMapping("/nhan-vien/them")
    public String processThemNhanVien(@Valid @ModelAttribute("nhanVienForm") NhanVienDangKyForm nhanVienForm,
                                       BindingResult bindingResult,
                                       RedirectAttributes redirectAttributes) {
         if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.nhanVienForm", bindingResult);
            redirectAttributes.addFlashAttribute("nhanVienForm", nhanVienForm);
            redirectAttributes.addFlashAttribute("showThemModalNV", true); 
            return "redirect:/quan-ly/nhan-vien"; 
        }
        try {
            taiKhoanService.dangKyNhanVien(nhanVienForm);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo tài khoản nhân viên thành công!");
        } catch (Exception e) {
             redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/quan-ly/nhan-vien"; 
    }
    
    @PostMapping("/nhan-vien/khoa/{idNhanVien}")
    public String khoaNhanVien(@PathVariable("idNhanVien") Integer idNhanVien, RedirectAttributes redirectAttributes) {
         try {
            NhanVien nv = nhanVienService.findById(idNhanVien); 
            if (nv.getTaiKhoan() != null) {
                taiKhoanService.khoaTaiKhoan(nv.getTaiKhoan().getId()); 
                redirectAttributes.addFlashAttribute("successMessage", "Khóa tài khoản thành công!");
            }
        } catch (Exception e) {
             redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
         return "redirect:/quan-ly/nhan-vien"; 
    }

    @PostMapping("/nhan-vien/mo-khoa/{idNhanVien}")
    public String moKhoaNhanVien(@PathVariable("idNhanVien") Integer idNhanVien, RedirectAttributes redirectAttributes) {
        try {
            NhanVien nv = nhanVienService.findById(idNhanVien); 
            if (nv.getTaiKhoan() != null) {
                taiKhoanService.moKhoaTaiKhoan(nv.getTaiKhoan().getId()); 
                redirectAttributes.addFlashAttribute("successMessage", "Mở khóa tài khoản thành công!");
            }
        } catch (Exception e) {
             redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
         return "redirect:/quan-ly/nhan-vien"; 
    }
    
    @GetMapping("/nhat-ky")
    public String quanLyNhatKy(Model model) {
        List<Object[]> logs = adminService.getUnifiedAuditLog();
        model.addAttribute("auditLogs", logs);
        return "quan-ly/nhat-ky"; 
    }
    
    
    @GetMapping("/don-hang/chi-tiet/{id}")
    public String xemChiTietDonHang(@PathVariable("id") Integer idDonHang, Model model, RedirectAttributes redirectAttributes) {
        try {
            DonHang donHang = donHangService.getChiTietDonHangChoQuanLy(idDonHang);
            model.addAttribute("donHang", donHang);
            return "quan-ly/chi-tiet-don-hang";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xem chi tiết: " + e.getMessage());
            return "redirect:/quan-ly/don-hang";
        }
    }
}