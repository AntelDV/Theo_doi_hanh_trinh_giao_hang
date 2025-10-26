package com.nhom12.doangiaohang.repository;

import com.nhom12.doangiaohang.model.NhanVien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NhanVienRepository extends JpaRepository<NhanVien, Integer> {

    Optional<NhanVien> findByTaiKhoan_TenDangNhap(String tenDangNhap);
    List<NhanVien> findByTaiKhoan_VaiTro_TenVaiTro(String tenVaiTro);
    Optional<NhanVien> findByTaiKhoan_Id(Integer taiKhoanId);
    Optional<NhanVien> findByEmail(String email);

    boolean existsByEmail(String email);
    boolean existsBySoDienThoai(String soDienThoai);
    boolean existsByMaNV(String maNV);
}