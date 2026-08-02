package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.RecruitmentRequirement;

import java.util.List;
import java.util.Map;

public interface RecruitmentRequirementService extends IService<RecruitmentRequirement> {

    Map<String, Object> getRecruitmentRequirements(int current, int size, String type, String status,
                                                   String keyword, String departmentId, String startDate, String endDate);

    RecruitmentRequirement createRequirement(RecruitmentRequirement requirement);

    RecruitmentRequirement updateRequirement(String id, RecruitmentRequirement requirement);

    void deleteRequirement(String id);

    void approveRequirement(String id, String approvalStatus);

    List<RecruitmentRequirement> getRequirementsByStore(String storeId);

    List<RecruitmentRequirement> getRequirementsByDepartment(String departmentId);
}
