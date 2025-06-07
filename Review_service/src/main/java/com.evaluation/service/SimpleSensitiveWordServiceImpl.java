package com.evaluation.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SimpleSensitiveWordServiceImpl implements SensitiveWordService {

    private static final Logger log = LoggerFactory.getLogger(SimpleSensitiveWordServiceImpl.class);
    private final Set<String> sensitiveWords = new HashSet<>();
    private static final String SENSITIVE_WORDS_FILE = "/sensitive-words.txt"; // In src/main/resources

    @PostConstruct
    public void loadSensitiveWords() {
        try (InputStream is = SimpleSensitiveWordServiceImpl.class.getResourceAsStream(SENSITIVE_WORDS_FILE)) {
            if (is == null) {
                log.warn("Sensitive words file not found: {}", SENSITIVE_WORDS_FILE);
                return;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                sensitiveWords.addAll(reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty() && !line.startsWith("#")) // Ignore empty lines and comments
                        .collect(Collectors.toSet()));
            }
            log.info("Loaded {} sensitive words from {}", sensitiveWords.size(), SENSITIVE_WORDS_FILE);
        } catch (Exception e) {
            log.error("Error loading sensitive words file: {}", SENSITIVE_WORDS_FILE, e);
        }
    }

    @Override
    public String filterContent(String content) {
        if (content == null || content.isEmpty() || sensitiveWords.isEmpty()) {
            return content;
        }
        String filteredContent = content;
        for (String word : sensitiveWords) {
            // Simple replacement, can be made more sophisticated (e.g., different replacement per word length)
            filteredContent = filteredContent.replaceAll("(?i)" + word, "*".repeat(word.length()));
        }
        if (!content.equals(filteredContent)) {
            log.debug("Content filtered for sensitive words.");
        }
        return filteredContent;
    }

    @Override
    public boolean containsSensitiveWords(String content) {
        if (content == null || content.isEmpty() || sensitiveWords.isEmpty()) {
            return false;
        }
        String lowerCaseContent = content.toLowerCase();
        for (String word : sensitiveWords) {
            if (lowerCaseContent.contains(word.toLowerCase())) {
                log.info("Content contains sensitive word: {}", word);
                return true;
            }
        }
        return false;
    }
}