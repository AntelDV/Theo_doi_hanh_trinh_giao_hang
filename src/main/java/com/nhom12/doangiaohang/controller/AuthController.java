package com.nhom12.doangiaohang.controller;

import com.nhom12.doangiaohang.dto.DangKyForm;
import com.nhom12.doangiaohang.dto.NhanVienDangKyForm;
import com.nhom12.doangiaohang.model.TaiKhoan; 
import com.nhom12.doangiaohang.service.TaiKhoanService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam; 
import org.springframework.web.servlet.mvc.support.RedirectAttributes; 

@Controller
public class AuthController {

    @Autowired
    private TaiKhoanService taiKhoanService;

    
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

    
    // ============================================================
    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }


    @GetMapping("/register-admin")
    public String showRegisterAdminForm(Model model) {
        if (!model.containsAttribute("nhanVienForm")) {
            model.addAttribute("nhanVienForm", new NhanVienDangKyForm());
        }
        return "register-admin";
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
            model.addAttribute("nhanVienForm", new NhanVienDangKyForm()); // Reset form
            return "register-admin";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "register-admin";
        }
    }

    
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam("username") String username, Model model) {
        try {
            taiKhoanService.processForgotPassword(username);
            // Chuyển hướng sang trang nhập Token, kèm theo username trên URL để tiện theo dõi
            return "redirect:/reset-password?username=" + username;
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "forgot-password";
        }
    }

    // Hiển thị form nhập Token và Mật khẩu mới
    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam(value = "username", required = false) String username, Model model) {
        model.addAttribute("username", username);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam("token") String token,
                                       @RequestParam("password") String password,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
        try {
            // Kiểm tra token hợp lệ
            TaiKhoan tk = taiKhoanService.validatePasswordResetToken(token);

            // Thực hiện đổi mật khẩu
            taiKhoanService.changeUserPassword(tk, password);

            redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công! Vui lòng đăng nhập lại.");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "reset-password";
        }
    }
}