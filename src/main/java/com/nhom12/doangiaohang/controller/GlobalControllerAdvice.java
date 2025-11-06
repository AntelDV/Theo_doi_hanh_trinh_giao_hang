package com.nhom12.doangiaohang.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Lớp "Global Helper" (Trình trợ giúp Toàn cục).
 * Nhiệm vụ: Tự động thêm các biến chung vào Model cho MỌI trang.
 */
@ControllerAdvice
public class GlobalControllerAdvice {

    /**
     * Hàm này sẽ tự động chạy trước mọi Controller.
     * Nó lấy URL (URI) hiện tại từ HttpServletRequest...
     * ...và đưa nó vào Model với tên là "currentURI".
     *
     * Giờ đây, mọi file Thymeleaf đều có thể truy cập biến ${currentURI}.
     */
    @ModelAttribute("currentURI")
    public String getCurrentURI(HttpServletRequest request) {
        // request ở đây là hợp lệ vì Spring đã tiêm nó vào hàm này.
        return request.getRequestURI();
    }
}