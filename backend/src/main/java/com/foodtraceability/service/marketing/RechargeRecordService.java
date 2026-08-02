package com.foodtraceability.service.marketing;

import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.marketing.RechargeRecordQueryDTO;
import com.foodtraceability.dto.marketing.RechargeRecordVO;
import com.foodtraceability.dto.marketing.RechargeRequestDTO;
import com.foodtraceability.entity.marketing.RechargeRecord;

/**
 * 充值记录服务接口
 * 提供充值记录查询、会员充值、导出等能力
 *
 * 支付状态：pending/success/failed/refunded/partial_refunded
 * 退款状态：none/pending/approved/rejected/refunded
 */
public interface RechargeRecordService {

    /**
     * 分页查询充值记录
     *
     * @param queryDTO 查询条件（含分页参数）
     * @return 分页结果
     */
    PageResult<RechargeRecordVO> getRecordPage(RechargeRecordQueryDTO queryDTO);

    /**
     * 根据ID获取充值记录详情
     *
     * @param recordId 记录ID
     * @return 记录VO，不存在返回null
     */
    RechargeRecordVO getRecordById(String recordId);

    /**
     * 会员充值
     * 业务流程：方案校验 → 生成流水号 → 创建充值记录 → 发放赠送 → 写入财务流水
     *
     * @param requestDTO 充值请求（memberId/planId/paymentMethod）
     * @return 创建的充值记录VO
     */
    RechargeRecordVO recharge(RechargeRequestDTO requestDTO);

    /**
     * 导出充值记录
     * 实际实现为占位（前端目前直接调用，后端可后续接入导出任务）
     *
     * @param queryDTO 查询条件
     */
    void exportRecords(RechargeRecordQueryDTO queryDTO);

    /**
     * 更新充值记录
     * 仅允许更新业务可变字段（会员信息、支付方式、支付状态、退款相关字段）
     * 金额、流水号等核心字段不可变更
     *
     * @param recordId 记录ID
     * @param record   含可更新字段的实体
     * @return 更新后的记录VO
     */
    RechargeRecordVO updateRecord(String recordId, RechargeRecord record);

    /**
     * 删除充值记录（逻辑删除）
     * 仅允许删除非成功支付状态的记录，避免误删财务流水
     *
     * @param recordId 记录ID
     * @return 是否删除成功
     */
    boolean deleteRecord(String recordId);
}
