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
        return "tra-cuu"; 
    }

    @PostMapping("/tra-cuu/don-hang")
    public String trackDonHang(@RequestParam("maVanDon") String maVanDon, Model model, RedirectAttributes redirectAttributes) {
        try {
            String maChuan = maVanDon.trim();
            DonHang donHang = donHangService.getDonHangByMaVanDon(maChuan);
            
            model.addAttribute("donHang", donHang);
            return "chi-tiet-cong-khai"; 
        } catch (Exception e) {
        	
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy đơn hàng với mã: " + maVanDon);
            return "redirect:/tra-cuu";
        }
    }
}