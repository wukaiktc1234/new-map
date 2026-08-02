package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.foodtraceability.dto.PositionCodeRuleDTO;
import com.foodtraceability.entity.GlobalConfig;
import com.foodtraceability.entity.PositionCodeRule;
import com.foodtraceability.mapper.GlobalConfigMapper;
import com.foodtraceability.mapper.PositionCodeRuleMapper;
import com.foodtraceability.service.PositionCodeRuleService;
import com.foodtraceability.utils.PositionCodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PositionCodeRuleServiceImpl implements PositionCodeRuleService {

    private static final Logger log = LoggerFactory.getLogger(PositionCodeRuleServiceImpl.class);

    private static final String CONFIG_KEY_FORMAT_OPTIONS = "position_code_format_options";
    private static final List<Map<String, String>> DEFAULT_FORMAT_OPTIONS = List.of(
        Map.of("label", "部门-职位-序号", "value", "{DEPT}-{CODE}-{SEQ}"),
        Map.of("label", "部门+职位+序号", "value", "{DEPT}{CODE}{SEQ}"),
        Map.of("label", "职位-序号", "value", "{CODE}-{SEQ}"),
        Map.of("label", "部门-职位", "value", "{DEPT}-{CODE}"),
        Map.of("label", "职位+序号", "value", "{CODE}{SEQ}"),
        Map.of("label", "部门+职位", "value", "{DEPT}{CODE}")
    );

    private final PositionCodeRuleMapper positionCodeRuleMapper;
    private final GlobalConfigMapper globalConfigMapper;
    private final PositionCodeGenerator positionCodeGenerator;

    public PositionCodeRuleServiceImpl(PositionCodeRuleMapper positionCodeRuleMapper, GlobalConfigMapper globalConfigMapper, PositionCodeGenerator positionCodeGenerator) {
        this.positionCodeRuleMapper = positionCodeRuleMapper;
        this.globalConfigMapper = globalConfigMapper;
        this.positionCodeGenerator = positionCodeGenerator;
    }

    @Override
    public List<PositionCodeRuleDTO> getAllRules() {
        QueryWrapper<PositionCodeRule> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("sort_order");
        List<PositionCodeRule> rules = positionCodeRuleMapper.selectList(queryWrapper);
        
        return rules.stream().map(rule -> {
            PositionCodeRuleDTO dto = new PositionCodeRuleDTO();
            dto.setId(rule.getId());
            dto.setKeyword(rule.getKeyword());
            dto.setCode(rule.getCode());
            return dto;
        }).toList();
    }

    @Override
    public PositionCodeRuleDTO addRule(PositionCodeRuleDTO rule) {
        PositionCodeRule positionCodeRule = new PositionCodeRule();
        positionCodeRule.setKeyword(rule.getKeyword());
        positionCodeRule.setCode(rule.getCode());
        positionCodeRule.setSortOrder(getNextSortOrder());
        
        positionCodeRuleMapper.insert(positionCodeRule);
        
        updatePositionCodeGeneratorRules();
        
        return convertToDTO(positionCodeRule);
    }

    @Override
    public void deleteRule(Long id) {
        positionCodeRuleMapper.deleteById(id);
        updatePositionCodeGeneratorRules();
    }

    @Override
    public void updateRules(List<PositionCodeRuleDTO> rules) {
        QueryWrapper<PositionCodeRule> queryWrapper = new QueryWrapper<>();
        positionCodeRuleMapper.delete(queryWrapper);
        
        int sortOrder = 1;
        for (PositionCodeRuleDTO rule : rules) {
            PositionCodeRule positionCodeRule = new PositionCodeRule();
            positionCodeRule.setKeyword(rule.getKeyword());
            positionCodeRule.setCode(rule.getCode());
            positionCodeRule.setSortOrder(sortOrder++);
            positionCodeRuleMapper.insert(positionCodeRule);
        }
        
        updatePositionCodeGeneratorRules();
    }

    @Override
    public String getGlobalFormatTemplate() {
        QueryWrapper<GlobalConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("config_key", "position_code_format_template");
        GlobalConfig config = globalConfigMapper.selectOne(queryWrapper);
        
        if (config != null && config.getConfigValue() != null) {
            return config.getConfigValue();
        }
        
        return "{DEPT}-{CODE}-{SEQ}";
    }

    @Override
    public void updateGlobalFormatTemplate(String formatTemplate) {
        try {
            log.info("开始更新全局职位编码格式模板: {}", formatTemplate);
            
            QueryWrapper<GlobalConfig> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("config_key", "position_code_format_template");
            
            GlobalConfig config = globalConfigMapper.selectOne(queryWrapper);
            
            if (config != null) {
                config.setConfigValue(formatTemplate);
                globalConfigMapper.updateById(config);
            } else {
                config = new GlobalConfig();
                config.setConfigKey("position_code_format_template");
                config.setConfigValue(formatTemplate);
                config.setConfigDesc("职位编码格式模板");
                globalConfigMapper.insert(config);
            }
            
            positionCodeGenerator.updateGlobalFormatTemplate(formatTemplate);
        } catch (Exception e) {
            log.error("更新全局职位编码格式模板失败: {}", e.getMessage(), e);
            throw new RuntimeException("更新全局格式模板失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void resetToDefault() {
        QueryWrapper<PositionCodeRule> queryWrapper = new QueryWrapper<>();
        positionCodeRuleMapper.delete(queryWrapper);
        
        updatePositionCodeGeneratorRules();
        
        QueryWrapper<GlobalConfig> configQueryWrapper = new QueryWrapper<>();
        configQueryWrapper.eq("config_key", "position_code_format_template");
        GlobalConfig config = globalConfigMapper.selectOne(configQueryWrapper);
        
        if (config != null) {
            config.setConfigValue("{DEPT}-{CODE}-{SEQ}");
            globalConfigMapper.updateById(config);
        }
        
        positionCodeGenerator.updateGlobalFormatTemplate("{DEPT}-{CODE}-{SEQ}");
    }

    private int getNextSortOrder() {
        QueryWrapper<PositionCodeRule> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("sort_order");
        queryWrapper.last("LIMIT 1");
        
        List<PositionCodeRule> rules = positionCodeRuleMapper.selectList(queryWrapper);
        return rules.isEmpty() ? 1 : rules.get(0).getSortOrder() + 1;
    }

    private PositionCodeRuleDTO convertToDTO(PositionCodeRule rule) {
        PositionCodeRuleDTO dto = new PositionCodeRuleDTO();
        dto.setId(rule.getId());
        dto.setKeyword(rule.getKeyword());
        dto.setCode(rule.getCode());
        return dto;
    }

    private void updatePositionCodeGeneratorRules() {
        List<PositionCodeRuleDTO> rules = getAllRules();
        Map<String, String> ruleMap = new HashMap<>();
        for (PositionCodeRuleDTO rule : rules) {
            ruleMap.put(rule.getKeyword(), rule.getCode());
        }
        positionCodeGenerator.updateCustomRules(ruleMap);
    }

    @Override
    public List<Map<String, String>> getFormatOptions() {
        try {
            QueryWrapper<GlobalConfig> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("config_key", CONFIG_KEY_FORMAT_OPTIONS);
            GlobalConfig config = globalConfigMapper.selectOne(queryWrapper);
            
            if (config != null && config.getConfigValue() != null && !config.getConfigValue().isEmpty()) {
                // 解析存储的格式选项 JSON
                return parseFormatOptions(config.getConfigValue());
            }
            
            return new ArrayList<>(DEFAULT_FORMAT_OPTIONS);
        } catch (Exception e) {
            log.error("获取格式选项列表失败: {}", e.getMessage(), e);
            return new ArrayList<>(DEFAULT_FORMAT_OPTIONS);
        }
    }

    @Override
    @Transactional
    public void saveFormatOptions(List<Map<String, String>> options) {
        try {
            log.info("保存格式选项列表，共 {} 条", options.size());
            
            // 将格式选项转换为 JSON 存储
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < options.size(); i++) {
                Map<String, String> option = options.get(i);
                sb.append("{\"label\":\"").append(option.get("label")).append("\",");
                sb.append("\"value\":\"").append(option.get("value")).append("\"}");
                if (i < options.size() - 1) {
                    sb.append(",");
                }
            }
            sb.append("]");
            String jsonValue = sb.toString();
            
            QueryWrapper<GlobalConfig> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("config_key", CONFIG_KEY_FORMAT_OPTIONS);
            GlobalConfig config = globalConfigMapper.selectOne(queryWrapper);
            
            if (config != null) {
                config.setConfigValue(jsonValue);
                globalConfigMapper.updateById(config);
            } else {
                config = new GlobalConfig();
                config.setConfigKey(CONFIG_KEY_FORMAT_OPTIONS);
                config.setConfigValue(jsonValue);
                config.setConfigDesc("职位编码格式选项列表");
                globalConfigMapper.insert(config);
            }
            
            log.info("格式选项列表保存成功");
        } catch (Exception e) {
            log.error("保存格式选项列表失败: {}", e.getMessage(), e);
            throw new RuntimeException("保存格式选项列表失败: " + e.getMessage(), e);
        }
    }

    private List<Map<String, String>> parseFormatOptions(String json) {
        try {
            List<Map<String, String>> options = new ArrayList<>();
            json = json.trim();
            if (!json.startsWith("[")) {
                return new ArrayList<>(DEFAULT_FORMAT_OPTIONS);
            }
            
            // 简单解析 JSON
            json = json.substring(1, json.length() - 1);
            String[] items = json.split("\\},\\s*\\{");
            
            for (String item : items) {
                item = item.replace("{", "").replace("}", "");
                String[] pairs = item.split(",");
                Map<String, String> option = new HashMap<>();
                for (String pair : pairs) {
                    String[] kv = pair.split(":");
                    if (kv.length == 2) {
                        String key = kv[0].trim().replace("\"", "");
                        String value = kv[1].trim().replace("\"", "");
                        option.put(key, value);
                    }
                }
                if (option.containsKey("label") && option.containsKey("value")) {
                    options.add(option);
                }
            }
            
            if (options.isEmpty()) {
                return new ArrayList<>(DEFAULT_FORMAT_OPTIONS);
            }
            
            return options;
        } catch (Exception e) {
            log.error("解析格式选项 JSON 失败: {}", json, e);
            return new ArrayList<>(DEFAULT_FORMAT_OPTIONS);
        }
    }
}
