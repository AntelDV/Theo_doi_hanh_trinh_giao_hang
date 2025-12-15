package com.nhom12.doangiaohang.service;

import com.nhom12.doangiaohang.model.DiaChi;
import com.nhom12.doangiaohang.model.KhachHang;
import com.nhom12.doangiaohang.repository.DiaChiRepository;
import com.nhom12.doangiaohang.utils.EncryptionUtil;
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

    private void decryptDiaChi(DiaChi dc) {
        if (dc != null) {
            try {
                // Giải mã Số nhà (App Level)
                dc.setSoNhaDuong(encryptionUtil.decrypt(dc.getSoNhaDuong()));
                // Quận huyện (DB Level) đã được @Formula tự giải mã vào tenQuanHuyen
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
        DiaChi diaChi = findById(idDiaChi); 
        if (!diaChi.getKhachHangSoHuu().getId().equals(kh.getId())) {
            throw new SecurityException("Bạn không có quyền thao tác trên địa chỉ này.");
        }
        return diaChi;
    }
    
    @Transactional
    public void themDiaChiMoi(DiaChi diaChi, Authentication authentication) {
        KhachHang kh = userHelper.getKhachHangHienTai(authentication);
        diaChi.setKhachHangSoHuu(kh); 
        
        // 1. Mã hóa App (Số nhà)
        diaChi.setSoNhaDuong(encryptionUtil.encrypt(diaChi.getSoNhaDuong()));
        
        // 2. Quận huyện: Set vào tenQuanHuyen -> Model chuyển thành quanHuyenRaw -> DB Trigger mã hóa
        
        if (diaChi.isLaMacDinh()) {
            unsetDefaultOtherAddresses(authentication, null); 
        }
        
        diaChiRepository.save(diaChi);
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
        
        // Cập nhật Quận Huyện
        existingDiaChi.setTenQuanHuyen(diaChiForm.getTenQuanHuyen());
        
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

        diaChiRepository.save(existingDiaChi);
    }

    @Transactional
    public void xoaDiaChi(Integer idDiaChi, Authentication authentication) {
        DiaChi diaChi = findByIdAndCheckOwnership(idDiaChi, authentication);
        if (diaChi.isLaMacDinh()) {
            List<DiaChi> list = getDiaChiByCurrentUser(authentication);
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