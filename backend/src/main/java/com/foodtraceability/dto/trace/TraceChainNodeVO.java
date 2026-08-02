package com.foodtraceability.dto.trace;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 追溯链节点VO
 */
@Schema(description = "追溯链节点信息")
public class TraceChainNodeVO {

    /** 节点ID */
    @Schema(description = "节点ID")
    private Long nodeId;

    /** 序号 */
    @Schema(description = "序号")
    private Integer nodeSequence;

    /** 节点类型 */
    @Schema(description = "节点类型")
    private Integer nodeType;

    /** 节点类型名称 */
    @Schema(description = "节点类型名称")
    private String nodeTypeName;

    /** 事件时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "事件时间")
    private LocalDateTime eventTime;

    /** 地点 */
    @Schema(description = "地点")
    private String location;

    /** 操作人姓名 */
    @Schema(description = "操作人姓名")
    private String operatorName;

    /** 详细信息 */
    @Schema(description = "详细信息")
    private Object detailJson;

    /** 附件URL */
    @Schema(description = "附件URL")
    private String attachmentUrl;

    public Long getNodeId() { return nodeId; }
    public void setNodeId(Long nodeId) { this.nodeId = nodeId; }
    public Integer getNodeSequence() { return nodeSequence; }
    public void setNodeSequence(Integer nodeSequence) { this.nodeSequence = nodeSequence; }
    public Integer getNodeType() { return nodeType; }
    public void setNodeType(Integer nodeType) { this.nodeType = nodeType; }
    public String getNodeTypeName() { return nodeTypeName; }
    public void setNodeTypeName(String nodeTypeName) { this.nodeTypeName = nodeTypeName; }
    public LocalDateTime getEventTime() { return eventTime; }
    public void setEventTime(LocalDateTime eventTime) { this.eventTime = eventTime; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public Object getDetailJson() { return detailJson; }
    public void setDetailJson(Object detailJson) { this.detailJson = detailJson; }
    public String getAttachmentUrl() { return attachmentUrl; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }
}
