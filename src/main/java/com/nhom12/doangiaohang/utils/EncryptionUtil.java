package com.nhom12.doangiaohang.utils;

import jakarta.annotation.PostConstruct; 
import org.slf4j.Logger; 
import org.slf4j.LoggerFactory; 
import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Component; // Gỡ bỏ @Component

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec; 
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom; 
import java.util.Base64;

// Gỡ bỏ @Component
// @Component
public class EncryptionUtil {

    private static final Logger logger = LoggerFactory.getLogger(EncryptionUtil.class);
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding"; 
    private static final int IV_SIZE = 16; 

    @Value("${app.encryption.key}")
    private String base64Key;

    private SecretKeySpec secretKey;
    private SecureRandom secureRandom; 

    @PostConstruct
    private void init() {
        try {
            byte[] decodedKey = Base64.getDecoder().decode(base64Key);
            if (decodedKey.length != 32) {
                 logger.error("Invalid AES key length. Expected 32 bytes for AES-256, but got {}.", decodedKey.length);
                 throw new IllegalArgumentException("Invalid AES key length. Expected 32 bytes for AES-256.");
            }
            this.secretKey = new SecretKeySpec(decodedKey, ALGORITHM);
            this.secureRandom = new SecureRandom();
             logger.info("EncryptionUtil initialized successfully with AES-256 key.");
        } catch (IllegalArgumentException e) {
             logger.error("Error decoding Base64 key: {}", e.getMessage());
             throw new RuntimeException("Failed to initialize EncryptionUtil due to invalid key.", e);
        }
    }

    /**
     * Mã hóa dữ liệu sử dụng AES-256-CBC.
     */
    public String encrypt(String dataToEncrypt) {
        if (dataToEncrypt == null) return null;
        try {
            byte[] iv = new byte[IV_SIZE];
            secureRandom.nextBytes(iv);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
            byte[] encryptedBytes = cipher.doFinal(dataToEncrypt.getBytes(StandardCharsets.UTF_8));

            byte[] combinedPayload = new byte[iv.length + encryptedBytes.length];
            System.arraycopy(iv, 0, combinedPayload, 0, iv.length);
            System.arraycopy(encryptedBytes, 0, combinedPayload, iv.length, encryptedBytes.length);

            return Base64.getEncoder().encodeToString(combinedPayload);
        } catch (Exception e) {
            logger.error("Error encrypting data: {}", e.getMessage(), e);
            return null; 
        }
    }

    /**
     * Giải mã dữ liệu đã được mã hóa bằng AES-256-CBC.
     */
    public String decrypt(String dataToDecrypt) {
         if (dataToDecrypt == null) return null;
        try {
            byte[] combinedPayload = Base64.getDecoder().decode(dataToDecrypt);

            if (combinedPayload.length < IV_SIZE) {
                logger.error("Error decrypting data: Input data too short to contain IV.");
                return "[Decryption Error]";
            }

            byte[] iv = new byte[IV_SIZE];
            System.arraycopy(combinedPayload, 0, iv, 0, iv.length);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            int encryptedDataLength = combinedPayload.length - IV_SIZE;
            byte[] encryptedBytes = new byte[encryptedDataLength];
            System.arraycopy(combinedPayload, IV_SIZE, encryptedBytes, 0, encryptedDataLength);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
             logger.error("Error decrypting data: {}", e.getMessage(), e);
             return "[Decryption Error]"; 
        }
    }
}