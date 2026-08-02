package com.foodtraceability.dto.purchase;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * 物资需求提报创建 DTO
 *
 * <p>requestNo 由后端自动生成（MR+yyyyMMdd+4位序号），不接受前端传入。</p>
 *
 * <p>金额单位：estimatedPrice/subtotalAmount 以"分"为单位（Long），由前端 DataConverter 转换。</p>
 */
@Schema(description = "物资需求提报创建 DTO")
public class MaterialRequestCreateDTO {

    /** 需求标题 */
    @NotBlank(message = "需求标题不能为空")
    @Size(min = 2, max = 100, message = "需求标题长度必须在2到100个字符之间")
    @Schema(description = "需求标题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    /** 提报门店名称 */
    @NotBlank(message = "提报门店不能为空")
    @Size(max = 100, message = "门店名称长度不能超过100")
    @Schema(description = "提报门店", requiredMode = Schema.RequiredMode.REQUIRED)
    private String storeName;

    /** 申请人ID（可选，由当前登录用户上下文传入） */
    @Schema(description = "申请人ID")
    private Long applicantId;

    /** 申请人姓名 */
    @Size(max = 50, message = "申请人姓名长度不能超过50")
    @Schema(description = "申请人姓名")
    private String applicantName;

    /** 期望到货日期 */
    @NotNull(message = "期望到货日期不能为空")
    @Schema(description = "期望到货日期", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate expectedDate;

    /** 备注 */
    @Size(max = 500, message = "备注长度不能超过500")
    @Schema(description = "备注")
    private String remark;

    /** 需求明细列表 */
    @NotEmpty(message = "需求明细不能为空")
    @Valid
    @Schema(description = "需求明细列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<MaterialRequestItemDTO> items;

    // ==================== Getter & Setter ====================

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public LocalDate getExpectedDate() {
        return expectedDate;
    }

    public void setExpectedDate(LocalDate expectedDate) {
        this.expectedDate = expectedDate;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public List<MaterialRequestItemDTO> getItems() {
        return items;
    }

    public void setItems(List<MaterialRequestItemDTO> items) {
        this.items = items;
    }
}
