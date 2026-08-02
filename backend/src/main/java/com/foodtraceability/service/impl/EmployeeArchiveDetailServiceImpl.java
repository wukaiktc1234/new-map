package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.Employee;
import com.foodtraceability.entity.EmployeeArchiveDetail;
import com.foodtraceability.entity.OnboardingArchive;
import com.foodtraceability.mapper.EmployeeArchiveDetailMapper;
import com.foodtraceability.mapper.EmployeeMapper;
import com.foodtraceability.mapper.OnboardingArchiveMapper;
import com.foodtraceability.service.EmployeeArchiveDetailService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 员工档案详细服务实现
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-03-20
 */
@Service
public class EmployeeArchiveDetailServiceImpl extends ServiceImpl<EmployeeArchiveDetailMapper, EmployeeArchiveDetail> implements EmployeeArchiveDetailService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EmployeeArchiveDetailServiceImpl.class);

    public EmployeeArchiveDetailServiceImpl(EmployeeMapper employeeMapper, OnboardingArchiveMapper archiveMapper) {
        this.employeeMapper = employeeMapper;
        this.archiveMapper = archiveMapper;
    }

    private final EmployeeMapper employeeMapper;
    private final OnboardingArchiveMapper archiveMapper;

    @Override
    public EmployeeArchiveDetail getByArchiveId(Long archiveId) {
        return baseMapper.selectByArchiveId(archiveId);
    }

    @Override
    public EmployeeArchiveDetail getByEmployeeId(String employeeId) {
        return baseMapper.selectByEmployeeId(employeeId);
    }

    @Override
    public int calculateCompleteness(EmployeeArchiveDetail detail) {
        if (detail == null) return 0;
        int score = 0;
        // 基本信息（30分）
        if (StringUtils.hasText(detail.getRealName())) score += 5;
        if (StringUtils.hasText(detail.getGender())) score += 3;
        if (detail.getBirthday() != null) score += 3;
        if (StringUtils.hasText(detail.getCurrentAddress())) score += 4;
        if (StringUtils.hasText(detail.getEmergencyContact())) score += 5;
        if (StringUtils.hasText(detail.getEmergencyPhone())) score += 5;
        if (StringUtils.hasText(detail.getEmergencyRelationship())) score += 5;
        // 教育背景（20分）
        if (StringUtils.hasText(detail.getEducationLevel())) score += 8;
        if (StringUtils.hasText(detail.getGraduationSchool())) score += 6;
        if (StringUtils.hasText(detail.getMajor())) score += 3;
        if (detail.getGraduationDate() != null) score += 3;
        // 银行信息（15分）
        if (StringUtils.hasText(detail.getBankName())) score += 5;
        if (StringUtils.hasText(detail.getBankCard())) score += 10;
        // 附件材料（20分）
        if (StringUtils.hasText(detail.getIdCardFrontUrl())) score += 5;
        if (StringUtils.hasText(detail.getIdCardBackUrl())) score += 5;
        if (StringUtils.hasText(detail.getPhotoUrl())) score += 5;
        if (StringUtils.hasText(detail.getDiplomaUrl())) score += 5;
        // 隐私协议（15分）
        if (detail.getPrivacyAgreementSigned() != null && detail.getPrivacyAgreementSigned() == 1) score += 10;
        if (detail.getDataAccuracyConfirmed() != null && detail.getDataAccuracyConfirmed() == 1) score += 5;
        return Math.min(score, 100);
    }

    @Override
    @Transactional
    public void updateCompleteness(Long detailId) {
        EmployeeArchiveDetail detail = getById(detailId);
        if (detail != null) {
            int score = calculateCompleteness(detail);
            detail.setCompletenessScore(score);
            updateById(detail);
        }
    }

    @Override
    @Transactional
    public void reviewArchive(Long detailId, String reviewStatus, String reviewComment, Long reviewerId) {
        EmployeeArchiveDetail detail = getById(detailId);
        if (detail == null) {
            throw new RuntimeException("档案不存在");
        }
        detail.setReviewStatus(reviewStatus);
        detail.setReviewComment(reviewComment);
        detail.setReviewBy(reviewerId);
        detail.setReviewTime(LocalDateTime.now());
        updateById(detail);
        // 如果审核通过，同步数据到员工表
        if (EmployeeArchiveDetail.STATUS_APPROVED.equals(reviewStatus)) {
            OnboardingArchive archive = archiveMapper.selectById(detail.getArchiveId());
            if (archive != null && archive.getEmployeeCode() != null) {
                syncToEmployee(archive.getEmployeeCode());
            }
        }
        log.info("档案审核完成，档案ID：{}，状态：{}", detailId, reviewStatus);
    }

    @Override
    @Transactional
    public void syncToEmployee(String employeeCode) {
        // 查询员工
        LambdaQueryWrapper<Employee> empWrapper = new LambdaQueryWrapper<>();
        empWrapper.eq(Employee::getEmployeeCode, employeeCode);
        Employee employee = employeeMapper.selectOne(empWrapper);
        if (employee == null) {
            log.warn("员工不存在，无法同步：{}", employeeCode);
            return;
        }
        // 查询档案
        OnboardingArchive archive = archiveMapper.selectByEmployeeCode(employeeCode);
        if (archive == null) {
            log.warn("入职档案不存在：{}", employeeCode);
            return;
        }
        // 查询档案详情
        EmployeeArchiveDetail detail = getByArchiveId(archive.getId());
        if (detail == null) {
            log.warn("档案详情不存在：{}", archive.getId());
            return;
        }
        // 同步数据
        if (StringUtils.hasText(detail.getRealName())) {
            employee.setName(detail.getRealName());
        }
        if (StringUtils.hasText(detail.getGender())) {
            employee.setGender(detail.getGender());
        }
        if (StringUtils.hasText(detail.getCurrentAddress())) {
            employee.setAddress(detail.getCurrentAddress());
        }
        if (StringUtils.hasText(detail.getPhotoUrl())) {
            employee.setPhotoUrl(detail.getPhotoUrl());
        }
        employeeMapper.updateById(employee);
        log.info("档案数据同步到员工表成功：{}", employeeCode);
    }

    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        // 总数
        statistics.put("total", Math.toIntExact(count()));
        // 各状态数量
        statistics.put("pending", baseMapper.countByReviewStatus(EmployeeArchiveDetail.STATUS_PENDING));
        statistics.put("approved", baseMapper.countByReviewStatus(EmployeeArchiveDetail.STATUS_APPROVED));
        statistics.put("rejected", baseMapper.countByReviewStatus(EmployeeArchiveDetail.STATUS_REJECTED));
        // 平均完整度
        Double avgScore = baseMapper.avgCompletenessScore();
        statistics.put("avgCompleteness", avgScore != null ? Math.round(avgScore * 10) / 10.0 : 0);
        return statistics;
    }
}
