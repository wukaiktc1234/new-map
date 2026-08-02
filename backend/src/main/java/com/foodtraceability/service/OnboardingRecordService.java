package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.OnboardingRecord;

import java.util.Map;

public interface OnboardingRecordService extends IService<OnboardingRecord> {
    
    Map<String, Object> getOnboardingRecords(int current, int size, String status, String interviewId);
    
    OnboardingRecord createOnboardingRecord(OnboardingRecord record);
    
    void updateOnboardingStatus(String id, String status);
    
    String generateRegistrationCode(String onboardingId);
    
    OnboardingRecord getOnboardingByInterviewId(String interviewId);
    
    void completeOnboarding(String id);
}
