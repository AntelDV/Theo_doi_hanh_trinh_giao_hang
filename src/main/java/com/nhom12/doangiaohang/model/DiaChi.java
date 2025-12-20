package com.nhom12.doangiaohang.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.ColumnTransformer;
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

    @NotBlank(message = "Số nhà, đường không được để trống")
    @Column(name = "SO_NHA_DUONG", nullable = false)
    private String soNhaDuong;

    @Column(name = "PHUONG_XA")
    private String phuongXa;

    
    @Column(name = "QUAN_HUYEN", nullable = false, columnDefinition = "RAW(2000)")
    @ColumnTransformer(
        read = "UTL_I18N.RAW_TO_CHAR(CSDL_NHOM12.encryption_pkg.decrypt_data(QUAN_HUYEN), 'AL32UTF8')",
        write = "UTL_I18N.STRING_TO_RAW(?, 'AL32UTF8')" 
    )
    private String quanHuyen;

    @NotBlank(message = "Tỉnh/Thành phố không được để trống")
    @Column(name = "TINH_TP", nullable = false)
    private String tinhTp;

    @Column(name = "LA_MAC_DINH", columnDefinition = "NUMBER(1,0) DEFAULT 0")
    private boolean laMacDinh = false;

    public String getFullAddress() {
        return soNhaDuong + (phuongXa != null && !phuongXa.isEmpty() ? ", " + phuongXa : "") + ", " + quanHuyen + ", " + tinhTp;
    }
}