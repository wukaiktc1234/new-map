package com.foodtraceability.dto.approval.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 风险预警VO
 * 用于描述审批过程中的风险评估结果
 * 由系统自动检测或人工审核时生成
 */
@Schema(description = "风险预警VO")
public class RiskWarningVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 风险等级
     * info=提示(低风险), warning=警告(中风险), danger=危险(高风险), prohibited=禁止(严重违规)
     */
    @Schema(description = "风险等级", example = "warning",
            allowableValues = {"info", "warning", "danger", "prohibited"})
    private String level;

    /**
     * 风险类别
     * health=健康相关, compliance=合规性, policy=政策违规,
     * budget=预算超支, schedule=排班冲突
     */
    @Schema(description = "风险类别", example = "policy",
            allowableValues = {"health", "compliance", "policy", "budget", "schedule"})
    private String category;

    /** 预警标题（简短描述） */
    @Schema(description = "预警标题", example = "年假余额不足警告")
    private String title;

    /** 详细描述说明 */
    @Schema(description = "详细描述",
            example = "当前员工年假余额为2天，本次申请3天，超出可用额度1天")
    private String description;

    /** 证据/依据（如具体数值、规则引用等） */
    @Schema(description = "证据/依据", example = "年假总额:10天, 已使用:8天, 剩余:2天, 申请:3天")
    private String evidence;

    /** 处理建议 */
    @Schema(description = "处理建议", example = "建议：1.调整请假天数至2天内；2.或改用事假/调休")
    private String suggestion;

    /** 数据来源（自动检测/人工审核） */
    @Schema(description = "数据来源", example = "auto_detect",
            allowableValues = {"auto_detect", "manual_review"})
    private String source;

    /** 预警生成时间 */
    @Schema(description = "生成时间", example = "2026-06-03T10:35:00")
    private LocalDateTime generatedAt;

    /**
     * 风险规则编码
     * 标识触发该预警的风控规则唯一编码，用于规则追踪和统计
     */
    @Schema(description = "风险规则编码", example = "LEAVE_BALANCE_INSUFFICIENT")
    private String ruleCode;

    /**
     * 风险评估时间
     * 记录风控引擎执行评估的时间戳
     */
    @Schema(description = "评估时间", example = "2026-06-03T10:35:00")
    private LocalDateTime evaluateTime;

    /** 复核人姓名（人工审核时填写） */
    @Schema(description = "复核人姓名", example = "系统管理员")
    private String reviewerName;

    /** 相关法规/制度引用（可选） */
    @Schema(description = "法规/制度引用", example = "《员工休假管理制度》第5条第2款")
    private String regulationRef;

    // ==================== Getter & Setter 方法 ====================

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }

    public String getRegulationRef() {
        return regulationRef;
    }

    public void setRegulationRef(String regulationRef) {
        this.regulationRef = regulationRef;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public LocalDateTime getEvaluateTime() {
        return evaluateTime;
    }

    public void setEvaluateTime(LocalDateTime evaluateTime) {
        this.evaluateTime = evaluateTime;
    }
}
