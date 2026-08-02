package com.foodtraceability.dto;

/**
 * 合同延期请求DTO
 */
public class ContractExtendDTO {
    private Integer months = 12;

    public ContractExtendDTO() {
    }

    public Integer getMonths() {
        return this.months;
    }

    public void setMonths(final Integer months) {
        this.months = months;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof ContractExtendDTO)) return false;
        final ContractExtendDTO other = (ContractExtendDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$months = this.getMonths();
        final java.lang.Object other$months = other.getMonths();
        if (this$months == null ? other$months != null : !this$months.equals(other$months)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ContractExtendDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $months = this.getMonths();
        result = result * PRIME + ($months == null ? 43 : $months.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "ContractExtendDTO(months=" + this.getMonths() + ")";
    }
}
