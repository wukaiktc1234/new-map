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
import com.foodtraceability.service.finance.VoucherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AutoVoucherServiceImpl 单元测试
 *
 * <p>Sprint 3.1 P0 T-019（TDD）：验证 getSubjectIdByCode 的真实查询行为、
 * Caffeine 缓存命中、参数校验与异常分支。</p>
 *
 * <p>测试位于 service.finance.impl 包下，便于访问 protected 方法
 * getSubjectIdByCode（参考 InvoiceReimbursementServiceImplTest 模式）。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AutoVoucherServiceImplTest {

    @Mock
    private VoucherService voucherService;

    @Mock
    private FinanceVoucherMapper voucherMapper;

    @Mock
    private AccountingSubjectService accountingSubjectService;

    /**
     * 每个 @Test 独立 new 实例（避免 Caffeine 缓存跨测试累积导致 verify 失败）。
     * 不使用 @InjectMocks，确保 subjectCodeCache 在每个测试中都是空的。
     */
    private AutoVoucherServiceImpl service;

    private AccountingSubject cashSubject;
    private AccountingSubject bankSubject;
    /** 主营业务收入（5001） */
    private AccountingSubject revenueSubject;
    /** 主营业务成本（4011） */
    private AccountingSubject costSubject;
    /** 库存商品（1405） */
    private AccountingSubject inventorySubject;
    /** 其他应付款（2241） */
    private AccountingSubject otherPayableSubject;
    /** 应付职工薪酬（2211） */
    private AccountingSubject salaryPayableSubject;
    /** 管理费用-差旅费（550201） */
    private AccountingSubject travelExpenseSubject;
    /** 管理费用-办公费（550203） */
    private AccountingSubject officeExpenseSubject;
    /** 管理费用-工资（550206） */
    private AccountingSubject salaryExpenseSubject;
    /** 管理费用-其他（550299） */
    private AccountingSubject otherExpenseSubject;

    @BeforeEach
    void setUp() {
        cashSubject = buildSubject(100L, "1001", "库存现金");
        bankSubject = buildSubject(101L, "1002", "银行存款");
        revenueSubject = buildSubject(500L, "5001", "主营业务收入");
        costSubject = buildSubject(401L, "4011", "主营业务成本");
        inventorySubject = buildSubject(140L, "1405", "库存商品");
        otherPayableSubject = buildSubject(224L, "2241", "其他应付款");
        salaryPayableSubject = buildSubject(221L, "2211", "应付职工薪酬");
        travelExpenseSubject = buildSubject(550201L, "550201", "管理费用-差旅费");
        officeExpenseSubject = buildSubject(550203L, "550203", "管理费用-办公费");
        salaryExpenseSubject = buildSubject(550206L, "550206", "管理费用-工资");
        otherExpenseSubject = buildSubject(550299L, "550299", "管理费用-其他");

        // 每个测试独立构造 service，保证缓存隔离
        service = new AutoVoucherServiceImpl(voucherService, voucherMapper, accountingSubjectService);
    }

    /**
     * 构建科目 mock 对象辅助方法
     */
    private AccountingSubject buildSubject(Long subjectId, String code, String name) {
        AccountingSubject subject = new AccountingSubject();
        subject.setSubjectId(subjectId);
        subject.setSubjectCode(code);
        subject.setSubjectName(name);
        return subject;
    }

    /**
     * 构建门店日结完成事件（T-037 测试辅助）
     */
    private StoreDailySettlementCompletedEvent buildSettlementEvent(Long settlementId,
                                                                     Long totalIncome,
                                                                     Long totalExpense) {
        return new StoreDailySettlementCompletedEvent(
                this,
                settlementId,
                2001L,
                "测试门店",
                LocalDate.of(2026, 6, 26),
                totalIncome,
                totalExpense,
                totalIncome - totalExpense,
                3001L,
                LocalDateTime.of(2026, 6, 26, 22, 0, 0)
        );
    }

    /**
     * 构建报销审批通过事件（T-043 测试辅助）
     */
    private InvoiceReimbursementApprovedEvent buildReimbursementEvent(Long reimbursementId,
                                                                      String reimbursementType,
                                                                      Long approvedAmount) {
        return new InvoiceReimbursementApprovedEvent(
                this,
                reimbursementId,
                "BX20260626001",
                4001L,
                5001L,
                "财务部",
                reimbursementType,
                approvedAmount,
                6001L,
                LocalDateTime.of(2026, 6, 26, 14, 30, 0)
        );
    }

    /**
     * 构建凭证VO mock 对象
     */
    private FinanceVoucherVO buildMockVoucherVO(Long voucherId) {
        FinanceVoucherVO vo = new FinanceVoucherVO();
        vo.setVoucherId(voucherId);
        return vo;
    }

    @Test
    @DisplayName("getSubjectIdByCode: 科目存在时返回真实ID（非0L）")
    void getSubjectIdByCode_success_returnsRealId() {
        when(accountingSubjectService.getByCode("1001")).thenReturn(cashSubject);

        Long result = service.getSubjectIdByCode("1001");

        assertEquals(100L, result, "应返回真实科目ID 100L，而非默认值0L");
        verify(accountingSubjectService, times(1)).getByCode("1001");
    }

    @Test
    @DisplayName("getSubjectIdByCode: 科目不存在时抛出BusinessException")
    void getSubjectIdByCode_notFound_throwsBusinessException() {
        when(accountingSubjectService.getByCode("9999")).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getSubjectIdByCode("9999"));
        assertTrue(ex.getMessage().contains("9999") || ex.getMessage().contains("科目"),
                "异常消息应包含科目编码或科目关键字，实际消息: " + ex.getMessage());
    }

    @Test
    @DisplayName("getSubjectIdByCode: 缓存命中时避免第二次DB查询")
    void getSubjectIdByCode_cacheHit_avoidsSecondDbCall() {
        when(accountingSubjectService.getByCode("1001")).thenReturn(cashSubject);

        Long first = service.getSubjectIdByCode("1001");
        Long second = service.getSubjectIdByCode("1001");

        assertEquals(100L, first, "首次调用应返回真实ID");
        assertEquals(100L, second, "第二次调用应返回相同ID（来自缓存）");
        verify(accountingSubjectService, times(1)).getByCode("1001");
    }

    @Test
    @DisplayName("getSubjectIdByCode: 不同科目编码独立缓存")
    void getSubjectIdByCode_differentCodes_cachedSeparately() {
        when(accountingSubjectService.getByCode("1001")).thenReturn(cashSubject);
        when(accountingSubjectService.getByCode("1002")).thenReturn(bankSubject);

        Long first = service.getSubjectIdByCode("1001");
        Long second = service.getSubjectIdByCode("1002");
        Long third = service.getSubjectIdByCode("1001");

        assertEquals(100L, first);
        assertEquals(101L, second);
        assertEquals(100L, third);
        verify(accountingSubjectService, times(1)).getByCode("1001");
        verify(accountingSubjectService, times(1)).getByCode("1002");
    }

    @Test
    @DisplayName("getSubjectIdByCode: null参数抛出IllegalArgumentException")
    void getSubjectIdByCode_nullCode_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getSubjectIdByCode(null));
    }

    @Test
    @DisplayName("getSubjectIdByCode: 空字符串/空白字符参数抛出IllegalArgumentException")
    void getSubjectIdByCode_emptyCode_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getSubjectIdByCode(""));
        assertThrows(IllegalArgumentException.class,
                () -> service.getSubjectIdByCode("   "));
    }

    // ==================== T-037: generateStoreSettlementVoucher 测试 ====================

    @Test
    @DisplayName("T-037 generateStoreSettlementVoucher: 幂等检查命中时返回null且不调用voucherService.create")
    void generateStoreSettlementVoucher_idempotentHit_returnsNull() {
        // 幂等检查命中：voucherMapper.selectBySource 返回非 null
        when(voucherMapper.selectBySource(9, 1001L)).thenReturn(new FinanceVoucher());

        StoreDailySettlementCompletedEvent event = buildSettlementEvent(1001L, 10000L, 5000L);

        FinanceVoucherVO result = service.generateStoreSettlementVoucher(event);

        assertNull(result, "幂等检查命中时应返回 null");
        verify(voucherService, never()).create(any());
    }

    @Test
    @DisplayName("T-037 generateStoreSettlementVoucher: 收入和支出均为0时返回null且不调用voucherService.create")
    void generateStoreSettlementVoucher_zeroAmount_returnsNull() {
        // 幂等检查未命中
        when(voucherMapper.selectBySource(9, 1001L)).thenReturn(null);

        StoreDailySettlementCompletedEvent event = buildSettlementEvent(1001L, 0L, 0L);

        FinanceVoucherVO result = service.generateStoreSettlementVoucher(event);

        assertNull(result, "收入和支出均为0时应返回 null");
        verify(voucherService, never()).create(any());
    }

    @Test
    @DisplayName("T-037 generateStoreSettlementVoucher: 仅收入时生成2条分录（借银行存款1002，贷主营业务收入5001）")
    void generateStoreSettlementVoucher_onlyIncome_generatesTwoEntries() {
        when(voucherMapper.selectBySource(9, 1001L)).thenReturn(null);
        when(accountingSubjectService.getByCode("1002")).thenReturn(bankSubject);
        when(accountingSubjectService.getByCode("5001")).thenReturn(revenueSubject);
        when(voucherService.create(any())).thenReturn(buildMockVoucherVO(8001L));

        StoreDailySettlementCompletedEvent event = buildSettlementEvent(1001L, 10000L, 0L);

        FinanceVoucherVO result = service.generateStoreSettlementVoucher(event);

        assertNotNull(result, "应返回非空凭证VO");
        assertEquals(8001L, result.getVoucherId());

        ArgumentCaptor<FinanceVoucherCreateDTO> captor = ArgumentCaptor.forClass(FinanceVoucherCreateDTO.class);
        verify(voucherService).create(captor.capture());
        FinanceVoucherCreateDTO dto = captor.getValue();

        // 验证凭证头：类型5、来源类型9、来源ID
        assertEquals(5, dto.getVoucherType(), "凭证类型应为 VOUCHER_TYPE_STORE_SETTLEMENT(5)");
        assertEquals(9, dto.getSourceType(), "来源类型应为 SOURCE_TYPE_STORE_SETTLEMENT(9)");
        assertEquals(1001L, dto.getSourceId(), "来源ID应为日结单ID");
        assertEquals(2, dto.getDetails().size(), "仅收入时应生成2条分录");

        // 验证借方：银行存款 1002，金额10000
        FinanceVoucherCreateDTO.VoucherDetailItem debitItem = dto.getDetails().get(0);
        assertEquals(101L, debitItem.getSubjectId(), "借方科目应为银行存款(ID=101)");
        assertEquals(10000L, debitItem.getDebitAmount(), "借方金额应为10000");
        assertEquals(0L, debitItem.getCreditAmount(), "借方分录贷方金额应为0");

        // 验证贷方：主营业务收入 5001，金额10000
        FinanceVoucherCreateDTO.VoucherDetailItem creditItem = dto.getDetails().get(1);
        assertEquals(500L, creditItem.getSubjectId(), "贷方科目应为主营业务收入(ID=500)");
        assertEquals(0L, creditItem.getDebitAmount(), "贷方分录借方金额应为0");
        assertEquals(10000L, creditItem.getCreditAmount(), "贷方金额应为10000");
    }

    @Test
    @DisplayName("T-037 generateStoreSettlementVoucher: 仅支出时生成2条分录（借主营业务成本4011，贷库存商品1405）")
    void generateStoreSettlementVoucher_onlyExpense_generatesTwoEntries() {
        when(voucherMapper.selectBySource(9, 1002L)).thenReturn(null);
        when(accountingSubjectService.getByCode("4011")).thenReturn(costSubject);
        when(accountingSubjectService.getByCode("1405")).thenReturn(inventorySubject);
        when(voucherService.create(any())).thenReturn(buildMockVoucherVO(8002L));

        StoreDailySettlementCompletedEvent event = buildSettlementEvent(1002L, 0L, 5000L);

        FinanceVoucherVO result = service.generateStoreSettlementVoucher(event);

        assertNotNull(result);
        assertEquals(8002L, result.getVoucherId());

        ArgumentCaptor<FinanceVoucherCreateDTO> captor = ArgumentCaptor.forClass(FinanceVoucherCreateDTO.class);
        verify(voucherService).create(captor.capture());
        FinanceVoucherCreateDTO dto = captor.getValue();

        assertEquals(5, dto.getVoucherType());
        assertEquals(9, dto.getSourceType());
        assertEquals(1002L, dto.getSourceId());
        assertEquals(2, dto.getDetails().size(), "仅支出时应生成2条分录");

        // 借方：主营业务成本 4011
        FinanceVoucherCreateDTO.VoucherDetailItem debitItem = dto.getDetails().get(0);
        assertEquals(401L, debitItem.getSubjectId(), "借方科目应为主营业务成本(ID=401)");
        assertEquals(5000L, debitItem.getDebitAmount());
        assertEquals(0L, debitItem.getCreditAmount());

        // 贷方：库存商品 1405
        FinanceVoucherCreateDTO.VoucherDetailItem creditItem = dto.getDetails().get(1);
        assertEquals(140L, creditItem.getSubjectId(), "贷方科目应为库存商品(ID=140)");
        assertEquals(0L, creditItem.getDebitAmount());
        assertEquals(5000L, creditItem.getCreditAmount());
    }

    @Test
    @DisplayName("T-037 generateStoreSettlementVoucher: 收入+支出时生成4条分录（借银行+成本，贷收入+库存）")
    void generateStoreSettlementVoucher_incomeAndExpense_generatesFourEntries() {
        when(voucherMapper.selectBySource(9, 1003L)).thenReturn(null);
        when(accountingSubjectService.getByCode("1002")).thenReturn(bankSubject);
        when(accountingSubjectService.getByCode("5001")).thenReturn(revenueSubject);
        when(accountingSubjectService.getByCode("4011")).thenReturn(costSubject);
        when(accountingSubjectService.getByCode("1405")).thenReturn(inventorySubject);
        when(voucherService.create(any())).thenReturn(buildMockVoucherVO(8003L));

        StoreDailySettlementCompletedEvent event = buildSettlementEvent(1003L, 10000L, 5000L);

        FinanceVoucherVO result = service.generateStoreSettlementVoucher(event);

        assertNotNull(result);
        ArgumentCaptor<FinanceVoucherCreateDTO> captor = ArgumentCaptor.forClass(FinanceVoucherCreateDTO.class);
        verify(voucherService).create(captor.capture());
        FinanceVoucherCreateDTO dto = captor.getValue();

        assertEquals(4, dto.getDetails().size(), "收入+支出时应生成4条分录");

        // 验证借贷平衡：借方合计 = 10000 + 5000 = 15000
        long totalDebit = dto.getDetails().stream().mapToLong(FinanceVoucherCreateDTO.VoucherDetailItem::getDebitAmount).sum();
        long totalCredit = dto.getDetails().stream().mapToLong(FinanceVoucherCreateDTO.VoucherDetailItem::getCreditAmount).sum();
        assertEquals(totalDebit, totalCredit, "借贷应平衡");
        assertEquals(15000L, totalDebit, "借方合计应为15000");
    }

    // ==================== T-043: generateReimbursementVoucher 测试 ====================

    @Test
    @DisplayName("T-043 generateReimbursementVoucher: 幂等检查命中时返回null且不调用voucherService.create")
    void generateReimbursementVoucher_idempotentHit_returnsNull() {
        when(voucherMapper.selectBySource(10, 7001L)).thenReturn(new FinanceVoucher());

        InvoiceReimbursementApprovedEvent event = buildReimbursementEvent(7001L, "差旅费", 20000L);

        FinanceVoucherVO result = service.generateReimbursementVoucher(event);

        assertNull(result, "幂等检查命中时应返回 null");
        verify(voucherService, never()).create(any());
    }

    @Test
    @DisplayName("T-043 generateReimbursementVoucher: approvedAmount为0时返回null")
    void generateReimbursementVoucher_zeroAmount_returnsNull() {
        when(voucherMapper.selectBySource(10, 7002L)).thenReturn(null);

        InvoiceReimbursementApprovedEvent event = buildReimbursementEvent(7002L, "办公费", 0L);

        FinanceVoucherVO result = service.generateReimbursementVoucher(event);

        assertNull(result, "审批金额为0时应返回 null");
        verify(voucherService, never()).create(any());
    }

    @Test
    @DisplayName("T-043 generateReimbursementVoucher: 差旅费映射科目编码550201")
    void generateReimbursementVoucher_travelType_mapsTo550201() {
        when(voucherMapper.selectBySource(10, 7003L)).thenReturn(null);
        when(accountingSubjectService.getByCode("550201")).thenReturn(travelExpenseSubject);
        when(accountingSubjectService.getByCode("2241")).thenReturn(otherPayableSubject);
        when(voucherService.create(any())).thenReturn(buildMockVoucherVO(9001L));

        InvoiceReimbursementApprovedEvent event = buildReimbursementEvent(7003L, "差旅费", 20000L);

        FinanceVoucherVO result = service.generateReimbursementVoucher(event);

        assertNotNull(result);
        ArgumentCaptor<FinanceVoucherCreateDTO> captor = ArgumentCaptor.forClass(FinanceVoucherCreateDTO.class);
        verify(voucherService).create(captor.capture());
        FinanceVoucherCreateDTO dto = captor.getValue();

        assertEquals(6, dto.getVoucherType(), "凭证类型应为 VOUCHER_TYPE_REIMBURSEMENT(6)");
        assertEquals(10, dto.getSourceType(), "来源类型应为 SOURCE_TYPE_REIMBURSEMENT(10)");
        assertEquals(7003L, dto.getSourceId());
        assertEquals(2, dto.getDetails().size());

        // 借方：管理费用-差旅费 550201
        FinanceVoucherCreateDTO.VoucherDetailItem debitItem = dto.getDetails().get(0);
        assertEquals(550201L, debitItem.getSubjectId(), "差旅费应映射到科目550201");
        assertEquals(20000L, debitItem.getDebitAmount());

        // 贷方：其他应付款 2241
        FinanceVoucherCreateDTO.VoucherDetailItem creditItem = dto.getDetails().get(1);
        assertEquals(224L, creditItem.getSubjectId(), "贷方应为其他应付款(ID=224)");
        assertEquals(20000L, creditItem.getCreditAmount());
    }

    @Test
    @DisplayName("T-043 generateReimbursementVoucher: 办公费映射科目编码550203")
    void generateReimbursementVoucher_officeType_mapsTo550203() {
        when(voucherMapper.selectBySource(10, 7004L)).thenReturn(null);
        when(accountingSubjectService.getByCode("550203")).thenReturn(officeExpenseSubject);
        when(accountingSubjectService.getByCode("2241")).thenReturn(otherPayableSubject);
        when(voucherService.create(any())).thenReturn(buildMockVoucherVO(9002L));

        InvoiceReimbursementApprovedEvent event = buildReimbursementEvent(7004L, "办公费", 8000L);

        service.generateReimbursementVoucher(event);

        ArgumentCaptor<FinanceVoucherCreateDTO> captor = ArgumentCaptor.forClass(FinanceVoucherCreateDTO.class);
        verify(voucherService).create(captor.capture());
        FinanceVoucherCreateDTO dto = captor.getValue();

        // 借方：管理费用-办公费 550203
        assertEquals(550203L, dto.getDetails().get(0).getSubjectId(), "办公费应映射到科目550203");
        assertEquals(8000L, dto.getDetails().get(0).getDebitAmount());
    }

    @Test
    @DisplayName("T-043 generateReimbursementVoucher: 未知类型/null类型映射科目编码550299（其他）")
    void generateReimbursementVoucher_unknownType_mapsTo550299() {
        when(voucherMapper.selectBySource(10, 7005L)).thenReturn(null);
        when(accountingSubjectService.getByCode("550299")).thenReturn(otherExpenseSubject);
        when(accountingSubjectService.getByCode("2241")).thenReturn(otherPayableSubject);
        when(voucherService.create(any())).thenReturn(buildMockVoucherVO(9003L));

        // 未知报销类型
        InvoiceReimbursementApprovedEvent event = buildReimbursementEvent(7005L, "未知类型", 5000L);

        service.generateReimbursementVoucher(event);

        ArgumentCaptor<FinanceVoucherCreateDTO> captor = ArgumentCaptor.forClass(FinanceVoucherCreateDTO.class);
        verify(voucherService).create(captor.capture());
        FinanceVoucherCreateDTO dto = captor.getValue();

        // 借方：管理费用-其他 550299
        assertEquals(550299L, dto.getDetails().get(0).getSubjectId(), "未知类型应映射到科目550299");
        assertEquals(5000L, dto.getDetails().get(0).getDebitAmount());
    }

    // ==================== T-045: generateSalaryVoucher 测试 ====================

    @Test
    @DisplayName("T-045 generateSalaryVoucher: 幂等检查命中时返回null且不调用voucherService.create")
    void generateSalaryVoucher_idempotentHit_returnsNull() {
        when(voucherMapper.selectBySource(11, 6001L)).thenReturn(new FinanceVoucher());

        FinanceVoucherVO result = service.generateSalaryVoucher(6001L, 50000L);

        assertNull(result, "幂等检查命中时应返回 null");
        verify(voucherService, never()).create(any());
    }

    @Test
    @DisplayName("T-045 generateSalaryVoucher: 正常生成凭证（借管理费用-工资550206，贷应付职工薪酬2211）")
    void generateSalaryVoucher_normal_generatesVoucher() {
        when(voucherMapper.selectBySource(11, 6002L)).thenReturn(null);
        when(accountingSubjectService.getByCode("550206")).thenReturn(salaryExpenseSubject);
        when(accountingSubjectService.getByCode("2211")).thenReturn(salaryPayableSubject);
        when(voucherService.create(any())).thenReturn(buildMockVoucherVO(10001L));

        FinanceVoucherVO result = service.generateSalaryVoucher(6002L, 50000L);

        assertNotNull(result);
        assertEquals(10001L, result.getVoucherId());

        ArgumentCaptor<FinanceVoucherCreateDTO> captor = ArgumentCaptor.forClass(FinanceVoucherCreateDTO.class);
        verify(voucherService).create(captor.capture());
        FinanceVoucherCreateDTO dto = captor.getValue();

        // 验证凭证类型7、来源类型11、来源ID
        assertEquals(7, dto.getVoucherType(), "凭证类型应为 VOUCHER_TYPE_SALARY(7)");
        assertEquals(11, dto.getSourceType(), "来源类型应为 SOURCE_TYPE_SALARY(11，非7避免与COST冲突)");
        assertEquals(6002L, dto.getSourceId());
        assertEquals(2, dto.getDetails().size());

        // 借方：管理费用-工资 550206
        FinanceVoucherCreateDTO.VoucherDetailItem debitItem = dto.getDetails().get(0);
        assertEquals(550206L, debitItem.getSubjectId(), "借方科目应为管理费用-工资(ID=550206)");
        assertEquals(50000L, debitItem.getDebitAmount());
        assertEquals(0L, debitItem.getCreditAmount());

        // 贷方：应付职工薪酬 2211
        FinanceVoucherCreateDTO.VoucherDetailItem creditItem = dto.getDetails().get(1);
        assertEquals(221L, creditItem.getSubjectId(), "贷方科目应为应付职工薪酬(ID=221)");
        assertEquals(0L, creditItem.getDebitAmount());
        assertEquals(50000L, creditItem.getCreditAmount());
    }

    @Test
    @DisplayName("T-045 generateSalaryVoucher: 验证借贷平衡")
    void generateSalaryVoucher_balancedDebitCredit() {
        when(voucherMapper.selectBySource(11, 6003L)).thenReturn(null);
        when(accountingSubjectService.getByCode("550206")).thenReturn(salaryExpenseSubject);
        when(accountingSubjectService.getByCode("2211")).thenReturn(salaryPayableSubject);
        when(voucherService.create(any())).thenReturn(buildMockVoucherVO(10002L));

        service.generateSalaryVoucher(6003L, 88888L);

        ArgumentCaptor<FinanceVoucherCreateDTO> captor = ArgumentCaptor.forClass(FinanceVoucherCreateDTO.class);
        verify(voucherService).create(captor.capture());
        FinanceVoucherCreateDTO dto = captor.getValue();

        long totalDebit = dto.getDetails().stream().mapToLong(FinanceVoucherCreateDTO.VoucherDetailItem::getDebitAmount).sum();
        long totalCredit = dto.getDetails().stream().mapToLong(FinanceVoucherCreateDTO.VoucherDetailItem::getCreditAmount).sum();
        assertEquals(totalDebit, totalCredit, "借贷应平衡");
        assertEquals(88888L, totalDebit, "借方合计应为88888");
    }
}
