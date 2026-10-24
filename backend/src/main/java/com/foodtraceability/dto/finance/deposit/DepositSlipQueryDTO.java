package com.foodtraceability.dto.finance.deposit;

/** SL2 列表查询条件（storeId / shiftId / status / slipNo + 分页）。 */
public class DepositSlipQueryDTO {

    private Long storeId;
    private Long shiftId;
    /** DRAFT / SUBMITTED / RECEIVED / DEPOSITED / VERIFIED / CLOSED / REJECTED / REVERSED */
    private String status;
    private String slipNo;
    private Long current = 1L;
    private Long size = 20L;

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public Long getShiftId() { return shiftId; }
    public void setShiftId(Long shiftId) { this.shiftId = shiftId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSlipNo() { return slipNo; }
    public void setSlipNo(String slipNo) { this.slipNo = slipNo; }

    public Long getCurrent() { return current; }
    public void setCurrent(Long current) { this.current = current; }

    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }
}
