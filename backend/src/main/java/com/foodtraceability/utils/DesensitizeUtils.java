package com.foodtraceability.utils;

import com.foodtraceability.annotation.Desensitize;
import com.foodtraceability.annotation.DesensitizeType;

/**
 * 数据脱敏工具类
 * 用于实现各种类型的脱敏处理逻辑
 */
public class DesensitizeUtils {
    
    /**
     * 手机号脱敏，保留前3位和后4位
     * @param phone 手机号
     * @return 脱敏后的手机号
     */
    public static String desensitizePhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
    }
    
    /**
     * 邮箱脱敏，保留@符号前的第一个字符和域名
     * @param email 邮箱
     * @return 脱敏后的邮箱
     */
    public static String desensitizeEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        return email.replaceAll("(\\w)\\w*(\\@\\w+\\.\\w+)", "$1****$2");
    }
    
    /**
     * 身份证号脱敏，保留前6位和后4位
     * @param idCard 身份证号
     * @return 脱敏后的身份证号
     */
    public static String desensitizeIdCard(String idCard) {
        if (idCard == null || idCard.length() < 10) {
            return idCard;
        }
        return idCard.replaceAll("(\\d{6})\\d*(\\d{4})", "$1********$2");
    }
    
    /**
     * 姓名脱敏，保留姓氏，其余用*替换
     * @param name 姓名
     * @return 脱敏后的姓名
     */
    public static String desensitizeName(String name) {
        if (name == null || name.length() < 2) {
            return name;
        }
        return name.replaceAll("(\\S)\\S*", "$1***");
    }
    
    /**
     * 地址脱敏，保留省市，其余用*替换
     * @param address 地址
     * @return 脱敏后的地址
     */
    public static String desensitizeAddress(String address) {
        if (address == null || address.length() < 5) {
            return address;
        }
        return address.replaceAll("(\\S{2,3}省|\\S{2}市)(\\S+)", "$1***");
    }
    
    /**
     * 银行卡号脱敏，保留前4位和后4位
     * @param bankCard 银行卡号
     * @return 脱敏后的银行卡号
     */
    public static String desensitizeBankCard(String bankCard) {
        if (bankCard == null || bankCard.length() < 8) {
            return bankCard;
        }
        return bankCard.replaceAll("(\\d{4})\\d*(\\d{4})", "$1********$2");
    }
    
    /**
     * 根据脱敏类型进行脱敏处理
     * @param value 原始值
     * @param type 脱敏类型
     * @return 脱敏后的值
     */
    public static String desensitizeByType(String value, DesensitizeType type) {
        if (value == null) {
            return null;
        }
        
        switch (type) {
            case PHONE:
                return desensitizePhone(value);
            case EMAIL:
                return desensitizeEmail(value);
            case ID_CARD:
                return desensitizeIdCard(value);
            case NAME:
                return desensitizeName(value);
            case ADDRESS:
                return desensitizeAddress(value);
            case BANK_CARD:
                return desensitizeBankCard(value);
            default:
                return value;
        }
    }
    
    /**
     * 自定义脱敏，根据起始位置和结束位置进行脱敏
     * @param value 原始值
     * @param start 开始位置
     * @param end 结束位置
     * @param replacement 替换字符
     * @return 脱敏后的值
     */
    public static String desensitizeCustom(String value, int start, int end, char replacement) {
        if (value == null || value.length() < start + end) {
            return value;
        }
        
        StringBuilder sb = new StringBuilder(value);
        for (int i = start; i < sb.length() - end; i++) {
            sb.setCharAt(i, replacement);
        }
        return sb.toString();
    }
}