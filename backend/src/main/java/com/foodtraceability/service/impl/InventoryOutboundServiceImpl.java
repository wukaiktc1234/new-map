package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.InventoryDecreaseDTO;
import com.foodtraceability.dto.InventoryOutboundCreateDTO;
import com.foodtraceability.entity.InventoryOutbound;
import com.foodtraceability.entity.InventoryOutboundItem;
import com.foodtraceability.mapper.InventoryOutboundItemMapper;
import com.foodtraceability.mapper.InventoryOutboundMapper;
import com.foodtraceability.service.InventoryOutboundService;
import com.foodtraceability.service.InventoryService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 库存出库单服务实现类
 * 实现库存出库管理相关的业务方法
 *
 * 类级 @Transactional：所有写操作均纳入事务管理，确保库存扣减与状态更新的原子性。
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class InventoryOutboundServiceImpl extends ServiceImpl<InventoryOutboundMapper, InventoryOutbound> implements InventoryOutboundService {

    private final InventoryOutboundMapper inventoryOutboundMapper;
    private final InventoryOutboundItemMapper inventoryOutboundItemMapper;
    private final InventoryService inventoryService;

    public InventoryOutboundServiceImpl(InventoryOutboundMapper inventoryOutboundMapper,
                                         InventoryOutboundItemMapper inventoryOutboundItemMapper,
                                         InventoryService inventoryService) {
        this.inventoryOutboundMapper = inventoryOutboundMapper;
        this.inventoryOutboundItemMapper = inventoryOutboundItemMapper;
        this.inventoryService = inventoryService;
    }

    @Override
    public IPage<Map<String, Object>> getOutboundPage(Page<InventoryOutbound> page,
                                                       String outboundType,
                                                       String status,
                                                       String warehouseId,
                                                       String keyword,
                                                       String startDate,
                                                       String endDate) {
        IPage<InventoryOutbound> outboundPage = inventoryOutboundMapper.selectOutboundPage(
                page, outboundType, status, warehouseId, keyword, startDate, endDate);

        return outboundPage.convert(this::convertOutboundToMap);
    }

    @Override
    public Map<String, Object> getOutboundByCode(String outboundCode) {
        LambdaQueryWrapper<InventoryOutbound> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InventoryOutbound::getOutboundCode, outboundCode);
        InventoryOutbound outbound = this.getOne(wrapper);
        if (outbound == null) {
            return null;
        }

        Map<String, Object> result = convertOutboundToMap(outbound);

        // 查询明细列表
        LambdaQueryWrapper<InventoryOutboundItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(InventoryOutboundItem::getOutboundId, outbound.getOutboundId());
        List<InventoryOutboundItem> items = inventoryOutboundItemMapper.selectList(itemWrapper);

        List<Map<String, Object>> itemList = items.stream()
                .map(this::convertItemToMap)
                .collect(Collectors.toList());
        result.put("items", itemList);

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryOutbound createOutbound(InventoryOutbound outbound, List<Map<String, Object>> items) {
        // 生成出库单号：OB + yyyyMMddHHmmss
        String outboundCode = "OB" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        outbound.setOutboundCode(outboundCode);
        outbound.setStatus("pending");
        outbound.setApplyTime(LocalDateTime.now());
        outbound.setCreateTime(LocalDateTime.now());
        outbound.setUpdateTime(LocalDateTime.now());

        // 计算总数量
        BigDecimal totalQuantity = BigDecimal.ZERO;
        for (Map<String, Object> itemMap : items) {
            if (itemMap.containsKey("requestQuantity") && itemMap.get("requestQuantity") != null) {
                totalQuantity = totalQuantity.add(new BigDecimal(itemMap.get("requestQuantity").toString()));
            }
        }
        outbound.setTotalQuantity(totalQuantity);
        // 总金额在执行出库时计算（基于实际出库数量和批次单价）
        outbound.setTotalAmount(0L);

        // 保存出库单
        this.save(outbound);

        // 保存明细
        for (Map<String, Object> itemMap : items) {
            InventoryOutboundItem item = new InventoryOutboundItem();
            item.setOutboundId(outbound.getOutboundId());
            item.setMaterialId(getStringValue(itemMap, "materialId"));
            item.setMaterialName(getStringValue(itemMap, "materialName"));
            item.setSpecification(getStringValue(itemMap, "specification"));
            item.setUnit(getStringValue(itemMap, "unit"));
            if (itemMap.containsKey("requestQuantity") && itemMap.get("requestQuantity") != null) {
                item.setRequestQuantity(new BigDecimal(itemMap.get("requestQuantity").toString()));
            }
            // 实际出库数量在执行出库时填写
            item.setActualQuantity(BigDecimal.ZERO);
            item.setBatchNo(getStringValue(itemMap, "batchNo"));
            item.setRemark(getStringValue(itemMap, "remark"));
            item.setCreateTime(LocalDateTime.now());
            item.setUpdateTime(LocalDateTime.now());
            inventoryOutboundItemMapper.insert(item);
        }

        return outbound;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryOutbound updateOutbound(String outboundCode, InventoryOutboundCreateDTO updateDTO) {
        LambdaQueryWrapper<InventoryOutbound> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InventoryOutbound::getOutboundCode, outboundCode);
        InventoryOutbound outbound = this.getOne(wrapper);
        if (outbound == null) {
            throw new RuntimeException("出库单不存在：" + outboundCode);
        }
        // 仅草稿状态（pending）允许更新，避免修改已审批/已执行的出库单
        if (!"pending".equals(outbound.getStatus())) {
            throw new RuntimeException("当前出库单状态不允许更新，仅草稿状态可更新：" + outbound.getStatus());
        }

        // 更新主表字段
        outbound.setOutboundType(updateDTO.getOutboundType());
        outbound.setWarehouseId(updateDTO.getWarehouseId());
        outbound.setTargetId(updateDTO.getTargetId());
        outbound.setTargetName(updateDTO.getTargetName());
        outbound.setReferenceNo(updateDTO.getReferenceNo());
        outbound.setOutboundDate(LocalDate.parse(updateDTO.getOutboundDate()));
        outbound.setRemark(updateDTO.getRemark());
        outbound.setUpdateTime(LocalDateTime.now());

        // 删除旧明细（明细无逻辑删除字段，物理删除后重新插入）
        LambdaQueryWrapper<InventoryOutboundItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(InventoryOutboundItem::getOutboundId, outbound.getOutboundId());
        inventoryOutboundItemMapper.delete(itemWrapper);

        // 重算总数量并保存新明细
        BigDecimal totalQuantity = BigDecimal.ZERO;
        for (InventoryOutboundCreateDTO.OutboundItemDTO itemDTO : updateDTO.getItems()) {
            InventoryOutboundItem item = new InventoryOutboundItem();
            item.setOutboundId(outbound.getOutboundId());
            item.setMaterialId(itemDTO.getMaterialId());
            item.setRequestQuantity(itemDTO.getRequestQuantity());
            item.setActualQuantity(BigDecimal.ZERO);
            item.setRemark(itemDTO.getRemark());
            item.setCreateTime(LocalDateTime.now());
            item.setUpdateTime(LocalDateTime.now());
            inventoryOutboundItemMapper.insert(item);
            if (itemDTO.getRequestQuantity() != null) {
                totalQuantity = totalQuantity.add(itemDTO.getRequestQuantity());
            }
        }
        outbound.setTotalQuantity(totalQuantity);

        this.updateById(outbound);
        return outbound;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOutbound(String outboundCode) {
        LambdaQueryWrapper<InventoryOutbound> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InventoryOutbound::getOutboundCode, outboundCode);
        InventoryOutbound outbound = this.getOne(wrapper);
        if (outbound == null) {
            throw new RuntimeException("出库单不存在：" + outboundCode);
        }
        // 仅草稿状态允许删除，防止误删已审批/已执行的出库单
        if (!"pending".equals(outbound.getStatus())) {
            throw new RuntimeException("当前出库单状态不允许删除，仅草稿状态可删除：" + outbound.getStatus());
        }

        // 删除关联明细（明细无逻辑删除字段，物理删除）
        LambdaQueryWrapper<InventoryOutboundItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(InventoryOutboundItem::getOutboundId, outbound.getOutboundId());
        inventoryOutboundItemMapper.delete(itemWrapper);

        // 业务核心数据使用逻辑删除，@TableLogic 自动将 deleted 置为 1
        this.removeById(outbound.getOutboundId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryOutbound approveOutbound(String outboundCode, Boolean approved, String opinion) {
        LambdaQueryWrapper<InventoryOutbound> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InventoryOutbound::getOutboundCode, outboundCode);
        InventoryOutbound outbound = this.getOne(wrapper);
        if (outbound == null) {
            throw new RuntimeException("出库单不存在：" + outboundCode);
        }

        if (!"pending".equals(outbound.getStatus())) {
            throw new RuntimeException("当前出库单状态不允许审批：" + outbound.getStatus());
        }

        if (approved) {
            outbound.setStatus("approved");
        } else {
            outbound.setStatus("rejected");
        }
        outbound.setApproveTime(LocalDateTime.now());
        outbound.setUpdateTime(LocalDateTime.now());
        // 从安全上下文获取当前审批人信息
        String currentUserName = getCurrentUserName();
        outbound.setApproveUserId(currentUserName);
        outbound.setApproveUserName(currentUserName);

        this.updateById(outbound);
        return outbound;
    }

    /**
     * 从安全上下文获取当前登录用户名
     * @return 当前用户名，未登录时返回空字符串
     */
    private String getCurrentUserName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return "";
        }
        return authentication.getName();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryOutbound executeOutbound(String outboundCode) {
        LambdaQueryWrapper<InventoryOutbound> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InventoryOutbound::getOutboundCode, outboundCode);
        InventoryOutbound outbound = this.getOne(wrapper);
        if (outbound == null) {
            throw new RuntimeException("出库单不存在：" + outboundCode);
        }

        if (!"approved".equals(outbound.getStatus())) {
            throw new RuntimeException("当前出库单状态不允许执行：" + outbound.getStatus());
        }

        outbound.setStatus("completed");
        outbound.setCompleteTime(LocalDateTime.now());
        outbound.setUpdateTime(LocalDateTime.now());

        // 更新明细的实际出库数量（实际出库数量=申请出库数量）并扣减库存
        LambdaQueryWrapper<InventoryOutboundItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(InventoryOutboundItem::getOutboundId, outbound.getOutboundId());
        List<InventoryOutboundItem> items = inventoryOutboundItemMapper.selectList(itemWrapper);

        long totalAmount = 0L;
        Long warehouseId = parseWarehouseId(outbound.getWarehouseId());

        for (InventoryOutboundItem item : items) {
            BigDecimal actualQty = item.getRequestQuantity() != null
                    ? item.getRequestQuantity() : BigDecimal.ZERO;
            item.setActualQuantity(actualQty);
            item.setUpdateTime(LocalDateTime.now());
            inventoryOutboundItemMapper.updateById(item);

            // 扣减实际库存
            if (actualQty.compareTo(BigDecimal.ZERO) > 0 && warehouseId != null) {
                Long materialId = parseMaterialId(item.getMaterialId());
                if (materialId != null) {
                    InventoryDecreaseDTO decreaseDTO = new InventoryDecreaseDTO();
                    decreaseDTO.setMaterialId(materialId);
                    decreaseDTO.setWarehouseId(warehouseId);
                    decreaseDTO.setQuantity(actualQty);
                    decreaseDTO.setTransactionType(getOutboundTransactionType(outbound.getOutboundType()));
                    decreaseDTO.setReferenceNo(outbound.getOutboundCode());
                    decreaseDTO.setReferenceType("inventory_outbound");
                    decreaseDTO.setRemark(item.getRemark());
                    inventoryService.decreaseInventory(decreaseDTO);
                }
            }

            // 累加总金额（单价 × 数量，分单位）
            if (item.getUnitCost() != null) {
                long itemAmount = item.getUnitCost() * actualQty.longValue();
                item.setTotalCost(itemAmount);
                totalAmount += itemAmount;
                inventoryOutboundItemMapper.updateById(item);
            }
        }
        outbound.setTotalAmount(totalAmount);

        this.updateById(outbound);

        return outbound;
    }

    /**
     * 根据出库类型映射交易类型
     * @param outboundType 出库类型（requisition/sale/return/other）
     * @return 出库交易类型：1销售出库 2领料出库 3调拨出库 4盘亏 5损耗 6其他
     */
    private Integer getOutboundTransactionType(String outboundType) {
        if (outboundType == null) return 6;
        switch (outboundType) {
            case "requisition": return 2;  // 领料出库
            case "sale": return 1;          // 销售出库
            case "return": return 6;       // 退货出库（其他）
            case "other": return 6;        // 其他
            default: return 6;
        }
    }

    /**
     * 解析物料ID（String → Long，兼容旧版字符串ID）
     * @param materialId 字符串物料ID
     * @return Long类型物料ID，无法解析时返回 null
     */
    private Long parseMaterialId(String materialId) {
        if (materialId == null || materialId.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(materialId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 解析仓库ID（String → Long，兼容旧版字符串ID）
     * @param warehouseId 字符串仓库ID
     * @return Long类型仓库ID，无法解析时返回 null
     */
    private Long parseWarehouseId(String warehouseId) {
        if (warehouseId == null || warehouseId.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(warehouseId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 将出库单实体转换为前端期望的Map格式
     */
    private Map<String, Object> convertOutboundToMap(InventoryOutbound outbound) {
        Map<String, Object> map = new HashMap<>();
        map.put("outboundId", outbound.getOutboundCode()); // 前端用业务编号作为ID
        map.put("outboundCode", outbound.getOutboundCode());
        map.put("outboundType", outbound.getOutboundType());
        map.put("outboundTypeName", getOutboundTypeName(outbound.getOutboundType()));
        map.put("warehouseId", outbound.getWarehouseId());
        map.put("warehouseName", outbound.getWarehouseName());
        map.put("targetId", outbound.getTargetId());
        map.put("targetName", outbound.getTargetName());
        map.put("referenceNo", outbound.getReferenceNo());
        map.put("outboundDate", outbound.getOutboundDate() != null
                ? outbound.getOutboundDate().toString() : "");
        map.put("totalQuantity", outbound.getTotalQuantity());
        // 金额统一以分为单位返回，由前端 DataConverter 负责分→元转换
        map.put("totalAmount", outbound.getTotalAmount());
        map.put("status", outbound.getStatus());
        map.put("statusName", getStatusName(outbound.getStatus()));
        map.put("applyUserId", outbound.getApplyUserId());
        map.put("applyUserName", outbound.getApplyUserName());
        map.put("applyTime", formatDateTime(outbound.getApplyTime()));
        map.put("approveUserId", outbound.getApproveUserId());
        map.put("approveUserName", outbound.getApproveUserName());
        map.put("approveTime", formatDateTime(outbound.getApproveTime()));
        map.put("completeTime", formatDateTime(outbound.getCompleteTime()));
        map.put("remark", outbound.getRemark());
        map.put("createTime", formatDateTime(outbound.getCreateTime()));
        map.put("updateTime", formatDateTime(outbound.getUpdateTime()));
        return map;
    }

    /**
     * 将明细实体转换为前端期望的Map格式
     */
    private Map<String, Object> convertItemToMap(InventoryOutboundItem item) {
        Map<String, Object> map = new HashMap<>();
        map.put("outboundItemId", String.valueOf(item.getOutboundItemId()));
        map.put("outboundId", String.valueOf(item.getOutboundId()));
        map.put("materialId", item.getMaterialId());
        map.put("materialName", item.getMaterialName());
        map.put("specification", item.getSpecification());
        map.put("unit", item.getUnit());
        map.put("requestQuantity", item.getRequestQuantity());
        map.put("actualQuantity", item.getActualQuantity());
        map.put("batchNo", item.getBatchNo());
        // 金额统一以分为单位返回，由前端 DataConverter 负责分→元转换
        map.put("unitCost", item.getUnitCost());
        map.put("totalCost", item.getTotalCost());
        map.put("remark", item.getRemark());
        return map;
    }

    /**
     * 获取出库类型名称
     */
    private String getOutboundTypeName(String outboundType) {
        if (outboundType == null) return "";
        switch (outboundType) {
            case "requisition": return "领料出库";
            case "sale": return "销售出库";
            case "return": return "退货出库";
            case "other": return "其他出库";
            default: return "";
        }
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(String status) {
        if (status == null) return "";
        switch (status) {
            case "pending": return "待审批";
            case "approved": return "已审批";
            case "completed": return "已出库";
            case "rejected": return "已驳回";
            default: return "";
        }
    }

    /**
     * 格式化日期时间
     */
    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 安全获取字符串值
     */
    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
}
