package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.foodtraceability.config.handler.JsonbListTypeHandler;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 营销CRM会员实体类
 * 完整的会员信息，包含积分、余额、消费统计、RFM评分等营销相关字段
 */
@TableName(value = "members", autoResultMap = true)
public class MarketingMember implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 会员ID（自增主键） */
    @TableId(type = IdType.AUTO)
    @TableField("member_id")
    private Long memberId;

    /** 会员卡号（唯一） */
    @TableField("member_no")
    private String memberNo;

    /** 手机号（登录账号，唯一） */
    @TableField("phone")
    private String phone;

    /** 登录密码（加密存储） */
    @TableField("password")
    private String password;

    /** 昵称 */
    @TableField("nickname")
    private String nickname;

    /** 头像URL */
    @TableField("avatar_url")
    private String avatarUrl;

    /** 性别：0未知 1男 2女 */
    @TableField("gender")
    private Integer gender;

    /** 生日 */
    @TableField("birthday")
    private LocalDate birthday;

    /** 邮箱 */
    @TableField("email")
    private String email;

    /** 会员等级ID（关联member_level表） */
    @TableField("member_level_id")
    private Long memberLevelId;

    /** 当前可用积分 */
    @TableField("points")
    private Integer points;

    /** 历史累计获得积分 */
    @TableField("total_points_earned")
    private Integer totalPointsEarned;

    /** 历史累计使用积分 */
    @TableField("total_points_used")
    private Integer totalPointsUsed;

    /** 余额（分），充值剩余 */
    @TableField("balance")
    private Long balance;

    /** 累计充值金额（分） */
    @TableField("total_recharge")
    private Long totalRecharge;

    /** 累计消费金额（分） */
    @TableField("total_consume")
    private Long totalConsume;

    /** 订单数量 */
    @TableField("order_count")
    private Integer orderCount;

    /** 最后消费时间 */
    @TableField("last_order_time")
    private LocalDateTime lastOrderTime;

    /** 最后到店时间 */
    @TableField("last_visit_time")
    private LocalDateTime lastVisitTime;

    /** 注册渠道：1注册APP 2收银台注册 3扫码 4导入 5小程序 */
    @TableField("register_channel")
    private Integer registerChannel;

    /** 状态：1正常 2冻结 3黑名单 4注销 */
    @TableField("status")
    private Integer status;

    /** 标签数组JSON：["VIP","高频","生日月"...] */
    @TableField(value = "tags", typeHandler = JsonbListTypeHandler.class)
    private List<String> tags;

    /** 备注 */
    @TableField("remark")
    private String remark;

    /** RFM-R最近消费得分(1-5) */
    @TableField("r_score")
    private Integer rScore;

    /** RFM-F消费频率得分(1-5) */
    @TableField("f_score")
    private Integer fScore;

    /** RFM-M消费金额得分(1-5) */
    @TableField("m_score")
    private Integer mScore;

    /** 客户分层标签 */
    @TableField("customer_segment")
    private String customerSegment;

    /** 创建时间 */
    @TableField("create_time")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField("update_time")
    private LocalDateTime updateTime;

    /** 逻辑删除标记：0未删除，1已删除 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    // ==================== 非持久化字段（关联查询时填充） ====================

    /** 等级名称（非数据库字段，用于VO展示） */
    @TableField(exist = false)
    private String levelName;

    /** 等级编码（非数据库字段） */
    @TableField(exist = false)
    private String levelCode;

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
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

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
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
}
