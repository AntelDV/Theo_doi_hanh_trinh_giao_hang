package com.nhom12.doangiaohang.utils;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class EncryptionUtil {

    private static final Logger logger = LoggerFactory.getLogger(EncryptionUtil.class);
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final int IV_SIZE = 16;

    @Value("${app.encryption.key}")
    private String base64Key;

    private SecretKeySpec staticSecretKey;
    private SecureRandom secureRandom;

    @PostConstruct
    private void init() {
        try {
            byte[] decodedKey = Base64.getDecoder().decode(base64Key);
            this.staticSecretKey = new SecretKeySpec(decodedKey, ALGORITHM);
            this.secureRandom = new SecureRandom();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khởi tạo EncryptionUtil: " + e.getMessage());
        }
    }

    public String encrypt(String data) {
        return encryptAES(data, this.staticSecretKey);
    }

    public String decrypt(String data) {
        return decryptAES(data, this.staticSecretKey);
    }

    public SecretKey generateSessionKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
            keyGen.init(256); 
            return keyGen.generateKey();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi sinh khóa phiên", e);
        }
    }

    public String keyToString(SecretKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    public SecretKey stringToKey(String keyStr) {
        byte[] decodedKey = Base64.getDecoder().decode(keyStr);
        return new SecretKeySpec(decodedKey, ALGORITHM);
    }

    public String encryptAES(String data, SecretKey key) {
        if (data == null) return null;
        try {
            byte[] iv = new byte[IV_SIZE];
            secureRandom.nextBytes(iv);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, ivParameterSpec);
            byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + encryptedBytes.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            logger.error("Lỗi mã hóa AES: {}", e.getMessage());
            return "[Lỗi Mã hóa]";
        }
    }

    public String decryptAES(String encryptedData, SecretKey key) {
        if (encryptedData == null) return null;
        try {
            
            if (!encryptedData.matches("^[A-Za-z0-9+/=]+$") || encryptedData.contains(" ")) {
                return encryptedData; 
            }

            byte[] combined = Base64.getDecoder().decode(encryptedData);
            if (combined.length < IV_SIZE) return encryptedData; 

            byte[] iv = new byte[IV_SIZE];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            int len = combined.length - IV_SIZE;
            byte[] encryptedBytes = new byte[len];
            System.arraycopy(combined, IV_SIZE, encryptedBytes, 0, len);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, ivParameterSpec);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return encryptedData;
        }
    }
}