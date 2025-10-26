package com.nhom12.doangiaohang.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@Table(name = "TRANG_THAI_DON_HANG")
@EqualsAndHashCode(of = "idTrangThai") // Dùng ID để so sánh, tránh lỗi
public class TrangThaiDonHang {
    
    @Id
    @Column(name = "ID_TRANG_THAI")
    // Chúng ta không dùng Sequence ở đây vì dữ liệu này là cố định (1, 2, 3...)
    private Integer idTrangThai;

    @Column(name = "TEN_TRANG_THAI", nullable = false, unique = true)
    private String tenTrangThai;
}