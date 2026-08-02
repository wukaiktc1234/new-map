package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 追溯链节点实体类
 * 记录追溯链条中的每个环节详情，包括采购、仓储、加工、销售等
 */
@TableName("trace_chain_nodes")
@Schema(description = "追溯链节点实体")
public class TraceChainNode {

    /** 主键ID */
    @TableId(value = "node_id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long nodeId;

    /** 关联追溯码ID */
    @TableField("trace_code_id")
    @Schema(description = "追溯码ID")
    private Long traceCodeId;

    /** 序号，从1开始 */
    @TableField("node_sequence")
    @Schema(description = "序号", example = "1")
    private Integer nodeSequence;

    /** 节点类型：1采购入库 2仓储入库 3加工制作 4出库发货 5上架销售 6检验检测 7消费者扫码 */
    @TableField("node_type")
    @Schema(description = "节点类型", example = "1")
    private Integer nodeType;

    /** 该环节发生时间 */
    @TableField("event_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "事件时间")
    private LocalDateTime eventTime;

    /** 地点描述 */
    @TableField("location")
    @Schema(description = "地点", example = "XX蔬菜批发市场")
    private String location;

    /** 操作人ID */
    @TableField("operator_id")
    @Schema(description = "操作人ID")
    private Long operatorId;

    /** 操作人姓名 */
    @TableField("operator_name")
    @Schema(description = "操作人姓名", example = "张三(采购员)")
    private String operatorName;

    /** 该环节详细信息JSON */
    @TableField("detail_json")
    @Schema(description = "详细信息JSON")
    private Object detailJson;

    /** 附件URL（质检照片/检验报告） */
    @TableField("attachment_url")
    @Schema(description = "附件URL")
    private String attachmentUrl;

    /** 经纬度（可选） */
    @TableField("geo_location")
    @Schema(description = "经纬度")
    private String geoLocation;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "删除标记")
    private Integer deleted;

    public Long getNodeId() {
        return nodeId;
    }

    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }

    public Long getTraceCodeId() {
        return traceCodeId;
    }

    public void setTraceCodeId(Long traceCodeId) {
        this.traceCodeId = traceCodeId;
    }

    public Integer getNodeSequence() {
        return nodeSequence;
    }

    public void setNodeSequence(Integer nodeSequence) {
        this.nodeSequence = nodeSequence;
    }

    public Integer getNodeType() {
        return nodeType;
    }

    public void setNodeType(Integer nodeType) {
        this.nodeType = nodeType;
    }

    public LocalDateTime getEventTime() {
        return eventTime;
    }

    public void setEventTime(LocalDateTime eventTime) {
        this.eventTime = eventTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public Object getDetailJson() {
        return detailJson;
    }

    public void setDetailJson(Object detailJson) {
        this.detailJson = detailJson;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }

    public String getGeoLocation() {
        return geoLocation;
    }

    public void setGeoLocation(String geoLocation) {
        this.geoLocation = geoLocation;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
