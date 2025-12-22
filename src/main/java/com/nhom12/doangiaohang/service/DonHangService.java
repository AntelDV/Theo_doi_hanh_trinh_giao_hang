package com.nhom12.doangiaohang.service;

import com.nhom12.doangiaohang.model.*;
import com.nhom12.doangiaohang.repository.*;
import com.nhom12.doangiaohang.utils.EncryptionUtil;
import com.nhom12.doangiaohang.utils.RSAUtil;
import com.nhom12.doangiaohang.service.HybridEncryptionService.HybridResult;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class DonHangService {

    @Autowired private DonHangRepository donHangRepository;
    @Autowired private HanhTrinhDonHangRepository hanhTrinhDonHangRepository;
    @Autowired private TrangThaiDonHangRepository trangThaiDonHangRepository;
    @Autowired private NhanVienRepository nhanVienRepository;
    @Autowired private DiaChiRepository diaChiRepository;
    @Autowired private ThanhToanRepository thanhToanRepository;
    @Autowired private TaiKhoanRepository taiKhoanRepository;
    @Autowired private CustomUserHelper userHelper;
    @Autowired private EncryptionUtil encryptionUtil;
    @Autowired private RSAUtil rsaUtil;
    @Autowired private HybridEncryptionService hybridService; 
    
    @PersistenceContext private EntityManager entityManager;

    private static final Pattern HEX_PATTERN = Pattern.compile("^[0-9A-Fa-f]+$");

    private String getMyPrivateKey(Authentication authentication) {
        TaiKhoan tk = userHelper.getTaiKhoanHienTai(authentication);
        if (tk == null || tk.getPrivateKey() == null) {
            throw new RuntimeException("Error No Private Key");
        }
        return encryptionUtil.decrypt(tk.getPrivateKey());
    }

    private String normalizeEncryptedString(String input) {
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

    private void decryptDonHangPII(DonHang donHang) {
        if (donHang != null) {
            Hibernate.initialize(donHang.getHanhTrinh());
            Hibernate.initialize(donHang.getThanhToan());
            Hibernate.initialize(donHang.getDiaChiLayHang());
            Hibernate.initialize(donHang.getKhachHangGui()); 

            entityManager.detach(donHang);
            if (donHang.getDiaChiLayHang() != null) entityManager.detach(donHang.getDiaChiLayHang());
            if (donHang.getThanhToan() != null) entityManager.detach(donHang.getThanhToan());
            if (donHang.getKhachHangGui() != null) entityManager.detach(donHang.getKhachHangGui());

            try {
                if (donHang.getTenNguoiNhan() != null) {
                    try { 
                        donHang.setTenNguoiNhan(encryptionUtil.decrypt(normalizeEncryptedString(donHang.getTenNguoiNhan()))); 
                    } catch (Exception e) {}
                }
                if (donHang.getDiaChiGiaoHang() != null) {
                    try { 
                        donHang.setDiaChiGiaoHang(encryptionUtil.decrypt(normalizeEncryptedString(donHang.getDiaChiGiaoHang()))); 
                    } catch (Exception e) {}
                }
                
                if (donHang.getDiaChiLayHang() != null && donHang.getDiaChiLayHang().getQuanHuyen() != null) {
                    try {
                        String rawQuan = donHang.getDiaChiLayHang().getQuanHuyen();
                        String normalized = normalizeEncryptedString(rawQuan);
                        String decryptedQuan = encryptionUtil.decrypt(normalized);
                        
                        if (!decryptedQuan.equals(normalized) && !decryptedQuan.equals(rawQuan)) {
                            donHang.getDiaChiLayHang().setQuanHuyen(decryptedQuan);
                        }
                    } catch (Exception e) { }
                }

                if (donHang.getKhachHangGui() != null && donHang.getKhachHangGui().getHoTen() != null) {
                    try {
                         String normalized = normalizeEncryptedString(donHang.getKhachHangGui().getHoTen());
                         donHang.getKhachHangGui().setHoTen(encryptionUtil.decrypt(normalized));
                    } catch (Exception e) {  }
                }

                if (donHang.getHanhTrinh() != null) {
                    for (HanhTrinhDonHang ht : donHang.getHanhTrinh()) {
                        Hibernate.initialize(ht.getNhanVienThucHien());
                        entityManager.detach(ht);
                        if (ht.getNhanVienThucHien() != null && ht.getNhanVienThucHien().getHoTen() != null) {
                            entityManager.detach(ht.getNhanVienThucHien());
                            try {
                                String normalized = normalizeEncryptedString(ht.getNhanVienThucHien().getHoTen());
                                ht.getNhanVienThucHien().setHoTen(encryptionUtil.decrypt(normalized));
                            } catch (Exception e) {  }
                        }
                    }
                }
            } catch (Exception e) { 
                System.err.println(e.getMessage());
            }
        }
    }

    private TaiKhoan findSystemAdmin() {
        return taiKhoanRepository.findAll().stream()
                .filter(t -> t.getVaiTro().getIdVaiTro() == 1)
                .findFirst().orElse(null);
    }

    @Transactional
    public DonHang taoDonHangMoi(DonHang donHang, Integer idDiaChiLayHang, Authentication authentication) {
        KhachHang khachHangGui = userHelper.getKhachHangHienTai(authentication);
        donHang.setKhachHangGui(khachHangGui);
        
        DiaChi diaChiLay = diaChiRepository.findById(idDiaChiLayHang)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Address ID"));
        donHang.setDiaChiLayHang(diaChiLay);
        
        donHang.setMaVanDon("DH" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        donHang.setNgayTao(new Date());

        String tenGoc = donHang.getTenNguoiNhan();
        String diaChiGoc = donHang.getDiaChiGiaoHang();
        String moTaHangHoaGoc = donHang.getMoTaHangHoa();

        donHang.setTenNguoiNhan(encryptionUtil.encrypt(tenGoc));
        donHang.setDiaChiGiaoHang(encryptionUtil.encrypt(diaChiGoc));

        if (donHang.getMoTaHangHoa() != null && !donHang.getMoTaHangHoa().trim().isEmpty()) {
            TaiKhoan admin = findSystemAdmin();
            if (admin != null && admin.getPublicKey() != null) {
                HybridResult res = hybridService.encrypt(donHang.getMoTaHangHoa(), admin.getPublicKey());
                donHang.setMoTaHangHoa(res.encryptedData);
                donHang.setMaKhoaHangHoa(res.encryptedSessionKey);
            }
        } else {
            donHang.setMoTaHangHoa(null);
            donHang.setMaKhoaHangHoa(null);
        }

        try {
            String dataToSign = "CreateOrder|" + donHang.getMaVanDon();
            String signature = rsaUtil.sign(dataToSign, getMyPrivateKey(authentication));
            donHang.setChKyKhachHang(signature);
        } catch (Exception e) { 
            throw new RuntimeException(e.getMessage());
        }

        if (donHang.getThanhToan() == null) { 
            ThanhToan tt = new ThanhToan();
            tt.setDonHang(donHang);
            tt.setTongTienCod(0.0);
            donHang.setThanhToan(tt);
        } else {
            donHang.getThanhToan().setDonHang(donHang); 
            if(donHang.getThanhToan().getTongTienCod() == null) {
                donHang.getThanhToan().setTongTienCod(0.0);
            }
        }
        
        DonHang saved = donHangRepository.save(donHang);
        
        HanhTrinhDonHang ht = new HanhTrinhDonHang();
        ht.setDonHang(saved);
        ht.setTrangThai(trangThaiDonHangRepository.findById(1).orElseThrow());
        ht.setThoiGianCapNhat(new Date());
        hanhTrinhDonHangRepository.save(ht);

        saved.setTenNguoiNhan(tenGoc);
        saved.setDiaChiGiaoHang(diaChiGoc);
        saved.setMoTaHangHoa(moTaHangHoaGoc);

        return saved;
    }

    @Transactional
    public void capNhatTrangThai(Integer idDonHang, Integer idTrangThaiMoi, String ghiChu, boolean daThanhToanCod, Authentication authentication) {
        NhanVien shipper = userHelper.getNhanVienHienTai(authentication);
        DonHang donHang = donHangRepository.findById(idDonHang)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        
        TrangThaiDonHang ttHienTai = donHang.getTrangThaiHienTai();
        int idHienTai = ttHienTai.getIdTrangThai();

        if (idHienTai != 1 && idHienTai != 7) { 
             NhanVien shipperDangGiu = donHang.getShipperHienTai();
             if (shipperDangGiu != null && !shipperDangGiu.getId().equals(shipper.getId())) {
                 throw new SecurityException("Access Denied");
             }
        }

        TrangThaiDonHang trangThaiMoi = trangThaiDonHangRepository.findById(idTrangThaiMoi)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Status"));

        if (idTrangThaiMoi == 5 && donHang.getThanhToan().getTongTienCod() > 0) {
             if (!daThanhToanCod) throw new IllegalStateException("COD not collected");
             donHang.getThanhToan().setDaThanhToanCod(true);
             thanhToanRepository.save(donHang.getThanhToan());
        }

        HanhTrinhDonHang ht = new HanhTrinhDonHang();
        ht.setDonHang(donHang);
        ht.setNhanVienThucHien(shipper);
        ht.setTrangThai(trangThaiMoi);
        ht.setThoiGianCapNhat(new Date());
        
        String ghiChuFinal = (ghiChu != null) ? ghiChu : "";
        ht.setGhiChuNhanVien(ghiChuFinal);

        if ((idTrangThaiMoi == 6 || idTrangThaiMoi == 8) && ghiChuFinal.length() > 5) {
            TaiKhoan admin = findSystemAdmin();
            if (admin != null) {
                HybridResult res = hybridService.encrypt(ghiChuFinal, admin.getPublicKey());
                ht.setChiTietSuCo(res.encryptedData);
                ht.setMaKhoaSuCo(res.encryptedSessionKey);
                ht.setGhiChuNhanVien("Report sent securely.");
            }
        }

        try {
            String dataToSign = "UpdateStatus|" + idDonHang + "|" + idTrangThaiMoi + "|" + shipper.getId() + "|" + ghiChuFinal;
            String signature = rsaUtil.sign(dataToSign, getMyPrivateKey(authentication));
            ht.setChKySo(signature);
        } catch (Exception e) {
             System.err.println(e.getMessage());
        }

        hanhTrinhDonHangRepository.save(ht);
        
        if (idTrangThaiMoi == 6) {
             HanhTrinhDonHang htAuto = new HanhTrinhDonHang();
             htAuto.setDonHang(donHang);
             htAuto.setTrangThai(trangThaiDonHangRepository.findById(7).orElseThrow());
             htAuto.setGhiChuNhanVien("System: Returned to queue.");
             htAuto.setThoiGianCapNhat(new Date(System.currentTimeMillis() + 1000));
             hanhTrinhDonHangRepository.save(htAuto);
        }
    }

    public List<DonHang> getDonHangCuaShipperHienTai(Authentication auth) {
        NhanVien shipper = userHelper.getNhanVienHienTai(auth);
        List<DonHang> list = donHangRepository.findDonHangDangXuLyCuaShipper(shipper.getId());
        list.forEach(this::decryptDonHangPII);
        return list;
    }
    
    public List<DonHang> getAllDonHangForQuanLy() {
       List<DonHang> list = donHangRepository.findAllByOrderByIdDonHangDesc();
       list.forEach(this::decryptDonHangPII);
       return list;
    }
    
    public DonHang getChiTietDonHangChoQuanLy(Integer id) {
        DonHang dh = donHangRepository.findById(id).orElseThrow();
        decryptDonHangPII(dh); 
        
        TaiKhoan currentAdmin = userHelper.getTaiKhoanHienTai(SecurityContextHolder.getContext().getAuthentication());
        String adminPrivKey = null;
        if (currentAdmin != null && currentAdmin.getPrivateKey() != null) {
            try {
                adminPrivKey = encryptionUtil.decrypt(currentAdmin.getPrivateKey());
            } catch (Exception e) { }
        }
        
        if (adminPrivKey != null) {
            if (dh.getMoTaHangHoa() != null && dh.getMaKhoaHangHoa() != null) {
                try {
                    String moTaDecrypted = hybridService.decrypt(dh.getMoTaHangHoa(), dh.getMaKhoaHangHoa(), adminPrivKey);
                    dh.setMoTaHangHoa(moTaDecrypted);
                } catch (Exception e) {
                    dh.setMoTaHangHoa("[Decryption Failed]");
                }
            }
            if (dh.getHanhTrinh() != null) {
                for (HanhTrinhDonHang ht : dh.getHanhTrinh()) {
                    if (ht.getChiTietSuCo() != null && ht.getMaKhoaSuCo() != null) {
                        try {
                            String suCoDecrypted = hybridService.decrypt(ht.getChiTietSuCo(), ht.getMaKhoaSuCo(), adminPrivKey);
                            ht.setChiTietSuCo(suCoDecrypted);
                        } catch (Exception e) {
                            ht.setChiTietSuCo("[Decryption Failed]");
                        }
                    }
                }
            }
        }
        return dh;
    }
    
    public List<DonHang> getDonHangCuaKhachHangHienTai(Authentication auth) {
        KhachHang kh = userHelper.getKhachHangHienTai(auth);
        List<DonHang> list = donHangRepository.findByKhachHangGui_IdOrderByIdDonHangDesc(kh.getId());
        list.forEach(this::decryptDonHangPII);
        return list;
    }
    
    public DonHang getChiTietDonHangCuaKhachHang(String ma, Authentication auth) {
        DonHang dh = donHangRepository.findByMaVanDon(ma).orElseThrow();
        decryptDonHangPII(dh);
        return dh;
    }
    
    public DonHang getDonHangByMaVanDon(String ma) {
        DonHang dh = donHangRepository.findByMaVanDon(ma)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        decryptDonHangPII(dh); 
        return dh;
    }

    @Transactional
    public void phanCongShipper(Integer idDon, Integer idShip, Authentication auth) {
        DonHang dh = donHangRepository.findById(idDon).orElseThrow();
        NhanVien ship = nhanVienRepository.findById(idShip).orElseThrow();
        
        entityManager.detach(ship); 
        String tenShipperRo = ship.getHoTen(); 
        try {
            tenShipperRo = encryptionUtil.decrypt(ship.getHoTen());
        } catch (Exception e) { }
        
        HanhTrinhDonHang ht = new HanhTrinhDonHang();
        ht.setDonHang(dh); 
        ht.setNhanVienThucHien(ship);
        ht.setTrangThai(trangThaiDonHangRepository.findById(1).orElseThrow()); 
        
        ht.setGhiChuNhanVien("Admin assigned to Shipper: " + tenShipperRo);
        ht.setThoiGianCapNhat(new Date());
        
        hanhTrinhDonHangRepository.save(ht);
    }
    @Transactional
    public void hoanKhoDonHang(Integer idDon, Authentication auth) {
        DonHang dh = donHangRepository.findById(idDon).orElseThrow();
        HanhTrinhDonHang ht = new HanhTrinhDonHang();
        ht.setDonHang(dh); 
        ht.setTrangThai(trangThaiDonHangRepository.findById(9).orElseThrow()); 
        ht.setGhiChuNhanVien("Admin confirmed return to warehouse.");
        ht.setThoiGianCapNhat(new Date());
        hanhTrinhDonHangRepository.save(ht);
    }

    @Transactional
    public void huyDonHang(Integer idDonHang, Authentication authentication) {
        KhachHang kh = userHelper.getKhachHangHienTai(authentication);
        DonHang donHang = donHangRepository.findById(idDonHang)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (!donHang.getKhachHangGui().getId().equals(kh.getId())) {
            throw new SecurityException("Access Denied");
        }

        if (donHang.getTrangThaiHienTai().getIdTrangThai() != 1) {
            throw new IllegalStateException("Cannot cancel processed order");
        }

        HanhTrinhDonHang ht = new HanhTrinhDonHang();
        ht.setDonHang(donHang);
        ht.setTrangThai(trangThaiDonHangRepository.findById(10)
                .orElseThrow(() -> new IllegalArgumentException("System Error"))); 
        ht.setGhiChuNhanVien("Customer cancelled order.");
        ht.setThoiGianCapNhat(new Date());

        try {
            String dataToSign = "CancelOrder|" + idDonHang + "|" + kh.getId();
            String signature = rsaUtil.sign(dataToSign, getMyPrivateKey(authentication));
            ht.setChKySo(signature);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        hanhTrinhDonHangRepository.save(ht);
    }
}