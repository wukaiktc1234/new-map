package com.foodtraceability.service.impl;

import com.foodtraceability.dto.EmployeeBasicInfo;
import com.foodtraceability.entity.Employee;
import com.foodtraceability.mapper.EmployeeMapper;
import com.foodtraceability.service.EmployeeBasicDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 员工基础数据服务实现类
 * 提供跨模块的员工基础信息共享，缓存由 Spring Cache（ConcurrentMapCacheManager）托管
 */
@Service
public class EmployeeBasicDataServiceImpl implements EmployeeBasicDataService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeBasicDataServiceImpl.class);

    private final EmployeeMapper employeeMapper;

    /**
     * 构造函数注入（禁止 @Autowired 字段注入）
     * @param employeeMapper 员工Mapper
     */
    public EmployeeBasicDataServiceImpl(EmployeeMapper employeeMapper) {
        this.employeeMapper = employeeMapper;
    }

    @Override
    public Map<String, EmployeeBasicInfo> batchGetBasicInfo(List<String> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            return new HashMap<>();
        }

        // 直接批量查询数据库
        List<Long> longIds = employeeIds.stream().map(Long::parseLong).toList();
        List<Employee> employees = employeeMapper.selectBatchIds(longIds);

        Map<String, EmployeeBasicInfo> result = new HashMap<>();
        for (Employee employee : employees) {
            result.put(String.valueOf(employee.getId()), convertToBasicInfo(employee));
        }

        // 对于未查到的ID，填充null
        for (String employeeId : employeeIds) {
            if (!result.containsKey(employeeId)) {
                result.put(employeeId, null);
            }
        }

        return result;
    }

    @Override
    @Cacheable(value = "employeeBasicInfo", key = "#employeeId", unless = "#result == null")
    public EmployeeBasicInfo getBasicInfo(String employeeId) {
        if (employeeId == null || employeeId.isEmpty()) {
            return null;
        }

        try {
            Employee employee = employeeMapper.selectById(Long.parseLong(employeeId));
            if (employee != null) {
                return convertToBasicInfo(employee);
            }
        } catch (Exception e) {
            log.error("获取员工信息失败: employeeId={}, error={}", employeeId, e.getMessage());
        }

        return null;
    }

    @Override
    @CacheEvict(value = "employeeBasicInfo", key = "#employeeId")
    public void clearCache(String employeeId) {
        // 缓存清理由 @CacheEvict 注解自动处理
    }

    @Override
    public void clearBatchCache(List<String> employeeIds) {
        // 批量缓存清理：Spring Cache 不支持批量 evict，由业务层在更新时逐个调用 clearCache
        // 此处保留空实现以维持接口契约
    }

    @Override
    @CacheEvict(value = "employeeBasicInfo", allEntries = true)
    public void clearAllCache() {
        // 缓存清理由 @CacheEvict(allEntries=true) 注解自动处理
    }

    private EmployeeBasicInfo convertToBasicInfo(Employee employee) {
        return EmployeeBasicInfo.builder()
                .employeeId(String.valueOf(employee.getId()))
                .employeeName(employee.getName())
                .employeeCode(employee.getEmployeeCode())
                .departmentId(employee.getDepartmentId())
                .departmentName(employee.getDepartmentName())
                .positionId(employee.getPositionId())
                .positionName(employee.getPositionName())
                .storeId(employee.getStoreId())
                .status(employee.getStatus())
                .phone(employee.getPhone())
                .updateTime(employee.getUpdatedTime())
                .build();
    }
}
