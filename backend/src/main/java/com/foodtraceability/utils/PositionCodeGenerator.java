package com.foodtraceability.utils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.foodtraceability.entity.Department;
import com.foodtraceability.entity.Position;
import com.foodtraceability.entity.PositionCodeRule;
import com.foodtraceability.mapper.DepartmentMapper;
import com.foodtraceability.mapper.PositionCodeRuleMapper;
import com.foodtraceability.mapper.PositionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PositionCodeGenerator implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(PositionCodeGenerator.class);

    private final PositionMapper positionMapper;

    private final DepartmentMapper departmentMapper;

    private final PositionCodeRuleMapper positionCodeRuleMapper;

    /**
     * 构造函数注入（禁止 @Autowired 字段注入）
     * @param positionMapper 职位Mapper，可选依赖
     * @param departmentMapper 部门Mapper，可选依赖
     * @param positionCodeRuleMapper 职位编码规则Mapper，可选依赖
     */
    public PositionCodeGenerator(@Nullable PositionMapper positionMapper, @Nullable DepartmentMapper departmentMapper, @Nullable PositionCodeRuleMapper positionCodeRuleMapper) {
        this.positionMapper = positionMapper;
        this.departmentMapper = departmentMapper;
        this.positionCodeRuleMapper = positionCodeRuleMapper;
    }

    /**
     * Spring Boot 启动后执行，加载自定义编码规则
     */
    @Override
    public void run(String... args) {
        try {
            loadCustomRulesFromDatabase();
        } catch (Exception e) {
            logger.error("初始化职位编码生成器失败", e);
        }
    }

    private static final Map<String, String> DEFAULT_POSITION_KEYWORD_MAP = new HashMap<>();

    static {
        // 优先添加完全匹配的关键职位
        DEFAULT_POSITION_KEYWORD_MAP.put("总经理", "GM");
        DEFAULT_POSITION_KEYWORD_MAP.put("副总经理", "DGM");
        DEFAULT_POSITION_KEYWORD_MAP.put("总监", "DIR");
        DEFAULT_POSITION_KEYWORD_MAP.put("经理", "MGR");
        DEFAULT_POSITION_KEYWORD_MAP.put("主管", "SUP");
        DEFAULT_POSITION_KEYWORD_MAP.put("专员", "SPE");
        DEFAULT_POSITION_KEYWORD_MAP.put("助理", "AST");
        DEFAULT_POSITION_KEYWORD_MAP.put("工程师", "ENG");
        DEFAULT_POSITION_KEYWORD_MAP.put("开发", "DEV");
        DEFAULT_POSITION_KEYWORD_MAP.put("设计师", "DES");
        DEFAULT_POSITION_KEYWORD_MAP.put("分析师", "ANA");
        DEFAULT_POSITION_KEYWORD_MAP.put("顾问", "CONS");
        DEFAULT_POSITION_KEYWORD_MAP.put("文员", "CLERK");
        DEFAULT_POSITION_KEYWORD_MAP.put("秘书", "SEC");
        DEFAULT_POSITION_KEYWORD_MAP.put("会计", "ACC");
        DEFAULT_POSITION_KEYWORD_MAP.put("出纳", "CASH");
        DEFAULT_POSITION_KEYWORD_MAP.put("审计", "AUD");
        DEFAULT_POSITION_KEYWORD_MAP.put("销售", "SALES");
        DEFAULT_POSITION_KEYWORD_MAP.put("市场", "MKT");
        DEFAULT_POSITION_KEYWORD_MAP.put("客服", "CS");
        DEFAULT_POSITION_KEYWORD_MAP.put("运营", "OPS");
        DEFAULT_POSITION_KEYWORD_MAP.put("产品", "PROD");
        DEFAULT_POSITION_KEYWORD_MAP.put("测试", "QA");
        DEFAULT_POSITION_KEYWORD_MAP.put("运维", "OPS");
        DEFAULT_POSITION_KEYWORD_MAP.put("架构师", "ARCH");
        DEFAULT_POSITION_KEYWORD_MAP.put("专家", "EXP");
        DEFAULT_POSITION_KEYWORD_MAP.put("组长", "TL");
        DEFAULT_POSITION_KEYWORD_MAP.put("领班", "TL");
        DEFAULT_POSITION_KEYWORD_MAP.put("店长", "GM");
        DEFAULT_POSITION_KEYWORD_MAP.put("副店长", "DGM");
        DEFAULT_POSITION_KEYWORD_MAP.put("厨师", "CHEF");
        DEFAULT_POSITION_KEYWORD_MAP.put("服务员", "WAIT");
        DEFAULT_POSITION_KEYWORD_MAP.put("收银员", "CASHIER");
        DEFAULT_POSITION_KEYWORD_MAP.put("采购员", "PUR");
        DEFAULT_POSITION_KEYWORD_MAP.put("仓管员", "WH");
        DEFAULT_POSITION_KEYWORD_MAP.put("配送员", "DEL");
        DEFAULT_POSITION_KEYWORD_MAP.put("司机", "DRV");
        DEFAULT_POSITION_KEYWORD_MAP.put("保安", "SEC");
        DEFAULT_POSITION_KEYWORD_MAP.put("保洁", "CLN");
        DEFAULT_POSITION_KEYWORD_MAP.put("培训师", "TRAINER");
        DEFAULT_POSITION_KEYWORD_MAP.put("总经理助理", "GMA");
        DEFAULT_POSITION_KEYWORD_MAP.put("监事", "SUPER");
    }

    private Map<String, String> customRulesMap = new HashMap<>();
    private String globalFormatTemplate = "{DEPT}-{CODE}-{SEQ}";

    public void setCustomRules(Map<String, String> rules) {
        if (rules != null && !rules.isEmpty()) {
            customRulesMap.clear();
            customRulesMap.putAll(rules);
            logger.info("设置职位编码规则，共 {} 条自定义规则", rules.size());
        }
    }

    public void setCustomFormatTemplates(Map<String, String> formatTemplates) {
        if (formatTemplates != null) {
            logger.info("设置职位编码格式模板，共 {} 条自定义模板", formatTemplates.size());
        } else {
            logger.info("设置职位编码格式模板，无自定义模板");
        }
    }

    public void loadCustomRulesFromDatabase() {
        try {
            if (positionCodeRuleMapper == null) {
                logger.warn("positionCodeRuleMapper 未注入，跳过从数据库加载职位编码规则");
                return;
            }
            
            try {
                List<PositionCodeRule> rules = positionCodeRuleMapper.selectList(null);
                if (!rules.isEmpty()) {
                    Map<String, String> ruleMap = new HashMap<>();
                    Map<String, String> formatTemplateMap = new HashMap<>();
                    for (PositionCodeRule rule : rules) {
                        ruleMap.put(rule.getKeyword(), rule.getCode());
                        if (rule.getFormatTemplate() != null) {
                            formatTemplateMap.put(rule.getKeyword(), rule.getFormatTemplate());
                        }
                    }
                    setCustomRules(ruleMap);
                    setCustomFormatTemplates(formatTemplateMap);
                    logger.info("从数据库加载职位编码规则，共 {} 条", ruleMap.size());
                }
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("Unknown column 'format_template'")) {
                    logger.warn("position_code_rules表缺少format_template字段，跳过格式模板加载");
                    try {
                        List<PositionCodeRule> rules = positionCodeRuleMapper.selectList(null);
                        if (!rules.isEmpty()) {
                            Map<String, String> ruleMap = new HashMap<>();
                            for (PositionCodeRule rule : rules) {
                                ruleMap.put(rule.getKeyword(), rule.getCode());
                            }
                            setCustomRules(ruleMap);
                            logger.info("从数据库加载职位编码规则（不包含格式模板），共 {} 条", ruleMap.size());
                        }
                    } catch (Exception ex) {
                        logger.error("加载职位编码规则失败", ex);
                    }
                } else {
                    logger.error("加载职位编码规则失败", e);
                }
            }
        } catch (Exception e) {
            logger.error("加载职位编码规则失败", e);
        }
    }

    public String generatePositionCode(String departmentId, String positionName) {
        try {
            if (positionName == null || positionName.trim().isEmpty()) {
                throw new IllegalArgumentException("职位名称不能为空");
            }

            // 获取职位前缀
            String positionPrefix = generatePositionPrefix(positionName);
            
            // 获取部门编码
            String departmentCode = getDepartmentCode(departmentId);
            
            // 生成序号
            int nextSeq = generateNextSequence(departmentCode, positionPrefix);
            
            // 使用全局格式模板生成编码
            String positionCode = generateCodeFromTemplate(departmentCode, positionPrefix, nextSeq);
            
            // 检查编码是否已存在，如果存在则递增序号直到唯一
            if (positionMapper != null) {
                int maxAttempts = 1000; // 防止无限循环
                int attemptSeq = nextSeq;
                while (isCodeExists(positionCode) && attemptSeq < maxAttempts) {
                    attemptSeq++;
                    positionCode = generateCodeFromTemplate(departmentCode, positionPrefix, attemptSeq);
                }
                if (attemptSeq >= maxAttempts) {
                    throw new RuntimeException("无法生成唯一的职位编码，已达到最大尝试次数（" + maxAttempts + "），请检查数据库中是否存在过多重复编码");
                }
            }
            
            logger.info("生成职位编码: {}, 部门ID: {}, 职位名称: {}, 部门编码: {}, 职位前缀: {}, 序号: {}, 格式模板: {}", 
                positionCode, departmentId, positionName, departmentCode, positionPrefix, nextSeq, globalFormatTemplate);
            
            return positionCode;
            
        } catch (Exception e) {
            logger.error("生成职位编码失败，部门ID: {}, 职位名称: {}", departmentId, positionName, e);
            throw new RuntimeException("生成职位编码失败: " + e.getMessage());
        }
    }
    
    private boolean isCodeExists(String positionCode) {
        try {
            QueryWrapper<Position> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("position_code", positionCode);
            queryWrapper.eq("deleted", 0);
            Long count = positionMapper.selectCount(queryWrapper);
            return count != null && count > 0;
        } catch (Exception e) {
            logger.warn("检查编码是否存在时出错: {}", positionCode, e);
            return false;
        }
    }
    
    private String generateCodeFromTemplate(String departmentCode, String positionPrefix, int seq) {
        String formatTemplate = this.globalFormatTemplate;
        
        // 替换占位符
        String code = formatTemplate
            .replace("{DEPT}", departmentCode)
            .replace("{CODE}", positionPrefix)
            .replace("{SEQ}", String.format("%03d", seq));
        
        return code;
    }
    
    private int generateNextSequence(String departmentCode, String positionPrefix) {
        int nextSeq = 1;
        
        try {
            if (positionMapper != null) {
                // 构建查询条件，根据格式模板生成匹配模式
                String formatTemplate = this.globalFormatTemplate;
                
                // 生成用于查询的前缀模式
                String queryPattern = formatTemplate
                    .replace("{DEPT}", departmentCode)
                    .replace("{CODE}", positionPrefix)
                    .replace("{SEQ}", "");
                
                // 清理查询模式，去除可能的尾随分隔符
                queryPattern = queryPattern.replaceAll("-+$|", "");
                
                if (!queryPattern.isEmpty()) {
                    QueryWrapper<Position> queryWrapper = new QueryWrapper<>();
                    queryWrapper.likeRight("position_code", queryPattern);
                    queryWrapper.orderByDesc("position_code");
                    queryWrapper.last("LIMIT 1");
                    
                    List<Position> positions = positionMapper.selectList(queryWrapper);
                    
                    if (!positions.isEmpty() && positions.get(0) != null) {
                        String lastCode = positions.get(0).getPositionCode();
                        if (lastCode != null) {
                            // 提取序号
                            int lastSeq = extractSequence(lastCode, formatTemplate, departmentCode, positionPrefix);
                            nextSeq = lastSeq + 1;
                            logger.debug("解析到上一个序号: {}, 下一个序号: {}", lastSeq, nextSeq);
                        }
                    }
                }
            } else {
                logger.warn("PositionMapper 未注入，使用默认序号 1");
            }
            
            if (nextSeq > 9999) {
                throw new RuntimeException("该职位类型编码已达上限（9999个），无法生成新编码");
            }
            
        } catch (Exception e) {
            logger.error("生成序号时出错，使用默认序号 1", e);
        }
        
        return nextSeq;
    }
    
    private int extractSequence(String lastCode, String formatTemplate, String departmentCode, String positionPrefix) {
        try {
            // 构建不包含序号的模板部分
            String templateWithoutSeq = formatTemplate.replace("{SEQ}", "");
            
            // 替换模板中的部门和职位编码
            String prefixPart = templateWithoutSeq
                .replace("{DEPT}", departmentCode)
                .replace("{CODE}", positionPrefix);
            
            // 从最后一个编码中提取序号部分
            if (lastCode.startsWith(prefixPart)) {
                String seqPart = lastCode.substring(prefixPart.length());
                // 清理可能的非数字字符
                seqPart = seqPart.replaceAll("[^0-9]", "");
                if (!seqPart.isEmpty()) {
                    return Integer.parseInt(seqPart);
                }
            }
            
            // 如果无法提取，尝试使用正则表达式
            Pattern pattern = Pattern.compile("\\d{3}$");
            Matcher matcher = pattern.matcher(lastCode);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group());
            }
            
        } catch (Exception e) {
            logger.warn("提取序号失败: {}", lastCode, e);
        }
        
        return 0; // 返回0，这样nextSeq会变成1
    }

    public String generatePositionPrefix(String positionName) {
        if (positionName == null || positionName.trim().isEmpty()) {
            return "POS";
        }
        
        // 清理职位名称，去除前后空格
        String cleanPositionName = positionName.trim();
        
        // 检查是否为副职
        boolean isDeputy = cleanPositionName.startsWith("副") || cleanPositionName.contains("副职");
        
        // 处理副职情况，提取正职名称
        String principalPositionName = cleanPositionName;
        if (isDeputy) {
            principalPositionName = cleanPositionName.replaceFirst("^副", "").replace("副职", "");
        }
        
        // 优先检查完全匹配
        if (!customRulesMap.isEmpty()) {
            // 先检查完全匹配（包括副职）
            if (customRulesMap.containsKey(cleanPositionName)) {
                return customRulesMap.get(cleanPositionName);
            }
            
            // 检查正职匹配
            if (customRulesMap.containsKey(principalPositionName)) {
                String principalCode = customRulesMap.get(principalPositionName);
                // 为副职添加前缀或后缀
                if (isDeputy && !principalCode.startsWith("D")) {
                    return "D" + principalCode; // 例如：MGR → DMGR
                }
                return principalCode;
            }
            
            // 如果没有完全匹配，再按长度排序进行包含匹配
            List<Map.Entry<String, String>> sortedEntries = new ArrayList<>(customRulesMap.entrySet());
            sortedEntries.sort((e1, e2) -> e2.getKey().length() - e1.getKey().length());
            
            for (Map.Entry<String, String> entry : sortedEntries) {
                String keyword = entry.getKey();
                String code = entry.getValue();
                if (cleanPositionName.contains(keyword)) {
                    return code;
                }
                if (principalPositionName.contains(keyword)) {
                    if (isDeputy && !code.startsWith("D")) {
                        return "D" + code;
                    }
                    return code;
                }
            }
        } else {
            // 先检查完全匹配（包括副职）
            if (DEFAULT_POSITION_KEYWORD_MAP.containsKey(cleanPositionName)) {
                return DEFAULT_POSITION_KEYWORD_MAP.get(cleanPositionName);
            }
            
            // 检查正职匹配
            if (DEFAULT_POSITION_KEYWORD_MAP.containsKey(principalPositionName)) {
                String principalCode = DEFAULT_POSITION_KEYWORD_MAP.get(principalPositionName);
                // 为副职添加前缀
                if (isDeputy && !principalCode.startsWith("D")) {
                    return "D" + principalCode; // 例如：GM → DGM, MGR → DMGR
                }
                return principalCode;
            }
            
            // 如果没有完全匹配，再按长度排序进行包含匹配
            List<Map.Entry<String, String>> sortedEntries = new ArrayList<>(DEFAULT_POSITION_KEYWORD_MAP.entrySet());
            sortedEntries.sort((e1, e2) -> e2.getKey().length() - e1.getKey().length());
            
            for (Map.Entry<String, String> entry : sortedEntries) {
                String keyword = entry.getKey();
                String code = entry.getValue();
                if (cleanPositionName.contains(keyword)) {
                    return code;
                }
                if (principalPositionName.contains(keyword)) {
                    if (isDeputy && !code.startsWith("D")) {
                        return "D" + code;
                    }
                    return code;
                }
            }
        }
        return "POS";
    }

    public String getDepartmentCode(String departmentId) {
        if (departmentId == null || departmentId.isEmpty()) {
            return "DEPT";
        }

        try {
            if (departmentMapper != null) {
                Long deptId = Long.parseLong(departmentId);
                Department department = departmentMapper.selectById(deptId);
                if (department != null && department.getDepartmentCode() != null) {
                    return department.getDepartmentCode();
                }
            } else {
                logger.warn("DepartmentMapper 未注入，返回默认部门代码 DEPT");
            }
        } catch (NumberFormatException e) {
            logger.warn("解析部门ID失败: {}", departmentId);
        }

        return "DEPT";
    }

    public void updateCustomRules(Map<String, String> customRules) {
        if (customRules != null && !customRules.isEmpty()) {
            customRulesMap.clear();
            customRulesMap.putAll(customRules);
            logger.info("更新职位编码规则，共 {} 条自定义规则", customRules.size());
        }
    }

    public void updateGlobalFormatTemplate(String formatTemplate) {
        if (formatTemplate != null && !formatTemplate.isEmpty()) {
            // 验证格式模板
            if (validateFormatTemplate(formatTemplate)) {
                this.globalFormatTemplate = formatTemplate;
                logger.info("更新全局职位编码格式模板: {}", formatTemplate);
            } else {
                logger.warn("格式模板验证失败，使用默认模板: {}", this.globalFormatTemplate);
            }
        }
    }
    
    public boolean validateFormatTemplate(String formatTemplate) {
        if (formatTemplate == null || formatTemplate.isEmpty()) {
            return false;
        }
        
        // 检查是否包含至少一个占位符
        boolean hasPlaceholders = formatTemplate.contains("{DEPT}") || 
                                 formatTemplate.contains("{CODE}") || 
                                 formatTemplate.contains("{SEQ}");
        
        if (!hasPlaceholders) {
            logger.warn("格式模板必须包含至少一个占位符: {}", formatTemplate);
            return false;
        }
        
        // 检查包含序号的模板是否也包含其他占位符
        if (formatTemplate.contains("{SEQ}") && 
            !formatTemplate.contains("{DEPT}") && 
            !formatTemplate.contains("{CODE}")) {
            logger.warn("包含序号的格式模板必须包含部门或职位编码: {}", formatTemplate);
            return false;
        }
        
        return true;
    }
}
