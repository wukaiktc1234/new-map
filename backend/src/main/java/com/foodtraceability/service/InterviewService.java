package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.Interview;

import java.util.Map;

public interface InterviewService extends IService<Interview> {
    
    Map<String, Object> getInterviews(int current, int size, String status, String resumeId);
    
    Interview createInterview(Interview interview);
    
    void updateInterviewStatus(String id, String status);
    
    void updateInterviewResult(String id, String result, String feedback);
    
    Interview getInterviewByResumeId(String resumeId);

    Interview getInterviewById(String id);

    /**
     * 创建人事面谈
     * @param interviewId 面试记录ID
     * @param hrInterviewerId 人事面试官ID
     * @param hrInterviewerName 人事面试官姓名
     * @return 更新后的面试记录
     */
    Interview createHrInterview(String interviewId, String hrInterviewerId, String hrInterviewerName);

    /**
     * 提交人事面评
     * @param interviewId 面试记录ID
     * @param evaluation 面评内容
     * @param score 评分
     * @param educationVerification 学历核验结果
     * @param educationRemark 学历核验备注
     * @param backgroundCheck 背调结果
     * @param backgroundRemark 背调备注
     * @param status 人事面谈状态
     * @return 更新后的面试记录
     */
    Interview submitHrEvaluation(String interviewId, String evaluation, Integer score,
                                 String educationVerification, String educationRemark,
                                 String backgroundCheck, String backgroundRemark,
                                 String status);
}
