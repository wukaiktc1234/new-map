package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.foodtraceability.controller.websocket.OrderWebSocketController;
import com.foodtraceability.dto.FoodTraceCodeGenerateDTO;
import com.foodtraceability.dto.ScanMatchResultDTO;
import com.foodtraceability.entity.*;
import com.foodtraceability.event.OrderCompletedEvent;
import com.foodtraceability.mapper.*;
import com.foodtraceability.service.FoodTraceCodeService;
import com.foodtraceability.service.KitchenScanService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 后厨扫描服务实现类
 * 负责原料追溯码扫描、匹配、使用记录等业务逻辑
 */
@Service
public class KitchenScanServiceImpl implements KitchenScanService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(KitchenScanServiceImpl.class);
    private final MaterialTraceCodeMapper traceCodeMapper;
    private final OrderMaterialRequirementMapper requirementMapper;
    private final MaterialUsageRecordMapper usageRecordMapper;
    private final KitchenOrderMapper kitchenOrderMapper;
    private final DishRecipeMapper dishRecipeMapper;
    private final FoodMapper foodMapper;
    private final FoodTraceCodeService foodTraceCodeService;
    private final ObjectMapper objectMapper;
    private final OrderWebSocketController orderWebSocketController;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public ScanMatchResultDTO scanAndMatch(String traceCode, String operatorId, String operatorName) {
        return scanAndMatchWithQuantity(traceCode, null, operatorId, operatorName);
    }

    /**
     * 扫描追溯码并匹配订单需求（支持指定数量）
     */
    @Override
    @Transactional
    public ScanMatchResultDTO scanAndMatchWithQuantity(String traceCode, BigDecimal quantity, String operatorId, String operatorName) {
        log.info("扫描追溯码: {}, 操作人: {}", traceCode, operatorName);

        // 1. 验证追溯码
        MaterialTraceCode traceCodeEntity = validateTraceCode(traceCode);
        if (traceCodeEntity == null) {
            return ScanMatchResultDTO.fail("追溯码不存在: " + traceCode);
        }
        BigDecimal availableQty = getAvailableQuantity(traceCodeEntity);
        if (availableQty == null) {
            return ScanMatchResultDTO.fail("追溯码不存在: " + traceCode);
        }
        if (availableQty.compareTo(BigDecimal.ZERO) <= 0) {
            return ScanMatchResultDTO.fail("该追溯码无可用数量");
        }

        // 2. 匹配待制作订单需求
        String materialName = traceCodeEntity.getMaterialName();
        List<OrderMaterialRequirement> pendingRequirements = requirementMapper.findPendingByMaterialName(materialName);
        if (pendingRequirements.isEmpty()) {
            return ScanMatchResultDTO.fail("当前没有需要该原料的待制作订单");
        }
        OrderMaterialRequirement matchedRequirement = pendingRequirements.get(0);

        // 3. 计算使用数量
        BigDecimal useQuantity = calculateUseQuantity(quantity, availableQty, matchedRequirement);

        // 4. 记录使用并更新需求状态
        recordUsageAndUpdateRequirement(traceCode, traceCodeEntity, matchedRequirement, useQuantity, operatorId, operatorName);

        // 5. 更新追溯码数量
        BigDecimal newAvailableQty = updateTraceCodeQuantities(traceCodeEntity, useQuantity, availableQty);

        // 6. 更新后厨订单状态
        KitchenOrderStatusUpdateResult statusResult = updateKitchenOrderStatus(matchedRequirement.getKitchenOrderId(), operatorId, operatorName);

        // 7. 构建返回结果
        return buildScanMatchResult(traceCode, materialName, useQuantity, traceCodeEntity,
            matchedRequirement, newAvailableQty, statusResult);
    }

    /**
     * 验证追溯码有效性
     * @return 有效的追溯码实体，无效时返回null
     */
    private MaterialTraceCode validateTraceCode(String traceCode) {
        LambdaQueryWrapper<MaterialTraceCode> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MaterialTraceCode::getTraceCode, traceCode);
        MaterialTraceCode traceCodeEntity = traceCodeMapper.selectOne(queryWrapper);
        if (traceCodeEntity == null) {
            return null;
        }
        if ("used".equals(traceCodeEntity.getStatus())) {
            return null;
        }
        if (traceCodeEntity.getExpiryDate() != null && traceCodeEntity.getExpiryDate().isBefore(java.time.LocalDate.now())) {
            return null;
        }
        return traceCodeEntity;
    }

    /**
     * 获取追溯码可用数量
     * @return 可用数量，无效时返回null
     */
    private BigDecimal getAvailableQuantity(MaterialTraceCode traceCodeEntity) {
        BigDecimal availableQty = traceCodeEntity.getAvailableQuantity();
        if (availableQty == null) {
            availableQty = traceCodeEntity.getQuantity();
            if (availableQty == null) {
                availableQty = traceCodeEntity.getWeight();
            }
        }
        if (availableQty == null || availableQty.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return availableQty;
    }

    /**
     * 计算使用数量
     */
    private BigDecimal calculateUseQuantity(BigDecimal requestedQuantity, BigDecimal availableQty, OrderMaterialRequirement requirement) {
        BigDecimal requiredQty = requirement.getRequiredQuantity();
        BigDecimal usedQty = requirement.getUsedQuantity();
        if (usedQty == null) {
            usedQty = BigDecimal.ZERO;
        }
        BigDecimal remainingRequired = requiredQty.subtract(usedQty);
        BigDecimal useQuantity = requestedQuantity != null ? requestedQuantity : remainingRequired.min(availableQty);
        if (useQuantity.compareTo(availableQty) > 0) {
            useQuantity = availableQty;
        }
        return useQuantity;
    }

    /**
     * 记录原料使用并更新需求状态
     */
    private void recordUsageAndUpdateRequirement(String traceCode, MaterialTraceCode traceCodeEntity,
            OrderMaterialRequirement matchedRequirement, BigDecimal useQuantity,
            String operatorId, String operatorName) {
        String materialName = traceCodeEntity.getMaterialName();

        // 创建使用记录
        MaterialUsageRecord usageRecord = new MaterialUsageRecord();
        usageRecord.setRecordId("UR" + System.currentTimeMillis());
        usageRecord.setTraceCodeId(traceCodeEntity.getTraceCodeId());
        usageRecord.setTraceCode(traceCode);
        usageRecord.setMaterialId(traceCodeEntity.getMaterialId());
        usageRecord.setMaterialName(materialName);
        usageRecord.setUsedQuantity(useQuantity);
        usageRecord.setUnit(traceCodeEntity.getUnit() != null ? traceCodeEntity.getUnit() : traceCodeEntity.getWeightUnit());
        usageRecord.setOrderId(matchedRequirement.getOrderId());
        usageRecord.setKitchenOrderId(matchedRequirement.getKitchenOrderId());
        usageRecord.setOrderNumber(matchedRequirement.getOrderNumber());
        usageRecord.setDishName(matchedRequirement.getDishName());
        usageRecord.setOperatorId(operatorId);
        usageRecord.setOperatorName(operatorName);
        usageRecord.setUsageTime(LocalDateTime.now());
        usageRecordMapper.insert(usageRecord);

        // 更新需求状态
        BigDecimal requiredQty = matchedRequirement.getRequiredQuantity();
        BigDecimal usedQty = matchedRequirement.getUsedQuantity();
        if (usedQty == null) {
            usedQty = BigDecimal.ZERO;
        }
        BigDecimal newUsedQty = usedQty.add(useQuantity);
        matchedRequirement.setUsedQuantity(newUsedQty);
        if (newUsedQty.compareTo(requiredQty) >= 0) {
            matchedRequirement.setStatus("completed");
        } else {
            matchedRequirement.setStatus("partial");
        }
        requirementMapper.updateById(matchedRequirement);
    }

    /**
     * 更新追溯码数量
     * @return 新的可用数量
     */
    private BigDecimal updateTraceCodeQuantities(MaterialTraceCode traceCodeEntity, BigDecimal useQuantity, BigDecimal availableQty) {
        BigDecimal newAvailableQty = availableQty.subtract(useQuantity);
        traceCodeEntity.setAvailableQuantity(newAvailableQty);
        traceCodeEntity.setUsedQuantity(traceCodeEntity.getUsedQuantity() != null ? traceCodeEntity.getUsedQuantity().add(useQuantity) : useQuantity);
        if (newAvailableQty.compareTo(BigDecimal.ZERO) <= 0) {
            traceCodeEntity.setStatus("used");
            traceCodeEntity.setUseTime(LocalDateTime.now());
        }
        traceCodeMapper.updateById(traceCodeEntity);
        return newAvailableQty;
    }

    /**
     * 后厨订单状态更新结果
     */
    private static class KitchenOrderStatusUpdateResult {
        KitchenOrder kitchenOrder;
        boolean triggeredMaking;
        boolean triggeredComplete;
        String orderStatusChange;

        KitchenOrderStatusUpdateResult(KitchenOrder kitchenOrder, boolean triggeredMaking,
                boolean triggeredComplete, String orderStatusChange) {
            this.kitchenOrder = kitchenOrder;
            this.triggeredMaking = triggeredMaking;
            this.triggeredComplete = triggeredComplete;
            this.orderStatusChange = orderStatusChange;
        }
    }

    /**
     * 更新后厨订单状态
     * 如果原料扫描触发制作开始或完成，自动更新后厨订单状态
     */
    private KitchenOrderStatusUpdateResult updateKitchenOrderStatus(String kitchenOrderId, String operatorId, String operatorName) {
        KitchenOrder kitchenOrder = null;
        boolean triggeredMaking = false;
        boolean triggeredComplete = false;
        String orderStatusChange = null;

        if (kitchenOrderId != null) {
            kitchenOrder = kitchenOrderMapper.selectOne(
                new LambdaQueryWrapper<KitchenOrder>().eq(KitchenOrder::getKitchenOrderId, kitchenOrderId));

            // 待制作 -> 制作中
            if (kitchenOrder != null && "pending".equals(kitchenOrder.getStatus())) {
                String previousStatus = kitchenOrder.getStatus();
                kitchenOrder.setStatus("making");
                kitchenOrder.setMakeStartTime(LocalDateTime.now());
                kitchenOrder.setChefId(Long.parseLong(operatorId != null ? operatorId : "0"));
                kitchenOrder.setChefName(operatorName);
                kitchenOrderMapper.updateById(kitchenOrder);
                triggeredMaking = true;
                orderStatusChange = "pending -> making";
                log.info("订单 {} 状态变更为制作中", kitchenOrder.getOrderNumber());
                orderWebSocketController.pushOrderStatusChange(kitchenOrder, previousStatus);
            }

            // 制作中 -> 已完成（所有原料扫描完毕）
            if (kitchenOrder != null && "making".equals(kitchenOrder.getStatus())) {
                boolean allMaterialsComplete = checkAllMaterialsComplete(kitchenOrderId);
                if (allMaterialsComplete) {
                    String previousStatus = kitchenOrder.getStatus();
                    kitchenOrder.setStatus("completed");
                    kitchenOrder.setMakeCompleteTime(LocalDateTime.now());
                    kitchenOrderMapper.updateById(kitchenOrder);
                    triggeredComplete = true;
                    orderStatusChange = "making -> completed (auto)";
                    log.info("订单 {} 所有原料已扫描完成，自动完成制作", kitchenOrder.getOrderNumber());
                    orderWebSocketController.pushOrderStatusChange(kitchenOrder, previousStatus);
                    orderWebSocketController.pushReadyForPickup(kitchenOrder);
                }
            }
        }

        return new KitchenOrderStatusUpdateResult(kitchenOrder, triggeredMaking, triggeredComplete, orderStatusChange);
    }

    /**
     * 构建扫描匹配结果
     */
    private ScanMatchResultDTO buildScanMatchResult(String traceCode, String materialName, BigDecimal useQuantity,
            MaterialTraceCode traceCodeEntity, OrderMaterialRequirement matchedRequirement,
            BigDecimal newAvailableQty, KitchenOrderStatusUpdateResult statusResult) {
        ScanMatchResultDTO result = ScanMatchResultDTO.success("原料使用成功");
        result.setTraceCode(traceCode);
        result.setMaterialName(materialName);
        result.setUsedQuantity(useQuantity);
        result.setUnit(traceCodeEntity.getUnit() != null ? traceCodeEntity.getUnit() : traceCodeEntity.getWeightUnit());
        result.setOrderId(matchedRequirement.getOrderId());
        result.setKitchenOrderId(matchedRequirement.getKitchenOrderId());
        result.setOrderNumber(matchedRequirement.getOrderNumber());
        result.setDishName(matchedRequirement.getDishName());
        result.setRemainingQuantity(newAvailableQty);
        result.setTriggeredMaking(statusResult.triggeredMaking);
        result.setTriggeredComplete(statusResult.triggeredComplete);
        result.setOrderStatusChange(statusResult.orderStatusChange);
        return result;
    }

    /**
     * 检查后厨订单的所有原料需求是否已完成
     */
    private boolean checkAllMaterialsComplete(String kitchenOrderId) {
        List<OrderMaterialRequirement> requirements = requirementMapper.findByKitchenOrderId(kitchenOrderId);
        if (requirements == null || requirements.isEmpty()) {
            return false;
        }
        for (OrderMaterialRequirement req : requirements) {
            if (!"completed".equals(req.getStatus())) {
                return false;
            }
        }
        return true;
    }

    @Override
    @Transactional
    public void lockMaterialsForOrder(String kitchenOrderId) {
        log.info("为后厨订单锁定原料: {}", kitchenOrderId);
        KitchenOrder kitchenOrder = kitchenOrderMapper.selectOne(new LambdaQueryWrapper<KitchenOrder>().eq(KitchenOrder::getKitchenOrderId, kitchenOrderId));
        if (kitchenOrder == null) {
            log.warn("后厨订单不存在: {}", kitchenOrderId);
            return;
        }
        List<OrderMaterialRequirement> requirements = requirementMapper.findByKitchenOrderId(kitchenOrderId);
        for (OrderMaterialRequirement req : requirements) {
            BigDecimal requiredQty = req.getRequiredQuantity();
            BigDecimal lockedQty = BigDecimal.ZERO;
            LambdaQueryWrapper<MaterialTraceCode> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(MaterialTraceCode::getMaterialName, req.getMaterialName()).ne(MaterialTraceCode::getStatus, "used").isNull(MaterialTraceCode::getExpiryDate).or().gt(MaterialTraceCode::getExpiryDate, java.time.LocalDate.now()).orderByAsc(MaterialTraceCode::getExpiryDate).orderByAsc(MaterialTraceCode::getCreateTime);
            List<MaterialTraceCode> availableCodes = traceCodeMapper.selectList(queryWrapper);
            for (MaterialTraceCode code : availableCodes) {
                if (lockedQty.compareTo(requiredQty) >= 0) {
                    break;
                }
                BigDecimal available = code.getAvailableQuantity() != null ? code.getAvailableQuantity() : code.getQuantity();
                if (available == null) {
                    available = code.getWeight();
                }
                BigDecimal lockAmount = requiredQty.subtract(lockedQty).min(available);
                BigDecimal codeLocked = code.getLockedQuantity() != null ? code.getLockedQuantity() : BigDecimal.ZERO;
                code.setLockedQuantity(codeLocked.add(lockAmount));
                code.setAvailableQuantity(available.subtract(lockAmount));
                traceCodeMapper.updateById(code);
                lockedQty = lockedQty.add(lockAmount);
            }
            req.setLockedQuantity(lockedQty);
            requirementMapper.updateById(req);
        }
        kitchenOrder.setMaterialLocked(1);
        kitchenOrder.setMaterialLockTime(LocalDateTime.now());
        kitchenOrderMapper.updateById(kitchenOrder);
    }

    @Override
    @Transactional
    public void unlockMaterialsForOrder(String kitchenOrderId) {
        log.info("释放后厨订单的原料锁定: {}", kitchenOrderId);
        List<OrderMaterialRequirement> requirements = requirementMapper.findByKitchenOrderId(kitchenOrderId);
        for (OrderMaterialRequirement req : requirements) {
            if (req.getLockedQuantity() != null && req.getLockedQuantity().compareTo(BigDecimal.ZERO) > 0) {
                LambdaQueryWrapper<MaterialTraceCode> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(MaterialTraceCode::getMaterialName, req.getMaterialName()).gt(MaterialTraceCode::getLockedQuantity, BigDecimal.ZERO);
                List<MaterialTraceCode> lockedCodes = traceCodeMapper.selectList(queryWrapper);
                BigDecimal toUnlock = req.getLockedQuantity();
                for (MaterialTraceCode code : lockedCodes) {
                    if (toUnlock.compareTo(BigDecimal.ZERO) <= 0) {
                        break;
                    }
                    BigDecimal codeLocked = code.getLockedQuantity() != null ? code.getLockedQuantity() : BigDecimal.ZERO;
                    BigDecimal unlockAmount = toUnlock.min(codeLocked);
                    code.setLockedQuantity(codeLocked.subtract(unlockAmount));
                    BigDecimal available = code.getAvailableQuantity() != null ? code.getAvailableQuantity() : BigDecimal.ZERO;
                    code.setAvailableQuantity(available.add(unlockAmount));
                    traceCodeMapper.updateById(code);
                    toUnlock = toUnlock.subtract(unlockAmount);
                }
                req.setLockedQuantity(BigDecimal.ZERO);
                requirementMapper.updateById(req);
            }
        }
    }

    public KitchenScanServiceImpl(final MaterialTraceCodeMapper traceCodeMapper, final OrderMaterialRequirementMapper requirementMapper, final MaterialUsageRecordMapper usageRecordMapper, final KitchenOrderMapper kitchenOrderMapper, final DishRecipeMapper dishRecipeMapper, final FoodMapper foodMapper, final FoodTraceCodeService foodTraceCodeService, final ObjectMapper objectMapper, final OrderWebSocketController orderWebSocketController, final ApplicationEventPublisher eventPublisher) {
        this.traceCodeMapper = traceCodeMapper;
        this.requirementMapper = requirementMapper;
        this.usageRecordMapper = usageRecordMapper;
        this.kitchenOrderMapper = kitchenOrderMapper;
        this.dishRecipeMapper = dishRecipeMapper;
        this.foodMapper = foodMapper;
        this.foodTraceCodeService = foodTraceCodeService;
        this.objectMapper = objectMapper;
        this.orderWebSocketController = orderWebSocketController;
        this.eventPublisher = eventPublisher;
    }
}
