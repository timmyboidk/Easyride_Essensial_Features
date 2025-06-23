package com.evaluation.util;

/**
 * 常量类，用于集中管理应用中的常量
 */
public class Constants {

    // HTTP 状态码
    public static final int SUCCESS_CODE = 200;
    public static final int CREATED_CODE = 201;
    public static final int BAD_REQUEST_CODE = 400;
    public static final int NOT_FOUND_CODE = 404;
    public static final int FORBIDDEN_CODE = 403;
    public static final int INTERNAL_SERVER_ERROR_CODE = 500;

    // 申诉状态
    public static final String COMPLAINT_STATUS_PENDING = "PENDING";
    public static final String COMPLAINT_STATUS_REVIEWED = "REVIEWED";
    public static final String COMPLAINT_STATUS_RESOLVED = "RESOLVED";
    public static final String COMPLAINT_STATUS_REJECTED = "REJECTED";

    // 其他常量可以在此处添加
}
