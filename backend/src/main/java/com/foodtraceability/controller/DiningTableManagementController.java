package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dataservice.DiningTableDataService;
import com.foodtraceability.dto.store.operation.DiningTableCreateDTO;
import com.foodtraceability.dto.store.operation.DiningTableQueryDTO;
import com.foodtraceability.dto.store.operation.DiningTableUpdateDTO;
import com.foodtraceability.dto.store.operation.vo.DiningTableVO;
import com.foodtraceability.dto.store.operation.vo.TableUsageRecordVO;
import com.foodtraceability.entity.DiningTableNew;
import com.foodtraceability.service.DiningTableNewService;
import com.foodtraceability.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 门店桌台管理控制器
 * 提供桌台的增删改查、状态管理、统计等RESTful API
 */
@RestController
@RequestMapping("/v1/store-management/tables")
@Tag(name = "门店桌台管理", description = "桌台的增删改查、状态管理、使用记录")
public class DiningTableManagementController {

    private final DiningTableNewService diningTableNewService;
    private final DiningTableDataService diningTableDataService;

    /**
     * 构造函数注入
     *
     * @param diningTableNewService  桌台服务
     * @param diningTableDataService 桌台数据服务（缓存）
     */
    public DiningTableManagementController(DiningTableNewService diningTableNewService,
                                           DiningTableDataService diningTableDataService) {
        this.diningTableNewService = diningTableNewService;
        this.diningTableDataService = diningTableDataService;
    }

    /**
     * 分页查询桌台列表
     * 支持按状态、桌台类型、关键词等条件筛选
     */
    @GetMapping
    @Operation(summary = "获取桌台列表", description = "分页查询当前门店的桌台列表，支持按状态、类型、关键词筛选")
    public Result<IPage<DiningTableVO>> getTableList(DiningTableQueryDTO query) {
        Long storeId = getCurrentStoreId();
        IPage<DiningTableNew> page = diningTableNewService.listByStoreId(storeId, query);

        // 转换为VO
        List<Long> tableIds = page.getRecords().stream()
                .map(DiningTableNew::getTableId)
                .collect(Collectors.toList());
        Map<Long, DiningTableVO> voMap = diningTableDataService.batchGetDiningTableBasicInfo(tableIds);

        IPage<DiningTableVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream()
                .map(t -> voMap.get(t.getTableId()))
                .collect(Collectors.toList()));

        return Result.success(voPage);
    }

    /**
     * 获取桌台详情
     */
    @GetMapping("/{tableId}")
    @Operation(summary = "获取桌台详情", description = "根据桌台ID获取桌台的完整信息")
    public Result<DiningTableVO> getTableDetail(@PathVariable Long tableId) {
        DiningTableVO vo = diningTableDataService.getDiningTableBasicInfo(tableId);
        return Result.success(vo);
    }

    /**
     * 创建桌台
     * 将DTO转换为实体，设置门店ID后创建
     */
    @PostMapping
    @Operation(summary = "创建桌台", description = "创建新的桌台记录")
    public Result<DiningTableVO> createTable(@Valid @RequestBody DiningTableCreateDTO dto) {
        // DTO转实体
        DiningTableNew table = new DiningTableNew();
        table.setStoreId(getCurrentStoreId());
        table.setTableCode(dto.getTableCode());
        table.setTableName(dto.getTableName());
        table.setAreaId(dto.getAreaId());
        table.setSeatsCount(dto.getSeatsCount());
        table.setTableType(dto.getTableType());
        table.setMinPeople(dto.getMinPeople());
        table.setMaxPeople(dto.getMaxPeople());
        table.setSortOrder(dto.getSortOrder());

        DiningTableNew created = diningTableNewService.create(table);

        // 清除门店下桌台缓存
        diningTableDataService.clearStoreDiningTableCache(getCurrentStoreId());

        DiningTableVO vo = diningTableDataService.getDiningTableBasicInfo(created.getTableId());
        return Result.success(vo);
    }

    /**
     * 更新桌台信息
     */
    @PutMapping("/{tableId}")
    @Operation(summary = "更新桌台", description = "更新指定桌台的信息")
    public Result<DiningTableVO> updateTable(@PathVariable Long tableId,
                                             @Valid @RequestBody DiningTableUpdateDTO dto) {
        // DTO转实体
        DiningTableNew table = new DiningTableNew();
        table.setTableId(tableId);
        table.setTableName(dto.getTableName());
        table.setAreaId(dto.getAreaId());
        table.setSeatsCount(dto.getSeatsCount());
        table.setTableType(dto.getTableType());
        table.setMinPeople(dto.getMinPeople());
        table.setMaxPeople(dto.getMaxPeople());
        table.setSortOrder(dto.getSortOrder());

        diningTableNewService.update(table);

        // 清除桌台缓存
        diningTableDataService.clearDiningTableCache(tableId);

        DiningTableVO vo = diningTableDataService.getDiningTableBasicInfo(tableId);
        return Result.success(vo);
    }

    /**
     * 更新桌台状态
     * 支持设置为维护中(4)、停用(5)、空闲(1)
     */
    @PutMapping("/{tableId}/status")
    @Operation(summary = "更新桌台状态", description = "更新桌台状态，支持维护中(4)、停用(5)、空闲(1)")
    public Result<Void> updateTableStatus(@PathVariable Long tableId,
                                          @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        if (status == null) {
            return Result.error("状态参数不能为空");
        }
        // 仅允许设置为维护中(4)、停用(5)、空闲(1)
        if (status != 1 && status != 4 && status != 5) {
            return Result.error("仅支持设置为空闲(1)、维护中(4)、停用(5)");
        }

        diningTableNewService.updateStatus(tableId, status, null);

        // 清除桌台缓存
        diningTableDataService.clearDiningTableCache(tableId);

        return Result.success();
    }

    /**
     * 获取桌台统计信息
     * 返回各状态桌台数量等统计数据
     */
    @GetMapping("/stats")
    @Operation(summary = "获取桌台统计", description = "获取当前门店的桌台统计数据")
    public Result<Map<String, Object>> getTableStats() {
        Long storeId = getCurrentStoreId();
        Map<String, Object> stats = diningTableNewService.getStatsByStoreId(storeId);
        return Result.success(stats);
    }

    /**
     * 获取桌台使用记录
     * 从订单系统获取桌台的使用记录，当前为占位实现
     */
    @GetMapping("/usage-records")
    @Operation(summary = "获取桌台使用记录", description = "查询桌台的使用记录（关联订单信息），当前为占位实现")
    public Result<IPage<TableUsageRecordVO>> getUsageRecords(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Long tableId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        // 占位实现：订单系统集成后替换为真实查询
        IPage<TableUsageRecordVO> emptyPage = new Page<>(page, size, 0);
        return Result.success(emptyPage);
    }

    /**
     * 获取当前门店ID
     * 从安全上下文中获取当前登录用户的门店ID
     *
     * @return 当前门店ID
     */
    private Long getCurrentStoreId() {
        String storeIdStr = SecurityUtils.getCurrentUserStoreId();
        if (storeIdStr != null && !storeIdStr.isEmpty()) {
            try {
                return Long.parseLong(storeIdStr);
            } catch (NumberFormatException e) {
                // storeId格式异常，回退到默认值
            }
        }
        // TODO: 认证系统完善后移除此默认值，未获取到门店ID时应抛出权限异常
        return 1L;
    }
}
