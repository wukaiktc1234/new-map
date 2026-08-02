package com.foodtraceability.service.finance.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.dto.finance.FinanceVoucherVO;
import com.foodtraceability.dto.finance.FundFlowVO;
import com.foodtraceability.dto.finance.PaymentCreateDTO;
import com.foodtraceability.dto.finance.PaymentVO;
import com.foodtraceability.entity.finance.AccountingSubject;
import com.foodtraceability.entity.finance.BankAccount;
import com.foodtraceability.entity.finance.Payment;
import com.foodtraceability.entity.finance.Payable;
import com.foodtraceability.exception.BusinessException;
import com.foodtraceability.mapper.finance.AccountingSubjectMapper;
import com.foodtraceability.mapper.finance.BankAccountMapper;
import com.foodtraceability.mapper.finance.PaymentMapper;
import com.foodtraceability.mapper.finance.PayableMapper;
import com.foodtraceability.service.finance.FundFlowService;
import com.foodtraceability.service.finance.VoucherService;
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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PaymentServiceImpl 单元测试
 *
 * <p>覆盖：付款单作废四账联动回滚、已确认付款单禁止修改等核心逻辑。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PaymentServiceImpl 单元测试")
class PaymentServiceImplTest {

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private PayableMapper payableMapper;

    @Mock
    private BankAccountMapper bankAccountMapper;

    @Mock
    private AccountingSubjectMapper subjectMapper;

    @Mock
    private FundFlowService fundFlowService;

    @Mock
    private VoucherService voucherService;

    @InjectMocks
    private PaymentServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", paymentMapper);
    }

    /**
     * 构造已确认付款单
     */
    private Payment buildConfirmedPayment(Long paymentId, Long amount) {
        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setPaymentNo("FK202607190001");
        payment.setPayableId(8001L);
        payment.setPaymentAmount(amount);
        payment.setPaymentMethod("bank_transfer");
        payment.setBankAccountId(2001L);
        payment.setPaymentDate(LocalDate.of(2026, 7, 19));
        payment.setStatus(1);
        payment.setFundFlowId(3001L);
        payment.setVoucherId(4001L);
        return payment;
    }

    /**
     * 构造应付账款
     */
    private Payable buildPayable(Long originalAmount, Long paidAmount, Long balanceAmount, Integer status) {
        Payable payable = new Payable();
        payable.setPayableId(8001L);
        payable.setPayableNo("AP202607190001");
        payable.setSupplierName("测试供应商");
        payable.setOriginalAmount(originalAmount);
        payable.setPaidAmount(paidAmount);
        payable.setBalanceAmount(balanceAmount);
        payable.setStatus(status);
        return payable;
    }

    /**
     * 构造银行账户
     */
    private BankAccount buildBankAccount(Long balance) {
        BankAccount account = new BankAccount();
        account.setAccountId(2001L);
        account.setAccountName("测试账户");
        account.setBalance(balance);
        account.setStatus(1);
        return account;
    }

    /**
     * 构造会计科目
     */
    private AccountingSubject buildSubject(Long subjectId, String subjectCode) {
        AccountingSubject subject = new AccountingSubject();
        subject.setSubjectId(subjectId);
        subject.setSubjectCode(subjectCode);
        subject.setSubjectName("测试科目");
        return subject;
    }

    // ============================================================
    // 1. voidPayment：四账联动回滚成功
    // ============================================================
    @Test
    @DisplayName("作废已确认付款单：四账联动回滚成功")
    void voidPayment_success() {
        // given
        Long paymentId = 1001L;
        Long amount = 50000L;
        Payment payment = buildConfirmedPayment(paymentId, amount);
        Payable payable = buildPayable(100000L, 50000L, 50000L, 2);
        BankAccount account = buildBankAccount(200000L);

        when(paymentMapper.selectById(paymentId)).thenReturn(payment);
        when(payableMapper.selectById(8001L)).thenReturn(payable);
        when(bankAccountMapper.selectById(2001L)).thenReturn(account);
        when(paymentMapper.updateById(any(Payment.class))).thenReturn(1);
        when(payableMapper.updateById(any(Payable.class))).thenReturn(1);
        when(bankAccountMapper.updateById(any(BankAccount.class))).thenReturn(1);
        when(subjectMapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(buildSubject(10L, "1002"))
            .thenReturn(buildSubject(20L, "2202"));

        FinanceVoucherVO voucherVO = new FinanceVoucherVO();
        voucherVO.setVoucherId(4002L);
        voucherVO.setVoucherNo("PZ202607190002");
        when(voucherService.create(any())).thenReturn(voucherVO);

        FundFlowVO flowVO = new FundFlowVO();
        flowVO.setFlowId(3002L);
        flowVO.setFlowNo("LS202607190002");
        when(fundFlowService.create(any())).thenReturn(flowVO);

        // when
        PaymentVO vo = service.voidPayment(paymentId, "测试作废");

        // then
        assertAll("作废后VO校验",
            () -> assertNotNull(vo),
            () -> assertEquals(2, vo.getStatus()),
            () -> assertEquals("测试作废", vo.getVoidRemark()),
            () -> assertNotNull(vo.getVoidTime()),
            () -> assertEquals(4002L, vo.getVoidVoucherId()),
            () -> assertEquals("PZ202607190002", vo.getVoidVoucherNo()),
            () -> assertEquals(3002L, vo.getVoidFundFlowId()),
            () -> assertEquals("LS202607190002", vo.getVoidFundFlowNo())
        );

        ArgumentCaptor<Payable> payableCaptor = ArgumentCaptor.forClass(Payable.class);
        verify(payableMapper, times(1)).updateById(payableCaptor.capture());
        Payable updatedPayable = payableCaptor.getValue();
        assertAll("应付账款回滚校验",
            () -> assertEquals(0L, updatedPayable.getPaidAmount()),
            () -> assertEquals(100000L, updatedPayable.getBalanceAmount()),
            () -> assertEquals(1, updatedPayable.getStatus())
        );

        ArgumentCaptor<BankAccount> accountCaptor = ArgumentCaptor.forClass(BankAccount.class);
        verify(bankAccountMapper, times(1)).updateById(accountCaptor.capture());
        assertEquals(250000L, accountCaptor.getValue().getBalance());

        ArgumentCaptor<com.foodtraceability.dto.finance.FundFlowCreateDTO> flowCaptor =
            ArgumentCaptor.forClass(com.foodtraceability.dto.finance.FundFlowCreateDTO.class);
        verify(fundFlowService, times(1)).create(flowCaptor.capture());
        assertAll("反向资金流水校验",
            () -> assertEquals(1, flowCaptor.getValue().getFlowDirection()),
            () -> assertEquals(7, flowCaptor.getValue().getFlowCategory()),
            () -> assertEquals(amount, flowCaptor.getValue().getAmount())
        );

        verify(paymentMapper, times(2)).updateById(any(Payment.class));
    }

    // ============================================================
    // 2. voidPayment：付款单不存在
    // ============================================================
    @Test
    @DisplayName("作废不存在的付款单应抛业务异常")
    void voidPayment_paymentNotFound_throws() {
        when(paymentMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> service.voidPayment(999L, "备注"));
        assertTrue(ex.getMessage().contains("付款单不存在"));
        verify(payableMapper, never()).selectById(any());
    }

    // ============================================================
    // 3. voidPayment：已作废的付款单不能再次作废
    // ============================================================
    @Test
    @DisplayName("已作废的付款单再次作废应抛业务异常")
    void voidPayment_alreadyVoided_throws() {
        Payment payment = buildConfirmedPayment(1001L, 50000L);
        payment.setStatus(2);
        when(paymentMapper.selectById(1001L)).thenReturn(payment);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> service.voidPayment(1001L, "备注"));
        assertTrue(ex.getMessage().contains("仅已确认的付款单可作废"));
        verify(payableMapper, never()).selectById(any());
    }

    // ============================================================
    // 4. voidPayment：关联应付账款不存在
    // ============================================================
    @Test
    @DisplayName("作废时关联应付账款不存在应抛业务异常")
    void voidPayment_payableNotFound_throws() {
        Payment payment = buildConfirmedPayment(1001L, 50000L);
        when(paymentMapper.selectById(1001L)).thenReturn(payment);
        when(payableMapper.selectById(8001L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> service.voidPayment(1001L, "备注"));
        assertTrue(ex.getMessage().contains("关联应付账款不存在"));
        verify(bankAccountMapper, never()).selectById(any());
    }

    // ============================================================
    // 5. voidPayment：关联银行账户不存在
    // ============================================================
    @Test
    @DisplayName("作废时付款银行账户不存在应抛业务异常")
    void voidPayment_bankAccountNotFound_throws() {
        Payment payment = buildConfirmedPayment(1001L, 50000L);
        Payable payable = buildPayable(100000L, 50000L, 50000L, 2);
        when(paymentMapper.selectById(1001L)).thenReturn(payment);
        when(payableMapper.selectById(8001L)).thenReturn(payable);
        when(bankAccountMapper.selectById(2001L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> service.voidPayment(1001L, "备注"));
        assertTrue(ex.getMessage().contains("付款银行账户不存在"));
        verify(paymentMapper, never()).updateById(any());
    }

    // ============================================================
    // 6. voidPayment：部分支付回滚后状态仍为部分支付
    // ============================================================
    @Test
    @DisplayName("部分作废回滚后应付账款状态仍为部分支付")
    void voidPayment_partialPayment_statusRemainsPartial() {
        Long paymentId = 1001L;
        Long amount = 30000L;
        Payment payment = buildConfirmedPayment(paymentId, amount);
        // 原金额 100000，已付 80000，余额 20000；回滚后已付 50000，余额 50000，仍为部分支付
        Payable payable = buildPayable(100000L, 80000L, 20000L, 2);
        BankAccount account = buildBankAccount(200000L);

        when(paymentMapper.selectById(paymentId)).thenReturn(payment);
        when(payableMapper.selectById(8001L)).thenReturn(payable);
        when(bankAccountMapper.selectById(2001L)).thenReturn(account);
        when(paymentMapper.updateById(any(Payment.class))).thenReturn(1);
        when(payableMapper.updateById(any(Payable.class))).thenReturn(1);
        when(bankAccountMapper.updateById(any(BankAccount.class))).thenReturn(1);
        when(subjectMapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(buildSubject(10L, "1002"))
            .thenReturn(buildSubject(20L, "2202"));
        when(voucherService.create(any())).thenReturn(new FinanceVoucherVO() {{ setVoucherId(4002L); setVoucherNo("PZ002"); }});
        when(fundFlowService.create(any())).thenReturn(new FundFlowVO() {{ setFlowId(3002L); setFlowNo("LS002"); }});

        service.voidPayment(paymentId, "部分作废");

        ArgumentCaptor<Payable> captor = ArgumentCaptor.forClass(Payable.class);
        verify(payableMapper).updateById(captor.capture());
        Payable updated = captor.getValue();
        assertAll("部分支付回滚校验",
            () -> assertEquals(50000L, updated.getPaidAmount()),
            () -> assertEquals(50000L, updated.getBalanceAmount()),
            () -> assertEquals(2, updated.getStatus())
        );
    }

    // ============================================================
    // 7. update：已确认付款单禁止修改
    // ============================================================
    @Test
    @DisplayName("修改已确认的付款单应抛业务异常")
    void update_confirmedPayment_notEditable_throws() {
        Payment payment = buildConfirmedPayment(1001L, 50000L);
        when(paymentMapper.selectById(1001L)).thenReturn(payment);

        PaymentCreateDTO dto = new PaymentCreateDTO();
        dto.setPaymentAmount(60000L);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> service.update(1001L, dto));
        assertTrue(ex.getMessage().contains("已确认的付款单不可修改"));
        verify(paymentMapper, never()).updateById(any());
    }

    // ============================================================
    // 8. update：未确认付款单可修改
    // ============================================================
    @Test
    @DisplayName("修改未确认的付款单应成功")
    void update_unconfirmedPayment_success() {
        Payment payment = buildConfirmedPayment(1001L, 50000L);
        payment.setStatus(0);
        when(paymentMapper.selectById(1001L)).thenReturn(payment);
        when(paymentMapper.updateById(any(Payment.class))).thenReturn(1);
        when(paymentMapper.selectById(1001L)).thenReturn(payment);

        PaymentCreateDTO dto = new PaymentCreateDTO();
        dto.setPaymentAmount(60000L);
        dto.setPaymentMethod("cash");
        dto.setBankAccountId(2002L);
        dto.setPaymentDate(LocalDate.of(2026, 7, 20));
        dto.setRemark("更新备注");

        PaymentVO vo = service.update(1001L, dto);

        assertAll("更新后VO校验",
            () -> assertNotNull(vo),
            () -> assertEquals(60000L, payment.getPaymentAmount()),
            () -> assertEquals("cash", payment.getPaymentMethod()),
            () -> assertEquals(2002L, payment.getBankAccountId()),
            () -> assertEquals("更新备注", payment.getRemark())
        );
    }
}
