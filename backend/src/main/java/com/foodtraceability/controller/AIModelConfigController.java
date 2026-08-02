package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.aimodel.AIModelConfigCreateDTO;
import com.foodtraceability.dto.aimodel.AIModelConfigQueryDTO;
import com.foodtraceability.dto.aimodel.AIModelConfigUpdateDTO;
import com.foodtraceability.dto.aimodel.AIModelConfigVO;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.service.AIModelConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * AI 模型配置控制器
 * 提供智能补货建议 AI 模型接入的管理 API（支持 local/api 两种类型）
 *
 * 路径说明：字面量路径（/code/{modelCode}、/statistics）定义在 /{id} 之前，
 * 避免 Spring 路径变量匹配冲突。
 */
@RestController
@RequestMapping("/v1/ai-models")
@Tag(name = "AI模型配置", description = "智能补货建议 AI 模型接入的管理接口（local/api 两种类型）")
public class AIModelConfigController {

    private static final Logger log = LoggerFactory.getLogger(AIModelConfigController.class);

    private final AIModelConfigService aiModelConfigService;

    public AIModelConfigController(AIModelConfigService aiModelConfigService) {
        this.aiModelConfigService = aiModelConfigService;
    }

    // ==================== 查询接口（字面量路径优先于 /{id}） ====================

    @GetMapping
    @Operation(summary = "分页查询 AI 模型配置列表")
    @PreAuthorize("hasAuthority('system:aimodel:manage') or hasAuthority('*')")
    public Result<PageResult<AIModelConfigVO>> getList(AIModelConfigQueryDTO query) {
        log.info("分页查询 AI 模型配置: page={}, size={}, modelType={}, status={}",
                query.getPage(), query.getSize(), query.getModelType(), query.getStatus());
        return Result.success(aiModelConfigService.getList(query));
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取 AI 模型配置统计信息（总数/启用/本地/API）")
    @PreAuthorize("hasAuthority('system:aimodel:manage') or hasAuthority('*')")
    public Result<Map<String, Object>> getStatistics() {
        log.info("获取 AI 模型配置统计信息");
        return Result.success(aiModelConfigService.getStatistics());
    }

    @GetMapping("/code/{modelCode}")
    @Operation(summary = "根据业务编码查询 AI 模型配置")
    @PreAuthorize("hasAuthority('system:aimodel:manage') or hasAuthority('*')")
    public Result<AIModelConfigVO> getByCode(
            @Parameter(description = "业务编码") @PathVariable String modelCode) {
        log.info("根据业务编码查询 AI 模型配置: modelCode={}", modelCode);
        AIModelConfigVO vo = aiModelConfigService.getByCode(modelCode);
        if (vo == null) {
            return Result.error("AI 模型配置不存在: " + modelCode);
        }
        return Result.success(vo);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询 AI 模型配置详情")
    @PreAuthorize("hasAuthority('system:aimodel:manage') or hasAuthority('*')")
    public Result<AIModelConfigVO> getById(
            @Parameter(description = "主键ID") @PathVariable Integer id) {
        log.info("根据ID查询 AI 模型配置: id={}", id);
        AIModelConfigVO vo = aiModelConfigService.getById(id);
        if (vo == null) {
            return Result.error("AI 模型配置不存在: id=" + id);
        }
        return Result.success(vo);
    }

    // ==================== 管理接口 ====================

    @PostMapping
    @Operation(summary = "创建 AI 模型配置")
    @PreAuthorize("hasAuthority('system:aimodel:manage') or hasAuthority('*')")
    public Result<AIModelConfigVO> create(@Valid @RequestBody AIModelConfigCreateDTO dto) {
        log.info("创建 AI 模型配置: modelName={}, modelType={}", dto.getModelName(), dto.getModelType());
        try {
            return Result.success(aiModelConfigService.create(dto));
        } catch (Exception e) {
            log.error("创建 AI 模型配置失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新 AI 模型配置（支持部分更新）")
    @PreAuthorize("hasAuthority('system:aimodel:manage') or hasAuthority('*')")
    public Result<AIModelConfigVO> update(
            @Parameter(description = "主键ID") @PathVariable Integer id,
            @Valid @RequestBody AIModelConfigUpdateDTO dto) {
        log.info("更新 AI 模型配置: id={}", id);
        try {
            return Result.success(aiModelConfigService.update(id, dto));
        } catch (Exception e) {
            log.error("更新 AI 模型配置失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "更新 AI 模型状态（body: {status: \"active\"}）")
    @PreAuthorize("hasAuthority('system:aimodel:manage') or hasAuthority('*')")
    public Result<Void> updateStatus(
            @Parameter(description = "主键ID") @PathVariable Integer id,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        log.info("更新 AI 模型状态: id={}, status={}", id, status);
        try {
            boolean ok = aiModelConfigService.updateStatus(id, status);
            return ok ? Result.success(null) : Result.error("更新状态失败：模型不存在或状态无效");
        } catch (Exception e) {
            log.error("更新 AI 模型状态失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/sync")
    @Operation(summary = "触发 AI 模型同步（更新 lastSyncTime）")
    @PreAuthorize("hasAuthority('system:aimodel:manage') or hasAuthority('*')")
    public Result<Void> syncModel(
            @Parameter(description = "主键ID") @PathVariable Integer id) {
        log.info("触发 AI 模型同步: id={}", id);
        try {
            boolean ok = aiModelConfigService.syncModel(id);
            return ok ? Result.success(null) : Result.error("同步失败：模型不存在");
        } catch (Exception e) {
            log.error("触发 AI 模型同步失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/test-connection")
    @Operation(summary = "测试 AI 模型连接是否可用（local 检查路径，api 发送 HTTP GET）")
    @PreAuthorize("hasAuthority('system:aimodel:manage') or hasAuthority('*')")
    public Result<Map<String, Object>> testConnection(
            @Parameter(description = "主键ID") @PathVariable Integer id) {
        log.info("测试 AI 模型连接: id={}", id);
        try {
            Map<String, Object> result = aiModelConfigService.testConnection(id);
            Boolean success = (Boolean) result.get("success");
            if (Boolean.TRUE.equals(success)) {
                log.info("AI 模型连接测试成功: id={}, modelType={}, responseTimeMs={}",
                        id, result.get("modelType"), result.get("responseTimeMs"));
            } else {
                log.warn("AI 模型连接测试失败: id={}, message={}", id, result.get("message"));
            }
            return Result.success(result);
        } catch (Exception e) {
            log.error("测试 AI 模型连接异常: id={}, error={}", id, e.getMessage());
            return Result.error("测试连接异常: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除 AI 模型配置（逻辑删除）")
    @PreAuthorize("hasAuthority('system:aimodel:manage') or hasAuthority('*')")
    public Result<Void> delete(
            @Parameter(description = "主键ID") @PathVariable Integer id) {
        log.info("删除 AI 模型配置: id={}", id);
        try {
            boolean ok = aiModelConfigService.delete(id);
            return ok ? Result.success(null) : Result.error("删除失败：模型不存在");
        } catch (Exception e) {
            log.error("删除 AI 模型配置失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
