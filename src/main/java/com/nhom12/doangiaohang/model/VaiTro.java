package com.nhom12.doangiaohang.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@Table(name = "VAI_TRO")
@EqualsAndHashCode(of = "idVaiTro") 
public class VaiTro {
    
    @Id
    @Column(name = "ID_VAI_TRO")
    private Integer idVaiTro;

    @Column(name = "TEN_VAI_TRO", nullable = false, unique = true)
    private String tenVaiTro;
}