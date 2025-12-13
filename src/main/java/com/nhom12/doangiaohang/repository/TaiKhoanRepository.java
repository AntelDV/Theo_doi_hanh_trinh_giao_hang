package com.nhom12.doangiaohang.repository;

import com.nhom12.doangiaohang.model.TaiKhoan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaiKhoanRepository extends JpaRepository<TaiKhoan, Integer> {
    
    Optional<TaiKhoan> findByTenDangNhap(String tenDangNhap);
    
    Optional<TaiKhoan> findByMaDatLaiMk(String maDatLaiMk);

    // Loại trừ chính người đang đăng nhập (để không gửi tin cho chính mình)
    @Query("SELECT t FROM TaiKhoan t " +
           "WHERE t.trangThai = true " +
           "AND t.tenDangNhap <> :currentUsername " +
           "AND t.vaiTro.idVaiTro IN (1, 2) " +
           "ORDER BY t.vaiTro.idVaiTro, t.tenDangNhap")
    List<TaiKhoan> findNguoiNhanKhaDung(@Param("currentUsername") String currentUsername);
}