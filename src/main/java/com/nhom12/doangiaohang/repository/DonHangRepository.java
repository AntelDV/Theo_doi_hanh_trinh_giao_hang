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
    
    /**
     * (SỬA LỖI 3 - KẾT HỢP)
     * Gỡ bỏ các hàm ...Decrypted()
     * Hàm @Formula trong DonHang.java sẽ tự động giải mã.
     */
    Optional<DonHang> findByMaVanDon(String maVanDon);

    /**
     * (SỬA LỖI 3 - KẾT HỢP)
     * Gỡ bỏ các hàm ...Decrypted()
     */
    List<DonHang> findByKhachHangGui_IdOrderByIdDonHangDesc(Integer idKhachHang);

    /**
     * (SỬA LỖI 3 - KẾT HỢP)
     * Gỡ bỏ các hàm ...Decrypted()
     */
    List<DonHang> findAllByOrderByIdDonHangDesc();

    /**
     * (SỬA LỖI 3 - KẾT HỢP)
     * Gỡ bỏ các hàm ...Decrypted()
     */
    // SỬA TỐI ƯU: Bỏ trạng thái 6 (Giao thất bại), vì logic service sẽ
    // tự động chuyển sang 7 (Chờ xử lý) và gỡ gán shipper.
    @Query("SELECT dh FROM DonHang dh JOIN dh.hanhTrinh ht " +
           "WHERE ht.nhanVienThucHien.id = :shipperId " +
           "AND ht.thoiGianCapNhat = (SELECT MAX(ht2.thoiGianCapNhat) FROM HanhTrinhDonHang ht2 WHERE ht2.donHang = dh) " +
           "AND ht.trangThai.idTrangThai IN (1, 2, 4, 8) " + // 1:Chờ lấy, 2:Đã lấy, 4:Đang giao, 8:Đang hoàn kho
           "ORDER BY dh.idDonHang DESC")
    List<DonHang> findDonHangDangXuLyCuaShipper(@Param("shipperId") Integer shipperId);
}