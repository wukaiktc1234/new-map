package com.foodtraceability.controller.purchase;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.purchase.MaterialArchiveCreateDTO;
import com.foodtraceability.dto.purchase.MaterialArchiveQueryDTO;
import com.foodtraceability.dto.purchase.MaterialArchiveUpdateDTO;
import com.foodtraceability.dto.purchase.MaterialArchiveVO;
import com.foodtraceability.service.purchase.MaterialArchiveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

/**
 * 商品档案管理控制器
 * 提供商品档案的增删改查及状态更新接口
 *
 * 命名说明：
 * - Controller 类名保持 PurchaseArchiveController（与前端 API 路径 /v1/purchase/archives 对应）
 * - Service 类名为 MaterialArchiveService（与实体 MaterialArchive 一致）
 */
@Tag(name = "商品档案")
@RestController
@RequestMapping("/v1/purchase/archives")
public class PurchaseArchiveController {

    private static final Logger log = LoggerFactory.getLogger(PurchaseArchiveController.class);

    private final MaterialArchiveService materialArchiveService;

    /** 构造函数注入（禁止 @Autowired 字段注入） */
    public PurchaseArchiveController(MaterialArchiveService materialArchiveService) {
        this.materialArchiveService = materialArchiveService;
    }

    /**
     * 分页查询商品档案列表
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @param status 状态（前端语义化字符串 'active'/'inactive'）
     * @param keyword 关键字（商品名称/编码模糊匹配）
     * @param categoryId 分类ID
     * @param categoryName 分类名称
     * @return 分页结果
     */
    @Operation(summary = "分页查询商品档案列表", description = "支持按状态、关键字、分类筛选")
    @GetMapping
    public Result<PageResult<MaterialArchiveVO>> getList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String categoryName) {
        try {
            MaterialArchiveQueryDTO queryDTO = new MaterialArchiveQueryDTO();
            queryDTO.setCurrent(page);
            queryDTO.setSize(size);
            queryDTO.setStatus(parseStatusToFrontendCode(status));
            queryDTO.setKeyword(keyword);
            queryDTO.setCategoryId(categoryId);
            queryDTO.setDepartmentId(departmentId);
            queryDTO.setCategoryName(categoryName);

            PageResult<MaterialArchiveVO> result = materialArchiveService.getArchivePage(queryDTO);
            return Result.success(result, "查询成功");
        } catch (Exception e) {
            log.error("分页查询商品档案列表失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取商品档案详情
     * @param id 商品档案ID
     * @return 商品档案详情
     */
    @Operation(summary = "获取商品档案详情", description = "根据ID获取商品档案详细信息")
    @GetMapping("/{id}")
    public Result<MaterialArchiveVO> getById(@PathVariable String id) {
        try {
            Long materialId = parseId(id);
            MaterialArchiveVO vo = materialArchiveService.getArchiveById(materialId);
            if (vo == null) {
                return Result.error("商品档案不存在");
            }
            return Result.success(vo, "查询成功");
        } catch (Exception e) {
            log.error("获取商品档案详情失败: id={}", id, e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 创建商品档案
     * @param createDTO 商品档案数据
     * @return 创建结果
     */
    @Operation(summary = "创建商品档案", description = "新增一条商品档案记录")
    @PostMapping
    public Result<MaterialArchiveVO> create(@Valid @RequestBody MaterialArchiveCreateDTO createDTO) {
        try {
            MaterialArchiveVO vo = materialArchiveService.createArchive(createDTO);
            return Result.success(vo, "创建成功");
        } catch (Exception e) {
            log.error("创建商品档案失败", e);
            return Result.error("创建失败: " + e.getMessage());
        }
    }

    /**
     * 更新商品档案
     * @param id 商品档案ID
     * @param updateDTO 商品档案数据
     * @return 更新结果
     */
    @Operation(summary = "更新商品档案", description = "根据ID更新商品档案信息")
    @PutMapping("/{id}")
    public Result<MaterialArchiveVO> update(@PathVariable String id, @Valid @RequestBody MaterialArchiveUpdateDTO updateDTO) {
        try {
            Long materialId = parseId(id);
            MaterialArchiveVO vo = materialArchiveService.updateArchive(materialId, updateDTO);
            return Result.success(vo, "更新成功");
        } catch (Exception e) {
            log.error("更新商品档案失败: id={}", id, e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    /**
     * 删除商品档案（逻辑删除）
     * @param id 商品档案ID
     * @return 删除结果
     */
    @Operation(summary = "删除商品档案", description = "根据ID逻辑删除商品档案记录")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        try {
            Long materialId = parseId(id);
            materialArchiveService.deleteArchive(materialId);
            return Result.success();
        } catch (Exception e) {
            log.error("删除商品档案失败: id={}", id, e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 更新商品状态（启用/停用）
     * @param id 商品档案ID
     * @param status 目标状态（前端语义化字符串 'active'/'inactive'）
     * @return 更新结果
     */
    @Operation(summary = "更新商品状态", description = "根据ID更新商品档案的启用/停用状态")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable String id, @RequestParam String status) {
        try {
            Long materialId = parseId(id);
            Integer statusCode = parseStatusToFrontendCode(status);
            if (statusCode == null) {
                return Result.error("无效的状态值: " + status + "（仅支持 active/inactive）");
            }
            materialArchiveService.updateStatus(materialId, statusCode);
            return Result.success();
        } catch (Exception e) {
            log.error("更新商品状态失败: id={}, status={}", id, status, e);
            return Result.error("操作失败: " + e.getMessage());
        }
    }

    // ==================== 内部辅助方法 ====================

    /**
     * 解析 ID 字符串为 Long
     * @throws IllegalArgumentException ID格式无效
     */
    private Long parseId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无效的商品ID: " + id);
        }
    }

    /**
     * 解析状态字符串为后端数字编码
     * - 'active' → 1
     * - 'inactive' → 0
     * - null/空 → null（查询时表示不过滤）
     */
    private Integer parseStatusToFrontendCode(String status) {
        if (status == null || status.isEmpty()) {
            return null;
        }
        return switch (status.toLowerCase()) {
            case "active" -> 1;
            case "inactive" -> 0;
            default -> null;
        };
    }
}
