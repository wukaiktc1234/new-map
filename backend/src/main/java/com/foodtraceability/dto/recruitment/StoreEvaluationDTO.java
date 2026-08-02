package com.foodtraceability.dto.recruitment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 门店面评提交DTO
 *
 * <p>门店面试官对候选人提交面试评价(评分1-5 + 评价内容)。</p>
 */
@Schema(description = "门店面评提交DTO")
public class StoreEvaluationDTO {

    @NotBlank(message = "门店面试官ID不能为空")
    @Schema(description = "门店面试官用户ID", example = "200",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String storeInterviewerId;

    @NotBlank(message = "门店面试官姓名不能为空")
    @Schema(description = "门店面试官姓名", example = "李店长",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String storeInterviewerName;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最小为1")
    @Max(value = 5, message = "评分最大为5")
    @Schema(description = "评分(1-5)", example = "4",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer score;

    @NotBlank(message = "面评内容不能为空")
    @Schema(description = "面评内容", example = "沟通能力好,符合岗位要求",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String evaluation;

    public String getStoreInterviewerId() {
        return storeInterviewerId;
    }

    public void setStoreInterviewerId(String storeInterviewerId) {
        this.storeInterviewerId = storeInterviewerId;
    }

    public String getStoreInterviewerName() {
        return storeInterviewerName;
    }

    public void setStoreInterviewerName(String storeInterviewerName) {
        this.storeInterviewerName = storeInterviewerName;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(String evaluation) {
        this.evaluation = evaluation;
    }
}
