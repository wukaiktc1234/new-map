package com.foodtraceability.service;

import com.foodtraceability.dto.DepartmentBasicInfo;

import java.util.List;
import java.util.Map;

public interface DepartmentDataService {

    Map<Long, DepartmentBasicInfo> batchGetDepartmentBasicInfo(List<Long> departmentIds);

    DepartmentBasicInfo getDepartmentBasicInfo(Long departmentId);

    void clearDepartmentCache(Long departmentId);

    void clearDepartmentBatchCache(List<Long> departmentIds);

    void clearAllDepartmentCache();

    List<Long> getAllDepartmentIds();
}
