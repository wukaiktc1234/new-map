package com.foodtraceability.dataservice.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.dataservice.RecruitmentApprovalDataService;
import com.foodtraceability.dto.store.operation.vo.RecruitmentApprovalVO;
import com.foodtraceability.entity.RecruitmentApproval;
import com.foodtraceability.mapper.RecruitmentApprovalMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 招聘审批数据服务实现类
 * 实现二级缓存：L1 Caffeine(本地) + L2 Redis
 * 支持按岗位、审批人等维度查询
 * 缓存键格式: recruitment_approval:basic:{approvalId}
 */
@Service
public class RecruitmentApprovalDataServiceImpl implements RecruitmentApprovalDataService {

    private static final Logger log = LoggerFactory.getLogger(RecruitmentApprovalDataServiceImpl.class);

    /** 缓存名称 */
    private static final String CACHE_NAME = "recruitmentApproval";

    private final RecruitmentApprovalMapper recruitmentApprovalMapper;

    /**
     * 构造函数注入
     *
     * @param recruitmentApprovalMapper 招聘审批Mapper
     */
    public RecruitmentApprovalDataServiceImpl(RecruitmentApprovalMapper recruitmentApprovalMapper) {
        this.recruitmentApprovalMapper = recruitmentApprovalMapper;
    }

    @Override
    @Cacheable(value = CACHE_NAME, key = "#approvalIds", unless = "#result == null || #result.isEmpty()")
    public Map<String, RecruitmentApprovalVO> batchGetApprovalBasicInfo(List<String> approvalIds) {
        if (approvalIds == null || approvalIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<RecruitmentApproval> approvals = recruitmentApprovalMapper.selectBatchIds(approvalIds);
        return approvals.stream()
                .filter(a -> a != null)
                .collect(Collectors.toMap(
                        RecruitmentApproval::getApprovalId,
                        this::convertToVO,
                        (v1, v2) -> v1,
                        LinkedHashMap::new
                ));
    }

    @Override
    @Cacheable(value = CACHE_NAME, key = "'basic:' + #approvalId", unless = "#result == null")
    public RecruitmentApprovalVO getApprovalBasicInfo(String approvalId) {
        if (approvalId == null || approvalId.isEmpty()) {
            return null;
        }

        RecruitmentApproval approval = recruitmentApprovalMapper.selectById(approvalId);
        return convertToVO(approval);
    }

    @Override
    public List<RecruitmentApprovalVO> getApprovalsByPostId(String postId) {
        if (postId == null || postId.isEmpty()) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<RecruitmentApproval> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RecruitmentApproval::getPostId, postId)
               .orderByDesc(RecruitmentApproval::getCreateTime);

        List<RecruitmentApproval> approvals = recruitmentApprovalMapper.selectList(wrapper);
        return approvals.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<RecruitmentApprovalVO> getPendingApprovalsForUser(String approverId, Integer currentLevel) {
        if (approverId == null || approverId.isEmpty()) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<RecruitmentApproval> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RecruitmentApproval::getApproverId, approverId)
               .eq(RecruitmentApproval::getApprovalStatus, "pending")
               .eq(RecruitmentApproval::getCurrentLevel, currentLevel != null ? currentLevel.shortValue() : (short) 1)
               .orderByAsc(RecruitmentApproval::getCreateTime);

        List<RecruitmentApproval> approvals = recruitmentApprovalMapper.selectList(wrapper);
        return approvals.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void clearApprovalCache(String approvalId) {
        log.debug("清除招聘审批缓存: approvalId={}", approvalId);
    }

    /**
     * 将招聘审批实体转换为视图对象
     *
     * @param approval 招聘审批实体
     * @return 视图对象
     */
    private RecruitmentApprovalVO convertToVO(RecruitmentApproval approval) {
        if (approval == null) {
            return null;
        }

        RecruitmentApprovalVO vo = new RecruitmentApprovalVO();
        vo.setApprovalId(approval.getApprovalId());
        vo.setPostId(approval.getPostId());
        vo.setApplicantName(approval.getApplicantName());
        vo.setPhone(maskPhone(approval.getPhone())); // 手机号脱敏
        vo.setProposedSalary(convertFenToYuan(approval.getProposedSalary()));
        vo.setInterviewScore(approval.getInterviewScore());
        vo.setInterviewerId(approval.getInterviewerId());
        vo.setCurrentLevel(approval.getCurrentLevel());
        vo.setApproverId(approval.getApproverId());
        vo.setApprovalStatus(approval.getApprovalStatus());
        vo.setRejectionReason(approval.getRejectionReason());
        vo.setApprovedAt(approval.getApprovedAt());
        vo.setFinalDecision(approval.getFinalDecision());
        vo.setHiredAt(approval.getHiredAt());
        vo.setEmployeeId(approval.getEmployeeId());
        vo.setCreateTime(approval.getCreateTime());

        // 设置显示名称
        vo.setCurrentLevelName(getLevelName(approval.getCurrentLevel()));
        vo.setApprovalStatusName(getApprovalStatusName(approval.getApprovalStatus()));

        return vo;
    }

    /** 分转元（薪资单位为分/月） */
    private String convertFenToYuan(Long fen) {
        if (fen == null) {
            return null;
        }
        return String.valueOf(fen / 100.0);
    }

    /** 手机号脱敏处理：138****8000 */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /** 获取审批层级名称 */
    private String getLevelName(Short level) {
        if (level == null) return "未知";
        switch (level) {
            case 1: return "初审";
            case 2: return "复审";
            case 3: return "终审";
            default: return "未知";
        }
    }

    /** 获取审批状态名称 */
    private String getApprovalStatusName(String status) {
        if (status == null) return "未知";
        switch (status) {
            case "pending": return "待审批";
            case "approved_by_regional": return "区域经理已通过";
            case "final_approved": return "终审通过";
            case "rejected": return "已驳回";
            case "withdrawn": return "已撤回";
            case "hired": return "已录用";
            default: return status;
        }
    }
}
