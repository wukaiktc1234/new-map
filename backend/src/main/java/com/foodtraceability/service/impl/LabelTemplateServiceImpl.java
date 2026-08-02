package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.dto.LabelLayoutConfigDTO;
import com.foodtraceability.entity.LabelTemplate;
import com.foodtraceability.mapper.LabelTemplateMapper;
import com.foodtraceability.service.LabelPrintService;
import com.foodtraceability.service.LabelTemplateService;
import com.foodtraceability.service.TsplGeneratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 标签模板服务实现类
 * 提供标签模板的业务逻辑处理
 */
@Service
public class LabelTemplateServiceImpl implements LabelTemplateService {
    
    private static final Logger log = LoggerFactory.getLogger(LabelTemplateServiceImpl.class);
    
    private static final int MIN_LABEL_SIZE = 20;
    private static final int MAX_LABEL_SIZE = 100;
    private static final int MAX_TEMPLATE_NAME_LENGTH = 50;
    private static final int MAX_BATCH_SIZE = 100;
    private static final int BATCH_CHUNK_SIZE = 20;
    

    public LabelTemplateServiceImpl(LabelTemplateMapper labelTemplateMapper, TsplGeneratorService tsplGeneratorService, LabelPrintService labelPrintService) {
        this.labelTemplateMapper = labelTemplateMapper;
        this.tsplGeneratorService = tsplGeneratorService;
        this.labelPrintService = labelPrintService;
    }

    private final LabelTemplateMapper labelTemplateMapper;
    
    private final TsplGeneratorService tsplGeneratorService;
    
    private final LabelPrintService labelPrintService;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LabelTemplate createTemplate(LabelTemplate template) {
        log.info("创建标签模板: {}", template.getTemplateName());
        
        String templateId = generateTemplateId(template.getId());
        template.setId(templateId);
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        
        if (template.getEnabled() == null) {
            template.setEnabled(true);
        }
        if (template.getIsDefault() == null) {
            template.setIsDefault(false);
        }
        if (template.getVersion() == null) {
            template.setVersion(0);
        }
        
        ensureSizeField(template);
        ensureBackgroundColor(template);
        
        labelTemplateMapper.insert(template);
        log.info("标签模板创建成功: {}", templateId);
        
        return template;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LabelTemplate updateTemplate(String id, LabelTemplate template) {
        log.info("更新标签模板: {}", id);
        
        LabelTemplate existing = labelTemplateMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("模板不存在");
        }
        
        if (template.getVersion() != null && !Objects.equals(existing.getVersion(), template.getVersion())) {
            throw new IllegalStateException("模板已被其他用户修改,请刷新后重试");
        }
        
        template.setId(id);
        template.setUpdatedAt(LocalDateTime.now());
        template.setCreatedAt(existing.getCreatedAt());
        template.setCreatedBy(existing.getCreatedBy());
        
        ensureSizeField(template);
        ensureBackgroundColor(template);
        
        int updateCount = labelTemplateMapper.updateById(template);
        if (updateCount == 0) {
            throw new IllegalStateException("更新失败,模板可能已被其他用户修改");
        }
        
        log.info("标签模板更新成功: {}", id);
        return template;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(String id) {
        log.info("删除标签模板: {}", id);
        
        LabelTemplate existing = labelTemplateMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("模板不存在");
        }
        
        labelTemplateMapper.deleteById(id);
        log.info("标签模板删除成功: {}", id);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefaultTemplate(String id) {
        log.info("设置默认模板: {}", id);
        
        LabelTemplate template = labelTemplateMapper.selectById(id);
        if (template == null) {
            throw new IllegalArgumentException("模板不存在");
        }
        
        LambdaQueryWrapper<LabelTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LabelTemplate::getTemplateType, template.getTemplateType());
        wrapper.eq(LabelTemplate::getIsDefault, true);
        List<LabelTemplate> defaults = labelTemplateMapper.selectList(wrapper);
        
        for (LabelTemplate t : defaults) {
            t.setIsDefault(false);
            t.setUpdatedAt(LocalDateTime.now());
            labelTemplateMapper.updateById(t);
        }
        
        template.setIsDefault(true);
        template.setUpdatedAt(LocalDateTime.now());
        labelTemplateMapper.updateById(template);
        
        log.info("默认模板设置成功: {}", id);
    }
    
    @Override
    public boolean isValidTemplateName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        if (name.length() > MAX_TEMPLATE_NAME_LENGTH) {
            return false;
        }
        String pattern = "^[\\u4e00-\\u9fa5a-zA-Z0-9_-]+$";
        return name.matches(pattern);
    }
    
    @Override
    public Map<String, String> validateTemplate(LabelTemplate template) {
        Map<String, String> errors = new HashMap<>();
        
        if (!isValidTemplateName(template.getTemplateName())) {
            errors.put("templateName", "模板名称格式不正确");
        }
        
        if (template.getLabelWidth() != null && 
            (template.getLabelWidth() < MIN_LABEL_SIZE || template.getLabelWidth() > MAX_LABEL_SIZE)) {
            errors.put("labelWidth", "标签宽度必须在" + MIN_LABEL_SIZE + "-" + MAX_LABEL_SIZE + "mm之间");
        }
        
        if (template.getLabelHeight() != null && 
            (template.getLabelHeight() < MIN_LABEL_SIZE || template.getLabelHeight() > MAX_LABEL_SIZE)) {
            errors.put("labelHeight", "标签高度必须在" + MIN_LABEL_SIZE + "-" + MAX_LABEL_SIZE + "mm之间");
        }
        
        return errors;
    }
    
    @Override
    public Map<String, Object> batchPrintLabels(LabelLayoutConfigDTO layoutConfig, 
                                                List<Map<String, Object>> labelDataList) {
        log.info("批量打印标签: 数量={}", labelDataList.size());
        
        Map<String, Object> result = new HashMap<>();
        
        if (labelDataList.size() > MAX_BATCH_SIZE) {
            result.put("success", false);
            result.put("message", String.format("单次批量打印数量不能超过%d张", MAX_BATCH_SIZE));
            return result;
        }
        
        TsplGeneratorService.ValidationResult validation = tsplGeneratorService.validateLayout(layoutConfig);
        if (!validation.isValid()) {
            result.put("success", false);
            result.put("message", "布局验证失败: " + validation.getMessage());
            return result;
        }
        
        int successCount = 0;
        int failCount = 0;
        
        for (int i = 0; i < labelDataList.size(); i += BATCH_CHUNK_SIZE) {
            int end = Math.min(i + BATCH_CHUNK_SIZE, labelDataList.size());
            List<Map<String, Object>> batch = labelDataList.subList(i, end);
            
            for (Map<String, Object> labelData : batch) {
                try {
                    String tspl = tsplGeneratorService.generateTsplWithData(layoutConfig, labelData);
                    boolean success = labelPrintService.printCustomLabel(tspl);
                    
                    if (success) {
                        successCount++;
                    } else {
                        failCount++;
                    }
                    
                    Thread.sleep(100);
                } catch (Exception e) {
                    log.error("打印标签失败: {}", e.getMessage());
                    failCount++;
                }
            }
            
            if (i + BATCH_CHUNK_SIZE < labelDataList.size()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        result.put("success", failCount == 0);
        result.put("message", String.format("批量打印完成: 成功 %d, 失败 %d", successCount, failCount));
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        
        log.info("批量打印完成: 成功={}, 失败={}", successCount, failCount);
        return result;
    }
    
    private String generateTemplateId(String providedId) {
        if (providedId != null && !providedId.isEmpty()) {
            LabelTemplate existing = labelTemplateMapper.selectById(providedId);
            if (existing != null) {
                log.warn("模板ID已存在,生成新ID: {} -> {}", providedId, UUID.randomUUID().toString());
                return UUID.randomUUID().toString();
            }
            return providedId;
        }
        return UUID.randomUUID().toString();
    }
    
    private void ensureSizeField(LabelTemplate template) {
        if (template.getSize() == null && 
            template.getLabelWidth() != null && 
            template.getLabelHeight() != null) {
            Map<String, Object> size = new HashMap<>();
            size.put("width", template.getLabelWidth());
            size.put("height", template.getLabelHeight());
            template.setSize(size);
        }
    }
    
    private void ensureBackgroundColor(LabelTemplate template) {
        if (template.getBackgroundColor() == null) {
            template.setBackgroundColor("#FFFFFF");
        }
    }
}
