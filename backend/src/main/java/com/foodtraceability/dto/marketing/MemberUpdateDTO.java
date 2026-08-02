package com.foodtraceability.dto.marketing;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 会员信息更新请求DTO
 */
@Schema(description = "会员信息更新请求")
public class MemberUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 昵称 */
    @Size(max = 50, message = "昵称不能超过50个字符")
    private String nickname;

    /** 头像URL */
    private String avatarUrl;

    /** 性别：0未知 1男 2女 */
    private Integer gender;

    /** 生日 */
    private LocalDate birthday;

    /** 邮箱 */
    private String email;

    /** 备注 */
    private String remark;

    // ==================== Getter & Setter ====================

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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
