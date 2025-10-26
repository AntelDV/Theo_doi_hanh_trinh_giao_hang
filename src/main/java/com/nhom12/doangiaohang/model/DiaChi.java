package com.nhom12.doangiaohang.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@Entity
@Table(name = "DIA_CHI")
@EqualsAndHashCode(of = "idDiaChi")
@ToString(exclude = "khachHangSoHuu")
public class DiaChi {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dia_chi_seq")
    @SequenceGenerator(name = "dia_chi_seq", sequenceName = "DIA_CHI_SEQ", allocationSize = 1)
    @Column(name = "ID_DIA_CHI")
    private Integer idDiaChi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_KHACH_HANG_SO_HUU", nullable = false)
    private KhachHang khachHangSoHuu;

    @NotBlank(message = "Số nhà, đường không được để trống")
    @Column(name = "SO_NHA_DUONG", nullable = false)
    private String soNhaDuong;

    @Column(name = "PHUONG_XA")
    private String phuongXa;

    @NotBlank(message = "Quận/Huyện không được để trống")
    @Column(name = "QUAN_HUYEN", nullable = false)
    private String quanHuyen;

    @NotBlank(message = "Tỉnh/Thành phố không được để trống")
    @Column(name = "TINH_TP", nullable = false)
    private String tinhTp;

    @Column(name = "LA_MAC_DINH", columnDefinition = "NUMBER(1,0) DEFAULT 0")
    private boolean laMacDinh = false;

    // Helper method
    public String getFullAddress() {
        return soNhaDuong + (phuongXa != null && !phuongXa.isEmpty() ? ", " + phuongXa : "") + ", " + quanHuyen + ", " + tinhTp;
    }
}