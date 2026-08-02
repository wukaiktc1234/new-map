package com.foodtraceability.service.finance.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.entity.finance.TransferTemplate;
import com.foodtraceability.exception.BusinessException;
import com.foodtraceability.mapper.finance.TransferTemplateMapper;
import com.foodtraceability.service.finance.TransferTemplateService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 结转模板Service实现
 */
@Service
public class TransferTemplateServiceImpl extends ServiceImpl<TransferTemplateMapper, TransferTemplate>
        implements TransferTemplateService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TransferTemplateVO create(TransferTemplateCreateDTO dto) {
        // 校验模板名称唯一性
        LambdaQueryWrapper<TransferTemplate> check = new LambdaQueryWrapper<>();
        check.eq(TransferTemplate::getTemplateName, dto.getTemplateName());
        if (baseMapper.selectCount(check) > 0) {
            throw new BusinessException("模板名称已存在");
        }

        TransferTemplate entity = new TransferTemplate();
        BeanUtils.copyProperties(dto, entity);
        entity.setIsEnabled(true); // 默认启用
        entity.setCreateUserId(1L); // 临时：创建人ID默认1L
        baseMapper.insert(entity);

        return convertToVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(Long templateId, TransferTemplateUpdateDTO dto) {
        TransferTemplate existing = baseMapper.selectById(templateId);
        if (existing == null) {
            throw new BusinessException("结转模板不存在");
        }

        // 校验模板名称唯一性（排除自身）
        if (dto.getTemplateName() != null && !dto.getTemplateName().equals(existing.getTemplateName())) {
            LambdaQueryWrapper<TransferTemplate> check = new LambdaQueryWrapper<>();
            check.eq(TransferTemplate::getTemplateName, dto.getTemplateName())
                 .ne(TransferTemplate::getTemplateId, templateId);
            if (baseMapper.selectCount(check) > 0) {
                throw new BusinessException("模板名称已存在");
            }
        }

        TransferTemplate updateEntity = new TransferTemplate();
        updateEntity.setTemplateId(templateId);

        if (dto.getTemplateName() != null) {
            updateEntity.setTemplateName(dto.getTemplateName());
        }
        if (dto.getTemplateType() != null) {
            updateEntity.setTemplateType(dto.getTemplateType());
        }
        if (dto.getSourceSubjectId() != null) {
            updateEntity.setSourceSubjectId(dto.getSourceSubjectId());
        }
        if (dto.getTargetSubjectId() != null) {
            updateEntity.setTargetSubjectId(dto.getTargetSubjectId());
        }
        if (dto.getAmountExpression() != null) {
            updateEntity.setAmountExpression(dto.getAmountExpression());
        }
        if (dto.getSummaryTemplate() != null) {
            updateEntity.setSummaryTemplate(dto.getSummaryTemplate());
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
    public boolean delete(Long templateId) {
        TransferTemplate existing = baseMapper.selectById(templateId);
        if (existing == null) {
            throw new BusinessException("结转模板不存在");
        }
        return baseMapper.deleteById(templateId) > 0;
    }

    @Override
    public TransferTemplateVO getDetail(Long templateId) {
        TransferTemplate entity = baseMapper.selectById(templateId);
        if (entity == null) {
            return null;
        }
        return convertToVO(entity);
    }

    @Override
    public IPage<TransferTemplateVO> getPage(TransferTemplateQueryDTO query) {
        Page<TransferTemplate> page = new Page<>(query.getCurrent(), query.getSize());
        LambdaQueryWrapper<TransferTemplate> wrapper = new LambdaQueryWrapper<>();

        if (query.getTemplateType() != null) {
            wrapper.eq(TransferTemplate::getTemplateType, query.getTemplateType());
        }
        if (query.getIsEnabled() != null) {
            wrapper.eq(TransferTemplate::getIsEnabled, query.getIsEnabled());
        }
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            wrapper.like(TransferTemplate::getTemplateName, query.getKeyword());
        }
        wrapper.orderByDesc(TransferTemplate::getTemplateId);

        IPage<TransferTemplate> entityPage = baseMapper.selectPage(page, wrapper);

        Page<TransferTemplateVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<TransferTemplateVO> voList = entityPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        @SuppressWarnings("unchecked")
        IPage<TransferTemplateVO> result = (IPage<TransferTemplateVO>) (IPage<?>) voPage;
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleEnabled(Long templateId, Boolean enabled) {
        TransferTemplate existing = baseMapper.selectById(templateId);
        if (existing == null) {
            throw new BusinessException("结转模板不存在");
        }
        TransferTemplate updateEntity = new TransferTemplate();
        updateEntity.setTemplateId(templateId);
        updateEntity.setIsEnabled(enabled);
        return baseMapper.updateById(updateEntity) > 0;
    }

    /**
     * 实体转VO
     */
    private TransferTemplateVO convertToVO(TransferTemplate entity) {
        TransferTemplateVO vo = new TransferTemplateVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
