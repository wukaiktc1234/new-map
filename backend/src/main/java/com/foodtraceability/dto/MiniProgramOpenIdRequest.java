package com.foodtraceability.dto;

/**
 * 小程序获取OpenId请求DTO
 */
public class MiniProgramOpenIdRequest {
    private String code;

    public MiniProgramOpenIdRequest() {
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof MiniProgramOpenIdRequest)) return false;
        final MiniProgramOpenIdRequest other = (MiniProgramOpenIdRequest) o;
        if (!other.canEqual(this)) return false;
        final Object this$code = this.getCode();
        final Object other$code = other.getCode();
        if (this$code == null ? other$code != null : !this$code.equals(other$code)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof MiniProgramOpenIdRequest;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $code = this.getCode();
        result = result * PRIME + ($code == null ? 43 : $code.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "MiniProgramOpenIdRequest(code=" + this.getCode() + ")";
    }
}
