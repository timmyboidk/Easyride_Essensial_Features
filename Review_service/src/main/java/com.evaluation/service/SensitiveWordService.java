package com.evaluation.service;

public interface SensitiveWordService {
    String filterContent(String content);
    boolean containsSensitiveWords(String content);
}