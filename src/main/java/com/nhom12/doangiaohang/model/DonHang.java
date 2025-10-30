package com.nhom12.doangiaohang.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "DON_HANG")
@EqualsAndHashCode(of = "idDonHang")
@ToString(exclude = {"khachHangGui", "diaChiLayHang", "hanhTrinh", "thanhToan"})
public class DonHang {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "don_hang_seq")
    @SequenceGenerator(name = "don_hang_seq", sequenceName = "DON_HANG_SEQ", allocationSize = 1)
    @Column(name = "ID_DON_HANG")
    private Integer idDonHang;

    @Column(name = "MA_VAN_DON", nullable = false, unique = true)
    private String maVanDon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_KHACH_HANG_GUI", nullable = false)
    private KhachHang khachHangGui;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_DIA_CHI_LAY_HANG", nullable = false)
    private DiaChi diaChiLayHang;

    @Column(name = "TEN_NGUOI_NHAN", nullable = false)
    private String tenNguoiNhan;

    @Column(name = "SDT_NGUOI_NHAN", nullable = false)
    private String sdtNguoiNhan;

    @Column(name = "DIA_CHI_GIAO_HANG", nullable = false, length = 500)
    private String diaChiGiaoHang;

    // =================================================================
    // SỬA LỖI XUNG ĐỘT HIBERNATE (RAW(255) vs RAW(2000))
    
    /**
     * Trường này mapping với cột RAW(2000) trong CSDL.
     * SỬA LỖI: Thêm (length = 2000) để Hibernate không cố gắng 
     * thay đổi nó về 255 byte.
     */
    @Column(name = "GHI_CHU_KHACH_HANG", length = 2000)
    private byte[] ghiChuKhachHangRaw;

    /**
     * Trường này KHÔNG lưu vào CSDL (@Transient).
     * Dùng để Form (UI) và Service (logic) làm việc (dạng String).
     */
    @Transient
    private String ghiChuKhachHang; 

    /**
     * Tự động chạy SAU KHI load dữ liệu từ CSDL.
     * Chuyển đổi byte[] (ghiChuKhachHangRaw) sang String (ghiChuKhachHang).
     * * QUAN TRỌNG: 
     * Vì CSDL đã mã hóa (Mức CSDL), chúng ta không thể giải mã nó ở đây.
     * Chúng ta phải gọi hàm giải mã của Oracle.
     * * GIẢI PHÁP TỐT NHẤT:
     * (Giữ nguyên từ file CSDL_NHOM12.sql của bạn)
     * ALTER TABLE DON_HANG MODIFY GHI_CHU_KHACH_HANG NVARCHAR2(500);
     * * Sau đó, thay thế @PostLoad và @PrePersist bằng:
     * * @Column(name = "GHI_CHU_KHACH_HANG")
     * @Convert(converter = GhiChuConverter.class) // Cần tạo 1 class Converter
     * private String ghiChuKhachHang;
     * * Tuy nhiên, để giữ cho CSDL của bạn (với Trigger và RAW) hoạt động,
     * chúng ta sẽ giữ nguyên logic @PostLoad/@PrePersist.
     */
    @PostLoad 
    void convertRawToString() {
        if (this.ghiChuKhachHangRaw != null) {
            try {
                // Thử chuyển byte[] sang String
                // Nếu trigger CSDL đã mã hóa, đây sẽ là chuỗi gibberish
                // Nếu trigger CSDL trả về lỗi (ví dụ: '[DB Error]'), nó sẽ hiển thị
                this.ghiChuKhachHang = new String(this.ghiChuKhachHangRaw, StandardCharsets.UTF_8);
            } catch (Exception e) {
                this.ghiChuKhachHang = "[Lỗi đọc ghi chú]";
            }
        }
    }

    /**
     * Tự động chạy TRƯỚC KHI lưu (INSERT/UPDATE) vào CSDL.
     * Chuyển đổi String (ghiChuKhachHang) từ Form sang byte[] (ghiChuKhachHangRaw).
     * Trigger "trg_don_hang_encrypt_note" của CSDL sẽ nhận byte[] này
     * và MÃ HÓA nó trước khi lưu vào cột RAW.
     */
    @PrePersist 
    @PreUpdate
    void convertStringToRaw() {
        if (this.ghiChuKhachHang != null && !this.ghiChuKhachHang.isEmpty()) {
            this.ghiChuKhachHangRaw = this.ghiChuKhachHang.getBytes(StandardCharsets.UTF_8);
        } else {
            this.ghiChuKhachHangRaw = null;
        }
    }
    // =================================================================

    @Column(name = "NGAY_TAO")
    @Temporal(TemporalType.DATE)
    private Date ngayTao;

    @OneToMany(mappedBy = "donHang", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("thoiGianCapNhat DESC")
    private List<HanhTrinhDonHang> hanhTrinh;
    
    @OneToOne(mappedBy = "donHang", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    private ThanhToan thanhToan;

    /**
     * Helper: Lấy trạng thái mới nhất từ danh sách hành trình.
     */
    @Transient
    public TrangThaiDonHang getTrangThaiHienTai() {
        if (hanhTrinh != null && !hanhTrinh.isEmpty()) {
            return hanhTrinh.get(0).getTrangThai();
        }
        return null;
    }

    /**
     * Helper: Lấy shipper đang được gán cho đơn hàng.
     */
    @Transient
    public NhanVien getShipperHienTai() {
         if (hanhTrinh != null) {
            for (HanhTrinhDonHang ht : hanhTrinh) {
                // Tìm hành trình mới nhất có gán shipper và trạng thái đang hoạt động
                if (ht.getNhanVienThucHien() != null && ht.getTrangThai() != null && 
                    List.of(1, 2, 3, 4, 6, 8).contains(ht.getTrangThai().getIdTrangThai())) {
                    return ht.getNhanVienThucHien();
                }
            }
        }
        return null;
    }
}