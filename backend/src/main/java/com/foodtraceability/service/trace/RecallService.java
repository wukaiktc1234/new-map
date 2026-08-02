package com.foodtraceability.service.trace;

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

import java.util.List;

/**
 * 召回管理服务接口
 * 提供反向召回相关的影响分析、批量召回、统计以及召回记录的CRUD功能
 */
public interface RecallService {

    /**
     * 召回影响范围分析
     * @param queryDTO 查询条件（批次号/供应商/目标名称）
     * @return 召回影响范围报告
     */
    RecallQueryResultVO analyzeRecall(RecallAnalyzeQueryDTO queryDTO);

    /**
     * 批量召回 - 逐条执行召回，记录成功/失败数，单条失败不中断整个批次
     * @param dto 批量召回请求（追溯码ID列表和召回原因）
     * @param operatorId 操作人ID（来自安全上下文）
     * @param operatorName 操作人姓名（来自安全上下文）
     * @return 批量召回结果统计
     */
    BatchRecallResultVO batchRecall(BatchRecallCreateDTO dto, Long operatorId, String operatorName);

    /**
     * 召回统计概览
     * @return 召回统计数据
     */
    RecallStatisticsVO getStatistics();

    /**
     * 查询批次受影响的所有订单（DF-032：订单级追溯）
     * @param batchNo 批次号
     * @return 受影响订单列表
     */
    List<OrderNew> findAffectedOrders(String batchNo);

    /**
     * 查询批次受影响的所有客户（DF-034：受影响客户列表）
     * @param batchNo 批次号
     * @return 受影响客户列表
     */
    List<Member> findAffectedCustomers(String batchNo);

    /**
     * 反向追溯：从溯源码查询所有受影响订单和客户（DF-033：反向追溯）
     * @param traceCode 追溯码
     * @return 召回影响范围报告（含受影响订单/菜品/客户）
     */
    RecallQueryResultVO reverseTrace(String traceCode);

    /**
     * 创建召回记录（审计留痕）- 发起一次召回并写入审计记录
     * @param dto 召回创建请求
     * @param operatorId 操作人ID（来自安全上下文）
     * @param operatorName 操作人姓名（来自安全上下文）
     * @return 创建后的召回记录
     */
    RecallRecord createRecallRecord(RecallCreateDTO dto, Long operatorId, String operatorName);

    /**
     * 分页查询召回记录列表
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    IPage<TraceCodeVO> queryPage(TraceCodeQueryDTO queryDTO);

    /**
     * 获取召回记录详情（含完整追溯链）
     * @param id 追溯码ID
     * @return 召回记录详情
     */
    TraceCodeVO getDetailById(Long id);

    /**
     * 创建召回记录
     * @param dto 创建请求
     * @param operatorId 操作人ID
     * @param operatorName 操作人姓名
     * @return 创建后的召回记录
     */
    TraceCodeVO create(TraceCodeCreateDTO dto, Long operatorId, String operatorName);

    /**
     * 更新召回记录
     * @param id 追溯码ID
     * @param dto 更新请求
     * @return 更新后的召回记录
     */
    TraceCodeVO update(Long id, TraceCodeUpdateDTO dto);

    /**
     * 删除召回记录（逻辑删除）
     * @param id 追溯码ID
     * @return 是否删除成功
     */
    boolean delete(Long id);
}
