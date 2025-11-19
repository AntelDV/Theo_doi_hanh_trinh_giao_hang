package com.nhom12.doangiaohang.utils;

import org.springframework.stereotype.Component;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class RSAUtil {

    private static final String ALGORITHM = "RSA";
    private static final int KEY_SIZE = 2048; 

    // 1. Sinh cặp khóa RSA (Public + Private)
    public KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
            keyGen.initialize(KEY_SIZE);
            return keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Lỗi khởi tạo RSA: " + e.getMessage());
        }
    }

    // Chuyển Key sang chuỗi Base64 để lưu DB
    public String keyToString(Key key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    // Khôi phục Public Key từ chuỗi Base64
    public PublicKey stringToPublicKey(String keyStr) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(keyStr);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        return keyFactory.generatePublic(spec);
    }

    // Khôi phục Private Key từ chuỗi Base64
    public PrivateKey stringToPrivateKey(String keyStr) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(keyStr);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        return keyFactory.generatePrivate(spec);
    }

    // 2. Mã hóa (Encrypt) bằng Public Key
    public String encrypt(String data, String publicKeyStr) {
        try {
            PublicKey publicKey = stringToPublicKey(publicKeyStr);
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(ALGORITHM);
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedBytes = cipher.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi mã hóa RSA: " + e.getMessage());
        }
    }

    // 3. Giải mã (Decrypt) bằng Private Key
    public String decrypt(String encryptedData, String privateKeyStr) {
        try {
            PrivateKey privateKey = stringToPrivateKey(privateKeyStr);
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(ALGORITHM);
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, privateKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
            return new String(decryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi giải mã RSA: " + e.getMessage());
        }
    }

    // 4. Tạo Chữ ký số (Sign) bằng Private Key
    // Dùng thuật toán SHA256withRSA
    public String sign(String data, String privateKeyStr) {
        try {
            PrivateKey privateKey = stringToPrivateKey(privateKeyStr);
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(data.getBytes());
            byte[] signatureBytes = signature.sign();
            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo chữ ký số: " + e.getMessage());
        }
    }

    // 5. Xác thực Chữ ký số (Verify) bằng Public Key
    public boolean verify(String data, String signatureStr, String publicKeyStr) {
        try {
            PublicKey publicKey = stringToPublicKey(publicKeyStr);
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(data.getBytes());
            byte[] signatureBytes = Base64.getDecoder().decode(signatureStr);
            return signature.verify(signatureBytes);
        } catch (Exception e) {
            System.err.println("Lỗi xác thực chữ ký: " + e.getMessage());
            return false;
        }
    }
}