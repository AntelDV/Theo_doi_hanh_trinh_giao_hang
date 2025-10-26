package com.nhom12.doangiaohang.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Date;

@Data
@Entity
@Table(name = "NHAT_KY_VAN_HANH")
@EqualsAndHashCode(of = "idNhatKy")
@ToString(exclude = "taiKhoanThucHien")
public class NhatKyVanHanh {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "nhat_ky_van_hanh_seq")
    @SequenceGenerator(name = "nhat_ky_van_hanh_seq", sequenceName = "NHAT_KY_VAN_HANH_SEQ", allocationSize = 1)
    @Column(name = "ID_NHAT_KY")
    private Integer idNhatKy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_TAI_KHOAN_THUC_HIEN", nullable = false)
    private TaiKhoan taiKhoanThucHien;

    @Column(name = "HANH_DONG", nullable = false, length = 100)
    private String hanhDong;

    @Column(name = "DOI_TUONG_BI_ANH_HUONG", length = 50)
    private String doiTuongBiAnhHuong;

    @Column(name = "ID_DOI_TUONG")
    private Integer idDoiTuong;

    @Column(name = "MO_TA_CHI_TIET", length = 1000)
    private String moTaChiTiet;

    @Column(name = "THOI_GIAN_THUC_HIEN", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    @Temporal(TemporalType.TIMESTAMP)
    private Date thoiGianThucHien;

    @Column(name = "DIA_CHI_IP", length = 50)
    private String diaChiIp;
}