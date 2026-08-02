package com.foodtraceability.service;

import com.foodtraceability.entity.CategoryOperationLog;
import com.foodtraceability.entity.FoodCategory;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 分类操作日志服务接口
 */
public interface CategoryOperationLogService {

    /**
     * 记录分类创建操作
     */
    void logCreate(FoodCategory category, String operatorId, String operatorName, HttpServletRequest request);

    /**
     * 记录分类更新操作
     */
    void logUpdate(FoodCategory oldCategory, FoodCategory newCategory, String operatorId, String operatorName, HttpServletRequest request);

    /**
     * 记录分类删除操作
     */
    void logDelete(FoodCategory category, String operatorId, String operatorName, HttpServletRequest request);

    /**
     * 查询操作日志列表
     */
    List<CategoryOperationLog> getOperationLogs(String categoryId);

    /**
     * 查询所有操作日志（分页）
     */
    List<CategoryOperationLog> getAllOperationLogs(Integer page, Integer size);
}