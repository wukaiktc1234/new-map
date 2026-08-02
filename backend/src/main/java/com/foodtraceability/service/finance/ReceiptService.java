package com.foodtraceability.service.finance;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.finance.ReceiptCreateDTO;
import com.foodtraceability.dto.finance.ReceiptVO;
import com.foodtraceability.entity.finance.Receipt;

import java.util.List;

/**
 * 收款单Service接口
 * 管理收款登记的完整流程：四账联动（应收账款+银行账户+资金流水+会计凭证）
 */
public interface ReceiptService extends IService<Receipt> {

    /**
     * 登记收款（四账联动核心方法）
     * 一个事务内完成：
     * 1. 创建收款单记录
     * 2. 更新应收账款余额和状态（含最后收款日期）
     * 3. 创建资金流水（自动联动银行账户余额增加）
     * 4. 生成收款会计凭证（借：银行存款 / 贷：应收账款）
     *
     * @param dto 收款登记DTO
     * @return 收款单VO（含关联的流水ID和凭证ID）
     */
    ReceiptVO registerReceipt(ReceiptCreateDTO dto);

    /**
     * 查询某笔应收账款的所有收款记录
     * @param receivableId 应收账款ID
     * @return 收款记录列表
     */
    List<ReceiptVO> getReceiptHistoryByReceivableId(Long receivableId);

    /**
     * 获取收款单详情
     * @param receiptId 收款单ID
     * @return 收款单VO
     */
    ReceiptVO getDetail(Long receiptId);

    /**
     * 更新收款单（仅更新可编辑字段：金额/方式/银行账户/日期/备注）
     * @param receiptId 收款单ID
     * @param dto       收款登记DTO
     * @return 更新后的收款单VO
     */
    ReceiptVO update(Long receiptId, ReceiptCreateDTO dto);
}
