package com.nhom12.doangiaohang.service;

import com.nhom12.doangiaohang.model.NhanVien;
import com.nhom12.doangiaohang.repository.NhanVienRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NhanVienService {

    @Autowired
    private NhanVienRepository nhanVienRepository;

    // Lấy tất cả các nhân viên có vai trò là "Shipper"
    public List<NhanVien> getAllShippers() {
        return nhanVienRepository.findByTaiKhoan_VaiTro_TenVaiTro("Shipper");
    }
    
    // (Thêm các hàm tạo, khóa/mở khóa nhân viên sau này)
}