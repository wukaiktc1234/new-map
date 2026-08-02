package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.foodtraceability.dto.EmployeeBasicInfo;
import com.foodtraceability.entity.Department;
import com.foodtraceability.entity.Employee;
import com.foodtraceability.entity.Position;
import com.foodtraceability.mapper.EmployeeMapper;
import com.foodtraceability.service.DepartmentService;
import com.foodtraceability.service.EmployeeDataService;
import com.foodtraceability.service.PositionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 员工数据服务实现类
 * 缓存层已由 Spring Cache（ConcurrentMapCacheManager）通过 @Cacheable/@CacheEvict 注解托管
 */
@Service
public class EmployeeDataServiceImpl implements EmployeeDataService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeDataServiceImpl.class);

    private final EmployeeMapper employeeMapper;

    private final DepartmentService departmentService;

    private final PositionService positionService;

    public EmployeeDataServiceImpl(EmployeeMapper employeeMapper, DepartmentService departmentService, PositionService positionService) {
        this.employeeMapper = employeeMapper;
        this.departmentService = departmentService;
        this.positionService = positionService;
    }

    @Override
    @Cacheable(value = "employeeBasicInfo", key = "#employeeId", unless = "#result == null")
    public EmployeeBasicInfo getEmployeeBasicInfo(String employeeId) {
        if (employeeId == null || employeeId.isEmpty()) {
            return null;
        }

        Employee employee = employeeMapper.selectById(employeeId);
        if (employee == null) {
            return null;
        }

        return convertToBasicInfo(employee);
    }

    @Override
    public Map<String, EmployeeBasicInfo> batchGetEmployeeBasicInfo(List<String> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 过滤无效ID
        List<String> validIds = employeeIds.stream()
            .filter(id -> id != null && !id.isEmpty())
            .collect(Collectors.toList());

        if (validIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 直接批量查询数据库
        List<Long> validIdLongs = validIds.stream()
            .map(Long::valueOf)
            .collect(Collectors.toList());
        QueryWrapper<Employee> wrapper = new QueryWrapper<>();
        wrapper.in("employee_id", validIdLongs);
        List<Employee> employees = employeeMapper.selectList(wrapper);
        Map<String, Employee> employeeMap = employees.stream()
            .collect(Collectors.toMap(emp -> String.valueOf(emp.getId()), emp -> emp));

        Map<String, EmployeeBasicInfo> result = new HashMap<>();
        for (String employeeId : validIds) {
            Employee employee = employeeMap.get(employeeId);
            if (employee != null) {
                result.put(employeeId, convertToBasicInfo(employee));
            } else {
                result.put(employeeId, null);
            }
        }

        return result;
    }

    @Override
    @CacheEvict(value = "employeeBasicInfo", key = "#employeeId")
    public void clearEmployeeCache(String employeeId) {
        // 缓存清理由 @CacheEvict 注解自动处理
    }

    @Override
    public void clearEmployeeBatchCache(List<String> employeeIds) {
        // 批量缓存清理：Spring Cache 不支持批量 evict，由业务层在更新时逐个调用 clearEmployeeCache
        // 此处保留空实现以维持接口契约
    }

    @Override
    @CacheEvict(value = "employeeBasicInfo", allEntries = true)
    public void clearAllEmployeeCache() {
        // 缓存清理由 @CacheEvict(allEntries=true) 注解自动处理
    }

    private EmployeeBasicInfo convertToBasicInfo(Employee employee) {
        EmployeeBasicInfo basicInfo = EmployeeBasicInfo.builder()
                .employeeId(String.valueOf(employee.getId()))
                .employeeName(employee.getName())
                .employeeCode(employee.getEmployeeCode())
                .departmentId(employee.getDepartmentId())
                .positionId(employee.getPositionId())
                .storeId(employee.getStoreId())
                .status(employee.getStatus())
                .phone(employee.getPhone())
                .version(System.currentTimeMillis())
                .updateTime(employee.getUpdatedTime() != null ? employee.getUpdatedTime() : LocalDateTime.now())
                .build();

        if (employee.getDepartmentId() != null) {
            try {
                Department department = departmentService.getDepartmentById(employee.getDepartmentId());
                if (department != null) {
                    basicInfo.setDepartmentName(department.getDepartmentName());
                }
            } catch (NumberFormatException e) {
                log.error("部门ID转换失败: {}", employee.getDepartmentId(), e);
            }
        }

        if (employee.getPositionId() != null) {
            try {
                Position position = positionService.getPositionById(employee.getPositionId());
                if (position != null) {
                    basicInfo.setPositionName(position.getPositionName());
                }
            } catch (NumberFormatException e) {
                log.error("职位ID转换失败: {}", employee.getPositionId(), e);
            }
        }

        return basicInfo;
    }
}
