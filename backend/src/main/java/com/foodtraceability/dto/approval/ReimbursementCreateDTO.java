package com.foodtraceability.dto.approval;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 报销申请创建DTO
 * 用于报销类型审批的详细表单数据
 * 包含报销项目列表、类别、收款账号、关联出差等信息
 */
@Schema(description = "报销申请创建DTO")
public class ReimbursementCreateDTO {

    /**
     * 报销项目列表
     * 至少包含1个项目，最多20个项目
     * 每个项目包含费用类别、金额、票据号等详细信息
     */
    @NotNull(message = "报销项目列表不能为空")
    @Size(min = 1, max = 20, message = "报销项目数量必须在1-20之间")
    @Schema(description = "报销项目列表（1-20项）",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ReimbursementItemDTO> items;

    /**
     * 报销类别
     * travel=差旅, office=办公, entertainment=招待, communication=通讯, other=其他
     */
    @NotBlank(message = "报销类别不能为空")
    @Pattern(regexp = "^(travel|office|entertainment|communication|other)$",
             message = "报销类别不合法")
    @Schema(description = "报销类别", example = "travel",
            allowableValues = {"travel", "office", "entertainment", "communication", "other"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String reimbursementCategory;

    /** 报销说明 */
    @Size(max = 1000, message = "报销说明长度不能超过1000个字符")
    @Schema(description = "报销说明", example = "参加上海餐饮展会的差旅费用报销")
    private String description;

    /** 收款银行账号 */
    @NotBlank(message = "收款银行账号不能为空")
    @Size(max = 50, message = "银行账号长度不能超过50个字符")
    @Schema(description = "收款银行账号", example = "622848****1234",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String bankAccount;

    /** 收款人姓名 */
    @NotBlank(message = "收款人姓名不能为空")
    @Size(max = 50, message = "收款人姓名长度不能超过50个字符")
    @Schema(description = "收款人姓名", example = "张三",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String payeeName;

    /** 关联出差申请ID（差旅报销时填写） */
    @Schema(description = "关联出差申请ID", example = "travel001")
    private String relatedTravelId;

    // ==================== Getter & Setter 方法 ====================

    public List<ReimbursementItemDTO> getItems() {
        return items;
    }

    public void setItems(List<ReimbursementItemDTO> items) {
        this.items = items;
    }

    public String getReimbursementCategory() {
        return reimbursementCategory;
    }

    public void setReimbursementCategory(String reimbursementCategory) {
        this.reimbursementCategory = reimbursementCategory;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(String bankAccount) {
        this.bankAccount = bankAccount;
    }

    public String getPayeeName() {
        return payeeName;
    }

    public void setPayeeName(String payeeName) {
        this.payeeName = payeeName;
    }

    public String getRelatedTravelId() {
        return relatedTravelId;
    }

    public void setRelatedTravelId(String relatedTravelId) {
        this.relatedTravelId = relatedTravelId;
    }
}
