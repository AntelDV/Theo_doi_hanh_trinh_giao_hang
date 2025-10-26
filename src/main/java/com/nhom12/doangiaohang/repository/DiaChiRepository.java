package com.nhom12.doangiaohang.repository;

import com.nhom12.doangiaohang.model.DiaChi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiaChiRepository extends JpaRepository<DiaChi, Integer> {
    
    // Tìm tất cả địa chỉ thuộc về một khách hàng
    List<DiaChi> findByKhachHangSoHuu_Id(Integer idKhachHang);
}