package com.foodtraceability.service;

import com.foodtraceability.dto.LongTermAssetInputTaxDTO;
import com.foodtraceability.dto.LongTermAssetInputTaxQueryDTO;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.math.BigDecimal;
import java.util.List;

/**
 * 长期资产进项税额抵扣服务接口
 *
 * 依据：财政部 税务总局公告2026年第15号《长期资产进项税额抵扣暂行办法》
 * 实施日期：2026年1月1日
 */
public interface LongTermAssetInputTaxService {

    /**
     * 登记长期资产进项税额
     *
     * @param dto 长期资产信息
     * @return 登记结果
     */
    LongTermAssetInputTaxDTO registerAsset(LongTermAssetInputTaxDTO dto);

    /**
     * 计算年度进项税额调整额
     *
     * 公式：年度应转出进项税额 = 总进项税额 × (不得抵扣年折旧额 ÷ 资产原值)
     *
     * @param assetId 资产ID
     * @param year 年度
     * @return 调整金额
     */
    BigDecimal calculateAnnualAdjustment(Long assetId, int year);

    /**
     * 执行年度进项税额调整
     *
     * @param assetId 资产ID
     * @param year 年度
     * @return 调整结果
     */
    LongTermAssetInputTaxDTO.AnnualAdjustmentResult executeAnnualAdjustment(Long assetId, int year);

    /**
     * 查询长期资产列表
     *
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<LongTermAssetInputTaxDTO> queryAssetPage(LongTermAssetInputTaxQueryDTO query);

    /**
     * 获取资产详情
     *
     * @param assetId 资产ID
     * @return 资产详情
     */
    LongTermAssetInputTaxDTO getAssetDetail(Long assetId);

    /**
     * 获取资产调整记录
     *
     * @param assetId 资产ID
     * @return 调整记录列表
     */
    List<LongTermAssetInputTaxDTO.AnnualAdjustmentRecord> getAdjustmentRecords(Long assetId);

    /**
     * 判断资产是否属于大额长期资产（原值>500万元）
     *
     * @param originalValue 资产原值
     * @return 是否大额资产
     */
    default boolean isLargeAsset(BigDecimal originalValue) {
        return originalValue != null && originalValue.compareTo(new BigDecimal("5000000")) > 0;
    }

    /**
     * 判断资产是否属于小额长期资产（原值≤500万元）
     *
     * @param originalValue 资产原值
     * @return 是否小额资产
     */
    default boolean isSmallAsset(BigDecimal originalValue) {
        return originalValue != null && originalValue.compareTo(new BigDecimal("5000000")) <= 0;
    }
}
