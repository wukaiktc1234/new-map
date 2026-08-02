package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.dto.SupplierEvaluationCreateDTO;
import com.foodtraceability.entity.PurchaseOrder;
import com.foodtraceability.entity.SupplierEvaluation;
import com.foodtraceability.mapper.PurchaseOrderMapper;
import com.foodtraceability.mapper.SupplierEvaluationMapper;
import com.foodtraceability.service.SupplierEvaluationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 供应商评估服务实现类
 * 管理供应商的定期评估，支持手动创建和基于历史数据自动计算
 */
@Service
public class SupplierEvaluationServiceImpl extends ServiceImpl<SupplierEvaluationMapper, SupplierEvaluation>
        implements SupplierEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(SupplierEvaluationServiceImpl.class);

    private final SupplierEvaluationMapper supplierEvaluationMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;

    public SupplierEvaluationServiceImpl(SupplierEvaluationMapper supplierEvaluationMapper,
                                         PurchaseOrderMapper purchaseOrderMapper) {
        this.supplierEvaluationMapper = supplierEvaluationMapper;
        this.purchaseOrderMapper = purchaseOrderMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupplierEvaluation createEvaluation(SupplierEvaluationCreateDTO createDTO) {
        // 检查同一供应商同一周期是否已有评估
        LambdaQueryWrapper<SupplierEvaluation> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(SupplierEvaluation::getSupplierId, createDTO.getSupplierId())
                .eq(SupplierEvaluation::getEvaluationPeriod, createDTO.getEvaluationPeriod());
        Long existingCount = supplierEvaluationMapper.selectCount(checkWrapper);
        if (existingCount > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                "该供应商在指定周期已有评估记录，请勿重复创建");
        }

        SupplierEvaluation evaluation = new SupplierEvaluation();
        evaluation.setSupplierId(createDTO.getSupplierId());
        evaluation.setEvaluationPeriod(createDTO.getEvaluationPeriod());
        evaluation.setDeliveryScore(createDTO.getDeliveryScore());
        evaluation.setQualityScore(createDTO.getQualityScore());
        evaluation.setPriceScore(createDTO.getPriceScore());
        evaluation.setServiceScore(createDTO.getServiceScore());
        evaluation.setTotalScore(createDTO.calculateTotalScore());
        evaluation.setEvaluationTime(LocalDateTime.now());
        evaluation.setRemark(createDTO.getRemark());

        supplierEvaluationMapper.insert(evaluation);
        log.info("创建供应商评估成功，供应商ID：{}，周期：{}", createDTO.getSupplierId(), createDTO.getEvaluationPeriod());

        return evaluation;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupplierEvaluation autoCalculateEvaluation(Long supplierId, String period) {
        // 查询该供应商在指定周期的所有已完成/已取消的订单
        LambdaQueryWrapper<PurchaseOrder> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(PurchaseOrder::getSupplierId, supplierId)
                .ge(PurchaseOrder::getOrderDate, period + "-01")
                .le(PurchaseOrder::getOrderDate, getLastDayOfMonth(period));
        List<PurchaseOrder> orders = purchaseOrderMapper.selectList(orderWrapper);

        if (orders.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                "该供应商在指定周期内无任何订单数据，无法自动计算评估");
        }

        // 计算各项指标
        int totalOrders = orders.size();
        int onTimeOrders = 0;      // 及时交货订单数
        int qualifiedOrders = 0;    // 合格订单数

        for (PurchaseOrder order : orders) {
            // 简化逻辑：已完成的算及时交货和合格
            if (order.getOrderStatus() != null && order.getOrderStatus() == 4) {
                onTimeOrders++;
                qualifiedOrders++;
            }
        }

        BigDecimal onTimeRate = totalOrders > 0 ?
                BigDecimal.valueOf(onTimeOrders * 100.0 / totalOrders).setScale(2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;
        BigDecimal qualifiedRate = totalOrders > 0 ?
                BigDecimal.valueOf(qualifiedOrders * 100.0 / totalOrders).setScale(2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        // 基于指标自动评分（简化算法）
        BigDecimal deliveryScore = calculateScoreFromRate(onTimeRate);
        BigDecimal qualityScore = calculateScoreFromRate(qualifiedRate);
        BigDecimal priceScore = BigDecimal.valueOf(4.00); // 默认中等价格竞争力
        BigDecimal serviceScore = BigDecimal.valueOf(4.00); // 默认中等服务

        // 计算综合评分
        double avgScore = (deliveryScore.doubleValue() + qualityScore.doubleValue()
                + priceScore.doubleValue() + serviceScore.doubleValue()) / 4.0;
        BigDecimal totalScore = BigDecimal.valueOf(avgScore).setScale(2, RoundingMode.HALF_UP);

        // 构建评估对象
        SupplierEvaluation evaluation = new SupplierEvaluation();
        evaluation.setSupplierId(supplierId);
        evaluation.setEvaluationPeriod(period);
        evaluation.setDeliveryScore(deliveryScore);
        evaluation.setQualityScore(qualityScore);
        evaluation.setPriceScore(priceScore);
        evaluation.setServiceScore(serviceScore);
        evaluation.setTotalScore(totalScore);
        evaluation.setOrderCount(totalOrders);
        evaluation.setOnTimeRate(onTimeRate);
        evaluation.setQualifiedRate(qualifiedRate);
        evaluation.setEvaluationTime(LocalDateTime.now());

        // 检查是否已存在
        LambdaQueryWrapper<SupplierEvaluation> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(SupplierEvaluation::getSupplierId, supplierId)
                .eq(SupplierEvaluation::getEvaluationPeriod, period);
        SupplierEvaluation existing = supplierEvaluationMapper.selectOne(existWrapper);

        if (existing != null) {
            // 更新已有记录
            evaluation.setEvaluationId(existing.getEvaluationId());
            supplierEvaluationMapper.updateById(evaluation);
            log.info("更新供应商自动评估，供应商ID：{}，周期：{}", supplierId, period);
        } else {
            // 创建新记录
            supplierEvaluationMapper.insert(evaluation);
            log.info("创建供应商自动评估，供应商ID：{}，周期：{}", supplierId, period);
        }

        return evaluation;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierEvaluation> getEvaluationsBySupplier(Long supplierId) {
        LambdaQueryWrapper<SupplierEvaluation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupplierEvaluation::getSupplierId, supplierId)
              .orderByDesc(SupplierEvaluation::getEvaluationPeriod);
        return supplierEvaluationMapper.selectList(wrapper);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierEvaluation getLatestEvaluation(Long supplierId) {
        LambdaQueryWrapper<SupplierEvaluation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupplierEvaluation::getSupplierId, supplierId)
              .orderByDesc(SupplierEvaluation::getEvaluationPeriod)
              .last("LIMIT 1");
        return supplierEvaluationMapper.selectOne(wrapper);
    }

    /**
     * 根据百分比比率计算评分（0-5分）
     */
    private BigDecimal calculateScoreFromRate(BigDecimal rate) {
        if (rate == null) {
            return BigDecimal.valueOf(3.00);
        }
        // 90%以上: 5分, 80-90%: 4分, 70-80%: 3分, 60-70%: 2分, 60%以下: 1分
        double r = rate.doubleValue();
        if (r >= 90) {
            return BigDecimal.valueOf(5.00);
        } else if (r >= 80) {
            return BigDecimal.valueOf(4.00);
        } else if (r >= 70) {
            return BigDecimal.valueOf(3.50);
        } else if (r >= 60) {
            return BigDecimal.valueOf(2.50);
        } else {
            return BigDecimal.valueOf(1.50);
        }
    }

    /**
     * 获取月份的最后一天日期字符串
     */
    private String getLastDayOfMonth(String period) {
        try {
            String[] parts = period.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);

            // 简化处理：返回月末日期
            int lastDay = switch (month) {
                case 1, 3, 5, 7, 8, 10, 12 -> 31;
                case 4, 6, 9, 11 -> 30;
                case 2 -> 28; // 简化不考虑闰年
                default -> 30;
            };

            return String.format("%s-%02d-%02d", year, month, lastDay);
        } catch (Exception e) {
            return period + "-31";
        }
    }
}
