package com.foodtraceability.service.impl;

import com.foodtraceability.dto.finance.FinanceRecordCreateDTO;
import com.foodtraceability.entity.Order;
import com.foodtraceability.service.finance.FinanceRecordService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 订单事件监听器
 * 用于监听订单状态变化，自动生成财务收支记录
 */
@Component
public class OrderEventListener {

    public OrderEventListener(FinanceRecordService financeRecordService) {
        this.financeRecordService = financeRecordService;
    }

    private final FinanceRecordService financeRecordService;

    /**
     * 监听订单完成事件，自动生成财务收支记录
     * @param order 订单实体
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCompletedEvent(Order order) {
        // 只处理订单状态为已完成的事件（已完成状态值为4）
        if (order.getOrderStatus() != null && order.getOrderStatus().equals(4)) {
            // 构造收支记录创建DTO
            FinanceRecordCreateDTO dto = new FinanceRecordCreateDTO();
            dto.setRecordType(1); // 1-收入
            dto.setRecordCategory(101); // 101-销售收入
            // 金额：元（BigDecimal）转分（Long）
            BigDecimal amountYuan = order.getActualAmount() != null
                    ? order.getActualAmount() : BigDecimal.ZERO;
            dto.setAmount(amountYuan.multiply(BigDecimal.valueOf(100L)).longValue());
            dto.setBusinessDate(LocalDate.now());
            dto.setRemark("订单完成，生成销售收入记录 - 订单ID：" + order.getOrderId());

            // 创建收支记录
            financeRecordService.create(dto);
        }
    }
}
