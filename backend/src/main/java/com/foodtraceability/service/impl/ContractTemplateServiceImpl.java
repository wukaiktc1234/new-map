package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.ContractTemplate;
import com.foodtraceability.mapper.ContractTemplateMapper;
import com.foodtraceability.service.ContractTemplateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 合同模板服务实现
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-03-20
 */
@Service
public class ContractTemplateServiceImpl extends ServiceImpl<ContractTemplateMapper, ContractTemplate> implements ContractTemplateService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ContractTemplateServiceImpl.class);

    @Override
    public List<ContractTemplate> getActiveTemplatesByType(String contractType) {
        LambdaQueryWrapper<ContractTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContractTemplate::getContractType, contractType).eq(ContractTemplate::getStatus, ContractTemplate.STATUS_ACTIVE).orderByDesc(ContractTemplate::getCreateTime);
        return list(wrapper);
    }

    @Override
    public ContractTemplate getByTemplateCode(String templateCode) {
        LambdaQueryWrapper<ContractTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContractTemplate::getTemplateCode, templateCode);
        return getOne(wrapper);
    }

    @Override
    public String previewTemplate(Long templateId, Map<String, Object> variables) {
        ContractTemplate template = getById(templateId);
        if (template == null) {
            throw new RuntimeException("模板不存在");
        }
        String content = template.getTemplateContent();
        if (variables != null && !variables.isEmpty()) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                String placeholder = "{{" + entry.getKey() + "}}";
                String value = entry.getValue() != null ? entry.getValue().toString() : "";
                content = content.replace(placeholder, value);
            }
        }
        // 清除未填充的变量占位符
        Pattern pattern = Pattern.compile("\\{\\{[^}]+\\}\\}");
        Matcher matcher = pattern.matcher(content);
        content = matcher.replaceAll("______");
        return content;
    }

    @Override
    @Transactional
    public ContractTemplate duplicateTemplate(Long templateId) {
        ContractTemplate source = getById(templateId);
        if (source == null) {
            throw new RuntimeException("源模板不存在");
        }
        ContractTemplate newTemplate = new ContractTemplate();
        newTemplate.setTemplateName(source.getTemplateName() + " (副本)");
        newTemplate.setTemplateCode(source.getTemplateCode() + "_" + UUID.randomUUID().toString().substring(0, 8));
        newTemplate.setContractType(source.getContractType());
        newTemplate.setTemplateContent(source.getTemplateContent());
        newTemplate.setTemplateVariables(source.getTemplateVariables());
        newTemplate.setVersion("1.0.0");
        newTemplate.setStatus(ContractTemplate.STATUS_INACTIVE);
        newTemplate.setDescription(source.getDescription());
        save(newTemplate);
        return newTemplate;
    }

    @Override
    public Map<String, Integer> getStatistics() {
        Map<String, Integer> statistics = new HashMap<>();
        statistics.put("total", Math.toIntExact(count()));
        statistics.put("active", baseMapper.countByStatus(ContractTemplate.STATUS_ACTIVE));
        statistics.put("inactive", baseMapper.countByStatus(ContractTemplate.STATUS_INACTIVE));
        statistics.put("fixedTerm", baseMapper.countByContractType(ContractTemplate.TYPE_FIXED_TERM));
        statistics.put("openEnded", baseMapper.countByContractType(ContractTemplate.TYPE_OPEN_ENDED));
        statistics.put("project", baseMapper.countByContractType(ContractTemplate.TYPE_PROJECT));
        return statistics;
    }
}
