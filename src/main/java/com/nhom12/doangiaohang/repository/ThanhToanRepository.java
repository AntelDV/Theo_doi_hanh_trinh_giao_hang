package com.nhom12.doangiaohang.repository;

import com.nhom12.doangiaohang.model.ThanhToan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional; 

@Repository
public interface ThanhToanRepository extends JpaRepository<ThanhToan, Integer> {
    
    // Tự động tìm bản ghi ThanhToan dựa trên ID của DonHang
    Optional<ThanhToan> findByDonHang_IdDonHang(Integer idDonHang);
}