package com.nhom12.doangiaohang.repository;

import com.nhom12.doangiaohang.model.NhatKyVanHanh;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; 
import org.springframework.stereotype.Repository;

import java.util.List; 

@Repository
public interface NhatKyVanHanhRepository extends JpaRepository<NhatKyVanHanh, Integer>, JpaSpecificationExecutor<NhatKyVanHanh> {
    
    List<NhatKyVanHanh> findAllByOrderByThoiGianThucHienDesc(); 
}