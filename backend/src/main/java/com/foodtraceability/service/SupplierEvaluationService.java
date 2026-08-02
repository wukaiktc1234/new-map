package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.SupplierEvaluationCreateDTO;
import com.foodtraceability.entity.SupplierEvaluation;

import java.util.List;

/**
 * 供应商评估服务接口
 * 管理供应商的定期评估，支持手动创建和自动计算
 */
public interface SupplierEvaluationService extends IService<SupplierEvaluation> {

    /**
     * 手动创建供应商评估
     * @param createDTO 评估数据
     * @return 创建后的评估记录
     */
    SupplierEvaluation createEvaluation(SupplierEvaluationCreateDTO createDTO);

    /**
     * 自动计算指定供应商在指定周期的评估结果
     * 基于历史订单数据自动计算各项评分指标
     * @param supplierId 供应商ID
     * @param period 评估周期（如2026-04）
     * @return 自动生成的评估记录
     */
    SupplierEvaluation autoCalculateEvaluation(Long supplierId, String period);

    /**
     * 获取供应商的评估历史列表
     * @param supplierId 供应商ID
     * @return 评估记录列表（按周期倒序）
     */
    List<SupplierEvaluation> getEvaluationsBySupplier(Long supplierId);

    /**
     * 获取供应商最新一次评估结果
     * @param supplierId 供应商ID
     * @return 最新评估记录
     */
    SupplierEvaluation getLatestEvaluation(Long supplierId);
}
