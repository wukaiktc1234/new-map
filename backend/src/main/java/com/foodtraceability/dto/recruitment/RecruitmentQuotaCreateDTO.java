package com.foodtraceability.dto.recruitment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 招聘名额创建DTO
 *
 * <p>HR 按年度/季度向门店下发招聘名额时使用。</p>
 */
@Schema(description = "招聘名额创建DTO")
public class RecruitmentQuotaCreateDTO {

    @NotNull(message = "年度不能为空")
    @Schema(description = "年度", example = "2026", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer year;

    @NotNull(message = "季度不能为空")
    @Min(value = 1, message = "季度最小为1")
    @Max(value = 4, message = "季度最大为4")
    @Schema(description = "季度(1-4)", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quarter;

    @NotNull(message = "门店ID不能为空")
    @Schema(description = "门店ID", example = "1001", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long storeId;

    @NotNull(message = "岗位ID不能为空")
    @Schema(description = "岗位ID", example = "2001", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long positionId;

    @NotNull(message = "名额数不能为空")
    @Min(value = 1, message = "名额数必须大于0")
    @Schema(description = "名额数", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer headcount;

    @Schema(description = "名额有效期", example = "2026-09-30")
    private LocalDate expireDate;

    @Size(max = 500, message = "备注长度不能超过500个字符")
    @Schema(description = "备注")
    private String remark;

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getQuarter() {
        return quarter;
    }

    public void setQuarter(Integer quarter) {
        this.quarter = quarter;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public Long getPositionId() {
        return positionId;
    }

    public void setPositionId(Long positionId) {
        this.positionId = positionId;
    }

    public Integer getHeadcount() {
        return headcount;
    }

    public void setHeadcount(Integer headcount) {
        this.headcount = headcount;
    }

    public LocalDate getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(LocalDate expireDate) {
        this.expireDate = expireDate;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
