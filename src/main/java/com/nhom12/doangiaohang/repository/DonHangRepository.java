package com.nhom12.doangiaohang.repository;

import com.nhom12.doangiaohang.model.DonHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DonHangRepository extends JpaRepository<DonHang, Integer> {

    Optional<DonHang> findByMaVanDon(String maVanDon);
    List<DonHang> findByKhachHangGui_IdOrderByIdDonHangDesc(Integer idKhachHang);
    List<DonHang> findAllByOrderByIdDonHangDesc();
    @Query("SELECT dh FROM DonHang dh JOIN dh.hanhTrinh ht " +
           "WHERE ht.nhanVienThucHien.id = :shipperId " +
           "AND ht.thoiGianCapNhat = (SELECT MAX(ht2.thoiGianCapNhat) FROM HanhTrinhDonHang ht2 WHERE ht2.donHang = dh) " +
           "AND ht.trangThai.idTrangThai IN (1, 2, 4, 8) " + 
           "ORDER BY dh.idDonHang DESC")
    List<DonHang> findDonHangDangXuLyCuaShipper(@Param("shipperId") Integer shipperId);
}