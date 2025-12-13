package com.nhom12.doangiaohang.repository;

import com.nhom12.doangiaohang.model.ThongBaoMat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ThongBaoMatRepository extends JpaRepository<ThongBaoMat, Integer> {
    // Tin nhận được (Hộp thư đến)
    List<ThongBaoMat> findByNguoiNhan_IdOrderByNgayTaoDesc(Integer idNguoiNhan);
    
    // Tin đã gửi (Hộp thư đi)
    List<ThongBaoMat> findByNguoiGui_IdOrderByNgayTaoDesc(Integer idNguoiGui);
}