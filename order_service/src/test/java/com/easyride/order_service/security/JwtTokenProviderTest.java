package com.easyride.order_service.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String secret = "testSecretKeyForJwtTokenGenerationWhichShouldBeLongEnough";
    private final long expiration = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(secret, expiration);
    }

    @Test
    void generateToken_Success() {
        OrderDetailsImpl userDetails = new OrderDetailsImpl(1L, "testuser", "password", null);
        Authentication authentication = Mockito.mock(Authentication.class);
        Mockito.when(authentication.getPrincipal()).thenReturn(userDetails);

        String token = jwtTokenProvider.generateToken(authentication);

        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    void getUserIdFromJWT_Success() {
        OrderDetailsImpl userDetails = new OrderDetailsImpl(1L, "testuser", "password", null);
        Authentication authentication = Mockito.mock(Authentication.class);
        Mockito.when(authentication.getPrincipal()).thenReturn(userDetails);

        String token = jwtTokenProvider.generateToken(authentication);

        Long userId = jwtTokenProvider.getUserIdFromJWT(token);

        assertEquals(1L, userId);
    }

    @Test
    void validateToken_Success() {
        OrderDetailsImpl userDetails = new OrderDetailsImpl(1L, "testuser", "password", null);
        Authentication authentication = Mockito.mock(Authentication.class);
        Mockito.when(authentication.getPrincipal()).thenReturn(userDetails);

        String token = jwtTokenProvider.generateToken(authentication);

        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    void validateToken_Invalid() {
        assertFalse(jwtTokenProvider.validateToken("invalid-token"));
    }
}
