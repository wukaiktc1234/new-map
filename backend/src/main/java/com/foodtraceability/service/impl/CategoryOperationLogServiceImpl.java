package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.CategoryOperationLog;
import com.foodtraceability.entity.FoodCategory;
import com.foodtraceability.mapper.CategoryOperationLogMapper;
import com.foodtraceability.service.CategoryOperationLogService;
import com.foodtraceability.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 分类操作日志服务实现类
 */
@Service
public class CategoryOperationLogServiceImpl extends ServiceImpl<CategoryOperationLogMapper, CategoryOperationLog> implements CategoryOperationLogService {
    private static final Logger logger = LoggerFactory.getLogger(CategoryOperationLogServiceImpl.class);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logCreate(FoodCategory category, String operatorId, String operatorName, HttpServletRequest request) {
        try {
            CategoryOperationLog operationLog = new CategoryOperationLog();
            operationLog.setCategoryId(category.getId().toString());
            operationLog.setCategoryName(category.getCategoryName());
            operationLog.setCategoryCode(category.getCategoryCode());
            operationLog.setParentId(category.getParentId() != null ? category.getParentId().toString() : null);
            operationLog.setOperationType("CREATE");
            operationLog.setOperationDesc("创建分类");
            operationLog.setNewData(JsonUtils.toJson(category));
            operationLog.setOperatorId(operatorId);
            operationLog.setOperatorName(operatorName);
            operationLog.setOperationTime(LocalDateTime.now());
            operationLog.setIpAddress(getClientIp(request));
            operationLog.setUserAgent(request.getHeader("User-Agent"));
            save(operationLog);
            logger.info("记录分类创建日志: {}", category.getCategoryName());
        } catch (Exception e) {
            logger.error("记录分类创建日志失败: {}", e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logUpdate(FoodCategory oldCategory, FoodCategory newCategory, String operatorId, String operatorName, HttpServletRequest request) {
        try {
            CategoryOperationLog operationLog = new CategoryOperationLog();
            operationLog.setCategoryId(newCategory.getId().toString());
            operationLog.setCategoryName(newCategory.getCategoryName());
            operationLog.setCategoryCode(newCategory.getCategoryCode());
            operationLog.setParentId(newCategory.getParentId() != null ? newCategory.getParentId().toString() : null);
            operationLog.setOperationType("UPDATE");
            operationLog.setOperationDesc("更新分类");
            operationLog.setOldData(JsonUtils.toJson(oldCategory));
            operationLog.setNewData(JsonUtils.toJson(newCategory));
            operationLog.setOperatorId(operatorId);
            operationLog.setOperatorName(operatorName);
            operationLog.setOperationTime(LocalDateTime.now());
            operationLog.setIpAddress(getClientIp(request));
            operationLog.setUserAgent(request.getHeader("User-Agent"));
            save(operationLog);
            logger.info("记录分类更新日志: {}", newCategory.getCategoryName());
        } catch (Exception e) {
            logger.error("记录分类更新日志失败: {}", e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logDelete(FoodCategory category, String operatorId, String operatorName, HttpServletRequest request) {
        try {
            CategoryOperationLog operationLog = new CategoryOperationLog();
            operationLog.setCategoryId(category.getId().toString());
            operationLog.setCategoryName(category.getCategoryName());
            operationLog.setCategoryCode(category.getCategoryCode());
            operationLog.setParentId(category.getParentId() != null ? category.getParentId().toString() : null);
            operationLog.setOperationType("DELETE");
            operationLog.setOperationDesc("删除分类");
            operationLog.setOldData(JsonUtils.toJson(category));
            operationLog.setOperatorId(operatorId);
            operationLog.setOperatorName(operatorName);
            operationLog.setOperationTime(LocalDateTime.now());
            operationLog.setIpAddress(getClientIp(request));
            operationLog.setUserAgent(request.getHeader("User-Agent"));
            save(operationLog);
            logger.info("记录分类删除日志: {}", category.getCategoryName());
        } catch (Exception e) {
            logger.error("记录分类删除日志失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public List<CategoryOperationLog> getOperationLogs(String categoryId) {
        return list(new LambdaQueryWrapper<CategoryOperationLog>().eq(CategoryOperationLog::getCategoryId, categoryId).orderByDesc(CategoryOperationLog::getOperationTime));
    }

    @Override
    public List<CategoryOperationLog> getAllOperationLogs(Integer page, Integer size) {
        Page<CategoryOperationLog> pageParam = new Page<>(page, size);
        return page(pageParam, new LambdaQueryWrapper<CategoryOperationLog>().orderByDesc(CategoryOperationLog::getOperationTime)).getRecords();
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    public CategoryOperationLogServiceImpl() {
    }
}
