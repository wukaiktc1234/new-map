package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.InventoryAdjustCreateDTO;
import com.foodtraceability.dto.InventoryDecreaseDTO;
import com.foodtraceability.dto.InventoryIncreaseDTO;
import com.foodtraceability.entity.InventoryAdjust;
import com.foodtraceability.entity.InventoryAdjustItem;
import com.foodtraceability.mapper.InventoryAdjustItemMapper;
import com.foodtraceability.mapper.InventoryAdjustMapper;
import com.foodtraceability.service.InventoryAdjustService;
import com.foodtraceability.service.InventoryService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 库存调整单服务实现类
 * 实现库存调整管理相关的业务方法
 *
 * 类级 @Transactional：所有写操作均纳入事务管理，确保库存增减与状态更新的原子性。
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class InventoryAdjustServiceImpl extends ServiceImpl<InventoryAdjustMapper, InventoryAdjust> implements InventoryAdjustService {

    private final InventoryAdjustMapper inventoryAdjustMapper;
    private final InventoryAdjustItemMapper inventoryAdjustItemMapper;
    private final InventoryService inventoryService;

    public InventoryAdjustServiceImpl(InventoryAdjustMapper inventoryAdjustMapper,
                                       InventoryAdjustItemMapper inventoryAdjustItemMapper,
                                       InventoryService inventoryService) {
        this.inventoryAdjustMapper = inventoryAdjustMapper;
        this.inventoryAdjustItemMapper = inventoryAdjustItemMapper;
        this.inventoryService = inventoryService;
    }

    @Override
    public IPage<Map<String, Object>> getAdjustPage(Page<InventoryAdjust> page,
                                                     String adjustType,
                                                     String status,
                                                     String warehouseId,
                                                     String keyword,
                                                     String startDate,
                                                     String endDate) {
        IPage<InventoryAdjust> adjustPage = inventoryAdjustMapper.selectAdjustPage(
                page, adjustType, status, warehouseId, keyword, startDate, endDate);

        // 转换为前端期望的格式（含明细数量等附加字段）
        return adjustPage.convert(this::convertAdjustToMap);
    }

    @Override
    public Map<String, Object> getAdjustByCode(String adjustCode) {
        LambdaQueryWrapper<InventoryAdjust> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InventoryAdjust::getAdjustCode, adjustCode);
        InventoryAdjust adjust = this.getOne(wrapper);
        if (adjust == null) {
            return null;
        }

        Map<String, Object> result = convertAdjustToMap(adjust);

        // 查询明细列表
        LambdaQueryWrapper<InventoryAdjustItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(InventoryAdjustItem::getAdjustId, adjust.getAdjustId());
        List<InventoryAdjustItem> items = inventoryAdjustItemMapper.selectList(itemWrapper);

        List<Map<String, Object>> itemList = items.stream()
                .map(this::convertItemToMap)
                .collect(Collectors.toList());
        result.put("items", itemList);

        // 构建审批历史（简化版，基于状态流转）
        List<Map<String, Object>> approvalHistory = buildApprovalHistory(adjust);
        result.put("approvalHistory", approvalHistory);

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryAdjust createAdjust(InventoryAdjust adjust, List<Map<String, Object>> items) {
        // 生成调整单号：AD + yyyyMMddHHmmss
        String adjustCode = "AD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        adjust.setAdjustCode(adjustCode);
        adjust.setStatus("pending");
        adjust.setApplyTime(LocalDateTime.now());
        adjust.setCreateTime(LocalDateTime.now());
        adjust.setUpdateTime(LocalDateTime.now());

        // 计算总数量和总金额
        BigDecimal totalQuantity = BigDecimal.ZERO;
        long totalAmount = 0L;
        for (Map<String, Object> itemMap : items) {
            BigDecimal adjustQuantity = new BigDecimal(itemMap.get("adjustQuantity").toString());
            totalQuantity = totalQuantity.add(adjustQuantity);
            if (itemMap.containsKey("adjustAmount") && itemMap.get("adjustAmount") != null) {
                totalAmount += Long.parseLong(itemMap.get("adjustAmount").toString());
            }
        }
        adjust.setTotalAdjustQuantity(totalQuantity);
        adjust.setTotalAdjustAmount(totalAmount);

        // 保存调整单
        this.save(adjust);

        // 保存明细
        List<InventoryAdjustItem> itemEntities = new ArrayList<>();
        for (Map<String, Object> itemMap : items) {
            InventoryAdjustItem item = new InventoryAdjustItem();
            item.setAdjustId(adjust.getAdjustId());
            item.setMaterialId(getStringValue(itemMap, "materialId"));
            item.setMaterialName(getStringValue(itemMap, "materialName"));
            item.setSpecification(getStringValue(itemMap, "specification"));
            item.setUnit(getStringValue(itemMap, "unit"));
            if (itemMap.containsKey("beforeQuantity") && itemMap.get("beforeQuantity") != null) {
                item.setBeforeQuantity(new BigDecimal(itemMap.get("beforeQuantity").toString()));
            }
            if (itemMap.containsKey("adjustQuantity") && itemMap.get("adjustQuantity") != null) {
                BigDecimal adjustQty = new BigDecimal(itemMap.get("adjustQuantity").toString());
                item.setAdjustQuantity(adjustQty);
                // 计算调整后数量
                if (item.getBeforeQuantity() != null) {
                    item.setAfterQuantity(item.getBeforeQuantity().add(adjustQty));
                }
            }
            if (itemMap.containsKey("unitCost") && itemMap.get("unitCost") != null) {
                // 元转分
                BigDecimal unitCostYuan = new BigDecimal(itemMap.get("unitCost").toString());
                item.setUnitCost(unitCostYuan.multiply(new BigDecimal("100")).longValue());
            }
            if (itemMap.containsKey("adjustAmount") && itemMap.get("adjustAmount") != null) {
                BigDecimal adjustAmountYuan = new BigDecimal(itemMap.get("adjustAmount").toString());
                item.setAdjustAmount(adjustAmountYuan.multiply(new BigDecimal("100")).longValue());
            }
            item.setBatchNo(getStringValue(itemMap, "batchNo"));
            item.setReason(getStringValue(itemMap, "reason"));
            item.setCreateTime(LocalDateTime.now());
            item.setUpdateTime(LocalDateTime.now());
            itemEntities.add(item);
        }

        for (InventoryAdjustItem item : itemEntities) {
            inventoryAdjustItemMapper.insert(item);
        }

        return adjust;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryAdjust updateAdjust(String adjustCode, InventoryAdjustCreateDTO updateDTO) {
        LambdaQueryWrapper<InventoryAdjust> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InventoryAdjust::getAdjustCode, adjustCode);
        InventoryAdjust adjust = this.getOne(wrapper);
        if (adjust == null) {
            throw new RuntimeException("调整单不存在：" + adjustCode);
        }
        // 仅草稿状态（pending）允许更新，避免修改已审批/已执行的调整单
        if (!"pending".equals(adjust.getStatus())) {
            throw new RuntimeException("当前调整单状态不允许更新，仅草稿状态可更新：" + adjust.getStatus());
        }

        // 更新主表字段
        adjust.setAdjustType(updateDTO.getAdjustType());
        adjust.setWarehouseId(updateDTO.getWarehouseId());
        adjust.setReferenceCheckCode(updateDTO.getReferenceCheckCode());
        adjust.setAdjustReason(updateDTO.getAdjustReason());
        adjust.setReferenceNo(updateDTO.getReferenceNo());
        adjust.setRemark(updateDTO.getRemark());
        if (updateDTO.getParticipatingDepts() != null && !updateDTO.getParticipatingDepts().isEmpty()) {
            adjust.setParticipatingDepts(updateDTO.getParticipatingDepts().toString());
        } else {
            adjust.setParticipatingDepts(null);
        }
        adjust.setUpdateTime(LocalDateTime.now());

        // 删除旧明细（明细无逻辑删除字段，物理删除后重新插入）
        LambdaQueryWrapper<InventoryAdjustItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(InventoryAdjustItem::getAdjustId, adjust.getAdjustId());
        inventoryAdjustItemMapper.delete(itemWrapper);

        // 重算总数量和总金额，并保存新明细
        BigDecimal totalQuantity = BigDecimal.ZERO;
        long totalAmount = 0L;
        for (InventoryAdjustCreateDTO.AdjustItemDTO itemDTO : updateDTO.getItems()) {
            InventoryAdjustItem item = new InventoryAdjustItem();
            item.setAdjustId(adjust.getAdjustId());
            item.setMaterialId(itemDTO.getMaterialId());
            item.setMaterialName(itemDTO.getMaterialName());
            item.setSpecification(itemDTO.getSpecification());
            item.setUnit(itemDTO.getUnit());
            if (itemDTO.getBeforeQuantity() != null) {
                item.setBeforeQuantity(itemDTO.getBeforeQuantity());
            }
            BigDecimal adjustQty = itemDTO.getAdjustQuantity();
            if (adjustQty != null) {
                item.setAdjustQuantity(adjustQty);
                if (item.getBeforeQuantity() != null) {
                    item.setAfterQuantity(item.getBeforeQuantity().add(adjustQty));
                }
                totalQuantity = totalQuantity.add(adjustQty);
            }
            if (itemDTO.getUnitCost() != null) {
                BigDecimal unitCostYuan = new BigDecimal(itemDTO.getUnitCost());
                item.setUnitCost(unitCostYuan.multiply(new BigDecimal("100")).longValue());
                if (adjustQty != null) {
                    BigDecimal adjustAmountYuan = unitCostYuan.multiply(adjustQty);
                    item.setAdjustAmount(adjustAmountYuan.multiply(new BigDecimal("100")).longValue());
                    totalAmount += item.getAdjustAmount();
                }
            }
            item.setBatchNo(itemDTO.getBatchNo());
            item.setReason(itemDTO.getReason());
            item.setCreateTime(LocalDateTime.now());
            item.setUpdateTime(LocalDateTime.now());
            inventoryAdjustItemMapper.insert(item);
        }
        adjust.setTotalAdjustQuantity(totalQuantity);
        adjust.setTotalAdjustAmount(totalAmount);

        this.updateById(adjust);
        return adjust;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAdjust(String adjustCode) {
        LambdaQueryWrapper<InventoryAdjust> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InventoryAdjust::getAdjustCode, adjustCode);
        InventoryAdjust adjust = this.getOne(wrapper);
        if (adjust == null) {
            throw new RuntimeException("调整单不存在：" + adjustCode);
        }
        // 仅草稿状态允许删除，防止误删已审批/已执行的调整单
        if (!"pending".equals(adjust.getStatus())) {
            throw new RuntimeException("当前调整单状态不允许删除，仅草稿状态可删除：" + adjust.getStatus());
        }

        // 删除关联明细（明细无逻辑删除字段，物理删除）
        LambdaQueryWrapper<InventoryAdjustItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(InventoryAdjustItem::getAdjustId, adjust.getAdjustId());
        inventoryAdjustItemMapper.delete(itemWrapper);

        // 业务核心数据使用逻辑删除，@TableLogic 自动将 deleted 置为 1
        this.removeById(adjust.getAdjustId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryAdjust approveAdjust(String adjustCode, Boolean approved, String opinion) {
        LambdaQueryWrapper<InventoryAdjust> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InventoryAdjust::getAdjustCode, adjustCode);
        InventoryAdjust adjust = this.getOne(wrapper);
        if (adjust == null) {
            throw new RuntimeException("调整单不存在：" + adjustCode);
        }

        if (!"pending".equals(adjust.getStatus())) {
            throw new RuntimeException("当前调整单状态不允许审批：" + adjust.getStatus());
        }

        if (approved) {
            adjust.setStatus("approved");
        } else {
            adjust.setStatus("rejected");
        }
        adjust.setApproveTime(LocalDateTime.now());
        adjust.setUpdateTime(LocalDateTime.now());
        // 从安全上下文获取当前审批人信息
        String currentUserName = getCurrentUserName();
        adjust.setApproveUserId(currentUserName);
        adjust.setApproveUserName(currentUserName);

        this.updateById(adjust);
        return adjust;
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
    public InventoryAdjust executeAdjust(String adjustCode) {
        LambdaQueryWrapper<InventoryAdjust> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InventoryAdjust::getAdjustCode, adjustCode);
        InventoryAdjust adjust = this.getOne(wrapper);
        if (adjust == null) {
            throw new RuntimeException("调整单不存在：" + adjustCode);
        }

        if (!"approved".equals(adjust.getStatus())) {
            throw new RuntimeException("当前调整单状态不允许执行：" + adjust.getStatus());
        }

        // 查询调整明细，遍历执行库存增减
        LambdaQueryWrapper<InventoryAdjustItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(InventoryAdjustItem::getAdjustId, adjust.getAdjustId());
        List<InventoryAdjustItem> items = inventoryAdjustItemMapper.selectList(itemWrapper);

        for (InventoryAdjustItem item : items) {
            BigDecimal adjustQty = item.getAdjustQuantity();
            if (adjustQty == null || adjustQty.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            Long materialId = parseMaterialId(item.getMaterialId());
            Long warehouseId = parseWarehouseId(adjust.getWarehouseId());
            if (materialId == null || warehouseId == null) {
                continue;
            }

            if (adjustQty.compareTo(BigDecimal.ZERO) > 0) {
                // 调整数量为正：增加库存（盘盈入库）
                InventoryIncreaseDTO increaseDTO = new InventoryIncreaseDTO();
                increaseDTO.setMaterialId(materialId);
                increaseDTO.setWarehouseId(warehouseId);
                increaseDTO.setQuantity(adjustQty);
                increaseDTO.setUnitCost(item.getUnitCost());
                increaseDTO.setBatchNo(item.getBatchNo());
                increaseDTO.setTransactionType(getIncreaseTransactionType(adjust.getAdjustType()));
                increaseDTO.setReferenceNo(adjust.getAdjustCode());
                increaseDTO.setReferenceType("inventory_adjust");
                increaseDTO.setRemark(item.getReason());
                inventoryService.increaseInventory(increaseDTO);
            } else {
                // 调整数量为负：减少库存（盘亏出库）
                InventoryDecreaseDTO decreaseDTO = new InventoryDecreaseDTO();
                decreaseDTO.setMaterialId(materialId);
                decreaseDTO.setWarehouseId(warehouseId);
                decreaseDTO.setQuantity(adjustQty.negate());
                decreaseDTO.setTransactionType(getDecreaseTransactionType(adjust.getAdjustType()));
                decreaseDTO.setReferenceNo(adjust.getAdjustCode());
                decreaseDTO.setReferenceType("inventory_adjust");
                decreaseDTO.setRemark(item.getReason());
                inventoryService.decreaseInventory(decreaseDTO);
            }
        }

        adjust.setStatus("completed");
        adjust.setCompleteTime(LocalDateTime.now());
        adjust.setUpdateTime(LocalDateTime.now());

        this.updateById(adjust);

        return adjust;
    }

    /**
     * 根据调整类型映射入库交易类型
     * @param adjustType 调整类型（gain/loss/temp_loss/weight_diff/other）
     * @return 入库交易类型：1采购入库 2调拨入库 3盘盈 4退货入库 5其他
     */
    private Integer getIncreaseTransactionType(String adjustType) {
        if (adjustType == null) return 5;
        switch (adjustType) {
            case "gain": return 3;        // 盘盈
            case "weight_diff": return 3; // 称重差异正向
            case "other": return 5;       // 其他
            default: return 5;
        }
    }

    /**
     * 根据调整类型映射出库交易类型
     * @param adjustType 调整类型（gain/loss/temp_loss/weight_diff/other）
     * @return 出库交易类型：1销售出库 2领料出库 3调拨出库 4盘亏 5损耗 6其他
     */
    private Integer getDecreaseTransactionType(String adjustType) {
        if (adjustType == null) return 6;
        switch (adjustType) {
            case "loss": return 4;          // 盘亏
            case "temp_loss": return 5;     // 温度损耗
            case "weight_diff": return 4;   // 称重差异负向（视同盘亏）
            case "other": return 6;         // 其他
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
     * 将调整单实体转换为前端期望的Map格式
     */
    private Map<String, Object> convertAdjustToMap(InventoryAdjust adjust) {
        Map<String, Object> map = new HashMap<>();
        map.put("adjustId", adjust.getAdjustCode()); // 前端用业务编号作为ID
        map.put("adjustCode", adjust.getAdjustCode());
        map.put("adjustType", adjust.getAdjustType());
        map.put("adjustTypeName", getAdjustTypeName(adjust.getAdjustType()));
        map.put("warehouseId", adjust.getWarehouseId());
        map.put("warehouseName", adjust.getWarehouseName());
        map.put("referenceCheckCode", adjust.getReferenceCheckCode());
        map.put("adjustReason", adjust.getAdjustReason());
        map.put("referenceNo", adjust.getReferenceNo());
        map.put("totalAdjustQuantity", adjust.getTotalAdjustQuantity());
        // 金额统一以分为单位返回，由前端 DataConverter 负责分→元转换
        map.put("totalAdjustAmount", adjust.getTotalAdjustAmount());
        map.put("status", adjust.getStatus());
        map.put("statusName", getStatusName(adjust.getStatus()));
        map.put("participatingDepts", parseParticipatingDepts(adjust.getParticipatingDepts()));
        map.put("applyUserId", adjust.getApplyUserId());
        map.put("applyUserName", adjust.getApplyUserName());
        map.put("applyTime", formatDateTime(adjust.getApplyTime()));
        map.put("approveUserId", adjust.getApproveUserId());
        map.put("approveUserName", adjust.getApproveUserName());
        map.put("approveTime", formatDateTime(adjust.getApproveTime()));
        map.put("completeTime", formatDateTime(adjust.getCompleteTime()));
        map.put("remark", adjust.getRemark());
        map.put("createTime", formatDateTime(adjust.getCreateTime()));
        map.put("updateTime", formatDateTime(adjust.getUpdateTime()));
        return map;
    }

    /**
     * 将明细实体转换为前端期望的Map格式
     */
    private Map<String, Object> convertItemToMap(InventoryAdjustItem item) {
        Map<String, Object> map = new HashMap<>();
        map.put("adjustItemId", String.valueOf(item.getAdjustItemId()));
        map.put("adjustId", String.valueOf(item.getAdjustId()));
        map.put("materialId", item.getMaterialId());
        map.put("materialName", item.getMaterialName());
        map.put("specification", item.getSpecification());
        map.put("unit", item.getUnit());
        map.put("beforeQuantity", item.getBeforeQuantity());
        map.put("adjustQuantity", item.getAdjustQuantity());
        map.put("afterQuantity", item.getAfterQuantity());
        // 金额统一以分为单位返回，由前端 DataConverter 负责分→元转换
        map.put("unitCost", item.getUnitCost());
        map.put("adjustAmount", item.getAdjustAmount());
        map.put("batchNo", item.getBatchNo());
        map.put("reason", item.getReason());
        return map;
    }

    /**
     * 构建审批历史（基于状态流转）
     */
    private List<Map<String, Object>> buildApprovalHistory(InventoryAdjust adjust) {
        List<Map<String, Object>> history = new ArrayList<>();

        // 提交步骤
        Map<String, Object> submit = new HashMap<>();
        submit.put("step", "submit");
        submit.put("userName", adjust.getApplyUserName());
        submit.put("time", formatDateTime(adjust.getApplyTime()));
        submit.put("comment", adjust.getRemark() != null ? adjust.getRemark() : "");
        history.add(submit);

        // 审批步骤
        if (adjust.getApproveTime() != null) {
            Map<String, Object> approve = new HashMap<>();
            if ("rejected".equals(adjust.getStatus())) {
                approve.put("step", "reject");
            } else {
                approve.put("step", "approve");
            }
            approve.put("userName", adjust.getApproveUserName());
            approve.put("time", formatDateTime(adjust.getApproveTime()));
            approve.put("comment", "");
            history.add(approve);
        }

        // 执行步骤
        if (adjust.getCompleteTime() != null) {
            Map<String, Object> execute = new HashMap<>();
            execute.put("step", "execute");
            execute.put("userName", adjust.getApplyUserName());
            execute.put("time", formatDateTime(adjust.getCompleteTime()));
            execute.put("comment", "已执行库存调整");
            history.add(execute);
        }

        return history;
    }

    /**
     * 获取调整类型名称
     */
    private String getAdjustTypeName(String adjustType) {
        if (adjustType == null) return "";
        switch (adjustType) {
            case "gain": return "盘盈调整";
            case "loss": return "盘亏调整";
            case "temp_loss": return "温度损耗";
            case "weight_diff": return "称重差异";
            case "other": return "其他调整";
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
            case "completed": return "已完成";
            case "rejected": return "已驳回";
            default: return "";
        }
    }

    /**
     * 解析参与部门JSON字符串为列表
     */
    private List<String> parseParticipatingDepts(String deptsJson) {
        if (deptsJson == null || deptsJson.isEmpty()) {
            return new ArrayList<>();
        }
        // 简化处理：去除方括号和引号，按逗号分割
        String cleaned = deptsJson.replaceAll("[\\[\\]\"]", "");
        if (cleaned.isEmpty()) {
            return new ArrayList<>();
        }
        return List.of(cleaned.split(","));
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
