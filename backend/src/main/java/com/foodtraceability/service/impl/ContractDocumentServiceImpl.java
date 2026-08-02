package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.ContractDocument;
import com.foodtraceability.mapper.ContractDocumentMapper;
import com.foodtraceability.service.ContractDocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 合同正文服务实现
 *
 * <p>实现合同正文的版本管理：
 * <ul>
 *   <li>每次保存创建新版本，旧版本 isCurrent 置为 0</li>
 *   <li>版本号递增，基于已有最大版本号 + 1</li>
 *   <li>使用 SHA-256 计算文档 hash 用于防篡改</li>
 * </ul></p>
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-03-20
 */
@Service
public class ContractDocumentServiceImpl
        extends ServiceImpl<ContractDocumentMapper, ContractDocument>
        implements ContractDocumentService {

    private static final Logger log = LoggerFactory.getLogger(ContractDocumentServiceImpl.class);

    @Override
    public ContractDocument getCurrentDocument(Long contractId) {
        if (contractId == null) {
            return null;
        }
        return baseMapper.selectCurrentByContractId(contractId);
    }

    @Override
    public List<ContractDocument> getDocumentHistory(Long contractId) {
        if (contractId == null) {
            return List.of();
        }
        return baseMapper.selectByContractId(contractId);
    }

    @Override
    public ContractDocument getDocumentByVersion(Long contractId, Integer version) {
        if (contractId == null || version == null) {
            return null;
        }
        return baseMapper.selectByContractIdAndVersion(contractId, version);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContractDocument saveDocument(Long contractId, String htmlContent, String editRemark, String operator) {
        if (contractId == null) {
            throw new IllegalArgumentException("合同ID不能为空");
        }
        if (htmlContent == null || htmlContent.isEmpty()) {
            throw new IllegalArgumentException("合同正文内容不能为空");
        }

        // 1. 将旧版本 isCurrent 设为 0
        clearCurrentVersion(contractId, operator);

        // 2. 计算新版本号（基于已有最大版本号 + 1）
        int newVersion = calculateNextVersion(contractId);

        // 3. 创建新版本
        ContractDocument document = new ContractDocument();
        document.setContractId(contractId);
        document.setHtmlContent(htmlContent);
        document.setDocumentHash(calculateDocumentHash(htmlContent));
        document.setVersion(newVersion);
        document.setSourceType(ContractDocument.SOURCE_EDIT);
        document.setSourceDesc("编辑保存");
        document.setCreatedBy(operator);
        document.setUpdatedBy(operator);
        document.setIsCurrent(ContractDocument.CURRENT_YES);
        document.setEditRemark(editRemark);
        document.setCreateTime(LocalDateTime.now());
        document.setUpdateTime(LocalDateTime.now());
        document.setDeleted(0);

        this.save(document);
        log.info("保存合同正文成功，合同ID：{}，新版本：{}，操作人：{}", contractId, newVersion, operator);
        return document;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContractDocument createDocumentFromTemplate(Long contractId, Long templateId, String htmlContent, String operator) {
        if (contractId == null) {
            throw new IllegalArgumentException("合同ID不能为空");
        }
        if (htmlContent == null || htmlContent.isEmpty()) {
            throw new IllegalArgumentException("合同正文内容不能为空");
        }

        // 1. 将旧版本 isCurrent 设为 0
        clearCurrentVersion(contractId, operator);

        // 2. 计算新版本号
        int newVersion = calculateNextVersion(contractId);

        // 3. 创建新版本
        ContractDocument document = new ContractDocument();
        document.setContractId(contractId);
        document.setHtmlContent(htmlContent);
        document.setTemplateId(templateId);
        document.setDocumentHash(calculateDocumentHash(htmlContent));
        document.setVersion(newVersion);
        document.setSourceType(ContractDocument.SOURCE_TEMPLATE);
        document.setSourceDesc("从模板创建");
        document.setCreatedBy(operator);
        document.setUpdatedBy(operator);
        document.setIsCurrent(ContractDocument.CURRENT_YES);
        document.setCreateTime(LocalDateTime.now());
        document.setUpdateTime(LocalDateTime.now());
        document.setDeleted(0);

        this.save(document);
        log.info("从模板创建合同正文成功，合同ID：{}，模板ID：{}，新版本：{}，操作人：{}",
                contractId, templateId, newVersion, operator);
        return document;
    }

    /**
     * 将合同的当前版本标记为非当前
     *
     * @param contractId 合同ID
     * @param operator   操作人
     */
    private void clearCurrentVersion(Long contractId, String operator) {
        LambdaUpdateWrapper<ContractDocument> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ContractDocument::getContractId, contractId)
                .eq(ContractDocument::getIsCurrent, ContractDocument.CURRENT_YES)
                .set(ContractDocument::getIsCurrent, ContractDocument.CURRENT_NO)
                .set(ContractDocument::getUpdatedBy, operator)
                .set(ContractDocument::getUpdateTime, LocalDateTime.now());
        baseMapper.update(null, updateWrapper);
    }

    /**
     * 计算下一个版本号（基于已有最大版本号 + 1）
     *
     * @param contractId 合同ID
     * @return 下一个版本号
     */
    private int calculateNextVersion(Long contractId) {
        List<ContractDocument> history = baseMapper.selectByContractId(contractId);
        if (history == null || history.isEmpty()) {
            return 1;
        }
        // selectByContractId 按 version DESC 排序，第一个为最大版本号
        Integer maxVersion = history.get(0).getVersion();
        return (maxVersion == null ? 0 : maxVersion) + 1;
    }

    /**
     * 计算文档内容的 SHA-256 hash
     *
     * @param content 文档内容
     * @return 64位十六进制字符串
     */
    private String calculateDocumentHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256算法不可用", e);
            throw new RuntimeException("计算文档hash失败：SHA-256算法不可用", e);
        }
    }
}
