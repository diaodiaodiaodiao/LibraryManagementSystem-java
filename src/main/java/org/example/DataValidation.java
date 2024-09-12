package org.example;

import java.util.regex.Pattern;

public class DataValidation {

    // 验证该值是否大于 0
    public static boolean isPositiveNumber(double value) {
        return value > 0;
    }

    // 验证性别
    public static boolean isValidGender(String gender) {
        return "男".equals(gender) || "女".equals(gender);
    }

    // 使用正则表达式验证电话号码
    public static boolean isValidTelephone(String telephone) {
        // 适用于7位或8位固定电话号码
        String regex = "^\\d{7,8}$";
        return Pattern.matches(regex, telephone);
    }
}
