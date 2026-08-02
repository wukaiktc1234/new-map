package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 发送注册码请求DTO
 */
@Schema(description = "发送注册码请求")
public class SendCodeRequest {

    @Schema(description = "注册码", example = "RC202512260001")
    private String code;

    @Schema(description = "接收方式（SMS：短信，EMAIL：邮箱）", example = "EMAIL")
    private String sendType;

    @Schema(description = "接收地址（手机号或邮箱）", example = "zhangsan@example.com")
    private String receiveAddress;

    @Schema(description = "发送人ID")
    private String sendBy;

    // Getters and Setters
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getSendType() {
        return sendType;
    }

    public void setSendType(String sendType) {
        this.sendType = sendType;
    }

    public String getReceiveAddress() {
        return receiveAddress;
    }

    public void setReceiveAddress(String receiveAddress) {
        this.receiveAddress = receiveAddress;
    }

    public String getSendBy() {
        return sendBy;
    }

    public void setSendBy(String sendBy) {
        this.sendBy = sendBy;
    }
}