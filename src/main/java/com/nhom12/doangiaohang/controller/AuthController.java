package com.nhom12.doangiaohang.controller;

import com.nhom12.doangiaohang.dto.DangKyForm;
import com.nhom12.doangiaohang.dto.NhanVienDangKyForm; // Import DTO mới
import com.nhom12.doangiaohang.service.TaiKhoanService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    @Autowired
    private TaiKhoanService taiKhoanService;

    // (Hàm đăng ký khách hàng giữ nguyên)
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        if (!model.containsAttribute("dangKyForm")) {
            model.addAttribute("dangKyForm", new DangKyForm());
        }
        return "register";
    }

    @PostMapping("/register")
    public String processRegistration(@Valid @ModelAttribute("dangKyForm") DangKyForm dangKyForm,
                                      BindingResult bindingResult,
                                      RedirectAttributes redirectAttributes,
                                      Model model) {
        if (bindingResult.hasErrors()) {
            return "register";
        }
        try {
            taiKhoanService.dangKyKhachHang(dangKyForm);
            redirectAttributes.addFlashAttribute("successMessage", "Đăng ký tài khoản thành công! Vui lòng đăng nhập.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "register";
        }
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }
    
    // === THÊM CÁC HÀM MỚI ĐỂ TẠO NHÂN VIÊN ===
    
    // Trang ẩn để tạo tài khoản nhân viên (chỉ để test)
    @GetMapping("/register-admin")
    public String showRegisterAdminForm(Model model) {
        model.addAttribute("nhanVienForm", new NhanVienDangKyForm());
        return "register-admin"; // Tạo file HTML này
    }

    @PostMapping("/register-admin")
    public String processRegisterAdmin(@Valid @ModelAttribute("nhanVienForm") NhanVienDangKyForm nhanVienForm,
                                       BindingResult bindingResult,
                                       Model model) {
        if (bindingResult.hasErrors()) {
            return "register-admin";
        }
        try {
            taiKhoanService.dangKyNhanVien(nhanVienForm);
            model.addAttribute("successMessage", "Tạo tài khoản nhân viên thành công!");
            model.addAttribute("nhanVienForm", new NhanVienDangKyForm()); // Tạo form mới
            return "register-admin";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "register-admin";
        }
    }
}