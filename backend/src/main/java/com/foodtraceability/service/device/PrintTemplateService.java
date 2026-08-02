package com.foodtraceability.service.device;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.PrintTemplateCreateDTO;
import com.foodtraceability.dto.PrintTemplateUpdateDTO;
import com.foodtraceability.dto.PrintTemplateVO;

import java.util.List;
import java.util.Map;

/**
 * 打印模板服务接口
 * 提供打印模板的CRUD、渲染、默认设置等功能
 */
public interface PrintTemplateService {

    /**
     * 创建打印模板
     * @param dto 创建请求DTO
     * @return 创建结果
     */
    Result<PrintTemplateVO> createTemplate(PrintTemplateCreateDTO dto);

    /**
     * 更新打印模板
     * @param templateId 模板ID
     * @param dto 更新请求DTO
     * @return 更新结果
     */
    Result<PrintTemplateVO> updateTemplate(Long templateId, PrintTemplateUpdateDTO dto);

    /**
     * 删除打印模板（逻辑删除）
     * @param templateId 模板ID
     * @return 删除结果
     */
    Result<Void> deleteTemplate(Long templateId);

    /**
     * 根据ID查询模板
     * @param templateId 模板ID
     * @return 模板信息
     */
    Result<PrintTemplateVO> getTemplateById(Long templateId);

    /**
     * 分页查询模板列表
     * @param page 分页参数
     * @param templateType 模板类型（可选）
     * @param storeId 门店ID（可选，null表示全局）
     * @return 分页结果
     */
    Result<IPage<PrintTemplateVO>> getTemplatePage(Page<?> page, Integer templateType, Long storeId);

    /**
     * 查询所有启用的模板
     * @param templateType 模板类型（可选）
     * @param storeId 门店ID（可选）
     * @return 模板列表
     */
    Result<List<PrintTemplateVO>> getEnabledTemplates(Integer templateType, Long storeId);

    /**
     * 获取默认模板
     * @param templateType 模板类型
     * @param storeId 门店ID（可选）
     * @return 默认模板
     */
    Result<PrintTemplateVO> getDefaultTemplate(Integer templateType, Long storeId);

    /**
     * 设置默认模板
     * @param templateId 模板ID
     * @return 设置结果
     */
    Result<Void> setAsDefault(Long templateId);

    /**
     * 渲染模板内容
     * @param templateId 模板ID
     * @param variables 变量映射
     * @return 渲染后的内容
     */
    Result<String> renderTemplate(Long templateId, Map<String, Object> variables);
}
