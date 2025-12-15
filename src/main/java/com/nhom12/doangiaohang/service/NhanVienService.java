package com.nhom12.doangiaohang.service;

import com.nhom12.doangiaohang.model.NhanVien;
import com.nhom12.doangiaohang.repository.NhanVienRepository;
import com.nhom12.doangiaohang.utils.EncryptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort; 
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NhanVienService {

    @Autowired
    private NhanVienRepository nhanVienRepository;
    
    @Autowired 
    private EncryptionUtil encryptionUtil;

    public List<NhanVien> getAllShippers() {
        List<NhanVien> shippers = nhanVienRepository.findByTaiKhoan_VaiTro_TenVaiTro("Shipper");
        shippers.forEach(this::decryptNhanVien);
        return shippers;
    }
    
    public List<NhanVien> getAllNhanVien() {
        List<NhanVien> nhanVienList = nhanVienRepository.findAll(Sort.by(Sort.Direction.ASC, "id")); 
        nhanVienList.forEach(this::decryptNhanVien);
        return nhanVienList;
    }
    
    public NhanVien findById(Integer id) {
        NhanVien nv = nhanVienRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên với ID: " + id));
        decryptNhanVien(nv);
        return nv;
    }
    
    /**
     * GIẢI MÃ PII (CHỈ MỨC ỨNG DỤNG)
     * Lưu ý: Không giải mã Email vì DB đã dùng @Formula
     */
    private void decryptNhanVien(NhanVien nv) {
        if (nv != null) {
            try {
                // Họ tên và SĐT được mã hóa bởi Java -> Cần giải mã
                nv.setHoTen(encryptionUtil.decrypt(nv.getHoTen()));
                nv.setSoDienThoai(encryptionUtil.decrypt(nv.getSoDienThoai()));
                
                // Email được mã hóa bởi Oracle (Trigger) và giải mã bởi @Formula
                // -> KHÔNG được giải mã ở đây nữa (nếu không sẽ lỗi BadPadding)
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}