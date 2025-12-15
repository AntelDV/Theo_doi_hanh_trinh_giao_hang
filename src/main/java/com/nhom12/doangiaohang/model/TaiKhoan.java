package com.nhom12.doangiaohang.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
@Data
@Entity
@Table(name = "TAI_KHOAN")
@EqualsAndHashCode(of = "id") 
@ToString(exclude = "vaiTro") 
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TaiKhoan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tai_khoan_seq")
    @SequenceGenerator(name = "tai_khoan_seq", sequenceName = "TAI_KHOAN_SEQ", allocationSize = 1)
    @Column(name = "ID_TAI_KHOAN")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER) 
    @JoinColumn(name = "ID_VAI_TRO", nullable = false)
    private VaiTro vaiTro;

    @Column(name = "TEN_DANG_NHAP", unique = true, nullable = false, length = 50)
    private String tenDangNhap;

    @Column(name = "MAT_KHAU_HASH", nullable = false)
    private String matKhau;

    @Column(name = "TRANG_THAI", columnDefinition = "NUMBER(1,0) DEFAULT 1")
    private boolean trangThai = true; 

    @Column(name = "MA_DAT_LAI_MK", length = 100)
    private String maDatLaiMk;

    @Column(name = "THOI_HAN_MA")
    @Temporal(TemporalType.DATE) 
    private Date thoiHanMa;
    
    @Column(name = "PUBLIC_KEY", columnDefinition = "CLOB")
    @Lob 
    private String publicKey;
    
    @Column(name = "PRIVATE_KEY", columnDefinition = "CLOB")
    @Lob
    private String privateKey;
    
}