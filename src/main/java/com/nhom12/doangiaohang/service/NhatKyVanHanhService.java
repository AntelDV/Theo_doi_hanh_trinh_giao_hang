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
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class NhatKyVanHanhService {

    @Autowired private NhatKyVanHanhRepository nhatKyVanHanhRepository;
    @Autowired private CustomUserHelper userHelper;
    @Autowired private EncryptionUtil encryptionUtil;
    @Autowired private RSAUtil rsaUtil;
    
    private static final Pattern HEX_PATTERN = Pattern.compile("^[0-9A-Fa-f]+$");

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
    
    private String cleanEncryptedString(String input) {
        if (input == null) return null;
        String clean = input.trim().replaceAll("\\s+", "");
        
        if (clean.length() % 2 == 0 && HEX_PATTERN.matcher(clean).matches()) {
            try {
                byte[] bytes = new byte[clean.length() / 2];
                for (int i = 0; i < clean.length(); i += 2) {
                    bytes[i / 2] = (byte) ((Character.digit(clean.charAt(i), 16) << 4)
                                         + Character.digit(clean.charAt(i+1), 16));
                }
                return Base64.getEncoder().encodeToString(bytes);
            } catch (Exception e) {
                return clean;
            }
        }
        return clean;
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

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            TaiKhoan myAccount = userHelper.getTaiKhoanHienTai(auth);
            
            if (myAccount != null && myAccount.getVaiTro().getIdVaiTro() == 1 && myAccount.getPrivateKey() != null) {
                String myPrivateKey = encryptionUtil.decrypt(myAccount.getPrivateKey());
                
                for (NhatKyVanHanh log : logs) {
                    String moTa = log.getMoTaChiTiet();
                    if (moTa != null && moTa.length() > 20) { 
                        try {
                            String cleaned = cleanEncryptedString(moTa);
                            String decrypted = rsaUtil.decrypt(cleaned, myPrivateKey);
                            
                            if (!decrypted.equals(cleaned) && !decrypted.equals(moTa)) {
                                 log.setMoTaChiTiet(decrypted);
                            }
                        } catch (Exception ex) {
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
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