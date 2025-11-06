package com.nhom12.doangiaohang.model;

import jakarta.persistence.*;
// XÓA CÁC IMPORT VALIDATION NÀY
// import jakarta.validation.constraints.Email;
// import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "NHAN_VIEN")
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"taiKhoan", "hanhTrinhThucHien"})
public class NhanVien {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "nhan_vien_seq")
    @SequenceGenerator(name = "nhan_vien_seq", sequenceName = "NHAN_VIEN_SEQ", allocationSize = 1)
    @Column(name = "ID_NHAN_VIEN")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_TAI_KHOAN", nullable = false, unique = true)
    private TaiKhoan taiKhoan;
    
    // GỠ BỎ @NotBlank
    @Column(name = "MA_NV", nullable = false, unique = true)
    private String maNV;

    // GỠ BỎ @NotBlank
    @Column(name = "HO_TEN", nullable = false)
    private String hoTen;

    // GỠ BỎ @Email
    @Column(name = "EMAIL", unique = true)
    private String email;

    @Column(name = "SO_DIEN_THOAI", unique = true)
    private String soDienThoai;

    @Column(name = "NGAY_VAO_LAM")
    @Temporal(TemporalType.DATE)
    private Date ngayVaoLam;

    @OneToMany(mappedBy = "nhanVienThucHien", fetch = FetchType.LAZY)
    private List<HanhTrinhDonHang> hanhTrinhThucHien;
}