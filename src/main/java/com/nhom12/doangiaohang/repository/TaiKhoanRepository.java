package com.nhom12.doangiaohang.repository;

import com.nhom12.doangiaohang.model.TaiKhoan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TaiKhoanRepository extends JpaRepository<TaiKhoan, Integer> {
    
    // Dùng cho việc đăng nhập và kiểm tra trùng tên đăng nhập
    Optional<TaiKhoan> findByTenDangNhap(String tenDangNhap);
    
    // Dùng cho chức năng quên mật khẩu
    Optional<TaiKhoan> findByMaDatLaiMk(String maDatLaiMk);

}