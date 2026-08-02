package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.ContractTemplate;

import java.util.List;
import java.util.Map;

/**
 * 合同模板服务接口
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-03-20
 */
public interface ContractTemplateService extends IService<ContractTemplate> {

    /**
     * 根据合同类型获取启用的模板列表
     */
    List<ContractTemplate> getActiveTemplatesByType(String contractType);

    /**
     * 根据模板编码获取模板
     */
    ContractTemplate getByTemplateCode(String templateCode);

    /**
     * 预览模板内容（填充变量）
     */
    String previewTemplate(Long templateId, Map<String, Object> variables);

    /**
     * 复制模板
     */
    ContractTemplate duplicateTemplate(Long templateId);

    /**
     * 获取模板统计
     */
    Map<String, Integer> getStatistics();
}
