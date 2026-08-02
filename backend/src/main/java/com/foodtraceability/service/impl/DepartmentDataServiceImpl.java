package com.foodtraceability.service.impl;

import com.foodtraceability.dto.DepartmentBasicInfo;
import com.foodtraceability.entity.Department;
import com.foodtraceability.mapper.DepartmentMapper;
import com.foodtraceability.service.DepartmentDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 部门数据服务实现类
 * 缓存层已由 Spring Cache（ConcurrentMapCacheManager）通过 @Cacheable/@CacheEvict 注解托管
 */
@Service
public class DepartmentDataServiceImpl implements DepartmentDataService {

    private static final Logger log = LoggerFactory.getLogger(DepartmentDataServiceImpl.class);

    private final DepartmentMapper departmentMapper;

    /**
     * 构造函数注入（禁止 @Autowired 字段注入）
     * @param departmentMapper 部门Mapper
     */
    public DepartmentDataServiceImpl(DepartmentMapper departmentMapper) {
        this.departmentMapper = departmentMapper;
    }

    @Override
    @Cacheable(value = "departmentBasicInfo", key = "#departmentId", unless = "#result == null")
    public DepartmentBasicInfo getDepartmentBasicInfo(Long departmentId) {
        if (departmentId == null) {
            return null;
        }

        Department department = departmentMapper.selectById(departmentId);
        if (department == null) {
            return null;
        }

        return convertToBasicInfo(department);
    }

    @Override
    public Map<Long, DepartmentBasicInfo> batchGetDepartmentBasicInfo(List<Long> departmentIds) {
        if (departmentIds == null || departmentIds.isEmpty()) {
            return new HashMap<>();
        }

        // 直接批量查询数据库
        List<Department> departments = departmentMapper.selectBatchIds(departmentIds);
        Map<Long, Department> departmentMap = departments.stream()
            .collect(Collectors.toMap(Department::getDepartmentId, dept -> dept));

        Map<Long, DepartmentBasicInfo> result = new HashMap<>();
        for (Long departmentId : departmentIds) {
            Department department = departmentMap.get(departmentId);
            if (department != null) {
                result.put(departmentId, convertToBasicInfo(department));
            } else {
                result.put(departmentId, null);
            }
        }

        return result;
    }

    @Override
    @CacheEvict(value = "departmentBasicInfo", key = "#departmentId")
    public void clearDepartmentCache(Long departmentId) {
        // 缓存清理由 @CacheEvict 注解自动处理
    }

    @Override
    public void clearDepartmentBatchCache(List<Long> departmentIds) {
        // 批量缓存清理：Spring Cache 不支持批量 evict，由业务层在更新时逐个调用 clearDepartmentCache
        // 此处保留空实现以维持接口契约
    }

    @Override
    @CacheEvict(value = "departmentBasicInfo", allEntries = true)
    public void clearAllDepartmentCache() {
        // 缓存清理由 @CacheEvict(allEntries=true) 注解自动处理
    }

    private DepartmentBasicInfo convertToBasicInfo(Department department) {
        return DepartmentBasicInfo.builder()
            .departmentId(department.getDepartmentId())
            .departmentName(department.getDepartmentName())
            .departmentCode(department.getDepartmentCode())
            .parentId(department.getParentId())
            .level(department.getLevel())
            .status(department.getStatus())
            .managerId(department.getManagerId())
            .managerName(department.getManagerName())
            .description(department.getDescription())
            .sortOrder(department.getSortOrder())
            .employeeCount(department.getEmployeeCount())
            .version(1L)
            .updateTime(department.getUpdatedAt())
            .build();
    }

    @Override
    public List<Long> getAllDepartmentIds() {
        List<Department> departments = departmentMapper.selectList(null);
        return departments.stream()
            .map(Department::getDepartmentId)
            .collect(Collectors.toList());
    }
}
