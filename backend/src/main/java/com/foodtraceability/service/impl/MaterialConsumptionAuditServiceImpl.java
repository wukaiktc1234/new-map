package com.foodtraceability.service.impl;

import com.foodtraceability.common.exception.MaterialDeductionContext;
import com.foodtraceability.common.exception.MaterialDeductionFailureException;
import com.foodtraceability.entity.MaterialConsumption;
import com.foodtraceability.mapper.MaterialConsumptionMapper;
import com.foodtraceability.service.MaterialConsumptionAuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * KDS 出餐扣料失败审计服务实现（P0）。
 * <p>
 * REQUIRES_NEW：悬挂调用方（主扣料）事务，在独立事务中提交 FAILED 审计行。
 * 主事务随后回滚时，审计行不受影响（已提交）。
 * <p>
 * 审计行复用 material_consumption 表：
 * <ul>
 *   <li>status = 'FAILED' 区分于普通消耗记录（status 为 NULL）；</li>
 *   <li>inventory_deducted = 0（失败时未发生有效扣减；主事务回滚后库存已恢复）；</li>
 *   <li>bom_materials 仅当 BOM 已展开时记录，未展开保持 NULL（禁止编造）。</li>
 * </ul>
 */
@Service
public class MaterialConsumptionAuditServiceImpl implements MaterialConsumptionAuditService {

    private static final Logger log = LoggerFactory.getLogger(MaterialConsumptionAuditServiceImpl.class);

    private final MaterialConsumptionMapper materialConsumptionMapper;

    public MaterialConsumptionAuditServiceImpl(MaterialConsumptionMapper materialConsumptionMapper) {
        this.materialConsumptionMapper = materialConsumptionMapper;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void recordDeductionFailure(MaterialDeductionFailureException failure) {
        MaterialDeductionContext ctx = failure.getContext();
        try {
            MaterialConsumption mc = new MaterialConsumption();
            mc.setConsumptionId(generateAuditConsumptionId());
            mc.setStatus("FAILED");
            mc.setFailureType(failure.getFailureType());
            mc.setFailureReason(failure.getMessage() != null && failure.getMessage().length() > 2000
                    ? failure.getMessage().substring(0, 2000) : failure.getMessage());

            mc.setKitchenOrderId(ctx.getKitchenOrderId());
            mc.setOrderId(ctx.getOrderId());
            mc.setOrderNumber(ctx.getOrderNumber());
            mc.setStoreId(ctx.getStoreId());
            mc.setStoreName(ctx.getStoreName());
            mc.setTrayCode(ctx.getTrayCode());

            mc.setBomMaterials(joinMaterials(ctx.getBomMaterials()));
            mc.setAttemptedMaterials(joinMaterials(ctx.getAttemptedMaterials()));
            mc.setFailedMaterialId(ctx.getFailedMaterialId() != null ? String.valueOf(ctx.getFailedMaterialId()) : null);
            mc.setFailedMaterialName(ctx.getFailedMaterialName());

            mc.setInventoryDeducted(0);
            LocalDateTime now = LocalDateTime.now();
            mc.setConsumeTime(now);
            mc.setCreateTime(now);
            mc.setUpdateTime(now);
            mc.setRemark("KDS出餐扣料失败审计记录：由独立REQUIRES_NEW事务写入，主扣料事务已回滚，material_consumed已恢复为0");

            materialConsumptionMapper.insert(mc);
            log.error("[扣料失败审计] 已写入FAILED审计行: consumptionId={}, kitchenOrderId={}, orderId={}, orderNumber={}, "
                            + "failureType={}, failedMaterialId={}, failedMaterialName={}, reason={}",
                    mc.getConsumptionId(), ctx.getKitchenOrderId(), ctx.getOrderId(), ctx.getOrderNumber(),
                    failure.getFailureType(), ctx.getFailedMaterialId(), ctx.getFailedMaterialName(), failure.getMessage());
        } catch (Exception ex) {
            // 审计写入失败不得影响主异常传播：仅记录 ERROR 日志。
            log.error("[扣料失败审计] 写入FAILED审计行失败（审计行可能丢失，不影响主异常传播）: kitchenOrderId={}, failureType={}, error={}",
                    ctx != null ? ctx.getKitchenOrderId() : null, failure.getFailureType(), ex.getMessage(), ex);
        }
    }

    /**
     * 生成唯一审计 consumption_id（MCF + 毫秒时间戳 + 4位随机数，共 20 字符，满足 UNIQUE 与长度 50）。
     */
    private String generateAuditConsumptionId() {
        return "MCF" + System.currentTimeMillis()
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private String joinMaterials(List<String> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        return items.stream().collect(Collectors.joining("; "));
    }
}
