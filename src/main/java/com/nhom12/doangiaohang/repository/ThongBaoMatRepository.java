package com.nhom12.doangiaohang.repository;

import com.nhom12.doangiaohang.model.ThongBaoMat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ThongBaoMatRepository extends JpaRepository<ThongBaoMat, Integer> {
    List<ThongBaoMat> findByNguoiNhan_IdOrderByNgayTaoDesc(Integer idNguoiNhan);
}