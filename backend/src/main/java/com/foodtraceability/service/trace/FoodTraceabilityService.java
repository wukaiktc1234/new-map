package com.foodtraceability.service.trace;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.dto.trace.*;
import com.foodtraceability.entity.Member;
import com.foodtraceability.entity.OrderNew;
import com.foodtraceability.entity.TraceCode;

import java.util.List;

/**
 * 食品追溯服务接口
 * 系统核心服务，提供追溯码的生成、查询、正向追溯和反向召回功能
 */
public interface FoodTraceabilityService extends IService<TraceCode> {

    /**
     * 生成追溯码 - 核心方法：创建追溯码并组装完整的追溯链数据
     * @param dto 创建请求
     * @param createUserId 操作用户ID
     * @return 生成的追溯码VO
     */
    TraceCodeVO generateTraceCode(TraceCodeCreateDTO dto, Long createUserId);

    /**
     * 正向追溯查询（扫码查询）- 根据追溯码查询完整的追溯链信息
     * @param traceCode 追溯码
     * @return 追溯码详情（含完整链条）
     */
    TraceCodeVO queryByTraceCode(String traceCode);

    /**
     * 反向召回查询 - 根据批次号/供应商/问题原料查询受影响范围
     * @param batchNo 批次号（可选）
     * @param supplierId 供应商ID（可选）
     * @param targetName 目标名称（可选，模糊搜索）
     * @return 召回影响范围报告
     */
    RecallQueryResultVO queryRecallImpact(String batchNo, Long supplierId, String targetName);

    /**
     * 查询批次受影响的所有订单（DF-032/DF-033：订单级追溯）
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
     * 分页查询追溯码列表
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    IPage<TraceCodeVO> queryPage(TraceCodeQueryDTO queryDTO);

    /**
     * 更新追溯码状态
     */
    boolean updateStatus(Long traceCodeId, Integer status);

    /**
     * 执行召回操作 - 将指定追溯码的状态更新为已召回，并记录召回链节点
     */
    boolean executeRecall(Long traceCodeId, String recallReason, Long operatorId, String operatorName);

    /**
     * 添加追溯链节点 - 向指定追溯码添加一个新的链路环节
     */
    TraceChainNodeVO addChainNode(Long traceCodeId, Integer nodeType, String location,
                                     Long operatorId, String operatorName, Object detailJson);

    /**
     * 记录消费者扫码行为 - 在追溯链中添加扫码节点
     */
    boolean recordScanEvent(String traceCode);

    /**
     * 更新风险等级 - 根据有效期、质检结果等信息自动计算风险等级
     */
    Integer refreshRiskLevel(Long traceCodeId);
}
