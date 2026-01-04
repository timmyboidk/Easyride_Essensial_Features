package com.easyride.user_service.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {

    @Test
    void encodePassword_Success() {
        String rawPassword = "password";
        String encodedPassword = PasswordUtil.encodePassword(rawPassword);

        assertNotNull(encodedPassword);
        assertNotEquals(rawPassword, encodedPassword);
    }

    @Test
    void matches_Success() {
        String rawPassword = "password";
        String encodedPassword = PasswordUtil.encodePassword(rawPassword);

        assertTrue(PasswordUtil.matches(rawPassword, encodedPassword));
    }

    @Test
    void matches_Failure() {
        String rawPassword = "password";
        String encodedPassword = PasswordUtil.encodePassword("differentPassword");

        assertFalse(PasswordUtil.matches(rawPassword, encodedPassword));
    }
}
