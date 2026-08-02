package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.OverAgeWorker;
import com.foodtraceability.mapper.OverAgeWorkerMapper;
import com.foodtraceability.service.OverAgeWorkerService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 超龄劳动者 Service 实现类
 */
@Service
public class OverAgeWorkerServiceImpl implements OverAgeWorkerService {

    private final OverAgeWorkerMapper overAgeWorkerMapper;

    public OverAgeWorkerServiceImpl(OverAgeWorkerMapper overAgeWorkerMapper) {
        this.overAgeWorkerMapper = overAgeWorkerMapper;
    }

    @Override
    public IPage<OverAgeWorker> getList(int page, int size, String keyword, String healthCheckResult) {
        QueryWrapper<OverAgeWorker> queryWrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.and(wrapper -> wrapper
                    .like("employee_name", keyword)
                    .or()
                    .like("employee_id", keyword)
                    .or()
                    .like("agreement_no", keyword)
            );
        }
        if (healthCheckResult != null && !healthCheckResult.isEmpty()) {
            queryWrapper.eq("health_check_result", healthCheckResult);
        }
        queryWrapper.orderByDesc("create_time");
        return overAgeWorkerMapper.selectPage(new Page<>(page, size), queryWrapper);
    }

    @Override
    public OverAgeWorker getById(Long id) {
        return overAgeWorkerMapper.selectById(id);
    }

    @Override
    public OverAgeWorker create(OverAgeWorker entity) {
        entity.setDeleted(0);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        overAgeWorkerMapper.insert(entity);
        return entity;
    }

    @Override
    public OverAgeWorker update(Long id, OverAgeWorker entity) {
        entity.setId(id);
        entity.setUpdateTime(LocalDateTime.now());
        overAgeWorkerMapper.updateById(entity);
        return overAgeWorkerMapper.selectById(id);
    }

    @Override
    public boolean delete(Long id) {
        return overAgeWorkerMapper.deleteById(id) > 0;
    }

    @Override
    public List<OverAgeWorker> getComplianceWarnings() {
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String todayStr = today.format(formatter);
        String warningDateStr = today.plusDays(30).format(formatter);

        // 查询所有未删除的超龄劳动者记录
        List<OverAgeWorker> allWorkers = overAgeWorkerMapper.selectList(
                new QueryWrapper<OverAgeWorker>().eq("deleted", 0)
        );

        // 筛选出有合规预警的记录
        return allWorkers.stream()
                .filter(worker -> hasComplianceWarning(worker, todayStr, warningDateStr))
                .collect(Collectors.toList());
    }

    /**
     * 判断超龄劳动者是否存在合规预警
     * 预警条件：协议即将到期、工伤保险即将到期、健康体检即将到期
     */
    private boolean hasComplianceWarning(OverAgeWorker worker, String todayStr, String warningDateStr) {
        // 检查协议到期预警
        if (worker.getAgreementEndDate() != null
                && worker.getAgreementEndDate().compareTo(todayStr) >= 0
                && worker.getAgreementEndDate().compareTo(warningDateStr) <= 0) {
            return true;
        }
        // 检查工伤保险到期预警
        if (worker.getWorkInjuryInsuranceExpiry() != null
                && worker.getWorkInjuryInsuranceExpiry().compareTo(todayStr) >= 0
                && worker.getWorkInjuryInsuranceExpiry().compareTo(warningDateStr) <= 0) {
            return true;
        }
        // 检查健康体检到期预警
        if (worker.getHealthCheckExpiry() != null
                && worker.getHealthCheckExpiry().compareTo(todayStr) >= 0
                && worker.getHealthCheckExpiry().compareTo(warningDateStr) <= 0) {
            return true;
        }
        return false;
    }

}
