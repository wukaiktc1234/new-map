package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 批量发送注册码请求DTO
 */
@Schema(description = "批量发送注册码请求")
public class BatchSendCodeRequest {

    @Schema(description = "注册码列表")
    private List<String> codes;

    @Schema(description = "接收方式（SMS：短信，EMAIL：邮箱）", example = "EMAIL")
    private String sendType;

    @Schema(description = "接收地址列表（手机号或邮箱）")
    private List<String> receiveAddresses;

    @Schema(description = "发送人ID")
    private String sendBy;

    // Getters and Setters
    public List<String> getCodes() {
        return codes;
    }

    public void setCodes(List<String> codes) {
        this.codes = codes;
    }

    public String getSendType() {
        return sendType;
    }

    public void setSendType(String sendType) {
        this.sendType = sendType;
    }

    public List<String> getReceiveAddresses() {
        return receiveAddresses;
    }

    public void setReceiveAddresses(List<String> receiveAddresses) {
        this.receiveAddresses = receiveAddresses;
    }

    public String getSendBy() {
        return sendBy;
    }

    public void setSendBy(String sendBy) {
        this.sendBy = sendBy;
    }
}