package com.nhom12.doangiaohang.controller;

import com.nhom12.doangiaohang.model.DonHang;
import com.nhom12.doangiaohang.service.DonHangService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class TrackingController {

    @Autowired
    private DonHangService donHangService;

    @GetMapping("/tra-cuu")
    public String showTrackingPage() {
        return "tra-cuu"; // Trang tra-cuu.html
    }

    @PostMapping("/tra-cuu/don-hang")
    public String trackDonHang(@RequestParam("maVanDon") String maVanDon, Model model, RedirectAttributes redirectAttributes) {
        try {
            DonHang donHang = donHangService.getDonHangByMaVanDon(maVanDon);
            model.addAttribute("donHang", donHang);
            return "chi-tiet-cong-khai"; // Trang chi-tiet-cong-khai.html
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/tra-cuu";
        }
    }
}