package com.foodtraceability.service.finance.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.entity.finance.TaxRateConfig;
import com.foodtraceability.exception.BusinessException;
import com.foodtraceability.mapper.finance.TaxRateConfigMapper;
import com.foodtraceability.service.finance.TaxRateConfigService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 税率配置Service实现
 */
@Service
public class TaxRateConfigServiceImpl extends ServiceImpl<TaxRateConfigMapper, TaxRateConfig>
        implements TaxRateConfigService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaxRateConfigVO create(TaxRateConfigCreateDTO dto) {
        // 校验同一税种+纳税人类型在有效期内不重复
        LambdaQueryWrapper<TaxRateConfig> check = new LambdaQueryWrapper<>();
        check.eq(TaxRateConfig::getTaxType, dto.getTaxType())
             .eq(TaxRateConfig::getTaxpayerType, dto.getTaxpayerType());
        if (dto.getEffectiveDate() != null && dto.getExpiryDate() != null) {
            // 区间重叠校验：existing.effectiveDate <= dto.expiryDate AND existing.expiryDate >= dto.effectiveDate
            check.le(TaxRateConfig::getEffectiveDate, dto.getExpiryDate())
                 .ge(TaxRateConfig::getExpiryDate, dto.getEffectiveDate());
        } else if (dto.getEffectiveDate() != null) {
            // 新配置有生效日期但无失效日期，校验是否与已有配置冲突
            check.isNull(TaxRateConfig::getExpiryDate)
                 .or(w -> w.ge(TaxRateConfig::getExpiryDate, dto.getEffectiveDate()));
        }
        if (baseMapper.selectCount(check) > 0) {
            throw new BusinessException("同一税种和纳税人类型在有效期内已存在税率配置");
        }

        TaxRateConfig entity = new TaxRateConfig();
        BeanUtils.copyProperties(dto, entity);
        entity.setIsActive(true); // 默认生效
        baseMapper.insert(entity);

        return convertToVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(Long configId, TaxRateConfigUpdateDTO dto) {
        TaxRateConfig existing = baseMapper.selectById(configId);
        if (existing == null) {
            throw new BusinessException("税率配置不存在");
        }

        TaxRateConfig updateEntity = new TaxRateConfig();
        updateEntity.setConfigId(configId);

        if (dto.getTaxType() != null) {
            updateEntity.setTaxType(dto.getTaxType());
        }
        if (dto.getTaxpayerType() != null) {
            updateEntity.setTaxpayerType(dto.getTaxpayerType());
        }
        if (dto.getTaxRate() != null) {
            updateEntity.setTaxRate(dto.getTaxRate());
        }
        if (dto.getPolicyVersion() != null) {
            updateEntity.setPolicyVersion(dto.getPolicyVersion());
        }
        if (dto.getEffectiveDate() != null) {
            updateEntity.setEffectiveDate(dto.getEffectiveDate());
        }
        if (dto.getExpiryDate() != null) {
            updateEntity.setExpiryDate(dto.getExpiryDate());
        }
        if (dto.getIsActive() != null) {
            updateEntity.setIsActive(dto.getIsActive());
        }
        if (dto.getRemark() != null) {
            updateEntity.setRemark(dto.getRemark());
        }

        return baseMapper.updateById(updateEntity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long configId) {
        TaxRateConfig existing = baseMapper.selectById(configId);
        if (existing == null) {
            throw new BusinessException("税率配置不存在");
        }
        return baseMapper.deleteById(configId) > 0;
    }

    @Override
    public TaxRateConfigVO getDetail(Long configId) {
        TaxRateConfig entity = baseMapper.selectById(configId);
        if (entity == null) {
            return null;
        }
        return convertToVO(entity);
    }

    @Override
    public IPage<TaxRateConfigVO> getPage(TaxRateConfigQueryDTO query) {
        Page<TaxRateConfig> page = new Page<>(query.getCurrent(), query.getSize());
        LambdaQueryWrapper<TaxRateConfig> wrapper = new LambdaQueryWrapper<>();

        if (query.getTaxType() != null) {
            wrapper.eq(TaxRateConfig::getTaxType, query.getTaxType());
        }
        if (query.getTaxpayerType() != null) {
            wrapper.eq(TaxRateConfig::getTaxpayerType, query.getTaxpayerType());
        }
        if (query.getIsActive() != null) {
            wrapper.eq(TaxRateConfig::getIsActive, query.getIsActive());
        }
        wrapper.orderByDesc(TaxRateConfig::getConfigId);

        IPage<TaxRateConfig> entityPage = baseMapper.selectPage(page, wrapper);

        Page<TaxRateConfigVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<TaxRateConfigVO> voList = entityPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        @SuppressWarnings("unchecked")
        IPage<TaxRateConfigVO> result = (IPage<TaxRateConfigVO>) (IPage<?>) voPage;
        return result;
    }

    @Override
    public TaxRateConfigVO getEffectiveRate(Integer taxType, Integer taxpayerType) {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<TaxRateConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaxRateConfig::getTaxType, taxType)
               .eq(TaxRateConfig::getTaxpayerType, taxpayerType)
               .eq(TaxRateConfig::getIsActive, true)
               .le(TaxRateConfig::getEffectiveDate, today)
               .and(w -> w.isNull(TaxRateConfig::getExpiryDate)
                       .or().ge(TaxRateConfig::getExpiryDate, today))
               .orderByDesc(TaxRateConfig::getEffectiveDate)
               .last("LIMIT 1");

        TaxRateConfig entity = baseMapper.selectOne(wrapper);
        if (entity == null) {
            return null;
        }
        return convertToVO(entity);
    }

    /**
     * 实体转VO
     */
    private TaxRateConfigVO convertToVO(TaxRateConfig entity) {
        TaxRateConfigVO vo = new TaxRateConfigVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
