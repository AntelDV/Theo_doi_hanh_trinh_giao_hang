package com.nhom12.doangiaohang.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Date;

@Data
@Entity
@Table(name = "TAI_KHOAN")
@EqualsAndHashCode(of = "id") // Dùng ID để so sánh, tránh lỗi vòng lặp
@ToString(exclude = "vaiTro") // Tránh lỗi vòng lặp khi log
public class TaiKhoan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tai_khoan_seq")
    @SequenceGenerator(name = "tai_khoan_seq", sequenceName = "TAI_KHOAN_SEQ", allocationSize = 1)
    @Column(name = "ID_TAI_KHOAN")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER) // Lấy vai trò ngay lập tức khi tải tài khoản
    @JoinColumn(name = "ID_VAI_TRO", nullable = false)
    private VaiTro vaiTro;

    @Column(name = "TEN_DANG_NHAP", unique = true, nullable = false, length = 50)
    private String tenDangNhap;

    @Column(name = "MAT_KHAU_HASH", nullable = false)
    private String matKhau;

    @Column(name = "TRANG_THAI", columnDefinition = "NUMBER(1,0) DEFAULT 1")
    private boolean trangThai = true; // true = 1 (Hoạt động), false = 0 (Bị khóa)

    @Column(name = "MA_DAT_LAI_MK", length = 100)
    private String maDatLaiMk;

    @Column(name = "THOI_HAN_MA")
    @Temporal(TemporalType.DATE) // Khớp với CSDL của bạn
    private Date thoiHanMa;
}