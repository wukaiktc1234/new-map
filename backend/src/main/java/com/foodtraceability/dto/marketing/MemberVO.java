package com.foodtraceability.dto.marketing;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 会员信息VO（视图对象）
 * 用于返回给前端的会员详细信息
 */
@Schema(description = "会员信息视图对象")
public class MemberVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 会员ID */
    private Long memberId;

    /** 会员卡号 */
    private String memberNo;

    /** 手机号（脱敏显示） */
    private String phone;

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatarUrl;

    /** 性别：0未知 1男 2女 */
    private Integer gender;

    /** 性别名称 */
    private String genderName;

    /** 生日 */
    private LocalDate birthday;

    /** 邮箱 */
    private String email;

    /** 会员等级ID */
    private Long memberLevelId;

    /** 等级名称 */
    private String levelName;

    /** 等级编码 */
    private String levelCode;

    /** 当前可用积分 */
    private Integer points;

    /** 历史累计获得积分 */
    private Integer totalPointsEarned;

    /** 历史累计使用积分 */
    private Integer totalPointsUsed;

    /** 账户余额（分） */
    private Long balance;

    /** 账户余额（元，用于展示） */
    private String balanceDisplay;

    /** 累计充值（分） */
    private Long totalRecharge;

    /** 累计消费（分） */
    private Long totalConsume;

    /** 累计消费（元，用于展示） */
    private String totalConsumeDisplay;

    /** 订单数量 */
    private Integer orderCount;

    /** 最后消费时间 */
    private LocalDateTime lastOrderTime;

    /** 最后到店时间 */
    private LocalDateTime lastVisitTime;

    /** 注册渠道 */
    private Integer registerChannel;

    /** 注册渠道名称 */
    private String registerChannelName;

    /** 状态：1正常 2冻结 3黑名单 4注销 */
    private Integer status;

    /** 状态名称 */
    private String statusName;

    /** 标签列表 */
    private List<String> tagsList;

    /** 备注 */
    private String remark;

    /** RFM-R最近消费得分(1-5) */
    private Integer rScore;

    /** RFM-F消费频率得分(1-5) */
    private Integer fScore;

    /** RFM-M消费金额得分(1-5) */
    private Integer mScore;

    /** 客户分层标签 */
    private String customerSegment;

    /** 客户分层描述 */
    private String customerSegmentDesc;

    /** 注册时间 */
    private LocalDateTime createTime;

    // ==================== Getter & Setter ====================

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

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

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public String getGenderName() {
        return genderName;
    }

    public void setGenderName(String genderName) {
        this.genderName = genderName;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getMemberLevelId() {
        return memberLevelId;
    }

    public void setMemberLevelId(Long memberLevelId) {
        this.memberLevelId = memberLevelId;
    }

    public String getLevelName() {
        return levelName;
    }

    public void setLevelName(String levelName) {
        this.levelName = levelName;
    }

    public String getLevelCode() {
        return levelCode;
    }

    public void setLevelCode(String levelCode) {
        this.levelCode = levelCode;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public Integer getTotalPointsEarned() {
        return totalPointsEarned;
    }

    public void setTotalPointsEarned(Integer totalPointsEarned) {
        this.totalPointsEarned = totalPointsEarned;
    }

    public Integer getTotalPointsUsed() {
        return totalPointsUsed;
    }

    public void setTotalPointsUsed(Integer totalPointsUsed) {
        this.totalPointsUsed = totalPointsUsed;
    }

    public Long getBalance() {
        return balance;
    }

    public void setBalance(Long balance) {
        this.balance = balance;
    }

    public String getBalanceDisplay() {
        return balanceDisplay;
    }

    public void setBalanceDisplay(String balanceDisplay) {
        this.balanceDisplay = balanceDisplay;
    }

    public Long getTotalRecharge() {
        return totalRecharge;
    }

    public void setTotalRecharge(Long totalRecharge) {
        this.totalRecharge = totalRecharge;
    }

    public Long getTotalConsume() {
        return totalConsume;
    }

    public void setTotalConsume(Long totalConsume) {
        this.totalConsume = totalConsume;
    }

    public String getTotalConsumeDisplay() {
        return totalConsumeDisplay;
    }

    public void setTotalConsumeDisplay(String totalConsumeDisplay) {
        this.totalConsumeDisplay = totalConsumeDisplay;
    }

    public Integer getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Integer orderCount) {
        this.orderCount = orderCount;
    }

    public LocalDateTime getLastOrderTime() {
        return lastOrderTime;
    }

    public void setLastOrderTime(LocalDateTime lastOrderTime) {
        this.lastOrderTime = lastOrderTime;
    }

    public LocalDateTime getLastVisitTime() {
        return lastVisitTime;
    }

    public void setLastVisitTime(LocalDateTime lastVisitTime) {
        this.lastVisitTime = lastVisitTime;
    }

    public Integer getRegisterChannel() {
        return registerChannel;
    }

    public void setRegisterChannel(Integer registerChannel) {
        this.registerChannel = registerChannel;
    }

    public String getRegisterChannelName() {
        return registerChannelName;
    }

    public void setRegisterChannelName(String registerChannelName) {
        this.registerChannelName = registerChannelName;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }

    public List<String> getTagsList() {
        return tagsList;
    }

    public void setTagsList(List<String> tagsList) {
        this.tagsList = tagsList;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Integer getrScore() {
        return rScore;
    }

    public void setrScore(Integer rScore) {
        this.rScore = rScore;
    }

    public Integer getfScore() {
        return fScore;
    }

    public void setfScore(Integer fScore) {
        this.fScore = fScore;
    }

    public Integer getmScore() {
        return mScore;
    }

    public void setmScore(Integer mScore) {
        this.mScore = mScore;
    }

    public String getCustomerSegment() {
        return customerSegment;
    }

    public void setCustomerSegment(String customerSegment) {
        this.customerSegment = customerSegment;
    }

    public String getCustomerSegmentDesc() {
        return customerSegmentDesc;
    }

    public void setCustomerSegmentDesc(String customerSegmentDesc) {
        this.customerSegmentDesc = customerSegmentDesc;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
