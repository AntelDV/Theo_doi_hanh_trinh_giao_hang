package com.nhom12.doangiaohang.controller;

// === BEGIN IMPORTS ===
// DTOs
import com.nhom12.doangiaohang.dto.NhanVienDangKyForm; 

// Models
import com.nhom12.doangiaohang.model.DonHang;
import com.nhom12.doangiaohang.model.NhanVien;
import com.nhom12.doangiaohang.model.NhatKyVanHanh; 
import com.nhom12.doangiaohang.model.TaiKhoan; 

// Services
import com.nhom12.doangiaohang.service.DonHangService;
import com.nhom12.doangiaohang.service.NhanVienService;
import com.nhom12.doangiaohang.service.NhatKyVanHanhService; 
import com.nhom12.doangiaohang.service.TaiKhoanService; 

// Spring & Validation
import jakarta.validation.Valid; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat; 
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult; 
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Java Utilities
import java.util.Date; 
import java.util.List;
// === END IMPORTS ===

@Controller
@RequestMapping("/quan-ly")
public class QuanLyController {

    // Dependencies Injection
    @Autowired private DonHangService donHangService;
    @Autowired private NhanVienService nhanVienService; 
    @Autowired private TaiKhoanService taiKhoanService; 
    @Autowired private NhatKyVanHanhService nhatKyVanHanhService; 

    // Trang chủ quản lý
    @GetMapping("/dashboard")
    public String dashboard() {
        return "quan-ly/dashboard"; 
    }

    // Luồng Quản lý đơn hàng - GET
    @GetMapping("/don-hang")
    public String quanLyDonHang(Model model) {
        List<DonHang> allDonHang = donHangService.getAllDonHangForQuanLy();
        List<NhanVien> shippers = nhanVienService.getAllShippers(); 

        model.addAttribute("donHangList", allDonHang);
        model.addAttribute("shipperList", shippers);
        return "quan-ly/don-hang"; 
    }

    // Phân công / Giao lại đơn hàng - POST
    @PostMapping("/don-hang/phan-cong")
    public String phanCongShipper(@RequestParam("idDonHang") Integer idDonHang,
                                  @RequestParam("idShipper") Integer idShipper,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        try {
            donHangService.phanCongShipper(idDonHang, idShipper, authentication);
            redirectAttributes.addFlashAttribute("successMessage", "Phân công/Giao lại thành công!");
        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi phân công: " + e.getMessage());
        } catch (Exception e) {
             redirectAttributes.addFlashAttribute("errorMessage", "Đã có lỗi hệ thống xảy ra.");
             e.printStackTrace(); // Giúp debug lỗi
        }
        return "redirect:/quan-ly/don-hang"; 
    }
    
    // Hoàn kho đơn hàng - POST
    @PostMapping("/don-hang/hoan-kho")
    public String hoanKhoDonHang(@RequestParam("idDonHang") Integer idDonHang,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
         try {
            donHangService.hoanKhoDonHang(idDonHang, authentication);
            redirectAttributes.addFlashAttribute("successMessage", "Duyệt hoàn kho thành công! Shipper sẽ đi hoàn hàng.");
        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hoàn kho: " + e.getMessage());
        } catch (Exception e) {
             redirectAttributes.addFlashAttribute("errorMessage", "Đã có lỗi hệ thống xảy ra.");
             e.printStackTrace(); // Giúp debug lỗi
        }
        return "redirect:/quan-ly/don-hang"; 
    }

    // === Luồng Quản lý Nhân viên ===
    // Hiển thị danh sách nhân viên - GET
    @GetMapping("/nhan-vien")
    public String quanLyNhanVien(Model model) {
        // Gọi hàm getAllNhanVien từ NhanVienService
        List<NhanVien> nhanVienList = nhanVienService.getAllNhanVien(); 
        model.addAttribute("nhanVienList", nhanVienList);
        if (!model.containsAttribute("nhanVienForm")) {
            model.addAttribute("nhanVienForm", new NhanVienDangKyForm());
        }
        return "quan-ly/nhan-vien"; 
    }
    
    // Tạo nhân viên mới - POST
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
        } catch (IllegalArgumentException e) {
             redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
             redirectAttributes.addFlashAttribute("nhanVienForm", nhanVienForm); 
             redirectAttributes.addFlashAttribute("showThemModalNV", true);
        } catch (Exception e) {
             redirectAttributes.addFlashAttribute("errorMessage", "Đã có lỗi hệ thống xảy ra.");
             e.printStackTrace(); // Giúp debug lỗi
        }
        return "redirect:/quan-ly/nhan-vien"; 
    }
    
    // Khóa tài khoản nhân viên - POST
    @PostMapping("/nhan-vien/khoa/{idNhanVien}")
    public String khoaNhanVien(@PathVariable("idNhanVien") Integer idNhanVien,
                                RedirectAttributes redirectAttributes) {
         try {
            // Gọi hàm findById từ NhanVienService
            NhanVien nv = nhanVienService.findById(idNhanVien); 
            TaiKhoan tk = nv.getTaiKhoan(); 
            if (tk != null) {
                taiKhoanService.khoaTaiKhoan(tk.getId()); 
                redirectAttributes.addFlashAttribute("successMessage", "Khóa tài khoản nhân viên [" + nv.getMaNV() + "] thành công!");
            } else {
                 redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: Không tìm thấy thông tin tài khoản của nhân viên này.");
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
             redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khóa: " + e.getMessage());
        } catch (Exception e) {
             redirectAttributes.addFlashAttribute("errorMessage", "Đã có lỗi hệ thống xảy ra.");
             e.printStackTrace(); // Giúp debug lỗi
        }
         return "redirect:/quan-ly/nhan-vien"; 
    }

    // Mở khóa tài khoản nhân viên - POST
    @PostMapping("/nhan-vien/mo-khoa/{idNhanVien}")
    public String moKhoaNhanVien(@PathVariable("idNhanVien") Integer idNhanVien,
                                  RedirectAttributes redirectAttributes) {
        try {
            // Gọi hàm findById từ NhanVienService
            NhanVien nv = nhanVienService.findById(idNhanVien); 
            TaiKhoan tk = nv.getTaiKhoan(); 
            if (tk != null) {
                taiKhoanService.moKhoaTaiKhoan(tk.getId()); 
                redirectAttributes.addFlashAttribute("successMessage", "Mở khóa tài khoản nhân viên [" + nv.getMaNV() + "] thành công!");
            } else {
                 redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: Không tìm thấy thông tin tài khoản của nhân viên này.");
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
             redirectAttributes.addFlashAttribute("errorMessage", "Lỗi mở khóa: " + e.getMessage());
        } catch (Exception e) {
             redirectAttributes.addFlashAttribute("errorMessage", "Đã có lỗi hệ thống xảy ra.");
             e.printStackTrace(); // Giúp debug lỗi
        }
         return "redirect:/quan-ly/nhan-vien"; 
    }
    
    // === Luồng Nhật ký ===
    // Hiển thị và lọc nhật ký - GET
    @GetMapping("/nhat-ky")
    public String quanLyNhatKy(
            @RequestParam(value = "tenDangNhap", required = false) String tenDangNhap,
            @RequestParam(value = "hanhDong", required = false) String hanhDong,
            @RequestParam(value = "tuNgay", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date tuNgay,
            @RequestParam(value = "denNgay", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date denNgay,
            Model model) {
        
        // Gọi hàm findNhatKy từ NhatKyVanHanhService       
        List<NhatKyVanHanh> nhatKyList = nhatKyVanHanhService.findNhatKy(tenDangNhap, hanhDong, tuNgay, denNgay); 
        model.addAttribute("nhatKyList", nhatKyList);
        
        model.addAttribute("tenDangNhapFilter", tenDangNhap);
        model.addAttribute("hanhDongFilter", hanhDong);
        model.addAttribute("tuNgayFilter", tuNgay);
        model.addAttribute("denNgayFilter", denNgay);
        
        return "quan-ly/nhat-ky"; 
    }
    
    @GetMapping("/don-hang/chi-tiet/{id}")
    public String xemChiTietDonHang(@PathVariable("id") Integer idDonHang, Model model, RedirectAttributes redirectAttributes) {
        try {
            DonHang donHang = donHangService.getChiTietDonHangChoQuanLy(idDonHang);
            model.addAttribute("donHang", donHang);
            return "quan-ly/chi-tiet-don-hang"; // Trả về view mới
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xem chi tiết: " + e.getMessage());
            return "redirect:/quan-ly/don-hang";
        }
    }
}