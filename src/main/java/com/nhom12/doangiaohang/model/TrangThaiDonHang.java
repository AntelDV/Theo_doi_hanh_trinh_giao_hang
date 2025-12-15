package com.nhom12.doangiaohang.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@Table(name = "TRANG_THAI_DON_HANG")
@EqualsAndHashCode(of = "idTrangThai") 
public class TrangThaiDonHang {
    
    @Id
    @Column(name = "ID_TRANG_THAI")
    private Integer idTrangThai;

    @Column(name = "TEN_TRANG_THAI", nullable = false, unique = true)
    private String tenTrangThai;
}