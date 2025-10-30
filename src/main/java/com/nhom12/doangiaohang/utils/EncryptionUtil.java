package com.nhom12.doangiaohang.utils;

import jakarta.annotation.PostConstruct; 
import org.slf4j.Logger; 
import org.slf4j.LoggerFactory; 
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component; // KÍCH HOẠT LẠI

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec; 
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom; 
import java.util.Base64;

/**
 * Lớp tiện ích mã hóa AES-256 (Mã hóa mức ứng dụng).
 * Dùng để mã hóa PII (Họ tên, Email, SĐT).
 * Sử dụng khóa từ application.properties.
 */
@Component // SỬA LỖI: KÍCH HOẠT LẠI COMPONENT NÀY
public class EncryptionUtil {

    private static final Logger logger = LoggerFactory.getLogger(EncryptionUtil.class);
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding"; 
    private static final int IV_SIZE = 16; // 16 bytes IV for CBC

    // Đọc khóa Base64 từ application.properties
    @Value("${app.encryption.key}")
    private String base64Key;

    private SecretKeySpec secretKey;
    private SecureRandom secureRandom; 

    /**
     * Khởi tạo SecretKeySpec từ khóa Base64 khi Spring Boot khởi động.
     */
    @PostConstruct
    private void init() {
        try {
            byte[] decodedKey = Base64.getDecoder().decode(base64Key);
            if (decodedKey.length != 32) { // AES-256 yêu cầu khóa 32 byte
                 logger.error("Invalid AES key length. Expected 32 bytes for AES-256, but got {}.", decodedKey.length);
                 throw new IllegalArgumentException("Invalid AES key length. Expected 32 bytes for AES-256.");
            }
            this.secretKey = new SecretKeySpec(decodedKey, ALGORITHM);
            this.secureRandom = new SecureRandom();
             logger.info("EncryptionUtil initialized successfully with AES-256 key.");
        } catch (IllegalArgumentException e) {
             logger.error("Error decoding Base64 key: {}. Key not found or invalid.", e.getMessage());
             // Lỗi này sẽ xảy ra nếu bạn quên thêm 'app.encryption.key' vào properties
             throw new RuntimeException("Failed to initialize EncryptionUtil. Did you set 'app.encryption.key' in properties?", e);
        }
    }

    /**
     * Mã hóa dữ liệu (String) sử dụng AES-256-CBC.
     * @param dataToEncrypt Dữ liệu (ví dụ: "Nguyễn Văn A")
     * @return Chuỗi Base64(IV + EncryptedData)
     */
    public String encrypt(String dataToEncrypt) {
        if (dataToEncrypt == null || dataToEncrypt.isEmpty()) return dataToEncrypt;
        try {
            // 1. Tạo Initialization Vector (IV) ngẫu nhiên
            byte[] iv = new byte[IV_SIZE];
            secureRandom.nextBytes(iv);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            // 2. Mã hóa
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
            byte[] encryptedBytes = cipher.doFinal(dataToEncrypt.getBytes(StandardCharsets.UTF_8));

            // 3. Kết hợp IV (16 bytes) và Dữ liệu đã mã hóa
            byte[] combinedPayload = new byte[iv.length + encryptedBytes.length];
            System.arraycopy(iv, 0, combinedPayload, 0, iv.length); // IV đầu tiên
            System.arraycopy(encryptedBytes, 0, combinedPayload, iv.length, encryptedBytes.length); // Data theo sau

            // 4. Trả về dạng Base64 để lưu vào CSDL (cột VARCHAR2)
            return Base64.getEncoder().encodeToString(combinedPayload);
        } catch (Exception e) {
            logger.error("Error encrypting data: {}", e.getMessage(), e);
            return "[Error Encrypting]"; 
        }
    }

    /**
     * Giải mã dữ liệu (Base64) đã được mã hóa bằng AES-256-CBC.
     * @param dataToDecrypt Chuỗi Base64(IV + EncryptedData)
     * @return Dữ liệu gốc (String)
     */
    public String decrypt(String dataToDecrypt) {
         if (dataToDecrypt == null || dataToDecrypt.isEmpty()) return dataToDecrypt;
        try {
            // 1. Giải mã Base64
            byte[] combinedPayload = Base64.getDecoder().decode(dataToDecrypt);

            if (combinedPayload.length < IV_SIZE) {
                logger.warn("Error decrypting data: Input data too short to contain IV. Returning as-is.");
                return dataToDecrypt; // Trả về như cũ nếu không phải dữ liệu mã hóa
            }

            // 2. Tách IV (16 bytes đầu tiên)
            byte[] iv = new byte[IV_SIZE];
            System.arraycopy(combinedPayload, 0, iv, 0, iv.length);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            // 3. Tách dữ liệu mã hóa (phần còn lại)
            int encryptedDataLength = combinedPayload.length - IV_SIZE;
            byte[] encryptedBytes = new byte[encryptedDataLength];
            System.arraycopy(combinedPayload, IV_SIZE, encryptedBytes, 0, encryptedDataLength);

            // 4. Giải mã
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
             // Lỗi này xảy ra nếu dữ liệu là plaintext (chưa mã hóa) hoặc sai khóa
             logger.warn("Warn decrypting data (maybe plaintext?): {}. Returning as-is.", e.getMessage());
             return dataToDecrypt; // Trả về dữ liệu gốc
        }
    }
}