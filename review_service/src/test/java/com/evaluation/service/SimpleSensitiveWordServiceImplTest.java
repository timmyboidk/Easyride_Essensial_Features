package com.evaluation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SimpleSensitiveWordServiceImplTest {

    private SimpleSensitiveWordServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SimpleSensitiveWordServiceImpl();
        // Manually inject sensitive words since file loading might depend on resources
        Set<String> words = new HashSet<>();
        words.add("bad");
        words.add("ugly");
        ReflectionTestUtils.setField(service, "sensitiveWords", words);
    }

    @Test
    void filterContent_Clean() {
        String content = "Good morning";
        assertEquals(content, service.filterContent(content));
    }

    @Test
    void filterContent_Dirty() {
        String content = "You are bad";
        String filtered = service.filterContent(content);
        assertEquals("You are ***", filtered); // bad is 3 chars
    }

    @Test
    void containsSensitiveWords_True() {
        assertTrue(service.containsSensitiveWords("This is very ugly"));
    }

    @Test
    void containsSensitiveWords_False() {
        assertFalse(service.containsSensitiveWords("This is beautiful"));
    }
}
