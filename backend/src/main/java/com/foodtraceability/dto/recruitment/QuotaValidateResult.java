package com.foodtraceability.dto.recruitment;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 名额校验结果
 *
 * <p>用于提报招聘需求时校验名额可用性。</p>
 *
 * <p>错误码说明:</p>
 * <ul>
 *   <li>3013: 名额已用完(valid=false, remaining=0)</li>
 *   <li>3014: 名额状态不可用(非active)</li>
 *   <li>3015: 名额不存在</li>
 * </ul>
 */
@Schema(description = "名额校验结果")
public class QuotaValidateResult {

    @Schema(description = "是否可用", example = "true")
    private boolean valid;

    @Schema(description = "剩余名额数", example = "3")
    private int remaining;

    @Schema(description = "错误码(校验失败时返回:3013=已用完,3014=状态不可用,3015=不存在)",
            example = "3013")
    private String errorCode;

    public QuotaValidateResult() {
    }

    public QuotaValidateResult(boolean valid, int remaining, String errorCode) {
        this.valid = valid;
        this.remaining = remaining;
        this.errorCode = errorCode;
    }

    /**
     * 构建校验通过的结果。
     *
     * @param remaining 剩余名额数
     * @return 校验通过结果
     */
    public static QuotaValidateResult ok(int remaining) {
        return new QuotaValidateResult(true, remaining, null);
    }

    /**
     * 构建校验失败的结果。
     *
     * @param remaining 剩余名额数
     * @param errorCode 错误码
     * @return 校验失败结果
     */
    public static QuotaValidateResult fail(int remaining, String errorCode) {
        return new QuotaValidateResult(false, remaining, errorCode);
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public int getRemaining() {
        return remaining;
    }

    public void setRemaining(int remaining) {
        this.remaining = remaining;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
}
