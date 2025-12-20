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
        model.addAttribute("sessions", adminService.getActiveSessions()); 
        model.addAttribute("systemLogs", adminService.getUnifiedLogs());
        
        return "quan-ly/giam-sat";
    }

    @PostMapping("/kill")
    public String killUser(@RequestParam("sessionId") String sessionId, 
                           RedirectAttributes redirectAttributes) {
        try {
            adminService.killSession(sessionId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã ngắt kết nối phiên làm việc: " + sessionId);
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
            redirectAttributes.addFlashAttribute("errorMessage", "Khôi phục thất bại: " + e.getMessage());
        }
        return "redirect:/quan-ly/giam-sat";
    }
    
    @GetMapping("/backup-json")
    public ResponseEntity<ByteArrayResource> downloadBackup() {
        try {
            adminService.backupData(); 
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
    
    @PostMapping("/backup-db")
    public String triggerDbBackup(RedirectAttributes redirectAttributes) {
        try {
            adminService.backupData();
            redirectAttributes.addFlashAttribute("successMessage", "Đã yêu cầu Oracle sao lưu (Data Pump). Kiểm tra thư mục server.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi sao lưu DB: " + e.getMessage());
        }
        return "redirect:/quan-ly/giam-sat";
    }
}