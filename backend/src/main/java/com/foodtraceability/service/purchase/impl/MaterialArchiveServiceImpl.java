package com.foodtraceability.service.purchase.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.purchase.*;
import com.foodtraceability.entity.Inventory;
import com.foodtraceability.entity.MaterialArchive;
import com.foodtraceability.entity.MaterialCategory;
import com.foodtraceability.entity.Supplier;
import com.foodtraceability.entity.Warehouse;
import com.foodtraceability.mapper.MaterialArchiveMapper;
import com.foodtraceability.mapper.MaterialCategoryMapper;
import com.foodtraceability.mapper.SupplierMapper;
import com.foodtraceability.mapper.WarehouseMapper;
import com.foodtraceability.service.InventoryService;
import com.foodtraceability.service.purchase.MaterialArchiveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 商品档案 Service 实现类
 * 实现商品档案的 CRUD + 状态切换
 */
@Service
public class MaterialArchiveServiceImpl implements MaterialArchiveService {

    private static final Logger log = LoggerFactory.getLogger(MaterialArchiveServiceImpl.class);
    private static final DateTimeFormatter CODE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final MaterialArchiveMapper materialArchiveMapper;
    private final SupplierMapper supplierMapper;
    private final InventoryService inventoryService;
    private final WarehouseMapper warehouseMapper;
    private final MaterialCategoryMapper materialCategoryMapper;

    /** 分类名称缓存（categoryId → 名称，避免重复查询） */
    private final Map<Long, String> categoryNameCache = new ConcurrentHashMap<>();

    public MaterialArchiveServiceImpl(
            MaterialArchiveMapper materialArchiveMapper,
            SupplierMapper supplierMapper,
            InventoryService inventoryService,
            WarehouseMapper warehouseMapper,
            MaterialCategoryMapper materialCategoryMapper) {
        this.materialArchiveMapper = materialArchiveMapper;
        this.supplierMapper = supplierMapper;
        this.inventoryService = inventoryService;
        this.warehouseMapper = warehouseMapper;
        this.materialCategoryMapper = materialCategoryMapper;
    }

    // ==================== 分页查询 ====================

    @Override
    public PageResult<MaterialArchiveVO> getArchivePage(MaterialArchiveQueryDTO queryDTO) {
        log.debug("分页查询商品档案: current={}, size={}, keyword={}, status={}",
                queryDTO.getCurrent(), queryDTO.getSize(), queryDTO.getKeyword(), queryDTO.getStatus());

        Page<MaterialArchive> page = new Page<>(
                queryDTO.getCurrent() != null ? queryDTO.getCurrent() : 1,
                queryDTO.getSize() != null ? queryDTO.getSize() : 10
        );

        QueryWrapper<MaterialArchive> wrapper = buildQueryWrapper(queryDTO);
        Page<MaterialArchive> result = materialArchiveMapper.selectPage(page, wrapper);

        // 批量查询关联的供应商名称
        Map<Long, String> supplierNameMap = batchGetSupplierNames(result.getRecords());

        // 转换为 VO
        List<MaterialArchiveVO> voList = result.getRecords().stream()
                .map(entity -> convertToVO(entity, supplierNameMap))
                .collect(Collectors.toList());

        PageResult<MaterialArchiveVO> pageResult = new PageResult<>();
        pageResult.setRecords(voList);
        pageResult.setTotal(result.getTotal());
        pageResult.setCurrent(result.getCurrent());
        pageResult.setSize(result.getSize());
        return pageResult;
    }

    // ==================== 详情查询 ====================

    @Override
    public MaterialArchiveVO getArchiveById(Long materialId) {
        log.debug("查询商品档案详情: materialId={}", materialId);
        MaterialArchive entity = materialArchiveMapper.selectById(materialId);
        if (entity == null) {
            return null;
        }
        // 关联查询供应商名称
        Map<Long, String> supplierNameMap = batchGetSupplierNames(Collections.singletonList(entity));
        return convertToVO(entity, supplierNameMap);
    }

    // ==================== 创建 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MaterialArchiveVO createArchive(MaterialArchiveCreateDTO createDTO) {
        log.info("创建商品档案: materialName={}", createDTO.getMaterialName());

        MaterialArchive entity = new MaterialArchive();
        entity.setMaterialCode(generateMaterialCode());
        entity.setMaterialName(createDTO.getMaterialName());
        entity.setCategoryId(createDTO.getCategoryId());
        entity.setUnit(createDTO.getUnit());
        entity.setSpec(createDTO.getSpec());
        entity.setReferencePrice(createDTO.getReferencePrice());
        entity.setSupplierId(createDTO.getSupplierId());
        entity.setBarcode(createDTO.getBarcode());
        entity.setOrigin(createDTO.getOrigin());
        entity.setShelfLife(createDTO.getShelfLife());
        entity.setStorageCondition(createDTO.getStorageCondition());
        entity.setDepartmentId(createDTO.getDepartmentId());
        entity.setStatus(1); // 默认启用
        entity.setRemark(createDTO.getRemark());

        materialArchiveMapper.insert(entity);
        log.info("商品档案创建成功: materialId={}, materialCode={}",
                entity.getMaterialId(), entity.getMaterialCode());

        // 联动库存：为新增物料生成初始库存记录（current_stock=0）
        ensureInventoryForMaterial(entity);

        Map<Long, String> supplierNameMap = batchGetSupplierNames(Collections.singletonList(entity));
        return convertToVO(entity, supplierNameMap);
    }

    // ==================== 更新 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MaterialArchiveVO updateArchive(Long materialId, MaterialArchiveUpdateDTO updateDTO) {
        log.info("更新商品档案: materialId={}", materialId);

        MaterialArchive existing = materialArchiveMapper.selectById(materialId);
        if (existing == null) {
            throw new RuntimeException("商品档案不存在: ID=" + materialId);
        }

        // 仅更新非 null 字段（部分更新）
        if (updateDTO.getMaterialName() != null) existing.setMaterialName(updateDTO.getMaterialName());
        if (updateDTO.getCategoryId() != null) existing.setCategoryId(updateDTO.getCategoryId());
        if (updateDTO.getUnit() != null) existing.setUnit(updateDTO.getUnit());
        if (updateDTO.getSpec() != null) existing.setSpec(updateDTO.getSpec());
        if (updateDTO.getReferencePrice() != null) existing.setReferencePrice(updateDTO.getReferencePrice());
        if (updateDTO.getSupplierId() != null) existing.setSupplierId(updateDTO.getSupplierId());
        if (updateDTO.getBarcode() != null) existing.setBarcode(updateDTO.getBarcode());
        if (updateDTO.getOrigin() != null) existing.setOrigin(updateDTO.getOrigin());
        if (updateDTO.getShelfLife() != null) existing.setShelfLife(updateDTO.getShelfLife());
        if (updateDTO.getStorageCondition() != null) existing.setStorageCondition(updateDTO.getStorageCondition());
        if (updateDTO.getDepartmentId() != null) existing.setDepartmentId(updateDTO.getDepartmentId());
        if (updateDTO.getRemark() != null) existing.setRemark(updateDTO.getRemark());

        materialArchiveMapper.updateById(existing);
        log.info("商品档案更新成功: materialId={}", materialId);

        // 联动库存：同步更新库存记录中的物料基础字段
        syncInventoryFromMaterial(existing);

        Map<Long, String> supplierNameMap = batchGetSupplierNames(Collections.singletonList(existing));
        return convertToVO(existing, supplierNameMap);
    }

    // ==================== 删除（逻辑删除） ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteArchive(Long materialId) {
        log.info("删除商品档案: materialId={}", materialId);
        MaterialArchive existing = materialArchiveMapper.selectById(materialId);
        if (existing == null) {
            throw new RuntimeException("商品档案不存在: ID=" + materialId);
        }
        // 逻辑删除（@TableLogic 自动处理）
        materialArchiveMapper.deleteById(materialId);
        log.info("商品档案删除成功: materialId={}", materialId);
    }

    // ==================== 状态切换 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long materialId, Integer status) {
        log.info("更新商品状态: materialId={}, status={}", materialId, status);
        if (status == null || (status != 0 && status != 1)) {
            throw new RuntimeException("无效的状态值: " + status + "（仅支持 0停用 1启用）");
        }
        MaterialArchive existing = materialArchiveMapper.selectById(materialId);
        if (existing == null) {
            throw new RuntimeException("商品档案不存在: ID=" + materialId);
        }
        existing.setStatus(status);
        materialArchiveMapper.updateById(existing);
        log.info("商品状态更新成功: materialId={}, status={}", materialId, status);

        // 联动库存：停用物料时同步停用库存记录（status=0）
        syncInventoryStatus(materialId, status);
    }

    // ==================== 内部辅助方法 ====================

    /**
     * 构建查询条件包装器
     */
    private QueryWrapper<MaterialArchive> buildQueryWrapper(MaterialArchiveQueryDTO queryDTO) {
        QueryWrapper<MaterialArchive> wrapper = new QueryWrapper<>();

        if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().isEmpty()) {
            String kw = queryDTO.getKeyword().trim();
            wrapper.and(w -> w.like("material_name", kw).or().like("material_code", kw));
        }
        if (queryDTO.getCategoryId() != null) {
            wrapper.eq("category_id", queryDTO.getCategoryId());
        }
        // 按使用部门过滤：匹配指定部门或通用物料（department_id 为空）
        if (queryDTO.getDepartmentId() != null) {
            wrapper.and(w -> w.eq("department_id", queryDTO.getDepartmentId())
                    .or().isNull("department_id"));
        }
        // 注意：categoryName 需要 JOIN material_categories 表查询，该表暂未创建
        // 下个子模块补建 material_categories 后，此处改为 JOIN 查询
        if (queryDTO.getCategoryName() != null && !queryDTO.getCategoryName().isEmpty()) {
            log.warn("categoryName 查询暂未支持（material_categories 表未创建）: {}", queryDTO.getCategoryName());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq("status", queryDTO.getStatus());
        }
        // 按创建时间倒序
        wrapper.orderByDesc("create_time");
        return wrapper;
    }

    /**
     * 生成商品编码
     * 格式：MAT + yyyyMMdd + 6位随机序号
     * 示例：MAT20260629123456
     */
    private String generateMaterialCode() {
        String datePart = LocalDateTime.now().format(CODE_DATE_FORMATTER);
        String randomPart = String.format("%06d", new Random().nextInt(1000000));
        return "MAT" + datePart + randomPart;
    }

    /**
     * 批量查询供应商名称（避免 N+1 问题）
     * @param archives 商品档案列表
     * @return Map<supplierId, supplierName>
     */
    private Map<Long, String> batchGetSupplierNames(List<MaterialArchive> archives) {
        if (archives == null || archives.isEmpty()) {
            return Collections.emptyMap();
        }
        // 收集所有非 null 的 supplierId
        Set<Long> supplierIds = archives.stream()
                .map(MaterialArchive::getSupplierId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (supplierIds.isEmpty()) {
            return Collections.emptyMap();
        }
        // 批量查询
        List<Supplier> suppliers = supplierMapper.selectBatchIds(supplierIds);
        return suppliers.stream()
                .collect(Collectors.toMap(
                        Supplier::getSupplierId,
                        Supplier::getSupplierName,
                        (a, b) -> a // 冲突时保留第一个
                ));
    }

    /**
     * Entity 转 VO（含关联字段）
     * @param entity 商品档案实体
     * @param supplierNameMap 供应商名称映射（避免重复查询）
     */
    private MaterialArchiveVO convertToVO(MaterialArchive entity, Map<Long, String> supplierNameMap) {
        MaterialArchiveVO vo = new MaterialArchiveVO();
        vo.setMaterialId(entity.getMaterialId());
        vo.setMaterialCode(entity.getMaterialCode());
        vo.setMaterialName(entity.getMaterialName());
        vo.setCategoryId(entity.getCategoryId());
        // 关联查询分类名称（material_categories 表已建立）
        if (entity.getCategoryId() != null) {
            vo.setCategoryName(categoryNameCache.computeIfAbsent(entity.getCategoryId(), this::loadCategoryName));
        }
        vo.setUnit(entity.getUnit());
        vo.setSpec(entity.getSpec());
        vo.setReferencePrice(entity.getReferencePrice());
        vo.setBarcode(entity.getBarcode());
        vo.setOrigin(entity.getOrigin());
        vo.setShelfLife(entity.getShelfLife());
        vo.setStorageCondition(entity.getStorageCondition());
        vo.setDepartmentId(entity.getDepartmentId());
        vo.setSupplierId(entity.getSupplierId());
        // 从映射中取供应商名称
        vo.setSupplierName(
                entity.getSupplierId() != null ? supplierNameMap.get(entity.getSupplierId()) : null
        );
        vo.setStatus(entity.getStatus());
        vo.setStatusName(statusToName(entity.getStatus()));
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    /**
     * 分类ID → 分类名称（带缓存）
     */
    private String loadCategoryName(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        try {
            MaterialCategory category = materialCategoryMapper.selectById(categoryId);
            return category != null ? category.getCategoryName() : null;
        } catch (Exception e) {
            log.warn("查询商品分类名称失败：categoryId={}, 错误={}", categoryId, e.getMessage());
            return null;
        }
    }

    /**
     * 状态码转中文名
     * @param status 1启用 0停用
     */    private String statusToName(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 1 -> "启用";
            case 0 -> "停用";
            default -> "未知";
        };
    }

    // ==================== 库存联动方法 ====================

    /**
     * 确保物料档案存在对应的库存初始记录
     * <p>若该物料在默认仓库下尚无库存记录，则新建一条 current_stock=0 的库存记录。
     * 已存在记录时不重复创建。</p>
     *
     * @param material 物料档案实体
     */
    @Override
    public void ensureInventoryForMaterial(MaterialArchive material) {
        if (material == null || material.getMaterialId() == null) {
            return;
        }

        Warehouse defaultWarehouse = getDefaultWarehouse();
        if (defaultWarehouse == null || defaultWarehouse.getWarehouseId() == null) {
            log.warn("未找到默认仓库，跳过物料[{}]的库存初始化", material.getMaterialId());
            return;
        }

        Long warehouseId = defaultWarehouse.getWarehouseId();
        Inventory existing = inventoryService.getByMaterialAndWarehouse(material.getMaterialId(), warehouseId);
        if (existing != null) {
            log.debug("物料[{}]在仓库[{}]已存在库存记录，跳过初始化", material.getMaterialId(), warehouseId);
            return;
        }

        Inventory inventory = new Inventory();
        inventory.setMaterialId(material.getMaterialId());
        inventory.setMaterialName(material.getMaterialName());
        inventory.setMaterialCode(material.getMaterialCode());
        inventory.setMaterialCategoryId(material.getCategoryId());
        inventory.setSpecification(material.getSpec());
        inventory.setUnit(material.getUnit());
        inventory.setWarehouseId(warehouseId);
        inventory.setQuantity(BigDecimal.ZERO);
        inventory.setLockedQuantity(BigDecimal.ZERO);
        inventory.setStatus(material.getStatus() != null && material.getStatus() == 1 ? 1 : 0);
        inventory.setCreateTime(LocalDateTime.now());
        inventory.setUpdateTime(LocalDateTime.now());

        inventoryService.save(inventory);
        log.info("物料档案联动创建库存初始记录成功: materialId={}, warehouseId={}",
                material.getMaterialId(), warehouseId);
    }

    /**
     * 同步更新库存记录中的物料基础字段
     *
     * @param material 物料档案实体
     */
    private void syncInventoryFromMaterial(MaterialArchive material) {
        if (material == null || material.getMaterialId() == null) {
            return;
        }

        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Inventory::getMaterialId, material.getMaterialId());
        List<Inventory> inventories = inventoryService.list(wrapper);
        if (inventories.isEmpty()) {
            // 无库存记录时，自动创建一条初始记录
            ensureInventoryForMaterial(material);
            return;
        }

        for (Inventory inventory : inventories) {
            inventory.setMaterialName(material.getMaterialName());
            inventory.setMaterialCode(material.getMaterialCode());
            inventory.setMaterialCategoryId(material.getCategoryId());
            inventory.setSpecification(material.getSpec());
            inventory.setUnit(material.getUnit());
            inventory.setUpdateTime(LocalDateTime.now());
            inventoryService.updateById(inventory);
        }
        log.info("物料档案联动更新库存记录成功: materialId={}, 影响记录数={}",
                material.getMaterialId(), inventories.size());
    }

    /**
     * 同步更新库存记录状态
     *
     * @param materialId 物料ID
     * @param status 物料档案状态（1启用 0停用）
     */
    private void syncInventoryStatus(Long materialId, Integer status) {
        if (materialId == null || status == null) {
            return;
        }

        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Inventory::getMaterialId, materialId);
        List<Inventory> inventories = inventoryService.list(wrapper);
        if (inventories.isEmpty()) {
            return;
        }

        Integer inventoryStatus = status == 1 ? 1 : 0;
        for (Inventory inventory : inventories) {
            inventory.setStatus(inventoryStatus);
            inventory.setUpdateTime(LocalDateTime.now());
            inventoryService.updateById(inventory);
        }
        log.info("物料档案状态联动更新库存状态成功: materialId={}, status={}, 影响记录数={}",
                materialId, inventoryStatus, inventories.size());
    }

    /**
     * 获取默认仓库
     * <p>优先返回第一个 status=1 的仓库，无记录时返回 null。</p>
     *
     * @return 默认仓库
     */
    private Warehouse getDefaultWarehouse() {
        LambdaQueryWrapper<Warehouse> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Warehouse::getStatus, 1);
        wrapper.orderByAsc(Warehouse::getWarehouseId);
        wrapper.last("LIMIT 1");
        return warehouseMapper.selectOne(wrapper);
    }
}
