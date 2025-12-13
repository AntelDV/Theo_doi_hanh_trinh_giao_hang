package com.nhom12.doangiaohang.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;

@Data
@Entity
@Table(name = "THONG_BAO_MAT")
public class ThongBaoMat {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tb_seq")
    @SequenceGenerator(name = "tb_seq", sequenceName = "THONG_BAO_MAT_SEQ", allocationSize = 1)
    @Column(name = "ID_THONG_BAO")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "ID_NGUOI_GUI", nullable = false)
    private TaiKhoan nguoiGui;

    @ManyToOne
    @JoinColumn(name = "ID_NGUOI_NHAN", nullable = false)
    private TaiKhoan nguoiNhan;

    @Column(name = "NOI_DUNG_MA_HOA", columnDefinition = "CLOB") 
    @Lob 
    private String noiDung; 
    @Column(name = "MA_KHOA_PHIEN", columnDefinition = "CLOB")
    @Lob 
    private String maKhoaPhien; 

    @Column(name = "MA_KHOA_PHIEN_GUI", columnDefinition = "CLOB")
    @Lob
    private String maKhoaPhienGui;

    @Column(name = "NGAY_TAO") 
    @Temporal(TemporalType.TIMESTAMP)
    private Date ngayTao = new Date();
}