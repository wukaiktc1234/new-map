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
     * 当前登录人 + 终端 + active（getCurrentShift 的员工过滤版，Lead 2026-10-30）。
     *
     * <p>为什么必须带 employee：{@code findActiveShiftByTerminalId} 只按终端取「最近一个 active」，
     * 一旦同终端残留他人班次，就会把**别人的班次**返回给当前收银员，
     * 连带把 openingCash（钱箱金额）暴露给非本人 —— 与 X-Operator-Id 同族的越权。</p>
     */
    @Select("SELECT * FROM pos_shifts WHERE employee_id = #{employeeId} AND terminal_id = #{terminalId} "
            + "AND status = 'active' ORDER BY start_time DESC LIMIT 1")
    PosShift findMyActiveShift(@Param("employeeId") String employeeId,
                               @Param("terminalId") String terminalId);

    /**
     * 同终端是否存在**他人**未结班次（只用于存在性提示，不返回金额/工号）。
     */
    @Select("SELECT COUNT(*) FROM pos_shifts WHERE terminal_id = #{terminalId} "
            + "AND status = 'active' AND employee_id <> #{employeeId}")
    int countOthersActiveShifts(@Param("employeeId") String employeeId,
                                @Param("terminalId") String terminalId);

    /**
     * 同终端他人未结班次的姓名（脱敏用，仅取 employee_name）
     */
    @Select("SELECT employee_name FROM pos_shifts WHERE terminal_id = #{terminalId} "
            + "AND status = 'active' AND employee_id <> #{employeeId} "
            + "ORDER BY start_time DESC LIMIT 1")
    String findOtherActiveEmployeeName(@Param("employeeId") String employeeId,
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

    /**
     * 一.2 钱箱口径：统计时间窗内**现金退款**（payment_method=1）合计（分）。
     * 钱箱只认现金；微信/支付宝/银行卡退款不进钱箱。参数化绑定，无字符串拼接。
     */
    @Select("SELECT COALESCE(SUM(COALESCE(refund_amount, 0)), 0) AS cash_refund_cents " +
            "FROM orders " +
            "WHERE create_time >= #{startTime} AND create_time <= #{endTime} " +
            "  AND payment_status = 2 AND deleted = 0 AND payment_method = 1")
    Map<String, Object> sumCashRefund(@Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime);

    /**
     * 交班面板异常单列表（Lead 2026-10-29 裁定② 的口径，SQL 由 Service 传入以保持 pending/processed 同源）。
     * 注意：这里的 ${} 不用 —— SQL 是**常量拼接**（两个分支都是固定字符串，无外部输入），
     * 唯一参数 #{shiftId} 走预编译绑定。
     */
    @Select("${sql}")
    java.util.List<java.util.Map<String, Object>> selectExceptionRows(@org.apache.ibatis.annotations.Param("sql") String sql,
                                                                    @org.apache.ibatis.annotations.Param("shiftId") Long shiftId);

    /** 今日最早一笔已支付订单时间（用于无班次时补建班次的起始时间） */
    @Select("SELECT MIN(create_time) FROM orders WHERE create_time::date = CURRENT_DATE AND payment_status = 2 AND deleted = 0")
    LocalDateTime findFirstPaidOrderTimeToday();
}
