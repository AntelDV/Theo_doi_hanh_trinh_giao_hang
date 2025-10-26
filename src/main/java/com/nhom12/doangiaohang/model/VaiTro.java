package com.nhom12.doangiaohang.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@Table(name = "VAI_TRO")
@EqualsAndHashCode(of = "idVaiTro") // Dùng ID để so sánh, tránh lỗi
public class VaiTro {
    
    @Id
    @Column(name = "ID_VAI_TRO")
    // Dữ liệu này là cố định (1, 2, 3) nên không cần Sequence
    private Integer idVaiTro;

    @Column(name = "TEN_VAI_TRO", nullable = false, unique = true)
    private String tenVaiTro;
}