package com.studentudp.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.crypto.BadPaddingException;
import org.junit.jupiter.api.Test;

class DesCryptoUtilTest {

    private static final String VALID_KEY = "PTIT2026";
    private static final String WRONG_KEY = "WRONGKEY";

    @Test
    void encryptAndDecryptRoundTripPreservesUtf8Vietnamese() throws DesCryptoException {
        String originalName = "Nguyễn Trần Minh Ánh";
        String cipher = DesCryptoUtil.encrypt(originalName, VALID_KEY);

        assertNotEquals(originalName, cipher);
        String decrypted = DesCryptoUtil.decrypt(cipher, VALID_KEY);
        assertEquals(originalName, decrypted);
    }

    @Test
    void decryptWithWrongKeyThrowsDesCryptoExceptionSafely() throws DesCryptoException {
        String cipher = DesCryptoUtil.encrypt("B21DCCN001", VALID_KEY);

        DesCryptoException ex = assertThrows(
                DesCryptoException.class,
                () -> DesCryptoUtil.decrypt(cipher, WRONG_KEY)
        );
        assertTrue(ex.getMessage().contains("Giải mã DES thất bại"));
        assertTrue(ex.getCause() instanceof BadPaddingException);
    }

    @Test
    void invalidKeyLengthsThrowDesCryptoException() {
        assertThrows(DesCryptoException.class, () -> DesCryptoUtil.encrypt("123", null));
        assertThrows(DesCryptoException.class, () -> DesCryptoUtil.encrypt("123", ""));
        assertThrows(DesCryptoException.class, () -> DesCryptoUtil.encrypt("123", "1234567"));
        assertThrows(DesCryptoException.class, () -> DesCryptoUtil.encrypt("123", "123456789"));
        // Multi-byte UTF-8 character pushes byte length past 8
        assertThrows(DesCryptoException.class, () -> DesCryptoUtil.encrypt("123", "abcdefgá"));
    }

    @Test
    void invalidBase64CiphertextThrowsDesCryptoException() {
        DesCryptoException ex = assertThrows(
                DesCryptoException.class,
                () -> DesCryptoUtil.decrypt("not-valid-base64!!!", VALID_KEY)
        );
        assertTrue(ex.getMessage().contains("Base64"));
    }
}
