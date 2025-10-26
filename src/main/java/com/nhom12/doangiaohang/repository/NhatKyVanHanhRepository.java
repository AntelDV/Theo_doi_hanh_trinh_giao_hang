package com.nhom12.doangiaohang.repository;

import com.nhom12.doangiaohang.model.NhatKyVanHanh;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; 
import org.springframework.stereotype.Repository;

import java.util.List; 

@Repository
// Kế thừa JpaSpecificationExecutor để hỗ trợ lọc động
public interface NhatKyVanHanhRepository extends JpaRepository<NhatKyVanHanh, Integer>, JpaSpecificationExecutor<NhatKyVanHanh> {
    
    // Lấy tất cả và sắp xếp theo thời gian mới nhất
    List<NhatKyVanHanh> findAllByOrderByThoiGianThucHienDesc(); 
}