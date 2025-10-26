package com.nhom12.doangiaohang.service;

import com.nhom12.doangiaohang.model.NhatKyVanHanh;
import com.nhom12.doangiaohang.model.TaiKhoan;
import com.nhom12.doangiaohang.repository.NhatKyVanHanhRepository;
import jakarta.persistence.criteria.Predicate; 
import jakarta.servlet.http.HttpServletRequest; // Import HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort; 
import org.springframework.data.jpa.domain.Specification; 
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder; // Import RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes; // Import ServletRequestAttributes

import java.util.ArrayList; 
import java.util.Date;
import java.util.List;

@Service
public class NhatKyVanHanhService {

    @Autowired
    private NhatKyVanHanhRepository nhatKyVanHanhRepository;

    // Ghi nhật ký mới (Tự động lấy IP)
    @Transactional
    public void logAction(TaiKhoan taiKhoanThucHien, String hanhDong, String doiTuongBiAnhHuong, Integer idDoiTuong, String moTaChiTiet) {
        NhatKyVanHanh log = new NhatKyVanHanh();
        log.setTaiKhoanThucHien(taiKhoanThucHien);
        log.setHanhDong(hanhDong);
        log.setDoiTuongBiAnhHuong(doiTuongBiAnhHuong);
        log.setIdDoiTuong(idDoiTuong);
        log.setMoTaChiTiet(moTaChiTiet);
        log.setDiaChiIp(getClientIpAddress()); // Lấy IP tự động
        log.setThoiGianThucHien(new Date()); 
        
        nhatKyVanHanhRepository.save(log);
    }
    
    // Lấy nhật ký có lọc
    public List<NhatKyVanHanh> findNhatKy(String tenDangNhap, String hanhDong, Date tuNgay, Date denNgay) {
        return nhatKyVanHanhRepository.findAll(Specification.where((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (tenDangNhap != null && !tenDangNhap.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("taiKhoanThucHien").get("tenDangNhap")), "%" + tenDangNhap.toLowerCase() + "%"));
            }
            if (hanhDong != null && !hanhDong.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("hanhDong")), "%" + hanhDong.toLowerCase() + "%"));
            }
            if (tuNgay != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("thoiGianThucHien"), tuNgay));
            }
             if (denNgay != null) {
                Date endDatePlusOne = new Date(denNgay.getTime() + (1000 * 60 * 60 * 24));
                predicates.add(cb.lessThan(root.get("thoiGianThucHien"), endDatePlusOne));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        }), Sort.by(Sort.Direction.DESC, "thoiGianThucHien")); 
    }
    
    // Hàm tiện ích lấy địa chỉ IP của client
    private String getClientIpAddress() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        // Handle multiple IPs in X-Forwarded-For header
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }
        return ipAddress;
    }
}