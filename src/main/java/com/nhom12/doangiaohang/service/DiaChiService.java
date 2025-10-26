package com.nhom12.doangiaohang.service;

import com.nhom12.doangiaohang.model.DiaChi;
import com.nhom12.doangiaohang.model.KhachHang;
import com.nhom12.doangiaohang.repository.DiaChiRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional; 

@Service
public class DiaChiService {

    @Autowired private DiaChiRepository diaChiRepository;
    @Autowired private CustomUserHelper userHelper; 

    // Lấy danh sách địa chỉ của người dùng hiện tại
    public List<DiaChi> getDiaChiByCurrentUser(Authentication authentication) {
        KhachHang kh = userHelper.getKhachHangHienTai(authentication);
        return diaChiRepository.findByKhachHangSoHuu_Id(kh.getId());
    }

    // Tìm địa chỉ theo ID
    public DiaChi findById(Integer id) {
        return diaChiRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy địa chỉ với ID: " + id));
    }
    
    // Tìm địa chỉ theo ID và kiểm tra quyền sở hữu
    public DiaChi findByIdAndCheckOwnership(Integer idDiaChi, Authentication authentication) {
        KhachHang kh = userHelper.getKhachHangHienTai(authentication);
        DiaChi diaChi = findById(idDiaChi);
        if (!diaChi.getKhachHangSoHuu().getId().equals(kh.getId())) {
            throw new SecurityException("Bạn không có quyền thao tác trên địa chỉ này.");
        }
        return diaChi;
    }
    
    // Thêm địa chỉ mới
    @Transactional
    public void themDiaChiMoi(DiaChi diaChi, Authentication authentication) {
        KhachHang kh = userHelper.getKhachHangHienTai(authentication);
        diaChi.setKhachHangSoHuu(kh); 
        
        if (diaChi.isLaMacDinh()) {
            unsetDefaultOtherAddresses(authentication, null); // Bỏ mặc định các địa chỉ khác
        }
        
        diaChiRepository.save(diaChi);
    }
    
    // Cập nhật địa chỉ
    @Transactional
    public void capNhatDiaChi(DiaChi diaChiForm, Authentication authentication) {
        DiaChi existingDiaChi = findByIdAndCheckOwnership(diaChiForm.getIdDiaChi(), authentication);
        
        existingDiaChi.setSoNhaDuong(diaChiForm.getSoNhaDuong());
        existingDiaChi.setPhuongXa(diaChiForm.getPhuongXa());
        existingDiaChi.setQuanHuyen(diaChiForm.getQuanHuyen());
        existingDiaChi.setTinhTp(diaChiForm.getTinhTp());
        
        if (diaChiForm.isLaMacDinh() && !existingDiaChi.isLaMacDinh()) {
            unsetDefaultOtherAddresses(authentication, existingDiaChi.getIdDiaChi());
            existingDiaChi.setLaMacDinh(true);
        } else if (!diaChiForm.isLaMacDinh() && existingDiaChi.isLaMacDinh()) {
             List<DiaChi> diaChiList = getDiaChiByCurrentUser(authentication);
             long defaultCount = diaChiList.stream().filter(DiaChi::isLaMacDinh).count();
             if (defaultCount <= 1 && diaChiList.size() > 1) { // Chỉ kiểm tra nếu còn địa chỉ khác
                 throw new IllegalStateException("Bạn phải có ít nhất một địa chỉ mặc định.");
             }
            existingDiaChi.setLaMacDinh(false);
        }

        diaChiRepository.save(existingDiaChi);
    }
    
    // Xóa địa chỉ
    @Transactional
    public void xoaDiaChi(Integer idDiaChi, Authentication authentication) {
        DiaChi diaChi = findByIdAndCheckOwnership(idDiaChi, authentication);
        
        // Kiểm tra xem địa chỉ có đang được dùng trong đơn hàng nào không (TODO)
        
        if (diaChi.isLaMacDinh()) {
            List<DiaChi> diaChiList = getDiaChiByCurrentUser(authentication);
            if (diaChiList.size() <= 1) {
                throw new IllegalStateException("Không thể xóa địa chỉ mặc định duy nhất.");
            }
            Optional<DiaChi> newDefault = diaChiList.stream()
                .filter(dc -> !dc.getIdDiaChi().equals(idDiaChi))
                .findFirst();
            if (newDefault.isPresent()) {
                newDefault.get().setLaMacDinh(true);
                diaChiRepository.save(newDefault.get());
            } else {
                 throw new IllegalStateException("Lỗi khi tìm địa chỉ thay thế mặc định.");
            }
        }
        
        diaChiRepository.delete(diaChi);
    }
    
    // Đặt làm mặc định
    @Transactional
    public void datLamMacDinh(Integer idDiaChi, Authentication authentication) {
        DiaChi diaChi = findByIdAndCheckOwnership(idDiaChi, authentication);
        if (!diaChi.isLaMacDinh()) {
            unsetDefaultOtherAddresses(authentication, idDiaChi);
            diaChi.setLaMacDinh(true);
            diaChiRepository.save(diaChi);
        }
    }
    
    // Hàm tiện ích: Bỏ chọn mặc định các địa chỉ khác
    private void unsetDefaultOtherAddresses(Authentication authentication, Integer excludeId) {
        List<DiaChi> diaChiList = getDiaChiByCurrentUser(authentication);
        for (DiaChi dc : diaChiList) {
            if (dc.isLaMacDinh() && (excludeId == null || !dc.getIdDiaChi().equals(excludeId))) {
                dc.setLaMacDinh(false);
                diaChiRepository.save(dc);
            }
        }
    }
}