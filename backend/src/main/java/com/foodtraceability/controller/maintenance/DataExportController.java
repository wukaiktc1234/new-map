package com.foodtraceability.controller.maintenance;

import com.foodtraceability.common.Result;
import com.foodtraceability.entity.User;
import com.foodtraceability.service.DataExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "数据导出", description = "GDPR合规数据导出接口")
@RestController
@RequestMapping("/v1/admin/export")
public class DataExportController {

    private static final Logger logger = LoggerFactory.getLogger(DataExportController.class);
    private static final Logger exportAuditLogger = LoggerFactory.getLogger("DATA_EXPORT_AUDIT");

    private final DataExportService dataExportService;

    public DataExportController(DataExportService dataExportService) {
        this.dataExportService = dataExportService;
    }

    @Operation(summary = "导出用户数据（GDPR合规）")
    @GetMapping("/users")
    @PreAuthorize("hasAuthority('admin:user:export')")
    public void exportUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDateTime startDate,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDateTime endDate,
            HttpServletResponse response) throws IOException {

        logger.info("开始导出用户数据: role={}, status={}, startDate={}, endDate={}", role, status, startDate, endDate);
        exportAuditLogger.info("[DATA_EXPORT] 操作=导出用户数据, 角色={}, 状态={}, startDate={}, endDate={}, 操作时间={}",
            role, status, startDate, endDate, LocalDateTime.now());

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=users_export_" +
            System.currentTimeMillis() + ".csv");

        response.getOutputStream().write(new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF});

        PrintWriter writer = response.getWriter();

        writer.println("用户ID,用户名,姓名,邮箱(脱敏),手机号(脱敏),角色,状态,创建时间,最后登录时间");

        List<User> users = dataExportService.queryUsersForExport(role, status, startDate, endDate);

        for (User user : users) {
            writer.println(String.join(",",
                dataExportService.escapeCsv(String.valueOf(user.getId())),
                dataExportService.escapeCsv(user.getUsername()),
                dataExportService.escapeCsv(user.getFullName()),
                dataExportService.escapeCsv(dataExportService.maskEmail(user.getEmail())),
                dataExportService.escapeCsv(dataExportService.maskPhone(user.getPhone())),
                dataExportService.escapeCsv(dataExportService.getRolesString(user.getId())),
                dataExportService.escapeCsv(String.valueOf(user.getStatus())),
                dataExportService.escapeCsv(dataExportService.formatDateTime(user.getCreatedTime())),
                dataExportService.escapeCsv(dataExportService.formatDateTime(user.getLastLoginTime()))
            ));
        }

        writer.flush();
        logger.info("用户数据导出完成: 共{}条记录", users.size());
    }
}
