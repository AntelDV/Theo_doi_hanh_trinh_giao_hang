package com.nhom12.doangiaohang.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

// XÓA CÁC IMPORT VALIDATION NÀY
// import jakarta.validation.constraints.Email;
// import jakarta.validation.constraints.NotBlank;
// import jakarta.validation.constraints.Size;

import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "KHACH_HANG")
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"taiKhoan", "diaChis", "donHangsGui"})
public class KhachHang {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "khach_hang_seq")
    @SequenceGenerator(name = "khach_hang_seq", sequenceName = "KHACH_HANG_SEQ", allocationSize = 1)
    @Column(name = "ID_KHACH_HANG")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_TAI_KHOAN", nullable = false, unique = true)
    private TaiKhoan taiKhoan;

    // GỠ BỎ VALIDATION
    @Column(name = "HO_TEN", nullable = false)
    private String hoTen;

    // GỠ BỎ VALIDATION
    @Column(name = "EMAIL", unique = true)
    private String email;

    @Column(name = "SO_DIEN_THOAI", unique = true)
    private String soDienThoai;

    @Column(name = "NGAY_TAO")
    @Temporal(TemporalType.DATE)
    private Date ngayTao;

    @OneToMany(mappedBy = "khachHangSoHuu", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DiaChi> diaChis;

    @OneToMany(mappedBy = "khachHangGui", fetch = FetchType.LAZY)
    private List<DonHang> donHangsGui;
}