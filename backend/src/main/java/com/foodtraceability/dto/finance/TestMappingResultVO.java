package com.foodtraceability.dto.finance;

import java.io.Serializable;
import java.util.List;

/**
 * 规则测试匹配结果VO
 */
public class TestMappingResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否匹配到规则 */
    private Boolean matched;

    /** 匹配到的规则 */
    private AccountMappingRuleVO rule;

    /** 模拟生成的分录列表 */
    private List<AutoVoucherVO.VoucherEntryVO> simulatedEntries;

    /** 提示信息 */
    private String message;

    public Boolean getMatched() {
        return matched;
    }

    public void setMatched(Boolean matched) {
        this.matched = matched;
    }

    public AccountMappingRuleVO getRule() {
        return rule;
    }

    public void setRule(AccountMappingRuleVO rule) {
        this.rule = rule;
    }

    public List<AutoVoucherVO.VoucherEntryVO> getSimulatedEntries() {
        return simulatedEntries;
    }

    public void setSimulatedEntries(List<AutoVoucherVO.VoucherEntryVO> simulatedEntries) {
        this.simulatedEntries = simulatedEntries;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
