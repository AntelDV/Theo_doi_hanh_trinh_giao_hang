package com.nhom12.doangiaohang.repository;

import com.nhom12.doangiaohang.model.ThongBaoMat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ThongBaoMatRepository extends JpaRepository<ThongBaoMat, Integer> {
    
    List<ThongBaoMat> findByNguoiNhan_IdOrderByNgayTaoDesc(Integer idNguoiNhan);
    List<ThongBaoMat> findByNguoiGui_IdOrderByNgayTaoDesc(Integer idNguoiGui);

    
    @Query(value = "SELECT * FROM THONG_BAO_MAT WHERE ID_NGUOI_NHAN = :idNguoiNhan ORDER BY NGAY_TAO DESC", nativeQuery = true)
    List<ThongBaoMat> findByNguoiNhanNative(@Param("idNguoiNhan") Integer idNguoiNhan);

    @Query(value = "SELECT * FROM THONG_BAO_MAT WHERE ID_NGUOI_GUI = :idNguoiGui ORDER BY NGAY_TAO DESC", nativeQuery = true)
    List<ThongBaoMat> findByNguoiGuiNative(@Param("idNguoiGui") Integer idNguoiGui);
}