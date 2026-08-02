package com.foodtraceability.service;

import com.foodtraceability.dto.EmployeeBasicInfo;

import java.util.List;
import java.util.Map;

public interface EmployeeDataService {

    Map<String, EmployeeBasicInfo> batchGetEmployeeBasicInfo(List<String> employeeIds);

    EmployeeBasicInfo getEmployeeBasicInfo(String employeeId);

    void clearEmployeeCache(String employeeId);

    void clearEmployeeBatchCache(List<String> employeeIds);

    void clearAllEmployeeCache();
}
