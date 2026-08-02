package com.foodtraceability.controller.hardware;

import com.foodtraceability.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 硬件配置数据库初始化控制器
 *
 * <p>通过 JdbcTemplate 在运行时手动创建 hardware_config 表并插入默认配置。
 * 注意：此方式违反 Flyway 迁移规范，且 hardware_and_traceability.sql 已标记为 DEPRECATED。
 * 新系统使用 devices 表（由 BaseDatabaseInitializer.createDeviceTables() 自动创建）。</p>
 *
 * @deprecated 已废弃，将在下个版本移除，由 DeviceDriver 体系替代。
 *             新系统使用 devices 表管理设备配置，由 BaseDatabaseInitializer 自动建表，
 *             无需运行时手动初始化。旧 hardware_config 表的初始化逻辑应迁移到 Flyway 脚本
 *             或 DatabaseInitializer。参见 docs/audit/MANAGEMENT_PRODUCT_REVIEW.md 第 4.2 节。
 */
@Deprecated
@RestController
@RequestMapping("/v1/hardware/init")
@Tag(name = "硬件初始化管理", description = "硬件配置数据库初始化接口（仅管理员可用）")
public class HardwareInitController {

    private static final Logger log = LoggerFactory.getLogger(HardwareInitController.class);

    public HardwareInitController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final JdbcTemplate jdbcTemplate;
    
    @PostMapping("/init-db")
    @Operation(summary = "初始化硬件配置数据库")
    @PreAuthorize("hasAuthority('hardware:init:manage') or hasRole('ADMIN') or hasAuthority('*')")
    public Result<String> initHardwareDatabase() {
        log.info("开始初始化硬件配置数据库");
        
        try {
            // 检查硬件配置表是否存在
            String checkTableSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'hardware_config'";
            Integer count = jdbcTemplate.queryForObject(checkTableSql, Integer.class);
            
            if (count == null || count == 0) {
                log.info("硬件配置表不存在，开始创建");
                createHardwareConfigTable();
                insertDefaultHardwareConfig();
                log.info("硬件配置表创建完成");
                return Result.success("硬件配置数据库初始化成功");
            } else {
                log.info("硬件配置表已存在，跳过创建");
                return Result.success("硬件配置数据库已存在");
            }
            
        } catch (Exception e) {
            log.error("数据库初始化失败: {}", e.getMessage());
            e.printStackTrace();
            return Result.error("数据库初始化失败: " + e.getMessage());
        }
    }
    
    private void createHardwareConfigTable() {
        String createTableSql = """
            CREATE TABLE IF NOT EXISTS hardware_config (
                id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                device_type VARCHAR(50),
                device_name VARCHAR(100),
                device_model VARCHAR(100),
                connection_type VARCHAR(50),
                ip_address VARCHAR(50),
                port VARCHAR(50),
                baud_rate VARCHAR(20),
                api_key VARCHAR(100),
                config_json TEXT,
                status SMALLINT DEFAULT 1,
                remark VARCHAR(500),
                store_id BIGINT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                created_by VARCHAR(50),
                updated_by VARCHAR(50),
                deleted SMALLINT DEFAULT 0
            )
        """;
        
        jdbcTemplate.execute(createTableSql);
        log.info("硬件配置表创建成功");
    }
    
    private void insertDefaultHardwareConfig() {
        String insertSql = """
            INSERT INTO hardware_config (device_type, device_name, connection_type, status, remark, created_by, updated_by) VALUES
            ('SCANNER', '默认扫码枪', 'SERIAL', 1, '系统默认配置', 'system', 'system'),
            ('PRINTER', '默认热敏打印机', 'NETWORK', 1, '系统默认配置', 'system', 'system'),
            ('CAMERA', '默认摄像头', 'NETWORK', 1, '系统默认配置', 'system', 'system')
        """;
        
        try {
            jdbcTemplate.execute(insertSql);
            log.info("默认硬件配置数据插入成功");
        } catch (Exception e) {
            log.error("默认数据可能已存在，跳过插入");
        }
    }
}