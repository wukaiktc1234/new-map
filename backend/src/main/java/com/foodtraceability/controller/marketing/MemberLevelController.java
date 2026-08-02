package com.foodtraceability.controller.marketing;

import com.foodtraceability.common.Result;
import com.foodtraceability.entity.MemberLevel;
import com.foodtraceability.service.MemberLevelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会员等级管理控制器
 * 对应前端: /v1/member-levels
 */
@RestController
@RequestMapping("/v1/member-levels")
@Tag(name = "会员等级管理", description = "会员等级体系的配置与管理")
public class MemberLevelController {

    private final MemberLevelService memberLevelService;

    public MemberLevelController(MemberLevelService memberLevelService) {
        this.memberLevelService = memberLevelService;
    }

    /**
     * 获取等级列表
     * 支持按状态筛选，默认返回所有未删除的等级
     */
    @GetMapping
    @Operation(summary = "获取等级列表", description = "支持按状态筛选，按 sortOrder 升序返回")
    @PreAuthorize("hasAuthority('member:level:view') or hasAuthority('member:view') or hasAuthority('*')")
    public Result<List<MemberLevel>> getList(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        List<MemberLevel> list;
        if (status != null) {
            // 按状态筛选
            list = memberLevelService.lambdaQuery()
                    .eq(MemberLevel::getStatus, status)
                    .like(keyword != null && !keyword.isBlank(), MemberLevel::getLevelName, keyword)
                    .orderByAsc(MemberLevel::getSortOrder)
                    .list();
        } else {
            // 默认返回所有启用的等级
            list = memberLevelService.listEnabledLevels();
        }
        return Result.success(list);
    }

    /**
     * 获取等级详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取等级详情")
    @PreAuthorize("hasAuthority('member:level:view') or hasAuthority('member:view') or hasAuthority('*')")
    public Result<MemberLevel> getById(@PathVariable Long id) {
        MemberLevel level = memberLevelService.getById(id);
        if (level == null) {
            return Result.error(4004, "等级不存在");
        }
        return Result.success(level);
    }

    /**
     * 创建等级
     */
    @PostMapping
    @Operation(summary = "创建等级")
    @PreAuthorize("hasAuthority('member:level:manage') or hasAuthority('member:manage') or hasAuthority('*')")
    public Result<MemberLevel> create(@RequestBody MemberLevel level) {
        // 设置默认值
        if (level.getStatus() == null) {
            level.setStatus(1);
        }
        if (level.getSortOrder() == null) {
            level.setSortOrder(0);
        }
        level.setCreateTime(LocalDateTime.now());
        level.setUpdateTime(LocalDateTime.now());
        boolean success = memberLevelService.save(level);
        if (!success) {
            return Result.error(5001, "创建失败");
        }
        return Result.success(level, "创建成功");
    }

    /**
     * 更新等级
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新等级")
    @PreAuthorize("hasAuthority('member:level:manage') or hasAuthority('member:manage') or hasAuthority('*')")
    public Result<MemberLevel> update(@PathVariable Long id, @RequestBody MemberLevel level) {
        level.setLevelId(id);
        level.setUpdateTime(LocalDateTime.now());
        boolean success = memberLevelService.updateById(level);
        if (!success) {
            return Result.error(5002, "更新失败");
        }
        return Result.success(memberLevelService.getById(id), "更新成功");
    }

    /**
     * 切换等级状态（启用/停用）
     */
    @PutMapping("/{id}/toggle-status")
    @Operation(summary = "切换等级状态", description = "启用↔停用切换")
    @PreAuthorize("hasAuthority('member:level:manage') or hasAuthority('member:manage') or hasAuthority('*')")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        MemberLevel level = memberLevelService.getById(id);
        if (level == null) {
            return Result.error(4004, "等级不存在");
        }
        // 切换状态：1→0，0→1
        Integer newStatus = level.getStatus() == 1 ? 0 : 1;
        level.setStatus(newStatus);
        level.setUpdateTime(LocalDateTime.now());
        boolean success = memberLevelService.updateById(level);
        if (!success) {
            return Result.error(5003, "状态切换失败");
        }
        return Result.success(null, newStatus == 1 ? "已启用" : "已停用");
    }

    /**
     * 删除等级（逻辑删除）
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除等级", description = "逻辑删除，已关联会员的等级不可删除")
    @PreAuthorize("hasAuthority('member:level:manage') or hasAuthority('member:manage') or hasAuthority('*')")
    public Result<Void> delete(@PathVariable Long id) {
        boolean success = memberLevelService.removeById(id);
        if (!success) {
            return Result.error(5004, "删除失败");
        }
        return Result.success(null, "删除成功");
    }
}
