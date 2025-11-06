package com.nhom12.doangiaohang.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

// Thêm 2 import này
import org.hibernate.annotations.Formula;
import java.nio.charset.StandardCharsets;

import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "DON_HANG")
@EqualsAndHashCode(of = "idDonHang")
@ToString(exclude = {"khachHangGui", "diaChiLayHang", "hanhTrinh", "thanhToan"})
public class DonHang {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "don_hang_seq")
    @SequenceGenerator(name = "don_hang_seq", sequenceName = "DON_HANG_SEQ", allocationSize = 1)
    @Column(name = "ID_DON_HANG")
    private Integer idDonHang;

    @Column(name = "MA_VAN_DON", nullable = false, unique = true)
    private String maVanDon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_KHACH_HANG_GUI", nullable = false)
    private KhachHang khachHangGui;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_DIA_CHI_LAY_HANG", nullable = false)
    private DiaChi diaChiLayHang;

    @Column(name = "TEN_NGUOI_NHAN", nullable = false)
    private String tenNguoiNhan;

    @Column(name = "SDT_NGUOI_NHAN", nullable = false)
    private String sdtNguoiNhan;

    @Column(name = "DIA_CHI_GIAO_HANG", nullable = false, length = 500)
    private String diaChiGiaoHang;

    // =================================================================
    // KẾT HỢP 2 MỨC MÃ HÓA (ỨNG DỤNG & CSDL)
    
    /**
     * CỘT ĐỂ VIẾT (WRITE):
     * Trường này map với cột RAW(2000). 
     * Ứng dụng sẽ GHI byte[] (plaintext) vào đây.
     * Trigger CSDL sẽ BẮT LẤY và MÃ HÓA nó.
     */
    @Column(name = "GHI_CHU_KHACH_HANG", length = 2000)
    private byte[] ghiChuKhachHangRaw;

    /**
     * TRƯỜNG ĐỂ ĐỌC (READ):
     * Trường này KHÔNG phải là cột, nó là một CÔNG THỨC (@Formula).
     * Khi SELECT, Hibernate sẽ tự động chạy hàm CSDL để GIẢI MÃ.
     * UTL_RAW.CAST_TO_VARCHAR2 là để chuyển RAW (đã giải mã) về String.
     * Trường này là READ-ONLY.
     */
    @Formula("UTL_RAW.CAST_TO_VARCHAR2(encryption_pkg.decrypt_data(GHI_CHU_KHACH_HANG))")
    private String ghiChuKhachHang; 

    /**
     * HÀM HỖ TRỢ VIẾT (WRITE HELPER):
     * Vì trường 'ghiChuKhachHang' ở trên là READ-ONLY,
     * chúng ta dùng hàm này (từ Controller/Service) để set giá trị
     * cho trường 'ghiChuKhachHangRaw' (trường sẽ được ghi).
     */
    public void setGhiChuKhachHangPlainText(String plainText) {
        if (plainText != null && !plainText.isEmpty()) {
            this.ghiChuKhachHangRaw = plainText.getBytes(StandardCharsets.UTF_8);
        } else {
            this.ghiChuKhachHangRaw = null;
        }
    }

    // Xóa bỏ các hàm @PostLoad và @PrePersist vì chúng ta đã dùng @Formula
    
    // =================================================================

    @Column(name = "NGAY_TAO")
    @Temporal(TemporalType.DATE)
    private Date ngayTao;

    @OneToMany(mappedBy = "donHang", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("thoiGianCapNhat DESC")
    private List<HanhTrinhDonHang> hanhTrinh;
    
    @OneToOne(mappedBy = "donHang", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    private ThanhToan thanhToan;

    /**
     * Helper: Lấy trạng thái mới nhất từ danh sách hành trình.
     */
    @Transient
    public TrangThaiDonHang getTrangThaiHienTai() {
        if (hanhTrinh != null && !hanhTrinh.isEmpty()) {
            return hanhTrinh.get(0).getTrangThai();
        }
        return null;
    }

    /**
     * Helper: Lấy shipper đang được gán cho đơn hàng.
     */
    @Transient
    public NhanVien getShipperHienTai() {
         if (hanhTrinh != null) {
            for (HanhTrinhDonHang ht : hanhTrinh) {
                // Tìm hành trình mới nhất có gán shipper và trạng thái đang hoạt động
                if (ht.getNhanVienThucHien() != null && ht.getTrangThai() != null && 
                    List.of(1, 2, 3, 4, 6, 8).contains(ht.getTrangThai().getIdTrangThai())) {
                    return ht.getNhanVienThucHien();
                }
            }
        }
        return null;
    }
}