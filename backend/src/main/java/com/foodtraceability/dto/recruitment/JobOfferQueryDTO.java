package com.foodtraceability.dto.recruitment;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 录用Offer分页查询DTO
 */
@Schema(description = "录用Offer分页查询DTO")
public class JobOfferQueryDTO {

    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Schema(description = "每页条数", example = "10")
    private Integer size = 10;

    @Schema(description = "招聘需求ID(VARCHAR(32)雪花)", example = "1789012345678901234")
    private String requirementId;

    @Schema(description = "候选人姓名(模糊搜索)", example = "张")
    private String candidateName;

    @Schema(description = "状态(pending/sent/accepted/onboarded/rejected/withdrawn)",
            example = "sent",
            allowableValues = {"pending", "sent", "accepted", "onboarded", "rejected", "withdrawn"})
    private String status;

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getRequirementId() {
        return requirementId;
    }

    public void setRequirementId(String requirementId) {
        this.requirementId = requirementId;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
