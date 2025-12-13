package com.nhom12.doangiaohang.controller;

import com.nhom12.doangiaohang.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/quan-ly/giam-sat")
public class AdminMonitorController {

    @Autowired
    private AdminService adminService;

    @GetMapping
    public String showMonitorPage(Model model) {
        // Lấy danh sách user đang online từ bảng THEO_DOI_ONLINE
        model.addAttribute("sessions", adminService.getActiveSessions());
        return "quan-ly/giam-sat";
    }

    @PostMapping("/kill")
    public String killUser(@RequestParam("sessionId") String sessionId, 
                           RedirectAttributes redirectAttributes) {
        try {
            adminService.killSession(sessionId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã ngắt kết nối người dùng thành công.");
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
            redirectAttributes.addFlashAttribute("errorMessage", "Khôi phục thất bại (Quá hạn hoặc lỗi DB): " + e.getMessage());
        }
        return "redirect:/quan-ly/giam-sat";
    }
    
    // TÍNH NĂNG MỚI: Tải Backup JSON
    @GetMapping("/backup-json")
    public ResponseEntity<ByteArrayResource> downloadBackup() {
        try {
            // Trigger thủ tục ghi log backup trong DB để có dấu vết kiểm toán
            adminService.backupData(); 
            
            // Lấy dữ liệu JSON thực tế từ Service
            String jsonData = adminService.exportDataToJson();
            ByteArrayResource resource = new ByteArrayResource(jsonData.getBytes(StandardCharsets.UTF_8));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=backup_system_" + System.currentTimeMillis() + ".json")
                    .contentType(MediaType.APPLICATION_JSON)
                    .contentLength(resource.contentLength())
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}