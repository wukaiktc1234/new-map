package com.foodtraceability.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * 会计事件接口
 * 定义标准化的会计事件数据结构，所有业务事件都需要实现此接口
 * 用于自动记账引擎的事件标准化处理
 * @author example
 * @since 2026-04-04
 */
public interface AccountingEvent {

    /**
     * 获取事件类型
     * 如：POS_SALE_CASH（POS现金收款）、POS_SALE_WECHAT（POS微信收款）、PURCHASE_INBOUND（采购入库）
     * @return 事件类型编码
     */
    String getEventType();

    /**
     * 获取源业务单据ID
     * 唯一标识产生此事件的业务单据
     * @return 源业务单据ID
     */
    String getSourceBusinessId();

    /**
     * 获取源业务单据编号
     * 业务系统中的可读编号，用于审计追溯
     * @return 源业务单据编号
     */
    String getSourceBusinessNo();

    /**
     * 获取事件日期
     * 用于确定凭证的会计期间归属
     * @return 事件发生日期
     */
    LocalDate getEventDate();

    /**
     * 获取总金额
     * 事件涉及的金额总量
     * @return 总金额
     */
    BigDecimal getAmount();

    /**
     * 获取扩展数据
     * 包含对方单位、支付方式、税率等额外信息
     * key为数据项名称，value为对应的值
     * @return 扩展数据Map
     */
    Map<String, Object> getExtraData();
}
