package com.foodtraceability.service.impl;

import com.foodtraceability.common.LogUtil;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.dto.DepartmentDTO;
import com.foodtraceability.entity.Department;
import com.foodtraceability.entity.Employee;
import com.foodtraceability.entity.Position;
import com.foodtraceability.event.DepartmentCreatedEvent;
import com.foodtraceability.event.DepartmentDeletedEvent;
import com.foodtraceability.mapper.DepartmentMapper;
import com.foodtraceability.mapper.EmployeeMapper;
import com.foodtraceability.mapper.PositionMapper;
import com.foodtraceability.service.DepartmentService;
import com.foodtraceability.service.EmployeeService;
import com.foodtraceability.service.OrganizationChangeLogService;
import com.foodtraceability.service.PositionService;
import com.foodtraceability.utils.DepartmentCodeGenerator;
import com.foodtraceability.utils.DepartmentConverter;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 部门服务实现类
 */
@Service
public class DepartmentServiceImpl implements DepartmentService {

    private static final Logger log = LoggerFactory.getLogger(DepartmentServiceImpl.class);

    private final DepartmentMapper departmentMapper;
    private final EmployeeMapper employeeMapper;
    private final PositionMapper positionMapper;
    private final OrganizationChangeLogService organizationChangeLogService;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final PositionService positionService;
    private final EmployeeService employeeService;

    public DepartmentServiceImpl(DepartmentMapper departmentMapper,
                                EmployeeMapper employeeMapper,
                                PositionMapper positionMapper,
                                OrganizationChangeLogService organizationChangeLogService,
                                ObjectMapper objectMapper,
                                ApplicationEventPublisher eventPublisher,
                                @Lazy PositionService positionService,
                                @Lazy EmployeeService employeeService) {
        this.departmentMapper = departmentMapper;
        this.employeeMapper = employeeMapper;
        this.positionMapper = positionMapper;
        this.organizationChangeLogService = organizationChangeLogService;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.positionService = positionService;
        this.employeeService = employeeService;
    }

    @Override
    public List<Department> getDepartmentTree() {
        List<Department> allDepartments = departmentMapper.getAllDepartments();
        // 实时统计各部门（含子孙部门）在职员工数，避免 employee_count 字段与实际数据不一致
        fillEmployeeCount(allDepartments);
        return buildDepartmentTree(allDepartments);
    }

    /**
     * 根据在职员工 department_id 实时填充部门 employeeCount。
     * 统计范围仅为当前部门的直接员工数，与员工管理、岗位管理口径保持一致。
     */
    private void fillEmployeeCount(List<Department> allDepartments) {
        if (allDepartments == null || allDepartments.isEmpty()) {
            return;
        }
        // 一次性查询所有在职员工（含试用期），按 department_id 分组计数
        // @TableLogic 已自动过滤 deleted=1 的记录，无需手动添加 deleted 条件
        QueryWrapper<Employee> wrapper = new QueryWrapper<>();
        wrapper.in("status", 1, 2);
        List<Employee> activeEmployees = employeeMapper.selectList(wrapper);
        Map<Long, Integer> directCountMap = new HashMap<>();
        for (Employee employee : activeEmployees) {
            Long deptId = employee.getDepartmentId();
            if (deptId == null) {
                continue;
            }
            directCountMap.put(deptId, directCountMap.getOrDefault(deptId, 0) + 1);
        }
        // 仅统计当前部门直接员工数
        for (Department dept : allDepartments) {
            dept.setEmployeeCount(directCountMap.getOrDefault(dept.getDepartmentId(), 0));
        }
    }

    @Override
    public List<Department> getAllDepartments() {
        return departmentMapper.getAllDepartments();
    }

    @Override
    public Department getDepartmentById(Long id) {
        return departmentMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Department createDepartment(Department department) {
        if (department.getDepartmentCode() == null || department.getDepartmentCode().isEmpty()) {
            String generatedCode = DepartmentCodeGenerator.generateCode(department.getDepartmentName());
            department.setDepartmentCode(generatedCode);
        }
        // 确保部门编码唯一：出现冲突时自动追加序号
        // 使用 countByDeptCode 统计全部记录（含已逻辑删除），避免软删除数据仍占用编码导致唯一索引冲突
        String baseCode = department.getDepartmentCode();
        String uniqueCode = baseCode;
        int suffix = 1;
        while (departmentMapper.countByDeptCode(uniqueCode) > 0) {
            uniqueCode = baseCode + "_" + suffix++;
        }
        department.setDepartmentCode(uniqueCode);
        if (department.getStatus() == null) {
            department.setStatus(1);
        }
        if (department.getParentId() == null) {
            department.setParentId(0L);
        }
        if (department.getLevel() == null) {
            Long parentId = department.getParentId();
            if (parentId == null || parentId == 0L) {
                department.setLevel(1);
            } else {
                Department parent = departmentMapper.selectById(parentId);
                if (parent == null) {
                    throw new RuntimeException("上级部门不存在");
                }
                Integer parentLevel = parent.getLevel();
                department.setLevel((parentLevel == null ? 1 : parentLevel) + 1);
            }
        }
        if (department.getDepartmentName() == null || department.getDepartmentName().trim().isEmpty()) {
            throw new RuntimeException("部门名称不能为空");
        }
        // 默认组织类型为 department；前端可显式传入 type（如 company/store/team/office/group）
        if (department.getType() == null || department.getType().trim().isEmpty()) {
            department.setType("department");
        }
        // 默认排序号为 0，避免 null 导致排序不稳定
        if (department.getSortOrder() == null) {
            department.setSortOrder(0);
        }

        departmentMapper.insert(department);
        
        try {
            String operatorId = getCurrentUserId();
            String ipAddress = getClientIpAddress();
            organizationChangeLogService.logDepartmentCreate(
                department.getDepartmentId().toString(), 
                department.getDepartmentName(), 
                operatorId,
                ipAddress
            );
            
            eventPublisher.publishEvent(new DepartmentCreatedEvent(
                this,
                department.getDepartmentId(),
                department.getDepartmentName(),
                department.getParentId(),
                operatorId
            ));
        } catch (Exception e) {
            LogUtil.logApiError("POST", "/departments", "记录部门创建日志", e.getMessage(), LogUtil.generateRequestId(), 
                "departmentId=" + department.getDepartmentId());
        }
        
        return department;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @SuppressWarnings("null")
    public Department updateDepartment(Long id, Department department) {
        log.info("开始更新部门，部门ID: {}", id);
        
        Department existingDepartment = departmentMapper.selectById(id);
        if (existingDepartment == null) {
            log.error("部门不存在，部门ID: {}", id);
            throw new RuntimeException("部门不存在");
        }
        
        log.info("更新前部门数据: {}", existingDepartment);
        log.info("请求更新数据: {}", department);
        
        if (department.getParentId() != null && department.getParentId().equals(id)) {
            log.error("不能将部门设置为自己的父部门，部门ID: {}", id);
            throw new RuntimeException("不能将部门设置为自己的父部门");
        }
        if (department.getParentId() != null && department.getParentId() != 0L && isDescendant(id, department.getParentId())) {
            log.error("不能将部门移动到其子孙部门下，部门ID: {}, 目标父部门ID: {}", id, department.getParentId());
            throw new RuntimeException("不能将部门移动到其子孙部门下");
        }
        
        String beforeValue = "";
        String afterValue = "";
        try {
            beforeValue = objectMapper.writeValueAsString(existingDepartment);
        } catch (JsonProcessingException e) {
            LogUtil.logApiError("PUT", "/departments/" + id, "序列化部门数据", e.getMessage(), LogUtil.generateRequestId(), "");
        }

        String originalDeptCode = existingDepartment.getDepartmentCode();
        Integer originalStatus = existingDepartment.getStatus();
        String originalType = existingDepartment.getType();
        Integer originalSortOrder = existingDepartment.getSortOrder();
        BeanUtils.copyProperties(department, existingDepartment, "departmentId", "createdTime", "createdBy");

        // 保留原始状态、类型与排序号，防止前端未传时被覆盖为空
        if (existingDepartment.getStatus() == null) {
            existingDepartment.setStatus(originalStatus);
        }
        if (existingDepartment.getType() == null || existingDepartment.getType().trim().isEmpty()) {
            existingDepartment.setType(originalType);
        }
        if (existingDepartment.getSortOrder() == null) {
            existingDepartment.setSortOrder(originalSortOrder);
        }

        if (department.getDepartmentCode() == null || department.getDepartmentCode().trim().isEmpty()) {
            existingDepartment.setDepartmentCode(originalDeptCode);
        }
        
        if (department.getParentId() != null) {
            if (department.getParentId() == 0L) {
                existingDepartment.setLevel(1);
            } else {
                Department parent = departmentMapper.selectById(department.getParentId());
                if (parent == null) {
                    throw new RuntimeException("上级部门不存在");
                }
                Integer parentLevel = parent.getLevel();
                existingDepartment.setLevel((parentLevel == null ? 1 : parentLevel) + 1);
            }
        }
        
        log.info("复制属性后的部门数据: {}", existingDepartment);
        
        int updateResult = departmentMapper.updateById(existingDepartment);
        log.info("数据库更新结果，影响行数: {}", updateResult);
        
        if (updateResult <= 0) {
            log.error("数据库更新失败，影响行数为0");
            throw new RuntimeException("数据库更新失败");
        }
        
        try {
            afterValue = objectMapper.writeValueAsString(existingDepartment);
            String operatorId = getCurrentUserId();
            String ipAddress = getClientIpAddress();
            organizationChangeLogService.logDepartmentUpdate(
                id.toString(), 
                beforeValue, 
                afterValue, 
                operatorId, 
                "更新部门信息",
                ipAddress
            );
        } catch (Exception e) {
            LogUtil.logApiError("PUT", "/departments/" + id, "记录部门更新日志", e.getMessage(), LogUtil.generateRequestId(), 
                "departmentId=" + id);
        }
        
        log.info("部门更新完成: {}", existingDepartment);
        return existingDepartment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDepartment(Long id) {
        Department department = departmentMapper.selectById(id);
        if (department == null) {
            throw new BusinessException(400, "部门不存在");
        }
        
        String operatorId = getCurrentUserId();
        
        List<Department> childDepartments = departmentMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Department>()
                .eq(Department::getParentId, id)
        );
        
        if (!childDepartments.isEmpty()) {
            throw new BusinessException(400, "该部门下存在" + childDepartments.size() + "个子部门，无法删除。请先删除或移动所有子部门。");
        }
        
        // 使用自定义 @Select 方法绕过 QueryWrapper 的参数类型推断问题
        // 详见 EmployeeMapper.countActiveByDepartmentId 的注释说明
        int activeEmployeeCount = employeeMapper.countActiveByDepartmentId(id.toString());

        if (activeEmployeeCount > 0) {
            throw new BusinessException(400, "该部门下存在" + activeEmployeeCount + "名在职员工，无法删除。请先调整或删除这些员工的部门。");
        }
        
        departmentMapper.deleteById(id);
        
        try {
            String ipAddress = getClientIpAddress();
            String beforeValue = toJson(department);
            organizationChangeLogService.logDepartmentDelete(id.toString(), beforeValue, operatorId, ipAddress);
            
            eventPublisher.publishEvent(new DepartmentDeletedEvent(
                this,
                department.getDepartmentId(),
                department.getDepartmentName(),
                department.getParentId(),
                operatorId
            ));
        } catch (Exception e) {
            LogUtil.logApiError("DELETE", "/departments/" + id, "记录部门删除日志", e.getMessage(), LogUtil.generateRequestId(), 
                "departmentId=" + id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDepartmentStatus(Long id, Integer status) {
        Department department = departmentMapper.selectById(id);
        if (department == null) {
            throw new RuntimeException("部门不存在");
        }
        
        String beforeValue = "";
        String afterValue = "";
        try {
            beforeValue = objectMapper.writeValueAsString(department);
        } catch (JsonProcessingException e) {
            LogUtil.logApiError("PUT", "/departments/" + id + "/status", "序列化部门数据", e.getMessage(), LogUtil.generateRequestId(), "");
        }
        
        department.setStatus(status);
        departmentMapper.updateById(department);
        
        updateChildrenDepartmentStatus(id, status);
        
        try {
            afterValue = objectMapper.writeValueAsString(department);
            String operatorId = getCurrentUserId();
            String ipAddress = getClientIpAddress();
            organizationChangeLogService.logDepartmentUpdate(
                id.toString(), 
                beforeValue, 
                afterValue, 
                operatorId, 
                "更新部门状态",
                ipAddress
            );
        } catch (Exception e) {
            LogUtil.logApiError("PUT", "/departments/" + id + "/status", "记录部门状态更新日志", e.getMessage(), LogUtil.generateRequestId(), 
                "departmentId=" + id);
        }
    }

    private void updateChildrenDepartmentStatus(Long parentId, Integer status) {
        List<Department> children = departmentMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Department>()
                .eq(Department::getParentId, parentId)
        );
        
        for (Department child : children) {
            child.setStatus(status);
            departmentMapper.updateById(child);
            updateChildrenDepartmentStatus(child.getDepartmentId(), status);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDepartmentSortOrder(Long id, Integer sortOrder) {
        Department department = departmentMapper.selectById(id);
        if (department == null) {
            throw new RuntimeException("部门不存在");
        }

        String beforeValue = "";
        String afterValue = "";
        try {
            beforeValue = objectMapper.writeValueAsString(department);
        } catch (JsonProcessingException e) {
            LogUtil.logApiError("PUT", "/departments/" + id + "/sort", "序列化部门数据", e.getMessage(), LogUtil.generateRequestId(), "");
        }

        department.setSortOrder(sortOrder);
        departmentMapper.updateById(department);

        try {
            afterValue = objectMapper.writeValueAsString(department);
            String operatorId = getCurrentUserId();
            String ipAddress = getClientIpAddress();
            organizationChangeLogService.logDepartmentUpdate(
                id.toString(),
                beforeValue,
                afterValue,
                operatorId,
                "更新部门排序",
                ipAddress
            );
        } catch (Exception e) {
            LogUtil.logApiError("PUT", "/departments/" + id + "/sort", "记录部门排序更新日志", e.getMessage(), LogUtil.generateRequestId(),
                "departmentId=" + id);
        }
    }

    /**
     * 构建部门树结构
     */
    private List<Department> buildDepartmentTree(List<Department> allDepartments) {
        Map<Long, List<Department>> departmentMap = new HashMap<>();
        for (Department dept : allDepartments) {
            Long parentId = dept.getParentId();
            if (parentId == null || parentId == 0L) {
                parentId = 0L;
            }
            departmentMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(dept);
        }

        List<Department> tree = new ArrayList<>();
        for (Department dept : allDepartments) {
            Long parentId = dept.getParentId();
            if (parentId == null || parentId == 0L) {
                tree.add(buildTree(dept, departmentMap));
            }
        }

        return tree;
    }

    /**
     * 递归构建部门树节点
     */
    private Department buildTree(Department department, Map<Long, List<Department>> departmentMap) {
        Long departmentId = department.getDepartmentId();
        List<Department> children = departmentMap.get(departmentId);
        if (children != null && !children.isEmpty()) {
            department.setChildren(children);
            for (Department child : children) {
                buildTree(child, departmentMap);
            }
        }
        return department;
    }

    /**
     * 判断 targetId 是否为 ancestorId 的子孙节点（含递归子节点）
     */
    private boolean isDescendant(Long ancestorId, Long targetId) {
        if (ancestorId == null || targetId == null) {
            return false;
        }
        List<Department> children = departmentMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Department>()
                .eq(Department::getParentId, ancestorId)
        );
        for (Department child : children) {
            Long childId = child.getDepartmentId();
            if (childId != null && childId.equals(targetId)) {
                return true;
            }
            if (isDescendant(childId, targetId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取当前登录用户ID
     */
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() != null) {
            return authentication.getName();
        }
        return "system";
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ipAddress = request.getHeader("X-Forwarded-For");
                if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                    ipAddress = request.getHeader("Proxy-Client-IP");
                }
                if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                    ipAddress = request.getHeader("WL-Proxy-Client-IP");
                }
                if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                    ipAddress = request.getRemoteAddr();
                }
                if (ipAddress != null && ipAddress.contains(",")) {
                    ipAddress = ipAddress.split(",")[0].trim();
                }
                return ipAddress;
            }
        } catch (Exception e) {
            log.warn("获取客户端IP地址失败: {}", e.getMessage());
        }
        return "unknown";
    }

    /**
     * 将对象转换为JSON字符串
     */
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("JSON序列化失败", e);
            return "{}";
        }
    }

    @Override
    public List<DepartmentDTO> getChildrenDepartments(Long parentId) {
        List<Department> children = departmentMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Department>()
                .eq(Department::getParentId, parentId == null ? 0L : parentId)
                .orderByAsc(Department::getDepartmentId)
        );
        return DepartmentConverter.toDTOList(children);
    }

    @Override
    public Map<String, Integer> getDepartmentEmployeeCounts() {
        try {
            QueryWrapper<Employee> queryWrapper = new QueryWrapper<>();
            queryWrapper.in("status", 1, 2); // 1: 在职, 2: 试用期
            // @TableLogic 已自动过滤 deleted=1 的记录

            List<Employee> activeEmployees = employeeMapper.selectList(queryWrapper);

            Map<String, Integer> departmentCounts = new HashMap<>();
            for (Employee employee : activeEmployees) {
                Long deptId = employee.getDepartmentId();
                if (deptId != null) {
                    departmentCounts.put(String.valueOf(deptId), departmentCounts.getOrDefault(String.valueOf(deptId), 0) + 1);
                }
            }

            return departmentCounts;
        } catch (Exception e) {
            log.error("获取部门员工数量统计失败", e);
            return new HashMap<>();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementEmployeeCount(Long departmentId) {
        Department department = departmentMapper.selectById(departmentId);
        if (department != null) {
            int currentCount = department.getEmployeeCount() != null ? department.getEmployeeCount() : 0;
            department.setEmployeeCount(currentCount + 1);
            departmentMapper.updateById(department);
            log.info("部门员工数量已增加: 部门ID={}, 当前人数={}", departmentId, department.getEmployeeCount());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void decrementEmployeeCount(Long departmentId) {
        Department department = departmentMapper.selectById(departmentId);
        if (department != null) {
            int currentCount = department.getEmployeeCount() != null ? department.getEmployeeCount() : 0;
            if (currentCount > 0) {
                department.setEmployeeCount(currentCount - 1);
                departmentMapper.updateById(department);
                log.info("部门员工数量已减少: 部门ID={}, 当前人数={}", departmentId, department.getEmployeeCount());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDepartmentStatusWithCascade(Long id, Integer status) {
        log.info("开始更新部门状态（级联），部门ID: {}, 状态: {}", id, status);

        Department department = departmentMapper.selectById(id);
        if (department == null) {
            throw new RuntimeException("部门不存在");
        }

        String beforeValue = "";
        String afterValue = "";
        try {
            beforeValue = objectMapper.writeValueAsString(department);
        } catch (JsonProcessingException e) {
            LogUtil.logApiError("PUT", "/departments/" + id + "/status/cascade", "序列化部门数据", e.getMessage(), LogUtil.generateRequestId(), "");
        }

        department.setStatus(status);
        departmentMapper.updateById(department);
        log.info("部门状态更新成功");

        updateChildrenDepartmentStatus(id, status);

        LambdaUpdateWrapper<Position> positionWrapper = new LambdaUpdateWrapper<>();
        positionWrapper.eq(Position::getDepartmentId, id);
        positionWrapper.set(Position::getStatus, status == 1 ? 1 : 0);
        positionMapper.update(null, positionWrapper);
        log.info("关联职位状态更新成功");

        LambdaUpdateWrapper<Employee> employeeWrapper = new LambdaUpdateWrapper<>();
        employeeWrapper.eq(Employee::getDepartmentId, id);
        employeeWrapper.set(Employee::getStatus, status == 1 ? 1 : 0);
        employeeService.update(employeeWrapper);
        log.info("关联员工状态更新成功");

        try {
            afterValue = objectMapper.writeValueAsString(department);
            String operatorId = getCurrentUserId();
            String ipAddress = getClientIpAddress();
            organizationChangeLogService.logDepartmentUpdate(
                id.toString(),
                beforeValue,
                afterValue,
                operatorId,
                "更新部门状态（级联）",
                ipAddress
            );
        } catch (Exception e) {
            LogUtil.logApiError("PUT", "/departments/" + id + "/status/cascade", "记录部门状态更新日志", e.getMessage(), LogUtil.generateRequestId(),
                "departmentId=" + id);
        }

        log.info("部门状态级联更新完成");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearAllDepartments() {
        log.info("开始清空所有部门数据");
        
        List<Department> allDepartments = departmentMapper.getAllDepartments();
        int count = allDepartments.size();
        
        if (count > 0) {
            // 先删除所有部门
            departmentMapper.deleteAllDepartments();
            log.info("成功清空所有部门数据，共删除 {} 个部门", count);
            
            try {
                String operatorId = getCurrentUserId();
                String ipAddress = getClientIpAddress();
                organizationChangeLogService.logDepartmentDelete(
                    "ALL",
                    "清空所有部门数据",
                    operatorId,
                    ipAddress
                );
            } catch (Exception e) {
                LogUtil.logApiError("DELETE", "/departments/all", "记录部门清空日志", e.getMessage(), LogUtil.generateRequestId(), "");
            }
        } else {
            log.info("部门数据为空，无需清空");
        }
    }
} 
