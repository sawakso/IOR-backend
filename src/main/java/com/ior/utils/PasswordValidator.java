package com.ior.utils;

import java.util.regex.Pattern;

/**
 * 密码强度验证工具类
 */
public class PasswordValidator {

    // 至少包含大写字母
    private static final Pattern UPPER_CASE_PATTERN = Pattern.compile(".*[A-Z].*");
    
    // 至少包含小写字母
    private static final Pattern LOWER_CASE_PATTERN = Pattern.compile(".*[a-z].*");
    
    // 至少包含数字
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*\\d.*");
    
    // 至少包含特殊字符
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");

    /**
     * 验证密码强度
     * @param password 待验证的密码
     * @return 验证结果，如果通过返回 null，否则返回错误信息
     */
    public static String validate(String password) {
        if (password == null) {
            return "密码不能为空";
        }

        // 长度检查：8-20位
        if (password.length() < 8) {
            return "密码长度不能少于8位";
        }
        if (password.length() > 20) {
            return "密码长度不能超过20位";
        }

        // 必须包含大写字母
        if (!UPPER_CASE_PATTERN.matcher(password).matches()) {
            return "密码必须包含至少一个大写字母";
        }

        // 必须包含小写字母
        if (!LOWER_CASE_PATTERN.matcher(password).matches()) {
            return "密码必须包含至少一个小写字母";
        }

        // 必须包含数字
        if (!DIGIT_PATTERN.matcher(password).matches()) {
            return "密码必须包含至少一个数字";
        }

        // 必须包含特殊字符
        if (!SPECIAL_CHAR_PATTERN.matcher(password).matches()) {
            return "密码必须包含至少一个特殊字符（如 !@#$%^&* 等）";
        }

        // 不能包含连续3个相同字符
        if (hasConsecutiveChars(password, 3)) {
            return "密码不能包含连续3个相同的字符";
        }

        // 不能是常见弱密码
        if (isCommonWeakPassword(password)) {
            return "密码过于简单，请使用更复杂的密码";
        }

        return null; // 验证通过
    }

    /**
     * 检查是否包含连续相同字符
     */
    private static boolean hasConsecutiveChars(String password, int count) {
        for (int i = 0; i <= password.length() - count; i++) {
            char current = password.charAt(i);
            boolean allSame = true;
            for (int j = 1; j < count; j++) {
                if (password.charAt(i + j) != current) {
                    allSame = false;
                    break;
                }
            }
            if (allSame) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查是否是常见弱密码
     */
    private static boolean isCommonWeakPassword(String password) {
        String lowerPassword = password.toLowerCase();
        String[] weakPasswords = {
            "password", "12345678", "qwerty12", "abc12345", 
            "admin123", "welcome1", "iloveyou", "passw0rd"
        };
        
        for (String weak : weakPasswords) {
            if (lowerPassword.equals(weak)) {
                return true;
            }
        }
        
        // 检查是否包含常见单词模式
        if (lowerPassword.contains("password") || lowerPassword.contains("admin")) {
            return true;
        }
        
        return false;
    }

    /**
     * 获取密码强度等级
     * @param password 密码
     * @return 强度等级：1-弱, 2-中等, 3-强, 4-非常强
     */
    public static int getStrengthLevel(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }

        int score = 0;

        // 长度评分
        if (password.length() >= 8) score++;
        if (password.length() >= 12) score++;
        if (password.length() >= 16) score++;

        // 字符类型评分
        if (UPPER_CASE_PATTERN.matcher(password).matches()) score++;
        if (LOWER_CASE_PATTERN.matcher(password).matches()) score++;
        if (DIGIT_PATTERN.matcher(password).matches()) score++;
        if (SPECIAL_CHAR_PATTERN.matcher(password).matches()) score++;

        // 根据得分返回等级
        if (score <= 3) return 1;      // 弱
        if (score <= 5) return 2;      // 中等
        if (score <= 6) return 3;      // 强
        return 4;                       // 非常强
    }
}
