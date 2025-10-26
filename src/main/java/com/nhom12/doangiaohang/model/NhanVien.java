package com.nhom12.doangiaohang.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
    
    @NotBlank(message = "Mã nhân viên không được để trống")
    @Column(name = "MA_NV", nullable = false, unique = true)
    private String maNV;

    @NotBlank(message = "Họ tên không được để trống")
    @Column(name = "HO_TEN", nullable = false)
    private String hoTen;

    @Email(message = "Email không hợp lệ")
    @Column(name = "EMAIL", unique = true)
    private String email;

    @Column(name = "SO_DIEN_THOAI", unique = true)
    private String soDienThoai;

    @Column(name = "NGAY_VAO_LAM")
    @Temporal(TemporalType.DATE)
    private Date ngayVaoLam;

    // Quan hệ một-nhiều với HanhTrinhDonHang (Nhân viên là người thực hiện)
    @OneToMany(mappedBy = "nhanVienThucHien", fetch = FetchType.LAZY)
    private List<HanhTrinhDonHang> hanhTrinhThucHien;
}