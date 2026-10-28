package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.PosShift;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * POS班次Mapper接口
 * 提供班次相关的数据库操作方法
 */
@Repository
public interface PosShiftMapper extends BaseMapper<PosShift> {

    /**
     * 根据终端ID查询当前活跃班次
     */
    @Select("SELECT * FROM pos_shifts WHERE terminal_id = #{terminalId} AND status = 'active' ORDER BY start_time DESC LIMIT 1")
    PosShift findActiveShiftByTerminalId(@Param("terminalId") String terminalId);

    /**
     * 按「当前登录人 + 终端 + active」查班次（③ 交班重写，Lead 2026-10-28）。
     *
     * <p>为什么不能只用 terminal：同 terminal 若残留两个 active 班次（历史夹具/异常关机），
     * 按「最近一个 active」关班会把**别人的班次关掉**，当前登录人仍然 open（ts-ops N4 复现 3 次）。
     * 参数全部用 #{ } 绑定，**无字符串拼接**。</p>
     */
    @Select("SELECT * FROM pos_shifts WHERE employee_id = #{employeeId} AND terminal_id = #{terminalId} "
            + "AND status = 'active' ORDER BY start_time DESC LIMIT 1")
    PosShift findActiveShiftByEmployeeAndTerminal(@Param("employeeId") String employeeId,
                                                  @Param("terminalId") String terminalId);

    /**
     * 查询终端今日所有班次
     */
    @Select("SELECT * FROM pos_shifts WHERE terminal_id = #{terminalId} AND DATE(start_time) = CURRENT_DATE ORDER BY start_time DESC")
    List<PosShift> findTodayShiftsByTerminalId(@Param("terminalId") String terminalId);

    /**
     * 统计指定时间范围内的订单数和总金额。
     *
     * <p>口径修复：原来读 {@code pos_orders}（遗留表，POS 新单从不写它），
     * 导致交班/日结永远显示 0 笔 0 元。现改读 canonical {@code orders}，
     * 以 payment_status=2（已支付）为资金口径，金额字段是「分」，由 service 转元。</p>
     */
    @Select("SELECT COUNT(*) AS total_orders, " +
            "       COALESCE(SUM(COALESCE(final_amount, total_amount)), 0) AS total_amount_cents, " +
            // ③ 交班重写（Lead 2026-10-28）：**必须扣本班次已退款金额**，否则交班把退掉的钱也算成营收。
            //    refund_amount 与 final_amount 同为「分」；已全额退款的单 order_status=5。
            "       COALESCE(SUM(COALESCE(refund_amount, 0)), 0) AS refund_cents, " +
            "       COALESCE(SUM(COALESCE(final_amount, total_amount) - COALESCE(refund_amount, 0)), 0) AS net_amount_cents " +
            "FROM orders " +
            "WHERE create_time >= #{startTime} AND create_time <= #{endTime} " +
            // 统计口径 = 时间窗 + payment_status=2，**不含 terminal 维度**（orders 无 terminal_id 列）。
            // 技术债：多终端同店场景统计会串，见 docs/audit/tech-debt-schema.md。
            "  AND payment_status = 2 AND deleted = 0 " +
            // 注意：这里**不按 terminal_id 过滤** —— orders 表没有 terminal_id 列（班次维度由时间窗界定），
            //   参数化绑定体现在 startTime/endTime；terminalId 的绑定见 findActiveShiftByEmployeeAndTerminal。
            "")
    Map<String, Object> countOrdersByTimeRange(
            @Param("terminalId") String terminalId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /** 按支付方式汇总（分）：1现金 2微信 3支付宝 4银行卡 5余额 */
    @Select("SELECT r.payment_method AS method, COALESCE(SUM(r.payment_amount), 0) AS amount_cents " +
            "FROM order_payment_records r " +
            "JOIN orders o ON o.order_id = r.order_id " +
            "WHERE o.create_time >= #{startTime} AND o.create_time <= #{endTime} " +
            "  AND o.payment_status = 2 AND o.deleted = 0 AND r.deleted = 0 " +
            "GROUP BY r.payment_method")
    List<Map<String, Object>> sumPaymentBreakdown(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /** 今日最早一笔已支付订单时间（用于无班次时补建班次的起始时间） */
    @Select("SELECT MIN(create_time) FROM orders WHERE create_time::date = CURRENT_DATE AND payment_status = 2 AND deleted = 0")
    LocalDateTime findFirstPaidOrderTimeToday();
}
