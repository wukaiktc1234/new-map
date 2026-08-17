package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;

/**
 * 销售订单实体（OICBE-B2-001 实体对齐：@TableName("sales_order") → "orders"，PD-015 唯一生产真相源）
 *
 * <p>T 技术契约（oic-be-task-board.md §四·4，已裁决）：T-1 主键随表（id→order_id，String）、
 * T-3 金额落新列集 bigint 分（totalAmount→total_amount、actualAmount→paid_amount 实收/实付语义、
 * discountAmount→discount_amount 唯一同名列按表类型对齐）、T-4 命名选边新列集（orderNo→order_code、
 * orderTime→create_time、estimatedTime→expected_time、remark→remark、peopleCount→dining_people_count）、
 * T-5 createdBy/updatedBy→create_by/update_by（String 直映）、T-6 tableNo→table_name。
 *
 * <p>类型按表对齐说明：金额字段原 Integer（分），orders 新列集为 bigint（分）→ 实体层用 Long（兼容类型，
 * 分语义不变）；调用方转换逻辑（如 T-039 应收链路的 Long 转换）在 Service 层保留，语义迁移归 Batch 3。
 *
 * <p>B 业务语义（BLOCKED，不猜测）：status（B-1 状态机映射，待 PD-017）、completedTime（B-2 处置，
 * 待 PD-018）、orderType（B-3 枚举映射，待 PD-019）——以 @TableField(exist = false) 标注保留字段，
 * 不参与 SQL（技术手段非业务决策，不猜测映射）。
 */
@TableName("orders")
public class SalesOrder {

    /** 订单ID（T-1：主键随表，orders.order_id varchar(32)，String 业务主键；@TableId 无 type=全局 ASSIGN_ID，对齐 OrderNew） */
    @TableId(value = "order_id")
    private String id;

    /** 订单编号（T-4：orderNo → orders.order_code，新列集选边） */
    @TableField("order_code")
    private String orderNo;

    /** 会员ID（直映 orders.customer_id bigint，Long 兼容） */
    @TableField("customer_id")
    private Long customerId;

    /** 顾客姓名（直映 orders.customer_name varchar(50)） */
    @TableField("customer_name")
    private String customerName;

    /** 顾客电话（直映 orders.customer_phone varchar(20)） */
    @TableField("customer_phone")
    private String customerPhone;

    /** 订单类型（B-3 BLOCKED：dinein/takeout/delivery ↔ 1堂食/2外卖/3自提/4打包 映射待 PD-019 决策，exist=false 不参与 SQL） */
    @TableField(exist = false)
    private String orderType;

    /** 订单总金额（分）（T-3：totalAmount → orders.total_amount bigint；原 Integer 对齐为 Long） */
    @TableField("total_amount")
    private Long totalAmount;

    /** 折扣优惠（分）（T-3：discountAmount → orders.discount_amount，唯一同名列按表类型 bigint 对齐） */
    @TableField("discount_amount")
    private Long discountAmount;

    /** 实收金额（分）（T-3：actualAmount → orders.paid_amount，实收/实付语义；原 Integer 对齐为 Long） */
    @TableField("paid_amount")
    private Long actualAmount;

    /** 订单状态（B-1 BLOCKED：pending/preparing/completed/delivered ↔ order_status 0-6 状态机映射待 PD-017 决策，exist=false 不参与 SQL） */
    @TableField(exist = false)
    private String status;

    /** 下单时间（T-4：orderTime → orders.create_time timestamp；Date 兼容类型，语义=下单即创建，随批 QA 复核） */
    @TableField("create_time")
    private Date orderTime;

    /** 预计完成时间（T-4：estimatedTime → orders.expected_time timestamp，新列集选边） */
    @TableField("expected_time")
    private Date estimatedTime;

    /** 完成时间（B-2 BLOCKED：orders 无对应列，补列/派生/废弃三选一待 PD-018 决策，exist=false 不参与 SQL） */
    @TableField(exist = false)
    private Date completedTime;

    /** 桌台名称（T-6：tableNo → orders.table_name varchar(50)，String 兼容；语义确认随批 QA 复核） */
    @TableField("table_name")
    private String tableNo;

    /** 用餐人数（T-4：peopleCount → orders.dining_people_count integer） */
    @TableField("dining_people_count")
    private Integer peopleCount;

    /** 备注（T-4：remark → orders.remark text，新列集选边） */
    @TableField("remark")
    private String remark;

    /** 门店ID（直映 orders.store_id bigint，V20260629_003 补列） */
    @TableField("store_id")
    private Long storeId;

    /** 创建人（T-5：createdBy → orders.create_by varchar(50)，String 直映） */
    @TableField("create_by")
    private String createdBy;

    /** 更新人（T-5：updatedBy → orders.update_by varchar(50)，String 直映） */
    @TableField("update_by")
    private String updatedBy;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Long getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(Long discountAmount) {
        this.discountAmount = discountAmount;
    }

    public Long getActualAmount() {
        return actualAmount;
    }

    public void setActualAmount(Long actualAmount) {
        this.actualAmount = actualAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getOrderTime() {
        return orderTime;
    }

    public void setOrderTime(Date orderTime) {
        this.orderTime = orderTime;
    }

    public Date getEstimatedTime() {
        return estimatedTime;
    }

    public void setEstimatedTime(Date estimatedTime) {
        this.estimatedTime = estimatedTime;
    }

    public Date getCompletedTime() {
        return completedTime;
    }

    public void setCompletedTime(Date completedTime) {
        this.completedTime = completedTime;
    }

    public String getTableNo() {
        return tableNo;
    }

    public void setTableNo(String tableNo) {
        this.tableNo = tableNo;
    }

    public Integer getPeopleCount() {
        return peopleCount;
    }

    public void setPeopleCount(Integer peopleCount) {
        this.peopleCount = peopleCount;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
