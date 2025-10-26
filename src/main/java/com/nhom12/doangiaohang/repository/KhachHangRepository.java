package com.nhom12.doangiaohang.repository;

import com.nhom12.doangiaohang.model.KhachHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface KhachHangRepository extends JpaRepository<KhachHang, Integer> {
    
    Optional<KhachHang> findByTaiKhoan_TenDangNhap(String tenDangNhap);
    boolean existsByEmail(String email);
    boolean existsBySoDienThoai(String soDienThoai);
    Optional<KhachHang> findByTaiKhoan_Id(Integer taiKhoanId);

    Optional<KhachHang> findByEmail(String email);
}