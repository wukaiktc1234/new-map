package com.foodtraceability.dto.finance.recon;

import jakarta.validation.constraints.NotBlank;

/**
 * R10 差异处理入参（§2.7 #7）。
 *
 * <p>resolution：WRITE_OFF（核销，只记决议不写流水）/ EXEMPT（豁免）/ REVERSE（冲正，P1 只挂
 * PENDING_P2_REVERSE，不生成反向流水，留给 P2）。remark 必填。</p>
 */
public class ReconResolveDTO {

    @NotBlank(message = "resolution 不能为空")
    private String resolution;

    @NotBlank(message = "remark 不能为空")
    private String remark;

    /** 仅 REVERSE 场景预留（P2 回填真实反向流水号）；本阶段不写库 */
    private String reverseFlowNo;

    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public String getReverseFlowNo() { return reverseFlowNo; }
    public void setReverseFlowNo(String reverseFlowNo) { this.reverseFlowNo = reverseFlowNo; }
}
