package com.foodtraceability.service.marketing;

import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.marketing.RefundApproveDTO;
import com.foodtraceability.dto.marketing.RefundCreateDTO;
import com.foodtraceability.dto.marketing.RefundQueryDTO;
import com.foodtraceability.dto.marketing.RefundVO;

/**
 * 退款申请服务接口
 * 退款审批流程：申请 → 审批 → 执行
 * 退款仅退本金，赠送按配置处理（清零或按比例扣减）
 *
 * 状态：pending-待审批 approved-已批准 rejected-已拒绝 executed-已执行 cancelled-已取消
 */
public interface RefundService {

    /**
     * 创建退款申请
     * 业务流程：
     * 1. 校验充值记录存在且支付成功
     * 2. 校验退款金额不超过可退本金
     * 3. 计算可退本金（充值本金 - 已退本金）
     * 4. 生成退款ID（rf-{yyyyMMddHHmmss}{4位随机数}）
     * 5. 更新充值记录 refund_status 为 pending
     *
     * @param createDTO 创建DTO
     * @return 创建后的退款申请VO
     */
    RefundVO createRefund(RefundCreateDTO createDTO);

    /**
     * 分页查询退款申请
     *
     * @param queryDTO 查询条件（含分页参数）
     * @return 分页结果
     */
    PageResult<RefundVO> getRefundPage(RefundQueryDTO queryDTO);

    /**
     * 审批退款申请
     * 业务流程：
     * 1. 校验退款申请存在且状态为 pending
     * 2. 同意：状态置为 approved，记录审批人和审批意见
     * 3. 拒绝：状态置为 rejected，更新充值记录 refund_status 为 rejected
     *
     * @param refundId 退款ID
     * @param approveDTO 审批DTO（approved/comment）
     */
    void approveRefund(String refundId, RefundApproveDTO approveDTO);

    /**
     * 执行退款
     * 业务流程：
     * 1. 校验退款申请存在且状态为 approved
     * 2. 执行退款：更新充值记录 refund_amount 和 refund_status
     * 3. 状态置为 executed，记录执行人和执行时间
     * 4. 写入财务流水（type=refund）
     *
     * @param refundId 退款ID
     */
    void executeRefund(String refundId);
}
