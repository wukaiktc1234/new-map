package com.foodtraceability.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 部门编码生成工具类
 */
public class DepartmentCodeGenerator {

    private static final Logger logger = LoggerFactory.getLogger(DepartmentCodeGenerator.class);

    /**
     * 根据部门名称生成部门编码
     * 格式：使用部门名称对应的英文缩写
     *
     * @param departmentName 部门名称
     * @return 部门编码
     */
    public static String generateCode(String departmentName) {
        if (departmentName == null || departmentName.trim().isEmpty()) {
            throw new IllegalArgumentException("部门名称不能为空");
        }

        String code = generatePrefix(departmentName);

        logger.info("生成部门编码: 部门名称={}, 编码={}", departmentName, code);
        return code;
    }

    /**
     * 生成编码前缀
     * 使用部门名称对应的英文缩写
     *
     * @param departmentName 部门名称
     * @return 前缀（英文缩写）
     */
    private static String generatePrefix(String departmentName) {
        Map<String, String> keywordMap = new HashMap<>();
        keywordMap.put("人事", "HR");
        keywordMap.put("人力", "HR");
        keywordMap.put("财务", "FIN");
        keywordMap.put("会计", "FIN");
        keywordMap.put("出纳", "FIN");
        keywordMap.put("技术", "TECH");
        keywordMap.put("研发", "R&D");
        keywordMap.put("开发", "DEV");
        keywordMap.put("市场", "MKT");
        keywordMap.put("销售", "SALES");
        keywordMap.put("生产", "PROD");
        keywordMap.put("制造", "PROD");
        keywordMap.put("质量", "QA");
        keywordMap.put("质检", "QA");
        keywordMap.put("采购", "PUR");
        keywordMap.put("行政", "ADMIN");
        keywordMap.put("后勤", "LOG");
        keywordMap.put("信息", "IT");
        keywordMap.put("客服", "CS");
        keywordMap.put("服务", "CS");
        keywordMap.put("总经理", "GM");
        keywordMap.put("运营", "OPS");
        keywordMap.put("法务", "LEGAL");
        keywordMap.put("审计", "AUDIT");
        keywordMap.put("战略", "STRAT");
        keywordMap.put("品牌", "BRAND");
        keywordMap.put("设计", "DESIGN");
        keywordMap.put("产品", "PRODUCT");
        keywordMap.put("测试", "QA");
        keywordMap.put("运维", "OPS");
        keywordMap.put("安全", "SEC");
        keywordMap.put("培训", "TRAIN");
        keywordMap.put("招聘", "RECRUIT");
        keywordMap.put("薪酬", "COMP");
        keywordMap.put("绩效", "PERF");
        keywordMap.put("员工", "ER");
        keywordMap.put("企业", "CULTURE");
        keywordMap.put("文化", "CULTURE");
        keywordMap.put("公共", "PR");
        keywordMap.put("关系", "PR");
        keywordMap.put("宣传", "PUB");
        keywordMap.put("投资", "INV");
        keywordMap.put("风控", "RISK");
        keywordMap.put("合规", "COMPLIANCE");
        keywordMap.put("董事会", "BOARD");
        keywordMap.put("监事会", "SUPER");
        keywordMap.put("工会", "UNION");
        keywordMap.put("店", "STORE");
        keywordMap.put("门店", "STORE");
        keywordMap.put("分店", "STORE");
        keywordMap.put("连锁", "STORE");
        keywordMap.put("零售", "STORE");

        for (Map.Entry<String, String> entry : keywordMap.entrySet()) {
            String keyword = entry.getKey();
            String code = entry.getValue();
            if (departmentName.contains(keyword)) {
                return code;
            }
        }

        return "DEPT";
    }
}
