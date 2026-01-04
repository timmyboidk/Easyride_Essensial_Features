package com.easyride.payment_service.util;

import lombok.Setter;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.Function;

@Component
public class EncryptionUtil {
    // 默认加解密函数，生产环境使用默认实现
    private static final String ALGORITHM = "AES";
    private static final String SECRET_KEY = "1234567890123456";

    // public static String encrypt(String plainText) {
    // try {
    // SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes("UTF-8"),
    // ALGORITHM);
    // Cipher cipher = Cipher.getInstance(ALGORITHM);
    // cipher.init(Cipher.ENCRYPT_MODE, keySpec);
    // byte[] encrypted = cipher.doFinal(plainText.getBytes("UTF-8"));
    // return Base64.getEncoder().encodeToString(encrypted);
    // } catch (Exception e) {
    // throw new RuntimeException("Encryption failed", e);
    // }
    // }
    //
    // public static String decrypt(String cipherText) {
    // try {
    // SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes("UTF-8"),
    // ALGORITHM);
    // Cipher cipher = Cipher.getInstance(ALGORITHM);
    // cipher.init(Cipher.DECRYPT_MODE, keySpec);
    // byte[] decoded = Base64.getDecoder().decode(cipherText);
    // byte[] decrypted = cipher.doFinal(decoded);
    // return new String(decrypted, "UTF-8");
    // } catch (Exception e) {
    // throw new RuntimeException("Decryption failed", e);
    // }
    // }
    // 允许在测试中覆盖默认的加密函数
    @Setter
    private static Function<String, String> encryptionFunction = EncryptionUtil::defaultEncrypt;
    // 允许在测试中覆盖默认的解密函数
    @Setter
    private static Function<String, String> decryptionFunction = EncryptionUtil::defaultDecrypt;

    public static String encrypt(String plainText) {
        return encryptionFunction.apply(plainText);
    }

    public static String decrypt(String cipherText) {
        return decryptionFunction.apply(cipherText);
    }

    // 默认加密实现，使用 AES 加密
    private static String defaultEncrypt(String plainText) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    // 默认解密实现，使用 AES 解密
    private static String defaultDecrypt(String cipherText) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decoded = Base64.getDecoder().decode(cipherText);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    public static void resetDefaults() {
        encryptionFunction = EncryptionUtil::defaultEncrypt;
        decryptionFunction = EncryptionUtil::defaultDecrypt;
    }
}
