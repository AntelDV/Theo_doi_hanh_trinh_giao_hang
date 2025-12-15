package com.nhom12.doangiaohang.service;

import com.nhom12.doangiaohang.model.NhanVien;
import com.nhom12.doangiaohang.repository.NhanVienRepository;
import com.nhom12.doangiaohang.utils.EncryptionUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort; 
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NhanVienService {

    @Autowired private NhanVienRepository nhanVienRepository;
    @Autowired private EncryptionUtil encryptionUtil;
    
    @PersistenceContext private EntityManager entityManager;

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
    
    private void decryptNhanVien(NhanVien nv) {
        if (nv != null) {
            Hibernate.initialize(nv.getTaiKhoan());            
            entityManager.detach(nv); 

            try {
                if (nv.getHoTen() != null && !nv.getHoTen().isEmpty()) {
                    nv.setHoTen(encryptionUtil.decrypt(nv.getHoTen()));
                }
            } catch (Exception e) {}

            try {
                if (nv.getSoDienThoai() != null && !nv.getSoDienThoai().isEmpty()) {
                    nv.setSoDienThoai(encryptionUtil.decrypt(nv.getSoDienThoai()));
                }
            } catch (Exception e) {}
        }
    }
}