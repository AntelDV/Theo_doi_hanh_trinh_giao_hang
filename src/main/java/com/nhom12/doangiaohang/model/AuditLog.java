package com.nhom12.doangiaohang.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime; 

@Entity
@Immutable 
@Table(name = "V_AUDIT_LOG_FULL")
public class AuditLog {

    @Id
    @Column(name = "ID")
    private Long id;

    @Column(name = "THOI_GIAN")
    private LocalDateTime thoiGian;

    @Column(name = "USER_DB")
    private String userDb;

    @Column(name = "HANH_DONG")
    private String hanhDong;

    @Column(name = "DOI_TUONG")
    private String doiTuong;

    @Column(name = "CHI_TIET")
    private String chiTiet;

    @Column(name = "LOAI_LOG") 
    private String loaiLog;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getThoiGian() {
        return thoiGian;
    }

    public void setThoiGian(LocalDateTime thoiGian) {
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

    public String getLoaiLog() {
        return loaiLog;
    }

    public void setLoaiLog(String loaiLog) {
        this.loaiLog = loaiLog;
    }
}