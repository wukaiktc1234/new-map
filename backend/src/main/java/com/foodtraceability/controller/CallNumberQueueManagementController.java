package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dataservice.CallNumberQueueDataService;
import com.foodtraceability.dto.store.operation.CallNumberQueueQueryDTO;
import com.foodtraceability.dto.store.operation.vo.CallNumberQueueVO;
import com.foodtraceability.entity.CallNumberQueueNew;
import com.foodtraceability.service.CallNumberQueueNewService;
import com.foodtraceability.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 门店叫号记录管理控制器
 * 提供叫号记录的查询、统计等RESTful API
 */
@RestController
@RequestMapping("/v1/store-management/queue")
@Tag(name = "门店叫号记录", description = "叫号记录的查询与统计")
public class CallNumberQueueManagementController {

    private final CallNumberQueueNewService callNumberQueueNewService;
    private final CallNumberQueueDataService callNumberQueueDataService;

    /**
     * 构造函数注入
     *
     * @param callNumberQueueNewService  叫号队列服务
     * @param callNumberQueueDataService 叫号队列数据服务（缓存）
     */
    public CallNumberQueueManagementController(CallNumberQueueNewService callNumberQueueNewService,
                                               CallNumberQueueDataService callNumberQueueDataService) {
        this.callNumberQueueNewService = callNumberQueueNewService;
        this.callNumberQueueDataService = callNumberQueueDataService;
    }

    /**
     * 分页查询叫号记录
     * 支持按状态、排队类型、关键词、日期范围等条件筛选
     */
    @GetMapping("/records")
    @Operation(summary = "获取叫号记录列表", description = "分页查询当前门店的叫号记录，支持按状态、类型、关键词、日期范围筛选")
    public Result<IPage<CallNumberQueueVO>> getQueueRecords(CallNumberQueueQueryDTO query) {
        Long storeId = getCurrentStoreId();
        IPage<CallNumberQueueNew> page = callNumberQueueNewService.listByStoreId(storeId, query);

        // 转换为VO
        List<Long> queueIds = page.getRecords().stream()
                .map(CallNumberQueueNew::getQueueId)
                .collect(Collectors.toList());
        List<CallNumberQueueVO> voList = queueIds.stream()
                .map(callNumberQueueDataService::getCallNumberQueueBasicInfo)
                .collect(Collectors.toList());

        IPage<CallNumberQueueVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(voList);

        return Result.success(voPage);
    }

    /**
     * 获取叫号统计信息
     * 返回各状态排队数量、平均等待时长等统计数据
     */
    @GetMapping("/stats")
    @Operation(summary = "获取叫号统计", description = "获取当前门店的叫号排队统计数据")
    public Result<Map<String, Object>> getQueueStats() {
        Long storeId = getCurrentStoreId();
        Map<String, Object> stats = callNumberQueueDataService.getQueueStatsByStoreId(storeId);
        return Result.success(stats);
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
