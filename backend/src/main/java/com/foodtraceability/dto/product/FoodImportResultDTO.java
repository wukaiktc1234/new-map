package com.foodtraceability.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 菜品导入结果DTO
 * 用于返回批量导入菜品的处理结果
 */
@Schema(description = "菜品导入结果")
public class FoodImportResultDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "总数量", example = "100")
    private Integer totalCount;

    @Schema(description = "成功数量", example = "95")
    private Integer successCount;

    @Schema(description = "失败数量", example = "5")
    private Integer failCount;

    @Schema(description = "错误信息列表")
    private List<String> errorMessages = new ArrayList<>();

    public FoodImportResultDTO() {
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Integer successCount) {
        this.successCount = successCount;
    }

    public Integer getFailCount() {
        return failCount;
    }

    public void setFailCount(Integer failCount) {
        this.failCount = failCount;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }

    public void setErrorMessages(List<String> errorMessages) {
        this.errorMessages = errorMessages;
    }
}
