package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.dto.product.*;
import com.foodtraceability.entity.FoodCategoryNew;
import com.foodtraceability.entity.FoodNew;
import com.foodtraceability.mapper.FoodCategoryNewMapper;
import com.foodtraceability.mapper.FoodNewMapper;
import com.foodtraceability.service.CategoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 分类服务实现类
 * 支持二级树形结构的完整管理
 */
@Service
public class CategoryServiceImpl implements CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryServiceImpl.class);
    private static final long ROOT_PARENT_ID = 0L;  // 顶级分类的父ID
    /** 分类状态常量（遵循项目统一标准：1启用 0停用） */
    private static final int STATUS_ENABLED = 1;     // 启用 active
    private static final int STATUS_DISABLED = 0;    // 停用 inactive

    private final FoodCategoryNewMapper categoryMapper;
    private final FoodNewMapper foodNewMapper;

    public CategoryServiceImpl(FoodCategoryNewMapper categoryMapper,
                               FoodNewMapper foodNewMapper) {
        this.categoryMapper = categoryMapper;
        this.foodNewMapper = foodNewMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryVO create(CategoryCreateDTO dto) {
        // 校验父级分类是否存在（非顶级时）
        if (dto.getParentId() != null && !dto.getParentId().equals(ROOT_PARENT_ID)) {
            validateParentExists(dto.getParentId());
        }

        // 检查同级下名称是否重复
        checkCategoryNameDuplicate(null, dto.getCategoryName(), dto.getParentId());

        // 构建实体并保存
        FoodCategoryNew category = new FoodCategoryNew();
        category.setCategoryName(dto.getCategoryName());
        category.setParentId(dto.getParentId() != null ? dto.getParentId() : ROOT_PARENT_ID);
        category.setIconUrl(dto.getIconUrl());
        category.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        category.setStatus(dto.getStatus() != null ? dto.getStatus() : STATUS_ENABLED);

        categoryMapper.insert(category);
        log.info("创建分类成功: categoryId={}, categoryName={}", 
                category.getCategoryId(), category.getCategoryName());

        return convertToVO(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryVO update(Long categoryId, CategoryUpdateDTO dto) {
        FoodCategoryNew existing = getExistingCategory(categoryId);

        // 校验父级（如果更新了）
        if (dto.getParentId() != null && !dto.getParentId().equals(existing.getParentId())) {
            if (!dto.getParentId().equals(ROOT_PARENT_ID)) {
                validateParentExists(dto.getParentId());
            }
            // 不能将自己设为自己的子节点
            if (isDescendant(categoryId, dto.getParentId())) {
                throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "不能将分类移动到其子分类下");
            }
        }

        // 检查名称重复（排除自身）
        Long parentId = dto.getParentId() != null ? dto.getParentId() : existing.getParentId();
        if (dto.getCategoryName() != null && !dto.getCategoryName().equals(existing.getCategoryName())) {
            checkCategoryNameDuplicate(categoryId, dto.getCategoryName(), parentId);
        }

        // 更新字段
        if (dto.getCategoryName() != null) {
            existing.setCategoryName(dto.getCategoryName());
        }
        if (dto.getParentId() != null) {
            existing.setParentId(dto.getParentId());
        }
        if (dto.getIconUrl() != null) {
            existing.setIconUrl(dto.getIconUrl());
        }
        if (dto.getSortOrder() != null) {
            existing.setSortOrder(dto.getSortOrder());
        }
        if (dto.getStatus() != null) {
            existing.setStatus(dto.getStatus());
        }

        categoryMapper.updateById(existing);
        log.info("更新分类成功: categoryId={}", categoryId);

        return convertToVO(existing);
    }

    @Override
    public CategoryVO getById(Long categoryId) {
        FoodCategoryNew category = getExistingCategory(categoryId);
        return convertToVO(category);
    }

    @Override
    public List<CategoryVO> getTree() {
        List<FoodCategoryNew> allCategories = categoryMapper.selectList(
                new LambdaQueryWrapper<FoodCategoryNew>()
                        .orderByAsc(FoodCategoryNew::getSortOrder)
                        .orderByAsc(FoodCategoryNew::getCategoryId)
        );
        return buildTree(allCategories, ROOT_PARENT_ID);
    }

    @Override
    public List<CategoryVO> getEnabledTree() {
        List<FoodCategoryNew> allCategories = categoryMapper.selectList(
                new LambdaQueryWrapper<FoodCategoryNew>()
                        .eq(FoodCategoryNew::getStatus, STATUS_ENABLED)
                        .orderByAsc(FoodCategoryNew::getSortOrder)
                        .orderByAsc(FoodCategoryNew::getCategoryId)
        );
        return buildTree(allCategories, ROOT_PARENT_ID);
    }

    @Override
    public List<CategoryVO> listAll() {
        List<FoodCategoryNew> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<FoodCategoryNew>()
                        .orderByAsc(FoodCategoryNew::getSortOrder)
                        .orderByAsc(FoodCategoryNew::getCategoryId)
        );
        return categories.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public List<CategoryVO> listChildren(Long parentId) {
        List<FoodCategoryNew> children = categoryMapper.selectList(
                new LambdaQueryWrapper<FoodCategoryNew>()
                        .eq(FoodCategoryNew::getParentId, parentId)
                        .orderByAsc(FoodCategoryNew::getSortOrder)
                        .orderByAsc(FoodCategoryNew::getCategoryId)
        );
        return children.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long categoryId, Integer status) {
        FoodCategoryNew category = getExistingCategory(categoryId);
        
        if (status != STATUS_ENABLED && status != STATUS_DISABLED) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无效的状态值: " + status);
        }
        
        category.setStatus(status);
        categoryMapper.updateById(category);
        
        // 如果停用分类，同时停用所有子分类和该分类下的菜品
        if (status == STATUS_DISABLED) {
            disableChildrenAndFoods(categoryId);
        }
        
        log.info("更新分类状态: categoryId={}, status={}", categoryId, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSortOrder(Long categoryId, Integer newSortOrder) {
        getExistingCategory(categoryId);  // 存在性校验
        
        FoodCategoryNew updateEntity = new FoodCategoryNew();
        updateEntity.setCategoryId(categoryId);
        updateEntity.setSortOrder(newSortOrder);
        categoryMapper.updateById(updateEntity);
        
        log.info("更新分类排序: categoryId={}, sortOrder={}", categoryId, newSortOrder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void moveCategory(Long categoryId, Long newParentId) {
        FoodCategoryNew category = getExistingCategory(categoryId);
        
        if (!newParentId.equals(ROOT_PARENT_ID)) {
            validateParentExists(newParentId);
        }
        // 防止循环引用
        if (isDescendant(categoryId, newParentId)) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "不能将分类移动到其子分类下");
        }
        
        category.setParentId(newParentId);
        categoryMapper.updateById(category);
        
        log.info("移动分类: categoryId={}, newParentId={}", categoryId, newParentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long categoryId) {
        FoodCategoryNew category = getExistingCategory(categoryId);
        
        // 检查是否有子分类
        long childCount = categoryMapper.selectCount(
                new LambdaQueryWrapper<FoodCategoryNew>().eq(FoodCategoryNew::getParentId, categoryId)
        );
        if (childCount > 0) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "该分类下还有子分类，请先删除或移动子分类");
        }
        
        // 检查是否有关联菜品（实时查询 foods 表，排除已删除的菜品）
        // 使用 countByCategoryId 显式查询，SQL 中已包含 deleted = 0 条件，
        // 确保删除校验与分类树中显示的 foodCount 一致，避免依赖 @TableLogic 自动过滤的潜在不一致
        long foodCount = foodNewMapper.countByCategoryId(categoryId);
        if (foodCount > 0) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "该分类下还有" + foodCount + "个菜品，请先转移或删除菜品");
        }
        
        categoryMapper.deleteById(categoryId);
        log.info("删除分类: categoryId={}", categoryId);
    }

    // ==================== 私有辅助方法 ====================

    private FoodCategoryNew getExistingCategory(Long categoryId) {
        FoodCategoryNew category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分类不存在: " + categoryId);
        }
        return category;
    }

    private void validateParentExists(Long parentId) {
        FoodCategoryNew parent = categoryMapper.selectById(parentId);
        if (parent == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "父级分类不存在: " + parentId);
        }
    }

    private void checkCategoryNameDuplicate(Long excludeId, String categoryName, Long parentId) {
        LambdaQueryWrapper<FoodCategoryNew> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FoodCategoryNew::getCategoryName, categoryName)
               .eq(FoodCategoryNew::getParentId, parentId);
        if (excludeId != null) {
            wrapper.ne(FoodCategoryNew::getCategoryId, excludeId);
        }
        
        Long count = categoryMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "同级下已存在同名分类: " + categoryName);
        }
    }

    /**
     * 判断targetId是否是categoryId的后代
     */
    private boolean isDescendant(Long categoryId, Long targetId) {
        if (categoryId.equals(targetId)) {
            return true;
        }
        
        Set<Long> descendantIds = getAllDescendantIds(categoryId);
        return descendantIds.contains(targetId);
    }

    /**
     * 获取某分类的所有后代ID集合
     */
    private Set<Long> getAllDescendantIds(Long parentId) {
        Set<Long> result = new HashSet<>();
        Queue<Long> queue = new LinkedList<>();
        queue.offer(parentId);
        
        while (!queue.isEmpty()) {
            Long currentId = queue.poll();
            List<FoodCategoryNew> children = categoryMapper.selectList(
                    new LambdaQueryWrapper<FoodCategoryNew>().eq(FoodCategoryNew::getParentId, currentId)
            );
            for (FoodCategoryNew child : children) {
                result.add(child.getCategoryId());
                queue.offer(child.getCategoryId());
            }
        }
        
        return result;
    }

    /**
     * 停用分类及其子分类下的所有菜品
     */
    private void disableChildrenAndFoods(Long categoryId) {
        // 收集该分类及所有子分类ID
        Set<Long> categoryIds = new HashSet<>();
        categoryIds.add(categoryId);
        categoryIds.addAll(getAllDescendantIds(categoryId));
        
        // 停用这些分类下的所有菜品（停售=0，遵循项目统一状态标准）
        if (!categoryIds.isEmpty()) {
            FoodNew updateFood = new FoodNew();
            updateFood.setStatus(0);  // 停售 inactive
            
            LambdaQueryWrapper<FoodNew> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(FoodNew::getCategoryId, categoryIds);
            
            foodNewMapper.update(updateFood, wrapper);
            
            // 停用子分类
            FoodCategoryNew updateCategory = new FoodCategoryNew();
            updateCategory.setStatus(STATUS_DISABLED);
            
            LambdaQueryWrapper<FoodCategoryNew> catWrapper = new LambdaQueryWrapper<>();
            catWrapper.in(FoodCategoryNew::getCategoryId, categoryIds);
            catWrapper.ne(FoodCategoryNew::getCategoryId, categoryId);  // 排除自身
            
            categoryMapper.update(updateCategory, catWrapper);
        }
    }

    /**
     * 构建树形结构
     */
    private List<CategoryVO> buildTree(List<FoodCategoryNew> allCategories, Long parentId) {
        return allCategories.stream()
                .filter(c -> Objects.equals(c.getParentId(), parentId))
                .map(category -> {
                    CategoryVO vo = convertToVO(category);
                    vo.setChildren(buildTree(allCategories, category.getCategoryId()));
                    return vo;
                })
                .sorted(Comparator.comparingInt(CategoryVO::getSortOrder)
                        .thenComparingLong(CategoryVO::getCategoryId))
                .collect(Collectors.toList());
    }

    private CategoryVO convertToVO(FoodCategoryNew category) {
        if (category == null) {
            return null;
        }

        CategoryVO vo = new CategoryVO();
        vo.setCategoryId(category.getCategoryId());
        vo.setCategoryName(category.getCategoryName());
        vo.setParentId(category.getParentId());
        vo.setIconUrl(category.getIconUrl());
        vo.setSortOrder(category.getSortOrder());
        vo.setStatus(category.getStatus());
        vo.setStatusName(getStatusName(category.getStatus()));
        vo.setCreateTime(category.getCreateTime());
        vo.setUpdateTime(category.getUpdateTime());

        // 设置父级名称
        if (category.getParentId() != null && !category.getParentId().equals(ROOT_PARENT_ID)) {
            FoodCategoryNew parent = categoryMapper.selectById(category.getParentId());
            if (parent != null) {
                vo.setParentName(parent.getCategoryName());
            }
        } else {
            vo.setParentName("顶级分类");
        }

        // 统计该分类下的菜品数量
        Long foodCount = foodNewMapper.countByCategoryId(category.getCategoryId());
        vo.setFoodCount(foodCount != null ? foodCount.intValue() : 0);

        return vo;
    }

    private String getStatusName(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case STATUS_ENABLED -> "启用";
            case STATUS_DISABLED -> "停用";
            default -> "未知";
        };
    }
}
