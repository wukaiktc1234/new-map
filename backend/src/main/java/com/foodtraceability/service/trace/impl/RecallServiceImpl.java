package com.foodtraceability.service.trace.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.dto.trace.BatchRecallCreateDTO;
import com.foodtraceability.dto.trace.BatchRecallResultVO;
import com.foodtraceability.dto.trace.RecallAnalyzeQueryDTO;
import com.foodtraceability.dto.trace.RecallCreateDTO;
import com.foodtraceability.dto.trace.RecallQueryResultVO;
import com.foodtraceability.dto.trace.RecallStatisticsVO;
import com.foodtraceability.dto.trace.TraceCodeCreateDTO;
import com.foodtraceability.dto.trace.TraceCodeQueryDTO;
import com.foodtraceability.dto.trace.TraceCodeUpdateDTO;
import com.foodtraceability.dto.trace.TraceCodeVO;
import com.foodtraceability.entity.Member;
import com.foodtraceability.entity.OrderNew;
import com.foodtraceability.entity.RecallRecord;
import com.foodtraceability.entity.TraceCode;
import com.foodtraceability.mapper.RecallRecordMapper;
import com.foodtraceability.service.trace.FoodTraceabilityService;
import com.foodtraceability.service.trace.RecallService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 召回管理服务实现类
 * 提供反向召回相关的影响分析、批量召回和统计功能
 * 统计查询异常将直接抛出，由全局异常处理器统一处理
 */
@Service
public class RecallServiceImpl implements RecallService {

    private static final Logger log = LoggerFactory.getLogger(RecallServiceImpl.class);

    /** 追溯码状态-已召回 */
    private static final int TRACE_STATUS_RECALLED = 4;
    /** 追溯码状态-即将过期 */
    private static final int TRACE_STATUS_EXPIRING_SOON = 2;
    /** 风险等级-中风险阈值 */
    private static final int RISK_LEVEL_MEDIUM = 2;
    /** 风险等级-高风险 */
    private static final int RISK_LEVEL_HIGH = 3;
    /** 召回记录状态-进行中 */
    private static final int RECALL_STATUS_IN_PROGRESS = 1;

    private final FoodTraceabilityService foodTraceabilityService;
    private final RecallRecordMapper recallRecordMapper;

    public RecallServiceImpl(FoodTraceabilityService foodTraceabilityService,
                             RecallRecordMapper recallRecordMapper) {
        this.foodTraceabilityService = foodTraceabilityService;
        this.recallRecordMapper = recallRecordMapper;
    }

    /**
     * 召回影响范围分析
     */
    @Override
    public RecallQueryResultVO analyzeRecall(RecallAnalyzeQueryDTO queryDTO) {
        return foodTraceabilityService.queryRecallImpact(
                queryDTO.getBatchNo(),
                queryDTO.getSupplierId(),
                queryDTO.getTargetName()
        );
    }

    /**
     * 批量召回
     * 逐条执行召回，记录成功/失败数；单条失败不中断整个批次，但会记录日志
     */
    @Override
    public BatchRecallResultVO batchRecall(BatchRecallCreateDTO dto, Long operatorId, String operatorName) {
        BatchRecallResultVO result = new BatchRecallResultVO();
        int successCount = 0;
        int failCount = 0;

        for (Long traceCodeId : dto.getTraceCodeIds()) {
            try {
                boolean ok = foodTraceabilityService.executeRecall(
                        traceCodeId,
                        dto.getReason(),
                        operatorId,
                        operatorName
                );
                if (ok) {
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                // 记录失败日志，不中断批次处理
                log.warn("批量召回单条失败，traceCodeId={}, error={}", traceCodeId, e.getMessage(), e);
                failCount++;
            }
        }

        result.setTotalCount(dto.getTraceCodeIds().size());
        result.setSuccessCount(successCount);
        result.setFailCount(failCount);
        return result;
    }

    /**
     * 召回统计概览
     * 实现真实统计：
     * - totalRecalledToday: 今日已召回（status=4 且 update_time 在今天）
     * - pendingRecall: 待处理召回（即将过期且中高风险 status=2 且 risk_level>=2）
     * - highRiskItems: 高风险项（risk_level=3）
     */
    @Override
    public RecallStatisticsVO getStatistics() {
        RecallStatisticsVO statistics = new RecallStatisticsVO();

        // 今日已召回：status=4（已召回）且 update_time 在今天
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long totalRecalledToday = foodTraceabilityService.count(
                new LambdaQueryWrapper<TraceCode>()
                        .eq(TraceCode::getStatus, TRACE_STATUS_RECALLED)
                        .ge(TraceCode::getUpdateTime, todayStart)
        );
        statistics.setTotalRecalledToday((int) totalRecalledToday);

        // 待处理召回：即将过期（status=2）且中高风险（risk_level>=2）
        long pendingRecall = foodTraceabilityService.count(
                new LambdaQueryWrapper<TraceCode>()
                        .eq(TraceCode::getStatus, TRACE_STATUS_EXPIRING_SOON)
                        .ge(TraceCode::getRiskLevel, RISK_LEVEL_MEDIUM)
        );
        statistics.setPendingRecall((int) pendingRecall);

        // 高风险项：risk_level=3
        long highRiskItems = foodTraceabilityService.count(
                new LambdaQueryWrapper<TraceCode>()
                        .eq(TraceCode::getRiskLevel, RISK_LEVEL_HIGH)
        );
        statistics.setHighRiskItems((int) highRiskItems);

        return statistics;
    }

    /**
     * 查询批次受影响的所有订单（DF-032：订单级追溯）
     * 委托给FoodTraceabilityService执行批次→追溯码→订单的完整反向追溯
     */
    @Override
    public List<OrderNew> findAffectedOrders(String batchNo) {
        if (batchNo == null || batchNo.trim().isEmpty()) {
            throw new IllegalArgumentException("批次号不能为空");
        }
        return foodTraceabilityService.findAffectedOrders(batchNo.trim());
    }

    /**
     * 查询批次受影响的所有客户（DF-034：受影响客户列表）
     * 通过受影响订单反查关联会员，去重后返回
     */
    @Override
    public List<Member> findAffectedCustomers(String batchNo) {
        if (batchNo == null || batchNo.trim().isEmpty()) {
            throw new IllegalArgumentException("批次号不能为空");
        }
        return foodTraceabilityService.findAffectedCustomers(batchNo.trim());
    }

    /**
     * 反向追溯：从溯源码查询所有受影响订单和客户（DF-033：反向追溯）
     * 溯源码→批次号→批次内所有追溯码→受影响订单/菜品/客户
     */
    @Override
    public RecallQueryResultVO reverseTrace(String traceCode) {
        if (traceCode == null || traceCode.trim().isEmpty()) {
            throw new IllegalArgumentException("追溯码不能为空");
        }
        return foodTraceabilityService.reverseTrace(traceCode.trim());
    }

    /**
     * 创建召回记录（审计留痕）
     * 完整事务流程：参数校验→影响范围统计→写入审计记录→返回记录
     * 事务保证：审计记录与影响范围快照原子性写入
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RecallRecord createRecallRecord(RecallCreateDTO dto, Long operatorId, String operatorName) {
        if (dto == null) {
            throw new IllegalArgumentException("召回请求不能为空");
        }
        if (operatorId == null) {
            throw new IllegalArgumentException("操作人ID不能为空");
        }

        // 1. 查询受影响范围快照（用于审计记录留痕）
        RecallQueryResultVO impact = foodTraceabilityService.queryRecallImpact(
                dto.getBatchNo(), null, null);

        // 2. 构建召回记录
        RecallRecord record = new RecallRecord();
        record.setBatchNo(dto.getBatchNo());
        record.setTraceCode(dto.getTraceCode());
        record.setRecallReason(dto.getRecallReason());
        record.setInitiatorId(operatorId);
        record.setInitiatorName(operatorName);
        record.setRecallTime(LocalDateTime.now());
        record.setAffectedOrderCount(impact.getSummary() != null ? impact.getSummary().getTotalOrders() : 0);
        record.setAffectedCustomerCount(foodTraceabilityService.findAffectedCustomers(dto.getBatchNo()).size());
        record.setAffectedTraceCodeCount(impact.getAffectedTraceCodes() != null ? impact.getAffectedTraceCodes().size() : 0);
        record.setStatus(RECALL_STATUS_IN_PROGRESS);
        record.setRemark(dto.getRemark());

        // 3. 写入审计记录
        int rows = recallRecordMapper.insert(record);
        if (rows <= 0) {
            throw new RuntimeException("召回记录写入失败");
        }
        log.info("召回记录创建成功：batchNo={}, operatorId={}, affectedOrders={}, affectedCustomers={}",
                dto.getBatchNo(), operatorId, record.getAffectedOrderCount(), record.getAffectedCustomerCount());
        return record;
    }

    /**
     * 分页查询召回记录列表
     * 委托给FoodTraceabilityService的通用分页查询
     */
    @Override
    public IPage<TraceCodeVO> queryPage(TraceCodeQueryDTO queryDTO) {
        return foodTraceabilityService.queryPage(queryDTO);
    }

    /**
     * 获取召回记录详情（含完整追溯链）
     * 先按ID查询实体，再通过追溯码查询完整链路信息
     */
    @Override
    public TraceCodeVO getDetailById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("追溯码ID不能为空");
        }
        TraceCode entity = foodTraceabilityService.getById(id);
        if (entity == null) {
            throw new IllegalArgumentException("召回记录不存在: " + id);
        }
        // 通过追溯码查询完整追溯链信息
        return foodTraceabilityService.queryByTraceCode(entity.getTraceCode());
    }

    /**
     * 创建召回记录
     * 委托给FoodTraceabilityService生成追溯码并初始化追溯链
     */
    @Override
    public TraceCodeVO create(TraceCodeCreateDTO dto, Long operatorId, String operatorName) {
        return foodTraceabilityService.generateTraceCode(dto, operatorId);
    }

    /**
     * 更新召回记录
     * 加载现有实体，应用DTO字段更新，再返回最新详情
     */
    @Override
    public TraceCodeVO update(Long id, TraceCodeUpdateDTO dto) {
        if (id == null) {
            throw new IllegalArgumentException("追溯码ID不能为空");
        }
        TraceCode entity = foodTraceabilityService.getById(id);
        if (entity == null) {
            throw new IllegalArgumentException("召回记录不存在: " + id);
        }

        // 按DTO字段更新（仅更新非空字段）
        if (dto.getTargetName() != null) {
            entity.setTargetName(dto.getTargetName());
        }
        if (dto.getBatchNo() != null) {
            entity.setBatchNo(dto.getBatchNo());
        }
        if (dto.getProductionDate() != null) {
            entity.setProductionDate(dto.getProductionDate());
        }
        if (dto.getExpiryDate() != null) {
            entity.setExpiryDate(dto.getExpiryDate());
        }
        if (dto.getSupplierName() != null) {
            entity.setSupplierName(dto.getSupplierName());
        }
        if (dto.getWarehouseId() != null) {
            entity.setWarehouseId(dto.getWarehouseId());
        }
        if (dto.getCurrentLocation() != null) {
            entity.setCurrentLocation(dto.getCurrentLocation());
        }
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
        if (dto.getRiskLevel() != null) {
            entity.setRiskLevel(dto.getRiskLevel());
        }
        if (dto.getQrCodeImageUrl() != null) {
            entity.setQrCodeImageUrl(dto.getQrCodeImageUrl());
        }
        if (dto.getExtraInfo() != null) {
            entity.setExtraInfo(dto.getExtraInfo());
        }

        boolean success = foodTraceabilityService.updateById(entity);
        if (!success) {
            throw new RuntimeException("更新召回记录失败: " + id);
        }
        // 返回更新后的完整详情
        return foodTraceabilityService.queryByTraceCode(entity.getTraceCode());
    }

    /**
     * 删除召回记录（逻辑删除）
     * 委托给FoodTraceabilityService，由@TableLogic注解自动处理逻辑删除
     */
    @Override
    public boolean delete(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("追溯码ID不能为空");
        }
        return foodTraceabilityService.removeById(id);
    }
}
