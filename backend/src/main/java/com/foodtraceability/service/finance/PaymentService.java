package com.foodtraceability.service.finance;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.finance.PaymentCreateDTO;
import com.foodtraceability.dto.finance.PaymentVO;
import com.foodtraceability.entity.finance.Payment;

import java.util.List;

/**
 * 付款单状态常量
 */
interface PaymentStatus {
    /** 已确认 */
    int CONFIRMED = 1;
    /** 已作废 */
    int VOIDED = 2;
}

/**
 * 付款单Service接口
 * 管理付款登记的完整流程：四账联动（应付账款+银行账户+资金流水+会计凭证）
 */
public interface PaymentService extends IService<Payment> {

    /**
     * 登记付款（四账联动核心方法）
     * 一个事务内完成：
     * 1. 创建付款单记录
     * 2. 更新应付账款余额和状态
     * 3. 创建资金流水（自动联动银行账户余额扣减）
     * 4. 生成付款会计凭证（借：应付账款 / 贷：银行存款）
     *
     * @param dto 付款登记DTO
     * @return 付款单VO（含关联的流水ID和凭证ID）
     */
    PaymentVO registerPayment(PaymentCreateDTO dto);

    /**
     * 查询某笔应付账款的所有付款记录
     * @param payableId 应付账款ID
     * @return 付款记录列表
     */
    List<PaymentVO> getPaymentHistoryByPayableId(Long payableId);

    /**
     * 获取付款单详情
     * @param paymentId 付款单ID
     * @return 付款单VO
     */
    PaymentVO getDetail(Long paymentId);

    /**
     * 更新付款单（仅更新可编辑字段：金额/方式/银行账户/日期/备注）
     * @param paymentId 付款单ID
     * @param dto       付款登记DTO
     * @return 更新后的付款单VO
     */
    PaymentVO update(Long paymentId, PaymentCreateDTO dto);

    /**
     * 作废付款单（四账联动回滚）
     *
     * <p>同一事务内完成：
     * 1. 校验付款单存在且状态为已确认
     * 2. 将付款单状态改为已作废，记录作废时间/备注
     * 3. 回滚应付账款（paid_amount - amount, balance_amount + amount）
     * 4. 回滚银行账户余额（balance + amount）
     * 5. 创建反向资金流水（方向=收入，类别=采购付款退回）
     * 6. 创建红冲会计凭证（借：银行存款 / 贷：应付账款）
     * 7. 回写新流水ID和新凭证ID到付款单</p>
     *
     * @param paymentId 付款单ID
     * @param remark    作废备注
     * @return 作废后的付款单VO
     */
    PaymentVO voidPayment(Long paymentId, String remark);
}
