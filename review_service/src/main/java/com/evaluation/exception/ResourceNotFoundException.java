package com.evaluation.exception;

/**
 * 资源未找到异常，抛出此异常表示请求的资源不存在
 */
public class ResourceNotFoundException extends RuntimeException {
    /**
     * 构造函数
     *
     * @param message 异常消息
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
