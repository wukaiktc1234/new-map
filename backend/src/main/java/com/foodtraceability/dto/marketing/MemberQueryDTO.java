package com.foodtraceability.dto.marketing;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 会员查询条件DTO
 */
@Schema(description = "会员查询条件")
public class MemberQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 会员卡号（模糊查询） */
    private String memberNo;

    /** 手机号（精确查询） */
    private String phone;

    /** 昵称（模糊查询） */
    private String nickname;

    /** 会员等级ID */
    private Long memberLevelId;

    /** 状态：1正常 2冻结 3黑名单 4注销 */
    private Integer status;

    /** 注册渠道 */
    private Integer registerChannel;

    /** 客户分层标签 */
    private String customerSegment;

    /** 标签筛选（逗号分隔） */
    private String tag;

    /** 积分范围-最小值 */
    private Integer pointsMin;

    /** 积分范围-最大值 */
    private Integer pointsMax;

    /** 余额范围-最小值（分） */
    private Long balanceMin;

    /** 余额范围-最大值（分） */
    private Long balanceMax;

    /** 最后到店时间-开始 */
    private String lastVisitStart;

    /** 最后到店时间-结束 */
    private String lastVisitEnd;

    /** 注册时间-开始 */
    private String createStart;

    /** 注册时间-结束 */
    private String createEnd;

    /** 页码 */
    private Integer current = 1;

    /** 每页大小 */
    private Integer size = 10;

    // ==================== Getter & Setter ====================

    public String getMemberNo() {
        return memberNo;
    }

    public void setMemberNo(String memberNo) {
        this.memberNo = memberNo;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Long getMemberLevelId() {
        return memberLevelId;
    }

    public void setMemberLevelId(Long memberLevelId) {
        this.memberLevelId = memberLevelId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getRegisterChannel() {
        return registerChannel;
    }

    public void setRegisterChannel(Integer registerChannel) {
        this.registerChannel = registerChannel;
    }

    public String getCustomerSegment() {
        return customerSegment;
    }

    public void setCustomerSegment(String customerSegment) {
        this.customerSegment = customerSegment;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Integer getPointsMin() {
        return pointsMin;
    }

    public void setPointsMin(Integer pointsMin) {
        this.pointsMin = pointsMin;
    }

    public Integer getPointsMax() {
        return pointsMax;
    }

    public void setPointsMax(Integer pointsMax) {
        this.pointsMax = pointsMax;
    }

    public Long getBalanceMin() {
        return balanceMin;
    }

    public void setBalanceMin(Long balanceMin) {
        this.balanceMin = balanceMin;
    }

    public Long getBalanceMax() {
        return balanceMax;
    }

    public void setBalanceMax(Long balanceMax) {
        this.balanceMax = balanceMax;
    }

    public String getLastVisitStart() {
        return lastVisitStart;
    }

    public void setLastVisitStart(String lastVisitStart) {
        this.lastVisitStart = lastVisitStart;
    }

    public String getLastVisitEnd() {
        return lastVisitEnd;
    }

    public void setLastVisitEnd(String lastVisitEnd) {
        this.lastVisitEnd = lastVisitEnd;
    }

    public String getCreateStart() {
        return createStart;
    }

    public void setCreateStart(String createStart) {
        this.createStart = createStart;
    }

    public String getCreateEnd() {
        return createEnd;
    }

    public void setCreateEnd(String createEnd) {
        this.createEnd = createEnd;
    }

    public Integer getCurrent() {
        return current;
    }

    public void setCurrent(Integer current) {
        this.current = current;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
