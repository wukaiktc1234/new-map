package com.foodtraceability.controller.maintenance;

import com.foodtraceability.annotation.RequiresPermission;
import com.foodtraceability.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/v1/database")
@Tag(name = "数据库管理", description = "数据库清理和数据刷新")
@Profile({"h2", "pg"})
public class DatabaseCleanupController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DatabaseCleanupController.class);

    public DatabaseCleanupController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final JdbcTemplate jdbcTemplate;

    @PostMapping("/clean-kitchen-orders")
    @Operation(summary = "清理并重新插入后厨订单测试数据（仅开发环境）")
    @RequiresPermission(value = "system:database:delete", action = "delete")
    public Result<String> cleanupAndRefreshKitchenOrders() {
        try {
            log.info("开始清理后厨订单测试数据");
            jdbcTemplate.update("UPDATE kitchen_order SET deleted = 1 WHERE kitchen_order_id LIKE 'KO%' AND (deleted = 0 OR deleted IS NULL)");
            log.info("已逻辑删除旧的后厨测试数据");
            LocalDateTime now = LocalDateTime.now();
            Object[][] orders = {{"KO001", "O001", "ORD202502170001", 0, "A01", "[{\"dishName\":\"宫保鸡丁\",\"quantity\":1},{\"dishName\":\"米饭\",\"quantity\":2}]", 2, 0, "pending", 1L, "总店", null, "不要辣"}, {"KO002", "O002", "ORD202502170002", 0, "B03", "[{\"dishName\":\"红烧肉\",\"quantity\":1},{\"dishName\":\"青菜\",\"quantity\":1}]", 2, 1, "pending", 1L, "总店", null, "多放点肉"}, {"KO003", "O003", "ORD202502170003", 1, null, "[{\"dishName\":\"麻婆豆腐\",\"quantity\":1},{\"dishName\":\"蛋炒饭\",\"quantity\":1}]", 2, 0, "making", 1L, "总店", "王师傅", null}, {"KO004", "O004", "ORD202502170004", 0, "C02", "[{\"dishName\":\"鱼香肉丝\",\"quantity\":1},{\"dishName\":\"紫菜蛋花汤\",\"quantity\":1}]", 2, 0, "making", 1L, "总店", "李师傅", null}, {"KO005", "O005", "ORD202502170005", 2, null, "[{\"dishName\":\"酸辣汤\",\"quantity\":2}]", 2, 0, "completed", 1L, "总店", "张师傅", null}, {"KO006", "O006", "ORD202502170006", 1, null, "[{\"dishName\":\"黄焖鸡米饭\",\"quantity\":1},{\"dishName\":\"可乐\",\"quantity\":1}]", 2, 2, "pending", 1L, "总店", null, "特急！顾客赶时间"}, {"KO007", "O007", "ORD202502170007", 0, "D05", "[{\"dishName\":\"水煮鱼\",\"quantity\":1},{\"dishName\":\"酸辣土豆丝\",\"quantity\":1},{\"dishName\":\"米饭\",\"quantity\":3}]", 3, 1, "making", 1L, "总店", "刘大厨", "微辣即可"}, {"KO008", "O008", "ORD202502170008", 2, null, "[{\"dishName\":\"小笼包\",\"quantity\":2},{\"dishName\":\"豆浆\",\"quantity\":2}]", 4, 0, "pending", 1L, "总店", null, "打包带走"}, {"KO009", "O009", "ORD202502170009", 0, "E08", "[{\"dishName\":\"烤鸭\",\"quantity\":1},{\"dishName\":\"面皮\",\"quantity\":2},{\"dishName\":\"葱丝黄瓜\",\"quantity\":1}]", 4, 0, "completed", 1L, "总店", "陈师傅", null}, {"KO010", "O010", "ORD202502170010", 1, null, "[{\"dishName\":\"西红柿炒鸡蛋\",\"quantity\":1},{\"dishName\":\"红烧茄子\",\"quantity\":1},{\"dishName\":\"米饭\",\"quantity\":2}]", 4, 0, "pending", 1L, "总店", null, "少放糖"}};
            for (Object[] order : orders) {
                try {
                    jdbcTemplate.update("INSERT INTO kitchen_order (kitchen_order_id, order_id, order_number, order_type, table_number, dish_items, total_dishes, priority, status, store_id, store_name, chef_name, remark, create_time, update_time, deleted) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)", order[0], order[1], order[2], order[3], order[4], order[5], order[6], order[7], order[8], order[9], order[10], order[11], order[12]);
                } catch (Exception e) {
                    log.debug("后厨订单数据已存在或插入失败: {}", order[0]);
                }
            }
            log.info("后厨测试数据刷新成功，共插入10条订单");
            return Result.success("后厨测试数据刷新成功，请刷新后厨端页面查看");
        } catch (Exception e) {
            log.error("刷新后厨测试数据失败", e);
            return Result.error("刷新失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/clean-voucher-test-data")
    @Operation(summary = "清理电子凭证测试数据（仅开发环境）")
    @RequiresPermission("system:database:delete")
    public Result<String> cleanVoucherTestData(@RequestParam(required = false) String fileHash) {
        try {
            String targetHash = fileHash != null ? fileHash : "4f164fec32372e90108c25f5edde9f1db2ee08ea40d13b76372986e4d5dde0f6";
            log.info("[清理测试数据] 开始清理电子凭证测试数据, fileHash: {}", targetHash);
            int deletedItems = jdbcTemplate.update("UPDATE electronic_invoice_item SET deleted = 1 WHERE invoice_id IN " + "(SELECT id FROM electronic_invoice WHERE voucher_id IN " + "(SELECT id FROM electronic_voucher WHERE source_file_hash = ?)) AND (deleted = 0 OR deleted IS NULL)", targetHash);
            log.info("[清理测试数据] 逻辑删除发票明细: {} 条", deletedItems);
            int deletedInvoices = jdbcTemplate.update("UPDATE electronic_invoice SET deleted = 1 WHERE voucher_id IN " + "(SELECT id FROM electronic_voucher WHERE source_file_hash = ?) AND (deleted = 0 OR deleted IS NULL)", targetHash);
            log.info("[清理测试数据] 逻辑删除发票: {} 条", deletedInvoices);
            int deletedLogs = jdbcTemplate.update("UPDATE voucher_signature_log SET deleted = 1 WHERE voucher_id IN " + "(SELECT id FROM electronic_voucher WHERE source_file_hash = ?) AND (deleted = 0 OR deleted IS NULL)", targetHash);
            log.info("[清理测试数据] 逻辑删除验签日志: {} 条", deletedLogs);
            int deletedVouchers = jdbcTemplate.update("UPDATE electronic_voucher SET deleted = 1 WHERE source_file_hash = ? AND (deleted = 0 OR deleted IS NULL)", targetHash);
            log.info("[清理测试数据] 逻辑删除凭证: {} 条", deletedVouchers);
            String message = String.format("清理完成: 凭证 %d 条, 发票 %d 条, 明细 %d 条, 日志 %d 条", deletedVouchers, deletedInvoices, deletedItems, deletedLogs);
            return Result.success(message);
        } catch (Exception e) {
            log.error("[清理测试数据] 清理失败", e);
            return Result.error("清理失败: " + e.getMessage());
        }
    }
}
