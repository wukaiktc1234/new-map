package com.foodtraceability.dto.finance.deposit;

/** SL1 返回：reused=true 表示该班次已有缴款单（shift_id 唯一，TD-33），未重复生成。 */
public class GenerateSlipResultVO {

    private boolean reused;
    private DepositSlipVO slip;

    public GenerateSlipResultVO() {
    }

    public GenerateSlipResultVO(boolean reused, DepositSlipVO slip) {
        this.reused = reused;
        this.slip = slip;
    }

    public boolean isReused() { return reused; }
    public void setReused(boolean reused) { this.reused = reused; }

    public DepositSlipVO getSlip() { return slip; }
    public void setSlip(DepositSlipVO slip) { this.slip = slip; }
}
