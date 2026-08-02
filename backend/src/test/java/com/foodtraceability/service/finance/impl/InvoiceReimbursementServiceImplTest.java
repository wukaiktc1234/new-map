package com.foodtraceability.service.finance.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.finance.InvoiceReimbursementApproveDTO;
import com.foodtraceability.dto.finance.InvoiceReimbursementPayDTO;
import com.foodtraceability.dto.finance.InvoiceReimbursementUpdateDTO;
import com.foodtraceability.dto.finance.InvoiceReimbursementVO;
import com.foodtraceability.dto.finance.ReimbursementCreateDTO;
import com.foodtraceability.dto.finance.ReimbursementItemDTO;
import com.foodtraceability.dto.finance.ReimbursementQueryDTO;
import com.foodtraceability.entity.finance.InvoiceReimbursement;
import com.foodtraceability.entity.finance.InvoiceReimbursementApprovalRecord;
import com.foodtraceability.entity.finance.InvoiceReimbursementItem;
import com.foodtraceability.exception.BusinessException;
import com.foodtraceability.mapper.finance.InvoiceReimbursementApprovalRecordMapper;
import com.foodtraceability.mapper.finance.InvoiceReimbursementItemMapper;
import com.foodtraceability.mapper.finance.InvoiceReimbursementMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * InvoiceReimbursementServiceImpl 单元测试
 *
 * <p>Sprint 3.1 P0 T-015 (TDD)：覆盖 create / approve / pay / cancel 主流程 + 边界条件
 * （金额为 0 / 状态非法流转 / 报销单不存在 / 报销单号生成）。</p>
 *
 * <p>状态机（5 状态）：草稿(0) → 已审批(1) → 已付款(2)；
 * 草稿(0) → 已拒绝(4)；草稿(0)/已审批(1) → 已取消(3)；
 * 已取消(3)/已拒绝(4) 为终态。</p>
 *
 * <p>测试类与实现类同包（impl），以便访问 protected generateReimbursementNo() 方法。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("InvoiceReimbursementServiceImpl 单元测试")
class InvoiceReimbursementServiceImplTest {

    @Mock
    private InvoiceReimbursementMapper reimbursementMapper;

    @Mock
    private InvoiceReimbursementItemMapper itemMapper;

    @Mock
    private InvoiceReimbursementApprovalRecordMapper approvalRecordMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Spy
    @InjectMocks
    private InvoiceReimbursementServiceImpl service;

    /** 公共：构造一个最小可用的创建DTO */
    private ReimbursementCreateDTO buildCreateDTO(Long totalAmount) {
        ReimbursementCreateDTO dto = new ReimbursementCreateDTO();
        dto.setApplicantId(1001L);
        dto.setApplicantName("张三");
        dto.setDepartmentId(2001L);
        dto.setDepartmentName("财务部");
        dto.setReimbursementType("差旅费");
        dto.setTotalAmount(totalAmount);
        dto.setApplyDate(LocalDate.of(2026, 6, 26));
        dto.setRemark("出差北京");

        ReimbursementItemDTO item = new ReimbursementItemDTO();
        item.setAmount(totalAmount);
        item.setExpenseType("差旅费");
        item.setExpenseDescription("北京出差机票");
        dto.setItems(Collections.singletonList(item));
        return dto;
    }

    /** 公共：stub 报销单号生成方法返回固定值 */
    @BeforeEach
    void stubReimbursementNo() {
        lenient().doReturn("RE202606260001").when(service).generateReimbursementNo();
    }

    // ============================================================
    // 1. create() 主流程 + 边界
    // ============================================================
    @Nested
    @DisplayName("create() 创建报销申请")
    class CreateTest {

        @Test
        @DisplayName("主流程：创建成功，status=0(草稿)，写入items和submit审批记录")
        void create_success() {
            // given
            ReimbursementCreateDTO dto = buildCreateDTO(100000L); // 1000元

            // 模拟 insert 回填 ID，并 stub getById 调用
            final Long generatedId = 8888L;
            doAnswer(invocation -> {
                InvoiceReimbursement entity = invocation.getArgument(0);
                entity.setReimbursementId(generatedId);
                return 1;
            }).when(reimbursementMapper).insert(any(InvoiceReimbursement.class));

            when(reimbursementMapper.selectById(generatedId)).thenAnswer(invocation -> {
                InvoiceReimbursement e = new InvoiceReimbursement();
                e.setReimbursementId(generatedId);
                e.setReimbursementNo("RE202606260001");
                e.setStatus(0);
                e.setPaymentStatus(0);
                e.setTotalAmount(100000L);
                e.setApplicantName("张三");
                e.setDepartmentName("财务部");
                e.setReimbursementType("差旅费");
                return e;
            });
            when(itemMapper.selectList(any())).thenReturn(Collections.emptyList());
            when(approvalRecordMapper.selectList(any())).thenReturn(Collections.emptyList());

            // when
            InvoiceReimbursementVO result = service.create(dto);

            // then
            assertNotNull(result);
            assertAll("创建结果验证",
                    () -> assertEquals("RE202606260001", result.getReimbursementNo()),
                    () -> assertEquals(0, result.getStatus(), "初始状态应为草稿(0)"),
                    () -> assertEquals(0, result.getPaymentStatus(), "初始付款状态应为未付款(0)"),
                    () -> assertEquals(100000L, result.getTotalAmount()),
                    () -> assertEquals("张三", result.getApplicantName()),
                    () -> assertEquals("财务部", result.getDepartmentName()),
                    () -> assertEquals("差旅费", result.getReimbursementType())
            );

            // 验证写入主表
            ArgumentCaptor<InvoiceReimbursement> mainCaptor = ArgumentCaptor.forClass(InvoiceReimbursement.class);
            verify(reimbursementMapper).insert(mainCaptor.capture());
            InvoiceReimbursement saved = mainCaptor.getValue();
            assertEquals("RE202606260001", saved.getReimbursementNo());
            assertEquals(0, saved.getStatus());

            // 验证写入明细
            ArgumentCaptor<InvoiceReimbursementItem> itemCaptor = ArgumentCaptor.forClass(InvoiceReimbursementItem.class);
            verify(itemMapper, times(1)).insert(itemCaptor.capture());
            assertEquals(100000L, itemCaptor.getValue().getAmount());

            // 验证写入审批记录（submit 动作）
            ArgumentCaptor<InvoiceReimbursementApprovalRecord> recordCaptor =
                    ArgumentCaptor.forClass(InvoiceReimbursementApprovalRecord.class);
            verify(approvalRecordMapper, times(1)).insert(recordCaptor.capture());
            assertEquals("submit", recordCaptor.getValue().getAction());
        }

        @Test
        @DisplayName("边界：金额为0时应抛BusinessException")
        void create_zeroAmount_throwsException() {
            ReimbursementCreateDTO dto = buildCreateDTO(0L);
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.create(dto));
            assertTrue(ex.getMessage().contains("金额") || ex.getMessage().contains("amount"),
                    "异常信息应提示金额错误");
            verify(reimbursementMapper, never()).insert(any());
        }

        @Test
        @DisplayName("边界：金额为null时应抛BusinessException")
        void create_nullAmount_throwsException() {
            ReimbursementCreateDTO dto = buildCreateDTO(100000L);
            dto.setTotalAmount(null);
            assertThrows(BusinessException.class, () -> service.create(dto));
            verify(reimbursementMapper, never()).insert(any());
        }

        @Test
        @DisplayName("边界：明细列表为空时应抛BusinessException")
        void create_emptyItems_throwsException() {
            ReimbursementCreateDTO dto = buildCreateDTO(100000L);
            dto.setItems(Collections.emptyList());
            assertThrows(BusinessException.class, () -> service.create(dto));
            verify(reimbursementMapper, never()).insert(any());
        }
    }

    // ============================================================
    // 2. approve() 主流程 + 边界
    // ============================================================
    @Nested
    @DisplayName("approve() 审批报销申请")
    class ApproveTest {

        @Test
        @DisplayName("主流程：审批通过，status 0→1，设置approvedAmount，写入approve审批记录")
        void approve_pass_success() {
            // given
            Long reimbursementId = 9001L;
            InvoiceReimbursement existing = new InvoiceReimbursement();
            existing.setReimbursementId(reimbursementId);
            existing.setReimbursementNo("RE202606260001");
            existing.setStatus(0); // 草稿
            existing.setTotalAmount(100000L);
            existing.setApplicantId(1001L);
            existing.setApplicantName("张三");
            existing.setDepartmentId(2001L);
            existing.setDepartmentName("财务部");
            existing.setReimbursementType("差旅费");
            when(reimbursementMapper.selectById(reimbursementId)).thenReturn(existing);
            when(itemMapper.selectList(any())).thenReturn(Collections.emptyList());
            when(approvalRecordMapper.selectList(any())).thenReturn(Collections.emptyList());

            InvoiceReimbursementApproveDTO dto = new InvoiceReimbursementApproveDTO();
            dto.setApproved(true);
            dto.setApprovedAmount(90000L); // 审批通过金额 900元
            dto.setRemark("同意");

            // when
            InvoiceReimbursementVO result = service.approve(reimbursementId, 8001L, "李审批", dto);

            // then
            assertNotNull(result);
            assertEquals(1, result.getStatus(), "状态应为已审批(1)");
            assertEquals(90000L, result.getApprovedAmount());
            assertEquals("李审批", result.getApproverName());
            assertNotNull(result.getApproveDate());

            // 验证更新主表
            ArgumentCaptor<InvoiceReimbursement> mainCaptor = ArgumentCaptor.forClass(InvoiceReimbursement.class);
            verify(reimbursementMapper).updateById(mainCaptor.capture());
            assertEquals(1, mainCaptor.getValue().getStatus());
            assertEquals(8001L, mainCaptor.getValue().getApproverId());

            // 验证写入 approve 审批记录
            ArgumentCaptor<InvoiceReimbursementApprovalRecord> recordCaptor =
                    ArgumentCaptor.forClass(InvoiceReimbursementApprovalRecord.class);
            verify(approvalRecordMapper).insert(recordCaptor.capture());
            assertEquals("approve", recordCaptor.getValue().getAction());

            // 验证发布 InvoiceReimbursementApprovedEvent（T-042）
            ArgumentCaptor<com.foodtraceability.event.InvoiceReimbursementApprovedEvent> eventCaptor =
                    ArgumentCaptor.forClass(com.foodtraceability.event.InvoiceReimbursementApprovedEvent.class);
            verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
            com.foodtraceability.event.InvoiceReimbursementApprovedEvent published = eventCaptor.getValue();
            assertAll("审批通过事件验证",
                    () -> assertEquals(reimbursementId, published.getReimbursementId()),
                    () -> assertEquals("RE202606260001", published.getReimbursementNo()),
                    () -> assertEquals(1001L, published.getApplicantId()),
                    () -> assertEquals(2001L, published.getDepartmentId()),
                    () -> assertEquals("财务部", published.getDepartmentName()),
                    () -> assertEquals("差旅费", published.getReimbursementType()),
                    () -> assertEquals(90000L, published.getApprovedAmount()),
                    () -> assertEquals(8001L, published.getApproverId()),
                    () -> assertNotNull(published.getApproveTime())
            );
        }

        @Test
        @DisplayName("T-042 边界：事件发布抛异常时不影响审批主流程（try-catch 隔离）")
        void approve_pass_eventPublishException_doesNotAffectMainFlow() {
            // given
            Long reimbursementId = 9005L;
            InvoiceReimbursement existing = new InvoiceReimbursement();
            existing.setReimbursementId(reimbursementId);
            existing.setReimbursementNo("RE202606260002");
            existing.setStatus(0);
            existing.setTotalAmount(100000L);
            existing.setApplicantId(1001L);
            existing.setDepartmentId(2001L);
            existing.setReimbursementType("办公费");
            when(reimbursementMapper.selectById(reimbursementId)).thenReturn(existing);
            when(itemMapper.selectList(any())).thenReturn(Collections.emptyList());
            when(approvalRecordMapper.selectList(any())).thenReturn(Collections.emptyList());

            // 模拟事件发布抛异常
            org.mockito.Mockito.doThrow(new RuntimeException("事件总线故障"))
                    .when(eventPublisher).publishEvent(any());

            InvoiceReimbursementApproveDTO dto = new InvoiceReimbursementApproveDTO();
            dto.setApproved(true);
            dto.setApprovedAmount(50000L);
            dto.setRemark("同意");

            // when & then：审批主流程应正常完成，不抛异常
            InvoiceReimbursementVO result = assertDoesNotThrow(
                    () -> service.approve(reimbursementId, 8001L, "李审批", dto));
            assertEquals(1, result.getStatus(), "审批应成功，状态为已审批(1)");

            // 验证主表更新与审批记录写入均已完成
            verify(reimbursementMapper, times(1)).updateById(any());
            verify(approvalRecordMapper, times(1)).insert(any());
        }

        @Test
        @DisplayName("主流程：审批拒绝，status 0→4，remark作为拒绝原因（已拒绝为终态）")
        void approve_reject_success() {
            Long reimbursementId = 9002L;
            InvoiceReimbursement existing = new InvoiceReimbursement();
            existing.setReimbursementId(reimbursementId);
            existing.setStatus(0);
            existing.setTotalAmount(100000L);
            when(reimbursementMapper.selectById(reimbursementId)).thenReturn(existing);
            when(itemMapper.selectList(any())).thenReturn(Collections.emptyList());
            when(approvalRecordMapper.selectList(any())).thenReturn(Collections.emptyList());

            InvoiceReimbursementApproveDTO dto = new InvoiceReimbursementApproveDTO();
            dto.setApproved(false);
            dto.setRemark("金额超预算");

            InvoiceReimbursementVO result = service.approve(reimbursementId, 8001L, "李审批", dto);

            assertEquals(4, result.getStatus(), "拒绝后状态应为已拒绝(4)");
            assertEquals("金额超预算", result.getRejectReason());

            ArgumentCaptor<InvoiceReimbursementApprovalRecord> recordCaptor =
                    ArgumentCaptor.forClass(InvoiceReimbursementApprovalRecord.class);
            verify(approvalRecordMapper).insert(recordCaptor.capture());
            assertEquals("reject", recordCaptor.getValue().getAction());
        }

        @Test
        @DisplayName("边界：审批已审批的报销单应抛BusinessException")
        void approve_alreadyApproved_throwsException() {
            Long reimbursementId = 9003L;
            InvoiceReimbursement existing = new InvoiceReimbursement();
            existing.setReimbursementId(reimbursementId);
            existing.setStatus(1); // 已审批
            when(reimbursementMapper.selectById(reimbursementId)).thenReturn(existing);

            InvoiceReimbursementApproveDTO dto = new InvoiceReimbursementApproveDTO();
            dto.setApproved(true);
            dto.setApprovedAmount(100000L);

            assertThrows(BusinessException.class,
                    () -> service.approve(reimbursementId, 8001L, "李审批", dto));
            verify(reimbursementMapper, never()).updateById(any());
        }

        @Test
        @DisplayName("边界：审批通过的报销单approvedAmount为null或<=0应抛BusinessException")
        void approve_pass_invalidAmount_throwsException() {
            Long reimbursementId = 9004L;
            InvoiceReimbursement existing = new InvoiceReimbursement();
            existing.setReimbursementId(reimbursementId);
            existing.setStatus(0);
            when(reimbursementMapper.selectById(reimbursementId)).thenReturn(existing);

            InvoiceReimbursementApproveDTO dto = new InvoiceReimbursementApproveDTO();
            dto.setApproved(true);
            dto.setApprovedAmount(0L); // 无效金额

            assertThrows(BusinessException.class,
                    () -> service.approve(reimbursementId, 8001L, "李审批", dto));
        }

        @Test
        @DisplayName("边界：报销单不存在应抛BusinessException")
        void approve_notFound_throwsException() {
            Long reimbursementId = 9999L;
            when(reimbursementMapper.selectById(reimbursementId)).thenReturn(null);

            InvoiceReimbursementApproveDTO dto = new InvoiceReimbursementApproveDTO();
            dto.setApproved(true);
            dto.setApprovedAmount(100000L);

            assertThrows(BusinessException.class,
                    () -> service.approve(reimbursementId, 8001L, "李审批", dto));
        }
    }

    // ============================================================
    // 3. pay() 主流程 + 边界
    // ============================================================
    @Nested
    @DisplayName("pay() 标记已付款")
    class PayTest {

        @Test
        @DisplayName("主流程：付款成功，status 1→2，paymentStatus 0→1，写入pay审批记录")
        void pay_success() {
            Long reimbursementId = 9101L;
            InvoiceReimbursement existing = new InvoiceReimbursement();
            existing.setReimbursementId(reimbursementId);
            existing.setStatus(1); // 已审批
            existing.setPaymentStatus(0); // 未付款
            existing.setApprovedAmount(90000L);
            when(reimbursementMapper.selectById(reimbursementId)).thenReturn(existing);
            when(itemMapper.selectList(any())).thenReturn(Collections.emptyList());
            when(approvalRecordMapper.selectList(any())).thenReturn(Collections.emptyList());

            InvoiceReimbursementPayDTO dto = new InvoiceReimbursementPayDTO();
            dto.setPaymentVoucherNo("PAY20260626001");
            dto.setPaymentDate(LocalDate.of(2026, 6, 26));

            InvoiceReimbursementVO result = service.pay(reimbursementId, dto);

            assertEquals(2, result.getStatus(), "状态应为已付款(2)");
            assertEquals(1, result.getPaymentStatus(), "付款状态应为已付款(1)");
            assertEquals("PAY20260626001", result.getPaymentVoucherNo());
            assertEquals(LocalDate.of(2026, 6, 26), result.getPaymentDate());

            ArgumentCaptor<InvoiceReimbursementApprovalRecord> recordCaptor =
                    ArgumentCaptor.forClass(InvoiceReimbursementApprovalRecord.class);
            verify(approvalRecordMapper).insert(recordCaptor.capture());
            assertEquals("pay", recordCaptor.getValue().getAction());
        }

        @Test
        @DisplayName("边界：付款未审批的报销单应抛BusinessException")
        void pay_notApproved_throwsException() {
            Long reimbursementId = 9102L;
            InvoiceReimbursement existing = new InvoiceReimbursement();
            existing.setReimbursementId(reimbursementId);
            existing.setStatus(0); // 草稿
            when(reimbursementMapper.selectById(reimbursementId)).thenReturn(existing);

            InvoiceReimbursementPayDTO dto = new InvoiceReimbursementPayDTO();
            dto.setPaymentVoucherNo("PAY20260626001");
            dto.setPaymentDate(LocalDate.of(2026, 6, 26));

            assertThrows(BusinessException.class, () -> service.pay(reimbursementId, dto));
            verify(reimbursementMapper, never()).updateById(any());
        }

        @Test
        @DisplayName("边界：付款已付款的报销单应抛BusinessException（幂等）")
        void pay_alreadyPaid_throwsException() {
            Long reimbursementId = 9103L;
            InvoiceReimbursement existing = new InvoiceReimbursement();
            existing.setReimbursementId(reimbursementId);
            existing.setStatus(2); // 已付款
            existing.setPaymentStatus(1);
            when(reimbursementMapper.selectById(reimbursementId)).thenReturn(existing);

            InvoiceReimbursementPayDTO dto = new InvoiceReimbursementPayDTO();
            dto.setPaymentVoucherNo("PAY20260626001");
            dto.setPaymentDate(LocalDate.of(2026, 6, 26));

            assertThrows(BusinessException.class, () -> service.pay(reimbursementId, dto));
        }
    }

    // ============================================================
    // 4. cancel() 主流程 + 边界
    // ============================================================
    @Nested
    @DisplayName("cancel() 取消报销申请")
    class CancelTest {

        @Test
        @DisplayName("主流程：取消成功，status 0→3，写入cancel审批记录")
        void cancel_success() {
            Long reimbursementId = 9201L;
            InvoiceReimbursement existing = new InvoiceReimbursement();
            existing.setReimbursementId(reimbursementId);
            existing.setStatus(0); // 草稿
            when(reimbursementMapper.selectById(reimbursementId)).thenReturn(existing);
            when(itemMapper.selectList(any())).thenReturn(Collections.emptyList());
            when(approvalRecordMapper.selectList(any())).thenReturn(Collections.emptyList());

            InvoiceReimbursementVO result = service.cancel(reimbursementId, "申请人主动取消");

            assertEquals(3, result.getStatus(), "状态应为已取消(3)");
            assertEquals("申请人主动取消", result.getRejectReason());

            ArgumentCaptor<InvoiceReimbursementApprovalRecord> recordCaptor =
                    ArgumentCaptor.forClass(InvoiceReimbursementApprovalRecord.class);
            verify(approvalRecordMapper).insert(recordCaptor.capture());
            assertEquals("cancel", recordCaptor.getValue().getAction());
        }

        @Test
        @DisplayName("主流程：已审批状态可取消，status 1→3（已审批→已取消合法流转）")
        void cancel_fromApproved_succeeds() {
            Long reimbursementId = 9202L;
            InvoiceReimbursement existing = new InvoiceReimbursement();
            existing.setReimbursementId(reimbursementId);
            existing.setStatus(1); // 已审批
            existing.setApplicantId(1001L);
            existing.setApplicantName("张三");
            when(reimbursementMapper.selectById(reimbursementId)).thenReturn(existing);
            when(itemMapper.selectList(any())).thenReturn(Collections.emptyList());
            when(approvalRecordMapper.selectList(any())).thenReturn(Collections.emptyList());

            InvoiceReimbursementVO result = service.cancel(reimbursementId, "审批后取消");

            assertEquals(3, result.getStatus(), "状态应为已取消(3)");
            assertEquals("审批后取消", result.getRejectReason());

            ArgumentCaptor<InvoiceReimbursementApprovalRecord> recordCaptor =
                    ArgumentCaptor.forClass(InvoiceReimbursementApprovalRecord.class);
            verify(approvalRecordMapper).insert(recordCaptor.capture());
            assertEquals("cancel", recordCaptor.getValue().getAction());
        }

        @Test
        @DisplayName("边界：取消已付款的报销单应抛BusinessException（终态）")
        void cancel_alreadyPaid_throwsException() {
            Long reimbursementId = 9203L;
            InvoiceReimbursement existing = new InvoiceReimbursement();
            existing.setReimbursementId(reimbursementId);
            existing.setStatus(2); // 已付款
            when(reimbursementMapper.selectById(reimbursementId)).thenReturn(existing);

            assertThrows(BusinessException.class,
                    () -> service.cancel(reimbursementId, "误取消"));
        }

        @Test
        @DisplayName("边界：取消已拒绝的报销单应抛BusinessException（终态）")
        void cancel_alreadyRejected_throwsException() {
            Long reimbursementId = 9204L;
            InvoiceReimbursement existing = new InvoiceReimbursement();
            existing.setReimbursementId(reimbursementId);
            existing.setStatus(4); // 已拒绝（终态）
            when(reimbursementMapper.selectById(reimbursementId)).thenReturn(existing);

            assertThrows(BusinessException.class,
                    () -> service.cancel(reimbursementId, "误取消"));
            verify(reimbursementMapper, never()).updateById(any());
        }
    }

    // ============================================================
    // 5. getById() + 报销单号生成
    // ============================================================
    @Nested
    @DisplayName("getById() 查询详情 + 报销单号生成")
    class GetByIdTest {

        @Test
        @DisplayName("主流程：查询成功，返回VO含明细和审批记录")
        void getById_success() {
            Long reimbursementId = 9301L;
            InvoiceReimbursement existing = new InvoiceReimbursement();
            existing.setReimbursementId(reimbursementId);
            existing.setReimbursementNo("RE202606260001");
            existing.setStatus(1);
            existing.setTotalAmount(100000L);
            existing.setApplicantName("张三");
            when(reimbursementMapper.selectById(reimbursementId)).thenReturn(existing);

            InvoiceReimbursementItem item = new InvoiceReimbursementItem();
            item.setItemId(1L);
            item.setReimbursementId(reimbursementId);
            item.setAmount(100000L);
            item.setExpenseType("差旅费");
            when(itemMapper.selectList(any())).thenReturn(Collections.singletonList(item));

            InvoiceReimbursementApprovalRecord record = new InvoiceReimbursementApprovalRecord();
            record.setRecordId(1L);
            record.setReimbursementId(reimbursementId);
            record.setAction("submit");
            when(approvalRecordMapper.selectList(any())).thenReturn(Collections.singletonList(record));

            InvoiceReimbursementVO result = service.getById(reimbursementId);

            assertNotNull(result);
            assertEquals(reimbursementId, result.getReimbursementId());
            assertEquals("RE202606260001", result.getReimbursementNo());
            assertEquals("已审批", result.getStatusName());
            assertNotNull(result.getItems());
            assertEquals(1, result.getItems().size());
            assertNotNull(result.getApprovalRecords());
            assertEquals(1, result.getApprovalRecords().size());
        }

        @Test
        @DisplayName("边界：查询不存在的报销单应抛BusinessException")
        void getById_notFound_throwsException() {
            Long reimbursementId = 9999L;
            when(reimbursementMapper.selectById(reimbursementId)).thenReturn(null);

            assertThrows(BusinessException.class, () -> service.getById(reimbursementId));
        }

        @Test
        @DisplayName("报销单号生成：格式为 RE + yyyyMMdd + 4位序号")
        void generateReimbursementNo_format() {
            // 覆盖 @BeforeEach 的 stub，调用真实方法
            org.mockito.Mockito.doCallRealMethod().when(service).generateReimbursementNo();
            // stub mapper.selectOne 返回 null（当天无记录）
            when(reimbursementMapper.selectOne(any(Wrapper.class), anyBoolean())).thenReturn(null);

            String no = service.generateReimbursementNo();

            assertNotNull(no);
            assertTrue(no.startsWith("RE"), "报销单号应以 RE 开头");
            assertEquals(14, no.length(), "报销单号长度应为 14（RE + 8位日期 + 4位序号）");
            assertTrue(no.endsWith("0001"), "首张报销单序号应为 0001");
        }
    }
}
