package com.foodtraceability.service.finance.recon.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodtraceability.exception.BusinessException;
import com.foodtraceability.dto.finance.recon.ImportBatchVO;
import com.foodtraceability.dto.finance.recon.ImportCommitResultVO;
import com.foodtraceability.dto.finance.recon.ImportRowVO;
import com.foodtraceability.entity.finance.recon.BankTransaction;
import com.foodtraceability.entity.finance.recon.ImportBatch;
import com.foodtraceability.entity.finance.recon.ImportRow;
import com.foodtraceability.mapper.finance.recon.BankTransactionMapper;
import com.foodtraceability.mapper.finance.recon.ImportBatchMapper;
import com.foodtraceability.mapper.finance.recon.ImportRowMapper;
import com.foodtraceability.service.finance.recon.BankImportService;
import com.foodtraceability.service.finance.recon.importing.CsvImportParser;
import com.foodtraceability.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 导入服务实现（P0-B · 两段式）。
 *
 * <p>红线：<b>只写</b> import_batches / import_rows / bank_transactions；
 * <b>绝不写</b> fund_flows，<b>绝不改</b> bank_accounts.balance（TD-14 裁决：否）。</p>
 *
 * <p>金额：全部复用 {@link CsvImportParser} → {@link com.foodtraceability.service.finance.recon.importing.MoneyYuanToFen}，
 * 本类内**没有**任何 ×100 换算。</p>
 */
@Service
public class BankImportServiceImpl implements BankImportService {

    private static final Logger log = LoggerFactory.getLogger(BankImportServiceImpl.class);

    private static final String ST_UPLOADED = "UPLOADED";
    private static final String ST_PARSED_OK = "PARSED_OK";
    private static final String ST_PARSED_FAILED = "PARSED_FAILED";
    private static final String ST_COMMITTED = "COMMITTED";
    private static final String ST_PARTIAL = "PARTIAL";
    private static final String ST_CANCELLED = "CANCELLED";

    private static final DateTimeFormatter BATCH_NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ImportBatchMapper batchMapper;
    private final ImportRowMapper rowMapper;
    private final BankTransactionMapper txnMapper;
    private final CsvImportParser parser;
    private final ObjectMapper objectMapper;

    public BankImportServiceImpl(ImportBatchMapper batchMapper,
                                 ImportRowMapper rowMapper,
                                 BankTransactionMapper txnMapper,
                                 CsvImportParser parser,
                                 ObjectMapper objectMapper) {
        this.batchMapper = batchMapper;
        this.rowMapper = rowMapper;
        this.txnMapper = txnMapper;
        this.parser = parser;
        this.objectMapper = objectMapper;
    }

    // ------------------------------------------------------------------
    // I1：上传 + 预检（不落事实表）
    // ------------------------------------------------------------------
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportBatchVO importCsv(MultipartFile file, String sourceType, Long accountId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("导入文件为空");
        }
        if (accountId == null) {
            throw new BusinessException("必须指定账户 accountId（外部流水按账户对账）");
        }
        CsvImportParser.SourceType st;
        try {
            st = CsvImportParser.SourceType.valueOf(sourceType == null ? "" : sourceType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("sourceType 必须是 WECHAT / ALIPAY / BANK");
        }

        byte[] bytes;
        try (InputStream in = file.getInputStream()) {
            bytes = in.readAllBytes();
        } catch (Exception e) {
            throw new BusinessException("读取上传文件失败: " + e.getMessage());
        }
        String sha = sha256(bytes);

        // 文件级重复仅作提示，**不早退**：每次都建新批次，靠行级 dedupe_basis 判重
        // （AC-3：同一文件第二次导入必须 dup_rows == total_rows，且事实表行数不变）
        ImportBatch sameSha = batchMapper.selectOne(new LambdaQueryWrapper<ImportBatch>()
                .eq(ImportBatch::getAccountId, accountId)
                .eq(ImportBatch::getSourceType, st.name())
                .eq(ImportBatch::getFileSha256, sha)
                .eq(ImportBatch::getDeleted, 0)
                .orderByDesc(ImportBatch::getBatchId)
                .last("LIMIT 1"));
        boolean reusedFile = sameSha != null && !ST_CANCELLED.equals(sameSha.getStatus());

        Long userId = currentUserId();
        String userName = currentUserName();

        ImportBatch batch = new ImportBatch();
        // batch_no 必须唯一（uk_import_batches_batch_no）：秒级时间戳 + 纳秒尾数 + 随机后缀，同秒同文件也不会撞
        String batchNo = "IMP" + LocalDateTime.now().format(BATCH_NO_FMT)
                + "-" + String.format("%03d", LocalDateTime.now().getNano() / 1_000_000)
                + String.format("%03d", (int) (Math.random() * 1000));
        batch.setBatchNo(batchNo);
        batch.setSourceType(st.name());
        batch.setAccountId(accountId);
        batch.setFileName(file.getOriginalFilename() == null ? "upload.csv" : file.getOriginalFilename());
        batch.setFileSha256(sha);
        batch.setTotalRows(0);
        batch.setOkRows(0);
        batch.setDupRows(0);
        batch.setErrorRows(0);
        batch.setStatus(ST_UPLOADED);
        batch.setCreateUserId(userId);
        batch.setCreatedBy(userName);
        batch.setCreateTime(LocalDateTime.now());
        batch.setUpdateTime(LocalDateTime.now());
        batch.setDeleted(0);
        batch.setVersion(0);
        batchMapper.insert(batch);

        // 解析（含编码判定、表头映射、金额换算、方向判定、批内幂等分流）
        Set<String> knownKeys = loadKnownKeys(accountId, st);
        CsvImportParser.BatchResult result = parser.parse(bytes, st, knownKeys);

        batch.setEncodingUsed(result.encodingUsed());
        batch.setTotalRows(result.totalRows());
        batch.setOkRows(result.okRows());
        batch.setDupRows(result.dupRows());
        batch.setErrorRows(result.errorRows());
        batch.setErrorSummary(result.errorSummary());

        if (result.status() == CsvImportParser.BatchStatus.PARSED_FAILED) {
            // AC-5：整批失败 → 不写任何 import_rows / bank_transactions
            batch.setStatus(ST_PARSED_FAILED);
            batch.setUpdateTime(LocalDateTime.now());
            batchMapper.updateById(batch);
            log.warn("导入批次 {} PARSED_FAILED: {}", batch.getBatchNo(), result.errorSummary());
            ImportBatchVO failedVo = toBatchVo(batch);
            failedVo.setReused(reusedFile);
            return failedVo;
        }

        // 行级回执（不含事实表写入）
        LocalDate minDate = null;
        LocalDate maxDate = null;
        for (CsvImportParser.ParsedRow row : result.rows()) {
            ImportRow ir = new ImportRow();
            ir.setBatchId(batch.getBatchId());
            ir.setRowNo(row.rowNo());
            ir.setRawJson(toJson(row));
            ir.setStatus(row.status().name());
            ir.setErrorCode(row.errorCode());
            ir.setErrorMessage(truncate(row.errorMessage(), 255));
            ir.setCreateTime(LocalDateTime.now());
            ir.setUpdateTime(LocalDateTime.now());
            rowMapper.insert(ir);

            if (row.txnTime() != null) {
                LocalDate d = row.txnTime().toLocalDate();
                if (minDate == null || d.isBefore(minDate)) {
                    minDate = d;
                }
                if (maxDate == null || d.isAfter(maxDate)) {
                    maxDate = d;
                }
            }
        }
        batch.setPeriodStart(minDate);
        batch.setPeriodEnd(maxDate);
        batch.setStatus(ST_PARSED_OK);
        batch.setUpdateTime(LocalDateTime.now());
        batchMapper.updateById(batch);
        ImportBatchVO vo = toBatchVo(batch);
        vo.setReused(reusedFile);
        return vo;
    }

    // ------------------------------------------------------------------
    // I2 / I3
    // ------------------------------------------------------------------
    @Override
    public ImportBatchVO getBatch(Long batchId) {
        ImportBatch batch = batchMapper.selectById(batchId);
        if (batch == null || Integer.valueOf(1).equals(batch.getDeleted())) {
            throw new BusinessException("导入批次不存在: " + batchId);
        }
        return toBatchVo(batch);
    }

    @Override
    public IPage<ImportRowVO> listRows(Long batchId, String status, int current, int size) {
        int pageSize = Math.min(Math.max(size, 1), 200);
        LambdaQueryWrapper<ImportRow> w = new LambdaQueryWrapper<ImportRow>()
                .eq(ImportRow::getBatchId, batchId)
                .eq(status != null && !status.isBlank(), ImportRow::getStatus, status)
                .orderByAsc(ImportRow::getRowNo);
        Page<ImportRow> page = rowMapper.selectPage(new Page<>(Math.max(current, 1), pageSize), w);
        IPage<ImportRowVO> out = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<ImportRowVO> records = new ArrayList<>();
        for (ImportRow r : page.getRecords()) {
            ImportRowVO vo = new ImportRowVO();
            vo.setRowId(r.getRowId());
            vo.setRowNo(r.getRowNo());
            vo.setStatus(r.getStatus());
            vo.setErrorCode(r.getErrorCode());
            vo.setErrorMessage(r.getErrorMessage());
            vo.setDuplicateOfRowNo(r.getDuplicateOfRowNo());
            vo.setMatchedTransactionId(r.getMatchedTransactionId());
            vo.setTransactionId(r.getTransactionId());
            vo.setRawPreview(truncate(r.getRawJson(), 500));
            records.add(vo);
        }
        out.setRecords(records);
        return out;
    }

    // ------------------------------------------------------------------
    // I4：提交落库（两段式的第二段）
    // ------------------------------------------------------------------
    /** 提交中标记（并发抢占用；不落库状态语义，只在 commit 内部短暂存在） */
    private static final String ST_COMMITTING = "COMMITTING";

    /**
     * 已提交批次的回执：统计**从 bank_transactions 按批次重算**（不依赖上次的内存/返回值），
     * 保证「重复 commit 返回同一结果」与库内事实一致。
     */
    private ImportCommitResultVO rebuildCommittedResult(ImportBatch batch, boolean reused) {
        Long batchId = batch.getBatchId();
        Long txnCount = txnMapper.selectCount(new LambdaQueryWrapper<BankTransaction>()
                .eq(BankTransaction::getBatchId, batchId)
                .eq(BankTransaction::getDeleted, 0));
        Long reviewCount = txnMapper.selectCount(new LambdaQueryWrapper<BankTransaction>()
                .eq(BankTransaction::getBatchId, batchId)
                .eq(BankTransaction::getDeleted, 0)
                .eq(BankTransaction::getNeedsReview, Boolean.TRUE));
        ImportCommitResultVO vo = new ImportCommitResultVO();
        vo.setBatchId(batchId);
        vo.setStatus(batch.getStatus());
        vo.setTotalRows(batch.getTotalRows());
        vo.setOkRows(batch.getOkRows());
        vo.setInsertedRows(txnCount == null ? 0 : txnCount.intValue());
        vo.setDupRows(batch.getDupRows());
        vo.setReviewRows(reviewCount == null ? 0 : reviewCount.intValue());
        vo.setErrorRows(batch.getErrorRows());
        vo.setReused(reused);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportCommitResultVO commit(Long batchId) {
        ImportBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new BusinessException("导入批次不存在: " + batchId);
        }
        // 幂等（bug-⑤③）：幂等键 = **批次 ID 单键**。
        //   为什么不是「批次 ID + 文件 sha256」：同一批次允许**补导新行**（门店当天补录），
        //   用文件哈希当 key 会把补导当成新批次而重复建批次；而重复 commit 同一批次必须幂等。
        //   统计不存内存/不靠上次返回值，而是从 bank_transactions 按批次**重算**，与库内事实一致。
        if (ST_COMMITTED.equals(batch.getStatus()) || ST_PARTIAL.equals(batch.getStatus())
                || ST_COMMITTING.equals(batch.getStatus())) {
            if (ST_COMMITTING.equals(batch.getStatus())) {
                // 另一请求正在提交中：直接按当前库内事实返回（不阻塞、不重复落库）
                log.info("[METRIC] import_commit_inflight batchId={}", batchId);
            }
            return rebuildCommittedResult(batch, true);
        }
        if (!ST_PARSED_OK.equals(batch.getStatus())) {
            throw new BusinessException("批次状态 " + batch.getStatus() + " 不允许提交（需 PARSED_OK）");
        }

        // 条件更新抢占（CAS）：只有把 PARSED_OK 改成 COMMITTING 的那一个请求才真正落库，
        // 并发第二个请求命中 0 行 → 按已提交结果返回 reused=true（不会双写、不会 500）
        int claimed = batchMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ImportBatch>()
                .eq(ImportBatch::getBatchId, batchId)
                .eq(ImportBatch::getStatus, ST_PARSED_OK)
                .set(ImportBatch::getStatus, ST_COMMITTING)
                .set(ImportBatch::getUpdateTime, LocalDateTime.now()));
        if (claimed != 1) {
            ImportBatch latest = batchMapper.selectById(batchId);
            log.info("[METRIC] import_commit_reused_concurrent batchId={} status={}", batchId,
                    latest == null ? null : latest.getStatus());
            return rebuildCommittedResult(latest == null ? batch : latest, true);
        }
        log.info("[METRIC] import_commit_claimed batchId={}", batchId);

        List<ImportRow> rows = rowMapper.selectList(new LambdaQueryWrapper<ImportRow>()
                .eq(ImportRow::getBatchId, batchId)
                .orderByAsc(ImportRow::getRowNo));

        int inserted = 0, dup = 0, review = 0, error = 0;
        Long userId = currentUserId();

        for (ImportRow row : rows) {
            if ("ERROR".equals(row.getStatus())) {
                error++;
                continue;
            }
            if ("DUPLICATE".equals(row.getStatus())) {
                dup++;
                // 补齐「与库内既有流水」的指向（AC-6 可回溯）
                if (row.getMatchedTransactionId() == null) {
                    Long matched = findExistingTxnId(batch, row);
                    if (matched != null) {
                        row.setMatchedTransactionId(matched);
                        row.setUpdateTime(LocalDateTime.now());
                        rowMapper.updateById(row);
                    }
                }
                continue;
            }

            CsvImportParser.ParsedRow parsed = fromJson(row.getRawJson());
            if (parsed == null) {
                row.setStatus("ERROR");
                row.setErrorCode("RAW_JSON_INVALID");
                row.setErrorMessage("行回执反序列化失败，未落库");
                row.setUpdateTime(LocalDateTime.now());
                rowMapper.updateById(row);
                error++;
                continue;
            }

            BankTransaction txn = new BankTransaction();
            txn.setBatchId(batchId);
            txn.setAccountId(batch.getAccountId());
            txn.setSourceType(batch.getSourceType());
            // 外部流水号加来源前缀（WX:/ALI:/BANK:）——跨来源不互认（design §1.1 TD-13），
            // 也让「同一笔渠道号」在不同渠道间不会互相覆盖；原始值仍在 raw_json 里可查。
            txn.setExternalTxnId(withSourcePrefix(batch.getSourceType(), parsed.externalTxnId()));
            txn.setMerchantOrderNo(parsed.merchantOrderNo());
            txn.setDirection(parsed.direction());
            txn.setIsRefund(Boolean.FALSE);
            txn.setAmount(parsed.amountFen());
            txn.setFee(parsed.feeFen());
            txn.setTxnTime(parsed.txnTime());
            txn.setBusinessDate(parsed.txnTime() == null ? null : parsed.txnTime().toLocalDate());
            txn.setCounterpartyAccount(parsed.counterpartyAccount());
            txn.setSummary(truncate(parsed.summary(), 255));
            txn.setDedupeBasis(parsed.dedupeBasis().name());
            txn.setRawHash(parsed.rawHash());
            // 退化键「待人工确认」行：落库但标 EXCEPTION + needs_review=1（TD-12 分流）
            if ("NEEDS_REVIEW".equals(row.getStatus())) {
                txn.setMatchStatus("EXCEPTION");
                txn.setNeedsReview(Boolean.TRUE);
            } else {
                txn.setMatchStatus("UNMATCHED");
                txn.setNeedsReview(Boolean.FALSE);
            }
            txn.setCreateUserId(userId);
            txn.setCreateTime(LocalDateTime.now());
            txn.setUpdateTime(LocalDateTime.now());
            txn.setDeleted(0);
            txn.setVersion(0);

            try {
                txnMapper.insert(txn);
                row.setTransactionId(txn.getTransactionId());
                row.setUpdateTime(LocalDateTime.now());
                rowMapper.updateById(row);
                inserted++;
                if ("NEEDS_REVIEW".equals(row.getStatus())) {
                    review++;
                }
            } catch (DuplicateKeyException e) {
                // 并发/唯一索引兜底：精确键或退化键撞车 → 判重复，不落库
                Long matched = findExistingTxnId(batch, row);
                row.setStatus("DUPLICATE");
                row.setMatchedTransactionId(matched);
                row.setUpdateTime(LocalDateTime.now());
                rowMapper.updateById(row);
                dup++;
            }
        }

        String finalStatus = (error > 0 || review > 0) ? ST_PARTIAL : ST_COMMITTED;
        batch.setStatus(finalStatus);
        // 恒等式：ok_rows + dup_rows + error_rows = total_rows（见 §5.1 表注释）
        batch.setOkRows(Math.max(batch.getTotalRows() - dup - error, 0));
        batch.setDupRows(dup);
        batch.setErrorRows(error);
        batch.setUpdateTime(LocalDateTime.now());
        batchMapper.updateById(batch);

        ImportCommitResultVO vo = new ImportCommitResultVO();
        vo.setBatchId(batchId);
        vo.setStatus(finalStatus);
        vo.setTotalRows(batch.getTotalRows());
        vo.setOkRows(batch.getOkRows());
        vo.setInsertedRows(inserted);
        vo.setDupRows(dup);
        vo.setReviewRows(review);
        vo.setErrorRows(error);
        vo.setReused(Boolean.FALSE);
        return vo;
    }

    // ------------------------------------------------------------------
    // I5：撤销
    // ------------------------------------------------------------------
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportBatchVO cancel(Long batchId, String reason) {
        ImportBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new BusinessException("导入批次不存在: " + batchId);
        }
        if (ST_COMMITTED.equals(batch.getStatus()) || ST_PARTIAL.equals(batch.getStatus())) {
            throw new BusinessException("已落库批次不可撤销（AC：批次不可变）；如需纠正请重新导入新批次");
        }
        batch.setStatus(ST_CANCELLED);
        batch.setErrorSummary(truncate(reason, 500));
        batch.setUpdateTime(LocalDateTime.now());
        batchMapper.updateById(batch);
        return toBatchVo(batch);
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    /** 库内既有键（精确键 external_txn_id + 退化键 时间/金额/对方账号），用于「与库内去重」 */
    private Set<String> loadKnownKeys(Long accountId, CsvImportParser.SourceType st) {
        Set<String> keys = new HashSet<>();
        List<BankTransaction> txs = txnMapper.selectList(new LambdaQueryWrapper<BankTransaction>()
                .eq(BankTransaction::getAccountId, accountId)
                .eq(BankTransaction::getSourceType, st.name())
                .eq(BankTransaction::getDeleted, 0)
                .select(BankTransaction::getExternalTxnId, BankTransaction::getTxnTime,
                        BankTransaction::getAmount, BankTransaction::getCounterpartyAccount));
        for (BankTransaction t : txs) {
            if (t.getExternalTxnId() != null && !t.getExternalTxnId().isBlank()) {
                keys.add(st.name() + "|" + stripSourcePrefix(t.getExternalTxnId()));
            }
            keys.add(st.name() + "|" + t.getTxnTime() + "|" + t.getAmount() + "|"
                    + (t.getCounterpartyAccount() == null ? "" : t.getCounterpartyAccount()));
        }
        return keys;
    }

    private Long findExistingTxnId(ImportBatch batch, ImportRow row) {
        CsvImportParser.ParsedRow parsed = fromJson(row.getRawJson());
        if (parsed == null) {
            return null;
        }
        if (parsed.externalTxnId() != null && !parsed.externalTxnId().isBlank()) {
            BankTransaction t = txnMapper.selectOne(new LambdaQueryWrapper<BankTransaction>()
                    .eq(BankTransaction::getAccountId, batch.getAccountId())
                    .eq(BankTransaction::getSourceType, batch.getSourceType())
                    .eq(BankTransaction::getExternalTxnId, withSourcePrefix(batch.getSourceType(), parsed.externalTxnId()))
                    .eq(BankTransaction::getDeleted, 0)
                    .last("LIMIT 1"));
            if (t != null) {
                return t.getTransactionId();
            }
        }
        BankTransaction t = txnMapper.selectOne(new LambdaQueryWrapper<BankTransaction>()
                .eq(BankTransaction::getAccountId, batch.getAccountId())
                .eq(BankTransaction::getTxnTime, parsed.txnTime())
                .eq(BankTransaction::getAmount, parsed.amountFen())
                .eq(parsed.counterpartyAccount() != null, BankTransaction::getCounterpartyAccount, parsed.counterpartyAccount())
                .eq(BankTransaction::getDeleted, 0)
                .last("LIMIT 1"));
        return t == null ? null : t.getTransactionId();
    }

    private ImportBatchVO toBatchVo(ImportBatch b) {
        ImportBatchVO vo = new ImportBatchVO();
        vo.setBatchId(b.getBatchId());
        vo.setBatchNo(b.getBatchNo());
        vo.setSourceType(b.getSourceType());
        vo.setAccountId(b.getAccountId());
        vo.setFileName(b.getFileName());
        vo.setFileSha256(b.getFileSha256());
        vo.setEncodingUsed(b.getEncodingUsed());
        vo.setPeriodStart(b.getPeriodStart());
        vo.setPeriodEnd(b.getPeriodEnd());
        vo.setTotalRows(b.getTotalRows());
        vo.setOkRows(b.getOkRows());
        vo.setDupRows(b.getDupRows());
        vo.setErrorRows(b.getErrorRows());
        vo.setStatus(b.getStatus());
        vo.setErrorSummary(b.getErrorSummary());
        vo.setCreateUserId(b.getCreateUserId());
        vo.setCreateTime(b.getCreateTime());
        vo.setReused(false);
        return vo;
    }

    /** 行回执的 raw_json（脱敏后的结构化原文，JSONB） */
    private String toJson(CsvImportParser.ParsedRow row) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("rowNo", row.rowNo());
        m.put("externalTxnId", row.externalTxnId());
        m.put("merchantOrderNo", row.merchantOrderNo());
        m.put("txnTime", row.txnTime() == null ? null : row.txnTime().toString());
        m.put("amountFen", row.amountFen());
        m.put("feeFen", row.feeFen());
        m.put("direction", row.direction());
        m.put("summary", row.summary());
        m.put("counterpartyAccount", row.counterpartyAccount());
        m.put("dedupeBasis", row.dedupeBasis() == null ? null : row.dedupeBasis().name());
        m.put("status", row.status().name());
        m.put("errorCode", row.errorCode());
        m.put("rawHash", row.rawHash());
        try {
            return objectMapper.writeValueAsString(m);
        } catch (Exception e) {
            return "{}";
        }
    }

    private CsvImportParser.ParsedRow fromJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> m = objectMapper.readValue(json, Map.class);
            Integer rowNo = toInt(m.get("rowNo"));
            String ext = (String) m.get("externalTxnId");
            String merchant = (String) m.get("merchantOrderNo");
            Object t = m.get("txnTime");
            LocalDateTime txnTime = t == null ? null : LocalDateTime.parse(String.valueOf(t));
            Long amount = toLong(m.get("amountFen"));
            Long fee = toLong(m.get("feeFen"));
            Integer dir = toInt(m.get("direction"));
            String summary = (String) m.get("summary");
            String cp = (String) m.get("counterpartyAccount");
            String basis = (String) m.get("dedupeBasis");
            String status = (String) m.get("status");
            String errCode = (String) m.get("errorCode");
            String rawHash = (String) m.get("rawHash");
            return new CsvImportParser.ParsedRow(
                    rowNo == null ? 0 : rowNo, ext, merchant, txnTime, amount, fee, dir, summary, cp,
                    basis == null ? CsvImportParser.DedupeBasis.COMPOSITE : CsvImportParser.DedupeBasis.valueOf(basis),
                    status == null ? CsvImportParser.RowStatus.OK : CsvImportParser.RowStatus.valueOf(status),
                    errCode, null, rawHash);
        } catch (Exception e) {
            log.warn("raw_json 反序列化失败: {}", e.getMessage());
            return null;
        }
    }

    /** JSONB 回读容错：数字可能以字符串形式返回（PGobject → text） */
    private static Integer toInt(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : Integer.valueOf(s);
    }

    private static Long toLong(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : Long.valueOf(s);
    }

    private Long currentUserId() {
        try {
            Long id = SecurityUtils.getCurrentUserId();
            return id == null ? 0L : id;
        } catch (Exception e) {
            return 0L;
        }
    }

    private String currentUserName() {
        try {
            String n = SecurityUtils.getCurrentUsername();
            return n == null || n.isBlank() ? "system" : n;
        } catch (Exception e) {
            return "system";
        }
    }

    /** 来源前缀：WECHAT→WX、ALIPAY→ALI、BANK→BANK（跨来源不互认，design §1.1 / TD-13） */
    static String withSourcePrefix(String sourceType, String externalTxnId) {
        if (externalTxnId == null || externalTxnId.isBlank()) {
            return externalTxnId;
        }
        String prefix = switch (sourceType == null ? "" : sourceType) {
            case "WECHAT" -> "WX:";
            case "ALIPAY" -> "ALI:";
            case "BANK" -> "BANK:";
            default -> "";
        };
        return externalTxnId.startsWith(prefix) || externalTxnId.contains(":") ? externalTxnId : prefix + externalTxnId;
    }

    static String stripSourcePrefix(String externalTxnId) {
        if (externalTxnId == null) {
            return null;
        }
        int i = externalTxnId.indexOf(':');
        return i < 0 ? externalTxnId : externalTxnId.substring(i + 1);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String sha256(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : d) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
