package com.foodtraceability.dto.finance.recon;

/**
 * R1 跑批返回：reused=true 表示命中幂等键（同日同账户已有 run，未 force），本次未重算。
 */
public class ReconRunCreateResultVO {

    private boolean reused;
    private ReconRunVO run;

    public ReconRunCreateResultVO() {
    }

    public ReconRunCreateResultVO(boolean reused, ReconRunVO run) {
        this.reused = reused;
        this.run = run;
    }

    public boolean isReused() { return reused; }
    public void setReused(boolean reused) { this.reused = reused; }

    public ReconRunVO getRun() { return run; }
    public void setRun(ReconRunVO run) { this.run = run; }
}
