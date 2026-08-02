package com.foodtraceability.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 创建申诉请求DTO
 */
public class AppealCreateDTO {

    /** 申诉类型：penalty / complaint */
    @NotBlank(message = "申诉类型不能为空")
    private String type;

    /** 是否匿名 */
    @NotNull(message = "匿名标识不能为空")
    private Boolean anonymousFlag;

    /** 申诉标题（必填，最大50字） */
    @NotBlank(message = "标题不能为空")
    @Size(max = 50, message = "标题不超过50个字符")
    private String title;

    /** 详细描述（必填，最大500字） */
    @NotBlank(message = "详细描述不能为空")
    @Size(max = 500, message = "描述不超过500个字符")
    private String description;

    /** 关联处罚单号（仅penalty模式，选填） */
    private String targetDecisionId;

    /** 期望结果（选填） */
    private String expectedResult;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getAnonymousFlag() {
        return anonymousFlag;
    }

    public void setAnonymousFlag(Boolean anonymousFlag) {
        this.anonymousFlag = anonymousFlag;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTargetDecisionId() {
        return targetDecisionId;
    }

    public void setTargetDecisionId(String targetDecisionId) {
        this.targetDecisionId = targetDecisionId;
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(String expectedResult) {
        this.expectedResult = expectedResult;
    }
}
