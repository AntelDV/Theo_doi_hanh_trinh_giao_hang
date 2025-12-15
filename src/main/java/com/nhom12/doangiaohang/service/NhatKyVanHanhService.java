package com.nhom12.doangiaohang.service;

import com.nhom12.doangiaohang.model.NhatKyVanHanh;
import com.nhom12.doangiaohang.model.TaiKhoan;
import com.nhom12.doangiaohang.repository.NhatKyVanHanhRepository;
import com.nhom12.doangiaohang.utils.EncryptionUtil;
import com.nhom12.doangiaohang.utils.RSAUtil;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class NhatKyVanHanhService {

    @Autowired private NhatKyVanHanhRepository nhatKyVanHanhRepository;
    @Autowired private CustomUserHelper userHelper;
    @Autowired private EncryptionUtil encryptionUtil;
    @Autowired private RSAUtil rsaUtil;

    @Transactional
    public void logAction(TaiKhoan taiKhoanThucHien, String hanhDong, String doiTuongBiAnhHuong, Integer idDoiTuong, String moTaChiTiet) {
        NhatKyVanHanh log = new NhatKyVanHanh();
        log.setTaiKhoanThucHien(taiKhoanThucHien);
        log.setHanhDong(hanhDong);
        log.setDoiTuongBiAnhHuong(doiTuongBiAnhHuong);
        log.setIdDoiTuong(idDoiTuong);
        log.setMoTaChiTiet(moTaChiTiet);
        log.setDiaChiIp(getClientIpAddress());
        log.setThoiGianThucHien(new Date());
        nhatKyVanHanhRepository.save(log);
    }
    
    public List<NhatKyVanHanh> findNhatKy(String tenDangNhap, String hanhDong, Date tuNgay, Date denNgay) {
        List<NhatKyVanHanh> logs = nhatKyVanHanhRepository.findAll(Specification.where((root, query, cb) -> {
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

        // Giải mã RSA cho từng dòng log
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            TaiKhoan myAccount = userHelper.getTaiKhoanHienTai(auth);
            
            if (myAccount != null && myAccount.getPrivateKey() != null) {
                // Giải mã Private Key (đang được mã hóa AES)
                String myPrivateKey = encryptionUtil.decrypt(myAccount.getPrivateKey());
                
                for (NhatKyVanHanh log : logs) {
                    // Chỉ giải mã nếu log đó được mã hóa bởi Trigger 
                    // Ở đây ta thử giải mã tất cả, nếu lỗi thì giữ nguyên text gốc
                    if (log.getMoTaChiTiet() != null && log.getMoTaChiTiet().length() > 50) { 
                        String decryptedContent = rsaUtil.decrypt(log.getMoTaChiTiet(), myPrivateKey);
                        log.setMoTaChiTiet(decryptedContent);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Không thể giải mã nhật ký: " + e.getMessage());
        }

        return logs;
    }
    
    private String getClientIpAddress() {
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            String ipAddress = request.getHeader("X-Forwarded-For");
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getRemoteAddr();
            }
            if (ipAddress != null && ipAddress.contains(",")) {
                ipAddress = ipAddress.split(",")[0].trim();
            }
            return ipAddress;
        } catch (Exception e) {
            return "Unknown";
        }
    }
}