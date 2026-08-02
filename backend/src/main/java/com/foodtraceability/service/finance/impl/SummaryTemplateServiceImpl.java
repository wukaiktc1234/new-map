package com.foodtraceability.service.finance.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.entity.finance.SummaryTemplate;
import com.foodtraceability.exception.BusinessException;
import com.foodtraceability.mapper.finance.SummaryTemplateMapper;
import com.foodtraceability.service.finance.SummaryTemplateService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 摘要模板Service实现
 */
@Service
public class SummaryTemplateServiceImpl extends ServiceImpl<SummaryTemplateMapper, SummaryTemplate>
        implements SummaryTemplateService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SummaryTemplateVO create(SummaryTemplateCreateDTO dto) {
        SummaryTemplate entity = new SummaryTemplate();
        BeanUtils.copyProperties(dto, entity);
        entity.setUsageCount(0);
        entity.setStatus(1); // 默认启用
        baseMapper.insert(entity);
        return convertToVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(Long templateId, SummaryTemplateCreateDTO dto) {
        SummaryTemplate existing = baseMapper.selectById(templateId);
        if (existing == null) {
            throw new BusinessException("摘要模板不存在");
        }

        SummaryTemplate updateEntity = new SummaryTemplate();
        updateEntity.setTemplateId(templateId);

        if (dto.getSummaryContent() != null) {
            updateEntity.setSummaryContent(dto.getSummaryContent());
        }
        if (dto.getCategory() != null) {
            updateEntity.setCategory(dto.getCategory());
        }

        return baseMapper.updateById(updateEntity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long templateId) {
        SummaryTemplate existing = baseMapper.selectById(templateId);
        if (existing == null) {
            throw new BusinessException("摘要模板不存在");
        }
        return baseMapper.deleteById(templateId) > 0;
    }

    @Override
    public IPage<SummaryTemplateVO> getPage(SummaryTemplateQueryDTO query) {
        Page<SummaryTemplate> page = new Page<>(query.getCurrent(), query.getSize());
        LambdaQueryWrapper<SummaryTemplate> wrapper = new LambdaQueryWrapper<>();

        if (query.getCategory() != null && !query.getCategory().isBlank()) {
            wrapper.eq(SummaryTemplate::getCategory, query.getCategory());
        }
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            wrapper.like(SummaryTemplate::getSummaryContent, query.getKeyword());
        }
        wrapper.orderByDesc(SummaryTemplate::getUsageCount);

        IPage<SummaryTemplate> entityPage = baseMapper.selectPage(page, wrapper);

        Page<SummaryTemplateVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<SummaryTemplateVO> voList = entityPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        @SuppressWarnings("unchecked")
        IPage<SummaryTemplateVO> result = (IPage<SummaryTemplateVO>) (IPage<?>) voPage;
        return result;
    }

    @Override
    public List<SummaryTemplateVO> search(String keyword, int limit) {
        LambdaQueryWrapper<SummaryTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SummaryTemplate::getStatus, 1); // 仅搜索启用的模板
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(SummaryTemplate::getSummaryContent, keyword);
        }
        wrapper.orderByDesc(SummaryTemplate::getUsageCount);
        wrapper.last("LIMIT " + Math.max(limit, 1));

        List<SummaryTemplate> entities = baseMapper.selectList(wrapper);
        return entities.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean incrementUsageCount(Long templateId) {
        LambdaUpdateWrapper<SummaryTemplate> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SummaryTemplate::getTemplateId, templateId)
               .setSql("usage_count = usage_count + 1");
        return baseMapper.update(null, wrapper) > 0;
    }

    /**
     * 实体转VO
     */
    private SummaryTemplateVO convertToVO(SummaryTemplate entity) {
        SummaryTemplateVO vo = new SummaryTemplateVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
