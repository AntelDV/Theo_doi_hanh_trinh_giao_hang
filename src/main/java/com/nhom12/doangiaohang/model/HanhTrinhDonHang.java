package com.nhom12.doangiaohang.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Date;

@Data
@Entity
@Table(name = "HANH_TRINH_DON_HANG")
@EqualsAndHashCode(of = "idHanhTrinh") // Chỉ dùng ID để so sánh
@ToString(exclude = {"donHang", "nhanVienThucHien"}) // Tránh vòng lặp vô hạn khi log
public class HanhTrinhDonHang {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hanh_trinh_don_hang_seq")
    @SequenceGenerator(name = "hanh_trinh_don_hang_seq", sequenceName = "HANH_TRINH_DON_HANG_SEQ", allocationSize = 1)
    @Column(name = "ID_HANH_TRINH")
    private Integer idHanhTrinh;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_DON_HANG", nullable = false)
    private DonHang donHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_NHAN_VIEN_THUC_HIEN")
    private NhanVien nhanVienThucHien;

    // Lỗi của bạn sẽ hết sau khi tạo file TrangThaiDonHang.java
    @ManyToOne(fetch = FetchType.EAGER) // Lấy trạng thái ngay lập tức
    @JoinColumn(name = "ID_TRANG_THAI", nullable = false)
    private TrangThaiDonHang trangThai;

    @Column(name = "GHI_CHU_NHAN_VIEN", length = 500)
    private String ghiChuNhanVien;

    @Column(name = "THOI_GIAN_CAP_NHAT", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    @Temporal(TemporalType.TIMESTAMP)
    private Date thoiGianCapNhat;
}