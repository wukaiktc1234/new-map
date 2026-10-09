package com.foodtraceability.service.impl;
import com.foodtraceability.common.util.LocationIdBridge;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.Employee;
import com.foodtraceability.entity.Department;
import com.foodtraceability.entity.Location;
import com.foodtraceability.entity.Position;
import com.foodtraceability.entity.User;
import com.foodtraceability.dto.EmployeeCreateDTO;
import com.foodtraceability.dto.EmployeeUpdateDTO;
import com.foodtraceability.entity.OnboardingArchive;
import com.foodtraceability.entity.OperationLogEntity;
import com.foodtraceability.mapper.EmployeeMapper;
import com.foodtraceability.mapper.LocationMapper;
import com.foodtraceability.mapper.UserMapper;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.security.model.SecurityUser;
import com.foodtraceability.service.EmployeeService;
import com.foodtraceability.service.DepartmentService;
import com.foodtraceability.service.OperationLogService;
import com.foodtraceability.service.OrganizationChangeLogService;
import com.foodtraceability.service.PositionService;
import com.foodtraceability.utils.EmployeeCodeGenerator;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 员工管理服务实现类
 */
@Service
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper, Employee> implements EmployeeService {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(EmployeeServiceImpl.class);

    private final PositionService positionService;

    private final DepartmentService departmentService;

    private final OrganizationChangeLogService organizationChangeLogService;

    private final EmployeeCodeGenerator employeeCodeGenerator;

    /**
     * 位置主表 Mapper（P1-USER-LOCATION-002 D1-3）：校验请求携带的 {@code locationId}
     * 确实存在于 {@code locations}，避免把非法归属写进 {@code employees.location_id}。
     */
    private final LocationMapper locationMapper;

    /**
     * 操作日志服务（P1-USER-LOCATION-002 D3-4）：
     * {@link #assignEmployeeLocation} 的服务级动态审计行（desc 含旧值→新值）。
     */
    private final OperationLogService operationLogService;

    /**
     * 用户 Mapper（P1-USER-LOCATION-002 C4 D3-1）：员工 → users.location_id **反向同步**。
     * 注入 Mapper 而非 UserServiceImpl：UserServiceImpl 已依赖 EmployeeService，反向注入服务会成环。
     */
    private final UserMapper userMapper;

    /**
     * 构造函数注入（禁止 @Autowired 字段注入）
     * 使用 @Lazy 解决循环依赖
     * @param positionService 职位服务
     * @param departmentService 部门服务
     * @param organizationChangeLogService 组织架构变更日志服务
     * @param employeeCodeGenerator 员工编号生成器
     * @param locationMapper 位置主表 Mapper（归属校验）
     * @param operationLogService 操作日志服务（D3-4 审计）
     * @param userMapper 用户 Mapper（C4 D3-1 反向同步）
     */
    public EmployeeServiceImpl(PositionService positionService, @Lazy DepartmentService departmentService,
                               OrganizationChangeLogService organizationChangeLogService,
                               EmployeeCodeGenerator employeeCodeGenerator,
                               LocationMapper locationMapper,
                               OperationLogService operationLogService,
                               UserMapper userMapper) {
        this.positionService = positionService;
        this.departmentService = departmentService;
        this.organizationChangeLogService = organizationChangeLogService;
        this.employeeCodeGenerator = employeeCodeGenerator;
        this.locationMapper = locationMapper;
        this.operationLogService = operationLogService;
        this.userMapper = userMapper;
    }

    @Override
    public List<Employee> getEmployeesByDepartmentId(Long departmentId) {
        QueryWrapper<Employee> wrapper = new QueryWrapper<>();
        wrapper.eq("department_id", departmentId);
        wrapper.eq("status", 1); // 只查询在职员工（1=active）
        return baseMapper.selectList(wrapper);
    }

    @Override
    public List<Employee> getEmployeesByPositionId(Long positionId) {
        QueryWrapper<Employee> wrapper = new QueryWrapper<>();
        wrapper.eq("position_id", positionId);
        wrapper.eq("status", 1); // 只查询在职员工（1=active）
        return baseMapper.selectList(wrapper);
    }

    @Override
    public Employee getEmployeeDetail(String id) {
        // 数据库 employee_id 为 BIGINT，必须将 String 路径参数转为 Long，避免 PostgreSQL 类型不匹配
        Long employeeId = null;
        try {
            employeeId = Long.valueOf(id);
        } catch (NumberFormatException e) {
            log.warn("员工ID格式无效：{}", id);
            return null;
        }
        Employee employee = baseMapper.selectById(employeeId);
        if (employee != null) {
            // 填充部门名称
            if (employee.getDepartmentId() != null) {
                Department department = departmentService.getDepartmentById(employee.getDepartmentId());
                if (department != null) {
                    employee.setDepartmentName(department.getDepartmentName());
                }
            }
            // 填充职位名称
            if (employee.getPositionId() != null) {
                Position position = positionService.getPositionById(employee.getPositionId());
                if (position != null) {
                    employee.setPositionName(position.getPositionName());
                }
            }
        }
        // 处理类型转换异常
        return employee;
    }

    @Override
    public boolean batchUpdateDepartment(List<Long> employeeIds, Long departmentId) {
        for (Long employeeId : employeeIds) {
            Employee employee = new Employee();
            employee.setId(employeeId);
            employee.setDepartmentId(departmentId);
            baseMapper.updateById(employee);
        }
        return true;
    }

    @Override
    public boolean batchUpdatePosition(List<Long> employeeIds, Long positionId) {
        for (Long employeeId : employeeIds) {
            Employee employee = new Employee();
            employee.setId(employeeId);
            employee.setPositionId(positionId);
            baseMapper.updateById(employee);
        }
        return true;
    }

    @Override
    public Integer getEmployeeCountByDepartmentId(Long departmentId) {
        QueryWrapper<Employee> wrapper = new QueryWrapper<>();
        wrapper.eq("department_id", departmentId);
        wrapper.eq("status", 1); // 只统计在职员工（1=active）
        return Math.toIntExact(baseMapper.selectCount(wrapper));
    }

    @Override
    public Integer getEmployeeCountByPositionId(Long positionId) {
        QueryWrapper<Employee> wrapper = new QueryWrapper<>();
        wrapper.eq("position_id", positionId);
        wrapper.eq("status", 1); // 只统计在职员工（1=active）
        return Math.toIntExact(baseMapper.selectCount(wrapper));
    }

    @Override
    public Map<String, Object> getEmployees(int current, int size) {
        try {
            // 查询所有员工
            List<Employee> employees = baseMapper.selectList(null);
            long total = employees.size();
            // 为每个员工填充部门和职位名称
            for (Employee employee : employees) {
                if (employee.getDepartmentId() != null) {
                    Department department = departmentService.getDepartmentById(employee.getDepartmentId());
                    if (department != null) {
                        employee.setDepartmentName(department.getDepartmentName());
                    }
                }
                if (employee.getPositionId() != null) {
                    Position position = positionService.getPositionById(employee.getPositionId());
                    if (position != null) {
                        employee.setPositionName(position.getPositionName());
                    }
                }
            }
            // 构建返回结果
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("records", employees);
            result.put("total", total);
            result.put("current", current);
            result.put("size", size);
            return result;
        } catch (Exception e) {
            log.error("获取员工列表时发生异常", e);
            // 返回空结果
            Map<String, Object> errorResult = new java.util.HashMap<>();
            errorResult.put("records", new java.util.ArrayList<>());
            errorResult.put("total", 0);
            errorResult.put("current", current);
            errorResult.put("size", size);
            return errorResult;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Employee createEmployeeFromArchive(OnboardingArchive archive) {
        if (archive == null) {
            log.error("创建员工失败，入职档案为空");
            return null;
        }
        // 检查员工编号是否已存在
        Employee existingEmployee = getEmployeeByCode(archive.getEmployeeCode());
        if (existingEmployee != null) {
            log.warn("员工编号已存在，员工编号：{}，员工ID：{}", archive.getEmployeeCode(), existingEmployee.getId());
            return existingEmployee;
        }
        // 创建员工记录
        Employee employee = new Employee();
        employee.setEmployeeCode(archive.getEmployeeCode());
        employee.setName(archive.getCandidateName());
        employee.setEmail(archive.getEmail());
        employee.setPhone(archive.getPhone());
        employee.setIdCard(archive.getIdCard());
        if (archive.getDepartmentId() != null && archive.getDepartmentId().matches("^[0-9]+$")) {
            employee.setDepartmentId(Long.parseLong(archive.getDepartmentId()));
        }
        employee.setStatus(1); // 默认在职状态（1=active）
        // 设置入职日期
        if (archive.getOnboardDate() != null) {
            employee.setHireDate(archive.getOnboardDate().atStartOfDay());
        } else {
            employee.setHireDate(LocalDateTime.now());
        }
        // 设置薪资
        if (archive.getFinalSalary() != null) {
            employee.setBaseSalary(archive.getFinalSalary());
        } else if (archive.getExpectedSalary() != null) {
            employee.setBaseSalary(archive.getExpectedSalary());
        }
        // 设置创建时间
        employee.setCreatedTime(LocalDateTime.now());
        // 保存员工
        this.save(employee);
        // 同步增加部门员工数
        if (employee.getDepartmentId() != null) {
            departmentService.incrementEmployeeCount(employee.getDepartmentId());
        }
        // 同步增加岗位在岗人数
        if (employee.getPositionId() != null) {
            positionService.incrementEmployeeCount(employee.getPositionId());
        }
        log.info("从入职档案创建员工成功，档案ID：{}，员工编号：{}，员工姓名：{}", archive.getId(), employee.getEmployeeCode(), employee.getName());
        return employee;
    }

    @Override
    public Employee getEmployeeByCode(String employeeCode) {
        if (employeeCode == null || employeeCode.isEmpty()) {
            return null;
        }
        QueryWrapper<Employee> wrapper = new QueryWrapper<>();
        wrapper.eq("employee_code", employeeCode);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Employee transferEmployee(Long employeeId, Long newDepartmentId, Long newPositionId, LocalDate transferDate, String reason) {
        Employee employee = getById(employeeId);
        if (employee == null) {
            throw new BusinessException(404, "员工不存在");
        }

        Long oldDepartmentId = employee.getDepartmentId();
        Long oldPositionId = employee.getPositionId();

        // 仅当部门或岗位发生变化时才更新
        boolean departmentChanged = newDepartmentId != null && !newDepartmentId.equals(oldDepartmentId);
        boolean positionChanged = newPositionId != null && !newPositionId.equals(oldPositionId);
        if (!departmentChanged && !positionChanged) {
            throw new BusinessException(400, "新部门或新岗位至少有一项需要与当前不同");
        }

        if (departmentChanged) {
            employee.setDepartmentId(newDepartmentId);
        }
        if (positionChanged) {
            employee.setPositionId(newPositionId);
        }
        updateById(employee);

        // 同步部门人数
        if (departmentChanged && oldDepartmentId != null) {
            departmentService.decrementEmployeeCount(oldDepartmentId);
        }
        if (departmentChanged && newDepartmentId != null) {
            departmentService.incrementEmployeeCount(newDepartmentId);
        }

        // 同步岗位在岗人数
        if (positionChanged && oldPositionId != null) {
            positionService.decrementEmployeeCount(oldPositionId);
        }
        if (positionChanged && newPositionId != null) {
            positionService.incrementEmployeeCount(newPositionId);
        }

        // 记录人事变动日志
        String beforeChange = String.format("departmentId=%s, positionId=%s", oldDepartmentId, oldPositionId);
        String afterChange = String.format("departmentId=%s, positionId=%s, transferDate=%s", newDepartmentId, newPositionId, transferDate);
        String operator = getCurrentUserId();
        organizationChangeLogService.recordEmployeeChange(employeeId, employee.getName(), beforeChange, afterChange, reason, operator);

        log.info("员工人事变动完成，员工ID：{}，原部门：{}，新部门：{}，原岗位：{}，新岗位：{}",
            employeeId, oldDepartmentId, newDepartmentId, oldPositionId, newPositionId);
        return getEmployeeDetail(String.valueOf(employeeId));
    }

    /**
     * 归属解析优先级（P1-USER-LOCATION-002 D1-3）：
     * <ol>
     *   <li>{@code locationId} 非空 → 校验存在于 {@code locations} 后直用；</li>
     *   <li>否则 {@code storeId} 非空 → 经 {@link LocationIdBridge#locationIdOfStore} 换算；</li>
     *   <li>否则 {@code warehouseId} 非空 → 经 {@link LocationIdBridge#locationIdOfWarehouse} 换算
     *       （<b>缺口①的新路径</b>：此前仓库归属恒为 NULL）；</li>
     *   <li>全空 → 返回 {@code null}：不落库、不猜、不兜底（不变量 I-3）。</li>
     * </ol>
     *
     * <p>返回 {@code null} 时 MyBatis-Plus 的 {@code updateById} 默认跳过该列，
     * 即"请求未表达归属意图"不会被误清空。
     *
     * @param locationId  新归属入参（首选）
     * @param storeId     观察期兼容入参（已废弃）
     * @param warehouseId 观察期兼容入参（已废弃）
     * @return locations.location_id，或 null
     */
    @SuppressWarnings("deprecation")
    private Long resolveLocationId(Long locationId, Long storeId, Long warehouseId) {
        if (locationId != null) {
            Location location = locationMapper.selectById(locationId);
            if (location == null || Integer.valueOf(1).equals(location.getDeleted())) {
                throw new BusinessException(400, "归属位置不存在或已删除：" + locationId);
            }
            if (Integer.valueOf(0).equals(location.getStatus())) {
                log.warn("归属位置已停用（status=0），仍按请求写入：locationId={}", locationId);
            }
            return location.getLocationId();
        }
        if (storeId != null) {
            return LocationIdBridge.locationIdOfStore(storeId);
        }
        if (warehouseId != null) {
            return LocationIdBridge.locationIdOfWarehouse(warehouseId);
        }
        return null;
    }

    /**
     * U9 映射（p1-user-location-002-design-001.md §7 U9）：
     * {@code locations.location_type} → {@code employees.work_location_type}。
     *
     * <p>两侧词表被各自 DB CHECK 硬约束且<b>不重合</b>（F-2）：
     * <ul>
     *   <li>{@code STORE} → {@code STORE}</li>
     *   <li>{@code CENTRAL} / {@code DEPOT} → {@code WAREHOUSE}</li>
     *   <li>{@code TRANSIT} 禁止作员工归属；词表外值（含 null）同处理 → 400 显式拒绝，<b>不猜</b></li>
     * </ul>
     *
     * @param locationType locations.location_type 值
     * @return employees.work_location_type 值
     * @throws BusinessException 400，当 TRANSIT 或词表外值
     */
    static String mapLocationTypeToWorkLocationType(String locationType) {
        if ("STORE".equals(locationType)) {
            return "STORE";
        }
        if ("CENTRAL".equals(locationType) || "DEPOT".equals(locationType)) {
            return "WAREHOUSE";
        }
        throw new BusinessException(400, "该位置类型不可作为员工归属：" + locationType);
    }

    /**
     * U10：按解析出的归属显式推导 {@code workLocationType}（不走 DB DEFAULT HEADQUARTERS）。
     *
     * <ul>
     *   <li>locationId 路径 → 主键回查 {@code locations.location_type} 走 U9 映射；</li>
     *   <li>storeId/warehouseId 桥路径 → 按来源推导（STORE / WAREHOUSE），不查库
     *       （与 C2 桥语义一致：桥分支不做存在性校验）；</li>
     * </ul>
     *
     * <p>调用方保证 resolvedLocationId 非空：解析为 null 时创建路径显式落
     * {@code HEADQUARTERS}（§3.2 总部职能 NULL 场景），更新路径不 set
     * （null → updateById 跳过该列，I-3）。
     *
     * @param resolvedLocationId resolveLocationId 解析结果（非空）
     * @param locationIdParam    请求入参 locationId（null 表示桥路径）
     * @param storeId            请求入参 storeId（观察期兼容）
     * @param warehouseId        请求入参 warehouseId（观察期兼容）
     * @return 显式 workLocationType 值
     */
    @SuppressWarnings("deprecation")
    private String deriveWorkLocationType(Long resolvedLocationId, Long locationIdParam, Long storeId, Long warehouseId) {
        if (locationIdParam == null) {
            // 桥路径：storeId → STORE；warehouseId → WAREHOUSE（按来源推导，与桥分支不查库一致）
            return storeId != null ? "STORE" : "WAREHOUSE";
        }
        Location location = locationMapper.selectById(resolvedLocationId);
        return mapLocationTypeToWorkLocationType(location == null ? null : location.getLocationType());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @SuppressWarnings("deprecation") // 观察期兼容：旧归属入参仍可用，见 resolveLocationId
    public Employee createEmployee(EmployeeCreateDTO dto) {
        Employee employee = new Employee();
        employee.setName(dto.getName());
        employee.setGender(dto.getGender());
        employee.setDepartmentId(dto.getDepartmentId());
        employee.setPositionId(dto.getPositionId());
        Long locationId = resolveLocationId(dto.getLocationId(), dto.getStoreId(), dto.getWarehouseId());
        employee.setLocationId(locationId);
        // U10：显式 set workLocationType——有 locationId 按 U9 映射推导；无 locationId → 显式 HEADQUARTERS（不走 DB DEFAULT）
        employee.setWorkLocationType(locationId == null
                ? "HEADQUARTERS"
                : deriveWorkLocationType(locationId, dto.getLocationId(), dto.getStoreId(), dto.getWarehouseId()));
        employee.setWarehouseId(dto.getWarehouseId());
        employee.setHireDate(dto.getHireDate());
        employee.setStatus(dto.getStatus());
        String employeeCode = employeeCodeGenerator.generateEmployeeCode(
            dto.getDepartmentId() != null ? dto.getDepartmentId().toString() : null, null);
        employee.setEmployeeCode(employeeCode);
        employee.setCreatedTime(LocalDateTime.now());
        this.save(employee);

        // 同步更新岗位在岗人数（仅在职/试用期才计入）
        if (employee.getPositionId() != null && employee.getStatus() != null && employee.getStatus() != 0) {
            positionService.incrementEmployeeCount(employee.getPositionId());
        }
        // 同步更新部门在岗人数（仅在职/试用期才计入）
        if (employee.getDepartmentId() != null && employee.getStatus() != null && employee.getStatus() != 0) {
            departmentService.incrementEmployeeCount(employee.getDepartmentId());
        }
        return employee;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @SuppressWarnings("deprecation") // 观察期兼容：旧归属入参仍可用，见 resolveLocationId
    public Employee updateEmployee(String id, EmployeeUpdateDTO dto) {
        Long employeeId;
        try {
            employeeId = Long.valueOf(id);
        } catch (NumberFormatException e) {
            throw new BusinessException(400, "员工ID格式无效");
        }
        Employee oldEmployee = getById(employeeId);
        if (oldEmployee == null) {
            throw new BusinessException(404, "员工不存在");
        }

        Employee employee = new Employee();
        employee.setId(employeeId);
        employee.setName(dto.getName());
        employee.setGender(dto.getGender());
        employee.setDepartmentId(dto.getDepartmentId());
        employee.setPositionId(dto.getPositionId());
        Long locationId = resolveLocationId(dto.getLocationId(), dto.getStoreId(), dto.getWarehouseId());
        employee.setLocationId(locationId);
        // U10/U9：请求表达了归属意图（locationId 可解析）→ 显式推导 workLocationType；
        // 未表达（全空 → null）→ 不 set，MyBatis-Plus updateById 跳过该列，存量值不被清空（I-3）
        employee.setWorkLocationType(locationId == null
                ? null
                : deriveWorkLocationType(locationId, dto.getLocationId(), dto.getStoreId(), dto.getWarehouseId()));
        employee.setWarehouseId(dto.getWarehouseId());
        employee.setHireDate(dto.getHireDate());
        employee.setStatus(dto.getStatus() != null ? dto.getStatus() : oldEmployee.getStatus());
        updateById(employee);

        // C4（D3-1）：表达了归属（locationId 可解析）→ 反向同步对应用户（入口决定方向：员工入口 → 反向；防写回环）
        // U2：找不到对应用户 → 不动 users、不报错；未表达归属（locationId=null）→ 不调用
        if (locationId != null) {
            syncUserAssignmentFromEmployee(employeeId, locationId);
        }

        // 同步更新岗位在岗人数
        syncPositionEmployeeCount(oldEmployee, employee);
        // 同步更新部门在岗人数
        syncDepartmentEmployeeCount(oldEmployee, employee);

        return getEmployeeDetail(id);
    }

    /**
     * 员工归属分配（P1-USER-LOCATION-002 C3，D3-2/D3-3/D3-4）。
     *
     * <p>只改「归属」两列：{@code location_id} + {@code work_location_type}
     * （后者由后端按 U9 显式推导，前端不得提交）；其余档案字段一律不动，
     * 与 {@link #updateEmployee} 的「改档案」职责分离（D3-2 拆分要求）。
     *
     * <p>校验语义（与 C2b 一致）：位置不存在/已删除 → 400；
     * {@code location_type = TRANSIT}（或词表外值）→ 400 显式拒绝，不猜。
     *
     * <p>审计（D3-4）：模块 EMPLOYEE_LOCATION_ASSIGN、type UPDATE、
     * desc 含「旧值 → 新值」——{@code @OperationLog} 注解 desc 为静态，
     * 动态旧值行参照 TaxRecordServiceImpl#logIdempotencyHit 的服务级补行先例。
     *
     * <p>C4（D3-1）将在 {@code updateById} 之后接入反向同步
     * {@code syncUserAssignmentFromEmployee}（入口决定方向：员工入口→反向，防回写循环）。
     *
     * @param id 员工ID
     * @param locationId 目标位置ID（非空；controller 层 @NotNull 校验）
     * @return 更新后的员工（含部门/岗位名称填充）
     * @throws BusinessException 400 员工ID格式无效/位置不存在或已删除/位置类型不可作归属；404 员工不存在
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Employee assignEmployeeLocation(String id, Long locationId) {
        Long employeeId;
        try {
            employeeId = Long.valueOf(id);
        } catch (NumberFormatException e) {
            throw new BusinessException(400, "员工ID格式无效");
        }
        Employee oldEmployee = getById(employeeId);
        if (oldEmployee == null) {
            throw new BusinessException(404, "员工不存在");
        }

        // 归属解析与校验（D1-3：不存在/已删除 → 400；已停用 status=0 仅告警放行）
        Long resolvedLocationId = resolveLocationId(locationId, null, null);
        // U9/U10：主键回查 location_type 推导 workLocationType（TRANSIT/词表外 → 400）
        String workLocationType = deriveWorkLocationType(resolvedLocationId, resolvedLocationId, null, null);

        Employee employee = new Employee();
        employee.setId(employeeId);
        employee.setLocationId(resolvedLocationId);
        employee.setWorkLocationType(workLocationType);
        employee.setUpdatedTime(LocalDateTime.now());
        employee.setUpdatedBy(currentOperatorName());
        updateById(employee);

        // C4（D3-1）：反向同步（入口决定方向：员工入口 → 反向；与 UserServiceImpl#syncEmployeeAssignment 正向同步互参，防写回环）
        String syncDesc = syncUserAssignmentFromEmployee(employeeId, resolvedLocationId);

        // D3-4 审计：服务级动态行，desc 含旧值 → 新值（含 users 侧旧值 → 新值，U1）
        logLocationAssignAudit(employeeId, oldEmployee, resolvedLocationId, workLocationType, syncDesc);

        return getEmployeeDetail(id);
    }

    /**
     * C4（D3-1）：员工 → 用户 **反向同步**（P1-USER-LOCATION-002）。
     *
     * <p>入口决定方向（D3-2 防写回环）：用户入口（assignStore / unassignStore）→ 正向同步
     * {@code UserServiceImpl#syncEmployeeAssignment}（写 employees 表）；
     * 员工入口（{@link #updateEmployee} 归属变更 / {@link #assignEmployeeLocation}）→ 反向同步（本方法，
     * 写 users 表）。每个方向各写对侧表一次，被调方法内不再反向触发，防止写回环。
     *
     * <p>关联键沿用既有约定：{@code employees.user_id} 优先（V20260930_002 建列回填），
     * {@code employee_code} 兜底（D3-1）。
     *
     * <p>U1（last-write-wins）：用户已有不同归属时，以本次员工侧写入为准，
     * 审计 desc 记录「旧值 → 新值」。U2（只写员工表）：找不到对应用户 → 不动
     * {@code users}、不建号、不抛错。
     *
     * <p>部分更新：仅写 {@code users.location_id} + {@code updated_time}，绝不触碰密码等其他字段。
     *
     * @param employeeId 员工ID（员工侧归属已写入）
     * @param newLocationId 新归属位置ID（非空；调用方未表达归属意图时不应调用）
     * @return users 侧审计 desc 片段（旧 → 新 / 未重复写 / 未动）
     */
    private String syncUserAssignmentFromEmployee(Long employeeId, Long newLocationId) {
        Employee employee = getById(employeeId);
        if (employee == null) {
            return "员工记录不存在，users 未动";
        }
        // 关联键：employees.user_id 优先，employee_code 兜底（D3-1）
        User user = null;
        if (employee.getUserId() != null) {
            user = userMapper.selectById(employee.getUserId());
        }
        if (user == null && employee.getEmployeeCode() != null && !employee.getEmployeeCode().isBlank()) {
            QueryWrapper<User> wrapper = new QueryWrapper<>();
            wrapper.eq("employee_code", employee.getEmployeeCode());
            user = userMapper.selectOne(wrapper.last("LIMIT 1"));
        }
        if (user == null) {
            log.debug("员工未找到对应用户，跳过反向同步（U2）: employeeId={}, userId={}, employeeCode={}",
                    employeeId, employee.getUserId(), employee.getEmployeeCode());
            return "未找到对应用户，users 未动（U2）";
        }
        Long oldUserLocationId = user.getLocationId();
        if (java.util.Objects.equals(oldUserLocationId, newLocationId)) {
            return "users.location_id 已是新值（" + newLocationId + "），未重复写";
        }
        // U1：last-write-wins（部分更新，不触碰其他字段）
        User update = new User();
        update.setId(user.getId());
        update.setLocationId(newLocationId);
        update.setUpdatedTime(LocalDateTime.now());
        userMapper.updateById(update);
        return "反向同步 users.location_id " + oldUserLocationId + " → " + newLocationId;
    }

    /**
     * D3-4 服务级动态审计行（模块 EMPLOYEE_LOCATION_ASSIGN，desc 含旧值→新值）。
     *
     * <p>参照 TaxRecordServiceImpl#logIdempotencyHit 先例：审计失败仅告警，不阻断主流程。
     *
     * @param syncDesc C4 反向同步 desc 片段（users 侧旧 → 新 / 未重复写 / 未动）
     */
    private void logLocationAssignAudit(Long employeeId, Employee oldEmployee,
                                        Long newLocationId, String newWorkLocationType, String syncDesc) {
        try {
            OperationLogEntity opLog = new OperationLogEntity();
            opLog.setOperationModule("EMPLOYEE_LOCATION_ASSIGN");
            opLog.setOperationType("UPDATE");
            opLog.setOperationDesc("员工归属分配：locationId " + oldEmployee.getLocationId() + " → " + newLocationId
                    + "；workLocationType " + oldEmployee.getWorkLocationType() + " → " + newWorkLocationType
                    + (syncDesc == null || syncDesc.isBlank() ? "" : "；" + syncDesc));
            opLog.setRequestMethod("PUT");
            opLog.setRequestUrl("/v1/employees/" + employeeId + "/assign-location");
            opLog.setRequestParams("{\"locationId\":" + newLocationId + "}");
            opLog.setBusinessId(String.valueOf(employeeId));
            opLog.setStatus("SUCCESS");
            opLog.setOperationTime(LocalDateTime.now());
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof SecurityUser) {
                SecurityUser user = (SecurityUser) authentication.getPrincipal();
                opLog.setOperatorId(user.getUserId());
                opLog.setOperatorName(user.getUsername());
            }
            operationLogService.logOperation(opLog);
        } catch (Exception e) {
            log.warn("记录员工归属分配审计日志失败: {}", e.getMessage());
        }
    }

    /**
     * 当前操作人用户名（安全上下文无主或异常时返回 null，不阻断主流程）。
     */
    private String currentOperatorName() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof SecurityUser) {
                return ((SecurityUser) authentication.getPrincipal()).getUsername();
            }
        } catch (Exception ignored) {
            // 操作人信息缺失不阻断主流程
        }
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteEmployee(String id) {
        Long employeeId;
        try {
            employeeId = Long.valueOf(id);
        } catch (NumberFormatException e) {
            throw new BusinessException(400, "员工ID格式无效");
        }
        Employee existingEmployee = getById(employeeId);
        if (existingEmployee == null) {
            return false;
        }
        boolean removed = removeById(employeeId);
        if (removed) {
            // 同步更新岗位在岗人数（仅在职/试用期员工离职时才扣减）
            if (existingEmployee.getPositionId() != null
                    && existingEmployee.getStatus() != null && existingEmployee.getStatus() != 0) {
                positionService.decrementEmployeeCount(existingEmployee.getPositionId());
            }
            // 同步更新部门在岗人数（仅在职/试用期员工离职时才扣减）
            if (existingEmployee.getDepartmentId() != null
                    && existingEmployee.getStatus() != null && existingEmployee.getStatus() != 0) {
                departmentService.decrementEmployeeCount(existingEmployee.getDepartmentId());
            }
        }
        return removed;
    }

    /**
     * 同步岗位在岗人数：根据员工岗位与状态变化，对旧岗位减员、新岗位增员
     */
    private void syncPositionEmployeeCount(Employee oldEmployee, Employee newEmployee) {
        if (oldEmployee == null) {
            return;
        }
        Long oldPositionId = oldEmployee.getPositionId();
        Long newPositionId = newEmployee.getPositionId() != null ? newEmployee.getPositionId() : oldPositionId;
        Integer oldStatus = oldEmployee.getStatus();
        Integer newStatus = newEmployee.getStatus();
        boolean oldActive = oldStatus != null && oldStatus != 0;
        boolean newActive = newStatus != null && newStatus != 0;

        // 岗位变更：旧岗位减员、新岗位增员
        if (!java.util.Objects.equals(oldPositionId, newPositionId)) {
            if (oldPositionId != null && oldActive) {
                positionService.decrementEmployeeCount(oldPositionId);
            }
            if (newPositionId != null && newActive) {
                positionService.incrementEmployeeCount(newPositionId);
            }
            return;
        }

        // 岗位未变但状态变化：在职/试用期 ↔ 离职
        if (oldActive && !newActive && oldPositionId != null) {
            positionService.decrementEmployeeCount(oldPositionId);
        } else if (!oldActive && newActive && newPositionId != null) {
            positionService.incrementEmployeeCount(newPositionId);
        }
    }

    /**
     * 同步部门在岗人数：根据员工部门与状态变化，对旧部门减员、新部门增员
     */
    private void syncDepartmentEmployeeCount(Employee oldEmployee, Employee newEmployee) {
        if (oldEmployee == null) {
            return;
        }
        Long oldDepartmentId = oldEmployee.getDepartmentId();
        Long newDepartmentId = newEmployee.getDepartmentId() != null ? newEmployee.getDepartmentId() : oldDepartmentId;
        Integer oldStatus = oldEmployee.getStatus();
        Integer newStatus = newEmployee.getStatus() != null ? newEmployee.getStatus() : oldStatus;
        boolean oldActive = oldStatus != null && oldStatus != 0;
        boolean newActive = newStatus != null && newStatus != 0;

        // 部门变更：旧部门减员、新部门增员
        if (!java.util.Objects.equals(oldDepartmentId, newDepartmentId)) {
            if (oldDepartmentId != null && oldActive) {
                departmentService.decrementEmployeeCount(oldDepartmentId);
            }
            if (newDepartmentId != null && newActive) {
                departmentService.incrementEmployeeCount(newDepartmentId);
            }
            return;
        }

        // 部门未变但状态变化：在职/试用期 ↔ 离职
        if (oldActive && !newActive && oldDepartmentId != null) {
            departmentService.decrementEmployeeCount(oldDepartmentId);
        } else if (!oldActive && newActive && newDepartmentId != null) {
            departmentService.incrementEmployeeCount(newDepartmentId);
        }
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
}
