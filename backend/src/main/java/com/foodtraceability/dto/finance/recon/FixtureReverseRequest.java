package com.foodtraceability.dto.finance.recon;

import java.util.List;

/** 夹具余额抹平入参（运维端点，业务上不存在该场景） */
public class FixtureReverseRequest {
    /** 待抹平的夹具流水号（必须 status=3 且 remark 含 [testFixture） */
    private List<String> flowNos;
    /** 抹平原因（写入反向流水备注与审计日志） */
    private String reason;

    public List<String> getFlowNos() { return flowNos; }
    public void setFlowNos(List<String> v) { this.flowNos = v; }
    public String getReason() { return reason; }
    public void setReason(String v) { this.reason = v; }
}
