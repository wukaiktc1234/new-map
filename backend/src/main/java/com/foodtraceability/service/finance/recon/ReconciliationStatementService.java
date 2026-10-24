package com.foodtraceability.service.finance.recon;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.dto.finance.recon.ReconciliationStatementVO;
import com.foodtraceability.dto.finance.recon.StatementCreateRequest;
import com.foodtraceability.dto.finance.recon.StatementQueryDTO;

/**
 * 银行存款余额调节表服务（S3–S9）。
 *
 * <p><b>硬规则一（design §4.3，不可违反）</b>：本服务的生成 / 重算 / 提交 / 审核动作
 * <b>不产生、不修改任何 fund_flows 流水，也不改 bank_accounts.balance</b>。
 * 未达账项只在 reconciliation_items 里挂账披露，只引用既有事实（flow_id / transaction_id）。</p>
 */
public interface ReconciliationStatementService {

    /** S3 生成/重算 */
    ReconciliationStatementVO generate(StatementCreateRequest req);

    /** S4 列表 */
    IPage<ReconciliationStatementVO> page(StatementQueryDTO query);

    /** S5 详情（含四类未达明细） */
    ReconciliationStatementVO detail(Long statementId);

    /** S6 提交（V5 门禁：is_balanced 必须 true） */
    ReconciliationStatementVO submit(Long statementId);

    /** S7 审核（V8 四眼门禁 + 角色门禁 + 复核 V5） */
    ReconciliationStatementVO approve(Long statementId);

    /** S9 导出 CSV */
    String exportCsv(Long statementId);
}
