package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("members")
public class Member {
    @TableId(value = "member_id", type = IdType.AUTO)
    private Long id;
    private String memberNo;
    @TableField("nickname")
    private String name;
    private String phone;
    @TableField(exist = false)
    private String gender;
    @TableField(exist = false)
    private LocalDateTime birthday;
    @TableField("member_level_id")
    private Integer level;
    @TableField(exist = false)
    private String levelName;
    private BigDecimal balance;
    private Integer points;
    @TableField("total_consume")
    private BigDecimal totalSpent;
    private Integer orderCount;
    @TableField(exist = false)
    private String status;
    @TableField("create_time")
    private LocalDateTime registeredAt;
    @TableField("last_visit_time")
    private LocalDateTime lastVisitAt;
    @TableField(exist = false)
    private LocalDateTime createdAt;
    @TableField("update_time")
    private LocalDateTime updatedAt;

    public Member() {
    }

    public Long getId() {
        return this.id;
    }

    public String getMemberNo() {
        return this.memberNo;
    }

    public String getName() {
        return this.name;
    }

    public String getPhone() {
        return this.phone;
    }

    public String getGender() {
        return this.gender;
    }

    public LocalDateTime getBirthday() {
        return this.birthday;
    }

    public Integer getLevel() {
        return this.level;
    }

    public String getLevelName() {
        return this.levelName;
    }

    public BigDecimal getBalance() {
        return this.balance;
    }

    public Integer getPoints() {
        return this.points;
    }

    public BigDecimal getTotalSpent() {
        return this.totalSpent;
    }

    public Integer getOrderCount() {
        return this.orderCount;
    }

    public String getStatus() {
        return this.status;
    }

    public LocalDateTime getRegisteredAt() {
        return this.registeredAt;
    }

    public LocalDateTime getLastVisitAt() {
        return this.lastVisitAt;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setMemberNo(final String memberNo) {
        this.memberNo = memberNo;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setPhone(final String phone) {
        this.phone = phone;
    }

    public void setGender(final String gender) {
        this.gender = gender;
    }

    public void setBirthday(final LocalDateTime birthday) {
        this.birthday = birthday;
    }

    public void setLevel(final Integer level) {
        this.level = level;
    }

    public void setLevelName(final String levelName) {
        this.levelName = levelName;
    }

    public void setBalance(final BigDecimal balance) {
        this.balance = balance;
    }

    public void setPoints(final Integer points) {
        this.points = points;
    }

    public void setTotalSpent(final BigDecimal totalSpent) {
        this.totalSpent = totalSpent;
    }

    public void setOrderCount(final Integer orderCount) {
        this.orderCount = orderCount;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public void setRegisteredAt(final LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }

    public void setLastVisitAt(final LocalDateTime lastVisitAt) {
        this.lastVisitAt = lastVisitAt;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(final LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof Member)) return false;
        final Member other = (Member) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$level = this.getLevel();
        final java.lang.Object other$level = other.getLevel();
        if (this$level == null ? other$level != null : !this$level.equals(other$level)) return false;
        final java.lang.Object this$points = this.getPoints();
        final java.lang.Object other$points = other.getPoints();
        if (this$points == null ? other$points != null : !this$points.equals(other$points)) return false;
        final java.lang.Object this$orderCount = this.getOrderCount();
        final java.lang.Object other$orderCount = other.getOrderCount();
        if (this$orderCount == null ? other$orderCount != null : !this$orderCount.equals(other$orderCount)) return false;
        final java.lang.Object this$memberNo = this.getMemberNo();
        final java.lang.Object other$memberNo = other.getMemberNo();
        if (this$memberNo == null ? other$memberNo != null : !this$memberNo.equals(other$memberNo)) return false;
        final java.lang.Object this$name = this.getName();
        final java.lang.Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final java.lang.Object this$phone = this.getPhone();
        final java.lang.Object other$phone = other.getPhone();
        if (this$phone == null ? other$phone != null : !this$phone.equals(other$phone)) return false;
        final java.lang.Object this$gender = this.getGender();
        final java.lang.Object other$gender = other.getGender();
        if (this$gender == null ? other$gender != null : !this$gender.equals(other$gender)) return false;
        final java.lang.Object this$birthday = this.getBirthday();
        final java.lang.Object other$birthday = other.getBirthday();
        if (this$birthday == null ? other$birthday != null : !this$birthday.equals(other$birthday)) return false;
        final java.lang.Object this$levelName = this.getLevelName();
        final java.lang.Object other$levelName = other.getLevelName();
        if (this$levelName == null ? other$levelName != null : !this$levelName.equals(other$levelName)) return false;
        final java.lang.Object this$balance = this.getBalance();
        final java.lang.Object other$balance = other.getBalance();
        if (this$balance == null ? other$balance != null : !this$balance.equals(other$balance)) return false;
        final java.lang.Object this$totalSpent = this.getTotalSpent();
        final java.lang.Object other$totalSpent = other.getTotalSpent();
        if (this$totalSpent == null ? other$totalSpent != null : !this$totalSpent.equals(other$totalSpent)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$registeredAt = this.getRegisteredAt();
        final java.lang.Object other$registeredAt = other.getRegisteredAt();
        if (this$registeredAt == null ? other$registeredAt != null : !this$registeredAt.equals(other$registeredAt)) return false;
        final java.lang.Object this$lastVisitAt = this.getLastVisitAt();
        final java.lang.Object other$lastVisitAt = other.getLastVisitAt();
        if (this$lastVisitAt == null ? other$lastVisitAt != null : !this$lastVisitAt.equals(other$lastVisitAt)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        final java.lang.Object this$updatedAt = this.getUpdatedAt();
        final java.lang.Object other$updatedAt = other.getUpdatedAt();
        if (this$updatedAt == null ? other$updatedAt != null : !this$updatedAt.equals(other$updatedAt)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof Member;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $level = this.getLevel();
        result = result * PRIME + ($level == null ? 43 : $level.hashCode());
        final java.lang.Object $points = this.getPoints();
        result = result * PRIME + ($points == null ? 43 : $points.hashCode());
        final java.lang.Object $orderCount = this.getOrderCount();
        result = result * PRIME + ($orderCount == null ? 43 : $orderCount.hashCode());
        final java.lang.Object $memberNo = this.getMemberNo();
        result = result * PRIME + ($memberNo == null ? 43 : $memberNo.hashCode());
        final java.lang.Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final java.lang.Object $phone = this.getPhone();
        result = result * PRIME + ($phone == null ? 43 : $phone.hashCode());
        final java.lang.Object $gender = this.getGender();
        result = result * PRIME + ($gender == null ? 43 : $gender.hashCode());
        final java.lang.Object $birthday = this.getBirthday();
        result = result * PRIME + ($birthday == null ? 43 : $birthday.hashCode());
        final java.lang.Object $levelName = this.getLevelName();
        result = result * PRIME + ($levelName == null ? 43 : $levelName.hashCode());
        final java.lang.Object $balance = this.getBalance();
        result = result * PRIME + ($balance == null ? 43 : $balance.hashCode());
        final java.lang.Object $totalSpent = this.getTotalSpent();
        result = result * PRIME + ($totalSpent == null ? 43 : $totalSpent.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $registeredAt = this.getRegisteredAt();
        result = result * PRIME + ($registeredAt == null ? 43 : $registeredAt.hashCode());
        final java.lang.Object $lastVisitAt = this.getLastVisitAt();
        result = result * PRIME + ($lastVisitAt == null ? 43 : $lastVisitAt.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        final java.lang.Object $updatedAt = this.getUpdatedAt();
        result = result * PRIME + ($updatedAt == null ? 43 : $updatedAt.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "Member(id=" + this.getId() + ", memberNo=" + this.getMemberNo() + ", name=" + this.getName() + ", phone=" + this.getPhone() + ", gender=" + this.getGender() + ", birthday=" + this.getBirthday() + ", level=" + this.getLevel() + ", levelName=" + this.getLevelName() + ", balance=" + this.getBalance() + ", points=" + this.getPoints() + ", totalSpent=" + this.getTotalSpent() + ", orderCount=" + this.getOrderCount() + ", status=" + this.getStatus() + ", registeredAt=" + this.getRegisteredAt() + ", lastVisitAt=" + this.getLastVisitAt() + ", createdAt=" + this.getCreatedAt() + ", updatedAt=" + this.getUpdatedAt() + ")";
    }
}
