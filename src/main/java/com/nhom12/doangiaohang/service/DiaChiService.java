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
import java.util.Optional; 

@Service
public class DiaChiService {

    @Autowired private DiaChiRepository diaChiRepository;
    @Autowired private CustomUserHelper userHelper; 
    @Autowired private EncryptionUtil encryptionUtil;

    // --- HÀM TIỆN ÍCH GIẢI MÃ ---
    
    private void decryptDiaChi(DiaChi dc) {
        if (dc != null) {
            try {
                // Giải mã Số nhà đường (App Level)
                dc.setSoNhaDuong(encryptionUtil.decrypt(dc.getSoNhaDuong()));
                // Quận huyện (DB Level) đã được @Formula tự giải mã
            } catch (Exception e) {
                dc.setSoNhaDuong("[Lỗi hiển thị]");
            }
        }
    }

    // --- CÁC HÀM READ (Cần giải mã) ---

    public List<DiaChi> getDiaChiByCurrentUser(Authentication authentication) {
        KhachHang kh = userHelper.getKhachHangHienTai(authentication);
        List<DiaChi> list = diaChiRepository.findByKhachHangSoHuu_Id(kh.getId());
        list.forEach(this::decryptDiaChi); // Giải mã để hiển thị lên web
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
        DiaChi diaChi = findById(idDiaChi); // Hàm này đã gọi decrypt rồi
        if (!diaChi.getKhachHangSoHuu().getId().equals(kh.getId())) {
            throw new SecurityException("Bạn không có quyền thao tác trên địa chỉ này.");
        }
        return diaChi;
    }
    
    // --- CÁC HÀM WRITE (Cần mã hóa) ---
    
    @Transactional
    public void themDiaChiMoi(DiaChi diaChi, Authentication authentication) {
        KhachHang kh = userHelper.getKhachHangHienTai(authentication);
        diaChi.setKhachHangSoHuu(kh); 
        
        // 1. Mã hóa App (Số nhà đường)
        diaChi.setSoNhaDuong(encryptionUtil.encrypt(diaChi.getSoNhaDuong()));
        
        // 2. Mã hóa DB (Quận huyện)
        // Để nguyên, Trigger trg_encrypt_quanhuyen_diachi sẽ lo.
        
        if (diaChi.isLaMacDinh()) {
            unsetDefaultOtherAddresses(authentication, null); 
        }
        
        diaChiRepository.save(diaChi);
    }
    
    @Transactional
    public void capNhatDiaChi(DiaChi diaChiForm, Authentication authentication) {
        // Lấy địa chỉ cũ từ DB (đang ở dạng mã hóa trong DB)
        // Lưu ý: Không dùng findByIdAndCheckOwnership ở đây vì nó sẽ giải mã -> gây lỗi Hibernate update ngược
        // Ta query trực tiếp và kiểm tra quyền thủ công
        
        DiaChi existingDiaChi = diaChiRepository.findById(diaChiForm.getIdDiaChi())
                 .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy địa chỉ."));
        
        KhachHang kh = userHelper.getKhachHangHienTai(authentication);
        if (!existingDiaChi.getKhachHangSoHuu().getId().equals(kh.getId())) {
             throw new SecurityException("Không có quyền.");
        }
        
        // Cập nhật thông tin mới
        // 1. Mã hóa App: Số nhà đường
        existingDiaChi.setSoNhaDuong(encryptionUtil.encrypt(diaChiForm.getSoNhaDuong()));
        
        // 2. Mã hóa DB: Quận huyện (Gửi plaintext xuống)
        existingDiaChi.setQuanHuyen(diaChiForm.getQuanHuyen());
        
        existingDiaChi.setPhuongXa(diaChiForm.getPhuongXa());
        existingDiaChi.setTinhTp(diaChiForm.getTinhTp());
        
        if (diaChiForm.isLaMacDinh() && !existingDiaChi.isLaMacDinh()) {
            unsetDefaultOtherAddresses(authentication, existingDiaChi.getIdDiaChi());
            existingDiaChi.setLaMacDinh(true);
        } else if (!diaChiForm.isLaMacDinh() && existingDiaChi.isLaMacDinh()) {
             // Logic kiểm tra số lượng địa chỉ mặc định
             long defaultCount = diaChiRepository.findByKhachHangSoHuu_Id(kh.getId())
                     .stream().filter(DiaChi::isLaMacDinh).count();
             if (defaultCount <= 1) { 
                 throw new IllegalStateException("Bạn phải có ít nhất một địa chỉ mặc định.");
             }
            existingDiaChi.setLaMacDinh(false);
        }

        diaChiRepository.save(existingDiaChi);
    }
    
    // --- CÁC HÀM KHÁC (Giữ nguyên) ---

    @Transactional
    public void xoaDiaChi(Integer idDiaChi, Authentication authentication) {
        // Logic xóa cần cẩn thận, lấy list lên check phải giải mã hoặc check trên raw data
        // Để đơn giản, ta cứ lấy lên check quyền rồi xóa
        DiaChi diaChi = findByIdAndCheckOwnership(idDiaChi, authentication);
        
        if (diaChi.isLaMacDinh()) {
            List<DiaChi> list = getDiaChiByCurrentUser(authentication);
            if (list.size() <= 1) {
                throw new IllegalStateException("Không thể xóa địa chỉ mặc định duy nhất.");
            }
            // Tìm cái khác làm mặc định (Logic này hơi phức tạp khi đã mã hóa, tạm thời bỏ qua bước auto-assign)
        }
        
        diaChiRepository.delete(diaChi);
    }
    
    @Transactional
    public void datLamMacDinh(Integer idDiaChi, Authentication authentication) {
        DiaChi diaChi = diaChiRepository.findById(idDiaChi).orElseThrow();
        // Check quyền...
        
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