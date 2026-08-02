package com.foodtraceability.service;

import com.foodtraceability.dto.PermissionTemplateDTO;
import com.foodtraceability.entity.PermissionTemplate;

import java.util.List;

/**
 * 权限模板服务接口
 * 提供权限中心 4 种模式（集中式单店/标准连锁/大型连锁/自定义）的管理能力
 */
public interface PermissionTemplateService {

    /**
     * 查询所有启用的权限模板
     * @return 权限模板列表
     */
    List<PermissionTemplateDTO> getAllEnabledTemplates();

    /**
     * 查询所有系统模板
     * @return 系统模板列表
     */
    List<PermissionTemplateDTO> getSystemTemplates();

    /**
     * 根据模板编码查询模板
     * @param code 模板编码
     * @return 权限模板
     */
    PermissionTemplateDTO getTemplateByCode(String code);

    /**
     * 根据企业类型和规模查询匹配的模板
     * @param enterpriseType 企业类型
     * @param scaleRange 规模范围
     * @return 权限模板列表
     */
    List<PermissionTemplateDTO> getTemplatesByEnterpriseTypeAndScale(String enterpriseType, String scaleRange);

    /**
     * 根据模板编码查询完整模板（含角色配置 JSON）
     * @param code 模板编码
     * @return 权限模板实体（含 roleConfig JSON 字符串）
     */
    PermissionTemplate getTemplateEntityByCode(String code);

    /**
     * 创建自定义模板
     * @param dto 模板数据
     * @return 创建后的模板
     */
    PermissionTemplateDTO createTemplate(PermissionTemplateDTO dto);

    /**
     * 更新模板
     * @param id 模板ID
     * @param dto 模板数据
     * @return 更新后的模板
     */
    PermissionTemplateDTO updateTemplate(Integer id, PermissionTemplateDTO dto);

    /**
     * 删除模板（逻辑删除）
     * @param id 模板ID
     */
    void deleteTemplate(Integer id);
}
