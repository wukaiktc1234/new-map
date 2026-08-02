package com.foodtraceability.service;

import com.foodtraceability.entity.VoucherHeader;
import com.foodtraceability.entity.VoucherLine;

import java.util.List;
import java.util.Map;

/**
 * 凭证服务接口
 * 处理凭证完整性控制、审核流程等业务逻辑
 * @author example
 * @since 2025-12-06
 */
public interface VoucherService {

    /**
     * 检查凭证完整性
     * @param header 凭证头
     * @param lines 凭证行列表
     * @return 检查结果，true表示完整，false表示不完整
     */
    boolean checkVoucherIntegrity(VoucherHeader header, List<VoucherLine> lines);

    /**
     * 检查凭证借贷平衡
     * @param lines 凭证行列表
     * @return 检查结果，true表示平衡，false表示不平衡
     */
    boolean checkDebitCreditBalance(List<VoucherLine> lines);

    /**
     * 检查凭证日期与会计期间一致性
     * @param header 凭证头
     * @return 检查结果，true表示一致，false表示不一致
     */
    boolean checkDatePeriodConsistency(VoucherHeader header);

    /**
     * 保存完整凭证（包括头和行）
     * @param header 凭证头
     * @param lines 凭证行列表
     * @return 保存结果，true表示成功，false表示失败
     */
    boolean saveCompleteVoucher(VoucherHeader header, List<VoucherLine> lines);

    /**
     * 提交凭证进行审核
     * @param voucherId 凭证ID
     * @return 提交结果，true表示成功，false表示失败
     */
    boolean submitVoucher(Long voucherId);

    /**
     * 审核凭证
     * @param voucherId 凭证ID
     * @param approved 是否通过审核
     * @param comment 审核意见
     * @return 审核结果，true表示成功，false表示失败
     */
    boolean approveVoucher(Long voucherId, boolean approved, String comment);

    /**
     * 记账凭证
     * @param voucherId 凭证ID
     * @return 记账结果，true表示成功，false表示失败
     */
    boolean postVoucher(Long voucherId);

    /**
     * 红冲凭证
     * @param voucherId 原凭证ID
     * @return 红冲结果，true表示成功，false表示失败
     */
    boolean reverseVoucher(Long voucherId);

    /**
     * 作废凭证
     * @param voucherId 凭证ID
     * @return 作废结果，true表示成功，false表示失败
     */
    boolean cancelVoucher(Long voucherId);

    /**
     * 获取凭证详情（包括头和行）
     * @param voucherId 凭证ID
     * @return 包含头和行的完整凭证
     */
    VoucherHeader getVoucherWithLines(Long voucherId);

    /**
     * 自动生成凭证
     * @param businessType 业务类型
     * @param businessData 业务数据
     * @return 生成的凭证头ID，null表示生成失败
     */
    Long autoGenerateVoucher(String businessType, Map<String, Object> businessData);

    /**
     * 批量生成凭证
     * @param businessType 业务类型
     * @param businessDataList 业务数据列表
     * @return 生成的凭证头ID列表
     */
    List<Long> batchGenerateVouchers(String businessType, List<Map<String, Object>> businessDataList);

    /**
     * 根据业务ID和类型生成凭证
     * @param businessId 业务ID
     * @param businessType 业务类型
     * @return 生成的凭证头ID，null表示生成失败
     */
    Long generateVoucherByBusinessIdAndType(Long businessId, String businessType);
}
