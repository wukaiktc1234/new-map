package com.foodtraceability.event;

import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 门店日结完成事件（F-008 联动）
 *
 * <p>Sprint 3.1 P0：门店日结状态变为"已完成"后发布此事件，
 * 监听器 {@code StoreDailySettlementEventListener} 消费此事件
 * 调用 {@code AutoVoucherService.generateStoreSettlementVoucher()} 生成财务凭证。</p>
 *
 * <p>事件发布采用 try-catch 隔离，发布失败不影响主事务（门店日结）。
 * 监听器使用 {@code @TransactionalEventListener(phase = AFTER_COMMIT)} 确保
 * 主事务提交后才消费。</p>
 *
 * <p>本类由 T-020 提前创建（T-020 依赖此事件类存在以声明 AutoVoucherService 方法签名），
 * 等价于完成 T-035 的事件类定义部分。事件发布逻辑在阶段5 T-036 完成。</p>
 */
public class StoreDailySettlementCompletedEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    /** 日结单ID */
    private final Long settlementId;

    /** 门店ID */
    private final Long storeId;

    /** 门店名称 */
    private final String storeName;

    /** 日结日期 */
    private final LocalDate settlementDate;

    /** 当日总收入（单位：分） */
    private final Long totalIncome;

    /** 当日总支出（单位：分） */
    private final Long totalExpense;

    /** 净利润（单位：分） */
    private final Long netProfit;

    /** 操作员ID */
    private final Long operatorId;

    /** 操作时间 */
    private final LocalDateTime operateTime;

    public StoreDailySettlementCompletedEvent(Object source,
                                              Long settlementId,
                                              Long storeId,
                                              String storeName,
                                              LocalDate settlementDate,
                                              Long totalIncome,
                                              Long totalExpense,
                                              Long netProfit,
                                              Long operatorId,
                                              LocalDateTime operateTime) {
        super(source);
        this.settlementId = settlementId;
        this.storeId = storeId;
        this.storeName = storeName;
        this.settlementDate = settlementDate;
        this.totalIncome = totalIncome;
        this.totalExpense = totalExpense;
        this.netProfit = netProfit;
        this.operatorId = operatorId;
        this.operateTime = operateTime;
    }

    public Long getSettlementId() {
        return settlementId;
    }

    public Long getStoreId() {
        return storeId;
    }

    public String getStoreName() {
        return storeName;
    }

    public LocalDate getSettlementDate() {
        return settlementDate;
    }

    public Long getTotalIncome() {
        return totalIncome;
    }

    public Long getTotalExpense() {
        return totalExpense;
    }

    public Long getNetProfit() {
        return netProfit;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public LocalDateTime getOperateTime() {
        return operateTime;
    }

    @Override
    public String toString() {
        return "StoreDailySettlementCompletedEvent{" +
                "settlementId=" + settlementId +
                ", storeId=" + storeId +
                ", storeName='" + storeName + '\'' +
                ", settlementDate=" + settlementDate +
                ", totalIncome=" + totalIncome +
                ", totalExpense=" + totalExpense +
                ", netProfit=" + netProfit +
                ", operatorId=" + operatorId +
                ", operateTime=" + operateTime +
                '}';
    }
}
