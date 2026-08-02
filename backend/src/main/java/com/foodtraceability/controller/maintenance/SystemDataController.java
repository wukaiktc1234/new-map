package com.foodtraceability.controller.maintenance;

import com.foodtraceability.annotation.RequiresPermission;
import com.foodtraceability.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/system/data")
@Tag(name = "系统管理")
@Profile({"h2", "pg"})
public class SystemDataController {


    public SystemDataController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final JdbcTemplate jdbcTemplate;

    @PostMapping("/clear-product-data")
    @Operation(summary = "清空产品管理数据（仅开发环境）")
    @RequiresPermission(value = "system:database:delete", action = "delete")
    public Result<String> clearProductData() {
        try {
            jdbcTemplate.execute("UPDATE dish_inventory SET deleted = 1 WHERE (deleted = 0 OR deleted IS NULL)");
            jdbcTemplate.execute("UPDATE combo_inventory SET deleted = 1 WHERE (deleted = 0 OR deleted IS NULL)");
            jdbcTemplate.execute("UPDATE dish_combo SET deleted = 1 WHERE (deleted = 0 OR deleted IS NULL)");
            jdbcTemplate.execute("UPDATE food SET deleted = 1 WHERE (deleted = 0 OR deleted IS NULL)");
            return Result.success("产品管理数据已清空");
        } catch (Exception e) {
            return Result.error(500, "清空数据失败: " + e.getMessage());
        }
    }
}
