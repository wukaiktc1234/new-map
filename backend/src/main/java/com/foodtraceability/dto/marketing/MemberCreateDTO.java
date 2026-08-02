package com.foodtraceability.dto.marketing;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 会员注册/创建请求DTO
 */
@Schema(description = "会员注册/创建请求")
public class MemberCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 手机号（必填，登录账号） */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 登录密码 */
    @Size(min = 6, max = 20, message = "密码长度应在6-20位之间")
    private String password;

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

    /** 注册渠道：1注册APP 2收银台注册 3扫码 4导入 5小程序 */
    @NotNull(message = "注册渠道不能为空")
    private Integer registerChannel;

    // ==================== Getter & Setter ====================

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

    public Integer getRegisterChannel() {
        return registerChannel;
    }

    public void setRegisterChannel(Integer registerChannel) {
        this.registerChannel = registerChannel;
    }
}
