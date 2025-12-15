package com.nhom12.doangiaohang.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.Formula;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@Entity
@Table(name = "DIA_CHI")
@EqualsAndHashCode(of = "idDiaChi")
@ToString(exclude = "khachHangSoHuu")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DiaChi {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dia_chi_seq")
    @SequenceGenerator(name = "dia_chi_seq", sequenceName = "DIA_CHI_SEQ", allocationSize = 1)
    @Column(name = "ID_DIA_CHI")
    private Integer idDiaChi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_KHACH_HANG_SO_HUU", nullable = false)
    @JsonIgnore
    private KhachHang khachHangSoHuu;

    // --- MÃ HÓA MỨC ỨNG DỤNG (JAVA) ---
    @NotBlank(message = "Số nhà, đường không được để trống")
    @Column(name = "SO_NHA_DUONG", nullable = false)
    private String soNhaDuong;

    @Column(name = "PHUONG_XA")
    private String phuongXa;

    // --- MÃ HÓA MỨC CSDL (TRIGGER) ---
    
    // 1. Cột vật lý (RAW): Dùng để lưu trữ (Write)
    @Column(name = "QUAN_HUYEN", nullable = false)
    @JsonIgnore
    private byte[] quanHuyenRaw;

    // 2. Trường ảo (String): Dùng để hiển thị (Read)
    // Đổi tên biến khác với tên cột để tránh xung đột mapping
    @Formula("UTL_I18N.RAW_TO_CHAR(encryption_pkg.decrypt_data(QUAN_HUYEN), 'AL32UTF8')")
    private String tenQuanHuyen;

    // Getter/Setter tùy chỉnh để map giữa String và RAW
    public void setTenQuanHuyen(String tenQuanHuyen) {
        this.tenQuanHuyen = tenQuanHuyen;
        // Chuyển sang byte[] để Hibernate lưu xuống cột QUAN_HUYEN
        if (tenQuanHuyen != null) {
            this.quanHuyenRaw = tenQuanHuyen.getBytes(StandardCharsets.UTF_8);
        } else {
            this.quanHuyenRaw = null;
        }
    }
    
    public String getTenQuanHuyen() {
        return tenQuanHuyen;
    }

    // Getter giả để tương thích với code cũ nếu có gọi getQuanHuyen()
    public String getQuanHuyen() {
        return getTenQuanHuyen();
    }
    public void setQuanHuyen(String qh) {
        setTenQuanHuyen(qh);
    }

    // ---------------------------------

    @NotBlank(message = "Tỉnh/Thành phố không được để trống")
    @Column(name = "TINH_TP", nullable = false)
    private String tinhTp;

    @Column(name = "LA_MAC_DINH", columnDefinition = "NUMBER(1,0) DEFAULT 0")
    private boolean laMacDinh = false;

    public String getFullAddress() {
        String qh = (tenQuanHuyen != null) ? tenQuanHuyen : "[Đang xử lý...]";
        return soNhaDuong + (phuongXa != null && !phuongXa.isEmpty() ? ", " + phuongXa : "") + ", " + qh + ", " + tinhTp;
    }
}