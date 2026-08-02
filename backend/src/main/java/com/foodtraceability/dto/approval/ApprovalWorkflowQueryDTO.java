package com.foodtraceability.dto.approval;

/**
 * 审批流程查询 DTO
 */
public class ApprovalWorkflowQueryDTO {

    /** 当前页码（默认1） */
    private Integer page = 1;

    /** 每页大小（默认10） */
    private Integer size = 10;

    /** 业务类型 */
    private String businessType;

    /** 权限模板编码 */
    private String templateCode;

    /** 是否启用（前端传入字符串"true"/"false"或布尔值） */
    private Boolean enabled;

    /** 关键词搜索（按流程名称模糊匹配） */
    private String keyword;

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

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
