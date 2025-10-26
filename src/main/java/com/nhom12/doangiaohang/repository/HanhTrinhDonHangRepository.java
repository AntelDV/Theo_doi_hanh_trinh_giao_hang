package com.nhom12.doangiaohang.repository;

import com.nhom12.doangiaohang.model.HanhTrinhDonHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HanhTrinhDonHangRepository extends JpaRepository<HanhTrinhDonHang, Integer> {
    // Không cần thêm phương thức tùy chỉnh, JpaRepository là đủ
}