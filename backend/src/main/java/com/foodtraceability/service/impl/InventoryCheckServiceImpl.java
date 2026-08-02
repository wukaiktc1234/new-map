package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.InventoryCheckCreateDTO;
import com.foodtraceability.entity.InventoryCheck;
import com.foodtraceability.entity.InventoryCheckItem;
import com.foodtraceability.mapper.InventoryCheckItemMapper;
import com.foodtraceability.mapper.InventoryCheckMapper;
import com.foodtraceability.service.InventoryCheckService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class InventoryCheckServiceImpl extends ServiceImpl<InventoryCheckMapper, InventoryCheck> implements InventoryCheckService {

    private final InventoryCheckMapper checkMapper;
    private final InventoryCheckItemMapper checkItemMapper;

    public InventoryCheckServiceImpl(InventoryCheckMapper checkMapper,
                                      InventoryCheckItemMapper checkItemMapper) {
        this.checkMapper = checkMapper;
        this.checkItemMapper = checkItemMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryCheck createCheck(InventoryCheckCreateDTO createDTO) {
        // 生成盘点单号
        String checkCode = generateCheckCode();

        InventoryCheck check = new InventoryCheck();
        check.setCheckCode(checkCode);
        check.setWarehouseId(createDTO.getWarehouseId());
        check.setCheckType(createDTO.getCheckType());
        check.setCheckDate(createDTO.getCheckDate());
        check.setCheckStatus(0); // 待盘点
        check.setRemark(createDTO.getRemark());
        check.setCreateTime(LocalDateTime.now());
        check.setUpdateTime(LocalDateTime.now());

        this.save(check);
        return check;
    }

    @Override
    public List<InventoryCheck> getChecksByWarehouse(Long warehouseId) {
        return this.lambdaQuery()
                .eq(InventoryCheck::getWarehouseId, warehouseId)
                .orderByDesc(InventoryCheck::getCreateTime)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryCheck updateCheck(Long checkId, InventoryCheckCreateDTO updateDTO) {
        InventoryCheck existing = this.getById(checkId);
        if (existing == null) {
            throw new RuntimeException("盘点单不存在：" + checkId);
        }
        // 仅草稿状态（待盘点）允许更新，避免覆盖已审批/已完成的盘点结果
        if (existing.getCheckStatus() != null && existing.getCheckStatus() != 0) {
            throw new RuntimeException("当前盘点单状态不允许更新，仅草稿状态可更新");
        }

        if (updateDTO.getWarehouseId() != null) {
            existing.setWarehouseId(updateDTO.getWarehouseId());
        }
        if (updateDTO.getCheckType() != null) {
            existing.setCheckType(updateDTO.getCheckType());
        }
        if (updateDTO.getCheckDate() != null) {
            existing.setCheckDate(updateDTO.getCheckDate());
        }
        if (updateDTO.getRemark() != null) {
            existing.setRemark(updateDTO.getRemark());
        }
        existing.setUpdateTime(LocalDateTime.now());

        this.updateById(existing);
        return this.getById(checkId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCheck(Long checkId) {
        InventoryCheck existing = this.getById(checkId);
        if (existing == null) {
            throw new RuntimeException("盘点单不存在：" + checkId);
        }
        // 仅草稿状态允许删除，防止误删已审批/已完成的盘点数据
        if (existing.getCheckStatus() != null && existing.getCheckStatus() != 0) {
            throw new RuntimeException("当前盘点单状态不允许删除，仅草稿状态可删除");
        }

        // 同步逻辑删除关联的盘点条目（草稿状态通常无条目，此处做安全兜底）
        LambdaQueryWrapper<InventoryCheckItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(InventoryCheckItem::getCheckId, checkId);
        List<InventoryCheckItem> items = checkItemMapper.selectList(itemWrapper);
        for (InventoryCheckItem item : items) {
            checkItemMapper.deleteById(item.getCheckItemId());
        }

        // 业务核心数据使用逻辑删除，@TableLogic 自动将 deleted 置为 1
        this.removeById(checkId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryCheck saveCheckItems(Long checkId, List<Map<String, Object>> items) {
        InventoryCheck check = this.getById(checkId);
        if (check == null) {
            throw new RuntimeException("盘点单不存在：" + checkId);
        }

        // 先清除该盘点单已有的条目，确保重复提交时不会产生脏数据
        LambdaQueryWrapper<InventoryCheckItem> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(InventoryCheckItem::getCheckId, checkId);
        checkItemMapper.delete(existWrapper);

        // 持久化盘点条目到 inventory_check_item 表
        if (items != null && !items.isEmpty()) {
            for (Map<String, Object> itemMap : items) {
                InventoryCheckItem item = new InventoryCheckItem();
                item.setCheckId(checkId);

                Object inventoryIdRaw = itemMap.get("inventoryId");
                if (inventoryIdRaw != null) {
                    try {
                        item.setInventoryId(Long.parseLong(inventoryIdRaw.toString()));
                    } catch (NumberFormatException ignored) {
                        // 兼容非数字ID，跳过赋值
                    }
                }

                item.setBookQty(toBigDecimal(itemMap.get("bookQty")));
                item.setActualQty(toBigDecimal(itemMap.get("actualQty")));

                // 计算差异数量（实盘 - 账面），前端未传时自动计算
                BigDecimal bookQty = item.getBookQty() != null ? item.getBookQty() : BigDecimal.ZERO;
                BigDecimal actualQty = item.getActualQty() != null ? item.getActualQty() : BigDecimal.ZERO;
                Object diffQtyRaw = itemMap.get("diffQty");
                if (diffQtyRaw != null) {
                    item.setDiffQty(toBigDecimal(diffQtyRaw));
                } else {
                    item.setDiffQty(actualQty.subtract(bookQty));
                }

                // 差异金额（分），元转分
                Object diffAmountRaw = itemMap.get("diffAmount");
                if (diffAmountRaw != null) {
                    try {
                        BigDecimal diffAmountYuan = new BigDecimal(diffAmountRaw.toString());
                        item.setDiffAmount(diffAmountYuan.multiply(new BigDecimal("100")).longValue());
                    } catch (NumberFormatException ignored) {
                        // 非数字金额跳过
                    }
                }

                Object reasonRaw = itemMap.get("reason");
                if (reasonRaw != null) {
                    item.setReason(reasonRaw.toString());
                }

                item.setCreateTime(LocalDateTime.now());
                item.setUpdateTime(LocalDateTime.now());
                checkItemMapper.insert(item);
            }
        }

        // 更新盘点单状态为盘点中
        check.setCheckStatus(1);
        check.setUpdateTime(LocalDateTime.now());
        this.updateById(check);
        return check;
    }

    /**
     * 安全将对象转换为 BigDecimal
     */
    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 生成盘点单号
     */
    private String generateCheckCode() {
        return "CK" + System.currentTimeMillis();
    }
}
