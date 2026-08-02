package com.foodtraceability.service.finance.impl;

import com.foodtraceability.entity.finance.AccountingSubject;
import com.foodtraceability.entity.finance.FinanceVoucher;
import com.foodtraceability.entity.finance.FinanceVoucherDetail;
import com.foodtraceability.exception.BusinessException;
import com.foodtraceability.mapper.finance.AccountingSubjectMapper;
import com.foodtraceability.mapper.finance.FinanceVoucherDetailMapper;
import com.foodtraceability.mapper.finance.FinanceVoucherMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * VoucherServiceImpl 单元测试
 *
 * <p>Sprint 3.1 P0 T-021/T-022（TDD）：验证凭证过账/反过账时
 * 科目余额按 ADR-005 规则更新，使用乐观锁防并发。</p>
 *
 * <p>测试位于 service.finance.impl 包下。由于 VoucherServiceImpl
 * 继承 ServiceImpl，baseMapper 字段需通过 ReflectionTestUtils 注入。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VoucherServiceImplTest {

    @Mock
    private FinanceVoucherMapper voucherMapper;

    @Mock
    private FinanceVoucherDetailMapper detailMapper;

    @Mock
    private AccountingSubjectMapper subjectMapper;

    @InjectMocks
    private VoucherServiceImpl service;

    /** 借方科目方向常量 */
    private static final int DIRECTION_DEBIT = 1;
    /** 贷方科目方向常量 */
    private static final int DIRECTION_CREDIT = 2;
    /** 凭证状态：已审核 */
    private static final int STATUS_APPROVED = 1;
    /** 凭证状态：已过账 */
    private static final int STATUS_POSTED = 2;

    @BeforeEach
    void setUp() {
        // ServiceImpl 的 baseMapper 是父类 protected 字段，@InjectMocks 无法注入，需手动设置
        ReflectionTestUtils.setField(service, "baseMapper", voucherMapper);
    }

    /**
     * 构造已审核凭证（状态=1）
     */
    private FinanceVoucher buildApprovedVoucher(Long voucherId) {
        FinanceVoucher voucher = new FinanceVoucher();
        voucher.setVoucherId(voucherId);
        voucher.setVoucherStatus(STATUS_APPROVED);
        return voucher;
    }

    /**
     * 构造科目
     * @param subjectId 科目ID
     * @param direction 余额方向（1=借方，2=贷方）
     * @param version 乐观锁版本号
     */
    private AccountingSubject buildSubject(Long subjectId, int direction, int version) {
        AccountingSubject subject = new AccountingSubject();
        subject.setSubjectId(subjectId);
        subject.setSubjectCode("100" + subjectId);
        subject.setSubjectName("测试科目" + subjectId);
        subject.setDirection(direction);
        subject.setVersion(version);
        return subject;
    }

    /**
     * 构造凭证明细
     */
    private FinanceVoucherDetail buildDetail(Long subjectId, Long debit, Long credit) {
        FinanceVoucherDetail detail = new FinanceVoucherDetail();
        detail.setSubjectId(subjectId);
        detail.setDebitAmount(debit);
        detail.setCreditAmount(credit);
        return detail;
    }

    // ==================== T-021 post() 测试 ====================

    @Test
    @DisplayName("post: 借方分录+借方科目 → balance + debit（规则1）")
    void post_debitEntry_debitSubject_increasesBalance() {
        FinanceVoucher voucher = buildApprovedVoucher(1L);
        when(voucherMapper.selectById(1L)).thenReturn(voucher);
        when(detailMapper.selectByVoucherId(1L)).thenReturn(List.of(
                buildDetail(10L, 10000L, 0L)
        ));
        when(subjectMapper.selectById(10L)).thenReturn(buildSubject(10L, DIRECTION_DEBIT, 0));
        when(subjectMapper.updateBalanceWithOptimisticLock(10L, 10000L, 0)).thenReturn(1);
        when(voucherMapper.updateById(any(FinanceVoucher.class))).thenReturn(1);

        boolean result = service.post(1L);

        assertTrue(result);
        assertEquals(STATUS_POSTED, voucher.getVoucherStatus(), "凭证状态应更新为已过账(2)");
        verify(subjectMapper, times(1)).updateBalanceWithOptimisticLock(10L, 10000L, 0);
    }

    @Test
    @DisplayName("post: 借方分录+贷方科目 → balance - debit（规则2）")
    void post_debitEntry_creditSubject_decreasesBalance() {
        FinanceVoucher voucher = buildApprovedVoucher(1L);
        when(voucherMapper.selectById(1L)).thenReturn(voucher);
        when(detailMapper.selectByVoucherId(1L)).thenReturn(List.of(
                buildDetail(20L, 5000L, 0L)
        ));
        when(subjectMapper.selectById(20L)).thenReturn(buildSubject(20L, DIRECTION_CREDIT, 0));
        when(subjectMapper.updateBalanceWithOptimisticLock(20L, -5000L, 0)).thenReturn(1);
        when(voucherMapper.updateById(any(FinanceVoucher.class))).thenReturn(1);

        boolean result = service.post(1L);

        assertTrue(result);
        // 借方分录+贷方科目：delta = -debit = -5000
        verify(subjectMapper).updateBalanceWithOptimisticLock(20L, -5000L, 0);
    }

    @Test
    @DisplayName("post: 贷方分录+借方科目 → balance - credit（规则3）")
    void post_creditEntry_debitSubject_decreasesBalance() {
        FinanceVoucher voucher = buildApprovedVoucher(1L);
        when(voucherMapper.selectById(1L)).thenReturn(voucher);
        when(detailMapper.selectByVoucherId(1L)).thenReturn(List.of(
                buildDetail(30L, 0L, 8000L)
        ));
        when(subjectMapper.selectById(30L)).thenReturn(buildSubject(30L, DIRECTION_DEBIT, 1));
        when(subjectMapper.updateBalanceWithOptimisticLock(30L, -8000L, 1)).thenReturn(1);
        when(voucherMapper.updateById(any(FinanceVoucher.class))).thenReturn(1);

        boolean result = service.post(1L);

        assertTrue(result);
        // 贷方分录+借方科目：delta = -credit = -8000
        verify(subjectMapper).updateBalanceWithOptimisticLock(30L, -8000L, 1);
    }

    @Test
    @DisplayName("post: 贷方分录+贷方科目 → balance + credit（规则4）")
    void post_creditEntry_creditSubject_increasesBalance() {
        FinanceVoucher voucher = buildApprovedVoucher(1L);
        when(voucherMapper.selectById(1L)).thenReturn(voucher);
        when(detailMapper.selectByVoucherId(1L)).thenReturn(List.of(
                buildDetail(40L, 0L, 12000L)
        ));
        when(subjectMapper.selectById(40L)).thenReturn(buildSubject(40L, DIRECTION_CREDIT, 2));
        when(subjectMapper.updateBalanceWithOptimisticLock(40L, 12000L, 2)).thenReturn(1);
        when(voucherMapper.updateById(any(FinanceVoucher.class))).thenReturn(1);

        boolean result = service.post(1L);

        assertTrue(result);
        // 贷方分录+贷方科目：delta = +credit = +12000
        verify(subjectMapper).updateBalanceWithOptimisticLock(40L, 12000L, 2);
    }

    @Test
    @DisplayName("post: 多条明细全部更新余额")
    void post_multipleDetails_updatesAllSubjects() {
        FinanceVoucher voucher = buildApprovedVoucher(1L);
        when(voucherMapper.selectById(1L)).thenReturn(voucher);
        when(detailMapper.selectByVoucherId(1L)).thenReturn(List.of(
                buildDetail(10L, 10000L, 0L),   // 借方+借方科目 → +10000
                buildDetail(20L, 0L, 10000L)    // 贷方+贷方科目 → +10000
        ));
        when(subjectMapper.selectById(10L)).thenReturn(buildSubject(10L, DIRECTION_DEBIT, 0));
        when(subjectMapper.selectById(20L)).thenReturn(buildSubject(20L, DIRECTION_CREDIT, 0));
        when(subjectMapper.updateBalanceWithOptimisticLock(anyLong(), anyLong(), any())).thenReturn(1);
        when(voucherMapper.updateById(any(FinanceVoucher.class))).thenReturn(1);

        boolean result = service.post(1L);

        assertTrue(result);
        verify(subjectMapper, times(2)).updateBalanceWithOptimisticLock(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("post: 凭证不存在抛 BusinessException")
    void post_voucherNotFound_throwsBusinessException() {
        when(voucherMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.post(999L));
        assertTrue(ex.getMessage().contains("凭证") || ex.getMessage().contains("不存在"));
    }

    @Test
    @DisplayName("post: 凭证状态非已审核(1)抛 BusinessException")
    void post_voucherNotApproved_throwsBusinessException() {
        FinanceVoucher voucher = buildApprovedVoucher(1L);
        voucher.setVoucherStatus(0); // 暂存
        when(voucherMapper.selectById(1L)).thenReturn(voucher);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.post(1L));
        assertTrue(ex.getMessage().contains("审核") || ex.getMessage().contains("过账"));
        // 不应调用余额更新
        verify(subjectMapper, never()).updateBalanceWithOptimisticLock(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("post: 科目不存在抛 BusinessException")
    void post_subjectNotFound_throwsBusinessException() {
        FinanceVoucher voucher = buildApprovedVoucher(1L);
        when(voucherMapper.selectById(1L)).thenReturn(voucher);
        when(detailMapper.selectByVoucherId(1L)).thenReturn(List.of(
                buildDetail(999L, 1000L, 0L)
        ));
        when(subjectMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.post(1L));
        assertTrue(ex.getMessage().contains("科目") || ex.getMessage().contains("不存在"));
    }

    @Test
    @DisplayName("post: 乐观锁冲突(返回0)抛 BusinessException")
    void post_optimisticLockConflict_throwsBusinessException() {
        FinanceVoucher voucher = buildApprovedVoucher(1L);
        when(voucherMapper.selectById(1L)).thenReturn(voucher);
        when(detailMapper.selectByVoucherId(1L)).thenReturn(List.of(
                buildDetail(10L, 1000L, 0L)
        ));
        when(subjectMapper.selectById(10L)).thenReturn(buildSubject(10L, DIRECTION_DEBIT, 0));
        when(subjectMapper.updateBalanceWithOptimisticLock(10L, 1000L, 0)).thenReturn(0); // 冲突

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.post(1L));
        assertTrue(ex.getMessage().contains("乐观锁") || ex.getMessage().contains("冲突")
                || ex.getMessage().contains("余额"));
    }

    @Test
    @DisplayName("post: 明细为空时抛 BusinessException")
    void post_emptyDetails_throwsBusinessException() {
        FinanceVoucher voucher = buildApprovedVoucher(1L);
        when(voucherMapper.selectById(1L)).thenReturn(voucher);
        when(detailMapper.selectByVoucherId(1L)).thenReturn(Collections.emptyList());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.post(1L));
        assertTrue(ex.getMessage().contains("明细") || ex.getMessage().contains("分录"));
    }

    // ==================== T-022 unpost() 测试 ====================

    /**
     * 构造已过账凭证（状态=2）
     */
    private FinanceVoucher buildPostedVoucher(Long voucherId) {
        FinanceVoucher voucher = new FinanceVoucher();
        voucher.setVoucherId(voucherId);
        voucher.setVoucherStatus(STATUS_POSTED);
        return voucher;
    }

    @Test
    @DisplayName("unpost: 借方科目余额反向更新（post时+debit，unpost时-debit）")
    void unpost_debitSubject_reversesBalance() {
        FinanceVoucher voucher = buildPostedVoucher(1L);
        when(voucherMapper.selectById(1L)).thenReturn(voucher);
        when(detailMapper.selectByVoucherId(1L)).thenReturn(List.of(
                buildDetail(10L, 10000L, 0L)
        ));
        when(subjectMapper.selectById(10L)).thenReturn(buildSubject(10L, DIRECTION_DEBIT, 0));
        // unpost 反向 delta = -(10000-0) = -10000
        when(subjectMapper.updateBalanceWithOptimisticLock(10L, -10000L, 0)).thenReturn(1);
        when(voucherMapper.updateById(any(FinanceVoucher.class))).thenReturn(1);

        boolean result = service.unpost(1L);

        assertTrue(result);
        assertEquals(STATUS_APPROVED, voucher.getVoucherStatus(), "凭证状态应回退为已审核(1)");
        verify(subjectMapper).updateBalanceWithOptimisticLock(10L, -10000L, 0);
    }

    @Test
    @DisplayName("unpost: 贷方科目余额反向更新（post时+credit，unpost时-credit）")
    void unpost_creditSubject_reversesBalance() {
        FinanceVoucher voucher = buildPostedVoucher(1L);
        when(voucherMapper.selectById(1L)).thenReturn(voucher);
        when(detailMapper.selectByVoucherId(1L)).thenReturn(List.of(
                buildDetail(20L, 0L, 12000L)
        ));
        when(subjectMapper.selectById(20L)).thenReturn(buildSubject(20L, DIRECTION_CREDIT, 0));
        // 贷方科目 post delta = credit-debit = 12000；unpost 反向 = -12000
        when(subjectMapper.updateBalanceWithOptimisticLock(20L, -12000L, 0)).thenReturn(1);
        when(voucherMapper.updateById(any(FinanceVoucher.class))).thenReturn(1);

        boolean result = service.unpost(1L);

        assertTrue(result);
        assertEquals(STATUS_APPROVED, voucher.getVoucherStatus());
        verify(subjectMapper).updateBalanceWithOptimisticLock(20L, -12000L, 0);
    }

    @Test
    @DisplayName("unpost: 多条明细全部反向更新")
    void unpost_multipleDetails_reversesAll() {
        FinanceVoucher voucher = buildPostedVoucher(1L);
        when(voucherMapper.selectById(1L)).thenReturn(voucher);
        when(detailMapper.selectByVoucherId(1L)).thenReturn(List.of(
                buildDetail(10L, 10000L, 0L),   // 借方+借方科目：post +10000，unpost -10000
                buildDetail(20L, 0L, 10000L)    // 贷方+贷方科目：post +10000，unpost -10000
        ));
        when(subjectMapper.selectById(10L)).thenReturn(buildSubject(10L, DIRECTION_DEBIT, 0));
        when(subjectMapper.selectById(20L)).thenReturn(buildSubject(20L, DIRECTION_CREDIT, 0));
        when(subjectMapper.updateBalanceWithOptimisticLock(anyLong(), anyLong(), any())).thenReturn(1);
        when(voucherMapper.updateById(any(FinanceVoucher.class))).thenReturn(1);

        boolean result = service.unpost(1L);

        assertTrue(result);
        verify(subjectMapper, times(2)).updateBalanceWithOptimisticLock(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("unpost: 凭证不存在抛 BusinessException")
    void unpost_voucherNotFound_throwsBusinessException() {
        when(voucherMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.unpost(999L));
        assertTrue(ex.getMessage().contains("凭证") || ex.getMessage().contains("不存在"));
    }

    @Test
    @DisplayName("unpost: 凭证状态非已过账(2)抛 BusinessException")
    void unpost_voucherNotPosted_throwsBusinessException() {
        FinanceVoucher voucher = buildPostedVoucher(1L);
        voucher.setVoucherStatus(STATUS_APPROVED); // 已审核，非已过账
        when(voucherMapper.selectById(1L)).thenReturn(voucher);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.unpost(1L));
        assertTrue(ex.getMessage().contains("过账") || ex.getMessage().contains("反过账"));
        verify(subjectMapper, never()).updateBalanceWithOptimisticLock(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("unpost: 科目不存在抛 BusinessException")
    void unpost_subjectNotFound_throwsBusinessException() {
        FinanceVoucher voucher = buildPostedVoucher(1L);
        when(voucherMapper.selectById(1L)).thenReturn(voucher);
        when(detailMapper.selectByVoucherId(1L)).thenReturn(List.of(
                buildDetail(999L, 1000L, 0L)
        ));
        when(subjectMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.unpost(1L));
        assertTrue(ex.getMessage().contains("科目") || ex.getMessage().contains("不存在"));
    }

    @Test
    @DisplayName("unpost: 乐观锁冲突(返回0)抛 BusinessException")
    void unpost_optimisticLockConflict_throwsBusinessException() {
        FinanceVoucher voucher = buildPostedVoucher(1L);
        when(voucherMapper.selectById(1L)).thenReturn(voucher);
        when(detailMapper.selectByVoucherId(1L)).thenReturn(List.of(
                buildDetail(10L, 1000L, 0L)
        ));
        when(subjectMapper.selectById(10L)).thenReturn(buildSubject(10L, DIRECTION_DEBIT, 0));
        when(subjectMapper.updateBalanceWithOptimisticLock(10L, -1000L, 0)).thenReturn(0); // 冲突

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.unpost(1L));
        assertTrue(ex.getMessage().contains("乐观锁") || ex.getMessage().contains("冲突")
                || ex.getMessage().contains("余额"));
    }

    @Test
    @DisplayName("unpost: 明细为空时抛 BusinessException")
    void unpost_emptyDetails_throwsBusinessException() {
        FinanceVoucher voucher = buildPostedVoucher(1L);
        when(voucherMapper.selectById(1L)).thenReturn(voucher);
        when(detailMapper.selectByVoucherId(1L)).thenReturn(Collections.emptyList());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.unpost(1L));
        assertTrue(ex.getMessage().contains("明细") || ex.getMessage().contains("分录"));
    }
}
