package com.foodtraceability.utils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.foodtraceability.entity.Food;
import com.foodtraceability.mapper.FoodMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

/**
 * 菜品编码生成器
 * 生成极简数字编码格式: D00001, D00002, ... D99999
 * 支持最多99999个菜品
 */
@Component
public class FoodCodeGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(FoodCodeGenerator.class);
    

    public FoodCodeGenerator(FoodMapper foodMapper) {
        this.foodMapper = foodMapper;
    }

    private final FoodMapper foodMapper;
    
    private static final String CODE_PREFIX = "D";
    private static final int MAX_NUMBER = 99999;
    
    /**
     * 生成下一个菜品编码
     * @return 新的菜品编码
     */
    public synchronized String generateNextCode() {
        logger.info("开始生成菜品编码");
        
        int nextNumber = findNextAvailableNumber();
        
        String foodCode = String.format("%s%05d", CODE_PREFIX, nextNumber);
        logger.info("生成新的菜品编码: {}", foodCode);
        return foodCode;
    }
    
    /**
     * 查找下一个可用的编码数字
     * @return 下一个可用的数字
     */
    private int findNextAvailableNumber() {
        QueryWrapper<Food> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("food_code");
        queryWrapper.likeRight("food_code", CODE_PREFIX);
        queryWrapper.orderByDesc("food_code");
        queryWrapper.last("LIMIT 1");
        
        List<Food> foods = foodMapper.selectList(queryWrapper);
        
        if (foods == null || foods.isEmpty()) {
            logger.info("没有找到现有菜品编码，从1开始");
            return 1;
        }
        
        Food lastFood = foods.get(0);
        String lastCode = lastFood.getFoodCode();
        
        if (lastCode == null || !lastCode.startsWith(CODE_PREFIX)) {
            logger.info("最后一个编码格式不正确: {}, 从1开始", lastCode);
            return 1;
        }
        
        try {
            String numberPart = lastCode.substring(CODE_PREFIX.length());
            int lastNumber = Integer.parseInt(numberPart);
            
            if (lastNumber >= MAX_NUMBER) {
                throw new RuntimeException("菜品编码已达最大值" + CODE_PREFIX + MAX_NUMBER);
            }
            
            int nextNumber = lastNumber + 1;
            
            if (!isCodeExists(String.format("%s%05d", CODE_PREFIX, nextNumber))) {
                return nextNumber;
            }
            
            return findFirstAvailableNumber();
            
        } catch (NumberFormatException e) {
            logger.warn("解析编码失败: {}, 从1开始", lastCode);
            return 1;
        }
    }
    
    /**
     * 检查编码是否已存在（包括已删除的记录）
     * @param code 编码
     * @return 是否存在
     */
    private boolean isCodeExists(String code) {
        QueryWrapper<Food> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("food_code", code);
        return foodMapper.selectCount(queryWrapper) > 0;
    }
    
    /**
     * 查找第一个可用的编码数字
     * @return 第一个可用的数字
     */
    private int findFirstAvailableNumber() {
        for (int i = 1; i <= MAX_NUMBER; i++) {
            String code = String.format("%s%05d", CODE_PREFIX, i);
            if (!isCodeExists(code)) {
                return i;
            }
        }
        throw new RuntimeException("菜品编码已达最大值" + CODE_PREFIX + MAX_NUMBER);
    }
    
    /**
     * 验证编码格式是否正确
     * @param code 待验证的编码
     * @return 是否有效
     */
    public boolean isValidCode(String code) {
        if (code == null || !code.startsWith(CODE_PREFIX)) {
            return false;
        }
        
        try {
            String numberPart = code.substring(CODE_PREFIX.length());
            int number = Integer.parseInt(numberPart);
            return number >= 1 && number <= MAX_NUMBER;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}