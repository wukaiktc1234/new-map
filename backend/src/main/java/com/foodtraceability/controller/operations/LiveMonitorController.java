package com.foodtraceability.controller.operations;

import com.foodtraceability.common.Result;
import com.foodtraceability.service.operations.LiveMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 实时监控控制器
 * 提供门店实时运营数据、趋势数据、告警列表等接口
 */
@RestController
@RequestMapping("/v1/operations/live-monitor")
@Tag(name = "实时监控", description = "门店实时运营数据监控接口")
public class LiveMonitorController {

    private static final Logger logger = LoggerFactory.getLogger(LiveMonitorController.class);

    private final LiveMonitorService liveMonitorService;

    public LiveMonitorController(LiveMonitorService liveMonitorService) {
        this.liveMonitorService = liveMonitorService;
    }

    /**
     * 实时监控总览
     * 返回门店总数、在线门店数、今日订单数、今日营收等汇总指标
     *
     * @return 总览数据
     */
    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('order:view') or hasAuthority('order:manage') or hasAuthority('*')")
    @Operation(summary = "实时监控总览", description = "返回门店总数、在线门店数、今日订单数、今日营收等汇总指标")
    public Result<Map<String, Object>> overview() {
        try {
            logger.info("获取实时监控总览数据");
            Map<String, Object> data = liveMonitorService.getOverview();
            return Result.success(data);
        } catch (Exception e) {
            logger.error("获取实时监控总览数据失败", e);
            return Result.error("获取实时监控总览数据失败");
        }
    }

    /**
     * 门店实时状态列表
     * 返回各门店的实时运营数据（营收、订单数、堂食/外卖/自提分布、翻台率、在岗人数、状态）
     *
     * @return 门店列表
     */
    @GetMapping("/stores")
    @PreAuthorize("hasAuthority('order:view') or hasAuthority('order:manage') or hasAuthority('*')")
    @Operation(summary = "门店实时状态列表", description = "返回各门店的实时运营数据")
    public Result<List<Map<String, Object>>> stores() {
        try {
            logger.info("获取门店实时状态列表");
            List<Map<String, Object>> list = liveMonitorService.getStores();
            return Result.success(list);
        } catch (Exception e) {
            logger.error("获取门店实时状态列表失败", e);
            return Result.error("获取门店实时状态列表失败");
        }
    }

    /**
     * 趋势数据
     * 根据时间维度返回营收和订单量的趋势数据
     *
     * @param dimension 时间维度：24h（24小时）/ 7d（7天）/ 30d（30天）
     * @return 趋势数据
     */
    @GetMapping("/trend")
    @PreAuthorize("hasAuthority('order:view') or hasAuthority('order:manage') or hasAuthority('*')")
    @Operation(summary = "趋势数据", description = "根据时间维度返回营收和订单量的趋势数据")
    public Result<Map<String, Object>> trend(
            @Parameter(description = "时间维度：24h/7d/30d")
            @RequestParam(defaultValue = "24h") String dimension) {
        try {
            logger.info("获取趋势数据，维度: {}", dimension);
            Map<String, Object> data = liveMonitorService.getTrend(dimension);
            return Result.success(data);
        } catch (Exception e) {
            logger.error("获取趋势数据失败", e);
            return Result.error("获取趋势数据失败");
        }
    }

    /**
     * 实时告警列表
     * 返回当前生效的告警信息
     *
     * @return 告警列表
     */
    @GetMapping("/alerts")
    @PreAuthorize("hasAuthority('order:view') or hasAuthority('order:manage') or hasAuthority('*')")
    @Operation(summary = "实时告警列表", description = "返回当前生效的告警信息")
    public Result<List<Map<String, Object>>> alerts() {
        try {
            logger.info("获取实时告警列表");
            List<Map<String, Object>> list = liveMonitorService.getAlerts();
            return Result.success(list);
        } catch (Exception e) {
            logger.error("获取实时告警列表失败", e);
            return Result.error("获取实时告警列表失败");
        }
    }
}
