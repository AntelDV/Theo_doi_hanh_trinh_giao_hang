package com.nhom12.doangiaohang.service;

import com.nhom12.doangiaohang.model.DiaChi;
import com.nhom12.doangiaohang.model.KhachHang;
import com.nhom12.doangiaohang.repository.DiaChiRepository;
import com.nhom12.doangiaohang.utils.EncryptionUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DiaChiService {

    @Autowired private DiaChiRepository diaChiRepository;
    @Autowired private CustomUserHelper userHelper; 
    @Autowired private EncryptionUtil encryptionUtil;
    
    @PersistenceContext private EntityManager entityManager;
    private void decryptDiaChi(DiaChi dc) {
        if (dc != null) {
            entityManager.detach(dc);
            try {
                dc.setSoNhaDuong(encryptionUtil.decrypt(dc.getSoNhaDuong()));
            } catch (Exception e) {
                dc.setSoNhaDuong("[Lỗi hiển thị]");
            }
        }
    }

    public List<DiaChi> getDiaChiByCurrentUser(Authentication authentication) {
        KhachHang kh = userHelper.getKhachHangHienTai(authentication);
        List<DiaChi> list = diaChiRepository.findByKhachHangSoHuu_Id(kh.getId());
        list.forEach(this::decryptDiaChi); 
        return list;
    }

    public DiaChi findById(Integer id) {
        DiaChi dc = diaChiRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy địa chỉ với ID: " + id));
        decryptDiaChi(dc);
        return dc;
    }
    
    public DiaChi findByIdAndCheckOwnership(Integer idDiaChi, Authentication authentication) {
        KhachHang kh = userHelper.getKhachHangHienTai(authentication);
        DiaChi diaChi = diaChiRepository.findById(idDiaChi).orElseThrow(); 
        if (!diaChi.getKhachHangSoHuu().getId().equals(kh.getId())) {
            throw new SecurityException("Bạn không có quyền thao tác trên địa chỉ này.");
        }
        decryptDiaChi(diaChi); 
        return diaChi;
    }
    
    @Transactional
    public void themDiaChiMoi(DiaChi diaChi, Authentication authentication) {
        KhachHang kh = userHelper.getKhachHangHienTai(authentication);
        diaChi.setKhachHangSoHuu(kh); 
        
        diaChi.setSoNhaDuong(encryptionUtil.encrypt(diaChi.getSoNhaDuong()));
        
        
        if (diaChi.isLaMacDinh()) {
            unsetDefaultOtherAddresses(authentication, null); 
        }
        
        DiaChi saved = diaChiRepository.save(diaChi);
        diaChiRepository.flush();
        entityManager.refresh(saved); 
    }
    
    @Transactional
    public void capNhatDiaChi(DiaChi diaChiForm, Authentication authentication) {
        DiaChi existingDiaChi = diaChiRepository.findById(diaChiForm.getIdDiaChi())
                 .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy địa chỉ."));
        
        KhachHang kh = userHelper.getKhachHangHienTai(authentication);
        if (!existingDiaChi.getKhachHangSoHuu().getId().equals(kh.getId())) {
             throw new SecurityException("Không có quyền.");
        }
        
        existingDiaChi.setSoNhaDuong(encryptionUtil.encrypt(diaChiForm.getSoNhaDuong()));
        
        if (diaChiForm.getQuanHuyen() != null && !diaChiForm.getQuanHuyen().equals(existingDiaChi.getQuanHuyen())) {
            existingDiaChi.setQuanHuyen(diaChiForm.getQuanHuyen());
        }
        
        existingDiaChi.setPhuongXa(diaChiForm.getPhuongXa());
        existingDiaChi.setTinhTp(diaChiForm.getTinhTp());
        
        if (diaChiForm.isLaMacDinh() && !existingDiaChi.isLaMacDinh()) {
            unsetDefaultOtherAddresses(authentication, existingDiaChi.getIdDiaChi());
            existingDiaChi.setLaMacDinh(true);
        } else if (!diaChiForm.isLaMacDinh() && existingDiaChi.isLaMacDinh()) {
             long defaultCount = diaChiRepository.findByKhachHangSoHuu_Id(kh.getId())
                     .stream().filter(DiaChi::isLaMacDinh).count();
             if (defaultCount <= 1) { 
                 throw new IllegalStateException("Bạn phải có ít nhất một địa chỉ mặc định.");
             }
            existingDiaChi.setLaMacDinh(false);
        }

        DiaChi saved = diaChiRepository.save(existingDiaChi);
        diaChiRepository.flush();
        entityManager.refresh(saved);
    }

    @Transactional
    public void xoaDiaChi(Integer idDiaChi, Authentication authentication) {
        DiaChi diaChi = diaChiRepository.findById(idDiaChi).orElseThrow();
        KhachHang kh = userHelper.getKhachHangHienTai(authentication);
        if(!diaChi.getKhachHangSoHuu().getId().equals(kh.getId())) throw new SecurityException("No Auth");

        if (diaChi.isLaMacDinh()) {
            List<DiaChi> list = diaChiRepository.findByKhachHangSoHuu_Id(kh.getId());
            if (list.size() <= 1) {
                throw new IllegalStateException("Không thể xóa địa chỉ mặc định duy nhất.");
            }
        }
        diaChiRepository.delete(diaChi);
    }
    
    @Transactional
    public void datLamMacDinh(Integer idDiaChi, Authentication authentication) {
        DiaChi diaChi = diaChiRepository.findById(idDiaChi).orElseThrow();
        if (!diaChi.isLaMacDinh()) {
            unsetDefaultOtherAddresses(authentication, idDiaChi);
            diaChi.setLaMacDinh(true);
            diaChiRepository.save(diaChi);
        }
    }
    
    private void unsetDefaultOtherAddresses(Authentication authentication, Integer excludeId) {
        KhachHang kh = userHelper.getKhachHangHienTai(authentication);
        List<DiaChi> diaChiList = diaChiRepository.findByKhachHangSoHuu_Id(kh.getId());
        for (DiaChi dc : diaChiList) {
            if (dc.isLaMacDinh() && (excludeId == null || !dc.getIdDiaChi().equals(excludeId))) {
                dc.setLaMacDinh(false);
                diaChiRepository.save(dc);
            }
        }
    }
}