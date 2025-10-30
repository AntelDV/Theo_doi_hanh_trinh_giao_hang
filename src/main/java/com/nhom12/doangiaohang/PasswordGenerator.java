package com.nhom12.doangiaohang;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGenerator {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "password123";
        String encodedPassword = encoder.encode(rawPassword);
        
        System.out.println("===============================================================");
        System.out.println("Mật khẩu (password123) đã được hash thành:");
        System.out.println(encodedPassword);
        System.out.println("Hãy COPY chuỗi hash ở trên (bắt đầu bằng $2a$)");
        System.out.println("===============================================================");
    }
}