package com.nhom12.doangiaohang.service;

import com.nhom12.doangiaohang.model.NhanVien;
import com.nhom12.doangiaohang.repository.NhanVienRepository;
import com.nhom12.doangiaohang.utils.EncryptionUtil; // <-- THÊM IMPORT
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort; 
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NhanVienService {

    @Autowired
    private NhanVienRepository nhanVienRepository;
    
    // THÊM AUTOWIRED ĐỂ SỬ DỤNG BỘ GIẢI MÃ
    @Autowired 
    private EncryptionUtil encryptionUtil;

    // Lấy tất cả shippers (đã giải mã)
    public List<NhanVien> getAllShippers() {
        List<NhanVien> shippers = nhanVienRepository.findByTaiKhoan_VaiTro_TenVaiTro("Shipper");
        // Giải mã PII cho Quản lý
        shippers.forEach(this::decryptNhanVien);
        return shippers;
    }
    
    /**
     * SỬA LỖI HIỂN THỊ MÃ HÓA
     * Lấy tất cả nhân viên và GIẢI MÃ PII (Họ tên, Email, SĐT)
     * để Quản lý có thể đọc được.
     */
    public List<NhanVien> getAllNhanVien() {
        List<NhanVien> nhanVienList = nhanVienRepository.findAll(Sort.by(Sort.Direction.ASC, "id")); 
        
        // Vòng lặp để giải mã PII cho từng nhân viên
        nhanVienList.forEach(this::decryptNhanVien);
        
        return nhanVienList;
    }
    
    // Tìm nhân viên theo ID (đã giải mã)
    public NhanVien findById(Integer id) {
        NhanVien nv = nhanVienRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên với ID: " + id));
        // Giải mã PII
        decryptNhanVien(nv);
        return nv;
    }
    
    /**
     * Hàm tiện ích nội bộ để giải mã PII của một đối tượng NhanVien.
     */
    private void decryptNhanVien(NhanVien nv) {
        if (nv != null) {
            nv.setHoTen(encryptionUtil.decrypt(nv.getHoTen()));
            nv.setEmail(encryptionUtil.decrypt(nv.getEmail()));
            nv.setSoDienThoai(encryptionUtil.decrypt(nv.getSoDienThoai()));
        }
    }
}