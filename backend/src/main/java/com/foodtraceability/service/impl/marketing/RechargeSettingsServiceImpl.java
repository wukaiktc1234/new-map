package com.foodtraceability.service.impl.marketing;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.dto.marketing.RechargeSettingsDTO;
import com.foodtraceability.entity.marketing.RechargeSystemSetting;
import com.foodtraceability.mapper.marketing.RechargeSystemSettingMapper;
import com.foodtraceability.service.marketing.RechargeSettingsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * 储值系统设置服务实现
 * 单行配置：所有设置以JSON形式存储在 recharge_system_settings 表
 * 固定ID为 RECHARGE_SETTINGS_001
 *
 * 默认设置遵循《单用途商业预付卡管理办法》要求：
 * - 单卡余额上限默认 5000 元
 * - 赠送比例硬上限默认 20%
 */
@Service
public class RechargeSettingsServiceImpl implements RechargeSettingsService {

    private static final Logger log = LoggerFactory.getLogger(RechargeSettingsServiceImpl.class);

    private final RechargeSystemSettingMapper settingsMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RechargeSettingsServiceImpl(RechargeSystemSettingMapper settingsMapper) {
        this.settingsMapper = settingsMapper;
    }

    @Override
    public RechargeSettingsDTO getSettings() {
        RechargeSystemSetting setting = settingsMapper.selectById(RechargeSystemSetting.DEFAULT_SETTING_ID);
        if (setting == null || setting.getConfigJson() == null || setting.getConfigJson().isEmpty()) {
            return buildDefaultSettings();
        }
        try {
            RechargeSettingsDTO dto = objectMapper.readValue(setting.getConfigJson(), RechargeSettingsDTO.class);
            // 字段缺失时使用默认值
            applyDefaultsIfNeeded(dto);
            return dto;
        } catch (JsonProcessingException e) {
            log.warn("解析储值系统设置JSON失败，返回默认设置：{}", e.getMessage());
            return buildDefaultSettings();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSettings(RechargeSettingsDTO settingsDTO) {
        applyDefaultsIfNeeded(settingsDTO);
        try {
            String configJson = objectMapper.writeValueAsString(settingsDTO);
            RechargeSystemSetting existing = settingsMapper.selectById(RechargeSystemSetting.DEFAULT_SETTING_ID);
            if (existing == null) {
                RechargeSystemSetting newSetting = new RechargeSystemSetting();
                newSetting.setSettingId(RechargeSystemSetting.DEFAULT_SETTING_ID);
                newSetting.setConfigJson(configJson);
                settingsMapper.insert(newSetting);
            } else {
                existing.setConfigJson(configJson);
                settingsMapper.updateById(existing);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("储值系统设置序列化失败", e);
        }
    }

    // ==================== 辅助方法 ====================

    /** 构建默认设置 */
    private RechargeSettingsDTO buildDefaultSettings() {
        RechargeSettingsDTO dto = new RechargeSettingsDTO();
        dto.setSingleRechargeLimit(1000);
        dto.setDailyRechargeLimit(3000);
        dto.setMonthlyRechargeLimit(10000);
        dto.setMaxBalanceLimit(5000);
        dto.setMaxBonusRate(20);
        dto.setDefaultBonusValidityDays(180);
        dto.setAllowedBonusTypes(Arrays.asList("balance", "coupon", "points", "mixed"));
        dto.setNoApprovalThreshold(500);
        dto.setManagerApprovalThreshold(2000);
        dto.setRefundFeeRate(0);
        dto.setRefundCooldownHours(24);
        dto.setBonusHandlingOnRefund("clear");
        dto.setRiskControlEnabled(true);
        dto.setAnomalyAlertEnabled(true);
        dto.setAlertChannels(Arrays.asList("sms", "wechat"));
        dto.setCurrentAgreementVersion("v1.0.0");
        dto.setAgreementReconfirmOnChange(true);
        dto.setFinanceIntegrationEnabled(false);
        dto.setFinanceSystemType("none");
        dto.setAutoVoucherGeneration(false);
        dto.setFundCustodyEnabled(false);
        dto.setCustodyBank("");
        dto.setCustodyRate(0);
        dto.setCustodyAccount("");
        return dto;
    }

    /** 字段为null时填充默认值（避免空指针） */
    private void applyDefaultsIfNeeded(RechargeSettingsDTO dto) {
        if (dto.getSingleRechargeLimit() == null) dto.setSingleRechargeLimit(1000);
        if (dto.getDailyRechargeLimit() == null) dto.setDailyRechargeLimit(3000);
        if (dto.getMonthlyRechargeLimit() == null) dto.setMonthlyRechargeLimit(10000);
        if (dto.getMaxBalanceLimit() == null) dto.setMaxBalanceLimit(5000);
        if (dto.getMaxBonusRate() == null) dto.setMaxBonusRate(20);
        if (dto.getDefaultBonusValidityDays() == null) dto.setDefaultBonusValidityDays(180);
        if (dto.getNoApprovalThreshold() == null) dto.setNoApprovalThreshold(500);
        if (dto.getManagerApprovalThreshold() == null) dto.setManagerApprovalThreshold(2000);
        if (dto.getRefundFeeRate() == null) dto.setRefundFeeRate(0);
        if (dto.getRefundCooldownHours() == null) dto.setRefundCooldownHours(24);
        if (dto.getBonusHandlingOnRefund() == null) dto.setBonusHandlingOnRefund("clear");
        if (dto.getRiskControlEnabled() == null) dto.setRiskControlEnabled(true);
        if (dto.getAnomalyAlertEnabled() == null) dto.setAnomalyAlertEnabled(true);
        if (dto.getCurrentAgreementVersion() == null) dto.setCurrentAgreementVersion("v1.0.0");
        if (dto.getAgreementReconfirmOnChange() == null) dto.setAgreementReconfirmOnChange(true);
        if (dto.getFinanceIntegrationEnabled() == null) dto.setFinanceIntegrationEnabled(false);
        if (dto.getFinanceSystemType() == null) dto.setFinanceSystemType("none");
        if (dto.getAutoVoucherGeneration() == null) dto.setAutoVoucherGeneration(false);
        if (dto.getFundCustodyEnabled() == null) dto.setFundCustodyEnabled(false);
        if (dto.getCustodyBank() == null) dto.setCustodyBank("");
        if (dto.getCustodyRate() == null) dto.setCustodyRate(0);
        if (dto.getCustodyAccount() == null) dto.setCustodyAccount("");
    }

    /** 显式引用 List 避免编译器未使用警告 */
    @SuppressWarnings("unused")
    private void unusedListReference() {
        List<String> unused = Arrays.asList("placeholder");
    }
}
