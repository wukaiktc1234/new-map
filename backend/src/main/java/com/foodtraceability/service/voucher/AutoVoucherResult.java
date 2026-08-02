package com.foodtraceability.service.voucher;

import com.foodtraceability.entity.AutoVoucherLog;
import com.foodtraceability.entity.VoucherHeader;
import com.foodtraceability.entity.VoucherLine;
import java.util.List;

/**
 * 自动凭证生成结果类
 * 封装自动记账引擎的处理结果，包含凭证数据、日志、评分等信息
 * @author example
 * @since 2026-04-04
 */
public class AutoVoucherResult {

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 凭证头信息
     */
    private VoucherHeader voucherHeader;

    /**
     * 凭证行列表
     */
    private List<VoucherLine> voucherLines;

    /**
     * 处理日志
     */
    private AutoVoucherLog log;

    /**
     * 质量评分（0-100分）
     */
    private Integer qualityScore;

    /**
     * 警告信息列表
     */
    private List<String> warnings;

    /**
     * 错误信息列表
     */
    private List<String> errors;

    public AutoVoucherResult() {
        this.warnings = new java.util.ArrayList<>();
        this.errors = new java.util.ArrayList<>();
    }

    /**
     * 判断是否处理成功
     * @return 是否成功
     */
    public boolean isSuccess() {
        return Boolean.TRUE.equals(success);
    }

    // getter and setter methods
    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public VoucherHeader getVoucherHeader() {
        return voucherHeader;
    }

    public void setVoucherHeader(VoucherHeader voucherHeader) {
        this.voucherHeader = voucherHeader;
    }

    public List<VoucherLine> getVoucherLines() {
        return voucherLines;
    }

    public void setVoucherLines(List<VoucherLine> voucherLines) {
        this.voucherLines = voucherLines;
    }

    public AutoVoucherLog getLog() {
        return log;
    }

    public void setLog(AutoVoucherLog log) {
        this.log = log;
    }

    public Integer getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(Integer qualityScore) {
        this.qualityScore = qualityScore;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    /**
     * 添加警告信息
     * @param warning 警告内容
     */
    public void addWarning(String warning) {
        if (this.warnings == null) {
            this.warnings = new java.util.ArrayList<>();
        }
        this.warnings.add(warning);
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    /**
     * 添加错误信息
     * @param error 错误内容
     */
    public void addError(String error) {
        if (this.errors == null) {
            this.errors = new java.util.ArrayList<>();
        }
        this.errors.add(error);
    }

    @Override
    public String toString() {
        return "AutoVoucherResult{" +
            "success=" + success +
            ", qualityScore=" + qualityScore +
            ", warnings=" + warnings +
            ", errors=" + errors +
            '}';
    }
}
