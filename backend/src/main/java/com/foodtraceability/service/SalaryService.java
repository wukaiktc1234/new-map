package com.foodtraceability.service;

import com.foodtraceability.entity.User;

import java.math.BigDecimal;
import java.util.List;

/**
 * 薪资管理服务接口
 */
public interface SalaryService {

    /**
     * 根据状态查询用户列表
     * @param status 用户状态
     * @return 用户列表
     */
    List<User> findUsersByStatus(String status);

    /**
     * 获取薪资规则金额
     * @param rules 规则列表
     * @param ruleType 规则类型
     * @param defaultAmount 默认金额
     * @return 规则金额
     */
    BigDecimal getRuleAmount(List<?> rules, String ruleType, BigDecimal defaultAmount);

    /**
     * 计算个税
     * @param totalEarnings 总收入
     * @param insurance 社保
     * @return 个税金额
     */
    BigDecimal calculateTax(BigDecimal totalEarnings, BigDecimal insurance);

    /**
     * 生成指定年月的薪资记录
     * @param year 年份
     * @param month 月份
     * @return 生成的记录数
     */
    int generateSalary(String year, String month);
}
