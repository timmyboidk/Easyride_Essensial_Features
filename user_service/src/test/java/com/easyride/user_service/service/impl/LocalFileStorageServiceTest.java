package com.easyride.user_service.service.impl;

import com.easyride.user_service.exception.FileStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class LocalFileStorageServiceTest {

    private LocalFileStorageServiceImpl fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new LocalFileStorageServiceImpl();
    }

    @Test
    void storeFile_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "hello world".getBytes());
        String filePath = fileStorageService.storeFile(file);

        assertNotNull(filePath);
        Path path = Path.of(filePath);
        assertTrue(Files.exists(path));
        assertEquals("hello world", Files.readString(path));

        Files.deleteIfExists(path);
    }

    @Test
    void storeFile_InvalidPath() {
        MockMultipartFile file = new MockMultipartFile("file", "../test.txt", "text/plain", "hello world".getBytes());

        assertThrows(FileStorageException.class, () -> {
            fileStorageService.storeFile(file);
        });
    }
}
