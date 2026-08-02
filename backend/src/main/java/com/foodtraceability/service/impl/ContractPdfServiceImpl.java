package com.foodtraceability.service.impl;

import com.foodtraceability.entity.ContractTemplate;
import com.foodtraceability.entity.ElectronicSignature;
import com.foodtraceability.entity.EmployeeLaborContract;
import com.foodtraceability.entity.OnboardingArchive;
import com.foodtraceability.mapper.ElectronicSignatureMapper;
import com.foodtraceability.service.ContractPdfService;
import com.foodtraceability.service.ContractTemplateService;
import com.foodtraceability.service.EmployeeLaborContractService;
import com.foodtraceability.service.OnboardingArchiveService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 合同PDF生成服务实现
 * 
 * 注：实际生产环境应使用Flying Saucer库生成PDF
 * 此实现为简化版本，生成HTML文件供预览
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-03-20
 */
@Service
public class ContractPdfServiceImpl implements ContractPdfService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ContractPdfServiceImpl.class);

    public ContractPdfServiceImpl(ContractTemplateService templateService, EmployeeLaborContractService contractService, OnboardingArchiveService archiveService, ElectronicSignatureMapper signatureMapper) {
        this.templateService = templateService;
        this.contractService = contractService;
        this.archiveService = archiveService;
        this.signatureMapper = signatureMapper;
    }

    private final ContractTemplateService templateService;
    private final EmployeeLaborContractService contractService;
    private final OnboardingArchiveService archiveService;
    private final ElectronicSignatureMapper signatureMapper;
    @Value("${app.contract.storage-path:./contracts}")
    private String storagePath;

    @Override
    public String generatePdf(ContractTemplate template, Map<String, Object> variables, OutputStream outputStream) {
        String htmlContent = template.getTemplateContent();
        // 替换变量
        if (variables != null && !variables.isEmpty()) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                String placeholder = "{{" + entry.getKey() + "}}";
                String value = entry.getValue() != null ? entry.getValue().toString() : "";
                htmlContent = htmlContent.replace(placeholder, value);
            }
        }
        // 清除未填充的变量
        Pattern pattern = Pattern.compile("\\{\\{[^}]+\\}\\}");
        Matcher matcher = pattern.matcher(htmlContent);
        htmlContent = matcher.replaceAll("______");
        // 写入输出流（实际应为PDF）
        try {
            outputStream.write(htmlContent.getBytes("UTF-8"));
        } catch (IOException e) {
            log.error("生成PDF失败", e);
            throw new RuntimeException("生成PDF失败: " + e.getMessage());
        }
        return htmlContent;
    }

    @Override
    public String generateContractPdf(Long contractId) {
        EmployeeLaborContract contract = contractService.getById(contractId);
        if (contract == null) {
            throw new RuntimeException("合同不存在");
        }
        // 获取模板
        ContractTemplate template = null;
        if (contract.getTemplateId() != null) {
            template = templateService.getById(contract.getTemplateId());
        }
        if (template == null) {
            template = templateService.getByTemplateCode("STANDARD_LABOR_CONTRACT");
        }
        if (template == null) {
            throw new RuntimeException("未找到合适的合同模板");
        }
        // 获取档案信息
        OnboardingArchive archive = null;
        if (contract.getArchiveId() != null) {
            archive = archiveService.getById(contract.getArchiveId());
        }
        // 构建变量
        Map<String, Object> variables = buildContractVariables(contract, archive);
        // 生成文件名
        String fileName = "contract_" + contractId + "_" + System.currentTimeMillis() + ".html";
        String filePath = storagePath + "/" + fileName;
        try {
            // 确保目录存在
            Path dirPath = Paths.get(storagePath);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
            // 生成HTML内容
            String htmlContent = templateService.previewTemplate(template.getId(), variables);
            // 写入文件
            Files.write(Paths.get(filePath), htmlContent.getBytes("UTF-8"));
            // 更新合同记录
            contract.setPdfFileUrl("/contracts/" + fileName);
            contract.setGeneratedTime(new Date());
            contractService.updateById(contract);
            log.info("合同PDF生成成功：{}", filePath);
            return "/contracts/" + fileName;
        } catch (Exception e) {
            log.error("生成合同PDF失败", e);
            throw new RuntimeException("生成合同PDF失败: " + e.getMessage());
        }
    }

    @Override
    public String regenerateContractPdf(Long contractId) {
        return generateContractPdf(contractId);
    }

    @Override
    public String mergeSignatureToPdf(Long contractId, String signatureData, String signerType) {
        EmployeeLaborContract contract = contractService.getById(contractId);
        if (contract == null) {
            throw new RuntimeException("合同不存在");
        }
        // 获取签名记录
        List<ElectronicSignature> signatures = signatureMapper.selectByContractId(contractId);
        // 实际生产环境应使用PDFBox将签名图片合成到PDF
        // 这里简化处理，更新状态
        if ("employee".equals(signerType)) {
            contract.setEmployeeSignTime(new Date());
        } else if ("company".equals(signerType)) {
            contract.setCompanySignTime(new Date());
        }
        // 检查是否双方都已签署
        if (contract.getCompanySignTime() != null && contract.getEmployeeSignTime() != null) {
            contract.setStatus(EmployeeLaborContract.STATUS_SIGNED);
            // 生成已签署版本的PDF
            String signedFileName = "contract_" + contractId + "_signed_" + System.currentTimeMillis() + ".html";
            contract.setSignedPdfUrl("/contracts/" + signedFileName);
        }
        contractService.updateById(contract);
        log.info("签名合成完成，合同ID：{}，签署人类型：{}", contractId, signerType);
        return contract.getSignedPdfUrl();
    }

    @Override
    public String getContractPreviewHtml(Long contractId) {
        EmployeeLaborContract contract = contractService.getById(contractId);
        if (contract == null) {
            throw new RuntimeException("合同不存在");
        }
        // 获取模板
        ContractTemplate template = null;
        if (contract.getTemplateId() != null) {
            template = templateService.getById(contract.getTemplateId());
        }
        if (template == null) {
            template = templateService.getByTemplateCode("STANDARD_LABOR_CONTRACT");
        }
        if (template == null) {
            throw new RuntimeException("未找到合适的合同模板");
        }
        // 获取档案信息
        OnboardingArchive archive = null;
        if (contract.getArchiveId() != null) {
            archive = archiveService.getById(contract.getArchiveId());
        }
        // 构建变量
        Map<String, Object> variables = buildContractVariables(contract, archive);
        return templateService.previewTemplate(template.getId(), variables);
    }

    @Override
    public Map<String, Object> buildContractVariables(EmployeeLaborContract contract, OnboardingArchive archive) {
        Map<String, Object> variables = new HashMap<>();
        // 合同基本信息
        variables.put("employeeName", contract.getEmployeeName() != null ? contract.getEmployeeName() : "");
        variables.put("employeeCode", contract.getEmployeeCode() != null ? contract.getEmployeeCode() : "");
        variables.put("position", contract.getPosition() != null ? contract.getPosition() : "");
        variables.put("salary", contract.getSalary() != null ? contract.getSalary().toString() : "");
        variables.put("workLocation", contract.getWorkLocation() != null ? contract.getWorkLocation() : "公司所在地");
        // 日期格式化
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
        if (contract.getStartDate() != null) {
            variables.put("startDate", contract.getStartDate().format(dateFormatter));
        } else {
            variables.put("startDate", "");
        }
        if (contract.getEndDate() != null) {
            variables.put("endDate", contract.getEndDate().format(dateFormatter));
        } else {
            variables.put("endDate", "无固定期限");
        }
        // 试用期
        variables.put("probationMonths", contract.getProbationMonths() != null ? contract.getProbationMonths().toString() : "0");
        // 试用期薪资（通常为正式薪资的80%）
        if (contract.getSalary() != null && contract.getProbationMonths() != null && contract.getProbationMonths() > 0) {
            variables.put("probationSalary", String.valueOf((int) (contract.getSalary().doubleValue() * 0.8)));
        } else {
            variables.put("probationSalary", variables.get("salary"));
        }
        // 合同类型文本
        String contractTypeText = "固定期限";
        if ("open-ended".equals(contract.getContractType())) {
            contractTypeText = "无固定期限";
        } else if ("project".equals(contract.getContractType())) {
            contractTypeText = "项目制";
        }
        variables.put("contractTypeText", contractTypeText);
        // 从档案获取更多信息
        if (archive != null) {
            variables.put("departmentName", archive.getDepartmentName() != null ? archive.getDepartmentName() : "");
        } else {
            variables.put("departmentName", "");
        }
        // 签署日期
        if (contract.getSignDate() != null) {
            variables.put("signDate", contract.getSignDate().format(dateFormatter));
        } else {
            variables.put("signDate", LocalDate.now().format(dateFormatter));
        }
        return variables;
    }
}
