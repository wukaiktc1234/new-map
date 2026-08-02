package com.foodtraceability.service.impl;

import com.foodtraceability.entity.Employee;
import com.foodtraceability.entity.HealthCertificate;
import com.foodtraceability.mapper.EmployeeMapper;
import com.foodtraceability.service.HrSystemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * HR系统服务实现类
 * <p>
 * 用于处理与 HR 系统的数据同步。健康证数据同步会将健康证到期日期
 * 写入员工档案，实现健康证→人事管理的链路连通。
 * </p>
 * <p>
 * 其他对外部 HR 系统的查询/同步接口仍为占位实现，待外部系统对接后替换。
 * </p>
 */
@Service
public class HrSystemServiceImpl implements HrSystemService {

    private static final Logger log = LoggerFactory.getLogger(HrSystemServiceImpl.class);

    private final EmployeeMapper employeeMapper;

    public HrSystemServiceImpl(EmployeeMapper employeeMapper) {
        this.employeeMapper = employeeMapper;
    }

    @Override
    public boolean syncHealthCertificateToHrSystem(HealthCertificate healthCertificate) {
        if (healthCertificate == null) {
            log.warn("健康证数据同步至HR系统失败：健康证数据为空");
            return false;
        }
        if (healthCertificate.getEmployeeId() == null) {
            log.warn("健康证数据同步至HR系统失败：员工ID为空，健康证ID: {}", healthCertificate.getId());
            return false;
        }
        try {
            // 根据员工ID查询员工档案
            Employee employee = employeeMapper.selectById(healthCertificate.getEmployeeId());
            if (employee == null) {
                log.warn("健康证数据同步至HR系统失败：未找到员工，员工ID: {}, 健康证ID: {}",
                        healthCertificate.getEmployeeId(), healthCertificate.getId());
                return false;
            }
            // 更新员工的健康证到期日期
            employee.setHealthCertificateExpiryDate(healthCertificate.getExpiryDate());
            int rows = employeeMapper.updateById(employee);
            if (rows > 0) {
                log.info("健康证数据同步至HR系统成功，健康证ID: {}, 员工ID: {}, 健康证到期日期: {}",
                        healthCertificate.getId(), healthCertificate.getEmployeeId(), healthCertificate.getExpiryDate());
                return true;
            } else {
                log.error("健康证数据同步至HR系统失败：更新员工记录失败，员工ID: {}, 健康证ID: {}",
                        healthCertificate.getEmployeeId(), healthCertificate.getId());
                return false;
            }
        } catch (Exception e) {
            log.error("健康证数据同步至HR系统失败，健康证ID: {}, 员工ID: {}, 错误信息: {}",
                    healthCertificate.getId(), healthCertificate.getEmployeeId(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Object getEmployeeInfoFromHrSystem(String employeeId) {
        try {
            // 占位实现：外部 HR 系统对接待实现，当前返回 null 表示占位
            log.warn("外部HR系统对接待实现 - 从HR系统获取员工信息占位返回 null，员工ID: {}", employeeId);
            return null;
        } catch (Exception e) {
            log.error("从HR系统获取员工信息失败，员工ID: {}, 错误信息: {}", employeeId, e.getMessage());
            return null;
        }
    }

    @Override
    public boolean syncHealthCertificateApprovalResult(HealthCertificate healthCertificate) {
        try {
            // 占位实现：外部 HR 系统对接待实现，当前返回 true 表示占位成功
            log.warn("外部HR系统对接待实现 - 健康证审核结果同步占位成功，健康证ID: {}, 审核状态: {}",
                    healthCertificate.getId(), healthCertificate.getApprovalStatus());
            return true;
        } catch (Exception e) {
            log.error("健康证审核结果同步至HR系统失败，健康证ID: {}, 错误信息: {}",
                    healthCertificate.getId(), e.getMessage());
            return false;
        }
    }
}
