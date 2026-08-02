package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.SignatureStatusDTO;
import com.foodtraceability.dto.VerifyCodeResponseDTO;
import com.foodtraceability.entity.ElectronicSignature;
import com.foodtraceability.entity.EmployeeLaborContract;
import com.foodtraceability.mapper.ElectronicSignatureMapper;
import com.foodtraceability.mapper.EmployeeLaborContractMapper;
import com.foodtraceability.service.ElectronicSignatureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 电子签名服务实现
 *
 * <p>实现合同签署流程管理，支持多方签署（公司方 + 员工方），
 * 包含验证码验证、签署状态跟踪、签署完成后自动更新合同状态等功能。</p>
 *
 * <p>签署流程：
 * <ol>
 *   <li>初始化签署记录（initSignatures）</li>
 *   <li>发送验证码（sendVerifyCode）</li>
 *   <li>签署合同（signContract，含验证码验证）</li>
 *   <li>全部签署完成后自动更新合同状态为 signed</li>
 * </ol></p>
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-03-20
 */
@Service
@Deprecated
public class ElectronicSignatureServiceImpl
        extends ServiceImpl<ElectronicSignatureMapper, ElectronicSignature>
        implements ElectronicSignatureService {

    private static final Logger log = LoggerFactory.getLogger(ElectronicSignatureServiceImpl.class);

    /** 验证码有效期（分钟） */
    private static final int VERIFY_CODE_EXPIRE_MINUTES = 5;

    /** 验证码长度 */
    private static final int VERIFY_CODE_LENGTH = 6;

    private final EmployeeLaborContractMapper contractMapper;

    public ElectronicSignatureServiceImpl(EmployeeLaborContractMapper contractMapper) {
        this.contractMapper = contractMapper;
    }

    @Override
    public List<ElectronicSignature> getSignaturesByContractId(Long contractId) {
        if (contractId == null) {
            return List.of();
        }
        return baseMapper.selectByContractId(contractId);
    }

    @Override
    public SignatureStatusDTO getSignatureStatus(Long contractId) {
        if (contractId == null) {
            return null;
        }
        EmployeeLaborContract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            log.warn("获取签署状态失败，合同不存在：{}", contractId);
            return null;
        }
        List<ElectronicSignature> signatures = baseMapper.selectByContractId(contractId);
        return buildSignatureStatus(contract, signatures);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ElectronicSignature> initSignatures(Long contractId, String operator) {
        if (contractId == null) {
            throw new IllegalArgumentException("合同ID不能为空");
        }
        EmployeeLaborContract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new IllegalArgumentException("合同不存在：" + contractId);
        }
        // 检查是否已有签署记录，避免重复初始化
        List<ElectronicSignature> existing = baseMapper.selectByContractId(contractId);
        if (existing != null && !existing.isEmpty()) {
            log.info("合同已存在签署记录，跳过初始化，合同ID：{}，现有记录数：{}", contractId, existing.size());
            return existing;
        }
        List<ElectronicSignature> created = new ArrayList<>();
        // 创建公司方签署记录
        created.add(createSignatureRecord(contractId,
                ElectronicSignature.SIGNER_COMPANY, "公司代表", operator));
        // 创建员工方签署记录
        String employeeName = contract.getEmployeeName() != null
                ? contract.getEmployeeName() : "员工";
        created.add(createSignatureRecord(contractId,
                ElectronicSignature.SIGNER_EMPLOYEE, employeeName, operator));
        log.info("初始化合同签署记录成功，合同ID：{}，操作人：{}，签署方数量：{}",
                contractId, operator, created.size());
        return created;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean signContract(Long contractId, String signerType, String signatureData,
                                String signIp, String signDevice, String verifyCode,
                                String operator) {
        // 1. 参数校验
        validateSignParams(contractId, signerType, signatureData, verifyCode);
        // 2. 获取并校验签署记录
        ElectronicSignature signature = getAndValidatePendingSignature(contractId, signerType);
        // 3. 验证码校验
        validateVerifyCode(signature, verifyCode);
        // 4. 更新签署记录
        updateSignatureAsSigned(signature, signatureData, signIp, signDevice, verifyCode);
        log.info("签署成功，合同ID：{}，签署人类型：{}，操作人：{}", contractId, signerType, operator);
        // 5. 检查是否所有签署方都已签署，若是则更新合同状态
        checkAndUpdateContractStatusIfAllSigned(contractId, signerType);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rejectSignature(Long contractId, String signerType, String reason, String operator) {
        if (contractId == null) {
            throw new IllegalArgumentException("合同ID不能为空");
        }
        if (signerType == null || signerType.trim().isEmpty()) {
            throw new IllegalArgumentException("签署人类型不能为空");
        }
        ElectronicSignature signature = baseMapper.selectByContractIdAndSignerType(contractId, signerType);
        if (signature == null) {
            throw new IllegalArgumentException("签署记录不存在，合同ID：" + contractId + "，签署人类型：" + signerType);
        }
        if (!ElectronicSignature.STATUS_PENDING.equals(signature.getStatus())) {
            throw new IllegalStateException("签署记录状态不正确，无法拒绝，当前状态：" + signature.getStatus());
        }
        signature.setStatus(ElectronicSignature.STATUS_REJECTED);
        signature.setSignTime(LocalDateTime.now());
        signature.setVerifyTime(LocalDateTime.now());
        this.updateById(signature);
        log.info("拒绝签署成功，合同ID：{}，签署人类型：{}，拒绝原因：{}，操作人：{}",
                contractId, signerType, reason, operator);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VerifyCodeResponseDTO sendVerifyCode(Long contractId, String signerType) {
        if (contractId == null) {
            throw new IllegalArgumentException("合同ID不能为空");
        }
        if (signerType == null || signerType.trim().isEmpty()) {
            throw new IllegalArgumentException("签署人类型不能为空");
        }
        ElectronicSignature signature = baseMapper.selectByContractIdAndSignerType(contractId, signerType);
        if (signature == null) {
            throw new IllegalArgumentException("签署记录不存在，合同ID：" + contractId + "，签署人类型：" + signerType);
        }
        // 生成6位随机验证码
        String verifyCode = generateVerifyCode();
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(VERIFY_CODE_EXPIRE_MINUTES);
        signature.setVerifyCode(verifyCode);
        this.updateById(signature);
        log.info("发送验证码成功，合同ID：{}，签署人类型：{}", contractId, signerType);
        // 实际项目应通过短信/邮件发送验证码，此处直接返回用于开发调试
        return new VerifyCodeResponseDTO(verifyCode, expireTime);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 创建单条签署记录
     *
     * @param contractId 合同ID
     * @param signerType 签署人类型
     * @param signerName 签署人姓名
     * @param operator   操作人
     * @return 创建的签署记录
     */
    private ElectronicSignature createSignatureRecord(Long contractId, String signerType,
                                                      String signerName, String operator) {
        ElectronicSignature signature = new ElectronicSignature();
        signature.setContractId(contractId);
        signature.setSignerType(signerType);
        signature.setSignerName(signerName);
        signature.setStatus(ElectronicSignature.STATUS_PENDING);
        signature.setCreateTime(LocalDateTime.now());
        signature.setUpdateTime(LocalDateTime.now());
        signature.setDeleted(0);
        this.save(signature);
        return signature;
    }

    /**
     * 构建签署状态看板数据
     *
     * @param contract   合同信息
     * @param signatures 签署记录列表
     * @return 签署状态看板DTO
     */
    private SignatureStatusDTO buildSignatureStatus(EmployeeLaborContract contract,
                                                    List<ElectronicSignature> signatures) {
        SignatureStatusDTO statusDTO = new SignatureStatusDTO();
        statusDTO.setContractId(contract.getId());
        statusDTO.setContractNo(contract.getContractNo());
        statusDTO.setEmployeeName(contract.getEmployeeName());
        int total = signatures != null ? signatures.size() : 0;
        int signed = 0;
        int pending = 0;
        int rejected = 0;
        List<SignatureStatusDTO.SignerDetail> signerDetails = new ArrayList<>();
        if (signatures != null) {
            for (ElectronicSignature sig : signatures) {
                if (ElectronicSignature.STATUS_SIGNED.equals(sig.getStatus())) {
                    signed++;
                } else if (ElectronicSignature.STATUS_PENDING.equals(sig.getStatus())) {
                    pending++;
                } else if (ElectronicSignature.STATUS_REJECTED.equals(sig.getStatus())) {
                    rejected++;
                }
                signerDetails.add(convertToSignerDetail(sig));
            }
        }
        statusDTO.setTotalSigners(total);
        statusDTO.setSignedCount(signed);
        statusDTO.setPendingCount(pending);
        statusDTO.setRejectedCount(rejected);
        // 计算签署进度百分比（已签署人数 / 总人数 * 100）
        int progressPercent = total > 0 ? (signed * 100 / total) : 0;
        statusDTO.setProgressPercent(progressPercent);
        statusDTO.setSigners(signerDetails);
        return statusDTO;
    }

    /**
     * 将签署记录转换为签署方详情
     *
     * @param signature 签署记录
     * @return 签署方详情
     */
    private SignatureStatusDTO.SignerDetail convertToSignerDetail(ElectronicSignature signature) {
        SignatureStatusDTO.SignerDetail detail = new SignatureStatusDTO.SignerDetail();
        detail.setSignerType(signature.getSignerType());
        detail.setSignerName(signature.getSignerName());
        detail.setStatus(signature.getStatus());
        detail.setSignTime(signature.getSignTime());
        detail.setSignIp(signature.getSignIp());
        return detail;
    }

    /**
     * 校验签署请求参数
     *
     * @param contractId    合同ID
     * @param signerType    签署人类型
     * @param signatureData 签名数据
     * @param verifyCode    验证码
     */
    private void validateSignParams(Long contractId, String signerType, String signatureData, String verifyCode) {
        if (contractId == null) {
            throw new IllegalArgumentException("合同ID不能为空");
        }
        if (signerType == null || signerType.trim().isEmpty()) {
            throw new IllegalArgumentException("签署人类型不能为空");
        }
        if (signatureData == null || signatureData.trim().isEmpty()) {
            throw new IllegalArgumentException("签名数据不能为空");
        }
        if (verifyCode == null || verifyCode.trim().isEmpty()) {
            throw new IllegalArgumentException("验证码不能为空");
        }
    }

    /**
     * 获取并校验待签署的签署记录
     *
     * @param contractId 合同ID
     * @param signerType 签署人类型
     * @return 签署记录
     */
    private ElectronicSignature getAndValidatePendingSignature(Long contractId, String signerType) {
        ElectronicSignature signature = baseMapper.selectByContractIdAndSignerType(contractId, signerType);
        if (signature == null) {
            throw new IllegalArgumentException("签署记录不存在，请先初始化签署记录");
        }
        if (!ElectronicSignature.STATUS_PENDING.equals(signature.getStatus())) {
            throw new IllegalStateException("签署记录状态不正确，无法签署，当前状态：" + signature.getStatus());
        }
        return signature;
    }

    /**
     * 验证验证码是否正确
     *
     * @param signature   签署记录
     * @param verifyCode  用户输入的验证码
     */
    private void validateVerifyCode(ElectronicSignature signature, String verifyCode) {
        String storedCode = signature.getVerifyCode();
        if (storedCode == null || storedCode.trim().isEmpty()) {
            throw new IllegalStateException("验证码未生成，请先发送验证码");
        }
        if (!storedCode.equals(verifyCode)) {
            throw new IllegalArgumentException("验证码不正确");
        }
    }

    /**
     * 更新签署记录为已签署状态
     *
     * @param signature     签署记录
     * @param signatureData 签名数据
     * @param signIp        签署IP
     * @param signDevice    签署设备
     * @param verifyCode    验证码
     */
    private void updateSignatureAsSigned(ElectronicSignature signature, String signatureData,
                                         String signIp, String signDevice, String verifyCode) {
        LocalDateTime now = LocalDateTime.now();
        signature.setStatus(ElectronicSignature.STATUS_SIGNED);
        signature.setSignatureData(signatureData);
        signature.setSignTime(now);
        signature.setSignIp(signIp);
        signature.setSignDevice(signDevice);
        signature.setVerifyCode(verifyCode);
        signature.setVerifyTime(now);
        signature.setUpdateTime(now);
        this.updateById(signature);
    }

    /**
     * 检查是否所有签署方都已签署，若是则更新合同状态为 signed
     *
     * @param contractId 合同ID
     * @param signerType 当前签署人类型
     */
    private void checkAndUpdateContractStatusIfAllSigned(Long contractId, String signerType) {
        List<ElectronicSignature> allSignatures = baseMapper.selectByContractId(contractId);
        if (allSignatures == null || allSignatures.isEmpty()) {
            return;
        }
        boolean allSigned = allSignatures.stream()
                .allMatch(sig -> ElectronicSignature.STATUS_SIGNED.equals(sig.getStatus()));
        if (allSigned) {
            updateContractStatusToSigned(contractId, signerType, allSignatures);
        }
    }

    /**
     * 更新合同状态为已签署，并设置各方签署时间
     *
     * @param contractId    合同ID
     * @param lastSignerType 最后签署人类型
     * @param signatures    所有签署记录
     */
    private void updateContractStatusToSigned(Long contractId, String lastSignerType,
                                              List<ElectronicSignature> signatures) {
        EmployeeLaborContract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            log.warn("更新合同状态失败，合同不存在：{}", contractId);
            return;
        }
        // 设置各方签署时间（将 LocalDateTime 转换为 Date）
        for (ElectronicSignature sig : signatures) {
            if (ElectronicSignature.SIGNER_COMPANY.equals(sig.getSignerType())
                    && sig.getSignTime() != null) {
                contract.setCompanySignTime(convertToDate(sig.getSignTime()));
            } else if (ElectronicSignature.SIGNER_EMPLOYEE.equals(sig.getSignerType())
                    && sig.getSignTime() != null) {
                contract.setEmployeeSignTime(convertToDate(sig.getSignTime()));
            }
        }
        contract.setStatus(EmployeeLaborContract.STATUS_SIGNED);
        contractMapper.updateById(contract);
        log.info("所有签署方已完成签署，合同状态更新为 signed，合同ID：{}，最后签署人：{}",
                contractId, lastSignerType);
    }

    /**
     * 生成6位随机数字验证码
     *
     * @return 6位验证码字符串
     */
    private String generateVerifyCode() {
        int code = ThreadLocalRandom.current().nextInt(
                (int) Math.pow(10, VERIFY_CODE_LENGTH - 1),
                (int) Math.pow(10, VERIFY_CODE_LENGTH));
        return String.valueOf(code);
    }

    /**
     * 将 LocalDateTime 转换为 Date
     *
     * @param localDateTime LocalDateTime
     * @return Date
     */
    private Date convertToDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
}
