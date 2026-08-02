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
     * 分页查询收支记录
     * @param page 分页参数
     * @param recordNo 记录编号（模糊）
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
