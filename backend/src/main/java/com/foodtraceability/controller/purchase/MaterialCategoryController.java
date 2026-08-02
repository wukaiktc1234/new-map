package com.foodtraceability.controller.purchase;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.purchase.MaterialCategoryCreateDTO;
import com.foodtraceability.dto.purchase.MaterialCategoryQueryDTO;
import com.foodtraceability.dto.purchase.MaterialCategoryUpdateDTO;
import com.foodtraceability.dto.purchase.MaterialCategoryVO;
import com.foodtraceability.service.purchase.MaterialCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品分类管理控制器
 * 提供分类的 CRUD、状态切换、列表查询接口
 *
 * 命名说明：
 * - Controller 类名保持 MaterialCategoryController
 * - 路径 /v1/material-categories（与前端 API 期望一致）
 */
@Tag(name = "商品分类")
@RestController
@RequestMapping("/v1/material-categories")
public class MaterialCategoryController {

    private static final Logger log = LoggerFactory.getLogger(MaterialCategoryController.class);

    private final MaterialCategoryService materialCategoryService;

    /** 构造函数注入（禁止 @Autowired 字段注入） */
    public MaterialCategoryController(MaterialCategoryService materialCategoryService) {
        this.materialCategoryService = materialCategoryService;
    }

    /**
     * 分页查询商品分类列表
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @param status 状态（前端语义化字符串 'active'/'inactive'）
     * @param keyword 关键字（名称或编码模糊匹配）
     * @return 分页结果（含 parentName + materialCount）
     */
    @Operation(summary = "分页查询商品分类列表", description = "支持按状态、关键字、父分类筛选")
    @GetMapping("/page")
    public Result<PageResult<MaterialCategoryVO>> getPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        try {
            MaterialCategoryQueryDTO queryDTO = new MaterialCategoryQueryDTO();
            queryDTO.setCurrent(page);
            queryDTO.setSize(size);
            queryDTO.setKeyword(keyword);
            queryDTO.setStatus(parseStatusToFrontendCode(status));

            PageResult<MaterialCategoryVO> result = materialCategoryService.getCategoryPage(queryDTO);
            return Result.success(result, "查询成功");
        } catch (Exception e) {
            log.error("分页查询商品分类列表失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有启用的商品分类列表（下拉选项用）
     */
    @Operation(summary = "获取启用商品分类列表", description = "返回所有启用状态的分类，用于下拉选项")
    @GetMapping
    public Result<List<MaterialCategoryVO>> getEnabledList() {
        try {
            List<MaterialCategoryVO> list = materialCategoryService.getEnabledList();
            return Result.success(list, "查询成功");
        } catch (Exception e) {
            log.error("获取启用商品分类列表失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取全部分类列表（包含禁用）
     */
    @Operation(summary = "获取全部商品分类列表", description = "返回所有分类，包含禁用状态")
    @GetMapping("/all")
    public Result<List<MaterialCategoryVO>> getAllList() {
        try {
            List<MaterialCategoryVO> list = materialCategoryService.getAllList();
            return Result.success(list, "查询成功");
        } catch (Exception e) {
            log.error("获取全部商品分类列表失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 根据 ID 获取商品分类详情
     * @param id 分类ID
     */
    @Operation(summary = "获取商品分类详情", description = "根据ID获取商品分类详细信息")
    @GetMapping("/{id}")
    public Result<MaterialCategoryVO> getById(@PathVariable String id) {
        try {
            Long categoryId = parseId(id);
            MaterialCategoryVO vo = materialCategoryService.getCategoryById(categoryId);
            if (vo == null) {
                return Result.error("商品分类不存在");
            }
            return Result.success(vo, "查询成功");
        } catch (Exception e) {
            log.error("获取商品分类详情失败: id={}", id, e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 创建商品分类
     * @param createDTO 创建数据
     */
    @Operation(summary = "创建商品分类", description = "新增一条商品分类记录")
    @PostMapping
    public Result<MaterialCategoryVO> create(@Valid @RequestBody MaterialCategoryCreateDTO createDTO) {
        try {
            MaterialCategoryVO vo = materialCategoryService.createCategory(createDTO);
            return Result.success(vo, "创建成功");
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("创建商品分类失败", e);
            return Result.error("创建失败: " + e.getMessage());
        }
    }

    /**
     * 更新商品分类（部分更新）
     * @param id 分类ID
     * @param updateDTO 更新数据
     */
    @Operation(summary = "更新商品分类", description = "根据ID更新商品分类信息")
    @PutMapping("/{id}")
    public Result<MaterialCategoryVO> update(@PathVariable String id, @Valid @RequestBody MaterialCategoryUpdateDTO updateDTO) {
        try {
            Long categoryId = parseId(id);
            MaterialCategoryVO vo = materialCategoryService.updateCategory(categoryId, updateDTO);
            return Result.success(vo, "更新成功");
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("更新商品分类失败: id={}", id, e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    /**
     * 删除商品分类（逻辑删除）
     * 删除前校验：有子分类或被商品档案引用时禁止删除
     * @param id 分类ID
     */
    @Operation(summary = "删除商品分类", description = "根据ID逻辑删除商品分类（存在子分类或被引用时禁止删除）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        try {
            Long categoryId = parseId(id);
            materialCategoryService.deleteCategory(categoryId);
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("删除商品分类失败: id={}", id, e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 更新分类状态（启用/停用）
     * @param id 分类ID
     * @param status 目标状态（前端语义化字符串 'active'/'inactive'）
     */
    @Operation(summary = "更新分类状态", description = "根据ID更新商品分类的启用/停用状态")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable String id, @RequestParam String status) {
        try {
            Long categoryId = parseId(id);
            Integer statusCode = parseStatusToFrontendCode(status);
            if (statusCode == null) {
                return Result.error("无效的状态值: " + status + "（仅支持 active/inactive）");
            }
            materialCategoryService.updateStatus(categoryId, statusCode);
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("更新分类状态失败: id={}, status={}", id, status, e);
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
            throw new IllegalArgumentException("无效的分类ID: " + id);
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
