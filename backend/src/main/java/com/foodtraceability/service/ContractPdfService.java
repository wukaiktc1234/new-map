package com.foodtraceability.service;

import com.foodtraceability.entity.ContractTemplate;
import com.foodtraceability.entity.EmployeeLaborContract;
import com.foodtraceability.entity.OnboardingArchive;

import java.io.OutputStream;
import java.util.Map;

/**
 * 合同PDF生成服务接口
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-03-20
 */
public interface ContractPdfService {

    /**
     * 根据模板生成合同PDF
     *
     * @param template 合同模板
     * @param variables 变量映射
     * @param outputStream 输出流
     * @return 生成的HTML内容
     */
    String generatePdf(ContractTemplate template, Map<String, Object> variables, OutputStream outputStream);

    /**
     * 根据合同ID生成PDF
     *
     * @param contractId 合同ID
     * @return PDF文件URL
     */
    String generateContractPdf(Long contractId);

    /**
     * 重新生成合同PDF
     *
     * @param contractId 合同ID
     * @return PDF文件URL
     */
    String regenerateContractPdf(Long contractId);

    /**
     * 将签名合成到PDF
     *
     * @param contractId 合同ID
     * @param signatureData 签名数据（Base64）
     * @param signerType 签署人类型
     * @return 合成后的PDF URL
     */
    String mergeSignatureToPdf(Long contractId, String signatureData, String signerType);

    /**
     * 获取合同预览HTML
     *
     * @param contractId 合同ID
     * @return HTML内容
     */
    String getContractPreviewHtml(Long contractId);

    /**
     * 构建合同变量映射
     *
     * @param contract 合同信息
     * @param archive 入职档案
     * @return 变量映射
     */
    Map<String, Object> buildContractVariables(EmployeeLaborContract contract, OnboardingArchive archive);
}
