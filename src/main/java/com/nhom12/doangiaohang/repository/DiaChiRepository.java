package com.nhom12.doangiaohang.repository;

import com.nhom12.doangiaohang.model.DiaChi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiaChiRepository extends JpaRepository<DiaChi, Integer> {
    
    List<DiaChi> findByKhachHangSoHuu_Id(Integer idKhachHang);
}