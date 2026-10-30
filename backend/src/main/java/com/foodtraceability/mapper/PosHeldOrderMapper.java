package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.PosHeldOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * POS 挂单 Mapper（Owner 2026-10-30 P0）。
 *
 * <p>所有 CAS 更新都返回 **affected rows**，由调用方判定冲突，禁止 select-then-update。</p>
 */
@Mapper
public interface PosHeldOrderMapper extends BaseMapper<PosHeldOrder> {

    /** 定制插入：cart_data 是 PG 原生 jsonb，字符串必须显式 CAST，不能依赖隐式转换 */
    @org.apache.ibatis.annotations.Insert("INSERT INTO pos_held_orders "
            + "(order_no, employee_id, terminal_id, store_id, cart_data, total_amount_fen, "
            + " status, version, expire_at, create_time, update_time, deleted) "
            + "VALUES (#{orderNo}, #{employeeId}, #{terminalId}, #{storeId}, "
            + " CAST(#{cartData} AS jsonb), #{totalAmountFen}, #{status}, #{version}, "
            + " #{expireAt}, now(), now(), 0)")
    @org.apache.ibatis.annotations.Options(useGeneratedKeys = true, keyProperty = "id")
    int insertHold(PosHeldOrder row);

    /** 幂等：按派生单号读取已有挂单（不区分状态，用于重复请求直接返回原单） */
    @Select("SELECT * FROM pos_held_orders WHERE order_no = #{orderNo} AND deleted = 0")
    PosHeldOrder selectByOrderNo(@Param("orderNo") String orderNo);

    /** 列表：仅本人 + 本终端 + 未过期 + HOLD */
    @Select("SELECT * FROM pos_held_orders "
            + "WHERE employee_id = #{employeeId} AND terminal_id = #{terminalId} "
            + "  AND status = 'HOLD' AND expire_at > now() AND deleted = 0 "
            + "ORDER BY create_time DESC")
    List<PosHeldOrder> selectHeld(@Param("employeeId") Long employeeId,
                                  @Param("terminalId") String terminalId);

    /** CAS：作废（仅 HOLD，仅本人，仅本终端） */
    @Update("UPDATE pos_held_orders SET status = 'CANCELLED', version = version + 1, update_time = now() "
            + "WHERE id = #{id} AND status = 'HOLD' AND employee_id = #{employeeId} "
            + "  AND terminal_id = #{terminalId} AND deleted = 0")
    int casCancel(@Param("id") Long id,
                  @Param("employeeId") Long employeeId,
                  @Param("terminalId") String terminalId);

    /** CAS：结账完成 */
    @Update("UPDATE pos_held_orders SET status = 'COMPLETED', version = version + 1, update_time = now() "
            + "WHERE id = #{id} AND status = 'HOLD' AND employee_id = #{employeeId} "
            + "  AND terminal_id = #{terminalId} AND deleted = 0")
    int casComplete(@Param("id") Long id,
                    @Param("employeeId") Long employeeId,
                    @Param("terminalId") String terminalId);

    /** TTL：把过期的 HOLD 置为 EXPIRED（幂等，可重复执行） */
    @Update("UPDATE pos_held_orders SET status = 'EXPIRED', version = version + 1, update_time = now() "
            + "WHERE status = 'HOLD' AND expire_at IS NOT NULL AND expire_at < now() AND deleted = 0")
    int expireOverdue();
}
