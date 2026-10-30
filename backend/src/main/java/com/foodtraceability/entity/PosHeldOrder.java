package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * POS 挂单实体（Owner 2026-10-30 P0）。
 *
 * <p>隔离三元组：employee_id + terminal_id + store_id。
 * terminal_id 来自请求 DTO（与 pos_shifts.terminal_id 同一来源），**不读 HTTP header**。</p>
 *
 * <p>金额单位：total_amount_fen 是 bigint **分**（铁律，不接受元）。
 * cart_data 是 PG 原生 jsonb（禁止退化成 text）。</p>
 */
@TableName("pos_held_orders")
@Schema(description = "POS 挂单实体")
public class PosHeldOrder {

    @TableId(type = IdType.AUTO, value = "id")
    private Long id;

    @Schema(description = "挂单号（由幂等键派生，唯一）")
    private String orderNo;

    @Schema(description = "收银员 user_id")
    private Long employeeId;

    @Schema(description = "终端ID（与 pos_shifts 同一来源）")
    private String terminalId;

    @Schema(description = "门店ID")
    private Long storeId;

    @Schema(description = "购物车 JSON（jsonb）")
    private String cartData;

    @Schema(description = "合计金额（分）")
    private Long totalAmountFen;

    @Schema(description = "状态：HOLD/COMPLETED/CANCELLED/EXPIRED")
    private String status;

    @Schema(description = "乐观锁版本")
    private Integer version;

    @Schema(description = "过期时间（创建当天 23:59:59）")
    private LocalDateTime expireAt;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getTerminalId() { return terminalId; }
    public void setTerminalId(String terminalId) { this.terminalId = terminalId; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public String getCartData() { return cartData; }
    public void setCartData(String cartData) { this.cartData = cartData; }
    public Long getTotalAmountFen() { return totalAmountFen; }
    public void setTotalAmountFen(Long totalAmountFen) { this.totalAmountFen = totalAmountFen; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public LocalDateTime getExpireAt() { return expireAt; }
    public void setExpireAt(LocalDateTime expireAt) { this.expireAt = expireAt; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
