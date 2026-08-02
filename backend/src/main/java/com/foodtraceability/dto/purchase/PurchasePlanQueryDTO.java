package com.foodtraceability.dto.purchase;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 采购计划查询 DTO（分页参数继承自父接口的 getCurrent/getSize）
 *
 * <p>状态字段 status 为后端数字编码（0~5），由前端 DataConverter 转换。</p>
 */
@Schema(description = "采购计划查询 DTO")
public class PurchasePlanQueryDTO {

    /** 当前页码（默认1） */
    @Schema(description = "当前页码", example = "1")
    private Long current = 1L;

    /** 每页条数（默认10） */
    @Schema(description = "每页条数", example = "10")
    private Long size = 10L;

    /** 计划编号（模糊匹配） */
    @Schema(description = "计划编号")
    private String planNo;

    /** 状态（后端数字编码 0~5） */
    @Schema(description = "状态")
    private Integer status;

    /** 部门ID */
    @Schema(description = "部门ID")
    private Long departmentId;

    /** 开始日期 */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "开始日期")
    private LocalDate startDate;

    /** 结束日期 */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "结束日期")
    private LocalDate endDate;

    /** 搜索关键词（计划编号或备注模糊匹配） */
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

    public String getPlanNo() {
        return planNo;
    }

    public void setPlanNo(String planNo) {
        this.planNo = planNo;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
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
