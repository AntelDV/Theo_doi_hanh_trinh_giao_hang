package com.nhom12.doangiaohang.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
@Data
@Entity
@Table(name = "LICH_SU_HOAT_DONG")
@EqualsAndHashCode(of = "idHoatDong")
@ToString(exclude = "taiKhoanThucHien")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class NhatKyVanHanh {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "lich_su_seq")
    @SequenceGenerator(name = "lich_su_seq", sequenceName = "LICH_SU_HOAT_DONG_SEQ", allocationSize = 1)
    @Column(name = "ID_HOAT_DONG") 
    private Integer idHoatDong;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_TAI_KHOAN_THUC_HIEN", nullable = false)
    private TaiKhoan taiKhoanThucHien;

    @Column(name = "HANH_DONG", nullable = false, length = 100)
    private String hanhDong;

    @Column(name = "DOI_TUONG_BI_ANH_HUONG", length = 50)
    private String doiTuongBiAnhHuong;

    @Column(name = "ID_DOI_TUONG")
    private Integer idDoiTuong;

    // Cột này chứa dữ liệu MÃ HÓA RSA từ Trigger
    @Column(name = "MO_TA_CHI_TIET", length = 4000) 
    private String moTaChiTiet;

    @Column(name = "THOI_GIAN_THUC_HIEN", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    @Temporal(TemporalType.TIMESTAMP)
    private Date thoiGianThucHien;

    @Column(name = "DIA_CHI_IP", length = 50)
    private String diaChiIp;
}