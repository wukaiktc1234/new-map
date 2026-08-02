package com.foodtraceability.service;

import com.foodtraceability.entity.PosShift;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * POS认证服务接口
 * 提供收银端的登录认证和班次管理功能
 */
public interface PosAuthService {

    /**
     * POS收银端登录
     * @param employeeId 员工ID/员工号
     * @param password 密码
     * @param terminalId 终端ID（可选）
     * @return 登录结果，包含token、用户信息、班次信息
     */
    Map<String, Object> login(String employeeId, String password, String terminalId);

    /**
     * 获取当前活跃班次
     * @param terminalId 终端ID
     * @return 当前班次信息，如果没有活跃班次返回null
     */
    PosShift getCurrentShift(String terminalId);

    /**
     * 开始新班次
     * @param terminalId 终端ID
     * @param employeeId 员工ID
     * @param employeeName 员工姓名
     * @param shiftType 班次类型（day/night/custom）
     * @param openingCash 开机现金金额
     * @return 新创建的班次对象
     */
    PosShift startShift(String terminalId, String employeeId, String employeeName,
                        String shiftType, BigDecimal openingCash);

    /**
     * 结束当前班次
     * @param terminalId 终端ID
     * @param remark 交接备注
     * @return 结束班次的统计结果（包含订单数、总金额等）
     */
    Map<String, Object> endShift(String terminalId, String remark);

    /**
     * 获取当前班次的统计摘要
     * @param terminalId 终端ID
     * @return 班次统计摘要（订单数、总金额、支付方式分布等）
     */
    Map<String, Object> getShiftSummary(String terminalId);

    /**
     * 获取今日所有班次记录
     * @param terminalId 终端ID
     * @return 今日班次列表
     */
    List<PosShift> getTodayShifts(String terminalId);
}
