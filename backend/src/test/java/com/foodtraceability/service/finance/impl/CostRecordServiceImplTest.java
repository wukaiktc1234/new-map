package com.foodtraceability.service.finance.impl;

import com.foodtraceability.entity.finance.CostRecord;
import com.foodtraceability.mapper.finance.CostRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CostRecordServiceImpl 单元测试
 *
 * <p>Sprint 3.1 P0 T-041（TDD）：验证 {@link CostRecordServiceImpl#recordReimbursementCost}
 * 将报销金额按部门归集到成本记录表（costType=7 其他成本）。</p>
 *
 * <p>测试位于 service.finance.impl 包下。由于 CostRecordServiceImpl 继承 ServiceImpl，
 * baseMapper 字段需通过 ReflectionTestUtils 注入（参考 VoucherServiceImplTest 模式）。</p>
 *
 * <p>幂等性：MAJOR-04 修复后，{@code recordReimbursementCost} 方法入口通过
 * {@code relatedVoucherId = reimbursementId AND costType = 7} 查询判重。
 * 本测试包含正常插入与幂等跳过两类场景。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CostRecordServiceImplTest {

    /** 成本类型：其他成本 */
    private static final int COST_TYPE_OTHER = 7;

    /** 计算方式：实际发生 */
    private static final int CALCULATION_METHOD_ACTUAL = 1;

    @Mock
    private CostRecordMapper costRecordMapper;

    @InjectMocks
    private CostRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        // ServiceImpl 的 baseMapper 是父类 protected 字段，@InjectMocks 无法注入，需手动设置
        ReflectionTestUtils.setField(service, "baseMapper", costRecordMapper);
        // generateCostNo 调用 getOne -> baseMapper.selectOne，默认返回 null（无上一条记录）
        when(costRecordMapper.selectOne(any())).thenReturn(null);
        // save 方法调用 baseMapper.insert，模拟返回 1（影响行数）
        when(costRecordMapper.insert(any(CostRecord.class))).thenReturn(1);
    }

    /**
     * 构造正常报销参数的辅助方法
     */
    private void callRecordReimbursementCost(Long reimbursementId,
                                             Long departmentId,
                                             String reimbursementType,
                                             Long amount,
                                             LocalDate occurDate) {
        service.recordReimbursementCost(reimbursementId, departmentId, reimbursementType, amount, occurDate);
    }

    @Test
    @DisplayName("T-041 正常调用：成本记录表新增一条记录，costType=7/costCenterId=departmentId/amount=amount")
    void recordReimbursementCost_normal_insertsCostRecordWithCorrectFields() {
        Long reimbursementId = 9001L;
        Long departmentId = 5001L;
        String reimbursementType = "差旅费";
        Long amount = 50000L; // 500.00 元
        LocalDate occurDate = LocalDate.of(2026, 6, 26);

        callRecordReimbursementCost(reimbursementId, departmentId, reimbursementType, amount, occurDate);

        // 捕获 insert 调用的 CostRecord 参数
        ArgumentCaptor<CostRecord> captor = ArgumentCaptor.forClass(CostRecord.class);
        verify(costRecordMapper).insert(captor.capture());

        CostRecord saved = captor.getValue();
        // 验证关键字段
        assertEquals(COST_TYPE_OTHER, saved.getCostType(), "costType 应为 7（其他成本）");
        assertEquals(departmentId, saved.getCostCenterId(), "costCenterId 应为 departmentId");
        assertEquals(amount, saved.getAmount(), "amount 应为报销金额");
        assertEquals(CALCULATION_METHOD_ACTUAL, saved.getCalculationMethod(), "calculationMethod 应为 1（实际发生）");
        assertEquals("2026-06", saved.getPeriod(), "period 应为 occurDate 格式化 yyyy-MM");
        assertEquals(reimbursementId, saved.getRelatedVoucherId(),
                "relatedVoucherId 应为 reimbursementId 用于幂等判重");
        // 验证 costNo 格式：CR + yyyyMMdd + 4位序号
        assertNotNull(saved.getCostNo(), "costNo 不应为空");
        assertTrue(saved.getCostNo().startsWith("CR20260626"), "costNo 应以 CR20260626 开头，实际: " + saved.getCostNo());
        assertEquals(14, saved.getCostNo().length(), "costNo 长度应为 14（CR + 8位日期 + 4位序号）");
        // 验证 remark 包含报销单号
        assertNotNull(saved.getRemark(), "remark 不应为空");
        assertTrue(saved.getRemark().contains("报销单号:" + reimbursementId), "remark 应包含报销单号");
        assertTrue(saved.getRemark().contains(reimbursementType), "remark 应包含报销类型");
    }

    @Test
    @DisplayName("T-041 amount=null：跳过成本归集，不调用 insert")
    void recordReimbursementCost_nullAmount_skipsInsert() {
        callRecordReimbursementCost(9002L, 5001L, "差旅费", null, LocalDate.of(2026, 6, 26));

        verify(costRecordMapper, never()).insert(any(CostRecord.class));
    }

    @Test
    @DisplayName("T-041 amount=0：跳过成本归集，不调用 insert")
    void recordReimbursementCost_zeroAmount_skipsInsert() {
        callRecordReimbursementCost(9003L, 5001L, "差旅费", 0L, LocalDate.of(2026, 6, 26));

        verify(costRecordMapper, never()).insert(any(CostRecord.class));
    }

    @Test
    @DisplayName("T-041 amount<0：跳过成本归集，不调用 insert")
    void recordReimbursementCost_negativeAmount_skipsInsert() {
        callRecordReimbursementCost(9004L, 5001L, "差旅费", -100L, LocalDate.of(2026, 6, 26));

        verify(costRecordMapper, never()).insert(any(CostRecord.class));
    }

    @Test
    @DisplayName("T-041 occurDate=null：跳过成本归集，不调用 insert")
    void recordReimbursementCost_nullOccurDate_skipsInsert() {
        callRecordReimbursementCost(9005L, 5001L, "差旅费", 50000L, null);

        verify(costRecordMapper, never()).insert(any(CostRecord.class));
    }

    @Test
    @DisplayName("T-041 reimbursementType=null：remark 不包含类型，但正常插入")
    void recordReimbursementCost_nullType_insertsWithoutTypeInRemark() {
        Long reimbursementId = 9006L;
        Long departmentId = 5002L;
        Long amount = 30000L;
        LocalDate occurDate = LocalDate.of(2026, 6, 26);

        callRecordReimbursementCost(reimbursementId, departmentId, null, amount, occurDate);

        ArgumentCaptor<CostRecord> captor = ArgumentCaptor.forClass(CostRecord.class);
        verify(costRecordMapper).insert(captor.capture());

        CostRecord saved = captor.getValue();
        assertEquals(COST_TYPE_OTHER, saved.getCostType());
        assertEquals(departmentId, saved.getCostCenterId());
        assertEquals(amount, saved.getAmount());
        assertNotNull(saved.getRemark());
        assertTrue(saved.getRemark().contains("报销单号:" + reimbursementId));
        // reimbursementType 为 null 时 remark 不应包含"类型:"
        assertTrue(!saved.getRemark().contains("类型:"), "remark 不应包含类型信息");
    }

    @Test
    @DisplayName("T-041 跨月份：occurDate 为不同月份时 period 正确格式化，costNo 用创建日期")
    void recordReimbursementCost_differentMonth_periodFormattedCorrectly() {
        callRecordReimbursementCost(9007L, 5003L, "办公费", 12000L, LocalDate.of(2026, 1, 15));

        ArgumentCaptor<CostRecord> captor = ArgumentCaptor.forClass(CostRecord.class);
        verify(costRecordMapper).insert(captor.capture());

        CostRecord saved = captor.getValue();
        // period 应基于 occurDate（成本归属期间）
        assertEquals("2026-01", saved.getPeriod(), "period 应为 2026-01");
        // costNo 应基于 LocalDate.now()（创建日期），而非 occurDate
        String expectedDatePrefix = "CR" + LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        assertTrue(saved.getCostNo().startsWith(expectedDatePrefix),
                "costNo 应以 " + expectedDatePrefix + " 开头（基于创建日期），实际: " + saved.getCostNo());
    }

    @Test
    @DisplayName("T-041 幂等检查：relatedVoucherId 已存在时跳过 insert（MAJOR-04）")
    void recordReimbursementCost_idempotent_skipInsertWhenAlreadyExists() {
        Long reimbursementId = 9008L;

        // 模拟已存在归集记录（selectCount 返回 > 0 触发幂等跳过）
        when(costRecordMapper.selectCount(any())).thenReturn(1L);

        callRecordReimbursementCost(reimbursementId, 5001L, "差旅费", 50000L, LocalDate.of(2026, 6, 26));

        // 验证幂等跳过：不调用 insert
        verify(costRecordMapper, never()).insert(any(CostRecord.class));
    }
}
