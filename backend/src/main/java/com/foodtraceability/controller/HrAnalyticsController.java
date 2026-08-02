package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.service.HrAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 人事分析控制器
 *
 * 提供 HR 模块的人事数据分析接口，对应前端 HRAnalytics.vue 页面。
 * 基础路径：/v1/hr/analytics
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-07-03
 */
@RestController
@RequestMapping("/v1/hr/analytics")
@Tag(name = "人事分析", description = "HR模块人事分析相关接口")
public class HrAnalyticsController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HrAnalyticsController.class);

    private final HrAnalyticsService hrAnalyticsService;

    /**
     * 构造函数注入（禁止 @Autowired 字段注入）
     *
     * @param hrAnalyticsService 人事分析服务
     */
    public HrAnalyticsController(HrAnalyticsService hrAnalyticsService) {
        this.hrAnalyticsService = hrAnalyticsService;
    }

    @GetMapping("/overview")
    @Operation(summary = "获取人事概览", description = "获取员工总数/在职/离职/本月入职/本月离职等概览数据")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'store_manager')")
    public Result<Map<String, Object>> getOverview() {
        log.info("获取人事概览数据");
        try {
            Map<String, Object> overview = hrAnalyticsService.getOverview();
            return Result.success(overview);
        } catch (Exception e) {
            log.error("获取人事概览数据失败", e);
            return Result.error("获取人事概览数据失败");
        }
    }

    @GetMapping("/departments")
    @Operation(summary = "获取部门人事统计", description = "按部门分组聚合人数/性别/薪资/流动情况，支持分页和筛选")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'store_manager')")
    public Result<Map<String, Object>> getDepartmentStats(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "部门名称筛选") @RequestParam(required = false) String department,
            @Parameter(description = "时间周期筛选，格式 yyyy-MM") @RequestParam(required = false) String period) {
        log.info("获取部门人事统计，页码：{}，每页：{}，部门：{}，周期：{}", page, pageSize, department, period);
        try {
            Map<String, Object> result = hrAnalyticsService.getDepartmentStats(page, pageSize, department, period);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取部门人事统计失败", e);
            return Result.error("获取部门人事统计失败");
        }
    }

    @GetMapping("/turnover-rate")
    @Operation(summary = "获取员工流动率分析", description = "获取入职率、离职率、净增长率及月度趋势")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'store_manager')")
    public Result<Map<String, Object>> getTurnoverRate() {
        log.info("获取员工流动率分析");
        try {
            Map<String, Object> result = hrAnalyticsService.getTurnoverRate();
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取员工流动率分析失败", e);
            return Result.error("获取员工流动率分析失败");
        }
    }

    @GetMapping("/performance")
    @Operation(summary = "获取绩效分析", description = "获取绩效分布、各部门绩效均值、绩效趋势")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'store_manager')")
    public Result<Map<String, Object>> getPerformance() {
        log.info("获取绩效分析数据");
        try {
            Map<String, Object> result = hrAnalyticsService.getPerformance();
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取绩效分析数据失败", e);
            return Result.error("获取绩效分析数据失败");
        }
    }

    @GetMapping("/training-effect")
    @Operation(summary = "获取培训效果分析", description = "获取培训完成率、培训场次、人均培训时长、培训满意度")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'store_manager')")
    public Result<Map<String, Object>> getTrainingEffect() {
        log.info("获取培训效果分析数据");
        try {
            Map<String, Object> result = hrAnalyticsService.getTrainingEffect();
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取培训效果分析数据失败", e);
            return Result.error("获取培训效果分析数据失败");
        }
    }

    @GetMapping("/recruitment-funnel")
    @Operation(summary = "获取招聘漏斗", description = "获取各招聘阶段人数：简历投递→初筛→面试→录用→入职")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'store_manager')")
    public Result<Map<String, Object>> getRecruitmentFunnel() {
        log.info("获取招聘漏斗数据");
        try {
            Map<String, Object> result = hrAnalyticsService.getRecruitmentFunnel();
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取招聘漏斗数据失败", e);
            return Result.error("获取招聘漏斗数据失败");
        }
    }

    @GetMapping("/attendance")
    @Operation(summary = "获取考勤分析", description = "获取出勤率、迟到次数、早退次数、缺勤次数、加班时长分布")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'store_manager')")
    public Result<Map<String, Object>> getAttendance() {
        log.info("获取考勤分析数据");
        try {
            Map<String, Object> result = hrAnalyticsService.getAttendance();
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取考勤分析数据失败", e);
            return Result.error("获取考勤分析数据失败");
        }
    }

    @GetMapping("/salary-distribution")
    @Operation(summary = "获取薪资分布分析", description = "获取薪资区间分布、各部门薪资均值、薪资分位数")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'store_manager')")
    public Result<Map<String, Object>> getSalaryDistribution() {
        log.info("获取薪资分布分析数据");
        try {
            Map<String, Object> result = hrAnalyticsService.getSalaryDistribution();
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取薪资分布分析数据失败", e);
            return Result.error("获取薪资分布分析数据失败");
        }
    }
}
