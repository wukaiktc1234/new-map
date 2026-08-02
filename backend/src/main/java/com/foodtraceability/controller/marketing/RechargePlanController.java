package com.foodtraceability.controller.marketing;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.marketing.RechargePlanCreateDTO;
import com.foodtraceability.dto.marketing.RechargePlanQueryDTO;
import com.foodtraceability.dto.marketing.RechargePlanUpdateDTO;
import com.foodtraceability.dto.marketing.RechargePlanVO;
import com.foodtraceability.service.marketing.RechargePlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 充值方案管理控制器
 * 对应前端 API 路径 /v1/recharge-plans
 *
 * 端点说明：
 * 1. GET    /v1/recharge-plans           - 查询方案列表（不分页）
 * 2. GET    /v1/recharge-plans/{id}       - 获取方案详情
 * 3. POST   /v1/recharge-plans            - 创建充值方案
 * 4. PUT    /v1/recharge-plans/{id}       - 更新充值方案
 * 5. DELETE /v1/recharge-plans/{id}       - 删除充值方案
 * 6. PUT    /v1/recharge-plans/{id}/toggle-status - 切换方案状态
 *
 * 状态：active-启用 inactive-停用
 */
@RestController
@RequestMapping("/v1/recharge-plans")
@Tag(name = "充值方案管理", description = "充值方案的增删改查、状态切换")
public class RechargePlanController {

    private final RechargePlanService rechargePlanService;

    public RechargePlanController(RechargePlanService rechargePlanService) {
        this.rechargePlanService = rechargePlanService;
    }

    /**
     * 查询充值方案列表（不分页，返回全部匹配项）
     */
    @GetMapping
    @Operation(summary = "查询充值方案列表")
    public Result<List<RechargePlanVO>> getPlanList(
            @Parameter(description = "方案名称（模糊匹配）") @RequestParam(required = false) String planName,
            @Parameter(description = "方案类型: standard/activity/tiered/custom") @RequestParam(required = false) String planType,
            @Parameter(description = "状态: active/inactive") @RequestParam(required = false) String status) {
        try {
            RechargePlanQueryDTO queryDTO = new RechargePlanQueryDTO();
            queryDTO.setPlanName(planName);
            queryDTO.setPlanType(planType);
            queryDTO.setStatus(status);
            List<RechargePlanVO> list = rechargePlanService.getPlanList(queryDTO);
            return Result.success(list, "查询充值方案列表成功");
        } catch (Exception e) {
            return Result.error(500, "查询充值方案列表失败：" + e.getMessage());
        }
    }

    /**
     * 根据ID获取方案详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取充值方案详情")
    public Result<RechargePlanVO> getPlanById(
            @Parameter(description = "方案ID") @PathVariable("id") String id) {
        try {
            RechargePlanVO vo = rechargePlanService.getPlanById(id);
            if (vo == null) {
                return Result.error(404, "充值方案不存在");
            }
            return Result.success(vo, "获取充值方案详情成功");
        } catch (Exception e) {
            return Result.error(500, "获取充值方案详情失败：" + e.getMessage());
        }
    }

    /**
     * 创建充值方案
     */
    @PostMapping
    @Operation(summary = "创建充值方案")
    public Result<RechargePlanVO> createPlan(@Valid @RequestBody RechargePlanCreateDTO createDTO) {
        try {
            RechargePlanVO vo = rechargePlanService.createPlan(createDTO);
            return Result.success(vo, "创建充值方案成功");
        } catch (RuntimeException e) {
            return Result.error(4001, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "创建充值方案失败：" + e.getMessage());
        }
    }

    /**
     * 更新充值方案
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新充值方案")
    public Result<RechargePlanVO> updatePlan(
            @Parameter(description = "方案ID") @PathVariable("id") String id,
            @Valid @RequestBody RechargePlanUpdateDTO updateDTO) {
        try {
            RechargePlanVO vo = rechargePlanService.updatePlan(id, updateDTO);
            return Result.success(vo, "更新充值方案成功");
        } catch (RuntimeException e) {
            return Result.error(4002, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "更新充值方案失败：" + e.getMessage());
        }
    }

    /**
     * 删除充值方案（逻辑删除）
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除充值方案")
    public Result<Void> deletePlan(
            @Parameter(description = "方案ID") @PathVariable("id") String id) {
        try {
            rechargePlanService.deletePlan(id);
            return Result.success(null, "删除充值方案成功");
        } catch (RuntimeException e) {
            return Result.error(4003, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "删除充值方案失败：" + e.getMessage());
        }
    }

    /**
     * 切换方案状态：active ↔ inactive
     */
    @PutMapping("/{id}/toggle-status")
    @Operation(summary = "切换充值方案状态")
    public Result<Void> toggleStatus(
            @Parameter(description = "方案ID") @PathVariable("id") String id) {
        try {
            rechargePlanService.toggleStatus(id);
            return Result.success(null, "切换方案状态成功");
        } catch (RuntimeException e) {
            return Result.error(4004, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "切换方案状态失败：" + e.getMessage());
        }
    }
}
