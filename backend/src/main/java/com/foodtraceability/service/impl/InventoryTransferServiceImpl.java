package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.Inventory;
import com.foodtraceability.entity.InventoryTransfer;
import com.foodtraceability.entity.Product;
import com.foodtraceability.entity.Warehouse;
import com.foodtraceability.entity.enums.InventoryTransferStatus;
import com.foodtraceability.mapper.InventoryMapper;
import com.foodtraceability.mapper.InventoryTransferMapper;
import com.foodtraceability.mapper.ProductMapper;
import com.foodtraceability.mapper.WarehouseMapper;
import com.foodtraceability.service.InventoryTransferService;
import com.foodtraceability.service.StoreInventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 调拨单服务实现类
 * 实现调拨管理相关的业务方法
 *
 * 状态码统一遵循 {@link InventoryTransferStatus}：
 *   0=草稿, 1=待审批, 2=已审批, 3=已完成, 4=已取消/已拒绝
 */
@Service
public class InventoryTransferServiceImpl extends ServiceImpl<InventoryTransferMapper, InventoryTransfer> implements InventoryTransferService {

    private static final Logger log = LoggerFactory.getLogger(InventoryTransferServiceImpl.class);


    public InventoryTransferServiceImpl(InventoryTransferMapper inventoryTransferMapper,
                                        ProductMapper productMapper,
                                        WarehouseMapper warehouseMapper,
                                        InventoryMapper inventoryMapper,
                                        StoreInventoryService storeInventoryService) {
        this.inventoryTransferMapper = inventoryTransferMapper;
        this.productMapper = productMapper;
        this.warehouseMapper = warehouseMapper;
        this.inventoryMapper = inventoryMapper;
        this.storeInventoryService = storeInventoryService;
    }

    private final InventoryTransferMapper inventoryTransferMapper;

    private final ProductMapper productMapper;

    private final WarehouseMapper warehouseMapper;

    private final InventoryMapper inventoryMapper;

    /**
     * 门店库存服务：用于调拨执行时同步 store_inventory 表
     * 与 inventory 表（中央仓库存）双写保持一致
     */
    private final StoreInventoryService storeInventoryService;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryTransfer createInventoryTransfer(InventoryTransfer inventoryTransfer) {
        // 获取产品信息
        if (inventoryTransfer.getProductId() != null) {
            Product product = productMapper.selectById(inventoryTransfer.getProductId());
            if (product != null) {
                inventoryTransfer.setProductName(product.getName());
            }
        }
        
        // 获取调出仓库信息
        if (inventoryTransfer.getFromWarehouseId() != null) {
            Warehouse fromWarehouse = warehouseMapper.selectById(inventoryTransfer.getFromWarehouseId());
            if (fromWarehouse != null) {
                inventoryTransfer.setFromWarehouseName(fromWarehouse.getName());
            }
        }
        
        // 获取调入仓库信息
        if (inventoryTransfer.getToWarehouseId() != null) {
            Warehouse toWarehouse = warehouseMapper.selectById(inventoryTransfer.getToWarehouseId());
            if (toWarehouse != null) {
                inventoryTransfer.setToWarehouseName(toWarehouse.getName());
            }
        }
        
        // 设置默认状态：新建调拨单默认进入「待审批」状态
        // 状态码统一遵循 InventoryTransferStatus（0=草稿, 1=待审批, 2=已审批, 3=已完成, 4=已取消）
        if (inventoryTransfer.getTransferStatus() == null) {
            inventoryTransfer.setTransferStatus(InventoryTransferStatus.PENDING_APPROVAL.getCode()); // 待审批
        }

        // 设置默认值
        if (inventoryTransfer.getDeleted() == null) {
            inventoryTransfer.setDeleted(0);
        }

        // 生成调拨单号（为空时自动生成）
        if (inventoryTransfer.getTransferCode() == null || inventoryTransfer.getTransferCode().isBlank()) {
            inventoryTransfer.setTransferCode(generateTransferCode());
        }

        inventoryTransfer.setCreatedAt(new Date());
        inventoryTransfer.setUpdatedAt(new Date());

        this.save(inventoryTransfer);
        log.info("创建调拨单成功: productId={}, fromWarehouse={}, toWarehouse={}, quantity={}, status={}",
                inventoryTransfer.getProductId(), inventoryTransfer.getFromWarehouseName(),
                inventoryTransfer.getToWarehouseName(), inventoryTransfer.getTransferQuantity(),
                inventoryTransfer.getTransferStatus());
        return inventoryTransfer;
    }
    
    @Override
    public InventoryTransfer updateInventoryTransfer(Long id, InventoryTransfer inventoryTransfer) {
        inventoryTransfer.setId(id);
        inventoryTransfer.setUpdatedAt(new Date());
        this.updateById(inventoryTransfer);
        return this.getById(id);
    }
    
    @Override
    public InventoryTransfer getInventoryTransferById(Long id) {
        return this.getById(id);
    }
    
    @Override
    public void deleteInventoryTransfer(Long id) {
        this.removeById(id);
    }
    
    @Override
    public IPage<InventoryTransfer> getInventoryTransferPage(Page<InventoryTransfer> page, String transferNo, Long fromWarehouseId, Long toWarehouseId, String status, String applyTimeStart, String applyTimeEnd) {
        return inventoryTransferMapper.selectInventoryTransferPage(page, transferNo, fromWarehouseId, toWarehouseId, status, applyTimeStart, applyTimeEnd);
    }

    /**
     * 生成调拨单号
     * <p>格式：TR + yyyyMMdd + 3位日序号，例如 TR20260723001</p>
     *
     * @return 调拨单号
     */
    private String generateTransferCode() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = this.count(new LambdaQueryWrapper<InventoryTransfer>().likeRight(InventoryTransfer::getTransferCode, "TR" + dateStr));
        return String.format("TR%s%03d", dateStr, count + 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryTransfer approveInventoryTransfer(Long id, String status, String remark) {
        InventoryTransfer inventoryTransfer = this.getById(id);
        if (inventoryTransfer == null) {
            throw new RuntimeException("调拨单不存在");
        }

        // 验证当前状态：仅「待审批」状态可审批
        // 修复 DF-016：原代码检查 status != 1，但创建时设为 0，导致审批永远失败
        // 现统一使用枚举：创建时设为 PENDING_APPROVAL(1)，此处校验 PENDING_APPROVAL(1)
        if (!InventoryTransferStatus.PENDING_APPROVAL.getCode().equals(inventoryTransfer.getTransferStatus())) {
            throw new RuntimeException("调拨单状态不正确，仅待审批状态可审批，当前状态："
                    + InventoryTransferStatus.fromCode(inventoryTransfer.getTransferStatus()).getDescription());
        }

        // 更新状态
        if ("approved".equals(status)) {
            inventoryTransfer.setStatus(InventoryTransferStatus.APPROVED.getCode()); // 已审批
        } else if ("rejected".equals(status)) {
            inventoryTransfer.setStatus(InventoryTransferStatus.CANCELLED.getCode()); // 已取消/已拒绝
        } else {
            throw new RuntimeException("无效的审批状态，仅支持 approved / rejected");
        }

        if (remark != null) {
            inventoryTransfer.setRemark(remark);
        }
        inventoryTransfer.setUpdatedAt(new Date());
        this.updateById(inventoryTransfer);
        log.info("审批调拨单: transferId={}, approveResult={}, newStatus={}",
                id, status, inventoryTransfer.getTransferStatus());
        return inventoryTransfer;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryTransfer executeInventoryTransfer(Long id) {
        InventoryTransfer inventoryTransfer = this.getById(id);
        if (inventoryTransfer == null) {
            throw new RuntimeException("调拨单不存在");
        }

        // 验证当前状态：仅「已审批」状态可执行
        if (!InventoryTransferStatus.APPROVED.getCode().equals(inventoryTransfer.getTransferStatus())) {
            throw new RuntimeException("调拨单未审批，无法执行，当前状态："
                    + InventoryTransferStatus.fromCode(inventoryTransfer.getTransferStatus()).getDescription());
        }

        // 校验同仓调拨（DF-021）：源仓库与目标仓库不能相同
        Long fromWarehouseId = inventoryTransfer.getFromWarehouseId();
        Long toWarehouseId = inventoryTransfer.getToWarehouseId();
        if (fromWarehouseId != null && fromWarehouseId.equals(toWarehouseId)) {
            throw new RuntimeException("源仓库与目标仓库不能相同，无法执行调拨");
        }

        // 执行库存调拨
        Long productId = inventoryTransfer.getProductId();
        BigDecimal quantity = inventoryTransfer.getTransferQuantity();
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("调拨数量必须大于0");
        }

        // 1. 更新中央仓 inventory 表（同事务强一致）
        // 从调出仓库扣减库存
        updateInventory(productId, fromWarehouseId, quantity.negate());
        // 向调入仓库增加库存
        updateInventory(productId, toWarehouseId, quantity);

        // 2. 同步门店库存 store_inventory 表（修复 DF-018：原实现只更新 inventory 表）
        // 仓库与门店的映射沿用 PurchaseStockinServiceImpl 既有约定：warehouseId 作为 storeId
        // （与项目现有 DF-006 设计限制保持一致，不在本次修复范围内变更）
        syncStoreInventory(inventoryTransfer, quantity);

        // 更新调拨单状态为「已完成」
        inventoryTransfer.setStatus(InventoryTransferStatus.COMPLETED.getCode());
        inventoryTransfer.setUpdatedAt(new Date());
        this.updateById(inventoryTransfer);

        log.info("执行调拨单成功: transferId={}, fromWarehouse={}, toWarehouse={}, quantity={}, newStatus={}",
                id, fromWarehouseId, toWarehouseId, quantity, inventoryTransfer.getTransferStatus());
        return inventoryTransfer;
    }

    /**
     * 同步门店库存（store_inventory 表）
     * <p>调拨执行时同时更新 store_inventory 表，保持中央仓 inventory 与门店 store_inventory 数据一致。
     * 在主事务内同步执行，任何一张表更新失败都将导致整个调拨事务回滚（强一致性）。</p>
     *
     * <p>注意：门店库存同步遵循项目既有约定，使用 warehouseId 作为 storeId
     * （与 PurchaseStockinServiceImpl.increaseInventoryForStockin 一致）。</p>
     *
     * @param transfer 调拨单
     * @param quantity 调拨数量
     */
    private void syncStoreInventory(InventoryTransfer transfer, BigDecimal quantity) {
        Long productId = transfer.getProductId();
        if (productId == null) {
            log.warn("调拨单 productId 为空，跳过 store_inventory 同步：transferId={}", transfer.getTransferId());
            return;
        }

        String fromStoreId = transfer.getFromWarehouseId() != null
                ? String.valueOf(transfer.getFromWarehouseId()) : null;
        String toStoreId = transfer.getToWarehouseId() != null
                ? String.valueOf(transfer.getToWarehouseId()) : null;

        // 从调出门店扣减库存（库存不足会抛 BusinessException，触发主事务回滚）
        if (fromStoreId != null) {
            storeInventoryService.decreaseStock(fromStoreId, productId, quantity, 3, "库存调拨出库");
            log.debug("调出门店库存扣减成功: storeId={}, productId={}, quantity={}",
                    fromStoreId, productId, quantity);
        }

        // 向调入门店增加库存（unitCost 传 null，目标门店库存以 0 成本初始化，
        // 与采购入库同步门店库存时使用 item.getUnitPrice() 的行为一致；
        // 调拨场景下成本追踪由 inventory 表承担，store_inventory 仅做数量同步）
        if (toStoreId != null) {
            storeInventoryService.increaseStock(
                    toStoreId,
                    productId,
                    transfer.getProductName(),
                    quantity,
                    null, // 单位：调拨单未携带，由 store_inventory 记录已有的单位保持不变
                    null, // 单位成本：调拨场景不传成本
                    3,
                    "库存调拨入库"
            );
            log.debug("调入门店库存增加成功: storeId={}, productId={}, quantity={}",
                    toStoreId, productId, quantity);
        }
    }
    
    /**
     * 更新库存
     */
    private void updateInventory(Long productId, Long warehouseId, BigDecimal quantity) {
        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();
        // inventory 表使用 material_id 作为物料标识，与调拨单的 product_id 等价
        wrapper.eq(Inventory::getMaterialId, productId)
               .eq(Inventory::getWarehouseId, warehouseId)
               .eq(Inventory::getDeleted, 0);

        Inventory inventory = inventoryMapper.selectOne(wrapper);

        if (inventory == null) {
            if (quantity.compareTo(BigDecimal.ZERO) < 0) {
                throw new RuntimeException("库存不足，无法扣减");
            }
            // 创建新库存记录
            inventory = new Inventory();
            inventory.setMaterialId(productId);
            inventory.setWarehouseId(warehouseId);
            inventory.setCurrentStock(quantity);
            inventory.setSafetyStock(BigDecimal.ZERO);
            inventory.setDeleted(0);
            inventoryMapper.insert(inventory);
            log.info("创建库存记录: productId={}, warehouseId={}, stock={}", productId, warehouseId, quantity);
        } else {
            BigDecimal oldStock = inventory.getCurrentStock();
            BigDecimal newStock = oldStock.add(quantity);
            if (newStock.compareTo(BigDecimal.ZERO) < 0) {
                throw new RuntimeException("库存不足，当前库存: " + oldStock);
            }
            inventory.setCurrentStock(newStock);
            inventoryMapper.updateById(inventory);
            log.info("更新库存: productId={}, warehouseId={}, oldStock={}, change={}, newStock={}",
                    productId, warehouseId, oldStock, quantity, newStock);
        }
    }
}
