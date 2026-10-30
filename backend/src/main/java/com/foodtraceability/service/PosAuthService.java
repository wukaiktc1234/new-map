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
     * 当前登录人 + 终端 的进行中班次（Lead 2026-10-30）。
     *
     * <p>与 {@link #getCurrentShift(String)} 的区别：**带 employee 过滤**。
     * 只有本方法的结果允许回传给前端 —— 否则会把同终端他人的 openingCash（钱箱金额）暴露出去。</p>
     */
    PosShift getMyCurrentShift(String terminalId);

    /**
     * 同终端是否存在他人未结班次（只返回存在性与脱敏姓名，绝不返回金额/shiftId/employeeId）。
     */
    java.util.Map<String, Object> terminalOpenShiftExists(String terminalId);

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
    /** ② POS「上一班」默认值：当前登录人所属门店最近一条已结束班次；无历史返回 null */
    Map<String, Object> lastEndedShiftOfMyStore();


    /**
     * 交班面板统计（Lead 2026-10-29）：**必须与 endShift 同源** ——
     * 内部复用 endShift 用的同一个 countShiftOrders + 同一套钱箱口径。
     */
    java.util.Map<String, Object> getShiftStatsForPanel(String terminalId);

    /** 交班面板异常单列表：tab=pending|processed（口径见 Lead 裁定②） */
    java.util.List<java.util.Map<String, Object>> listShiftExceptions(String terminalId, String tab);

    /** GET /v1/pos/auth/me：刷新时恢复前端能力快照（只读，权限来源 UserPermissionCache）。 */
    Map<String, Object> getCurrentUserInfo(String terminalId);
}
