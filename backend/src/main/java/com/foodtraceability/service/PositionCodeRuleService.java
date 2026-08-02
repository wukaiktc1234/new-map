package com.foodtraceability.service;

import com.foodtraceability.dto.PositionCodeRuleDTO;
import com.foodtraceability.entity.PositionCodeRule;
import java.util.List;
import java.util.Map;

public interface PositionCodeRuleService {
    
    List<PositionCodeRuleDTO> getAllRules();
    
    PositionCodeRuleDTO addRule(PositionCodeRuleDTO rule);
    
    void deleteRule(Long id);
    
    void updateRules(List<PositionCodeRuleDTO> rules);
    
    String getGlobalFormatTemplate();
    
    void updateGlobalFormatTemplate(String formatTemplate);
    
    void resetToDefault();
    
    List<Map<String, String>> getFormatOptions();
    
    void saveFormatOptions(List<Map<String, String>> options);
}
