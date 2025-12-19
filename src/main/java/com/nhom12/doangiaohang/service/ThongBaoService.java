package com.nhom12.doangiaohang.service;

import com.nhom12.doangiaohang.model.TaiKhoan;
import com.nhom12.doangiaohang.model.ThongBaoMat;
import com.nhom12.doangiaohang.repository.TaiKhoanRepository;
import com.nhom12.doangiaohang.repository.ThongBaoMatRepository;
import com.nhom12.doangiaohang.utils.EncryptionUtil;
import com.nhom12.doangiaohang.utils.RSAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.util.List;

@Service
public class ThongBaoService {

    @Autowired private ThongBaoMatRepository thongBaoRepository;
    @Autowired private TaiKhoanRepository taiKhoanRepository;
    @Autowired private CustomUserHelper userHelper;
    @Autowired private EncryptionUtil encryptionUtil;
    @Autowired private RSAUtil rsaUtil;
    @Autowired private NhatKyVanHanhService nhatKyService; 
    @PersistenceContext private EntityManager entityManager;
    
    public List<TaiKhoan> getDanhSachNguoiNhan(Authentication auth) {
        return taiKhoanRepository.findNguoiNhanKhaDung(auth.getName());
    }

    @Transactional
    public void guiThongBaoMat(String usernameNguoiNhan, String noiDung, Authentication auth) {
        TaiKhoan nguoiGui = userHelper.getTaiKhoanHienTai(auth);
        TaiKhoan nguoiNhan = taiKhoanRepository.findByTenDangNhap(usernameNguoiNhan)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người nhận"));

        if (nguoiNhan.getPublicKey() == null || nguoiGui.getPublicKey() == null) 
            throw new IllegalArgumentException("Chưa có Key RSA.");

        try {
            SecretKey sessionKey = encryptionUtil.generateSessionKey();
            String sessionKeyStr = encryptionUtil.keyToString(sessionKey);
            String encryptedContent = encryptionUtil.encryptAES(noiDung, sessionKey);
            String encryptedKeyForReceiver = rsaUtil.encrypt(sessionKeyStr, nguoiNhan.getPublicKey());
            String encryptedKeyForSender = rsaUtil.encrypt(sessionKeyStr, nguoiGui.getPublicKey());

            
            String labelName = (nguoiNhan.getVaiTro().getIdVaiTro() == 1) ? "CONF" : "PUB";

            String sql = "INSERT INTO THONG_BAO_MAT " +
                    "(ID_THONG_BAO, ID_NGUOI_GUI, ID_NGUOI_NHAN, NOI_DUNG_MA_HOA, MA_KHOA_PHIEN, MA_KHOA_PHIEN_GUI, NGAY_TAO) " +
                    "VALUES (THONG_BAO_MAT_SEQ.NEXTVAL, :gui, :nhan, :noidung, :khoaNhan, :khoaGui, CURRENT_TIMESTAMP)";
            
            entityManager.createNativeQuery(sql)
                    .setParameter("gui", nguoiGui.getId())
                    .setParameter("nhan", nguoiNhan.getId())
                    .setParameter("noidung", encryptedContent)
                    .setParameter("khoaNhan", encryptedKeyForReceiver)
                    .setParameter("khoaGui", encryptedKeyForSender)
                    .executeUpdate();
            
            nhatKyService.logAction(nguoiGui, "GUI_THONG_BAO_MAT", "THONG_BAO_MAT", 0, "Đã gửi tin nhắn cho " + usernameNguoiNhan);

        } catch (Exception e) {
            throw new RuntimeException("Lỗi gửi tin: " + e.getMessage());
        }
    }

    public List<ThongBaoMat> getThongBaoCuaToi(Authentication auth) {
        TaiKhoan toi = userHelper.getTaiKhoanHienTai(auth);
        List<ThongBaoMat> list = thongBaoRepository.findByNguoiNhanNative(toi.getId());
        decryptList(list, toi, true);
        return list;
    }

    public List<ThongBaoMat> getThongBaoDaGui(Authentication auth) {
        TaiKhoan toi = userHelper.getTaiKhoanHienTai(auth);        
        List<ThongBaoMat> list = thongBaoRepository.findByNguoiGuiNative(toi.getId());
        decryptList(list, toi, false);
        return list;
    }

    private void decryptList(List<ThongBaoMat> list, TaiKhoan user, boolean isInbox) {
        if (user.getPrivateKey() == null) return;
        try {
            String myPrivKey = encryptionUtil.decrypt(user.getPrivateKey());
            for (ThongBaoMat tb : list) {
                try {
                    String encryptedKey = isInbox ? tb.getMaKhoaPhien() : tb.getMaKhoaPhienGui();
                    if (encryptedKey == null) {
                        tb.setNoiDung("[Lỗi khóa]"); continue;
                    }
                    String sessionKeyStr = rsaUtil.decrypt(encryptedKey, myPrivKey);
                    SecretKey key = encryptionUtil.stringToKey(sessionKeyStr);
                    tb.setNoiDung(encryptionUtil.decryptAES(tb.getNoiDung(), key));
                } catch (Exception e) {
                    tb.setNoiDung("[Nội dung được bảo mật]");
                }
            }
        } catch (Exception e) { }
    }
}