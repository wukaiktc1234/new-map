package com.foodtraceability.service;

import com.foodtraceability.dto.VoucherAccountingRequestDTO;
import com.foodtraceability.dto.VoucherAccountingResultDTO;

import java.util.List;

/**
 * 电子凭证入账服务接口
 *
 * 依据：财政部《电子凭证会计数据标准应用指南(推广应用版1.0)》
 * 实现电子凭证到记账凭证的自动转换
 */
public interface VoucherAccountingService {

    /**
     * 单张电子凭证入账
     *
     * @param electronicVoucherId 电子凭证ID
     * @param request 入账请求参数
     * @return 入账结果
     */
    VoucherAccountingResultDTO accounting(Long electronicVoucherId, VoucherAccountingRequestDTO request);

    /**
     * 批量电子凭证入账
     *
     * @param electronicVoucherIds 电子凭证ID列表
     * @param request 入账请求参数
     * @return 批量入账结果
     */
    List<VoucherAccountingResultDTO> batchAccounting(List<Long> electronicVoucherIds, VoucherAccountingRequestDTO request);

    /**
     * 根据电子凭证类型获取默认会计分录模板
     *
     * @param voucherType 凭证类型
     * @return 会计分录模板
     */
    List<AccountingEntryTemplate> getDefaultTemplate(String voucherType);

    /**
     * 预览入账效果
     *
     * @param electronicVoucherId 电子凭证ID
     * @return 预览结果
     */
    VoucherAccountingResultDTO previewAccounting(Long electronicVoucherId);

    /**
     * 会计分录模板
     */
    class AccountingEntryTemplate {
        private String debitAccount;
        private String debitAccountName;
        private String creditAccount;
        private String creditAccountName;
        private String summary;
        private String amountSource;

        public String getDebitAccount() { return debitAccount; }
        public void setDebitAccount(String debitAccount) { this.debitAccount = debitAccount; }
        public String getDebitAccountName() { return debitAccountName; }
        public void setDebitAccountName(String debitAccountName) { this.debitAccountName = debitAccountName; }
        public String getCreditAccount() { return creditAccount; }
        public void setCreditAccount(String creditAccount) { this.creditAccount = creditAccount; }
        public String getCreditAccountName() { return creditAccountName; }
        public void setCreditAccountName(String creditAccountName) { this.creditAccountName = creditAccountName; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        public String getAmountSource() { return amountSource; }
        public void setAmountSource(String amountSource) { this.amountSource = amountSource; }
    }
}
