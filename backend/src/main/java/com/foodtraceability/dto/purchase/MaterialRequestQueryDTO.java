package com.foodtraceability.dto.purchase;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 物资需求提报查询 DTO（分页参数 + 过滤条件）
 *
 * <p>状态字段 status 为后端数字编码（0~4），由前端 DataConverter 转换。</p>
 */
@Schema(description = "物资需求提报查询 DTO")
public class MaterialRequestQueryDTO {

    /** 当前页码（默认1） */
    @Schema(description = "当前页码", example = "1")
    private Long current = 1L;

    /** 每页条数（默认10） */
    @Schema(description = "每页条数", example = "10")
    private Long size = 10L;

    /** 提报单号（模糊匹配） */
    @Schema(description = "提报单号")
    private String requestNo;

    /** 状态（后端数字编码 0~4） */
    @Schema(description = "状态")
    private Integer status;

    /** 提报门店名称 */
    @Schema(description = "提报门店")
    private String storeName;

    /** 申请人ID */
    @Schema(description = "申请人ID")
    private Long applicantId;

    /** 开始日期 */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "开始日期")
    private LocalDate startDate;

    /** 结束日期 */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "结束日期")
    private LocalDate endDate;

    /** 搜索关键词（提报单号/标题模糊匹配） */
    @Schema(description = "搜索关键词")
    private String keyword;

    // ==================== Getter & Setter ====================

    public Long getCurrent() {
        return current;
    }

    public void setCurrent(Long current) {
        this.current = current;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getRequestNo() {
        return requestNo;
    }

    public void setRequestNo(String requestNo) {
        this.requestNo = requestNo;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public Long getApplicantId() {
        return applicantId;
    }

    public void setApplicantId(Long applicantId) {
        this.applicantId = applicantId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
