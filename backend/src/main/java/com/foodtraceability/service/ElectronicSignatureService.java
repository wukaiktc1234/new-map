package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.SignatureStatusDTO;
import com.foodtraceability.dto.VerifyCodeResponseDTO;
import com.foodtraceability.entity.ElectronicSignature;

import java.util.List;

/**
 * 电子签名服务接口
 *
 * <p>提供合同签署流程管理能力，包括：
 * <ul>
 *   <li>初始化多方签署记录（公司方 + 员工方）</li>
 *   <li>发送签署验证码</li>
 *   <li>签署合同（带签名数据 + 验证码验证）</li>
 *   <li>拒绝签署</li>
 *   <li>查询签署记录和签署状态看板</li>
 * </ul></p>
 *
 * <p>签署流程：初始化签署记录 → 发送验证码 → 验证码验证 → 签署 →
 * 检查是否全部签署完成 → 更新合同状态</p>
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-03-20
 */
@Deprecated
public interface ElectronicSignatureService extends IService<ElectronicSignature> {

    /**
     * 获取合同的签署记录列表
     *
     * @param contractId 合同ID
     * @return 签署记录列表，按创建时间排序
     */
    List<ElectronicSignature> getSignaturesByContractId(Long contractId);

    /**
     * 获取合同签署状态摘要（签署进度看板数据）
     *
     * @param contractId 合同ID
     * @return 签署状态看板数据，包含总签署人数、已签署人数、待签署人数、
     *         已拒绝人数、签署进度百分比、各方签署详情
     */
    SignatureStatusDTO getSignatureStatus(Long contractId);

    /**
     * 初始化合同签署记录
     *
     * <p>创建2条签署记录：公司方（company）和员工方（employee），状态为 pending。
     * 若已存在签署记录则跳过创建。</p>
     *
     * @param contractId 合同ID
     * @param operator   操作人
     * @return 创建的签署记录列表
     */
    List<ElectronicSignature> initSignatures(Long contractId, String operator);

    /**
     * 签署合同（带签名数据）
     *
     * <p>逻辑：
     * <ol>
     *   <li>验证验证码是否正确</li>
     *   <li>更新对应签署记录：status=signed, signatureData, signTime, signIp, signDevice, verifyCode, verifyTime</li>
     *   <li>检查是否所有签署方都已签署</li>
     *   <li>如果全部签署完成，更新合同状态为 signed，并设置 companySignTime/employeeSignTime</li>
     * </ol></p>
     *
     * @param contractId    合同ID
     * @param signerType    签署人类型: employee-员工, company-公司
     * @param signatureData 签名数据（Base64）
     * @param signIp        签署IP地址
     * @param signDevice    签署设备信息
     * @param verifyCode    验证码
     * @param operator      操作人
     * @return 是否签署成功
     */
    boolean signContract(Long contractId, String signerType, String signatureData,
                         String signIp, String signDevice, String verifyCode, String operator);

    /**
     * 拒绝签署
     *
     * <p>更新签署记录状态为 rejected。</p>
     *
     * @param contractId 合同ID
     * @param signerType 签署人类型: employee-员工, company-公司
     * @param reason     拒绝原因
     * @param operator   操作人
     * @return 是否拒绝成功
     */
    boolean rejectSignature(Long contractId, String signerType, String reason, String operator);

    /**
     * 发送验证码
     *
     * <p>生成6位随机验证码，存入签署记录的 verifyCode 字段。
     * 实际项目应通过短信/邮件发送，此处返回验证码用于开发调试。</p>
     *
     * @param contractId 合同ID
     * @param signerType 签署人类型: employee-员工, company-公司
     * @return 验证码响应（含验证码和过期时间）
     */
    VerifyCodeResponseDTO sendVerifyCode(Long contractId, String signerType);
}
