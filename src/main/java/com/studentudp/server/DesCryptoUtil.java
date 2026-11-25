package com.studentudp.server;

import com.studentudp.common.ProtocolConstants;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

/**
 * Symmetric DES/ECB/PKCS5Padding + Base64 helper.
 * Never hardcodes an encryption key; caller must supply the 8-byte key from UI/runtime input.
 */
public final class DesCryptoUtil {

    private static final String ALGORITHM = "DES";
    private static final String TRANSFORMATION = "DES/ECB/PKCS5Padding";

    private DesCryptoUtil() {
    }

    /**
     * Validates that the key encodes to exactly 8 bytes in UTF-8 and builds a DES SecretKey.
     */
    public static SecretKey buildKey(String rawKey) throws DesCryptoException {
        if (rawKey == null || rawKey.isEmpty()) {
            throw new DesCryptoException("Khóa DES chưa được nhập (yêu cầu đúng 8 ký tự).");
        }
        byte[] keyBytes = rawKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != ProtocolConstants.DES_KEY_LENGTH_BYTES) {
            throw new DesCryptoException(
                    "Khóa DES phải đúng 8 bytes UTF-8 (8 ký tự ASCII). Độ dài hiện tại: "
                            + keyBytes.length + " bytes."
            );
        }
        try {
            DESKeySpec keySpec = new DESKeySpec(keyBytes);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(keySpec);
        } catch (GeneralSecurityException ex) {
            throw new DesCryptoException("Không thể khởi tạo khóa DES: " + ex.getMessage(), ex);
        }
    }

    /**
     * Encrypts plaintext using DES/ECB/PKCS5Padding and returns a Base64-encoded string.
     */
    public static String encrypt(String plainText, String rawKey) throws DesCryptoException {
        if (plainText == null) {
            throw new DesCryptoException("Dữ liệu cần mã hóa không được null.");
        }
        SecretKey key = buildKey(rawKey);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (GeneralSecurityException ex) {
            throw new DesCryptoException("Lỗi mã hóa DES: " + ex.getMessage(), ex);
        }
    }

    /**
     * Decrypts a Base64-encoded DES ciphertext. Wraps BadPaddingException / IllegalBlockSizeException
     * and invalid Base64 errors into DesCryptoException so callers handle wrong keys safely.
     */
    public static String decrypt(String cipherBase64, String rawKey) throws DesCryptoException {
        if (cipherBase64 == null || cipherBase64.isBlank()) {
            throw new DesCryptoException("Chuỗi mã hóa trống, không thể giải mã.");
        }
        SecretKey key = buildKey(rawKey);
        byte[] cipherBytes;
        try {
            cipherBytes = Base64.getDecoder().decode(cipherBase64.trim());
        } catch (IllegalArgumentException ex) {
            throw new DesCryptoException("Dữ liệu không đúng định dạng Base64: " + ex.getMessage(), ex);
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decryptedBytes = cipher.doFinal(cipherBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (BadPaddingException ex) {
            throw new DesCryptoException(
                    "Giải mã DES thất bại (BadPaddingException): Khóa DES không đúng hoặc dữ liệu bị thay đổi.",
                    ex
            );
        } catch (IllegalBlockSizeException ex) {
            throw new DesCryptoException(
                    "Giải mã DES thất bại (IllegalBlockSizeException): Kích thước khối dữ liệu không hợp lệ.",
                    ex
            );
        } catch (GeneralSecurityException ex) {
            throw new DesCryptoException("Lỗi giải mã DES: " + ex.getMessage(), ex);
        }
    }
}
