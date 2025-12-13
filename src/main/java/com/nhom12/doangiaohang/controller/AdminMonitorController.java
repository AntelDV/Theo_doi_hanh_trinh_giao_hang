package com.nhom12.doangiaohang.controller;

import com.nhom12.doangiaohang.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/quan-ly/giam-sat")
public class AdminMonitorController {

    @Autowired
    private AdminService adminService;

    @GetMapping
    public String showMonitorPage(Model model) {
        // Lấy danh sách user đang online
        model.addAttribute("sessions", adminService.getActiveSessions());
        return "quan-ly/giam-sat";
    }

    @PostMapping("/kill")
    public String killUser(@RequestParam("sid") Integer sid, 
                           @RequestParam("serial") Integer serial,
                           RedirectAttributes redirectAttributes) {
        try {
            adminService.killSession(sid, serial);
            redirectAttributes.addFlashAttribute("successMessage", "Đã ngắt kết nối phiên làm việc thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể ngắt kết nối: " + e.getMessage());
        }
        return "redirect:/quan-ly/giam-sat";
    }

    @PostMapping("/restore")
    public String restoreData(@RequestParam("minutes") Integer minutes,
                              RedirectAttributes redirectAttributes) {
        try {
            adminService.restoreData(minutes);
            redirectAttributes.addFlashAttribute("successMessage", "Đã khôi phục dữ liệu về " + minutes + " phút trước.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Khôi phục thất bại (Dữ liệu quá hạn hoặc lỗi hệ thống).");
            e.printStackTrace();
        }
        return "redirect:/quan-ly/giam-sat";
    }
}