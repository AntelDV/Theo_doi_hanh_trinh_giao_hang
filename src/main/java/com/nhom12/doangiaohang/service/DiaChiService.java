package com.nhom12.doangiaohang.service;

import com.nhom12.doangiaohang.model.DiaChi;
import com.nhom12.doangiaohang.model.KhachHang;
import com.nhom12.doangiaohang.repository.DiaChiRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DiaChiService {

    @Autowired private DiaChiRepository diaChiRepository;
    @Autowired private CustomUserHelper userHelper; // Dùng file tiện ích

    public List<DiaChi> getDiaChiByCurrentUser(Authentication authentication) {
        KhachHang kh = userHelper.getKhachHangHienTai(authentication);
        return diaChiRepository.findByKhachHangSoHuu_Id(kh.getId());
    }

    public DiaChi findById(Integer id) {
        return diaChiRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy địa chỉ với ID: " + id));
    }
    
    // === THÊM HÀM NÀY ĐỂ XỬ LÝ SỔ ĐỊA CHỈ ===
    @Transactional
    public void themDiaChiMoi(DiaChi diaChi, Authentication authentication) {
        KhachHang kh = userHelper.getKhachHangHienTai(authentication);
        diaChi.setKhachHangSoHuu(kh); // Gán chủ sở hữu cho địa chỉ
        
        // Logic xử lý "Đặt làm mặc định"
        if (diaChi.isLaMacDinh()) {
            // Bỏ chọn tất cả địa chỉ mặc định cũ
            List<DiaChi> diaChiList = getDiaChiByCurrentUser(authentication);
            for (DiaChi dc : diaChiList) {
                if (dc.isLaMacDinh()) {
                    dc.setLaMacDinh(false);
                    diaChiRepository.save(dc);
                }
            }
        }
        
        diaChiRepository.save(diaChi);
    }
}