package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.PrintFormatTemplate;

import java.util.List;

/**
 * 打印格式模板服务接口
 */
public interface PrintFormatTemplateService extends IService<PrintFormatTemplate> {
    
    /**
     * 根据模板类型和设备类型查询模板列表
     * @param templateType 模板类型
     * @param deviceType 设备类型
     * @param storeId 门店ID
     * @return 模板列表
     */
    List<PrintFormatTemplate> getTemplatesByTypeAndDevice(String templateType, String deviceType, Long storeId);
    
    /**
     * 查询默认模板
     * @param templateType 模板类型
     * @param deviceType 设备类型
     * @param storeId 门店ID
     * @return 默认模板
     */
    PrintFormatTemplate getDefaultTemplate(String templateType, String deviceType, Long storeId);
    
    /**
     * 保存打印格式模板
     * @param template 打印格式模板
     * @return 保存结果
     */
    PrintFormatTemplate saveTemplate(PrintFormatTemplate template);
    
    /**
     * 删除打印格式模板
     * @param id 模板ID
     * @return 删除结果
     */
    boolean deleteTemplate(Long id);
    
    /**
     * 设置默认模板
     * @param id 模板ID
     * @param templateType 模板类型
     * @param deviceType 设备类型
     * @param storeId 门店ID
     * @return 设置结果
     */
    boolean setDefaultTemplate(Long id, String templateType, String deviceType, Long storeId);
    
    /**
     * 启用/禁用模板
     * @param id 模板ID
     * @param status 状态：0-禁用, 1-启用
     * @return 操作结果
     */
    boolean updateTemplateStatus(Long id, Integer status);
}
