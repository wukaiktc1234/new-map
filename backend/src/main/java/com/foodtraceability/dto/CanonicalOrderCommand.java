package com.foodtraceability.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Canonical Order Creation Command（统一订单创建语义合同）
 *
 * 所有订单创建入口（POS收银/POS扫码/小程序）统一转换为此 DTO 后调用
 * PosOrderCreateService.createCanonicalOrder()。
 *
 * 合同约定：
 * - 金额单位：元（BigDecimal），canonical service 内部统一转分（Long）
 * - 状态：创建时 order_status=0 + payment_status=0
 * - 订单号：ORD{yyyyMMdd}{sequence}（canonical service 生成）
 * - 幂等：可选 idempotency_key
 *
 * W1 前置任务：建立合同。W1-EC-01/02/03 切换写入目标表。
 */
public class CanonicalOrderCommand {

    /** 桌号（堂食必填） */
    private Integer tableNumber;

    /** 桌台ID（堂食必填，关联 dining_tables） */
    private Long tableId;

    /** 订单项列表（金额单位：元） */
    private List<CanonicalOrderItemCommand> items;

    /** 订单类型：0堂食 1外卖 2自提（已由入口转换为整数） */
    private Integer orderType;

    /** 订单来源：1收银台 2小程序 3第三方平台 4POS终端 */
    private Integer orderSource;

    /** 门店ID（stores_new.store_id 的字符串形式） */
    private String storeId;

    /** 用户标识（已登录用户ID 或 openid 派生） */
    private String userId;

    /** 幂等性键（可选，用于防止重复提交） */
    private String idempotencyKey;

    /** 备注 */
    private String remark;

    /** 联系人姓名（外卖必填） */
    private String contactName;

    /** 联系人电话（外卖必填，将被加密存储） */
    private String contactPhone;

    /** 配送地址（外卖必填，将被加密存储） */
    private String deliveryAddress;

    /** 支付方式（字符串，由 canonical service 转换为整数） */
    private String paymentMethod;

    // ==================== Constructors ====================

    public CanonicalOrderCommand() {
    }

    // ==================== Getters ====================

    public Integer getTableNumber() { return tableNumber; }
    public Long getTableId() { return tableId; }
    public List<CanonicalOrderItemCommand> getItems() { return items; }
    public Integer getOrderType() { return orderType; }
    public Integer getOrderSource() { return orderSource; }
    public String getStoreId() { return storeId; }
    public String getUserId() { return userId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getRemark() { return remark; }
    public String getContactName() { return contactName; }
    public String getContactPhone() { return contactPhone; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public String getPaymentMethod() { return paymentMethod; }

    // ==================== Setters ====================

    public void setTableNumber(Integer tableNumber) { this.tableNumber = tableNumber; }
    public void setTableId(Long tableId) { this.tableId = tableId; }
    public void setItems(List<CanonicalOrderItemCommand> items) { this.items = items; }
    public void setOrderType(Integer orderType) { this.orderType = orderType; }
    public void setOrderSource(Integer orderSource) { this.orderSource = orderSource; }
    public void setStoreId(String storeId) { this.storeId = storeId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public void setRemark(String remark) { this.remark = remark; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    /** P0-B：建单必须绑定活跃班次（orders.shift_id） */
    private Long shiftId;

    public Long getShiftId() {
        return shiftId;
    }

    public void setShiftId(Long shiftId) {
        this.shiftId = shiftId;
    }
}
