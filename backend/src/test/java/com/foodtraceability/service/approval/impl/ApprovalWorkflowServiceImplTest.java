package com.foodtraceability.service.approval.impl;

import com.foodtraceability.dto.approval.ApprovalActionDTO;
import com.foodtraceability.entity.approval.ApprovalAuditLog;
import com.foodtraceability.mapper.approval.ApprovalAuditLogMapper;
import com.foodtraceability.mapper.approval.ApprovalWorkflowMapper;
import com.foodtraceability.service.approval.handler.BusinessApprovalHandler;
import com.foodtraceability.service.approval.strategy.ApprovalStrategyFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * 审批工作流服务单元测试
 * 验证审批通过/驳回时能正确路由到业务审批处理器
 */
class ApprovalWorkflowServiceImplTest {

    private ApprovalWorkflowMapper approvalWorkflowMapper;
    private ApprovalAuditLogMapper approvalAuditLogMapper;
    private BusinessApprovalHandler purchaseOrderHandler;
    private ApprovalStrategyFactory approvalStrategyFactory;
    private ApprovalWorkflowServiceImpl approvalWorkflowService;

    @BeforeEach
    void setUp() {
        approvalWorkflowMapper = mock(ApprovalWorkflowMapper.class);
        approvalAuditLogMapper = mock(ApprovalAuditLogMapper.class);
        purchaseOrderHandler = mock(BusinessApprovalHandler.class);
        approvalStrategyFactory = mock(ApprovalStrategyFactory.class);

        when(purchaseOrderHandler.getBusinessType()).thenReturn("purchase_order");

        approvalWorkflowService = new ApprovalWorkflowServiceImpl(
                approvalWorkflowMapper,
                approvalAuditLogMapper,
                Collections.singletonList(purchaseOrderHandler),
                approvalStrategyFactory
        );
    }

    /**
     * 测试审批通过时触发采购订单处理器
     */
    @Test
    void approveShouldTriggerPurchaseOrderHandler() {
        ApprovalActionDTO actionDTO = new ApprovalActionDTO();
        actionDTO.setBusinessType("purchase_order");
        actionDTO.setBusinessId("12345");
        actionDTO.setComment("同意采购");

        approvalWorkflowService.approve(actionDTO);

        // 验证写入了审批记录
        ArgumentCaptor<ApprovalAuditLog> logCaptor = ArgumentCaptor.forClass(ApprovalAuditLog.class);
        verify(approvalAuditLogMapper, times(1)).insert(logCaptor.capture());
        ApprovalAuditLog savedLog = logCaptor.getValue();
        assertEquals("purchase_order", savedLog.getBusinessType());
        assertEquals("12345", savedLog.getBusinessId());
        assertEquals(ApprovalAuditLog.ACTION_APPROVE, savedLog.getAction());

        // 验证触发了采购订单业务处理器
        verify(purchaseOrderHandler, times(1))
                .onApproved("12345", "同意采购", null);
    }

    /**
     * 测试审批驳回时触发采购订单处理器
     */
    @Test
    void rejectShouldTriggerPurchaseOrderHandler() {
        ApprovalActionDTO actionDTO = new ApprovalActionDTO();
        actionDTO.setBusinessType("purchase_order");
        actionDTO.setBusinessId("12345");
        actionDTO.setComment("价格过高");

        approvalWorkflowService.reject(actionDTO);

        // 验证写入了审批记录
        ArgumentCaptor<ApprovalAuditLog> logCaptor = ArgumentCaptor.forClass(ApprovalAuditLog.class);
        verify(approvalAuditLogMapper, times(1)).insert(logCaptor.capture());
        ApprovalAuditLog savedLog = logCaptor.getValue();
        assertEquals("purchase_order", savedLog.getBusinessType());
        assertEquals(ApprovalAuditLog.ACTION_REJECT, savedLog.getAction());

        // 验证触发了采购订单业务处理器
        verify(purchaseOrderHandler, times(1))
                .onRejected("12345", "价格过高", null);
    }

    /**
     * 测试提交审批时写入提交记录
     */
    @Test
    void submitApprovalShouldInsertSubmitLog() {
        approvalWorkflowService.submitApproval("purchase_order", "12345");

        ArgumentCaptor<ApprovalAuditLog> logCaptor = ArgumentCaptor.forClass(ApprovalAuditLog.class);
        verify(approvalAuditLogMapper, times(1)).insert(logCaptor.capture());
        ApprovalAuditLog savedLog = logCaptor.getValue();
        assertNotNull(savedLog);
        assertEquals("purchase_order", savedLog.getBusinessType());
        assertEquals("12345", savedLog.getBusinessId());
        assertEquals(ApprovalAuditLog.ACTION_SUBMIT, savedLog.getAction());
    }
}
