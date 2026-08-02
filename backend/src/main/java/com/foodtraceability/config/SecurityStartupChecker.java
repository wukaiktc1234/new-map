package com.foodtraceability.config;

import com.foodtraceability.entity.User;
import com.foodtraceability.mapper.UserMapper;
import com.foodtraceability.security.utils.PasswordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityStartupChecker {

    private static final Logger logger = LoggerFactory.getLogger(SecurityStartupChecker.class);

    private final UserMapper userMapper;

    private final ResourceLoader resourceLoader;

    private final Environment environment;

    public SecurityStartupChecker(UserMapper userMapper, ResourceLoader resourceLoader, Environment environment) {
        this.userMapper = userMapper;
        this.resourceLoader = resourceLoader;
        this.environment = environment;
    }

    @Value("${app.security.check-default-admin:true}")
    private boolean checkDefaultAdmin;

    @Value("${app.security.allow-default-admin-in-dev:false}")
    private boolean allowDefaultAdminInDev;

    private static final List<String> DEFAULT_USERNAMES = Arrays.asList("admin", "administrator", "root", "superadmin");
    
    private static final List<String> DEFAULT_PASSWORDS = Arrays.asList(
        "admin", "admin123", "admin123456", "123456", "password", "root", "root123"
    );

    @Bean
    @Profile({"prod", "production"})
    public CommandLineRunner checkDefaultAdminPassword() {
        return args -> {
            if (!checkDefaultAdmin) {
                logger.info("默认管理员检查已禁用");
                return;
            }

            logger.info("=== 生产环境安全检查：检测默认管理员账户 ===");
            
            List<User> users = userMapper.selectList(null);
            boolean foundVulnerable = false;
            
            for (User user : users) {
                if (DEFAULT_USERNAMES.contains(user.getUsername().toLowerCase())) {
                    for (String defaultPwd : DEFAULT_PASSWORDS) {
                        if (PasswordUtils.matches(defaultPwd, user.getPassword())) {
                            logger.error("========================================");
                            logger.error("【严重安全风险】发现使用默认密码的管理员账户！");
                            logger.error("用户名: {}", user.getUsername());
                            logger.error("默认密码: {}", defaultPwd);
                            logger.error("========================================");
                            logger.error("请立即修改该账户密码，或删除该账户后重新创建！");
                            logger.error("系统启动已终止，修复后重新启动。");
                            foundVulnerable = true;
                        }
                    }
                }
            }
            
            if (foundVulnerable) {
                throw new SecurityException(
                    "生产环境安全检查失败：发现使用默认密码的管理员账户。" +
                    "请修改密码后重新启动系统。"
                );
            }
            
            logger.info("=== 安全检查通过：未发现默认密码账户 ===");
        };
    }

    @Bean
    @Profile({"dev", "test", "local"})
    public CommandLineRunner checkDefaultAdminPasswordDev() {
        return args -> {
            if (!checkDefaultAdmin || allowDefaultAdminInDev) {
                logger.info("开发环境：跳过默认管理员密码检查");
                return;
            }

            logger.info("=== 开发环境安全检查：检测默认管理员账户 ===");

            List<User> users = userMapper.selectList(null);

            for (User user : users) {
                if (DEFAULT_USERNAMES.contains(user.getUsername().toLowerCase())) {
                    for (String defaultPwd : DEFAULT_PASSWORDS) {
                        if (PasswordUtils.matches(defaultPwd, user.getPassword())) {
                            logger.warn("========================================");
                            logger.warn("【安全警告】发现使用默认密码的管理员账户！");
                            logger.warn("用户名: {}", user.getUsername());
                            logger.warn("建议：生产环境部署前必须修改此密码！");
                            logger.warn("========================================");
                        }
                    }
                }
            }
        };
    }

    /**
     * 检测V999测试SQL迁移脚本是否存在于主资源目录
     * 安全红线：测试专用迁移脚本禁止出现在生产迁移路径中
     *
     * 使用@PostConstruct确保在Flyway迁移执行之前检测
     * CommandLineRunner在Flyway之后执行，无法阻止测试脚本被执行
     */
    @jakarta.annotation.PostConstruct
    public void checkTestMigrationScript() {
        // 仅生产环境执行该检查；@Profile 对 @PostConstruct 方法不生效，需显式判断
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        if (!activeProfiles.contains("prod") && !activeProfiles.contains("production")) {
            return;
        }

        logger.info("=== 生产环境安全检查：检测测试迁移脚本 ===");
        Resource resource = resourceLoader.getResource("classpath:db/migration/V999__create_test_admin_user.sql");
        if (resource.exists()) {
            logger.error("========================================");
            logger.error("【安全红线】检测到V999测试迁移脚本存在于主资源目录！");
            logger.error("文件位置: db/migration/V999__create_test_admin_user.sql");
            logger.error("此脚本会创建测试管理员账户，禁止在生产环境执行");
            logger.error("请将此文件移动到 src/test/resources/db/migration/ 目录");
            logger.error("========================================");
            throw new SecurityException(
                "生产环境安全检查失败：V999测试迁移脚本存在于主资源目录，" +
                "请将其移动到 src/test/resources/db/migration/ 目录后重新启动。"
            );
        }
        logger.info("=== 安全检查通过：未检测到测试迁移脚本 ===");
    }
}
