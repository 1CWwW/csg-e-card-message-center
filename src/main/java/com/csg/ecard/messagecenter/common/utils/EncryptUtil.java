package com.csg.ecard.messagecenter.common.utils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * 配置密文解密工具。
 */
public final class EncryptUtil {

    private EncryptUtil() {
    }

    /**
     * 解密十六进制格式的 AES 密文。
     *
     * @param text       十六进制密文
     * @param encryptKey 密钥派生字符串
     * @return 解密后的明文
     */
    public static String aesDecryptFromHex(String text, String encryptKey) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        byte[] encryptedBytes = HexFormat.of().parseHex(text);
        return new String(aesDecrypt(encryptedBytes, encryptKey), StandardCharsets.UTF_8);
    }

    private static byte[] aesDecrypt(byte[] bytes, String encryptKey) {
        return crypt("AES", Cipher.DECRYPT_MODE, 128, bytes, encryptKey);
    }

    private static byte[] crypt(String method, int mode, int keySize, byte[] bytes, String encryptKey) {
        if (encryptKey == null || encryptKey.isEmpty()) {
            throw new IllegalArgumentException("encryptKey is null or empty");
        }
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(method);
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
            secureRandom.setSeed(encryptKey.getBytes(StandardCharsets.UTF_8));
            keyGenerator.init(keySize, secureRandom);
            SecretKey secretKey = keyGenerator.generateKey();
            SecretKeySpec key = new SecretKeySpec(secretKey.getEncoded(), method);
            Cipher cipher = Cipher.getInstance(method);
            cipher.init(mode, key);
            return cipher.doFinal(bytes);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decrypt property", ex);
        }
    }
}
