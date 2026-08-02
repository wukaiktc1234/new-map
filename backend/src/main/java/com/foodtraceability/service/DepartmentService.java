package com.foodtraceability.service;

import com.foodtraceability.dto.DepartmentDTO;
import com.foodtraceability.entity.Department;
import java.util.List;
import java.util.Map;

/**
 * 部门服务接口
 */
public interface DepartmentService {

    /**
     * 获取部门树结构
     */
    List<Department> getDepartmentTree();

    /**
     * 获取所有部门列表
     */
    List<Department> getAllDepartments();

    /**
     * 根据ID获取部门
     */
    Department getDepartmentById(Long id);

    /**
     * 创建部门
     */
    Department createDepartment(Department department);

    /**
     * 更新部门
     */
    Department updateDepartment(Long id, Department department);

    /**
     * 删除部门
     */
    void deleteDepartment(Long id);

    /**
     * 更新部门状态
     */
    void updateDepartmentStatus(Long id, Integer status);

    /**
     * 更新部门状态（级联更新关联的职位和员工）
     */
    void updateDepartmentStatusWithCascade(Long id, Integer status);

    /**
     * 更新部门排序
     */
    void updateDepartmentSortOrder(Long id, Integer sortOrder);

    /**
     * 获取指定部门的子部门（用于懒加载）
     */
    List<DepartmentDTO> getChildrenDepartments(Long parentId);

    /**
     * 获取部门员工数量统计
     */
    Map<String, Integer> getDepartmentEmployeeCounts();

    /**
     * 增加部门员工数量
     */
    void incrementEmployeeCount(Long departmentId);

    /**
     * 减少部门员工数量
     */
    void decrementEmployeeCount(Long departmentId);

    /**
     * 清空所有部门数据
     */
    void clearAllDepartments();
}
