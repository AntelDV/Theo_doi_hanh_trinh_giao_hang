package com.nhom12.doangiaohang.controller;

import com.nhom12.doangiaohang.model.TaiKhoan;
import com.nhom12.doangiaohang.model.ThongBaoMat;
import com.nhom12.doangiaohang.service.ThongBaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/thong-bao")
public class ThongBaoController {

    @Autowired private ThongBaoService thongBaoService;

    @GetMapping
    public String xemThongBao(Model model, Authentication authentication) {
        try {
            // Hộp thư đến
            List<ThongBaoMat> listTinDen = thongBaoService.getThongBaoCuaToi(authentication);
            model.addAttribute("danhSachThongBao", listTinDen);
            
            // Hộp thư đi 
            List<ThongBaoMat> listTinDi = thongBaoService.getThongBaoDaGui(authentication);
            model.addAttribute("danhSachDaGui", listTinDi);

            // Danh sách người nhận
            List<TaiKhoan> listNguoiNhan = thongBaoService.getDanhSachNguoiNhan(authentication);
            model.addAttribute("listNguoiNhan", listNguoiNhan);
            
            return "thong-bao"; 
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Lỗi tải dữ liệu: " + e.getMessage());
            return "thong-bao";
        }
    }

    @PostMapping("/gui")
    public String guiThongBao(@RequestParam("nguoiNhan") String usernameNguoiNhan,
                              @RequestParam("noiDung") String noiDung,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            thongBaoService.guiThongBaoMat(usernameNguoiNhan, noiDung, authentication);
            redirectAttributes.addFlashAttribute("successMessage", "Đã gửi tin nhắn bảo mật thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Gửi thất bại: " + e.getMessage());
        }
        return "redirect:/thong-bao";
    }
}