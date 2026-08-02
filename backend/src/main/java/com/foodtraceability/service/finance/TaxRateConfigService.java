package com.foodtraceability.service.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.entity.finance.TaxRateConfig;

/**
 * 税率配置Service接口
 * 管理各税种的税率配置，支持按纳税人类型和有效期管理税率政策
 */
public interface TaxRateConfigService extends IService<TaxRateConfig> {

    /**
     * 创建税率配置
     * @param dto 创建DTO
     * @return 配置VO
     */
    TaxRateConfigVO create(TaxRateConfigCreateDTO dto);

    /**
     * 更新税率配置
     * @param configId 配置ID
     * @param dto 更新DTO
     * @return 是否成功
     */
    boolean update(Long configId, TaxRateConfigUpdateDTO dto);

    /**
     * 删除税率配置（逻辑删除）
     * @param configId 配置ID
     * @return 是否成功
     */
    boolean delete(Long configId);

    /**
     * 获取配置详情
     * @param configId 配置ID
     * @return 配置VO
     */
    TaxRateConfigVO getDetail(Long configId);

    /**
     * 分页查询税率配置
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<TaxRateConfigVO> getPage(TaxRateConfigQueryDTO query);

    /**
     * 查询当前生效的税率配置
     * @param taxType 税种
     * @param taxpayerType 纳税人类型
     * @return 生效的税率配置VO
     */
    TaxRateConfigVO getEffectiveRate(Integer taxType, Integer taxpayerType);
}
