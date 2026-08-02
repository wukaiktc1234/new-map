package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 食品溯源流程实体类
 * 记录食品从生产到销售的完整流程信息
 */
@TableName("food_trace")
@Schema(description = "食品溯源流程实体")
public class FoodTrace implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 溯源ID，主键
     */
    @TableId(value = "trace_id", type = IdType.ASSIGN_ID)
    @Schema(description = "溯源ID", example = "T001")
    private String traceId;
    /**
     * 食品ID，外键
     */
    @TableField("food_id")
    @Schema(description = "食品ID", example = "F001")
    private String foodId;
    /**
     * 食品名称
     */
    @TableField("food_name")
    @Schema(description = "食品名称", example = "有机苹果")
    private String foodName;
    /**
     * 批次号
     */
    @TableField("batch_number")
    @Schema(description = "批次号", example = "B20241201001")
    private String batchNumber;
    /**
     * 流程阶段（1-生产，2-加工，3-质检，4-包装，5-运输，6-仓储，7-销售）
     */
    @TableField("process_stage")
    @Schema(description = "流程阶段", example = "1")
    private Integer processStage;
    /**
     * 流程阶段名称
     */
    @TableField("stage_name")
    @Schema(description = "流程阶段名称", example = "生产阶段")
    private String stageName;
    /**
     * 操作类型（produce-生产，process-加工，inspect-质检，package-包装，transport-运输，store-仓储，sell-销售）
     */
    @TableField("operation_type")
    @Schema(description = "操作类型", example = "produce")
    private String operationType;
    /**
     * 操作描述
     */
    @TableField("operation_description")
    @Schema(description = "操作描述", example = "在果园进行采摘作业")
    private String operationDescription;
    /**
     * 操作时间
     */
    @TableField("operation_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "操作时间", example = "2024-12-01 08:00:00")
    private LocalDateTime operationTime;
    /**
     * 操作地点
     */
    @TableField("operation_location")
    @Schema(description = "操作地点", example = "山东省烟台市栖霞市果园")
    private String operationLocation;
    /**
     * 操作人
     */
    @TableField("operator")
    @Schema(description = "操作人", example = "张三")
    private String operator;
    /**
     * 操作人联系方式
     */
    @TableField("operator_contact")
    @Schema(description = "操作人联系方式", example = "13800138000")
    private String operatorContact;
    /**
     * 责任单位
     */
    @TableField("responsible_company")
    @Schema(description = "责任单位", example = "山东烟台果园有限公司")
    private String responsibleCompany;
    /**
     * 责任人工号
     */
    @TableField("responsible_employee_id")
    @Schema(description = "责任人工号", example = "EMP001")
    private String responsibleEmployeeId;
    /**
     * 责任人姓名
     */
    @TableField("responsible_name")
    @Schema(description = "责任人姓名", example = "李四")
    private String responsibleName;
    /**
     * 责任人联系方式
     */
    @TableField("responsible_contact")
    @Schema(description = "责任人联系方式", example = "13900139000")
    private String responsibleContact;
    /**
     * 温度（摄氏度，预留传感器数据）
     */
    @TableField("temperature")
    @Schema(description = "温度（摄氏度）", example = "25.5")
    private Double temperature;
    /**
     * 湿度（百分比，预留传感器数据）
     */
    @TableField("humidity")
    @Schema(description = "湿度（百分比）", example = "60.0")
    private Double humidity;
    /**
     * 环境数据（JSON格式，预留扩展）
     */
    @TableField("environment_data")
    @Schema(description = "环境数据", example = "{\"air_quality\":\"good\",\"pressure\":\"1013\"}")
    private String environmentData;
    /**
     * 质检结果（pass-合格，fail-不合格，pending-待检）
     */
    @TableField("quality_result")
    @Schema(description = "质检结果", example = "pass")
    private String qualityResult;
    /**
     * 质检报告编号
     */
    @TableField("quality_report_no")
    @Schema(description = "质检报告编号", example = "Q20241201001")
    private String qualityReportNo;
    /**
     * 质检报告附件URL
     */
    @TableField("quality_report_url")
    @Schema(description = "质检报告附件URL", example = "/uploads/quality/report001.pdf")
    private String qualityReportUrl;
    /**
     * 运输方式（truck-卡车，ship-船舶，plane-飞机，train-火车）
     */
    @TableField("transport_method")
    @Schema(description = "运输方式", example = "truck")
    private String transportMethod;
    /**
     * 运输工具编号
     */
    @TableField("transport_vehicle_no")
    @Schema(description = "运输工具编号", example = "鲁F12345")
    private String transportVehicleNo;
    /**
     * 运输起始地
     */
    @TableField("transport_origin")
    @Schema(description = "运输起始地", example = "山东烟台")
    private String transportOrigin;
    /**
     * 运输目的地
     */
    @TableField("transport_destination")
    @Schema(description = "运输目的地", example = "北京")
    private String transportDestination;
    /**
     * 运输开始时间
     */
    @TableField("transport_start_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "运输开始时间", example = "2024-12-01 10:00:00")
    private LocalDateTime transportStartTime;
    /**
     * 运输结束时间
     */
    @TableField("transport_end_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "运输结束时间", example = "2024-12-02 18:00:00")
    private LocalDateTime transportEndTime;
    /**
     * 仓储位置
     */
    @TableField("storage_location")
    @Schema(description = "仓储位置", example = "仓库A区-货架1-层1")
    private String storageLocation;
    /**
     * 仓储条件
     */
    @TableField("storage_conditions")
    @Schema(description = "仓储条件", example = "常温干燥")
    private String storageConditions;
    /**
     * 销售商名称
     */
    @TableField("seller_name")
    @Schema(description = "销售商名称", example = "华润万家超市")
    private String sellerName;
    /**
     * 销售商地址
     */
    @TableField("seller_address")
    @Schema(description = "销售商地址", example = "北京市朝阳区xxx路xxx号")
    private String sellerAddress;
    /**
     * 销售商联系方式
     */
    @TableField("seller_contact")
    @Schema(description = "销售商联系方式", example = "010-12345678")
    private String sellerContact;
    /**
     * 销售时间
     */
    @TableField("sale_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "销售时间", example = "2024-12-03 09:00:00")
    private LocalDateTime saleTime;
    /**
     * 销售价格
     */
    @TableField("sale_price")
    @Schema(description = "销售价格", example = "15.80")
    private Double salePrice;
    /**
     * 备注信息
     */
    @TableField("remarks")
    @Schema(description = "备注信息", example = "运输过程中注意防潮")
    private String remarks;
    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    /**
     * 创建人
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @Schema(description = "创建人", example = "admin")
    private String createBy;
    /**
     * 更新人
     */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新人", example = "admin")
    private String updateBy;
    /**
     * 逻辑删除标记（0-正常，1-删除）
     */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "删除标记", example = "0")
    private Integer deleted;
    /**
     * 设备相关字段（预留扩展）
     */
    @TableField("device_id")
    @Schema(description = "设备ID（预留）", example = "DEV001")
    private String deviceId;
    /**
     * RFID标签（预留扩展）
     */
    @TableField("rfid_tag")
    @Schema(description = "RFID标签（预留）", example = "RFID20241201001")
    private String rfidTag;
    /**
     * GPS位置信息（预留扩展）
     */
    @TableField("gps_location")
    @Schema(description = "GPS位置（预留）", example = "116.4074,39.9042")
    private String gpsLocation;
    /**
     * 传感器数据（JSON格式，预留扩展）
     */
    @TableField("sensor_data")
    @Schema(description = "传感器数据（预留）", example = "{\"temperature\":\"25\",\"humidity\":\"60\"}")
    private String sensorData;
    /**
     * 图片附件URLs（JSON数组格式）
     */
    @TableField("image_urls")
    @Schema(description = "图片附件URLs", example = "[\"/uploads/trace/img1.jpg\",\"/uploads/trace/img2.jpg\"]")
    private String imageUrls;
    /**
     * 描述信息
     */
    @TableField("description")
    @Schema(description = "描述信息", example = "苹果采摘，品质优良")
    private String description;

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public LocalDateTime getOperationTime() {
        return operationTime;
    }

    public void setOperationTime(LocalDateTime operationTime) {
        this.operationTime = operationTime;
    }

    public LocalDateTime getCreatedTime() {
        return createTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createTime = createdTime;
    }

    public String getFoodId() {
        return foodId;
    }

    public void setFoodId(String foodId) {
        this.foodId = foodId;
    }

    public String getStage() {
        return stageName;
    }

    public void setStage(String stage) {
        this.stageName = stage;
    }

    public LocalDateTime getUpdatedTime() {
        return updateTime;
    }

    public void setUpdatedTime(LocalDateTime updatedTime) {
        this.updateTime = updatedTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getOperationLocation() {
        return operationLocation;
    }

    public void setLocation(String location) {
        this.operationLocation = location;
    }

    public String getLocation() {
        return operationLocation;
    }

    public String getFoodName() {
        return foodName;
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public FoodTrace() {
    }

    /**
     * 流程阶段（1-生产，2-加工，3-质检，4-包装，5-运输，6-仓储，7-销售）
     */
    public Integer getProcessStage() {
        return this.processStage;
    }

    /**
     * 流程阶段名称
     */
    public String getStageName() {
        return this.stageName;
    }

    /**
     * 操作描述
     */
    public String getOperationDescription() {
        return this.operationDescription;
    }

    /**
     * 操作人联系方式
     */
    public String getOperatorContact() {
        return this.operatorContact;
    }

    /**
     * 责任单位
     */
    public String getResponsibleCompany() {
        return this.responsibleCompany;
    }

    /**
     * 责任人工号
     */
    public String getResponsibleEmployeeId() {
        return this.responsibleEmployeeId;
    }

    /**
     * 责任人姓名
     */
    public String getResponsibleName() {
        return this.responsibleName;
    }

    /**
     * 责任人联系方式
     */
    public String getResponsibleContact() {
        return this.responsibleContact;
    }

    /**
     * 温度（摄氏度，预留传感器数据）
     */
    public Double getTemperature() {
        return this.temperature;
    }

    /**
     * 湿度（百分比，预留传感器数据）
     */
    public Double getHumidity() {
        return this.humidity;
    }

    /**
     * 环境数据（JSON格式，预留扩展）
     */
    public String getEnvironmentData() {
        return this.environmentData;
    }

    /**
     * 质检结果（pass-合格，fail-不合格，pending-待检）
     */
    public String getQualityResult() {
        return this.qualityResult;
    }

    /**
     * 质检报告编号
     */
    public String getQualityReportNo() {
        return this.qualityReportNo;
    }

    /**
     * 质检报告附件URL
     */
    public String getQualityReportUrl() {
        return this.qualityReportUrl;
    }

    /**
     * 运输方式（truck-卡车，ship-船舶，plane-飞机，train-火车）
     */
    public String getTransportMethod() {
        return this.transportMethod;
    }

    /**
     * 运输工具编号
     */
    public String getTransportVehicleNo() {
        return this.transportVehicleNo;
    }

    /**
     * 运输起始地
     */
    public String getTransportOrigin() {
        return this.transportOrigin;
    }

    /**
     * 运输目的地
     */
    public String getTransportDestination() {
        return this.transportDestination;
    }

    /**
     * 运输开始时间
     */
    public LocalDateTime getTransportStartTime() {
        return this.transportStartTime;
    }

    /**
     * 运输结束时间
     */
    public LocalDateTime getTransportEndTime() {
        return this.transportEndTime;
    }

    /**
     * 仓储位置
     */
    public String getStorageLocation() {
        return this.storageLocation;
    }

    /**
     * 仓储条件
     */
    public String getStorageConditions() {
        return this.storageConditions;
    }

    /**
     * 销售商名称
     */
    public String getSellerName() {
        return this.sellerName;
    }

    /**
     * 销售商地址
     */
    public String getSellerAddress() {
        return this.sellerAddress;
    }

    /**
     * 销售商联系方式
     */
    public String getSellerContact() {
        return this.sellerContact;
    }

    /**
     * 销售时间
     */
    public LocalDateTime getSaleTime() {
        return this.saleTime;
    }

    /**
     * 销售价格
     */
    public Double getSalePrice() {
        return this.salePrice;
    }

    /**
     * 备注信息
     */
    public String getRemarks() {
        return this.remarks;
    }

    /**
     * 创建时间
     */
    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    /**
     * 更新时间
     */
    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    /**
     * 创建人
     */
    public String getCreateBy() {
        return this.createBy;
    }

    /**
     * 更新人
     */
    public String getUpdateBy() {
        return this.updateBy;
    }

    /**
     * 逻辑删除标记（0-正常，1-删除）
     */
    public Integer getDeleted() {
        return this.deleted;
    }

    /**
     * 设备相关字段（预留扩展）
     */
    public String getDeviceId() {
        return this.deviceId;
    }

    /**
     * RFID标签（预留扩展）
     */
    public String getRfidTag() {
        return this.rfidTag;
    }

    /**
     * GPS位置信息（预留扩展）
     */
    public String getGpsLocation() {
        return this.gpsLocation;
    }

    /**
     * 传感器数据（JSON格式，预留扩展）
     */
    public String getSensorData() {
        return this.sensorData;
    }

    /**
     * 图片附件URLs（JSON数组格式）
     */
    public String getImageUrls() {
        return this.imageUrls;
    }

    /**
     * 流程阶段（1-生产，2-加工，3-质检，4-包装，5-运输，6-仓储，7-销售）
     */
    public void setProcessStage(final Integer processStage) {
        this.processStage = processStage;
    }

    /**
     * 流程阶段名称
     */
    public void setStageName(final String stageName) {
        this.stageName = stageName;
    }

    /**
     * 操作描述
     */
    public void setOperationDescription(final String operationDescription) {
        this.operationDescription = operationDescription;
    }

    /**
     * 操作地点
     */
    public void setOperationLocation(final String operationLocation) {
        this.operationLocation = operationLocation;
    }

    /**
     * 操作人联系方式
     */
    public void setOperatorContact(final String operatorContact) {
        this.operatorContact = operatorContact;
    }

    /**
     * 责任单位
     */
    public void setResponsibleCompany(final String responsibleCompany) {
        this.responsibleCompany = responsibleCompany;
    }

    /**
     * 责任人工号
     */
    public void setResponsibleEmployeeId(final String responsibleEmployeeId) {
        this.responsibleEmployeeId = responsibleEmployeeId;
    }

    /**
     * 责任人姓名
     */
    public void setResponsibleName(final String responsibleName) {
        this.responsibleName = responsibleName;
    }

    /**
     * 责任人联系方式
     */
    public void setResponsibleContact(final String responsibleContact) {
        this.responsibleContact = responsibleContact;
    }

    /**
     * 温度（摄氏度，预留传感器数据）
     */
    public void setTemperature(final Double temperature) {
        this.temperature = temperature;
    }

    /**
     * 湿度（百分比，预留传感器数据）
     */
    public void setHumidity(final Double humidity) {
        this.humidity = humidity;
    }

    /**
     * 环境数据（JSON格式，预留扩展）
     */
    public void setEnvironmentData(final String environmentData) {
        this.environmentData = environmentData;
    }

    /**
     * 质检结果（pass-合格，fail-不合格，pending-待检）
     */
    public void setQualityResult(final String qualityResult) {
        this.qualityResult = qualityResult;
    }

    /**
     * 质检报告编号
     */
    public void setQualityReportNo(final String qualityReportNo) {
        this.qualityReportNo = qualityReportNo;
    }

    /**
     * 质检报告附件URL
     */
    public void setQualityReportUrl(final String qualityReportUrl) {
        this.qualityReportUrl = qualityReportUrl;
    }

    /**
     * 运输方式（truck-卡车，ship-船舶，plane-飞机，train-火车）
     */
    public void setTransportMethod(final String transportMethod) {
        this.transportMethod = transportMethod;
    }

    /**
     * 运输工具编号
     */
    public void setTransportVehicleNo(final String transportVehicleNo) {
        this.transportVehicleNo = transportVehicleNo;
    }

    /**
     * 运输起始地
     */
    public void setTransportOrigin(final String transportOrigin) {
        this.transportOrigin = transportOrigin;
    }

    /**
     * 运输目的地
     */
    public void setTransportDestination(final String transportDestination) {
        this.transportDestination = transportDestination;
    }

    /**
     * 运输开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setTransportStartTime(final LocalDateTime transportStartTime) {
        this.transportStartTime = transportStartTime;
    }

    /**
     * 运输结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setTransportEndTime(final LocalDateTime transportEndTime) {
        this.transportEndTime = transportEndTime;
    }

    /**
     * 仓储位置
     */
    public void setStorageLocation(final String storageLocation) {
        this.storageLocation = storageLocation;
    }

    /**
     * 仓储条件
     */
    public void setStorageConditions(final String storageConditions) {
        this.storageConditions = storageConditions;
    }

    /**
     * 销售商名称
     */
    public void setSellerName(final String sellerName) {
        this.sellerName = sellerName;
    }

    /**
     * 销售商地址
     */
    public void setSellerAddress(final String sellerAddress) {
        this.sellerAddress = sellerAddress;
    }

    /**
     * 销售商联系方式
     */
    public void setSellerContact(final String sellerContact) {
        this.sellerContact = sellerContact;
    }

    /**
     * 销售时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setSaleTime(final LocalDateTime saleTime) {
        this.saleTime = saleTime;
    }

    /**
     * 销售价格
     */
    public void setSalePrice(final Double salePrice) {
        this.salePrice = salePrice;
    }

    /**
     * 备注信息
     */
    public void setRemarks(final String remarks) {
        this.remarks = remarks;
    }

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    /**
     * 创建人
     */
    public void setCreateBy(final String createBy) {
        this.createBy = createBy;
    }

    /**
     * 更新人
     */
    public void setUpdateBy(final String updateBy) {
        this.updateBy = updateBy;
    }

    /**
     * 逻辑删除标记（0-正常，1-删除）
     */
    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    /**
     * 设备相关字段（预留扩展）
     */
    public void setDeviceId(final String deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * RFID标签（预留扩展）
     */
    public void setRfidTag(final String rfidTag) {
        this.rfidTag = rfidTag;
    }

    /**
     * GPS位置信息（预留扩展）
     */
    public void setGpsLocation(final String gpsLocation) {
        this.gpsLocation = gpsLocation;
    }

    /**
     * 传感器数据（JSON格式，预留扩展）
     */
    public void setSensorData(final String sensorData) {
        this.sensorData = sensorData;
    }

    /**
     * 图片附件URLs（JSON数组格式）
     */
    public void setImageUrls(final String imageUrls) {
        this.imageUrls = imageUrls;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "FoodTrace(traceId=" + this.getTraceId() + ", foodId=" + this.getFoodId() + ", foodName=" + this.getFoodName() + ", batchNumber=" + this.getBatchNumber() + ", processStage=" + this.getProcessStage() + ", stageName=" + this.getStageName() + ", operationType=" + this.getOperationType() + ", operationDescription=" + this.getOperationDescription() + ", operationTime=" + this.getOperationTime() + ", operationLocation=" + this.getOperationLocation() + ", operator=" + this.getOperator() + ", operatorContact=" + this.getOperatorContact() + ", responsibleCompany=" + this.getResponsibleCompany() + ", responsibleEmployeeId=" + this.getResponsibleEmployeeId() + ", responsibleName=" + this.getResponsibleName() + ", responsibleContact=" + this.getResponsibleContact() + ", temperature=" + this.getTemperature() + ", humidity=" + this.getHumidity() + ", environmentData=" + this.getEnvironmentData() + ", qualityResult=" + this.getQualityResult() + ", qualityReportNo=" + this.getQualityReportNo() + ", qualityReportUrl=" + this.getQualityReportUrl() + ", transportMethod=" + this.getTransportMethod() + ", transportVehicleNo=" + this.getTransportVehicleNo() + ", transportOrigin=" + this.getTransportOrigin() + ", transportDestination=" + this.getTransportDestination() + ", transportStartTime=" + this.getTransportStartTime() + ", transportEndTime=" + this.getTransportEndTime() + ", storageLocation=" + this.getStorageLocation() + ", storageConditions=" + this.getStorageConditions() + ", sellerName=" + this.getSellerName() + ", sellerAddress=" + this.getSellerAddress() + ", sellerContact=" + this.getSellerContact() + ", saleTime=" + this.getSaleTime() + ", salePrice=" + this.getSalePrice() + ", remarks=" + this.getRemarks() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ", createBy=" + this.getCreateBy() + ", updateBy=" + this.getUpdateBy() + ", deleted=" + this.getDeleted() + ", deviceId=" + this.getDeviceId() + ", rfidTag=" + this.getRfidTag() + ", gpsLocation=" + this.getGpsLocation() + ", sensorData=" + this.getSensorData() + ", imageUrls=" + this.getImageUrls() + ", description=" + this.getDescription() + ")";
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof FoodTrace)) return false;
        final FoodTrace other = (FoodTrace) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$processStage = this.getProcessStage();
        final java.lang.Object other$processStage = other.getProcessStage();
        if (this$processStage == null ? other$processStage != null : !this$processStage.equals(other$processStage)) return false;
        final java.lang.Object this$temperature = this.getTemperature();
        final java.lang.Object other$temperature = other.getTemperature();
        if (this$temperature == null ? other$temperature != null : !this$temperature.equals(other$temperature)) return false;
        final java.lang.Object this$humidity = this.getHumidity();
        final java.lang.Object other$humidity = other.getHumidity();
        if (this$humidity == null ? other$humidity != null : !this$humidity.equals(other$humidity)) return false;
        final java.lang.Object this$salePrice = this.getSalePrice();
        final java.lang.Object other$salePrice = other.getSalePrice();
        if (this$salePrice == null ? other$salePrice != null : !this$salePrice.equals(other$salePrice)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$traceId = this.getTraceId();
        final java.lang.Object other$traceId = other.getTraceId();
        if (this$traceId == null ? other$traceId != null : !this$traceId.equals(other$traceId)) return false;
        final java.lang.Object this$foodId = this.getFoodId();
        final java.lang.Object other$foodId = other.getFoodId();
        if (this$foodId == null ? other$foodId != null : !this$foodId.equals(other$foodId)) return false;
        final java.lang.Object this$foodName = this.getFoodName();
        final java.lang.Object other$foodName = other.getFoodName();
        if (this$foodName == null ? other$foodName != null : !this$foodName.equals(other$foodName)) return false;
        final java.lang.Object this$batchNumber = this.getBatchNumber();
        final java.lang.Object other$batchNumber = other.getBatchNumber();
        if (this$batchNumber == null ? other$batchNumber != null : !this$batchNumber.equals(other$batchNumber)) return false;
        final java.lang.Object this$stageName = this.getStageName();
        final java.lang.Object other$stageName = other.getStageName();
        if (this$stageName == null ? other$stageName != null : !this$stageName.equals(other$stageName)) return false;
        final java.lang.Object this$operationType = this.getOperationType();
        final java.lang.Object other$operationType = other.getOperationType();
        if (this$operationType == null ? other$operationType != null : !this$operationType.equals(other$operationType)) return false;
        final java.lang.Object this$operationDescription = this.getOperationDescription();
        final java.lang.Object other$operationDescription = other.getOperationDescription();
        if (this$operationDescription == null ? other$operationDescription != null : !this$operationDescription.equals(other$operationDescription)) return false;
        final java.lang.Object this$operationTime = this.getOperationTime();
        final java.lang.Object other$operationTime = other.getOperationTime();
        if (this$operationTime == null ? other$operationTime != null : !this$operationTime.equals(other$operationTime)) return false;
        final java.lang.Object this$operationLocation = this.getOperationLocation();
        final java.lang.Object other$operationLocation = other.getOperationLocation();
        if (this$operationLocation == null ? other$operationLocation != null : !this$operationLocation.equals(other$operationLocation)) return false;
        final java.lang.Object this$operator = this.getOperator();
        final java.lang.Object other$operator = other.getOperator();
        if (this$operator == null ? other$operator != null : !this$operator.equals(other$operator)) return false;
        final java.lang.Object this$operatorContact = this.getOperatorContact();
        final java.lang.Object other$operatorContact = other.getOperatorContact();
        if (this$operatorContact == null ? other$operatorContact != null : !this$operatorContact.equals(other$operatorContact)) return false;
        final java.lang.Object this$responsibleCompany = this.getResponsibleCompany();
        final java.lang.Object other$responsibleCompany = other.getResponsibleCompany();
        if (this$responsibleCompany == null ? other$responsibleCompany != null : !this$responsibleCompany.equals(other$responsibleCompany)) return false;
        final java.lang.Object this$responsibleEmployeeId = this.getResponsibleEmployeeId();
        final java.lang.Object other$responsibleEmployeeId = other.getResponsibleEmployeeId();
        if (this$responsibleEmployeeId == null ? other$responsibleEmployeeId != null : !this$responsibleEmployeeId.equals(other$responsibleEmployeeId)) return false;
        final java.lang.Object this$responsibleName = this.getResponsibleName();
        final java.lang.Object other$responsibleName = other.getResponsibleName();
        if (this$responsibleName == null ? other$responsibleName != null : !this$responsibleName.equals(other$responsibleName)) return false;
        final java.lang.Object this$responsibleContact = this.getResponsibleContact();
        final java.lang.Object other$responsibleContact = other.getResponsibleContact();
        if (this$responsibleContact == null ? other$responsibleContact != null : !this$responsibleContact.equals(other$responsibleContact)) return false;
        final java.lang.Object this$environmentData = this.getEnvironmentData();
        final java.lang.Object other$environmentData = other.getEnvironmentData();
        if (this$environmentData == null ? other$environmentData != null : !this$environmentData.equals(other$environmentData)) return false;
        final java.lang.Object this$qualityResult = this.getQualityResult();
        final java.lang.Object other$qualityResult = other.getQualityResult();
        if (this$qualityResult == null ? other$qualityResult != null : !this$qualityResult.equals(other$qualityResult)) return false;
        final java.lang.Object this$qualityReportNo = this.getQualityReportNo();
        final java.lang.Object other$qualityReportNo = other.getQualityReportNo();
        if (this$qualityReportNo == null ? other$qualityReportNo != null : !this$qualityReportNo.equals(other$qualityReportNo)) return false;
        final java.lang.Object this$qualityReportUrl = this.getQualityReportUrl();
        final java.lang.Object other$qualityReportUrl = other.getQualityReportUrl();
        if (this$qualityReportUrl == null ? other$qualityReportUrl != null : !this$qualityReportUrl.equals(other$qualityReportUrl)) return false;
        final java.lang.Object this$transportMethod = this.getTransportMethod();
        final java.lang.Object other$transportMethod = other.getTransportMethod();
        if (this$transportMethod == null ? other$transportMethod != null : !this$transportMethod.equals(other$transportMethod)) return false;
        final java.lang.Object this$transportVehicleNo = this.getTransportVehicleNo();
        final java.lang.Object other$transportVehicleNo = other.getTransportVehicleNo();
        if (this$transportVehicleNo == null ? other$transportVehicleNo != null : !this$transportVehicleNo.equals(other$transportVehicleNo)) return false;
        final java.lang.Object this$transportOrigin = this.getTransportOrigin();
        final java.lang.Object other$transportOrigin = other.getTransportOrigin();
        if (this$transportOrigin == null ? other$transportOrigin != null : !this$transportOrigin.equals(other$transportOrigin)) return false;
        final java.lang.Object this$transportDestination = this.getTransportDestination();
        final java.lang.Object other$transportDestination = other.getTransportDestination();
        if (this$transportDestination == null ? other$transportDestination != null : !this$transportDestination.equals(other$transportDestination)) return false;
        final java.lang.Object this$transportStartTime = this.getTransportStartTime();
        final java.lang.Object other$transportStartTime = other.getTransportStartTime();
        if (this$transportStartTime == null ? other$transportStartTime != null : !this$transportStartTime.equals(other$transportStartTime)) return false;
        final java.lang.Object this$transportEndTime = this.getTransportEndTime();
        final java.lang.Object other$transportEndTime = other.getTransportEndTime();
        if (this$transportEndTime == null ? other$transportEndTime != null : !this$transportEndTime.equals(other$transportEndTime)) return false;
        final java.lang.Object this$storageLocation = this.getStorageLocation();
        final java.lang.Object other$storageLocation = other.getStorageLocation();
        if (this$storageLocation == null ? other$storageLocation != null : !this$storageLocation.equals(other$storageLocation)) return false;
        final java.lang.Object this$storageConditions = this.getStorageConditions();
        final java.lang.Object other$storageConditions = other.getStorageConditions();
        if (this$storageConditions == null ? other$storageConditions != null : !this$storageConditions.equals(other$storageConditions)) return false;
        final java.lang.Object this$sellerName = this.getSellerName();
        final java.lang.Object other$sellerName = other.getSellerName();
        if (this$sellerName == null ? other$sellerName != null : !this$sellerName.equals(other$sellerName)) return false;
        final java.lang.Object this$sellerAddress = this.getSellerAddress();
        final java.lang.Object other$sellerAddress = other.getSellerAddress();
        if (this$sellerAddress == null ? other$sellerAddress != null : !this$sellerAddress.equals(other$sellerAddress)) return false;
        final java.lang.Object this$sellerContact = this.getSellerContact();
        final java.lang.Object other$sellerContact = other.getSellerContact();
        if (this$sellerContact == null ? other$sellerContact != null : !this$sellerContact.equals(other$sellerContact)) return false;
        final java.lang.Object this$saleTime = this.getSaleTime();
        final java.lang.Object other$saleTime = other.getSaleTime();
        if (this$saleTime == null ? other$saleTime != null : !this$saleTime.equals(other$saleTime)) return false;
        final java.lang.Object this$remarks = this.getRemarks();
        final java.lang.Object other$remarks = other.getRemarks();
        if (this$remarks == null ? other$remarks != null : !this$remarks.equals(other$remarks)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime)) return false;
        final java.lang.Object this$createBy = this.getCreateBy();
        final java.lang.Object other$createBy = other.getCreateBy();
        if (this$createBy == null ? other$createBy != null : !this$createBy.equals(other$createBy)) return false;
        final java.lang.Object this$updateBy = this.getUpdateBy();
        final java.lang.Object other$updateBy = other.getUpdateBy();
        if (this$updateBy == null ? other$updateBy != null : !this$updateBy.equals(other$updateBy)) return false;
        final java.lang.Object this$deviceId = this.getDeviceId();
        final java.lang.Object other$deviceId = other.getDeviceId();
        if (this$deviceId == null ? other$deviceId != null : !this$deviceId.equals(other$deviceId)) return false;
        final java.lang.Object this$rfidTag = this.getRfidTag();
        final java.lang.Object other$rfidTag = other.getRfidTag();
        if (this$rfidTag == null ? other$rfidTag != null : !this$rfidTag.equals(other$rfidTag)) return false;
        final java.lang.Object this$gpsLocation = this.getGpsLocation();
        final java.lang.Object other$gpsLocation = other.getGpsLocation();
        if (this$gpsLocation == null ? other$gpsLocation != null : !this$gpsLocation.equals(other$gpsLocation)) return false;
        final java.lang.Object this$sensorData = this.getSensorData();
        final java.lang.Object other$sensorData = other.getSensorData();
        if (this$sensorData == null ? other$sensorData != null : !this$sensorData.equals(other$sensorData)) return false;
        final java.lang.Object this$imageUrls = this.getImageUrls();
        final java.lang.Object other$imageUrls = other.getImageUrls();
        if (this$imageUrls == null ? other$imageUrls != null : !this$imageUrls.equals(other$imageUrls)) return false;
        final java.lang.Object this$description = this.getDescription();
        final java.lang.Object other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof FoodTrace;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $processStage = this.getProcessStage();
        result = result * PRIME + ($processStage == null ? 43 : $processStage.hashCode());
        final java.lang.Object $temperature = this.getTemperature();
        result = result * PRIME + ($temperature == null ? 43 : $temperature.hashCode());
        final java.lang.Object $humidity = this.getHumidity();
        result = result * PRIME + ($humidity == null ? 43 : $humidity.hashCode());
        final java.lang.Object $salePrice = this.getSalePrice();
        result = result * PRIME + ($salePrice == null ? 43 : $salePrice.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $traceId = this.getTraceId();
        result = result * PRIME + ($traceId == null ? 43 : $traceId.hashCode());
        final java.lang.Object $foodId = this.getFoodId();
        result = result * PRIME + ($foodId == null ? 43 : $foodId.hashCode());
        final java.lang.Object $foodName = this.getFoodName();
        result = result * PRIME + ($foodName == null ? 43 : $foodName.hashCode());
        final java.lang.Object $batchNumber = this.getBatchNumber();
        result = result * PRIME + ($batchNumber == null ? 43 : $batchNumber.hashCode());
        final java.lang.Object $stageName = this.getStageName();
        result = result * PRIME + ($stageName == null ? 43 : $stageName.hashCode());
        final java.lang.Object $operationType = this.getOperationType();
        result = result * PRIME + ($operationType == null ? 43 : $operationType.hashCode());
        final java.lang.Object $operationDescription = this.getOperationDescription();
        result = result * PRIME + ($operationDescription == null ? 43 : $operationDescription.hashCode());
        final java.lang.Object $operationTime = this.getOperationTime();
        result = result * PRIME + ($operationTime == null ? 43 : $operationTime.hashCode());
        final java.lang.Object $operationLocation = this.getOperationLocation();
        result = result * PRIME + ($operationLocation == null ? 43 : $operationLocation.hashCode());
        final java.lang.Object $operator = this.getOperator();
        result = result * PRIME + ($operator == null ? 43 : $operator.hashCode());
        final java.lang.Object $operatorContact = this.getOperatorContact();
        result = result * PRIME + ($operatorContact == null ? 43 : $operatorContact.hashCode());
        final java.lang.Object $responsibleCompany = this.getResponsibleCompany();
        result = result * PRIME + ($responsibleCompany == null ? 43 : $responsibleCompany.hashCode());
        final java.lang.Object $responsibleEmployeeId = this.getResponsibleEmployeeId();
        result = result * PRIME + ($responsibleEmployeeId == null ? 43 : $responsibleEmployeeId.hashCode());
        final java.lang.Object $responsibleName = this.getResponsibleName();
        result = result * PRIME + ($responsibleName == null ? 43 : $responsibleName.hashCode());
        final java.lang.Object $responsibleContact = this.getResponsibleContact();
        result = result * PRIME + ($responsibleContact == null ? 43 : $responsibleContact.hashCode());
        final java.lang.Object $environmentData = this.getEnvironmentData();
        result = result * PRIME + ($environmentData == null ? 43 : $environmentData.hashCode());
        final java.lang.Object $qualityResult = this.getQualityResult();
        result = result * PRIME + ($qualityResult == null ? 43 : $qualityResult.hashCode());
        final java.lang.Object $qualityReportNo = this.getQualityReportNo();
        result = result * PRIME + ($qualityReportNo == null ? 43 : $qualityReportNo.hashCode());
        final java.lang.Object $qualityReportUrl = this.getQualityReportUrl();
        result = result * PRIME + ($qualityReportUrl == null ? 43 : $qualityReportUrl.hashCode());
        final java.lang.Object $transportMethod = this.getTransportMethod();
        result = result * PRIME + ($transportMethod == null ? 43 : $transportMethod.hashCode());
        final java.lang.Object $transportVehicleNo = this.getTransportVehicleNo();
        result = result * PRIME + ($transportVehicleNo == null ? 43 : $transportVehicleNo.hashCode());
        final java.lang.Object $transportOrigin = this.getTransportOrigin();
        result = result * PRIME + ($transportOrigin == null ? 43 : $transportOrigin.hashCode());
        final java.lang.Object $transportDestination = this.getTransportDestination();
        result = result * PRIME + ($transportDestination == null ? 43 : $transportDestination.hashCode());
        final java.lang.Object $transportStartTime = this.getTransportStartTime();
        result = result * PRIME + ($transportStartTime == null ? 43 : $transportStartTime.hashCode());
        final java.lang.Object $transportEndTime = this.getTransportEndTime();
        result = result * PRIME + ($transportEndTime == null ? 43 : $transportEndTime.hashCode());
        final java.lang.Object $storageLocation = this.getStorageLocation();
        result = result * PRIME + ($storageLocation == null ? 43 : $storageLocation.hashCode());
        final java.lang.Object $storageConditions = this.getStorageConditions();
        result = result * PRIME + ($storageConditions == null ? 43 : $storageConditions.hashCode());
        final java.lang.Object $sellerName = this.getSellerName();
        result = result * PRIME + ($sellerName == null ? 43 : $sellerName.hashCode());
        final java.lang.Object $sellerAddress = this.getSellerAddress();
        result = result * PRIME + ($sellerAddress == null ? 43 : $sellerAddress.hashCode());
        final java.lang.Object $sellerContact = this.getSellerContact();
        result = result * PRIME + ($sellerContact == null ? 43 : $sellerContact.hashCode());
        final java.lang.Object $saleTime = this.getSaleTime();
        result = result * PRIME + ($saleTime == null ? 43 : $saleTime.hashCode());
        final java.lang.Object $remarks = this.getRemarks();
        result = result * PRIME + ($remarks == null ? 43 : $remarks.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        final java.lang.Object $createBy = this.getCreateBy();
        result = result * PRIME + ($createBy == null ? 43 : $createBy.hashCode());
        final java.lang.Object $updateBy = this.getUpdateBy();
        result = result * PRIME + ($updateBy == null ? 43 : $updateBy.hashCode());
        final java.lang.Object $deviceId = this.getDeviceId();
        result = result * PRIME + ($deviceId == null ? 43 : $deviceId.hashCode());
        final java.lang.Object $rfidTag = this.getRfidTag();
        result = result * PRIME + ($rfidTag == null ? 43 : $rfidTag.hashCode());
        final java.lang.Object $gpsLocation = this.getGpsLocation();
        result = result * PRIME + ($gpsLocation == null ? 43 : $gpsLocation.hashCode());
        final java.lang.Object $sensorData = this.getSensorData();
        result = result * PRIME + ($sensorData == null ? 43 : $sensorData.hashCode());
        final java.lang.Object $imageUrls = this.getImageUrls();
        result = result * PRIME + ($imageUrls == null ? 43 : $imageUrls.hashCode());
        final java.lang.Object $description = this.getDescription();
        result = result * PRIME + ($description == null ? 43 : $description.hashCode());
        return result;
    }
}
