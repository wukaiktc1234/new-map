package com.foodtraceability.service.finance.impl;

import com.foodtraceability.dto.finance.FinanceVoucherCreateDTO;
import com.foodtraceability.dto.finance.FinanceVoucherVO;
import com.foodtraceability.entity.finance.AccountingSubject;
import com.foodtraceability.entity.finance.FinanceVoucher;
import com.foodtraceability.event.InvoiceReimbursementApprovedEvent;
import com.foodtraceability.event.StoreDailySettlementCompletedEvent;
import com.foodtraceability.exception.BusinessException;
import com.foodtraceability.mapper.finance.FinanceVoucherMapper;
import com.foodtraceability.service.finance.AccountingSubjectService;
import com.foodtraceability.service.finance.AutoVoucherService;
import com.foodtraceability.service.finance.VoucherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自动凭证生成Service实现类
 * 根据业务单据自动生成记账凭证，保证幂等性
 *
 * 重构说明：
 * - 已移除 Caffeine 缓存依赖（pom 已移除 Caffeine）
 * - 科目缓存改为内存 ConcurrentHashMap，带 TTL 机制
 * - 保留所有业务逻辑和数据库操作
 */
@Service
public class AutoVoucherServiceImpl implements AutoVoucherService {

    private static final Logger log = LoggerFactory.getLogger(AutoVoucherServiceImpl.class);

    /** 科目缓存键前缀：accounting_subject:code:{subjectCode} */
    private static final String CACHE_KEY_PREFIX = "accounting_subject:code:";

    /** 缓存过期时间（毫秒），1 小时 */
    private static final long CACHE_EXPIRE_MS = 60 * 60 * 1000L;

    /**
     * 缓存条目，存储值和过期时间
     */
    private static final class CacheEntry {
        final Long value;
        final long expireAt;

        CacheEntry(Long value) {
            this.value = value;
            this.expireAt = System.currentTimeMillis() + CACHE_EXPIRE_MS;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }
    }

    private final VoucherService voucherService;
    private final FinanceVoucherMapper voucherMapper;
    private final AccountingSubjectService accountingSubjectService;

    /** 本地缓存：科目编码 → 科目ID（过期1小时） */
    private final ConcurrentHashMap<String, CacheEntry> subjectCodeCache = new ConcurrentHashMap<>();

    public AutoVoucherServiceImpl(VoucherService voucherService,
                                  FinanceVoucherMapper voucherMapper,
                                  AccountingSubjectService accountingSubjectService) {
        this.voucherService = voucherService;
        this.voucherMapper = voucherMapper;
        this.accountingSubjectService = accountingSubjectService;
    }

    /** 来源类型常量 */
    private static final int SOURCE_TYPE_PURCHASE = 2;              // 采购入库
    private static final int SOURCE_TYPE_SALES = 3;                 // 销售出库
    private static final int SOURCE_TYPE_COST = 7;                  // 成本结转
    private static final int SOURCE_TYPE_DEPRECIATION = 8;          // 折旧
    private static final int SOURCE_TYPE_STORE_SETTLEMENT = 9;      // 门店日结（T-037）
    private static final int SOURCE_TYPE_REIMBURSEMENT = 10;        // 发票报销（T-043）
    private static final int SOURCE_TYPE_SALARY = 11;               // 工资（T-045：原值7与COST冲突，改为11）

    /** 凭证类型常量 */
    private static final int VOUCHER_TYPE_PURCHASE = 2;             // 采购入库
    private static final int VOUCHER_TYPE_SALES = 3;                // 销售出库
    private static final int VOUCHER_TYPE_COST = 4;                 // 费用
    private static final int VOUCHER_TYPE_STORE_SETTLEMENT = 5;     // 门店日结（T-037）
    private static final int VOUCHER_TYPE_REIMBURSEMENT = 6;        // 报销（T-043）
    private static final int VOUCHER_TYPE_SALARY = 7;               // 工资（T-045）

    /** 报销类型 → 科目编码映射（T-043） */
    private static final String REIMBURSEMENT_SUBJECT_TRAVEL = "550201";       // 差旅费
    private static final String REIMBURSEMENT_SUBJECT_ENTERTAINMENT = "550202"; // 招待费
    private static final String REIMBURSEMENT_SUBJECT_OFFICE = "550203";        // 办公费
    private static final String REIMBURSEMENT_SUBJECT_TRANSPORT = "550204";     // 交通费
    private static final String REIMBURSEMENT_SUBJECT_COMMUNICATION = "550205"; // 通讯费
    private static final String REIMBURSEMENT_SUBJECT_OTHER = "550299";         // 其他

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceVoucherVO generatePurchaseVoucher(Long purchaseOrderId, Long amount, Long supplierId) {
        log.info("生成采购入库凭证，采购单ID：{}，金额：{}", purchaseOrderId, amount);

        // 幂等检查：同一采购单只能生成一张凭证
        if (existsBySource(SOURCE_TYPE_PURCHASE, purchaseOrderId)) {
            throw new BusinessException("该采购单已生成凭证，不能重复生成");
        }

        FinanceVoucherCreateDTO dto = new FinanceVoucherCreateDTO();
        dto.setVoucherDate(LocalDate.now());
        dto.setVoucherType(VOUCHER_TYPE_PURCHASE);
        dto.setSourceType(SOURCE_TYPE_PURCHASE);
        dto.setSourceId(purchaseOrderId);
        dto.setRemark("采购入库自动生成凭证");

        List<FinanceVoucherCreateDTO.VoucherDetailItem> details = new ArrayList<>();

        // 借:库存商品 (1405)
        FinanceVoucherCreateDTO.VoucherDetailItem debitItem = new FinanceVoucherCreateDTO.VoucherDetailItem();
        debitItem.setSummary("采购入库-库存商品");
        debitItem.setSubjectId(getSubjectIdByCode("1405"));
        debitItem.setDebitAmount(amount);
        debitItem.setCreditAmount(0L);
        details.add(debitItem);

        // 贷:应付账款 (2202)
        FinanceVoucherCreateDTO.VoucherDetailItem creditItem = new FinanceVoucherCreateDTO.VoucherDetailItem();
        creditItem.setSummary("采购入库-应付账款");
        creditItem.setSubjectId(getSubjectIdByCode("2202"));
        creditItem.setDebitAmount(0L);
        creditItem.setCreditAmount(amount);
        details.add(creditItem);

        dto.setDetails(details);

        return voucherService.create(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceVoucherVO generateSalesVoucher(Long salesOrderId, Long amount, Long taxAmount, Integer paymentMethod) {
        log.info("生成销售出库凭证，销售单ID：{}，金额：{}", salesOrderId, amount);

        // 幂等检查
        if (existsBySource(SOURCE_TYPE_SALES, salesOrderId)) {
            throw new BusinessException("该销售单已生成凭证，不能重复生成");
        }

        FinanceVoucherCreateDTO dto = new FinanceVoucherCreateDTO();
        dto.setVoucherDate(LocalDate.now());
        dto.setVoucherType(VOUCHER_TYPE_SALES);
        dto.setSourceType(SOURCE_TYPE_SALES);
        dto.setSourceId(salesOrderId);
        dto.setRemark("销售出库自动生成凭证");

        List<FinanceVoucherCreateDTO.VoucherDetailItem> details = new ArrayList<>();
        long totalAmount = amount + (taxAmount != null ? taxAmount : 0L);

        // 根据支付方式确定借方科目
        String debitSubjectCode;
        String debitSummary;
        if (paymentMethod != null && paymentMethod == 1) {
            debitSubjectCode = "1001"; // 库存现金
            debitSummary = "销售收款-现金";
        } else if (paymentMethod != null && paymentMethod == 2) {
            debitSubjectCode = "1002"; // 银行存款
            debitSummary = "销售收款-银行";
        } else {
            debitSubjectCode = "1122"; // 应收账款
            debitSummary = "销售挂账-应收";
        }

        // 借:银行/现金/应收
        FinanceVoucherCreateDTO.VoucherDetailItem debitItem = new FinanceVoucherCreateDTO.VoucherDetailItem();
        debitItem.setSummary(debitSummary);
        debitItem.setSubjectId(getSubjectIdByCode(debitSubjectCode));
        debitItem.setDebitAmount(totalAmount);
        debitItem.setCreditAmount(0L);
        details.add(debitItem);

        // 贷:主营业务收入 (5001)
        FinanceVoucherCreateDTO.VoucherDetailItem revenueItem = new FinanceVoucherCreateDTO.VoucherDetailItem();
        revenueItem.setSummary("销售收入");
        revenueItem.setSubjectId(getSubjectIdByCode("5001"));
        revenueItem.setDebitAmount(0L);
        revenueItem.setCreditAmount(amount);
        details.add(revenueItem);

        // 贷:销项税额 (22210102)
        if (taxAmount != null && taxAmount > 0) {
            FinanceVoucherCreateDTO.VoucherDetailItem taxItem = new FinanceVoucherCreateDTO.VoucherDetailItem();
            taxItem.setSummary("销项税额");
            taxItem.setSubjectId(getSubjectIdByCode("22210102"));
            taxItem.setDebitAmount(0L);
            taxItem.setCreditAmount(taxAmount);
            details.add(taxItem);
        }

        dto.setDetails(details);

        return voucherService.create(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceVoucherVO generateCostTransferVoucher(String period, Long amount) {
        log.info("生成成本结转凭证，期间：{}，金额：{}", period, amount);

        FinanceVoucherCreateDTO dto = new FinanceVoucherCreateDTO();
        dto.setVoucherDate(LocalDate.now());
        dto.setVoucherType(VOUCHER_TYPE_COST);
        dto.setSourceType(SOURCE_TYPE_COST);
        dto.setRemark("成本结转自动生成凭证-" + period);

        List<FinanceVoucherCreateDTO.VoucherDetailItem> details = new ArrayList<>();

        // 借:主营业务成本 (4011)
        FinanceVoucherCreateDTO.VoucherDetailItem debitItem = new FinanceVoucherCreateDTO.VoucherDetailItem();
        debitItem.setSummary("结转主营业务成本");
        debitItem.setSubjectId(getSubjectIdByCode("4011"));
        debitItem.setDebitAmount(amount);
        debitItem.setCreditAmount(0L);
        details.add(debitItem);

        // 贷:库存商品 (1405)
        FinanceVoucherCreateDTO.VoucherDetailItem creditItem = new FinanceVoucherCreateDTO.VoucherDetailItem();
        creditItem.setSummary("结转库存商品");
        creditItem.setSubjectId(getSubjectIdByCode("1405"));
        creditItem.setDebitAmount(0L);
        creditItem.setCreditAmount(amount);
        details.add(creditItem);

        dto.setDetails(details);

        return voucherService.create(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceVoucherVO generateSalaryVoucher(Long salaryRecordId, Long amount) {
        log.info("生成工资计提凭证，工资记录ID：{}，金额：{}", salaryRecordId, amount);

        // 幂等检查：同一工资记录只能生成一张凭证
        if (existsBySource(SOURCE_TYPE_SALARY, salaryRecordId)) {
            log.warn("工资记录 {} 已生成凭证，跳过", salaryRecordId);
            return null;
        }

        FinanceVoucherCreateDTO dto = new FinanceVoucherCreateDTO();
        dto.setVoucherDate(LocalDate.now());
        dto.setVoucherType(VOUCHER_TYPE_SALARY);
        dto.setSourceType(SOURCE_TYPE_SALARY);
        dto.setSourceId(salaryRecordId);
        dto.setRemark("工资计提自动生成凭证");

        List<FinanceVoucherCreateDTO.VoucherDetailItem> details = new ArrayList<>();

        // 借:管理费用-工资 (550206)
        FinanceVoucherCreateDTO.VoucherDetailItem debitItem = new FinanceVoucherCreateDTO.VoucherDetailItem();
        debitItem.setSummary("计提管理人员工资");
        debitItem.setSubjectId(getSubjectIdByCode("550206"));
        debitItem.setDebitAmount(amount);
        debitItem.setCreditAmount(0L);
        details.add(debitItem);

        // 贷:应付职工薪酬 (2211)
        FinanceVoucherCreateDTO.VoucherDetailItem creditItem = new FinanceVoucherCreateDTO.VoucherDetailItem();
        creditItem.setSummary("计提应付职工薪酬");
        creditItem.setSubjectId(getSubjectIdByCode("2211"));
        creditItem.setDebitAmount(0L);
        creditItem.setCreditAmount(amount);
        details.add(creditItem);

        dto.setDetails(details);

        return voucherService.create(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceVoucherVO generateDepreciationVoucher(String period, Long amount) {
        log.info("生成折旧计提凭证，期间：{}，金额：{}", period, amount);

        FinanceVoucherCreateDTO dto = new FinanceVoucherCreateDTO();
        dto.setVoucherDate(LocalDate.now());
        dto.setVoucherType(VOUCHER_TYPE_COST);
        dto.setSourceType(SOURCE_TYPE_DEPRECIATION);
        dto.setRemark("折旧计提自动生成凭证-" + period);

        List<FinanceVoucherCreateDTO.VoucherDetailItem> details = new ArrayList<>();

        // 借:管理费用-折旧 (550208)
        FinanceVoucherCreateDTO.VoucherDetailItem debitItem = new FinanceVoucherCreateDTO.VoucherDetailItem();
        debitItem.setSummary("计提固定资产折旧");
        debitItem.setSubjectId(getSubjectIdByCode("550208"));
        debitItem.setDebitAmount(amount);
        debitItem.setCreditAmount(0L);
        details.add(debitItem);

        // 贷:累计折旧 (1602)
        FinanceVoucherCreateDTO.VoucherDetailItem creditItem = new FinanceVoucherCreateDTO.VoucherDetailItem();
        creditItem.setSummary("累计折旧");
        creditItem.setSubjectId(getSubjectIdByCode("1602"));
        creditItem.setDebitAmount(0L);
        creditItem.setCreditAmount(amount);
        details.add(creditItem);

        dto.setDetails(details);

        return voucherService.create(dto);
    }

    /**
     * 门店日结完成自动生成凭证（F-008 联动）
     *
     * <p>Sprint 3.1 P0 T-037：实现门店日结凭证生成。</p>
     *
     * <p>凭证规则（E03）：
     * 凭证类型 VOUCHER_TYPE_STORE_SETTLEMENT（常量值 5）；
     * 来源类型 SOURCE_TYPE_STORE_SETTLEMENT（常量值 9）；
     * 借：银行存款（1002），金额 = totalIncome；
     * 贷：主营业务收入（5001），金额 = totalIncome；
     * 借：主营业务成本（4011），金额 = totalExpense；
     * 贷：库存商品（1405），金额 = totalExpense；
     * 幂等检查 existsBySource(SOURCE_TYPE_STORE_SETTLEMENT, settlementId)，已存在则跳过返回 null。</p>
     *
     * @param event 门店日结完成事件
     * @return 凭证VO；幂等检查命中时返回 null
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceVoucherVO generateStoreSettlementVoucher(StoreDailySettlementCompletedEvent event) {
        log.info("生成门店日结凭证，日结单ID：{}，门店：{}，收入：{}，支出：{}",
                event.getSettlementId(), event.getStoreName(),
                event.getTotalIncome(), event.getTotalExpense());

        // 1. 幂等检查：同一日结单只能生成一张凭证
        if (existsBySource(SOURCE_TYPE_STORE_SETTLEMENT, event.getSettlementId())) {
            log.warn("日结单 {} 已生成凭证，跳过", event.getSettlementId());
            return null;
        }

        // 2. 参数校验
        Long totalIncome = event.getTotalIncome() != null ? event.getTotalIncome() : 0L;
        Long totalExpense = event.getTotalExpense() != null ? event.getTotalExpense() : 0L;
        if (totalIncome == 0L && totalExpense == 0L) {
            log.warn("日结单 {} 收入和支出均为0，跳过凭证生成", event.getSettlementId());
            return null;
        }

        // 3. 构造凭证DTO
        FinanceVoucherCreateDTO dto = new FinanceVoucherCreateDTO();
        dto.setVoucherDate(event.getSettlementDate() != null ? event.getSettlementDate() : LocalDate.now());
        dto.setVoucherType(VOUCHER_TYPE_STORE_SETTLEMENT);
        dto.setSourceType(SOURCE_TYPE_STORE_SETTLEMENT);
        dto.setSourceId(event.getSettlementId());
        dto.setRemark("门店日结自动生成凭证-" + event.getStoreName()
                + "-" + (event.getSettlementDate() != null ? event.getSettlementDate() : LocalDate.now()));

        List<FinanceVoucherCreateDTO.VoucherDetailItem> details = new ArrayList<>();

        // 4. 收入分录：借银行存款，贷主营业务收入
        if (totalIncome > 0L) {
            // 借：银行存款 (1002)
            FinanceVoucherCreateDTO.VoucherDetailItem debitItem = new FinanceVoucherCreateDTO.VoucherDetailItem();
            debitItem.setSummary("门店日结收入-" + event.getStoreName());
            debitItem.setSubjectId(getSubjectIdByCode("1002"));
            debitItem.setDebitAmount(totalIncome);
            debitItem.setCreditAmount(0L);
            details.add(debitItem);

            // 贷：主营业务收入 (5001)
            FinanceVoucherCreateDTO.VoucherDetailItem revenueItem = new FinanceVoucherCreateDTO.VoucherDetailItem();
            revenueItem.setSummary("确认主营业务收入");
            revenueItem.setSubjectId(getSubjectIdByCode("5001"));
            revenueItem.setDebitAmount(0L);
            revenueItem.setCreditAmount(totalIncome);
            details.add(revenueItem);
        }

        // 5. 成本分录：借主营业务成本，贷库存商品
        if (totalExpense > 0L) {
            // 借：主营业务成本 (4011)
            FinanceVoucherCreateDTO.VoucherDetailItem costDebitItem = new FinanceVoucherCreateDTO.VoucherDetailItem();
            costDebitItem.setSummary("结转门店日结成本-" + event.getStoreName());
            costDebitItem.setSubjectId(getSubjectIdByCode("4011"));
            costDebitItem.setDebitAmount(totalExpense);
            costDebitItem.setCreditAmount(0L);
            details.add(costDebitItem);

            // 贷：库存商品 (1405)
            FinanceVoucherCreateDTO.VoucherDetailItem inventoryItem = new FinanceVoucherCreateDTO.VoucherDetailItem();
            inventoryItem.setSummary("结转库存商品成本");
            inventoryItem.setSubjectId(getSubjectIdByCode("1405"));
            inventoryItem.setDebitAmount(0L);
            inventoryItem.setCreditAmount(totalExpense);
            details.add(inventoryItem);
        }

        dto.setDetails(details);

        return voucherService.create(dto);
    }

    /**
     * 发票报销审批通过自动生成凭证（F-011 联动）
     *
     * <p>Sprint 3.1 P0 T-043：实现报销凭证生成。</p>
     *
     * <p>凭证规则：
     * 凭证类型 VOUCHER_TYPE_REIMBURSEMENT（常量值 6）；
     * 来源类型 SOURCE_TYPE_REIMBURSEMENT（常量值 10）；
     * 借：管理费用-报销（5502XX，按报销类型映射）；
     * 贷：其他应付款（2241，未付款时默认）；
     * 幂等检查 existsBySource(SOURCE_TYPE_REIMBURSEMENT, reimbursementId)，已存在则跳过返回 null。</p>
     *
     * <p>报销类型 → 科目编码映射：
     * 差旅费→550201 / 招待费→550202 / 办公费→550203 / 交通费→550204 / 通讯费→550205 / 其他→550299。</p>
     *
     * @param event 发票报销审批通过事件
     * @return 凭证VO；幂等检查命中时返回 null
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceVoucherVO generateReimbursementVoucher(InvoiceReimbursementApprovedEvent event) {
        log.info("生成报销凭证，报销单ID：{}，单号：{}，类型：{}，金额：{}",
                event.getReimbursementId(), event.getReimbursementNo(),
                event.getReimbursementType(), event.getApprovedAmount());

        // 1. 幂等检查：同一报销单只能生成一张凭证
        if (existsBySource(SOURCE_TYPE_REIMBURSEMENT, event.getReimbursementId())) {
            log.warn("报销单 {} 已生成凭证，跳过", event.getReimbursementId());
            return null;
        }

        // 2. 参数校验
        Long approvedAmount = event.getApprovedAmount() != null ? event.getApprovedAmount() : 0L;
        if (approvedAmount <= 0L) {
            log.warn("报销单 {} 审批金额为0或负数，跳过凭证生成", event.getReimbursementId());
            return null;
        }

        // 3. 构造凭证DTO
        FinanceVoucherCreateDTO dto = new FinanceVoucherCreateDTO();
        dto.setVoucherDate(event.getApproveTime() != null
                ? event.getApproveTime().toLocalDate()
                : LocalDate.now());
        dto.setVoucherType(VOUCHER_TYPE_REIMBURSEMENT);
        dto.setSourceType(SOURCE_TYPE_REIMBURSEMENT);
        dto.setSourceId(event.getReimbursementId());
        dto.setRemark("报销审批自动生成凭证-" + event.getReimbursementNo()
                + "-" + event.getReimbursementType());

        List<FinanceVoucherCreateDTO.VoucherDetailItem> details = new ArrayList<>();

        // 4. 借：管理费用-报销（按报销类型映射科目编码）
        String expenseSubjectCode = mapReimbursementTypeToSubjectCode(event.getReimbursementType());
        FinanceVoucherCreateDTO.VoucherDetailItem debitItem = new FinanceVoucherCreateDTO.VoucherDetailItem();
        debitItem.setSummary("报销-" + event.getReimbursementType() + "-" + event.getReimbursementNo());
        debitItem.setSubjectId(getSubjectIdByCode(expenseSubjectCode));
        debitItem.setDebitAmount(approvedAmount);
        debitItem.setCreditAmount(0L);
        details.add(debitItem);

        // 5. 贷：其他应付款 (2241)，报销单默认未付款状态
        FinanceVoucherCreateDTO.VoucherDetailItem creditItem = new FinanceVoucherCreateDTO.VoucherDetailItem();
        creditItem.setSummary("应付报销款-" + event.getReimbursementNo());
        creditItem.setSubjectId(getSubjectIdByCode("2241"));
        creditItem.setDebitAmount(0L);
        creditItem.setCreditAmount(approvedAmount);
        details.add(creditItem);

        dto.setDetails(details);

        return voucherService.create(dto);
    }

    /**
     * 报销类型 → 管理费用科目编码映射（T-043）
     *
     * @param reimbursementType 报销类型（差旅费/招待费/办公费/交通费/通讯费/其他）
     * @return 对应的科目编码；未知类型默认返回 550299（其他）
     */
    private String mapReimbursementTypeToSubjectCode(String reimbursementType) {
        if (reimbursementType == null) {
            return REIMBURSEMENT_SUBJECT_OTHER;
        }
        return switch (reimbursementType) {
            case "差旅费" -> REIMBURSEMENT_SUBJECT_TRAVEL;
            case "招待费" -> REIMBURSEMENT_SUBJECT_ENTERTAINMENT;
            case "办公费" -> REIMBURSEMENT_SUBJECT_OFFICE;
            case "交通费" -> REIMBURSEMENT_SUBJECT_TRANSPORT;
            case "通讯费" -> REIMBURSEMENT_SUBJECT_COMMUNICATION;
            default -> REIMBURSEMENT_SUBJECT_OTHER;
        };
    }

    @Override
    public boolean existsBySource(Integer sourceType, Long sourceId) {
        FinanceVoucher existing = voucherMapper.selectBySource(sourceType, sourceId);
        return existing != null;
    }

    /**
     * 根据科目编码获取科目ID（带内存本地缓存）
     *
     * <p>Sprint 3.1 P0 T-019：移除 TODO，调用 AccountingSubjectService 真实查询，
     * 科目不存在时抛 BusinessException。缓存键 accounting_subject:code:{subjectCode}。
     * protected 访问便于同包测试（参考 InvoiceReimbursementServiceImpl 模式）。</p>
     *
     * @param subjectCode 科目编码（不能为空）
     * @return 科目ID
     * @throws IllegalArgumentException 当 subjectCode 为 null 或空白
     * @throws BusinessException 当科目不存在时
     */
    protected Long getSubjectIdByCode(String subjectCode) {
        if (subjectCode == null || subjectCode.isBlank()) {
            throw new IllegalArgumentException("科目编码不能为空");
        }
        String cacheKey = buildSubjectCacheKey(subjectCode);
        // 手动查缓存（替代 Caffeine 的 cache.get(key, mappingFunction) API）
        CacheEntry entry = subjectCodeCache.get(cacheKey);
        if (entry != null && !entry.isExpired()) {
            return entry.value;
        }
        if (entry != null) {
            subjectCodeCache.remove(cacheKey, entry);
        }
        Long subjectId = loadSubjectIdFromDb(subjectCode);
        subjectCodeCache.put(cacheKey, new CacheEntry(subjectId));
        return subjectId;
    }

    /**
     * 构建科目缓存键
     * @param subjectCode 科目编码
     * @return 缓存键，格式 accounting_subject:code:{subjectCode}
     */
    private String buildSubjectCacheKey(String subjectCode) {
        return CACHE_KEY_PREFIX + subjectCode;
    }

    /**
     * 从数据库加载科目ID（缓存未命中时调用）
     * @param subjectCode 科目编码
     * @return 科目ID
     * @throws BusinessException 当科目不存在或科目ID为空时
     */
    private Long loadSubjectIdFromDb(String subjectCode) {
        log.debug("缓存未命中，从数据库查询科目编码：{}", subjectCode);
        AccountingSubject subject = accountingSubjectService.getByCode(subjectCode);
        if (subject == null) {
            throw new BusinessException("科目编码不存在：" + subjectCode);
        }
        Long subjectId = subject.getSubjectId();
        if (subjectId == null) {
            throw new BusinessException("科目ID为空，科目编码：" + subjectCode);
        }
        return subjectId;
    }
}
