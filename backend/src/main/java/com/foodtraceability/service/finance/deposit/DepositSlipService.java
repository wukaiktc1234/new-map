package com.foodtraceability.service.finance.deposit;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.dto.finance.deposit.DepositSlipQueryDTO;
import com.foodtraceability.dto.finance.deposit.DepositSlipVO;
import com.foodtraceability.dto.finance.deposit.DepositToBankDTO;
import com.foodtraceability.dto.finance.deposit.GenerateSlipDTO;
import com.foodtraceability.dto.finance.deposit.GenerateSlipResultVO;
import com.foodtraceability.dto.finance.deposit.ReceiveSlipDTO;
import com.foodtraceability.dto.finance.deposit.RejectSlipDTO;
import com.foodtraceability.dto.finance.deposit.ReverseSlipDTO;

/**
 * 现金缴款单服务（P2-A：SL1–SL11）。
 *
 * <p>状态机：DRAFT → SUBMITTED → RECEIVED → DEPOSITED → VERIFIED → CLOSED；
 * SUBMITTED/RECEIVED 可 REJECTED；已落流水（DEPOSITED/VERIFIED/CLOSED）可 REVERSED。</p>
 *
 * <p><b>三段式红线</b>：支付（SALE）不动；RECEIVED 不生成任何流水；只有 DEPOSITED 生成
 * <b>恰好 2 条</b>内部转账（现金户出、银行户入，flow_category=5）。</p>
 */
public interface DepositSlipService {

    /** SL1 按班次生成缴款单（shift_id 唯一，已存在则 reused=true） */
    GenerateSlipResultVO generate(GenerateSlipDTO dto);

    /** SL2 列表 */
    IPage<DepositSlipVO> list(DepositSlipQueryDTO query);

    /** SL3 详情 */
    DepositSlipVO detail(Long slipId);

    /** SL4 提交（CASHIER）：DRAFT → SUBMITTED */
    DepositSlipVO submit(Long slipId);

    /** SL5 收款（TELLER）：SUBMITTED → RECEIVED，只记短款，不生成流水 */
    DepositSlipVO receive(Long slipId, ReceiveSlipDTO dto);

    /** SL6 存行（TELLER）：RECEIVED → DEPOSITED，生成恰好 2 条内部转账 */
    DepositSlipVO deposit(Long slipId, DepositToBankDTO dto);

    /** SL7 复核（FINANCE_DIRECTOR）：DEPOSITED → VERIFIED，校验四眼 */
    DepositSlipVO verify(Long slipId);

    /** SL8 关闭：VERIFIED → CLOSED */
    DepositSlipVO close(Long slipId);

    /** SL9 驳回：SUBMITTED/RECEIVED → REJECTED */
    DepositSlipVO reject(Long slipId, RejectSlipDTO dto);

    /** SL10 冲正（FINANCE_DIRECTOR）：只增反向流水，原流水 status=3 */
    DepositSlipVO reverse(Long slipId, ReverseSlipDTO dto);

    /**
     * N19-2④：人工确认退款→班次归属（幂等 + 覆盖 + 乐观锁），并重算 refund_component_fen / difference_net_fen。
     *
     * @param slipId         缴款单（重算对象）
     * @param refundRecordId 退款单号（唯一键）
     * @param shiftId        归属班次
     * @param reason         确认原因
     */
    DepositSlipVO confirmAttribution(Long slipId, Long refundRecordId, Long shiftId, String reason);

    /** N19-3b：本单「待确认归属」数据源（边界带内且无归属的退款 + 候选班次 + 缺时刻计数） */
    com.foodtraceability.dto.finance.deposit.DepositSlipPendingRefundsVO pendingRefunds(Long slipId);

    /** 差异3：某营业日的存行汇总（含未绑流水的单，unmatchedCount 不得静默为 0） */
    com.foodtraceability.dto.finance.deposit.DepositDailySummaryVO dailySummary(java.time.LocalDate date);

    /** SL11 导出 CSV */
    String exportCsv(DepositSlipQueryDTO query);
}
