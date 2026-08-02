package com.foodtraceability.common.util;

import com.foodtraceability.dto.PurchaseArrivalQueryDTO;
import com.foodtraceability.dto.ReceiptConfirmationQueryDTO;
import com.foodtraceability.entity.Employee;
import com.foodtraceability.entity.User;
import com.foodtraceability.service.EmployeeService;
import com.foodtraceability.service.UserService;
import com.foodtraceability.utils.SecurityUtils;
import org.springframework.stereotype.Component;

/**
 * 工作归属权限过滤辅助类
 * 根据当前登录员工的门店/仓库归属自动过滤业务数据
 */
@Component
public class WorkLocationFilterHelper {

    private static final String LOCATION_STORE = "STORE";
    private static final String LOCATION_WAREHOUSE = "WAREHOUSE";
    private static final String LOCATION_HEADQUARTERS = "HEADQUARTERS";

    private final UserService userService;
    private final EmployeeService employeeService;

    public WorkLocationFilterHelper(UserService userService, EmployeeService employeeService) {
        this.userService = userService;
        this.employeeService = employeeService;
    }

    /**
     * 获取当前登录用户关联的员工信息
     * @return 员工信息，未找到返回null
     */
    public Employee getCurrentEmployee() {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            return null;
        }
        User user = userService.getUserByUsername(username);
        if (user == null || user.getEmployeeCode() == null || user.getEmployeeCode().isEmpty()) {
            return null;
        }
        return employeeService.getEmployeeByCode(user.getEmployeeCode());
    }

    /**
     * 应用到货单查询的工作归属过滤
     * 门店员工只看本门店到货单，仓库员工只看本仓库到货单，其他用户看全部
     * @param queryDTO 查询条件
     */
    public void applyArrivalFilter(PurchaseArrivalQueryDTO queryDTO) {
        if (queryDTO == null) {
            return;
        }
        if (SecurityUtils.isAdmin()) {
            return;
        }
        Employee employee = getCurrentEmployee();
        if (employee == null || employee.getWorkLocationType() == null) {
            return;
        }
        String locationType = employee.getWorkLocationType();
        if (LOCATION_STORE.equals(locationType)) {
            queryDTO.setReceiverType("STORE");
            queryDTO.setStoreId(employee.getStoreId() != null ? String.valueOf(employee.getStoreId()) : null);
            queryDTO.setWarehouseId(null);
        } else if (LOCATION_WAREHOUSE.equals(locationType)) {
            queryDTO.setReceiverType("WAREHOUSE");
            queryDTO.setWarehouseId(employee.getWarehouseId());
            queryDTO.setStoreId(null);
        }
        // HEADQUARTERS 不限制，可查看全部
    }

    /**
     * 应用收货确认单查询的工作归属过滤
     * 门店员工只看本门店确认单，仓库员工只看本仓库确认单，其他用户看全部
     * @param queryDTO 查询条件
     */
    public void applyConfirmationFilter(ReceiptConfirmationQueryDTO queryDTO) {
        if (queryDTO == null) {
            return;
        }
        if (SecurityUtils.isAdmin()) {
            return;
        }
        Employee employee = getCurrentEmployee();
        if (employee == null || employee.getWorkLocationType() == null) {
            return;
        }
        String locationType = employee.getWorkLocationType();
        if (LOCATION_STORE.equals(locationType)) {
            queryDTO.setReceiverType("STORE");
            queryDTO.setStoreId(employee.getStoreId() != null ? String.valueOf(employee.getStoreId()) : null);
            queryDTO.setWarehouseId(null);
        } else if (LOCATION_WAREHOUSE.equals(locationType)) {
            queryDTO.setReceiverType("WAREHOUSE");
            queryDTO.setWarehouseId(employee.getWarehouseId());
            queryDTO.setStoreId(null);
        }
        // HEADQUARTERS 不限制，可查看全部
    }

    /**
     * 判断当前用户是否为门店人员
     */
    public boolean isStoreStaff() {
        Employee employee = getCurrentEmployee();
        return employee != null && LOCATION_STORE.equals(employee.getWorkLocationType());
    }

    /**
     * 判断当前用户是否为仓库人员
     */
    public boolean isWarehouseStaff() {
        Employee employee = getCurrentEmployee();
        return employee != null && LOCATION_WAREHOUSE.equals(employee.getWorkLocationType());
    }

    /**
     * 获取当前用户的门店ID
     */
    public String getCurrentStoreId() {
        Employee employee = getCurrentEmployee();
        return employee != null && employee.getStoreId() != null ? String.valueOf(employee.getStoreId()) : null;
    }

    /**
     * 获取当前用户的仓库ID
     */
    public Long getCurrentWarehouseId() {
        Employee employee = getCurrentEmployee();
        return employee != null ? employee.getWarehouseId() : null;
    }
}
