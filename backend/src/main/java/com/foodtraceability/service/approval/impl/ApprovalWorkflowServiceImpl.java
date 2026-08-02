package com.foodtraceability.service.approval.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.approval.ApprovalActionDTO;
import com.foodtraceability.dto.approval.ApprovalAuditLogVO;
import com.foodtraceability.dto.approval.ApprovalCurrentNodeVO;
import com.foodtraceability.dto.approval.ApprovalWorkflowCreateDTO;
import com.foodtraceability.dto.approval.ApprovalWorkflowQueryDTO;
import com.foodtraceability.dto.approval.ApprovalWorkflowVO;
import com.foodtraceability.entity.approval.ApprovalAuditLog;
import com.foodtraceability.entity.approval.ApprovalWorkflow;
import com.foodtraceability.mapper.approval.ApprovalAuditLogMapper;
import com.foodtraceability.mapper.approval.ApprovalWorkflowMapper;
import com.foodtraceability.security.model.SecurityUser;
import com.foodtraceability.service.approval.ApprovalWorkflowService;
import com.foodtraceability.service.approval.handler.BusinessApprovalHandler;
import com.foodtraceability.service.approval.strategy.ApprovalStrategyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 审批工作流服务实现类
 *
 * <p>JSON 字段处理：
 * <ul>
 *   <li>nodes / conditions 在 Entity 中以 String 存储 JSON 字符串</li>
 *   <li>VO/DTO 中以 List&lt;Map&lt;String, Object&gt;&gt; 结构化对象数组传输</li>
 *   <li>使用 Jackson ObjectMapper 在 Service 层做序列化/反序列化</li>
 * </ul>
 *
 * <p>审批操作记录：
 * <ul>
 *   <li>submitApproval → 写入 action=submit 的审批记录</li>
 *   <li>approve → 写入 action=approve 的审批记录</li>
 *   <li>reject → 写入 action=reject 的审批记录</li>
 *   <li>withdraw → 写入 action=withdraw 的审批记录</li>
 * </ul>
 */
@Service
public class ApprovalWorkflowServiceImpl implements ApprovalWorkflowService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalWorkflowServiceImpl.class);

    /** JSON 序列化/反序列化器 */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** List&lt;Map&gt; 类型引用（用于 JSON 反序列化） */
    private static final TypeReference<List<Map<String, Object>>> LIST_MAP_TYPE =
            new TypeReference<List<Map<String, Object>>>() {};

    private final ApprovalWorkflowMapper approvalWorkflowMapper;
    private final ApprovalAuditLogMapper approvalAuditLogMapper;
    /** 业务审批处理器映射：businessType -> handler，用于审批通过/驳回时联动业务状态 */
    private final Map<String, BusinessApprovalHandler> businessApprovalHandlers;
    /** 审批策略工厂：用于按策略类型解析审批人 */
    private final ApprovalStrategyFactory approvalStrategyFactory;

    /**
     * 构造函数注入
     * @param approvalWorkflowMapper 审批流程 Mapper
     * @param approvalAuditLogMapper 审批日志 Mapper
     * @param handlers 所有业务审批处理器实现（Spring 自动注入）
     * @param approvalStrategyFactory 审批策略工厂
     */
    public ApprovalWorkflowServiceImpl(ApprovalWorkflowMapper approvalWorkflowMapper,
                                       ApprovalAuditLogMapper approvalAuditLogMapper,
                                       List<BusinessApprovalHandler> handlers,
                                       ApprovalStrategyFactory approvalStrategyFactory) {
        this.approvalWorkflowMapper = approvalWorkflowMapper;
        this.approvalAuditLogMapper = approvalAuditLogMapper;
        this.businessApprovalHandlers = new HashMap<>();
        for (BusinessApprovalHandler handler : handlers) {
            businessApprovalHandlers.put(handler.getBusinessType(), handler);
            log.info("注册业务审批处理器: businessType={}, class={}",
                    handler.getBusinessType(), handler.getClass().getSimpleName());
        }
        this.approvalStrategyFactory = approvalStrategyFactory;
    }

    // ==================== 审批流程 CRUD ====================

    @Override
    public PageResult<ApprovalWorkflowVO> getWorkflowList(ApprovalWorkflowQueryDTO queryDTO) {
        int current = queryDTO.getPage() != null && queryDTO.getPage() > 0 ? queryDTO.getPage() : 1;
        int size = queryDTO.getSize() != null && queryDTO.getSize() > 0 ? queryDTO.getSize() : 10;

        Page<ApprovalWorkflow> page = new Page<>(current, size);
        LambdaQueryWrapper<ApprovalWorkflow> wrapper = new LambdaQueryWrapper<>();

        if (queryDTO.getBusinessType() != null && !queryDTO.getBusinessType().isEmpty()) {
            wrapper.eq(ApprovalWorkflow::getBusinessType, queryDTO.getBusinessType());
        }
        if (queryDTO.getTemplateCode() != null && !queryDTO.getTemplateCode().isEmpty()) {
            wrapper.eq(ApprovalWorkflow::getTemplateCode, queryDTO.getTemplateCode());
        }
        if (queryDTO.getEnabled() != null) {
            wrapper.eq(ApprovalWorkflow::getEnabled, queryDTO.getEnabled());
        }
        if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().isEmpty()) {
            wrapper.like(ApprovalWorkflow::getWorkflowName, queryDTO.getKeyword());
        }
        wrapper.orderByDesc(ApprovalWorkflow::getUpdateTime);

        IPage<ApprovalWorkflow> resultPage = approvalWorkflowMapper.selectPage(page, wrapper);

        List<ApprovalWorkflowVO> voList = resultPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(
                resultPage.getTotal(),
                voList,
                resultPage.getCurrent(),
                resultPage.getSize()
        );
    }

    @Override
    public ApprovalWorkflowVO getWorkflowById(String workflowId) {
        ApprovalWorkflow entity = approvalWorkflowMapper.selectById(workflowId);
        if (entity == null) {
            return null;
        }
        return convertToVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApprovalWorkflowVO createWorkflow(ApprovalWorkflowCreateDTO createDTO) {
        ApprovalWorkflow entity = new ApprovalWorkflow();
        entity.setWorkflowName(createDTO.getWorkflowName());
        entity.setBusinessType(createDTO.getBusinessType());
        entity.setTemplateCode(createDTO.getTemplateCode());
        entity.setEnabled(createDTO.getEnabled() != null ? createDTO.getEnabled() : Boolean.TRUE);
        entity.setNodes(serializeToJson(createDTO.getNodes()));
        entity.setConditions(serializeToJson(createDTO.getConditions()));

        approvalWorkflowMapper.insert(entity);
        log.info("创建审批流程成功: workflowId={}, name={}", entity.getWorkflowId(), entity.getWorkflowName());
        return convertToVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApprovalWorkflowVO updateWorkflow(String workflowId, ApprovalWorkflowCreateDTO updateDTO) {
        ApprovalWorkflow entity = approvalWorkflowMapper.selectById(workflowId);
        if (entity == null) {
            throw new RuntimeException("审批流程不存在");
        }

        entity.setWorkflowName(updateDTO.getWorkflowName());
        entity.setBusinessType(updateDTO.getBusinessType());
        entity.setTemplateCode(updateDTO.getTemplateCode());
        if (updateDTO.getEnabled() != null) {
            entity.setEnabled(updateDTO.getEnabled());
        }
        entity.setNodes(serializeToJson(updateDTO.getNodes()));
        entity.setConditions(serializeToJson(updateDTO.getConditions()));

        approvalWorkflowMapper.updateById(entity);
        log.info("更新审批流程成功: workflowId={}", workflowId);
        return convertToVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWorkflow(String workflowId) {
        ApprovalWorkflow entity = approvalWorkflowMapper.selectById(workflowId);
        if (entity == null) {
            throw new RuntimeException("审批流程不存在");
        }
        approvalWorkflowMapper.deleteById(workflowId);
        log.info("删除审批流程成功: workflowId={}", workflowId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleEnabled(String workflowId, Boolean enabled) {
        ApprovalWorkflow entity = approvalWorkflowMapper.selectById(workflowId);
        if (entity == null) {
            throw new RuntimeException("审批流程不存在");
        }
        entity.setEnabled(enabled);
        approvalWorkflowMapper.updateById(entity);
        log.info("切换审批流程启用状态: workflowId={}, enabled={}", workflowId, enabled);
    }

    @Override
    public ApprovalWorkflowVO getDefaultWorkflow(String businessType, String templateCode) {
        LambdaQueryWrapper<ApprovalWorkflow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalWorkflow::getBusinessType, businessType)
                .eq(ApprovalWorkflow::getTemplateCode, templateCode)
                .eq(ApprovalWorkflow::getEnabled, Boolean.TRUE)
                .last("LIMIT 1");
        ApprovalWorkflow entity = approvalWorkflowMapper.selectOne(wrapper);
        if (entity == null) {
            return null;
        }
        return convertToVO(entity);
    }

    // ==================== 审批操作 ====================

    @Override
    public ApprovalCurrentNodeVO getCurrentNode(String businessId, String businessType) {
        // 查询该业务最近的审批记录，推断当前节点
        LambdaQueryWrapper<ApprovalAuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalAuditLog::getBusinessId, businessId)
                .eq(ApprovalAuditLog::getBusinessType, businessType)
                .orderByDesc(ApprovalAuditLog::getOperateTime)
                .last("LIMIT 1");
        ApprovalAuditLog latestLog = approvalAuditLogMapper.selectOne(wrapper);

        if (latestLog == null) {
            return null;
        }

        // 如果最近一次操作是提交或通过，则当前节点为下一个节点
        // 如果最近一次操作是驳回或撤回，则审批已结束
        ApprovalCurrentNodeVO vo = new ApprovalCurrentNodeVO();
        vo.setNodeId(latestLog.getNodeId());
        vo.setNodeName(latestLog.getNodeName());
        vo.setApproverType(latestLog.getApproverType());
        vo.setApproverName(latestLog.getApproverName());

        if (ApprovalAuditLog.ACTION_SUBMIT.equals(latestLog.getAction())) {
            vo.setStatus("pending");
        } else if (ApprovalAuditLog.ACTION_APPROVE.equals(latestLog.getAction())) {
            vo.setStatus("completed");
        } else {
            vo.setStatus("waiting");
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitApproval(String businessType, String businessId) {
        // 查找该业务类型的默认审批流程（启用状态）
        ApprovalWorkflow workflow = findEnabledWorkflow(businessType);
        String workflowId = workflow != null ? workflow.getWorkflowId() : null;
        ApprovalNodeInfo firstNode = workflow != null ? extractFirstNodeInfo(workflow) : null;
        String nodeName = firstNode != null ? firstNode.getNodeName() : "提交审批";
        String nodeId = firstNode != null ? firstNode.getNodeId() : null;
        String approverType = firstNode != null ? firstNode.getApproverType() : ApprovalAuditLog.APPROVER_TYPE_ROLE;
        String approverName = firstNode != null ? firstNode.getApproverName() : null;

        ApprovalAuditLog logEntity = buildAuditLog(
                workflowId, businessId, businessType,
                nodeId, nodeName,
                approverType, approverName != null ? approverName : getCurrentOperatorName(),
                ApprovalAuditLog.ACTION_SUBMIT, "提交审批");
        approvalAuditLogMapper.insert(logEntity);
        log.info("提交审批: businessType={}, businessId={}", businessType, businessId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(ApprovalActionDTO actionDTO) {
        ApprovalAuditLog latestLog = findLatestLog(actionDTO.getBusinessId(), actionDTO.getBusinessType());
        String workflowId = latestLog != null ? latestLog.getWorkflowId() : null;
        String nodeId = latestLog != null ? latestLog.getNodeId() : null;
        String nodeName = latestLog != null ? latestLog.getNodeName() : "审批节点";
        String approverType = latestLog != null ? latestLog.getApproverType() : ApprovalAuditLog.APPROVER_TYPE_ROLE;
        String approverName = latestLog != null && latestLog.getApproverName() != null
                ? latestLog.getApproverName()
                : null;

        ApprovalAuditLog logEntity = buildAuditLog(
                workflowId, actionDTO.getBusinessId(), actionDTO.getBusinessType(),
                nodeId, nodeName,
                approverType, approverName != null ? approverName : getCurrentOperatorName(),
                ApprovalAuditLog.ACTION_APPROVE,
                actionDTO.getComment() != null ? actionDTO.getComment() : "同意");
        approvalAuditLogMapper.insert(logEntity);
        log.info("审批通过: businessType={}, businessId={}", actionDTO.getBusinessType(), actionDTO.getBusinessId());

        // 触发业务状态联动（第二层：BusinessApprovalHandler）
        triggerBusinessHandler(actionDTO, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(ApprovalActionDTO actionDTO) {
        ApprovalAuditLog latestLog = findLatestLog(actionDTO.getBusinessId(), actionDTO.getBusinessType());
        String workflowId = latestLog != null ? latestLog.getWorkflowId() : null;
        String nodeId = latestLog != null ? latestLog.getNodeId() : null;
        String nodeName = latestLog != null ? latestLog.getNodeName() : "审批节点";
        String approverType = latestLog != null ? latestLog.getApproverType() : ApprovalAuditLog.APPROVER_TYPE_ROLE;
        String approverName = latestLog != null && latestLog.getApproverName() != null
                ? latestLog.getApproverName()
                : null;

        ApprovalAuditLog logEntity = buildAuditLog(
                workflowId, actionDTO.getBusinessId(), actionDTO.getBusinessType(),
                nodeId, nodeName,
                approverType, approverName != null ? approverName : getCurrentOperatorName(),
                ApprovalAuditLog.ACTION_REJECT,
                actionDTO.getComment() != null ? actionDTO.getComment() : "驳回");
        approvalAuditLogMapper.insert(logEntity);
        log.info("审批驳回: businessType={}, businessId={}", actionDTO.getBusinessType(), actionDTO.getBusinessId());

        // 触发业务状态联动（第二层：BusinessApprovalHandler）
        triggerBusinessHandler(actionDTO, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(String businessId) {
        // 撤回审批需基于已有审批记录推断业务类型，找不到则抛出业务异常
        ApprovalAuditLog latestLog = findLatestLog(businessId, null);
        if (latestLog == null) {
            throw new RuntimeException("未找到对应的审批记录，无法撤回");
        }
        String workflowId = latestLog.getWorkflowId();
        String businessType = latestLog.getBusinessType();
        String nodeId = latestLog.getNodeId();
        String nodeName = latestLog.getNodeName();
        String approverType = latestLog.getApproverType() != null ? latestLog.getApproverType() : ApprovalAuditLog.APPROVER_TYPE_ROLE;
        String approverName = latestLog.getApproverName() != null ? latestLog.getApproverName() : getCurrentOperatorName();

        ApprovalAuditLog logEntity = buildAuditLog(
                workflowId, businessId, businessType,
                nodeId, nodeName,
                approverType, approverName,
                ApprovalAuditLog.ACTION_WITHDRAW, "撤回审批");
        approvalAuditLogMapper.insert(logEntity);
        log.info("撤回审批: businessId={}", businessId);
    }

    @Override
    public List<ApprovalAuditLogVO> getApprovalLogs(String businessId) {
        LambdaQueryWrapper<ApprovalAuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalAuditLog::getBusinessId, businessId)
                .orderByAsc(ApprovalAuditLog::getOperateTime); // 时间正序：提交在前
        List<ApprovalAuditLog> logs = approvalAuditLogMapper.selectList(wrapper);
        if (logs == null || logs.isEmpty()) {
            return Collections.emptyList();
        }
        return logs.stream().map(this::convertLogToVO).collect(Collectors.toList());
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 触发业务审批处理器，联动业务实体状态变更（第二层）
     * <p>根据 businessType 路由到对应的 BusinessApprovalHandler，
     * 调用 onApproved 或 onRejected 完成业务状态联动。
     * <p>未注册 handler 的 businessType 仅记录 debug 日志，不阻断审批流程。
     *
     * @param actionDTO 审批操作参数
     * @param approved true=审批通过，false=审批驳回
     */
    private void triggerBusinessHandler(ApprovalActionDTO actionDTO, boolean approved) {
        BusinessApprovalHandler handler = businessApprovalHandlers.get(actionDTO.getBusinessType());
        if (handler == null) {
            log.debug("未找到业务审批处理器（仅记录审批日志）: businessType={}", actionDTO.getBusinessType());
            return;
        }
        String operatorId = getCurrentOperatorId();
        if (approved) {
            handler.onApproved(actionDTO.getBusinessId(), actionDTO.getComment(), operatorId);
        } else {
            handler.onRejected(actionDTO.getBusinessId(), actionDTO.getComment(), operatorId);
        }
        log.info("业务状态联动成功: businessType={}, businessId={}, approved={}",
                actionDTO.getBusinessType(), actionDTO.getBusinessId(), approved);
    }

    /**
     * 获取当前操作人ID（从 Spring Security 上下文）
     * @return 当前用户ID（未认证或获取失败返回 null）
     */
    private String getCurrentOperatorId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof SecurityUser securityUser) {
                return securityUser.getUserId();
            }
        } catch (Exception e) {
            log.warn("获取当前操作人ID失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 获取当前操作人姓名（从 Spring Security 上下文）
     * @return 当前用户姓名或用户名（未认证或获取失败返回 "系统用户"）
     */
    private String getCurrentOperatorName() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof SecurityUser securityUser) {
                if (securityUser.getName() != null && !securityUser.getName().isEmpty()) {
                    return securityUser.getName();
                }
                if (securityUser.getUsername() != null && !securityUser.getUsername().isEmpty()) {
                    return securityUser.getUsername();
                }
            }
        } catch (Exception e) {
            log.warn("获取当前操作人姓名失败: {}", e.getMessage());
        }
        return "系统用户";
    }

    /**
     * 审批节点信息（从 workflow nodes JSON 中提取）
     */
    private static class ApprovalNodeInfo {
        private final String nodeId;
        private final String nodeName;
        private final String approverType;
        private final String approverName;

        ApprovalNodeInfo(String nodeId, String nodeName, String approverType, String approverName) {
            this.nodeId = nodeId;
            this.nodeName = nodeName;
            this.approverType = approverType;
            this.approverName = approverName;
        }

        String getNodeId() { return nodeId; }
        String getNodeName() { return nodeName; }
        String getApproverType() { return approverType; }
        String getApproverName() { return approverName; }
    }

    /**
     * 从审批流程的 nodes 中提取第一个节点完整信息
     */
    private ApprovalNodeInfo extractFirstNodeInfo(ApprovalWorkflow workflow) {
        List<Map<String, Object>> nodes = deserializeFromJson(workflow.getNodes());
        if (nodes.isEmpty()) {
            return null;
        }
        Map<String, Object> firstNode = nodes.get(0);
        String nodeId = firstNode.get("nodeId") != null ? firstNode.get("nodeId").toString() : null;
        String nodeName = firstNode.get("nodeName") != null ? firstNode.get("nodeName").toString() : "提交审批";
        String approverType = firstNode.get("approverType") != null ? firstNode.get("approverType").toString() : ApprovalAuditLog.APPROVER_TYPE_ROLE;
        String approverName = firstNode.get("approverName") != null ? firstNode.get("approverName").toString() : null;
        return new ApprovalNodeInfo(nodeId, nodeName, approverType, approverName);
    }

    /**
     * Entity 转 VO（包含 JSON 反序列化）
     */
    private ApprovalWorkflowVO convertToVO(ApprovalWorkflow entity) {
        ApprovalWorkflowVO vo = new ApprovalWorkflowVO();
        vo.setWorkflowId(entity.getWorkflowId());
        vo.setWorkflowName(entity.getWorkflowName());
        vo.setBusinessType(entity.getBusinessType());
        vo.setTemplateCode(entity.getTemplateCode());
        vo.setEnabled(entity.getEnabled());
        vo.setNodes(deserializeFromJson(entity.getNodes()));
        vo.setConditions(deserializeFromJson(entity.getConditions()));
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    /**
     * 审批记录 Entity 转 VO
     */
    private ApprovalAuditLogVO convertLogToVO(ApprovalAuditLog entity) {
        ApprovalAuditLogVO vo = new ApprovalAuditLogVO();
        vo.setLogId(entity.getLogId());
        vo.setWorkflowId(entity.getWorkflowId());
        vo.setBusinessId(entity.getBusinessId());
        vo.setBusinessType(entity.getBusinessType());
        vo.setNodeId(entity.getNodeId());
        vo.setNodeName(entity.getNodeName());
        vo.setApproverType(entity.getApproverType());
        vo.setApproverName(entity.getApproverName());
        vo.setAction(entity.getAction());
        vo.setOpinion(entity.getOpinion());
        vo.setOperateTime(entity.getOperateTime());
        return vo;
    }

    /**
     * 将 List&lt;Map&gt; 序列化为 JSON 字符串
     * @param list 结构化对象数组（可为 null）
     * @return JSON 字符串（null 或空数组返回 null）
     */
    private String serializeToJson(List<Map<String, Object>> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(list);
        } catch (Exception e) {
            log.error("JSON 序列化失败", e);
            return null;
        }
    }

    /**
     * 将 JSON 字符串反序列化为 List&lt;Map&gt;
     * @param json JSON 字符串（可为 null）
     * @return 结构化对象数组（null 或解析失败返回空列表）
     */
    private List<Map<String, Object>> deserializeFromJson(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            List<Map<String, Object>> result = OBJECT_MAPPER.readValue(json, LIST_MAP_TYPE);
            return result != null ? result : new ArrayList<>();
        } catch (Exception e) {
            log.error("JSON 反序列化失败: {}", json, e);
            return new ArrayList<>();
        }
    }

    /**
     * 查找启用状态的审批流程（按业务类型匹配，忽略权限模板）
     */
    private ApprovalWorkflow findEnabledWorkflow(String businessType) {
        LambdaQueryWrapper<ApprovalWorkflow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalWorkflow::getBusinessType, businessType)
                .eq(ApprovalWorkflow::getEnabled, Boolean.TRUE)
                .orderByAsc(ApprovalWorkflow::getCreateTime)
                .last("LIMIT 1");
        return approvalWorkflowMapper.selectOne(wrapper);
    }

    /**
     * 查找业务最近一条审批记录
     * @param businessId 业务ID
     * @param businessType 业务类型（可为 null，表示不限制）
     */
    private ApprovalAuditLog findLatestLog(String businessId, String businessType) {
        LambdaQueryWrapper<ApprovalAuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalAuditLog::getBusinessId, businessId);
        if (businessType != null && !businessType.isEmpty()) {
            wrapper.eq(ApprovalAuditLog::getBusinessType, businessType);
        }
        wrapper.orderByDesc(ApprovalAuditLog::getOperateTime)
                .last("LIMIT 1");
        return approvalAuditLogMapper.selectOne(wrapper);
    }

    /**
     * 从审批流程的 nodes 中提取第一个节点名称
     */
    private String extractFirstNodeName(ApprovalWorkflow workflow) {
        List<Map<String, Object>> nodes = deserializeFromJson(workflow.getNodes());
        if (nodes.isEmpty()) {
            return "审批节点";
        }
        Object name = nodes.get(0).get("nodeName");
        return name != null ? name.toString() : "审批节点";
    }

    /**
     * 构建审批记录 Entity
     */
    private ApprovalAuditLog buildAuditLog(String workflowId, String businessId, String businessType,
                                           String nodeId, String nodeName,
                                           String approverType, String approverName,
                                           String action, String opinion) {
        ApprovalAuditLog logEntity = new ApprovalAuditLog();
        logEntity.setWorkflowId(workflowId);
        logEntity.setBusinessId(businessId);
        logEntity.setBusinessType(businessType);
        logEntity.setNodeId(nodeId);
        logEntity.setNodeName(nodeName);
        logEntity.setApproverType(approverType);
        logEntity.setApproverName(approverName);
        logEntity.setAction(action);
        logEntity.setOpinion(opinion);
        logEntity.setOperateTime(LocalDateTime.now());
        return logEntity;
    }
}
