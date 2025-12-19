package com.nhom12.doangiaohang.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.util.Date;

@Entity
@Immutable
@Table(name = "V_AUDIT_LOG_FULL")
public class AuditLog {

    @Id
    @Column(name = "ID")
    private Long id;

    @Column(name = "THOI_GIAN")
    private Date thoiGian;

    @Column(name = "USER_DB")
    private String userDb;

    @Column(name = "HANH_DONG")
    private String hanhDong;

    @Column(name = "DOI_TUONG")
    private String doiTuong;

    @Column(name = "CHI_TIET")
    private String chiTiet;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getThoiGian() {
        return thoiGian;
    }

    public void setThoiGian(Date thoiGian) {
        this.thoiGian = thoiGian;
    }

    public String getUserDb() {
        return userDb;
    }

    public void setUserDb(String userDb) {
        this.userDb = userDb;
    }

    public String getHanhDong() {
        return hanhDong;
    }

    public void setHanhDong(String hanhDong) {
        this.hanhDong = hanhDong;
    }

    public String getDoiTuong() {
        return doiTuong;
    }

    public void setDoiTuong(String doiTuong) {
        this.doiTuong = doiTuong;
    }

    public String getChiTiet() {
        return chiTiet;
    }

    public void setChiTiet(String chiTiet) {
        this.chiTiet = chiTiet;
    }
}