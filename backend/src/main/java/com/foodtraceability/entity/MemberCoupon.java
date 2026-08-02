package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户优惠券实体类
 * 会员从优惠券模板领取后生成的具体优惠券实例
 */
@TableName("member_coupon")
public class MemberCoupon implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 优惠券记录ID（自增主键） */
    @TableId(type = IdType.AUTO)
    @TableField("coupon_id")
    private Long couponId;

    /** 优惠券模板ID */
    @TableField("template_id")
    private Long templateId;

    /** 会员ID */
    @TableField("member_id")
    private Long memberId;

    /** 优惠券唯一号码（用于核销） */
    @TableField("coupon_no")
    private String couponNo;

    /**
     * 状态
     * 0未使用 1已使用 2已过期 3已作废
     */
    @TableField("status")
    private Integer status;

    /** 领取时间 */
    @TableField("receive_time")
    private LocalDateTime receiveTime;

    /** 使用时间 */
    @TableField("use_time")
    private LocalDateTime useTime;

    /** 过期时间 */
    @TableField("expiry_time")
    private LocalDateTime expiryTime;

    /** 使用时的订单ID */
    @TableField("order_id")
    private Long orderId;

    /** 关联订单号 */
    @TableField("order_no")
    private String orderNo;

    /** 逻辑删除标记：0未删除，1已删除 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    // ==================== 非持久化字段 ====================

    /** 模板名称（用于展示） */
    @TableField(exist = false)
    private String templateName;

    /** 优惠值描述（如"减20元"或"95折"） */
    @TableField(exist = false)
    private String discountDesc;

    /** 最低消费门槛（分） */
    @TableField(exist = false)
    private Long minConsumption;

    /** 会员手机号 */
    @TableField(exist = false)
    private String memberPhone;

    /** 会员昵称 */
    @TableField(exist = false)
    private String memberNickname;

    // ==================== Getter & Setter ====================

    public Long getCouponId() {
        return couponId;
    }

    public void setCouponId(Long couponId) {
        this.couponId = couponId;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public String getCouponNo() {
        return couponNo;
    }

    public void setCouponNo(String couponNo) {
        this.couponNo = couponNo;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getReceiveTime() {
        return receiveTime;
    }

    public void setReceiveTime(LocalDateTime receiveTime) {
        this.receiveTime = receiveTime;
    }

    public LocalDateTime getUseTime() {
        return useTime;
    }

    public void setUseTime(LocalDateTime useTime) {
        this.useTime = useTime;
    }

    public LocalDateTime getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(LocalDateTime expiryTime) {
        this.expiryTime = expiryTime;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getDiscountDesc() {
        return discountDesc;
    }

    public void setDiscountDesc(String discountDesc) {
        this.discountDesc = discountDesc;
    }

    public Long getMinConsumption() {
        return minConsumption;
    }

    public void setMinConsumption(Long minConsumption) {
        this.minConsumption = minConsumption;
    }

    public String getMemberPhone() {
        return memberPhone;
    }

    public void setMemberPhone(String memberPhone) {
        this.memberPhone = memberPhone;
    }

    public String getMemberNickname() {
        return memberNickname;
    }

    public void setMemberNickname(String memberNickname) {
        this.memberNickname = memberNickname;
    }
}
