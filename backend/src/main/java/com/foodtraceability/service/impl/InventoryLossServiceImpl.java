package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.InventoryDecreaseDTO;
import com.foodtraceability.entity.InventoryLoss;
import com.foodtraceability.entity.InventoryLossDetail;
import com.foodtraceability.mapper.InventoryLossDetailMapper;
import com.foodtraceability.mapper.InventoryLossMapper;
import com.foodtraceability.service.InventoryLossService;
import com.foodtraceability.service.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 报损单服务实现类
 * 实现报损管理相关的业务方法
 *
 * 类级 @Transactional：所有写操作均纳入事务管理，确保库存扣减与状态更新的原子性。
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class InventoryLossServiceImpl extends ServiceImpl<InventoryLossMapper, InventoryLoss> implements InventoryLossService {

    private final InventoryLossMapper inventoryLossMapper;
    private final InventoryLossDetailMapper inventoryLossDetailMapper;
    private final InventoryService inventoryService;

    public InventoryLossServiceImpl(InventoryLossMapper inventoryLossMapper,
                                     InventoryLossDetailMapper inventoryLossDetailMapper,
                                     InventoryService inventoryService) {
        this.inventoryLossMapper = inventoryLossMapper;
        this.inventoryLossDetailMapper = inventoryLossDetailMapper;
        this.inventoryService = inventoryService;
    }

    @Override
    public InventoryLoss createInventoryLoss(InventoryLoss inventoryLoss) {
        this.save(inventoryLoss);
        return inventoryLoss;
    }

    @Override
    public InventoryLoss updateInventoryLoss(Long id, InventoryLoss inventoryLoss) {
        inventoryLoss.setLossId(id);
        this.updateById(inventoryLoss);
        return this.getById(id);
    }

    @Override
    public InventoryLoss getInventoryLossById(Long id) {
        return this.getById(id);
    }

    @Override
    public void deleteInventoryLoss(Long id) {
        this.removeById(id);
    }

    @Override
    public IPage<InventoryLoss> getInventoryLossPage(Page<InventoryLoss> page, String lossNo, Long warehouseId, String status, String applyTimeStart, String applyTimeEnd) {
        return inventoryLossMapper.selectInventoryLossPage(page, lossNo, warehouseId, status, applyTimeStart, applyTimeEnd);
    }

    @Override
    public InventoryLoss approveInventoryLoss(Long id, String status, String remark) {
        InventoryLoss inventoryLoss = this.getById(id);
        if (inventoryLoss != null) {
            // 将字符串状态转换为整数：approved->1(已审核), rejected->-1(已拒绝)
            Integer statusValue = parseLossStatus(status);
            inventoryLoss.setLossStatus(statusValue);
            // 这里可以添加审批人ID和审批时间，假设当前用户ID可以通过上下文获取
            // inventoryLoss.setApproverId(approverId);
            // inventoryLoss.setApproveTime(LocalDateTime.now());
            this.updateById(inventoryLoss);
        }
        return inventoryLoss;
    }

    /**
     * 解析报损状态字符串为整数值
     * @param status 状态字符串
     * @return 对应的整数值
     */
    private Integer parseLossStatus(String status) {
        if (status == null) {
            return 0; // 默认待审核
        }
        switch (status.toLowerCase()) {
            case "approved":
            case "1":
                return 1; // 已审核
            case "processed":
            case "2":
                return 2; // 已处理
            case "rejected":
            case "-1":
                return -1; // 已拒绝
            default:
                return 0; // 待审核
        }
    }

    /**
     * 处理报损：根据报损明细扣减实际库存，并更新报损单状态为已处理。
     *
     * <p>业务流程：
     * 1. 查询报损单及明细
     * 2. 遍历明细，调用 InventoryService.decreaseInventory 扣减库存
     * 3. transactionType=3（按业务约定，报损走调拨出库类型编码）
     * 4. 更新报损单状态为 2（已处理）</p>
     *
     * @param id 报损单ID
     * @return 已处理的报损单
     */
    @Override
    public InventoryLoss processInventoryLoss(Long id) {
        InventoryLoss inventoryLoss = this.getById(id);
        if (inventoryLoss == null) {
            throw new RuntimeException("报损单不存在：" + id);
        }

        // 查询报损明细
        LambdaQueryWrapper<InventoryLossDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(InventoryLossDetail::getLossId, id);
        List<InventoryLossDetail> details = inventoryLossDetailMapper.selectList(detailWrapper);

        Long warehouseId = inventoryLoss.getWarehouseId();
        String lossCode = inventoryLoss.getLossCode();

        // 遍历明细扣减库存
        for (InventoryLossDetail detail : details) {
            BigDecimal qty = detail.getQuantity();
            if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (detail.getProductId() == null || warehouseId == null) {
                continue;
            }

            InventoryDecreaseDTO decreaseDTO = new InventoryDecreaseDTO();
            decreaseDTO.setMaterialId(detail.getProductId());
            decreaseDTO.setWarehouseId(warehouseId);
            decreaseDTO.setQuantity(qty);
            decreaseDTO.setTransactionType(3);
            decreaseDTO.setReferenceNo(lossCode);
            decreaseDTO.setReferenceType("inventory_loss");
            decreaseDTO.setRemark(detail.getReason());
            inventoryService.decreaseInventory(decreaseDTO);
        }

        inventoryLoss.setLossStatus(2); // 2:已处理
        inventoryLoss.setUpdateTime(LocalDateTime.now());
        this.updateById(inventoryLoss);

        return inventoryLoss;
    }
}
