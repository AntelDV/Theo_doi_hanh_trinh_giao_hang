package com.nhom12.doangiaohang.controller;

import com.nhom12.doangiaohang.config.DataSourceConfig;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;

@Controller
public class SetupController {

    @GetMapping("/setup")
    public String showSetupPage(Model model) {
        File file = new File(DataSourceConfig.CONFIG_FILE);
        if (file.exists()) {
            model.addAttribute("message", "Hệ thống đã được cấu hình. Nếu muốn cấu hình lại, hãy xóa file 'db-config.properties' và khởi động lại.");
            return "setup-success";
        }
        return "setup";
    }

    @PostMapping("/setup")
    public String processSetup(@RequestParam("host") String host,
                               @RequestParam("port") String port,
                               @RequestParam("sid") String sid,
                               @RequestParam("username") String username,
                               @RequestParam("password") String password,
                               Model model) {
        try {
            Properties props = new Properties();
            String url = "jdbc:oracle:thin:@" + host + ":" + port + ":" + sid;
            
            props.setProperty("url", url);
            props.setProperty("username", username);
            props.setProperty("password", password);

            // Lưu vào file
            try (FileOutputStream fos = new FileOutputStream(DataSourceConfig.CONFIG_FILE)) {
                props.store(fos, "Oracle Database Configuration");
            }

            return "redirect:/setup-success";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Lỗi lưu cấu hình: " + e.getMessage());
            return "setup";
        }
    }
    
    @GetMapping("/setup-success")
    public String setupSuccess() {
        return "setup-success";
    }
}