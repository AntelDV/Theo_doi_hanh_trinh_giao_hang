package com.nhom12.doangiaohang.service;

import com.nhom12.doangiaohang.utils.EncryptionUtil;
import com.nhom12.doangiaohang.utils.RSAUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

@Service
public class HybridEncryptionService {

    @Autowired private EncryptionUtil encryptionUtil;
    @Autowired private RSAUtil rsaUtil;

    // Class chứa kết quả trả về (Dữ liệu + Khóa)
    public static class HybridResult {
        public String encryptedData;      // Dữ liệu đã mã hóa AES
        public String encryptedSessionKey; // Khóa AES đã mã hóa RSA
        
        public HybridResult(String d, String k) {
            this.encryptedData = d;
            this.encryptedSessionKey = k;
        }
    }

    /**
     * MÃ HÓA LAI:
     * Input: Dữ liệu gốc + Public Key người nhận
     * Output: Dữ liệu mã hóa + Khóa phiên mã hóa
     */
    public HybridResult encrypt(String originalData, String receiverPublicKey) {
        try {
            // 1. Sinh khóa phiên ngẫu nhiên (AES)
            SecretKey sessionKey = encryptionUtil.generateSessionKey();
            
            // 2. Dùng khóa phiên mã hóa dữ liệu lớn
            String encryptedData = encryptionUtil.encryptAES(originalData, sessionKey);
            
            // 3. Dùng Public Key (RSA) mã hóa khóa phiên
            String sessionKeyStr = encryptionUtil.keyToString(sessionKey);
            String encryptedSessionKey = rsaUtil.encrypt(sessionKeyStr, receiverPublicKey);
            
            return new HybridResult(encryptedData, encryptedSessionKey);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi quy trình Mã hóa Lai: " + e.getMessage());
        }
    }

    /**
     * GIẢI MÃ LAI:
     * Input: Dữ liệu mã hóa + Khóa phiên mã hóa + Private Key của mình
     * Output: Dữ liệu gốc
     */
    public String decrypt(String encryptedData, String encryptedSessionKey, String myPrivateKey) {
        try {
            if (encryptedData == null || encryptedSessionKey == null) return null;

            // 1. Dùng Private Key (RSA) giải mã lấy lại Khóa phiên
            String sessionKeyStr = rsaUtil.decrypt(encryptedSessionKey, myPrivateKey);
            SecretKey sessionKey = encryptionUtil.stringToKey(sessionKeyStr);
            
            // 2. Dùng Khóa phiên (AES) giải mã dữ liệu gốc
            return encryptionUtil.decryptAES(encryptedData, sessionKey);
        } catch (Exception e) {
            System.err.println("Lỗi giải mã lai: " + e.getMessage());
            return "[Nội dung được bảo mật - Không thể giải mã]";
        }
    }
}