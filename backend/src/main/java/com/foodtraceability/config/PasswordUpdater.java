package com.foodtraceability.config;

import com.foodtraceability.security.utils.PasswordUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;

/**
 * 开发环境专用：启动时重置admin密码为默认值
 * 安全红线：禁止在生产环境自动重置密码
 *
 * 使用白名单模式：仅在dev/test/local profile下加载
 * 黑名单模式(!prod)在profile未设置时会误执行，存在安全风险
 */
@Component
@Profile({"dev", "test", "local", "default"})
public class PasswordUpdater implements CommandLineRunner {

    private final DataSource dataSource;

    public PasswordUpdater(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        String newPassword = "Admin@123";
        String hashedPassword = PasswordUtils.encryptPassword(newPassword);

        System.out.println("PasswordUpdater: Checking admin user");
        System.out.println("New password: " + newPassword);
        System.out.println("Hashed password: " + hashedPassword);

        try (Connection conn = dataSource.getConnection()) {
            // 先检查 admin 用户是否存在
            boolean userExists = false;
            try (PreparedStatement checkStmt = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE username = ?")) {
                checkStmt.setString(1, "admin");
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    userExists = rs.getInt(1) > 0;
                }
            }
            
            System.out.println("Admin user exists: " + userExists);
            
            if (userExists) {
                // 更新密码
                try (PreparedStatement pstmt = conn.prepareStatement("UPDATE users SET password = ? WHERE username = ?")) {
                    pstmt.setString(1, hashedPassword);
                    pstmt.setString(2, "admin");
                    int rowsAffected = pstmt.executeUpdate();
                    System.out.println("Rows affected: " + rowsAffected);
                    System.out.println("Password updated successfully!");
                }
                // 验证更新后的密码
                try (PreparedStatement verifyStmt = conn.prepareStatement("SELECT password FROM users WHERE username = ?")) {
                    verifyStmt.setString(1, "admin");
                    ResultSet rs = verifyStmt.executeQuery();
                    if (rs.next()) {
                        String storedHash = rs.getString(1);
                        boolean matches = PasswordUtils.matches(newPassword, storedHash);
                        System.out.println("Password verification after update: " + matches);
                    }
                }
            } else {
                // 创建 admin 用户
                System.out.println("Creating admin user...");
                String insertSql = "INSERT INTO users (username, password, name, email, status, create_time, update_time) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setString(1, "admin");
                    pstmt.setString(2, hashedPassword);
                    pstmt.setString(3, "系统管理员");
                    pstmt.setString(4, "admin@example.com");
                    pstmt.setInt(5, 1);
                    pstmt.setObject(6, LocalDateTime.now());
                    pstmt.setObject(7, LocalDateTime.now());

                    int rowsAffected = pstmt.executeUpdate();
                    System.out.println("Rows inserted: " + rowsAffected);
                    System.out.println("Admin user created successfully!");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error updating password: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
