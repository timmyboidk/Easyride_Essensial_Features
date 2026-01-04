package com.easyride.payment_service.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class EncryptionUtilTest {

    @AfterEach
    void tearDown() {
        EncryptionUtil.resetDefaults();
    }

    @Test
    void testEncryptionDecryption_Default() {
        String plainText = "Hello World";
        String encrypted = EncryptionUtil.encrypt(plainText);
        assertNotEquals(plainText, encrypted);

        String decrypted = EncryptionUtil.decrypt(encrypted);
        assertEquals(plainText, decrypted);
    }

    @Test
    void testMocking() {
        EncryptionUtil.setEncryptionFunction(s -> "mock_" + s);
        assertEquals("mock_test", EncryptionUtil.encrypt("test"));

        // AfterEach will reset defaults
    }
}
