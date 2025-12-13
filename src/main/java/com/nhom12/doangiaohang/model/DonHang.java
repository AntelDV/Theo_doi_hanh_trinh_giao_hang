package com.nhom12.doangiaohang.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.Formula;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@Entity
@Table(name = "DON_HANG")
@EqualsAndHashCode(of = "idDonHang")
@ToString(exclude = {"khachHangGui", "diaChiLayHang", "hanhTrinh", "thanhToan"})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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

    @Column(name = "TEN_NGUOI_NHAN", nullable = false, length = 500)
    private String tenNguoiNhan;

    @Column(name = "DIA_CHI_GIAO_HANG", nullable = false, length = 2000)
    private String diaChiGiaoHang;

    @Column(name = "SDT_NGUOI_NHAN", nullable = false)
    private byte[] sdtNguoiNhanRaw;

    @Formula("UTL_RAW.CAST_TO_VARCHAR2(encryption_pkg.decrypt_data(SDT_NGUOI_NHAN))")
    private String sdtNguoiNhan;

    public void setSdtNguoiNhan(String sdt) {
        this.sdtNguoiNhan = sdt;
        if (sdt != null) {
            this.sdtNguoiNhanRaw = sdt.getBytes(StandardCharsets.UTF_8);
        } else {
            this.sdtNguoiNhanRaw = null;
        }
    }
    
    public String getSdtNguoiNhan() {
        return sdtNguoiNhan;
    }

    @Column(name = "GHI_CHU_KHACH_HANG", length = 500)
    private String ghiChuKhachHang;

    @Column(name = "NGAY_TAO")
    @Temporal(TemporalType.DATE)
    private Date ngayTao;

    @Column(name = "CH_KY_KHACH_HANG", columnDefinition = "CLOB")
    @Lob
    private String chKyKhachHang;
    
    // === TUẦN 7 (SV3): MÃ HÓA LAI - MÔ TẢ HÀNG HÓA ===
    @Column(name = "MO_TA_HANG_HOA", columnDefinition = "CLOB")
    @Lob
    private String moTaHangHoa; // Dữ liệu

    @Column(name = "MA_KHOA_HANG_HOA", columnDefinition = "CLOB")
    @Lob
    private String maKhoaHangHoa; // Khóa
    // =================================================

    @OneToMany(mappedBy = "donHang", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("thoiGianCapNhat DESC")
    private List<HanhTrinhDonHang> hanhTrinh;
    
    @OneToOne(mappedBy = "donHang", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    private ThanhToan thanhToan;

    @Transient
    public TrangThaiDonHang getTrangThaiHienTai() {
        if (hanhTrinh != null && !hanhTrinh.isEmpty()) {
            return hanhTrinh.get(0).getTrangThai();
        }
        return null;
    }

    @Transient
    public NhanVien getShipperHienTai() {
         if (hanhTrinh != null) {
            for (HanhTrinhDonHang ht : hanhTrinh) {
                if (ht.getNhanVienThucHien() != null && ht.getTrangThai() != null && 
                    List.of(1, 2, 3, 4, 6, 8).contains(ht.getTrangThai().getIdTrangThai())) {
                    return ht.getNhanVienThucHien();
                }
            }
        }
        return null;
    }
}