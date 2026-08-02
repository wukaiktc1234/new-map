package com.foodtraceability.service;

import com.foodtraceability.dto.*;

import java.util.List;

/**
 * 系统初始化服务
 * 处理系统初始化向导的业务逻辑
 */
public interface SystemInitService {

    /**
     * 检查系统是否已初始化
     * @return 是否已初始化
     */
    boolean isSystemInitialized();

    /**
     * 获取当前初始化状态
     * @return 初始化状态
     */
    SystemInitStatusDTO getInitStatus();

    /**
     * 开始初始化流程
     * @return 初始化状态
     */
    SystemInitStatusDTO startInit();

    /**
     * 保存企业信息
     * @param enterpriseInfo 企业信息
     * @return 是否成功
     */
    boolean saveEnterpriseInfo(EnterpriseInfoDTO enterpriseInfo);

    /**
     * 保存组织架构
     * @param organizationInfo 组织架构信息
     * @return 是否成功
     */
    boolean saveOrganization(OrganizationInfoDTO organizationInfo);

    /**
     * 获取权限模板列表
     * @return 模板列表
     */
    List<PermissionTemplateDTO> getPermissionTemplates();

    /**
     * 根据企业类型和规模获取推荐的权限模板
     * @param enterpriseType 企业类型
     * @param scale 企业规模
     * @return 推荐的模板列表
     */
    List<PermissionTemplateDTO> getRecommendedTemplates(String enterpriseType, String scale);

    /**
     * 应用权限模板
     * @param templateId 模板ID
     * @return 是否成功
     */
    boolean applyPermissionTemplate(Integer templateId);

    /**
     * 自定义角色配置
     * @param roleConfigs 角色配置列表
     * @return 是否成功
     */
    boolean configureRoles(List<RoleConfigDTO> roleConfigs);

    /**
     * 完成初始化
     * @return 是否成功
     */
    boolean completeInit();

    /**
     * 跳过初始化向导
     * @return 是否成功
     */
    boolean skipInit();

    /**
     * 重置系统初始化状态（仅用于测试）
     * @return 是否成功
     */
    boolean resetInitStatus();

    /**
     * 初始化公司信息并创建默认组织架构
     * @param companyInitDTO 公司初始化信息
     * @return 是否成功
     */
    boolean initializeCompany(CompanyInitDTO companyInitDTO);
}
