package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.dataservice.FoodDataService;
import com.foodtraceability.dto.product.*;
import com.foodtraceability.entity.ComboIngredientNew;
import com.foodtraceability.entity.DishRecipeNew;
import com.foodtraceability.entity.FoodCategoryNew;
import com.foodtraceability.entity.FoodNew;
import com.foodtraceability.mapper.ComboIngredientNewMapper;
import com.foodtraceability.mapper.DishRecipeNewMapper;
import com.foodtraceability.mapper.FoodCategoryNewMapper;
import com.foodtraceability.mapper.FoodNewMapper;
import com.foodtraceability.service.FoodService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 菜品服务实现类
 * 完整的菜品业务逻辑：CRUD、状态管理、编码生成、缓存更新
 */
@Service
public class FoodServiceImpl extends ServiceImpl<FoodNewMapper, FoodNew> implements FoodService {

    private static final Logger log = LoggerFactory.getLogger(FoodServiceImpl.class);
    /** 菜品状态常量（遵循项目统一标准：1在售 0停售 2售罄） */
    private static final int STATUS_ON_SALE = 1;   // 在售 active
    private static final int STATUS_OFF_SALE = 0;  // 停售 inactive
    private static final int STATUS_SOLD_OUT = 2;  // 售罄 soldout

    private final FoodNewMapper foodNewMapper;
    private final FoodCategoryNewMapper categoryMapper;
    private final FoodDataService foodDataService;
    private final ComboIngredientNewMapper comboIngredientMapper;
    private final DishRecipeNewMapper dishRecipeNewMapper;

    public FoodServiceImpl(FoodNewMapper foodNewMapper,
                           FoodCategoryNewMapper categoryMapper,
                           FoodDataService foodDataService,
                           ComboIngredientNewMapper comboIngredientMapper,
                           DishRecipeNewMapper dishRecipeNewMapper) {
        this.foodNewMapper = foodNewMapper;
        this.categoryMapper = categoryMapper;
        this.foodDataService = foodDataService;
        this.comboIngredientMapper = comboIngredientMapper;
        this.dishRecipeNewMapper = dishRecipeNewMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FoodVO create(FoodCreateDTO dto) {
        // 1. 校验分类是否存在且启用
        validateCategory(dto.getCategoryId());

        // 2. 检查菜品名称是否重复
        checkFoodNameDuplicate(null, dto.getFoodName());

        // 3. 构建实体并保存
        FoodNew food = new FoodNew();
        food.setFoodName(dto.getFoodName());
        food.setFoodCode(generateFoodCode());
        food.setCategoryId(dto.getCategoryId());
        food.setSpecification(dto.getSpecification());
        food.setUnit(dto.getUnit() != null ? dto.getUnit() : "份");
        food.setSalePrice(dto.getSalePrice());
        food.setCostPrice(dto.getCostPrice());
        food.setStock(dto.getStock());
        food.setMinStock(dto.getMinStock());
        food.setImageUrl(dto.getImageUrl());
        food.setDescription(dto.getDescription());
        food.setCookingTime(dto.getCookingTime());
        food.setStatus(dto.getStatus() != null ? dto.getStatus() : STATUS_ON_SALE);
        food.setIsRecommend(dto.getIsRecommend() != null ? dto.getIsRecommend() : false);
        food.setIsSpicy(dto.getIsSpicy());
        food.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);

        foodNewMapper.insert(food);
        log.info("创建菜品成功: foodId={}, foodName={}", food.getFoodId(), food.getFoodName());

        // 保存原料明细并计算成本
        List<RecipeItemDTO> recipes = dto.getRecipes();
        if (recipes != null && !recipes.isEmpty()) {
            long totalCostFen = saveRecipes(food.getFoodId(), recipes, food.getFoodName(), food.getFoodCode());
            // 更新成本价（分）
            food.setCostPrice(totalCostFen);
            foodNewMapper.updateById(food);
        }

        return convertToVO(food);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FoodVO update(Long foodId, FoodUpdateDTO dto) {
        // 1. 校验菜品是否存在
        FoodNew existing = getExistingFood(foodId);

        // 2. 校验分类（如果提供了）
        if (dto.getCategoryId() != null) {
            validateCategory(dto.getCategoryId());
        }

        // 3. 检查名称是否重复（排除自身）
        if (dto.getFoodName() != null && !dto.getFoodName().equals(existing.getFoodName())) {
            checkFoodNameDuplicate(foodId, dto.getFoodName());
        }

        // 4. 更新字段（只更新非空字段）
        if (dto.getFoodName() != null) {
            existing.setFoodName(dto.getFoodName());
        }
        if (dto.getCategoryId() != null) {
            existing.setCategoryId(dto.getCategoryId());
        }
        if (dto.getSpecification() != null) {
            existing.setSpecification(dto.getSpecification());
        }
        if (dto.getUnit() != null) {
            existing.setUnit(dto.getUnit());
        }
        if (dto.getSalePrice() != null) {
            existing.setSalePrice(dto.getSalePrice());
        }
        if (dto.getCostPrice() != null) {
            existing.setCostPrice(dto.getCostPrice());
        }
        if (dto.getStock() != null) {
            existing.setStock(dto.getStock());
        }
        if (dto.getMinStock() != null) {
            existing.setMinStock(dto.getMinStock());
        }
        if (dto.getImageUrl() != null) {
            existing.setImageUrl(dto.getImageUrl());
        }
        if (dto.getDescription() != null) {
            existing.setDescription(dto.getDescription());
        }
        if (dto.getCookingTime() != null) {
            existing.setCookingTime(dto.getCookingTime());
        }
        if (dto.getStatus() != null) {
            existing.setStatus(dto.getStatus());
        }
        if (dto.getIsRecommend() != null) {
            existing.setIsRecommend(dto.getIsRecommend());
        }
        if (dto.getIsSpicy() != null) {
            existing.setIsSpicy(dto.getIsSpicy());
        }
        if (dto.getSortOrder() != null) {
            existing.setSortOrder(dto.getSortOrder());
        }

        foodNewMapper.updateById(existing);

        // 5. 处理原料明细（如果请求中携带 recipes 字段）
        List<RecipeItemDTO> recipes = dto.getRecipes();
        if (recipes != null) {
            // 先删除旧的原料明细（逻辑删除）
            LambdaQueryWrapper<DishRecipeNew> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(DishRecipeNew::getFoodId, foodId);
            dishRecipeNewMapper.delete(deleteWrapper);

            // 再插入新的原料明细
            if (!recipes.isEmpty()) {
                long totalCostFen = saveRecipes(foodId, recipes, existing.getFoodName(), existing.getFoodCode());
                // 更新成本价（分）
                existing.setCostPrice(totalCostFen);
                foodNewMapper.updateById(existing);
            } else {
                // 明细被清空时重置成本价为0
                existing.setCostPrice(0L);
                foodNewMapper.updateById(existing);
            }
        }

        // 6. 清除缓存
        foodDataService.clearFoodCache(foodId);

        log.info("更新菜品成功: foodId={}", foodId);
        return convertToVO(existing);
    }

    @Override
    public FoodVO getById(Long foodId) {
        FoodNew food = getExistingFood(foodId);
        FoodVO vo = convertToVO(food);
        // 加载原料明细设置到VO
        vo.setRecipes(loadRecipesByFoodId(foodId));
        return vo;
    }

    @Override
    public Page<FoodVO> queryPage(FoodQueryDTO queryDto) {
        Page<FoodNew> page = new Page<>(queryDto.getPage(), queryDto.getSize());

        LambdaQueryWrapper<FoodNew> wrapper = new LambdaQueryWrapper<>();

        // 名称模糊搜索
        if (StringUtils.hasText(queryDto.getFoodName())) {
            wrapper.like(FoodNew::getFoodName, queryDto.getFoodName());
        }
        // 编码精确搜索
        if (StringUtils.hasText(queryDto.getFoodCode())) {
            wrapper.eq(FoodNew::getFoodCode, queryDto.getFoodCode());
        }
        // 关键字搜索（名称或编码模糊匹配，二选一）
        if (StringUtils.hasText(queryDto.getKeyword())) {
            wrapper.and(w -> w.like(FoodNew::getFoodName, queryDto.getKeyword())
                               .or().like(FoodNew::getFoodCode, queryDto.getKeyword()));
        }
        // 分类筛选
        if (queryDto.getCategoryId() != null) {
            wrapper.eq(FoodNew::getCategoryId, queryDto.getCategoryId());
        }
        // 状态筛选
        if (queryDto.getStatus() != null) {
            wrapper.eq(FoodNew::getStatus, queryDto.getStatus());
        }
        // 推荐筛选
        if (queryDto.getIsRecommend() != null) {
            wrapper.eq(FoodNew::getIsRecommend, queryDto.getIsRecommend());
        }
        // 辣味筛选
        if (queryDto.getIsSpicy() != null) {
            wrapper.eq(FoodNew::getIsSpicy, queryDto.getIsSpicy());
        }

        // 排序
        String sortField = StringUtils.hasText(queryDto.getSortField()) ? 
                queryDto.getSortField() : "sort_order";
        boolean isAsc = "asc".equalsIgnoreCase(queryDto.getSortOrder());
        
        if ("food_name".equals(sortField)) {
            wrapper.orderBy(true, isAsc, FoodNew::getFoodName);
        } else if ("sale_price".equals(sortField)) {
            wrapper.orderBy(true, isAsc, FoodNew::getSalePrice);
        } else if ("create_time".equals(sortField)) {
            wrapper.orderBy(true, isAsc, FoodNew::getCreateTime);
        } else {
            wrapper.orderByAsc(FoodNew::getSortOrder);
            wrapper.orderByDesc(FoodNew::getFoodId);
        }

        Page<FoodNew> resultPage = foodNewMapper.selectPage(page, wrapper);

        // 转换为VO
        Page<FoodVO> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        voPage.setRecords(resultPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList()));

        return voPage;
    }

    @Override
    public List<FoodVO> listOnSale() {
        LambdaQueryWrapper<FoodNew> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FoodNew::getStatus, STATUS_ON_SALE)
               .orderByAsc(FoodNew::getSortOrder)
               .orderByDesc(FoodNew::getFoodId);

        List<FoodNew> foods = foodNewMapper.selectList(wrapper);
        return foods.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public List<FoodVO> listRecommend(int limit) {
        // 安全性校验：限制最大返回数量，防止恶意请求
        int safeLimit = Math.min(Math.max(limit, 1), 100);

        LambdaQueryWrapper<FoodNew> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FoodNew::getIsRecommend, true)
               .eq(FoodNew::getStatus, STATUS_ON_SALE)
               .orderByAsc(FoodNew::getSortOrder)
               .last("LIMIT " + safeLimit);

        List<FoodNew> foods = foodNewMapper.selectList(wrapper);
        return foods.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public List<FoodVO> listByCategory(Long categoryId) {
        LambdaQueryWrapper<FoodNew> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FoodNew::getCategoryId, categoryId)
               .eq(FoodNew::getStatus, STATUS_ON_SALE)
               .orderByAsc(FoodNew::getSortOrder)
               .orderByDesc(FoodNew::getFoodId);

        List<FoodNew> foods = foodNewMapper.selectList(wrapper);
        return foods.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long foodId, Integer status) {
        FoodNew food = getExistingFood(foodId);
        
        // 状态校验
        validateStatus(status);
        
        food.setStatus(status);
        foodNewMapper.updateById(food);
        
        // 清除缓存
        foodDataService.clearFoodCache(foodId);
        
        log.info("更新菜品状态: foodId={}, status={}", foodId, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateStatus(List<Long> foodIds, Integer status) {
        if (foodIds == null || foodIds.isEmpty()) {
            return;
        }

        // 安全性校验：限制单次操作数量，防止恶意大批量请求
        if (foodIds.size() > 100) {
            throw new IllegalArgumentException("单次批量更新状态不能超过100条记录");
        }

        validateStatus(status);
        
        FoodNew updateEntity = new FoodNew();
        updateEntity.setStatus(status);
        
        LambdaQueryWrapper<FoodNew> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(FoodNew::getFoodId, foodIds);
        
        foodNewMapper.update(updateEntity, wrapper);
        
        // 批量清除缓存
        foodDataService.clearFoodBatchCache(foodIds);
        
        log.info("批量更新菜品状态: count={}, status={}", foodIds.size(), status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long foodId) {
        getExistingFood(foodId);

        LambdaQueryWrapper<ComboIngredientNew> ingredientWrapper = new LambdaQueryWrapper<>();
        ingredientWrapper.eq(ComboIngredientNew::getFoodId, foodId);
        long comboRefCount = comboIngredientMapper.selectCount(ingredientWrapper);
        if (comboRefCount > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "该菜品被套餐引用，无法删除。请先从相关套餐中移除该菜品。");
        }

        foodNewMapper.deleteById(foodId);

        foodDataService.clearFoodCache(foodId);

        log.info("删除菜品: foodId={}", foodId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> foodIds) {
        if (foodIds == null || foodIds.isEmpty()) {
            return;
        }

        // 安全性校验：限制单次操作数量，防止恶意大批量请求
        if (foodIds.size() > 100) {
            throw new IllegalArgumentException("单次批量删除不能超过100条记录");
        }

        foodNewMapper.deleteBatchIds(foodIds);
        
        // 批量清除缓存
        foodDataService.clearFoodBatchCache(foodIds);
        
        log.info("批量删除菜品: count={}", foodIds.size());
    }

    // ==================== 导入导出 ====================

    /**
     * 批量导入菜品（Excel）
     * 解析Excel文件，逐行校验并创建菜品，返回成功/失败统计
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FoodImportResultDTO importFoods(MultipartFile file) {
        FoodImportResultDTO result = new FoodImportResultDTO();
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        // 1. 文件校验
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "导入文件不能为空");
        }

        // 2. 预加载所有启用的分类（名称→ID映射）
        Map<String, Long> categoryNameToIdMap = loadCategoryNameMap();

        // 3. 解析Excel并逐行处理
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();

            for (int i = 1; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                // 跳过空行（菜品名称为空视为空行）
                String foodName = getCellAsString(row.getCell(0));
                if (!StringUtils.hasText(foodName)) {
                    continue;
                }

                try {
                    FoodNew food = parseFoodFromRow(row, categoryNameToIdMap);
                    food.setFoodCode(generateFoodCode());
                    foodNewMapper.insert(food);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    errors.add("第" + (i + 1) + "行: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("解析Excel文件失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "解析Excel文件失败: " + e.getMessage());
        }

        result.setTotalCount(successCount + failCount);
        result.setSuccessCount(successCount);
        result.setFailCount(failCount);
        result.setErrorMessages(errors);

        log.info("菜品导入完成: 总数={}, 成功={}, 失败={}", result.getTotalCount(), successCount, failCount);
        return result;
    }

    /**
     * 导出菜品数据（Excel）
     * 根据查询条件导出匹配的菜品数据为Excel文件
     */
    @Override
    public void exportFoods(FoodQueryDTO queryDto, HttpServletResponse response) {
        // 1. 查询匹配的菜品数据（不分页，导出全部）
        List<FoodNew> foods = queryFoodsForExport(queryDto);

        // 2. 加载分类映射（ID→名称）
        Map<Long, String> categoryIdToNameMap = loadCategoryIdToNameMap();

        // 3. 生成Excel并写入响应
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            Sheet sheet = workbook.createSheet("菜品数据");

            // 表头
            String[] headers = {"菜品编码", "菜品名称", "分类", "售价(元)", "成本(元)", "单位", "规格",
                    "库存", "最低库存", "制作时间(分钟)", "状态", "是否推荐", "是否辣", "排序", "描述"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            // 数据行
            for (int i = 0; i < foods.size(); i++) {
                Row row = sheet.createRow(i + 1);
                FoodNew food = foods.get(i);
                writeFoodToRow(row, food, categoryIdToNameMap);
            }

            // 设置响应头并写入输出流
            setExcelResponseHeaders(response, "菜品数据导出");
            workbook.write(response.getOutputStream());
            workbook.dispose();
        } catch (IOException e) {
            log.error("导出Excel失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "导出Excel失败: " + e.getMessage());
        }

        log.info("菜品导出完成: 共{}条记录", foods.size());
    }

    /**
     * 下载导入模板（Excel）
     * 生成包含表头和示例数据的Excel模板文件
     *
     * 模板字段与编辑窗口保持一致，仅包含编辑窗口中可填写的字段：
     * 菜品名称、分类名称、售价、成本、状态、是否推荐、排序、描述
     */
    @Override
    public void downloadImportTemplate(HttpServletResponse response) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("菜品导入模板");

            // 表头（与编辑窗口字段保持一致，移除单位/规格/库存/最低库存/制作时间/是否辣）
            String[] headers = {"菜品名称", "分类名称", "售价(元)", "成本(元)",
                    "状态", "是否推荐", "排序", "描述"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            // 示例行
            Row exampleRow = sheet.createRow(1);
            exampleRow.createCell(0).setCellValue("红烧肉");
            exampleRow.createCell(1).setCellValue("热菜");
            exampleRow.createCell(2).setCellValue(38.00);
            exampleRow.createCell(3).setCellValue(15.00);
            exampleRow.createCell(4).setCellValue("在售");
            exampleRow.createCell(5).setCellValue("否");
            exampleRow.createCell(6).setCellValue(0);
            exampleRow.createCell(7).setCellValue("经典家常菜");

            // 设置列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 5000);
            }

            setExcelResponseHeaders(response, "菜品导入模板");
            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            log.error("生成导入模板失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "生成导入模板失败: " + e.getMessage());
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 获取已存在的菜品，不存在则抛异常
     */
    private FoodNew getExistingFood(Long foodId) {
        FoodNew food = foodNewMapper.selectById(foodId);
        if (food == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "菜品不存在: " + foodId);
        }
        return food;
    }

    /**
     * 校验分类是否存在且启用
     */
    private void validateCategory(Long categoryId) {
        FoodCategoryNew category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分类不存在: " + categoryId);
        }
        if (category.getStatus() != 1) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "分类已停用: " + category.getCategoryName());
        }
    }

    /**
     * 检查菜品名称是否重复
     */
    private void checkFoodNameDuplicate(Long excludeId, String foodName) {
        LambdaQueryWrapper<FoodNew> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FoodNew::getFoodName, foodName);
        if (excludeId != null) {
            wrapper.ne(FoodNew::getFoodId, excludeId);
        }
        
        Long count = foodNewMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "菜品名称已存在: " + foodName);
        }
    }

    /**
     * 校验状态值合法性
     */
    private void validateStatus(Integer status) {
        if (status == null || (status != STATUS_ON_SALE && status != STATUS_OFF_SALE && status != STATUS_SOLD_OUT)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无效的状态值: " + status);
        }
    }

    /**
     * 生成菜品编码
     * 格式: FD + 年月日(yyMMdd) + 3位序号（短编码，如 FD260731001）
     */
    private String generateFoodCode() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        // 查询当天最大编码
        String prefix = "FD" + datePart;
        
        LambdaQueryWrapper<FoodNew> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(FoodNew::getFoodCode, prefix)
               .orderByDesc(FoodNew::getFoodCode)
               .last("LIMIT 1");
        
        FoodNew lastFood = foodNewMapper.selectOne(wrapper);
        
        int seq = 1;
        if (lastFood != null && lastFood.getFoodCode() != null) {
            try {
                String lastSeqStr = lastFood.getFoodCode().substring(prefix.length());
                seq = Integer.parseInt(lastSeqStr) + 1;
            } catch (Exception e) {
                seq = 1;
            }
        }
        
        return String.format("%s%03d", prefix, seq);
    }

    /**
     * 将实体转换为VO
     */
    private FoodVO convertToVO(FoodNew food) {
        if (food == null) {
            return null;
        }

        FoodVO vo = new FoodVO();
        vo.setFoodId(food.getFoodId());
        vo.setFoodCode(food.getFoodCode());
        vo.setFoodName(food.getFoodName());
        vo.setCategoryId(food.getCategoryId());
        vo.setSpecification(food.getSpecification());
        vo.setUnit(food.getUnit());
        vo.setSalePrice(food.getSalePrice());
        vo.setCostPrice(food.getCostPrice());
        vo.setStock(food.getStock());
        vo.setMinStock(food.getMinStock());
        vo.setImageUrl(food.getImageUrl());
        vo.setDescription(food.getDescription());
        vo.setCookingTime(food.getCookingTime());
        vo.setStatus(food.getStatus());
        vo.setStatusName(getStatusName(food.getStatus()));
        vo.setIsRecommend(food.getIsRecommend());
        vo.setIsSpicy(food.getIsSpicy());
        vo.setSortOrder(food.getSortOrder());
        vo.setCreateTime(food.getCreateTime());
        vo.setUpdateTime(food.getUpdateTime());

        // 计算毛利和毛利率
        if (food.getSalePrice() != null && food.getCostPrice() != null && food.getCostPrice() > 0) {
            long profit = food.getSalePrice() - food.getCostPrice();
            vo.setProfit(profit);
            double profitRate = Math.round((double) profit / food.getCostPrice() * 10000) / 100.0;
            vo.setProfitRate(profitRate);
        }

        // 设置分类名称
        if (food.getCategoryId() != null) {
            FoodCategoryNew category = categoryMapper.selectById(food.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getCategoryName());
            }
        }

        return vo;
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case STATUS_ON_SALE -> "在售";
            case STATUS_OFF_SALE -> "停售";
            case STATUS_SOLD_OUT -> "售罄";
            default -> "未知";
        };
    }

    /**
     * 批量保存菜品原料明细到 dish_recipes 表
     * @param foodId 菜品ID
     * @param recipes 原料明细列表
     * @param foodName 菜品名称（冗余存储）
     * @param foodCode 菜品编码（冗余存储）
     * @return 原料总成本（分）
     */
    private long saveRecipes(Long foodId, List<RecipeItemDTO> recipes, String foodName, String foodCode) {
        long totalCostFen = 0L;
        int sortOrder = 0;
        for (RecipeItemDTO item : recipes) {
            if (item.getMaterialId() == null) {
                continue;
            }
            // 单价（分），默认0
            long unitPriceFen = item.getUnitPrice() != null ? item.getUnitPrice() : 0L;
            // 数量，默认0
            double quantity = item.getQuantity() != null ? item.getQuantity() : 0D;
            // 小计成本（分）= 单价（分）× 数量
            long subtotalFen = Math.round(unitPriceFen * quantity);
            totalCostFen += subtotalFen;

            DishRecipeNew recipe = new DishRecipeNew();
            recipe.setFoodId(foodId);
            recipe.setMaterialId(item.getMaterialId());
            recipe.setMaterialName(item.getMaterialName());
            recipe.setRequiredQuantity(BigDecimal.valueOf(quantity));
            recipe.setUnit(item.getUnit());
            recipe.setLossRate(BigDecimal.valueOf(item.getLossRate() != null ? item.getLossRate() : 0D));
            recipe.setUnitCost(unitPriceFen);
            recipe.setSubtotalCost(subtotalFen);
            dishRecipeNewMapper.insert(recipe);
            sortOrder++;
        }
        return totalCostFen;
    }

    /**
     * 根据菜品ID加载原料明细列表
     */
    private List<RecipeItemDTO> loadRecipesByFoodId(Long foodId) {
        LambdaQueryWrapper<DishRecipeNew> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DishRecipeNew::getFoodId, foodId)
               .orderByAsc(DishRecipeNew::getRecipeId);
        List<DishRecipeNew> recipes = dishRecipeNewMapper.selectList(wrapper);
        if (recipes == null || recipes.isEmpty()) {
            return new ArrayList<>();
        }
        List<RecipeItemDTO> result = new ArrayList<>(recipes.size());
        for (DishRecipeNew recipe : recipes) {
            RecipeItemDTO dto = new RecipeItemDTO();
            dto.setMaterialId(recipe.getMaterialId());
            dto.setMaterialName(recipe.getMaterialName());
            dto.setQuantity(recipe.getRequiredQuantity() != null ? recipe.getRequiredQuantity().doubleValue() : 0D);
            dto.setUnit(recipe.getUnit());
            dto.setUnitPrice(recipe.getUnitCost());
            dto.setLossRate(recipe.getLossRate() != null ? recipe.getLossRate().doubleValue() : 0D);
            result.add(dto);
        }
        return result;
    }

    // ==================== 导入导出辅助方法 ====================

    /**
     * 加载启用的分类名称→ID映射（用于导入时根据分类名查找ID）
     */
    private Map<String, Long> loadCategoryNameMap() {
        LambdaQueryWrapper<FoodCategoryNew> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FoodCategoryNew::getStatus, 1);
        List<FoodCategoryNew> categories = categoryMapper.selectList(wrapper);
        Map<String, Long> map = new HashMap<>();
        for (FoodCategoryNew category : categories) {
            map.put(category.getCategoryName(), category.getCategoryId());
        }
        return map;
    }

    /**
     * 加载分类ID→名称映射（用于导出时根据ID查找名称）
     */
    private Map<Long, String> loadCategoryIdToNameMap() {
        List<FoodCategoryNew> categories = categoryMapper.selectList(null);
        Map<Long, String> map = new HashMap<>();
        for (FoodCategoryNew category : categories) {
            map.put(category.getCategoryId(), category.getCategoryName());
        }
        return map;
    }

    /**
     * 查询菜品用于导出（不分页，导出全部匹配数据）
     */
    private List<FoodNew> queryFoodsForExport(FoodQueryDTO queryDto) {
        LambdaQueryWrapper<FoodNew> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(queryDto.getFoodName())) {
            wrapper.like(FoodNew::getFoodName, queryDto.getFoodName());
        }
        if (StringUtils.hasText(queryDto.getFoodCode())) {
            wrapper.eq(FoodNew::getFoodCode, queryDto.getFoodCode());
        }
        if (queryDto.getCategoryId() != null) {
            wrapper.eq(FoodNew::getCategoryId, queryDto.getCategoryId());
        }
        if (queryDto.getStatus() != null) {
            wrapper.eq(FoodNew::getStatus, queryDto.getStatus());
        }
        if (queryDto.getIsRecommend() != null) {
            wrapper.eq(FoodNew::getIsRecommend, queryDto.getIsRecommend());
        }
        if (queryDto.getIsSpicy() != null) {
            wrapper.eq(FoodNew::getIsSpicy, queryDto.getIsSpicy());
        }

        wrapper.orderByAsc(FoodNew::getSortOrder).orderByDesc(FoodNew::getFoodId);
        return foodNewMapper.selectList(wrapper);
    }

    /**
     * 从Excel行解析菜品数据
     * 列顺序（与导入模板一致）：0菜品名称 1分类名称 2售价(元) 3成本(元)
     *         4状态 5是否推荐 6排序 7描述
     *
     * 说明：模板字段已裁剪，与编辑窗口保持一致。
     * 单位/规格/库存/最低库存/制作时间/是否辣等字段不在模板中，
     * 使用数据库默认值或实体默认值（unit默认"份"，库存相关默认0，isSpicy默认false）。
     */
    private FoodNew parseFoodFromRow(Row row, Map<String, Long> categoryNameToIdMap) {
        String foodName = getCellAsString(row.getCell(0));
        String categoryName = getCellAsString(row.getCell(1));
        String salePriceStr = getCellAsString(row.getCell(2));
        String costPriceStr = getCellAsString(row.getCell(3));
        String statusStr = getCellAsString(row.getCell(4));
        String isRecommendStr = getCellAsString(row.getCell(5));
        String sortOrderStr = getCellAsString(row.getCell(6));
        String description = getCellAsString(row.getCell(7));

        // 校验必填字段
        if (!StringUtils.hasText(foodName)) {
            throw new IllegalArgumentException("菜品名称不能为空");
        }
        if (!StringUtils.hasText(categoryName)) {
            throw new IllegalArgumentException("分类名称不能为空");
        }
        Long categoryId = categoryNameToIdMap.get(categoryName);
        if (categoryId == null) {
            throw new IllegalArgumentException("分类不存在或已停用: " + categoryName);
        }
        if (!StringUtils.hasText(salePriceStr)) {
            throw new IllegalArgumentException("售价不能为空");
        }

        // 检查菜品名称是否重复
        checkFoodNameDuplicate(null, foodName);

        // 构建实体（模板未包含的字段使用默认值）
        FoodNew food = new FoodNew();
        food.setFoodName(foodName);
        food.setCategoryId(categoryId);
        food.setSalePrice(yuanToFen(salePriceStr));
        if (StringUtils.hasText(costPriceStr)) {
            food.setCostPrice(yuanToFen(costPriceStr));
        }
        // 单位默认"份"（与创建接口一致）
        food.setUnit("份");
        food.setStatus(parseStatusValue(statusStr));
        food.setIsRecommend(parseBooleanValue(isRecommendStr));
        if (StringUtils.hasText(sortOrderStr)) {
            food.setSortOrder(parseIntegerValue(sortOrderStr, "排序"));
        } else {
            food.setSortOrder(0);
        }
        if (StringUtils.hasText(description)) {
            food.setDescription(description);
        }

        return food;
    }

    /**
     * 将菜品数据写入Excel行
     * 列顺序：0编码 1名称 2分类 3售价(元) 4成本(元) 5单位 6规格
     *         7库存 8最低库存 9制作时间 10状态 11推荐 12辣 13排序 14描述
     */
    private void writeFoodToRow(Row row, FoodNew food, Map<Long, String> categoryIdToNameMap) {
        row.createCell(0).setCellValue(food.getFoodCode() != null ? food.getFoodCode() : "");
        row.createCell(1).setCellValue(food.getFoodName() != null ? food.getFoodName() : "");
        String categoryName = food.getCategoryId() != null
                ? categoryIdToNameMap.getOrDefault(food.getCategoryId(), "") : "";
        row.createCell(2).setCellValue(categoryName);
        row.createCell(3).setCellValue(fenToYuanValue(food.getSalePrice()));
        row.createCell(4).setCellValue(fenToYuanValue(food.getCostPrice()));
        row.createCell(5).setCellValue(food.getUnit() != null ? food.getUnit() : "");
        row.createCell(6).setCellValue(food.getSpecification() != null ? food.getSpecification() : "");
        row.createCell(7).setCellValue(food.getStock() != null ? food.getStock() : 0);
        row.createCell(8).setCellValue(food.getMinStock() != null ? food.getMinStock() : 0);
        row.createCell(9).setCellValue(food.getCookingTime() != null ? food.getCookingTime() : 0);
        row.createCell(10).setCellValue(getStatusName(food.getStatus()));
        row.createCell(11).setCellValue(Boolean.TRUE.equals(food.getIsRecommend()) ? "是" : "否");
        row.createCell(12).setCellValue(Boolean.TRUE.equals(food.getIsSpicy()) ? "是" : "否");
        row.createCell(13).setCellValue(food.getSortOrder() != null ? food.getSortOrder() : 0);
        row.createCell(14).setCellValue(food.getDescription() != null ? food.getDescription() : "");
    }

    /**
     * 获取单元格值作为字符串
     */
    private String getCellAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue().toString();
                }
                double num = cell.getNumericCellValue();
                if (num == Math.floor(num)) {
                    yield String.valueOf((long) num);
                }
                yield String.valueOf(num);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    /**
     * 元转分（String → Long）
     */
    private Long yuanToFen(String yuanStr) {
        double yuan = Double.parseDouble(yuanStr.trim());
        return Math.round(yuan * 100);
    }

    /**
     * 分转元（Long → double）
     */
    private double fenToYuanValue(Long fen) {
        if (fen == null) return 0.0;
        return fen / 100.0;
    }

    /**
     * 解析整数值
     */
    private Integer parseIntegerValue(String str, String fieldName) {
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + "格式不正确: " + str);
        }
    }

    /**
     * 解析状态值（在售→1, 停售→0, 售罄→2）
     */
    private Integer parseStatusValue(String statusStr) {
        if (!StringUtils.hasText(statusStr)) {
            return STATUS_ON_SALE;
        }
        return switch (statusStr.trim()) {
            case "在售" -> STATUS_ON_SALE;
            case "停售" -> STATUS_OFF_SALE;
            case "售罄" -> STATUS_SOLD_OUT;
            default -> STATUS_ON_SALE;
        };
    }

    /**
     * 解析布尔值（是→true, 否→false）
     */
    private Boolean parseBooleanValue(String str) {
        if (!StringUtils.hasText(str)) {
            return false;
        }
        return "是".equals(str.trim()) || "true".equalsIgnoreCase(str.trim());
    }

    /**
     * 设置Excel下载响应头
     */
    private void setExcelResponseHeaders(HttpServletResponse response, String fileName) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode(fileName + ".xlsx", StandardCharsets.UTF_8));
    }
}
