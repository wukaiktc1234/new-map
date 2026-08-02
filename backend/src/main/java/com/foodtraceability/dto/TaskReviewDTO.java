package com.foodtraceability.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 审核任务请求DTO
 */
public class TaskReviewDTO {

    /** 审核结果：approve=通过 / reject=驳回 */
    @NotBlank(message = "审核结果不能为空")
    private String action;

    /** 审核意见 */
    private String comment;

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
