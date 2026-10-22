package com.foodtraceability.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.finance.FinanceRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.math.BigDecimal;

/**
 * 收支流水Mapper接口
 */
@Mapper
public interface FinanceRecordMapper extends BaseMapper<FinanceRecord> {

    /**
     * 统计某订单已记录的「收入」流水条数（幂等判重）。
     *
     * <p>2026-10-23：POS 支付成功时已写一条收入（record_category=101），
     * 订单完成事件若再写一条会造成营收重复入账。事件侧写前必须先查此项。</p>
     */
    @org.apache.ibatis.annotations.Select(
            "SELECT COUNT(*) FROM finance_records WHERE order_code = #{orderCode} " +
            "AND record_type = 1 AND deleted = 0")
    int countIncomeByOrderCode(@org.apache.ibatis.annotations.Param("orderCode") String orderCode);

    /**
     * 分页查询收支记录
     * @param page 分页参数
     * @param recordNo 记录编号（模糊）
     * @param orderCode W1-EC-05: 订单编号（精确）
     * @param recordType 收支类型
     * @param recordCategory 收支类别
     * @param paymentMethod 支付方式
     * @param counterpartyName 对方名称（模糊）
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param approvalStatus 审批状态
     * @return 收支记录分页数据
     */
    IPage<FinanceRecord> selectRecordPage(Page<FinanceRecord> page,
                                          @Param("recordNo") String recordNo,
                                          @Param("orderCode") String orderCode,
                                          @Param("recordType") Integer recordType,
                                          @Param("recordCategory") Integer recordCategory,
                                          @Param("paymentMethod") Integer paymentMethod,
                                          @Param("counterpartyName") String counterpartyName,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate,
                                          @Param("approvalStatus") Integer approvalStatus);

    /**
     * 统计指定期间内的收支汇总
     * @param recordType 收支类型
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 总金额
     */
    BigDecimal sumAmountByCondition(@Param("recordType") Integer recordType,
                                    @Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);
}
