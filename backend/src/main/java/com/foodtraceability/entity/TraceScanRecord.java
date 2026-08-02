package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 消费者扫码记录实体类
 * 记录消费者扫码查询追溯信息的全量行为数据
 */
@TableName("trace_scan_record")
@Schema(description = "消费者扫码记录实体")
public class TraceScanRecord {

    /** 主键ID */
    @TableId(value = "scan_id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long scanId;

    /** 追溯码ID */
    @TableField("trace_code_id")
    @Schema(description = "追溯码ID")
    private Long traceCodeId;

    /** 追溯码（冗余） */
    @TableField("trace_code")
    @Schema(description = "追溯码")
    private String traceCode;

    /** 扫码时间 */
    @TableField("scan_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "扫码时间")
    private LocalDateTime scanTime;

    /** 扫码位置（粗略描述） */
    @TableField("scan_location")
    @Schema(description = "扫码位置")
    private String scanLocation;

    /** 位置详情JSON：{province,city,district,address,lat,lng} */
    @TableField("scan_location_detail")
    @Schema(description = "位置详情JSON")
    private String scanLocationDetail;

    /** IP地址 */
    @TableField("ip_address")
    @Schema(description = "IP地址")
    private String ipAddress;

    /** User-Agent */
    @TableField("user_agent")
    @Schema(description = "User-Agent")
    private String userAgent;

    /** 设备类型：MOBILE/PC/TABLET */
    @TableField("device_type")
    @Schema(description = "设备类型", example = "MOBILE")
    private String deviceType;

    /** 该追溯码第几次扫码（从1开始） */
    @TableField("scan_count")
    @Schema(description = "该追溯码第几次扫码")
    private Integer scanCount;

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

    public Long getScanId() {
        return scanId;
    }

    public void setScanId(Long scanId) {
        this.scanId = scanId;
    }

    public Long getTraceCodeId() {
        return traceCodeId;
    }

    public void setTraceCodeId(Long traceCodeId) {
        this.traceCodeId = traceCodeId;
    }

    public String getTraceCode() {
        return traceCode;
    }

    public void setTraceCode(String traceCode) {
        this.traceCode = traceCode;
    }

    public LocalDateTime getScanTime() {
        return scanTime;
    }

    public void setScanTime(LocalDateTime scanTime) {
        this.scanTime = scanTime;
    }

    public String getScanLocation() {
        return scanLocation;
    }

    public void setScanLocation(String scanLocation) {
        this.scanLocation = scanLocation;
    }

    public String getScanLocationDetail() {
        return scanLocationDetail;
    }

    public void setScanLocationDetail(String scanLocationDetail) {
        this.scanLocationDetail = scanLocationDetail;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public Integer getScanCount() {
        return scanCount;
    }

    public void setScanCount(Integer scanCount) {
        this.scanCount = scanCount;
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
