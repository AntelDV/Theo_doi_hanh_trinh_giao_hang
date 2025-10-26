package com.nhom12.doangiaohang.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@Entity
@Table(name = "THANH_TOAN")
@EqualsAndHashCode(of = "idThanhToan")
@ToString(exclude = "donHang")
public class ThanhToan {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "thanh_toan_seq")
    @SequenceGenerator(name = "thanh_toan_seq", sequenceName = "THANH_TOAN_SEQ", allocationSize = 1)
    @Column(name = "ID_THANH_TOAN")
    private Integer idThanhToan;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_DON_HANG", nullable = false, unique = true)
    private DonHang donHang;

    @Column(name = "TONG_TIEN_COD", columnDefinition = "NUMBER DEFAULT 0")
    private Double tongTienCod = 0.0;

    @Column(name = "PHI_VAN_CHUYEN")
    private Double phiVanChuyen;

    @Column(name = "DA_THANH_TOAN_COD", columnDefinition = "NUMBER(1,0) DEFAULT 0")
    private boolean daThanhToanCod = false;
}