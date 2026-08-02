package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.entity.User;
import com.foodtraceability.mapper.UserMapper;
import com.foodtraceability.service.DataExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据导出服务实现类
 */
@Service
public class DataExportServiceImpl implements DataExportService {

    private static final Logger logger = LoggerFactory.getLogger(DataExportServiceImpl.class);
    private static final int MAX_EXPORT_ROWS = 10000;

    private final UserMapper userMapper;

    public DataExportServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> queryUsersForExport(String role, String status, LocalDateTime startDate, LocalDateTime endDate) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();

        if (status != null && !status.isEmpty()) {
            queryWrapper.eq(User::getStatus, status);
        }

        if (startDate != null) {
            queryWrapper.ge(User::getCreatedTime, startDate);
        }

        if (endDate != null) {
            queryWrapper.le(User::getCreatedTime, endDate);
        }

        queryWrapper.orderByAsc(User::getId);
        queryWrapper.last("LIMIT " + MAX_EXPORT_ROWS);

        List<User> result = userMapper.selectList(queryWrapper);

        if (role != null && !role.isEmpty()) {
            result.removeIf(u -> !hasRole(u.getId(), role));
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public String getRolesString(Long userId) {
        try {
            return String.join(",", userMapper.getUserRoles(userId));
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public String maskEmail(String email) {
        if (email == null || email.isEmpty()) return "";
        int atIndex = email.indexOf("@");
        if (atIndex <= 2) return "***";
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }

    @Override
    public String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone == null ? "" : "***";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    @Override
    public String formatDateTime(LocalDateTime dt) {
        return dt != null ? dt.toString().replace("T", " ") : "";
    }

    @Override
    public String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasRole(Long userId, String role) {
        try {
            List<String> roles = userMapper.getUserRoles(userId);
            return roles != null && roles.contains(role);
        } catch (Exception e) {
            logger.warn("检查用户角色失败: userId={}, role={}", userId, role, e);
            return false;
        }
    }
}
