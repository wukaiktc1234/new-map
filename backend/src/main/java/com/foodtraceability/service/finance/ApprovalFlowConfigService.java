package com.foodtraceability.service.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.entity.finance.ApprovalFlowConfig;

/**
 * 审批流配置Service接口
 * 管理财务单据的审批流配置，包括审批节点、单据类型等，支持多级审批流程
 */
public interface ApprovalFlowConfigService extends IService<ApprovalFlowConfig> {

    /**
     * 创建审批流配置
     * @param dto 创建DTO
     * @return 配置VO
     */
    ApprovalFlowConfigVO create(ApprovalFlowConfigCreateDTO dto);

    /**
     * 更新审批流配置
     * @param configId 配置ID
     * @param dto 更新DTO
     * @return 是否成功
     */
    boolean update(Long configId, ApprovalFlowConfigUpdateDTO dto);

    /**
     * 删除审批流配置（逻辑删除）
     * @param configId 配置ID
     * @return 是否成功
     */
    boolean delete(Long configId);

    /**
     * 获取配置详情
     * @param configId 配置ID
     * @return 配置VO
     */
    ApprovalFlowConfigVO getDetail(Long configId);

    /**
     * 分页查询审批流配置
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<ApprovalFlowConfigVO> getPage(ApprovalFlowConfigQueryDTO query);

    /**
     * 按单据类型查询启用的审批流配置
     * @param documentType 单据类型
     * @return 配置VO
     */
    ApprovalFlowConfigVO getByDocumentType(String documentType);

    /**
     * 启用/停用配置
     * @param configId 配置ID
     * @param enabled 是否启用
     * @return 是否成功
     */
    boolean toggleEnabled(Long configId, Boolean enabled);
}
