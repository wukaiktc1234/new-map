package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.entity.InventoryTransfer;
import com.foodtraceability.entity.Product;
import com.foodtraceability.entity.Warehouse;
import com.foodtraceability.entity.enums.InventoryTransferStatus;

import com.foodtraceability.mapper.InventoryTransferMapper;
import com.foodtraceability.mapper.ProductMapper;
import com.foodtraceability.mapper.WarehouseMapper;
import com.foodtraceability.service.InventoryTransferService;
import com.foodtraceability.service.InventoryService;
import com.foodtraceability.service.LocationService;
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
                                        InventoryService inventoryService,
                                        LocationService locationService) {
        this.inventoryTransferMapper = inventoryTransferMapper;
        this.productMapper = productMapper;
        this.warehouseMapper = warehouseMapper;
        this.inventoryService = inventoryService;
        this.locationService = locationService;
    }

    private final InventoryTransferMapper inventoryTransferMapper;

    private final ProductMapper productMapper;

    private final WarehouseMapper warehouseMapper;

    private final InventoryService inventoryService;
    private final LocationService locationService;
    
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

        // M3-M4 S7b：原 updateInventory（warehouse_id + current_stock 列）已删除——
        // post-DDL 这些列不存在；syncStoreInventory 已通过统一 InventoryService 完成 location 维度增减。

        // 同步库存（统一账 location 维度，S6a 已收编）
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
     * 调拨库存同步（统一账 location 维度）。
     * <p><b>P0-B 卡（P1-TRANSFER-UNMAPPED-REJECT-001）</b>：双侧位置<b>先解析、后写账</b>；
     * 任一侧未映射到位置即显式拒绝（宪法 §三.4 禁止默认兜底/静默跳过），文案与同族 6 处
     * （PurchaseStockin / PurchaseArrival / PurchaseReturn / OtherInbound / ReceiptConfirmation /
     * OrderNew）对齐。收编前此处对 null location 仅"跳过"，导致未映射源仓时调入仓凭空增加、
     * 未映射目标仓时调出仓蒸发，而单据仍被置为"已完成"。</p>
     *
     * @param transfer 调拨单
     * @param quantity 调拨数量
     */
    private void syncStoreInventory(InventoryTransfer transfer, BigDecimal quantity) {
        Long productId = transfer.getProductId();
        if (productId == null) {
            // P0-B 同族：缺物料时"静默跳过 + 单据置已完成"属假成功，禁止（宪法 §III.4）
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "调拨单缺少物料ID（productId），禁止静默跳过：transferId=" + transfer.getTransferId());
        }

        // 先解析双侧位置（未映射即拒绝，此时尚未写任何账）
        Long fromLocationId = requireLocationId(transfer.getFromWarehouseId(), "调出仓");
        Long toLocationId = requireLocationId(transfer.getToWarehouseId(), "调入仓");

        // 流水 source_ref 携带调拨单号，支持按单追溯（原为常量字符串，无法按单对账）
        String ref = "调拨单:" + (transfer.getTransferCode() != null
                ? transfer.getTransferCode() : String.valueOf(transfer.getTransferId()));

        // 从调出位置扣减库存（库存不足/行不存在会抛 BusinessException，触发主事务回滚）
        inventoryService.decreaseStockAtLocation(fromLocationId, productId, quantity,
                "TRANSFER_OUT", "库存调拨出库 - " + ref);
        log.debug("调出位置库存扣减成功: locationId={}, productId={}, quantity={}",
                fromLocationId, productId, quantity);

        // 向调入位置增加库存（unitCost 传 null：调拨场景不传成本，§6-6 现状迁移，Q3 批复前不改）
        inventoryService.increaseStockAtLocation(
                toLocationId,
                productId,
                transfer.getProductName(),
                quantity,
                null, // 单位：调拨单未携带，由库存行记录已有的单位保持不变
                null, // 单位成本：§6-6 现状迁移
                "TRANSFER_IN",
                "库存调拨入库 - " + ref
        );
        log.debug("调入位置库存增加成功: locationId={}, productId={}, quantity={}",
                toLocationId, productId, quantity);
    }

    /**
     * P0-B：warehouseId → locationId 解析；未映射即显式拒绝（宪法 §三.4）。
     *
     * @param side 侧别文案（调出仓/调入仓），用于定位失败原因
     */
    private Long requireLocationId(Long warehouseId, String side) {
        if (warehouseId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "调拨单" + side + "为空，无法解析库存位置");
        }
        com.foodtraceability.entity.Location location = locationService.resolveByWarehouseId(warehouseId);
        if (location == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "调拨单" + side + "未映射到位置，禁止调拨（warehouseId=" + warehouseId
                            + "；宪法 §三.4 未映射显式拒绝，禁止默认兜底）");
        }
        return location.getLocationId();
    }

}
