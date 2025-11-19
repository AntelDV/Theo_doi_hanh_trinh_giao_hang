package com.nhom12.doangiaohang.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.Formula; // Import quan trọng
import java.nio.charset.StandardCharsets;

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
    
    @Column(name = "MA_NV", nullable = false, unique = true)
    private String maNV;

    // --- MÃ HÓA MỨC ỨNG DỤNG (JAVA) ---
    // Java sẽ mã hóa HoTen và SĐT trước khi lưu vào đây
    @Column(name = "HO_TEN", nullable = false)
    private String hoTen;

    @Column(name = "SO_DIEN_THOAI", unique = true)
    private String soDienThoai;

    // --- MÃ HÓA MỨC CSDL (TRIGGER) ---
    
    /**
     * Cột vật lý: EMAIL (Kiểu RAW)
     * Java ghi byte[] plaintext vào đây, Trigger sẽ bắt và mã hóa.
     */
    @Column(name = "EMAIL", unique = true)
    private byte[] emailRaw;

    /**
     * Trường ảo: Đọc dữ liệu đã giải mã từ DB lên.
     */
    @Formula("UTL_RAW.CAST_TO_VARCHAR2(encryption_pkg.decrypt_data(EMAIL))")
    private String email;

    /**
     * Setter đặc biệt: Chuyển String Email thành byte[] để gửi xuống DB.
     */
    public void setEmail(String email) {
        this.email = email;
        if (email != null) {
            this.emailRaw = email.getBytes(StandardCharsets.UTF_8);
        } else {
            this.emailRaw = null;
        }
    }
    
    public String getEmail() {
        return email;
    }

    // ---------------------------------

    @Column(name = "NGAY_VAO_LAM")
    @Temporal(TemporalType.DATE)
    private Date ngayVaoLam;

    @OneToMany(mappedBy = "nhanVienThucHien", fetch = FetchType.LAZY)
    private List<HanhTrinhDonHang> hanhTrinhThucHien;
}