package com.foodtraceability.service.finance.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.entity.finance.ApprovalFlowConfig;
import com.foodtraceability.exception.BusinessException;
import com.foodtraceability.mapper.finance.ApprovalFlowConfigMapper;
import com.foodtraceability.service.finance.ApprovalFlowConfigService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 审批流配置Service实现
 */
@Service
public class ApprovalFlowConfigServiceImpl extends ServiceImpl<ApprovalFlowConfigMapper, ApprovalFlowConfig>
        implements ApprovalFlowConfigService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApprovalFlowConfigVO create(ApprovalFlowConfigCreateDTO dto) {
        // 校验同一documentType的启用配置不重复
        LambdaQueryWrapper<ApprovalFlowConfig> check = new LambdaQueryWrapper<>();
        check.eq(ApprovalFlowConfig::getDocumentType, dto.getDocumentType())
             .eq(ApprovalFlowConfig::getIsEnabled, true);
        if (baseMapper.selectCount(check) > 0) {
            throw new BusinessException("该单据类型已存在启用的审批流配置");
        }

        ApprovalFlowConfig entity = new ApprovalFlowConfig();
        BeanUtils.copyProperties(dto, entity);
        entity.setIsEnabled(true); // 默认启用
        baseMapper.insert(entity);

        return convertToVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(Long configId, ApprovalFlowConfigUpdateDTO dto) {
        ApprovalFlowConfig existing = baseMapper.selectById(configId);
        if (existing == null) {
            throw new BusinessException("审批流配置不存在");
        }

        // 校验documentType唯一性（启用状态下，排除自身）
        if (dto.getDocumentType() != null && !dto.getDocumentType().equals(existing.getDocumentType())) {
            LambdaQueryWrapper<ApprovalFlowConfig> check = new LambdaQueryWrapper<>();
            check.eq(ApprovalFlowConfig::getDocumentType, dto.getDocumentType())
                 .eq(ApprovalFlowConfig::getIsEnabled, true)
                 .ne(ApprovalFlowConfig::getConfigId, configId);
            if (baseMapper.selectCount(check) > 0) {
                throw new BusinessException("该单据类型已存在启用的审批流配置");
            }
        }

        ApprovalFlowConfig updateEntity = new ApprovalFlowConfig();
        updateEntity.setConfigId(configId);

        if (dto.getConfigName() != null) {
            updateEntity.setConfigName(dto.getConfigName());
        }
        if (dto.getDocumentType() != null) {
            updateEntity.setDocumentType(dto.getDocumentType());
        }
        if (dto.getApprovalNodes() != null) {
            updateEntity.setApprovalNodes(dto.getApprovalNodes());
        }
        if (dto.getIsEnabled() != null) {
            updateEntity.setIsEnabled(dto.getIsEnabled());
        }
        if (dto.getRemark() != null) {
            updateEntity.setRemark(dto.getRemark());
        }

        return baseMapper.updateById(updateEntity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long configId) {
        ApprovalFlowConfig existing = baseMapper.selectById(configId);
        if (existing == null) {
            throw new BusinessException("审批流配置不存在");
        }
        return baseMapper.deleteById(configId) > 0;
    }

    @Override
    public ApprovalFlowConfigVO getDetail(Long configId) {
        ApprovalFlowConfig entity = baseMapper.selectById(configId);
        if (entity == null) {
            return null;
        }
        return convertToVO(entity);
    }

    @Override
    public IPage<ApprovalFlowConfigVO> getPage(ApprovalFlowConfigQueryDTO query) {
        Page<ApprovalFlowConfig> page = new Page<>(query.getCurrent(), query.getSize());
        LambdaQueryWrapper<ApprovalFlowConfig> wrapper = new LambdaQueryWrapper<>();

        if (query.getDocumentType() != null && !query.getDocumentType().isBlank()) {
            wrapper.eq(ApprovalFlowConfig::getDocumentType, query.getDocumentType());
        }
        if (query.getIsEnabled() != null) {
            wrapper.eq(ApprovalFlowConfig::getIsEnabled, query.getIsEnabled());
        }
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            wrapper.like(ApprovalFlowConfig::getConfigName, query.getKeyword());
        }
        wrapper.orderByDesc(ApprovalFlowConfig::getConfigId);

        IPage<ApprovalFlowConfig> entityPage = baseMapper.selectPage(page, wrapper);

        Page<ApprovalFlowConfigVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<ApprovalFlowConfigVO> voList = entityPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        @SuppressWarnings("unchecked")
        IPage<ApprovalFlowConfigVO> result = (IPage<ApprovalFlowConfigVO>) (IPage<?>) voPage;
        return result;
    }

    @Override
    public ApprovalFlowConfigVO getByDocumentType(String documentType) {
        LambdaQueryWrapper<ApprovalFlowConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalFlowConfig::getDocumentType, documentType)
               .eq(ApprovalFlowConfig::getIsEnabled, true)
               .last("LIMIT 1");
        ApprovalFlowConfig entity = baseMapper.selectOne(wrapper);
        if (entity == null) {
            return null;
        }
        return convertToVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleEnabled(Long configId, Boolean enabled) {
        ApprovalFlowConfig existing = baseMapper.selectById(configId);
        if (existing == null) {
            throw new BusinessException("审批流配置不存在");
        }
        // 启用时校验同一documentType不重复
        if (Boolean.TRUE.equals(enabled)) {
            LambdaQueryWrapper<ApprovalFlowConfig> check = new LambdaQueryWrapper<>();
            check.eq(ApprovalFlowConfig::getDocumentType, existing.getDocumentType())
                 .eq(ApprovalFlowConfig::getIsEnabled, true)
                 .ne(ApprovalFlowConfig::getConfigId, configId);
            if (baseMapper.selectCount(check) > 0) {
                throw new BusinessException("该单据类型已存在启用的审批流配置");
            }
        }
        ApprovalFlowConfig updateEntity = new ApprovalFlowConfig();
        updateEntity.setConfigId(configId);
        updateEntity.setIsEnabled(enabled);
        return baseMapper.updateById(updateEntity) > 0;
    }

    /**
     * 实体转VO
     */
    private ApprovalFlowConfigVO convertToVO(ApprovalFlowConfig entity) {
        ApprovalFlowConfigVO vo = new ApprovalFlowConfigVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
