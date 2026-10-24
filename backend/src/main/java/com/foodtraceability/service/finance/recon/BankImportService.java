package com.foodtraceability.service.finance.recon;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.dto.finance.recon.BankTransactionQueryDTO;
import com.foodtraceability.dto.finance.recon.BankTransactionVO;
import com.foodtraceability.dto.finance.recon.ImportBatchVO;
import com.foodtraceability.dto.finance.recon.ImportCommitResultVO;
import com.foodtraceability.dto.finance.recon.ImportRowVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 银行/渠道流水导入服务（P0-B · I1–I5）。
 *
 * <p><b>P0 范围红线</b>：只写 import_batches / import_rows / bank_transactions 三张导入表；
 * <b>不写</b> fund_flows、<b>不碰</b> bank_accounts.balance（TD-14 已裁决否）。</p>
 */
public interface BankImportService {

    /** I1：上传 + 预检（**不落事实表**） */
    ImportBatchVO importCsv(MultipartFile file, String sourceType, Long accountId);

    /** I2：批次详情 */
    ImportBatchVO getBatch(Long batchId);

    /** I3：行级回执分页 */
    IPage<ImportRowVO> listRows(Long batchId, String status, int current, int size);

    /** I4：确认落库（两段式的第二段） */
    ImportCommitResultVO commit(Long batchId);

    /** I5：撤销未提交批次 */
    ImportBatchVO cancel(Long batchId, String reason);
}
