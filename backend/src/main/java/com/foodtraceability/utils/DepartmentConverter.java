package com.foodtraceability.utils;

import com.foodtraceability.dto.DepartmentDTO;
import com.foodtraceability.entity.Department;

import java.util.ArrayList;
import java.util.List;

/**
 * 部门实体转换工具类
 */
public class DepartmentConverter {

    /**
     * 将部门实体转换为DTO
     * @param department 部门实体
     * @return 部门DTO
     */
    public static DepartmentDTO toDTO(Department department) {
        if (department == null) {
            return null;
        }

        DepartmentDTO dto = new DepartmentDTO();
        dto.setId(department.getDepartmentId());
        dto.setName(department.getDepartmentName());
        dto.setCode(department.getDepartmentCode());
        dto.setParentId(department.getParentId());
        dto.setLevel(department.getLevel());
        dto.setEmployeeCount(department.getEmployeeCount() != null ? department.getEmployeeCount() : 0);
        dto.setManager(department.getManagerName());
        // 使用实体 type 字段；为兼容历史数据，type 为空时按 code 推断默认值
        dto.setType(department.getType() != null ? department.getType() : (department.getDepartmentCode() != null ? "department" : "other"));
        dto.setStatus(department.getStatus() == 1 ? "active" : "inactive");
        dto.setSort(department.getSortOrder());
        dto.setRemark(department.getDescription());

        // 转换子部门
        if (department.getChildren() != null && !department.getChildren().isEmpty()) {
            List<DepartmentDTO> childrenDTO = new ArrayList<>();
            for (Department child : department.getChildren()) {
                childrenDTO.add(toDTO(child));
            }
            dto.setChildren(childrenDTO);
            dto.setHasChildren(true);
        } else {
            dto.setHasChildren(false);
        }

        return dto;
    }

    /**
     * 将部门实体列表转换为DTO列表
     * @param departments 部门实体列表
     * @return 部门DTO列表
     */
    public static List<DepartmentDTO> toDTOList(List<Department> departments) {
        if (departments == null || departments.isEmpty()) {
            return new ArrayList<>();
        }

        List<DepartmentDTO> dtoList = new ArrayList<>();
        for (Department department : departments) {
            DepartmentDTO dto = new DepartmentDTO();
            dto.setId(department.getDepartmentId());
            dto.setName(department.getDepartmentName());
            dto.setCode(department.getDepartmentCode());
            dto.setParentId(department.getParentId());
            dto.setLevel(department.getLevel());
            dto.setEmployeeCount(department.getEmployeeCount() != null ? department.getEmployeeCount() : 0);
            dto.setManager(department.getManagerName());
            // 使用实体 type 字段；为兼容历史数据，type 为空时按 code 推断默认值
            dto.setType(department.getType() != null ? department.getType() : (department.getDepartmentCode() != null ? "department" : "other"));
            dto.setStatus(department.getStatus() == 1 ? "active" : "inactive");
            dto.setSort(department.getSortOrder());
            dto.setRemark(department.getDescription());
            dto.setHasChildren(true); // 懒加载模式下，默认认为有子节点
            dtoList.add(dto);
        }

        return dtoList;
    }

    /**
     * 将部门树实体转换为DTO树
     * @param departmentTree 部门实体树
     * @return 部门DTO树
     */
    public static List<DepartmentDTO> toDTOTree(List<Department> departmentTree) {
        if (departmentTree == null || departmentTree.isEmpty()) {
            return new ArrayList<>();
        }

        List<DepartmentDTO> dtoTree = new ArrayList<>();
        for (Department department : departmentTree) {
            dtoTree.add(toDTO(department));
        }
        return dtoTree;
    }
}
