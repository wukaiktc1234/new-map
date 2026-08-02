package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.Receivable;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.ReceivableQueryDTO;
import com.foodtraceability.dto.ReceivableStatisticsDTO;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 应收账款Service接口
 * 定义应收账款的核心业务逻辑
 */
public interface ReceivableService extends IService<Receivable> {

    /**
     * 创建应收账款
     * @param receivable 应收账款信息
     * @return 创建的应收账款
     */
    Receivable createReceivable(Receivable receivable);

    /**
     * 根据ID查询应收账款
     * @param id 应收账款ID
     * @return 应收账款信息
     */
    Receivable getReceivableById(Long id);

    /**
     * 分页查询应收账款
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    PageResult<Receivable> queryReceivables(ReceivableQueryDTO queryDTO);

    /**
     * 更新应收账款
     * @param receivable 应收账款信息
     * @return 更新后的应收账款
     */
    Receivable updateReceivable(Receivable receivable);

    /**
     * 收回应收账款
     * @param id 应收账款ID
     * @param amount 收回金额
     * @return 更新后的应收账款
     */
    Receivable receivePayment(Long id, BigDecimal amount);

    /**
     * 标记应收账款为逾期
     * @param id 应收账款ID
     * @return 更新后的应收账款
     */
    Receivable markAsOverdue(Long id);

    /**
     * 批量处理逾期应收账款
     * @return 处理的逾期记录数量
     */
    int batchProcessOverdueReceivables();

    /**
     * 计算逾期天数
     * @param dueDate 到期日
     * @return 逾期天数
     */
    Integer calculateOverdueDays(Date dueDate);

    /**
     * 根据订单ID创建应收账款
     * @param orderId 订单ID
     * @return 创建的应收账款
     */
    Receivable createReceivableFromOrder(Long orderId);

    /**
     * 查询应收账款统计信息
     * @param queryDTO 查询条件
     * @return 统计结果
     */
    ReceivableStatisticsDTO getReceivableStatistics(ReceivableQueryDTO queryDTO);
}
