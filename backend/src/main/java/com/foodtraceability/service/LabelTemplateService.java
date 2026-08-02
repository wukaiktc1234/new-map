package com.foodtraceability.service;

import com.foodtraceability.dto.LabelLayoutConfigDTO;
import com.foodtraceability.entity.LabelTemplate;

import java.util.Map;

/**
 * 标签模板服务接口
 * 提供标签模板的业务逻辑处理
 */
public interface LabelTemplateService {
    
    /**
     * 创建标签模板
     * @param template 模板数据
     * @return 创建后的模板
     */
    LabelTemplate createTemplate(LabelTemplate template);
    
    /**
     * 更新标签模板
     * @param id 模板ID
     * @param template 模板数据
     * @return 更新后的模板
     */
    LabelTemplate updateTemplate(String id, LabelTemplate template);
    
    /**
     * 删除标签模板
     * @param id 模板ID
     */
    void deleteTemplate(String id);
    
    /**
     * 设置默认模板
     * @param id 模板ID
     */
    void setDefaultTemplate(String id);
    
    /**
     * 验证模板名称
     * @param name 模板名称
     * @return 是否有效
     */
    boolean isValidTemplateName(String name);
    
    /**
     * 验证模板数据
     * @param template 模板数据
     * @return 验证结果
     */
    Map<String, String> validateTemplate(LabelTemplate template);
    
    /**
     * 批量打印标签
     * @param layoutConfig 布局配置
     * @param labelDataList 标签数据列表
     * @return 打印结果
     */
    Map<String, Object> batchPrintLabels(LabelLayoutConfigDTO layoutConfig, 
                                         java.util.List<Map<String, Object>> labelDataList);
}
