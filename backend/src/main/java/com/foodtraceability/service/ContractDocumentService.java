package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.ContractDocument;

import java.util.List;

/**
 * 合同正文服务接口
 *
 * <p>提供合同正文的版本管理能力，包括获取当前正文、版本历史、
 * 保存新版本（自动递增版本号、计算文档hash）、从模板创建正文等。</p>
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-03-20
 */
public interface ContractDocumentService extends IService<ContractDocument> {

    /**
     * 获取合同当前正文
     *
     * @param contractId 合同ID
     * @return 当前版本正文，不存在返回 null
     */
    ContractDocument getCurrentDocument(Long contractId);

    /**
     * 获取版本历史（按版本号倒序）
     *
     * @param contractId 合同ID
     * @return 版本历史列表
     */
    List<ContractDocument> getDocumentHistory(Long contractId);

    /**
     * 获取指定版本
     *
     * @param contractId 合同ID
     * @param version    版本号
     * @return 指定版本正文，不存在返回 null
     */
    ContractDocument getDocumentByVersion(Long contractId, Integer version);

    /**
     * 保存合同正文（创建新版本）
     *
     * <p>逻辑：
     * <ol>
     *   <li>将旧版本 isCurrent 设为 0</li>
     *   <li>创建新版本 isCurrent=1，version 递增</li>
     *   <li>计算 documentHash（SHA-256）</li>
     *   <li>sourceType 设为 "edit"</li>
     * </ol></p>
     *
     * @param contractId  合同ID
     * @param htmlContent 合同正文HTML内容
     * @param editRemark  修改备注
     * @param operator    操作人
     * @return 新创建的合同正文
     */
    ContractDocument saveDocument(Long contractId, String htmlContent, String editRemark, String operator);

    /**
     * 从模板创建合同正文
     *
     * <p>sourceType 设为 "template"。若合同已有当前正文，将旧版本 isCurrent 置为 0。</p>
     *
     * @param contractId  合同ID
     * @param templateId  模板ID
     * @param htmlContent 合同正文HTML内容（来自模板渲染结果）
     * @param operator    操作人
     * @return 新创建的合同正文
     */
    ContractDocument createDocumentFromTemplate(Long contractId, Long templateId, String htmlContent, String operator);
}
