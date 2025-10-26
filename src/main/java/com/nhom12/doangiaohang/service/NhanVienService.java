package com.nhom12.doangiaohang.service;

import com.nhom12.doangiaohang.model.NhanVien;
import com.nhom12.doangiaohang.repository.NhanVienRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort; // Import Sort
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NhanVienService {

    @Autowired
    private NhanVienRepository nhanVienRepository;

    // Lấy tất cả shippers
    public List<NhanVien> getAllShippers() {
        return nhanVienRepository.findByTaiKhoan_VaiTro_TenVaiTro("Shipper");
    }
    
    // Lấy tất cả nhân viên (Quản lý và Shipper), sắp xếp theo ID
    public List<NhanVien> getAllNhanVien() {
        return nhanVienRepository.findAll(Sort.by(Sort.Direction.ASC, "id")); 
    }
    
    // Tìm nhân viên theo ID
    public NhanVien findById(Integer id) {
        return nhanVienRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên với ID: " + id));
    }
    
    // (TODO: Thêm hàm sửa thông tin nhân viên nếu cần)
}