package com.foodtraceability.controller.seal;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.seal.SealCreateDTO;
import com.foodtraceability.dto.seal.SealUpdateDTO;
import com.foodtraceability.dto.seal.SealUsageRecordDTO;
import com.foodtraceability.dto.seal.SealVO;
import com.foodtraceability.entity.seal.SealUsageLog;
import com.foodtraceability.service.seal.SealService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 电子签章管理控制器
 * 提供印章的增删改查、状态管理、授权查询及使用记录管理接口
 *
 * <p>路径前缀：/v1/seals
 *
 * <p>注意：字面量路径(/page、/authorized、/usage-logs)在 /{id} 之前定义，
 * 避免路径变量匹配冲突。
 */
@Tag(name = "电子签章-印章管理", description = "电子签章模块印章相关接口")
@RestController
@RequestMapping("/v1/seals")
public class SealController {

    private final SealService sealService;

    public SealController(SealService sealService) {
        this.sealService = sealService;
    }

    // ==================== 字面量路径(必须在 /{id} 之前) ====================

    /**
     * 分页查询印章列表
     * 支持按印章类型、状态筛选，按印章名称或保管人关键词搜索
     */
    @Operation(summary = "查询印章列表",
            description = "分页查询印章列表，支持按印章类型、状态筛选及关键词搜索")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('seal:view') or hasAuthority('*')")
    public Result<PageResult<SealVO>> getSealList(
            @Parameter(description = "页码(从1开始)") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页条数") @RequestParam(required = false) Integer size,
            @Parameter(description = "印章类型(official/finance/contract/legal/custom)")
            @RequestParam(required = false) String sealType,
            @Parameter(description = "状态(active/inactive/revoked)")
            @RequestParam(required = false) String status,
            @Parameter(description = "关键词(匹配印章名称或保管人)")
            @RequestParam(required = false) String keyword) {
        try {
            PageResult<Map<String, Object>> result =
                    sealService.getSealList(page, size, sealType, status, keyword);
            // 转换为VO
            List<SealVO> voList = result.getRecords().stream()
                    .map(this::mapToSealVO)
                    .collect(Collectors.toList());
            PageResult<SealVO> voResult = new PageResult<>(result.getTotal(), voList, result.getPage(), result.getSize());
            return Result.success(voResult);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取当前用户已授权的印章(用于签署时选择)
     */
    @Operation(summary = "获取已授权印章",
            description = "根据使用场景和用户ID获取已授权的启用状态印章列表")
    @GetMapping("/authorized")
    @PreAuthorize("hasAuthority('seal:view') or hasAuthority('*')")
    public Result<List<SealVO>> getAuthorized(
            @Parameter(description = "使用场景(hr_contract/purchase_contract/electronic_contract)")
            @RequestParam String scene,
            @Parameter(description = "用户ID") @RequestParam(required = false) String userId) {
        try {
            List<Map<String, Object>> list = sealService.getAuthorizedSeals(scene, userId);
            List<SealVO> voList = list.stream().map(this::mapToSealVO).collect(Collectors.toList());
            return Result.success(voList);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 分页查询印章使用记录
     */
    @Operation(summary = "查询印章使用记录",
            description = "分页查询印章使用记录，支持按印章ID和业务类型筛选")
    @GetMapping("/usage-logs")
    @PreAuthorize("hasAuthority('seal:view') or hasAuthority('*')")
    public Result<PageResult<SealUsageLog>> getUsageLogs(
            @Parameter(description = "页码(从1开始)") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页条数") @RequestParam(required = false) Integer size,
            @Parameter(description = "印章ID") @RequestParam(required = false) String sealId,
            @Parameter(description = "业务类型(hr_contract/purchase_contract/electronic_contract)")
            @RequestParam(required = false) String businessType) {
        try {
            PageResult<SealUsageLog> result =
                    sealService.getUsageLogs(page, size, sealId, businessType);
            return Result.success(result);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 记录印章使用(签署时调用)
     */
    @Operation(summary = "记录印章使用",
            description = "记录一次印章使用，用于审计追溯。自动捕获操作人IP地址")
    @PostMapping("/usage-logs")
    @PreAuthorize("hasAuthority('seal:manage') or hasAuthority('*')")
    public Result<SealUsageLog> recordUsage(
            @Valid @RequestBody SealUsageRecordDTO recordDTO,
            HttpServletRequest request) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("sealId", recordDTO.getSealId());
            data.put("businessType", recordDTO.getBusinessType());
            data.put("businessId", recordDTO.getBusinessId());
            data.put("businessNo", recordDTO.getBusinessNo());
            data.put("operator", recordDTO.getOperator());
            data.put("remark", recordDTO.getRemark());

            String ipAddress = getClientIpAddress(request);
            SealUsageLog log = sealService.recordUsage(data, ipAddress);
            return Result.success(log, "使用记录已保存");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 新增印章
     */
    @Operation(summary = "新增印章",
            description = "新建一个印章，默认状态为启用(active)")
    @PostMapping
    @PreAuthorize("hasAuthority('seal:manage') or hasAuthority('*')")
    public Result<SealVO> createSeal(@Valid @RequestBody SealCreateDTO createDTO) {
        try {
            Map<String, Object> data = sealCreateDTOToMap(createDTO);
            Map<String, Object> seal = sealService.createSeal(data);
            return Result.success(mapToSealVO(seal), "印章创建成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // ==================== 路径变量接口 ====================

    /**
     * 获取印章详情
     */
    @Operation(summary = "获取印章详情",
            description = "根据印章ID获取印章详细信息")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('seal:view') or hasAuthority('*')")
    public Result<SealVO> getSealById(
            @Parameter(description = "印章ID") @PathVariable String id) {
        Map<String, Object> seal = sealService.getSealById(id);
        if (seal == null) {
            return Result.error("印章不存在");
        }
        return Result.success(mapToSealVO(seal));
    }

    /**
     * 更新印章
     */
    @Operation(summary = "更新印章",
            description = "更新印章信息，仅更新请求体中包含的字段")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('seal:manage') or hasAuthority('*')")
    public Result<SealVO> updateSeal(
            @Parameter(description = "印章ID") @PathVariable String id,
            @Valid @RequestBody SealUpdateDTO updateDTO) {
        try {
            Map<String, Object> data = sealUpdateDTOToMap(updateDTO);
            Map<String, Object> seal = sealService.updateSeal(id, data);
            return Result.success(mapToSealVO(seal), "印章更新成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 作废印章(逻辑删除)
     */
    @Operation(summary = "作废印章",
            description = "将印章状态改为作废(revoked)，记录保持可见用于审计追溯")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('seal:delete') or hasAuthority('*')")
    public Result<String> deleteSeal(
            @Parameter(description = "印章ID") @PathVariable String id) {
        try {
            sealService.revokeSeal(id);
            return Result.success("印章作废成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 切换印章状态(启用/停用)
     */
    @Operation(summary = "切换印章状态",
            description = "切换印章启用/停用状态，请求体需包含目标状态字段")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('seal:manage') or hasAuthority('*')")
    public Result<SealVO> updateStatus(
            @Parameter(description = "印章ID") @PathVariable String id,
            @RequestBody Map<String, String> body) {
        try {
            String status = body != null ? body.get("status") : null;
            if (status == null || status.trim().isEmpty()) {
                return Result.error("状态不能为空");
            }
            Map<String, Object> seal = sealService.updateSealStatus(id, status);
            return Result.success(mapToSealVO(seal), "状态切换成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * SealCreateDTO → Map 转换
     */
    private Map<String, Object> sealCreateDTOToMap(SealCreateDTO dto) {
        Map<String, Object> map = new HashMap<>();
        map.put("sealName", dto.getSealName());
        map.put("sealType", dto.getSealType());
        map.put("sealImage", dto.getSealImage());
        map.put("keeper", dto.getKeeper());
        map.put("authorizedUsers", dto.getAuthorizedUsers());
        map.put("authorizedScenes", dto.getAuthorizedScenes());
        map.put("remark", dto.getRemark());
        return map;
    }

    /**
     * SealUpdateDTO → Map 转换
     */
    private Map<String, Object> sealUpdateDTOToMap(SealUpdateDTO dto) {
        Map<String, Object> map = new HashMap<>();
        if (dto.getSealName() != null) map.put("sealName", dto.getSealName());
        if (dto.getSealType() != null) map.put("sealType", dto.getSealType());
        if (dto.getSealImage() != null) map.put("sealImage", dto.getSealImage());
        if (dto.getKeeper() != null) map.put("keeper", dto.getKeeper());
        if (dto.getAuthorizedUsers() != null) map.put("authorizedUsers", dto.getAuthorizedUsers());
        if (dto.getAuthorizedScenes() != null) map.put("authorizedScenes", dto.getAuthorizedScenes());
        if (dto.getStatus() != null) map.put("status", dto.getStatus());
        if (dto.getRemark() != null) map.put("remark", dto.getRemark());
        return map;
    }

    /**
     * Map → SealVO 转换
     */
    @SuppressWarnings("unchecked")
    private SealVO mapToSealVO(Map<String, Object> map) {
        if (map == null) return null;
        SealVO vo = new SealVO();
        vo.setSealId((String) map.get("id"));
        if (vo.getSealId() == null) vo.setSealId((String) map.get("sealId"));
        vo.setSealName((String) map.get("sealName"));
        vo.setSealType((String) map.get("sealType"));
        vo.setSealImageUrl((String) map.get("sealImage"));
        vo.setStatus((String) map.get("status"));
        vo.setKeeper((String) map.get("keeper"));
        if (map.get("authorizedUsers") instanceof List) {
            vo.setAuthorizedUsers((List<String>) map.get("authorizedUsers"));
        }
        if (map.get("authorizedScenes") instanceof List) {
            vo.setAuthorizedScenes((List<String>) map.get("authorizedScenes"));
        }
        vo.setCreateBy((String) map.get("createBy"));
        vo.setRemark((String) map.get("remark"));
        // createTime 由 SealServiceImpl.formatDateTime 格式化为 "yyyy-MM-dd HH:mm:ss"
        // LocalDateTime.parse 默认使用 ISO-8601(以 T 分隔),需把空格替换为 T
        Object createTimeObj = map.get("createTime");
        if (createTimeObj != null) {
            String timeStr = createTimeObj.toString().replace(" ", "T").substring(0, 19);
            vo.setCreateTime(java.time.LocalDateTime.parse(timeStr));
        }
        return vo;
    }

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
