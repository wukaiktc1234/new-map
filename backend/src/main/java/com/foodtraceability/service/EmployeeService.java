package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.EmployeeCreateDTO;
import com.foodtraceability.dto.EmployeeUpdateDTO;
import com.foodtraceability.entity.Employee;
import com.foodtraceability.entity.OnboardingArchive;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 员工管理服务接口
 */
public interface EmployeeService extends IService<Employee> {

    /**
     * 获取员工列表（分页）
     * @param current 当前页码
     * @param size 每页大小
     * @return 分页员工数据
     */
    Map<String, Object> getEmployees(int current, int size);

    /**
     * 根据部门ID获取员工列表
     * @param departmentId 部门ID
     * @return 员工列表
     */
    List<Employee> getEmployeesByDepartmentId(Long departmentId);

    /**
     * 根据职位ID获取员工列表
     * @param positionId 职位ID
     * @return 员工列表
     */
    List<Employee> getEmployeesByPositionId(Long positionId);

    /**
     * 获取员工详情（包含部门和职位信息）
     * @param id 员工ID
     * @return 员工详情
     */
    Employee getEmployeeDetail(String id);

    /**
     * 批量更新员工部门
     * @param employeeIds 员工ID列表
     * @param departmentId 新部门ID
     * @return 更新结果
     */
    boolean batchUpdateDepartment(List<Long> employeeIds, Long departmentId);

    /**
     * 批量更新员工职位
     * @param employeeIds 员工ID列表
     * @param positionId 新职位ID
     * @return 更新结果
     */
    boolean batchUpdatePosition(List<Long> employeeIds, Long positionId);

    /**
     * 获取部门员工数量
     * @param departmentId 部门ID
     * @return 员工数量
     */
    Integer getEmployeeCountByDepartmentId(Long departmentId);

    /**
     * 获取职位员工数量
     * @param positionId 职位ID
     * @return 员工数量
     */
    Integer getEmployeeCountByPositionId(Long positionId);

    /**
     * 从入职档案创建员工
     * @param archive 入职档案
     * @return 创建的员工
     */
    Employee createEmployeeFromArchive(OnboardingArchive archive);

    /**
     * 根据员工编号获取员工
     * @param employeeCode 员工编号
     * @return 员工信息
     */
    Employee getEmployeeByCode(String employeeCode);

    /**
     * 员工人事变动（调岗/调部门）
     * @param employeeId 员工ID
     * @param newDepartmentId 新部门ID
     * @param newPositionId 新职位ID
     * @param transferDate 变动生效日期
     * @param reason 变动原因
     * @return 变动后的员工信息
     */
    Employee transferEmployee(Long employeeId, Long newDepartmentId, Long newPositionId, LocalDate transferDate, String reason);

    /**
     * 创建员工并同步部门/岗位在岗人数
     * @param dto 员工创建DTO
     * @return 创建后的员工
     */
    Employee createEmployee(EmployeeCreateDTO dto);

    /**
     * 更新员工并同步部门/岗位在岗人数
     * @param id 员工ID
     * @param dto 员工更新DTO
     * @return 更新后的员工
     */
    Employee updateEmployee(String id, EmployeeUpdateDTO dto);

    /**
     * 删除员工并同步部门/岗位在岗人数
     * @param id 员工ID
     * @return 是否成功
     */
    boolean deleteEmployee(String id);
}
