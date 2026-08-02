package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.entity.Interview;
import com.foodtraceability.mapper.InterviewMapper;
import com.foodtraceability.service.InterviewService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class InterviewServiceImpl extends ServiceImpl<InterviewMapper, Interview> implements InterviewService {
    
    @Override
    public Map<String, Object> getInterviews(int current, int size, String status, String resumeId) {
        Page<Interview> page = new Page<>(current, size);
        LambdaQueryWrapper<Interview> queryWrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(Interview::getStatus, status);
        }
        if (StringUtils.hasText(resumeId)) {
            queryWrapper.eq(Interview::getResumeId, resumeId);
        }
        
        queryWrapper.orderByDesc(Interview::getInterviewDate);
        IPage<Interview> result = this.page(page, queryWrapper);
        
        Map<String, Object> response = new HashMap<>();
        response.put("records", result.getRecords());
        response.put("total", result.getTotal());
        response.put("current", result.getCurrent());
        response.put("size", result.getSize());
        response.put("pages", result.getPages());
        
        return response;
    }
    
    @Override
    public Interview createInterview(Interview interview) {
        interview.setInterviewCode(generateInterviewCode());
        interview.setStatus("scheduled");
        interview.setResult("pending");
        if (!StringUtils.hasText(interview.getInterviewRound())) {
            interview.setInterviewRound("first");
        }
        interview.setCreatedAt(LocalDateTime.now());
        interview.setUpdatedAt(LocalDateTime.now());
        this.save(interview);
        return interview;
    }
    
    @Override
    public void updateInterviewStatus(String id, String status) {
        Interview interview = this.getById(id);
        if (interview != null) {
            interview.setStatus(status);
            interview.setUpdatedAt(LocalDateTime.now());
            this.updateById(interview);
        }
    }
    
    @Override
    public void updateInterviewResult(String id, String result, String feedback) {
        Interview interview = this.getById(id);
        if (interview != null) {
            interview.setResult(result);
            interview.setFeedback(feedback);
            interview.setStatus("completed");
            interview.setUpdatedAt(LocalDateTime.now());
            this.updateById(interview);
        }
    }
    
    @Override
    public Interview getInterviewByResumeId(String resumeId) {
        LambdaQueryWrapper<Interview> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Interview::getResumeId, resumeId);
        queryWrapper.orderByDesc(Interview::getInterviewDate);
        queryWrapper.last("LIMIT 1");
        return this.getOne(queryWrapper);
    }

    @Override
    public Interview getInterviewById(String id) {
        return this.getById(id);
    }

    @Override
    public Interview createHrInterview(String interviewId, String hrInterviewerId, String hrInterviewerName) {
        Interview interview = this.getById(interviewId);
        if (interview == null) {
            throw new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND, "面试记录不存在");
        }

        // 流程阶段校验：区分两种招聘流程
        // 流程I（门店发起）：面试记录中存在 storeInterviewerId，需门店初面通过后才能进行人事面谈
        // 流程II（HR下发）：面试记录中无 storeInterviewerId，不需要门店初面，直接进行人事面谈
        if (StringUtils.hasText(interview.getStoreInterviewerId())) {
            // 流程I：检查门店面评是否已通过（storeEvaluationScore存在且result='pass'）
            if (interview.getStoreEvaluationScore() == null
                    || !"pass".equals(interview.getResult())) {
                throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                        "门店初面未通过，无法进行人事面谈");
            }
        }

        interview.setHrInterviewerId(hrInterviewerId);
        interview.setHrInterviewerName(hrInterviewerName);
        interview.setHrInterviewStatus("scheduled");
        interview.setUpdatedAt(LocalDateTime.now());
        this.updateById(interview);
        return interview;
    }

    @Override
    public Interview submitHrEvaluation(String interviewId, String evaluation, Integer score,
                                        String educationVerification, String educationRemark,
                                        String backgroundCheck, String backgroundRemark,
                                        String status) {
        Interview interview = this.getById(interviewId);
        if (interview != null) {
            interview.setHrEvaluation(evaluation);
            interview.setHrEvaluationScore(score);
            interview.setHrEvaluationTime(LocalDateTime.now());
            interview.setEducationVerification(educationVerification);
            interview.setEducationVerificationRemark(educationRemark);
            interview.setBackgroundCheckResult(backgroundCheck);
            interview.setBackgroundCheckRemark(backgroundRemark);
            interview.setHrInterviewStatus(status);
            interview.setUpdatedAt(LocalDateTime.now());
            this.updateById(interview);
        }
        return interview;
    }

    private String generateInterviewCode() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "INT" + timestamp;
    }
}
