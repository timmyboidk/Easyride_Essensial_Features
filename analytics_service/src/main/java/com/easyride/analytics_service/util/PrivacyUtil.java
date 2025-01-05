package com.easyride.analytics_service.util;

import com.easyride.analytics_service.model.AnalyticsRecord;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * 隐私工具类，提供脱敏、匿名化等功能。
 */
@Slf4j
public class PrivacyUtil {

    /**
     * 通用字符串脱敏逻辑:
     * 1. 若长度 <= 2，则全打星号
     * 2. 若长度 > 2，则保留首尾，其他打星号
     */
    public static String maskString(String source) {
        if (StringUtils.isEmpty(source)) {
            return source;
        }
        int length = source.length();
        if (length <= 2) {
            return repeatStar(length);
        } else {
            return source.charAt(0) + repeatStar(length - 2) + source.charAt(length - 1);
        }
    }

    /**
     * 重复 * 号
     */
    private static String repeatStar(int count) {
        return "*".repeat(Math.max(0, count));
    }

    /**
     * 针对电话、邮箱等做更细的规则
     */
    public static String maskEmail(String email) {
        if (StringUtils.isEmpty(email) || !email.contains("@")) {
            return maskString(email);
        }
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domainPart = parts[1];
        // localPart 脱敏，如 abcd -> a**d
        return maskString(localPart) + "@" + domainPart;
    }

    /**
     * 针对 AnalyticsRecord 中的敏感字段进行脱敏
     * 可视具体业务需求来定，比如 dimensionValue 中可能存储用户ID或手机号码等
     */
    public static void anonymizeRecord(AnalyticsRecord record) {
        if (record == null) {
            return;
        }

        // 假设 dimensionKey="phoneNumber" 或 "email" 时需要脱敏
        if ("phoneNumber".equalsIgnoreCase(record.getDimensionKey())) {
            record.setDimensionValue(maskString(record.getDimensionValue()));
        } else if ("email".equalsIgnoreCase(record.getDimensionKey())) {
            record.setDimensionValue(maskEmail(record.getDimensionValue()));
        }
        // 若还有其他字段需要脱敏，可在此添加相应逻辑

        // 还可对 metricName 中若包含用户ID等敏感信息的场景处理
        // ...
    }
}
