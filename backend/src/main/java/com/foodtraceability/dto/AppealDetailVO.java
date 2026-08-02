package com.foodtraceability.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 申诉详情视图对象（VO）
 *
 * 包含完整信息、附件列表和处理日志
 */
public class AppealDetailVO extends AppealVO {

    /** 详细描述 */
    private String description;

    /** 关联处罚单号 */
    private String targetDecisionId;

    /** 期望结果 */
    private String expectedResult;

    /** 附件列表 */
    private List<AppealAttachmentVO> attachments;

    /** 处理日志列表 */
    private List<AppealProcessLogVO> processLogs;

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

    public List<AppealAttachmentVO> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<AppealAttachmentVO> attachments) {
        this.attachments = attachments;
    }

    public List<AppealProcessLogVO> getProcessLogs() {
        return processLogs;
    }

    public void setProcessLogs(List<AppealProcessLogVO> processLogs) {
        this.processLogs = processLogs;
    }
}
