package com.evaluation.exception;

/**
 * 错误请求异常，抛出此异常表示请求参数不合法或缺失
 */
public class BadRequestException extends RuntimeException {
    /**
     * 构造函数
     *
     * @param message 异常消息
     */
    public BadRequestException(String message) {
        super(message);
    }
}
