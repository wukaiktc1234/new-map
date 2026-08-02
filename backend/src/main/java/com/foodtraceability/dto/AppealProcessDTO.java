package com.foodtraceability.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 处理申诉请求DTO
 */
public class AppealProcessDTO {

    /** 处理动作：accept/reject/resolve/close */
    @NotBlank(message = "处理动作不能为空")
    private String action;

    /** 处理意见/备注 */
    private String comment;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
