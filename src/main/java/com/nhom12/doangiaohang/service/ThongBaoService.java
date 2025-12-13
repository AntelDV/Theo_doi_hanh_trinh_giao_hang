package com.nhom12.doangiaohang.service;

import com.nhom12.doangiaohang.model.TaiKhoan;
import com.nhom12.doangiaohang.model.ThongBaoMat;
import com.nhom12.doangiaohang.repository.TaiKhoanRepository;
import com.nhom12.doangiaohang.repository.ThongBaoMatRepository;
import com.nhom12.doangiaohang.service.HybridEncryptionService.HybridResult;
import com.nhom12.doangiaohang.utils.EncryptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ThongBaoService {

    @Autowired private ThongBaoMatRepository thongBaoRepository;
    @Autowired private TaiKhoanRepository taiKhoanRepository;
    @Autowired private HybridEncryptionService hybridService;
    @Autowired private CustomUserHelper userHelper;
    @Autowired private EncryptionUtil encryptionUtil;

    public List<TaiKhoan> getDanhSachNguoiNhan(Authentication auth) {
        String currentUsername = auth.getName();
        return taiKhoanRepository.findNguoiNhanKhaDung(currentUsername);
    }

    @Transactional
    public void guiThongBaoMat(String usernameNguoiNhan, String noiDung, Authentication auth) {
        TaiKhoan nguoiGui = userHelper.getTaiKhoanHienTai(auth);
        TaiKhoan nguoiNhan = taiKhoanRepository.findByTenDangNhap(usernameNguoiNhan)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người nhận: " + usernameNguoiNhan));

        if (nguoiNhan.getPublicKey() == null || nguoiNhan.getPublicKey().isEmpty()) {
            throw new IllegalArgumentException("Người nhận chưa có khóa RSA.");
        }

        HybridResult res = hybridService.encrypt(noiDung, nguoiNhan.getPublicKey());

        ThongBaoMat tb = new ThongBaoMat();
        tb.setNguoiGui(nguoiGui);
        tb.setNguoiNhan(nguoiNhan);
        tb.setNoiDung(res.encryptedData);
        tb.setMaKhoaPhien(res.encryptedSessionKey);
        
        thongBaoRepository.save(tb);
    }

    // Hộp thư đến 
    public List<ThongBaoMat> getThongBaoCuaToi(Authentication auth) {
        TaiKhoan toi = userHelper.getTaiKhoanHienTai(auth);
        List<ThongBaoMat> list = thongBaoRepository.findByNguoiNhan_IdOrderByNgayTaoDesc(toi.getId());
        
        if (toi.getPrivateKey() != null) {
            try {
                String myPrivateKey = encryptionUtil.decrypt(toi.getPrivateKey());
                for (ThongBaoMat tb : list) {
                    String content = hybridService.decrypt(tb.getNoiDung(), tb.getMaKhoaPhien(), myPrivateKey);
                    tb.setNoiDung(content);
                }
            } catch (Exception e) {}
        }
        return list;
    }

    public List<ThongBaoMat> getThongBaoDaGui(Authentication auth) {
        TaiKhoan toi = userHelper.getTaiKhoanHienTai(auth);
        return thongBaoRepository.findByNguoiGui_IdOrderByNgayTaoDesc(toi.getId());
    }
}