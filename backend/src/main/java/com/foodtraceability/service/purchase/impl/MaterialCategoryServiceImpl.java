package com.foodtraceability.service.purchase.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.purchase.MaterialCategoryCreateDTO;
import com.foodtraceability.dto.purchase.MaterialCategoryQueryDTO;
import com.foodtraceability.dto.purchase.MaterialCategoryUpdateDTO;
import com.foodtraceability.dto.purchase.MaterialCategoryVO;
import com.foodtraceability.entity.MaterialCategory;
import com.foodtraceability.mapper.MaterialCategoryMapper;
import com.foodtraceability.service.purchase.MaterialCategoryService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 商品分类服务实现
 * 提供分类的 CRUD + 状态切换 + 删除引用校验 + 关联统计
 */
@Service
public class MaterialCategoryServiceImpl implements MaterialCategoryService {

    private final MaterialCategoryMapper materialCategoryMapper;

    /** 构造函数注入（禁止 @Autowired 字段注入） */
    public MaterialCategoryServiceImpl(MaterialCategoryMapper materialCategoryMapper) {
        this.materialCategoryMapper = materialCategoryMapper;
    }

    // ==================== 公共方法 ====================

    @Override
    public PageResult<MaterialCategoryVO> getCategoryPage(MaterialCategoryQueryDTO queryDTO) {
        int current = queryDTO.getCurrent() == null ? 1 : queryDTO.getCurrent();
        int size = queryDTO.getSize() == null ? 10 : queryDTO.getSize();

        LambdaQueryWrapper<MaterialCategory> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().isEmpty()) {
            String kw = queryDTO.getKeyword();
            wrapper.and(w -> w.like(MaterialCategory::getCategoryName, kw)
                    .or().like(MaterialCategory::getCategoryCode, kw));
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(MaterialCategory::getStatus, queryDTO.getStatus());
        }
        if (queryDTO.getParentId() != null) {
            wrapper.eq(MaterialCategory::getParentId, queryDTO.getParentId());
        }
        wrapper.orderByAsc(MaterialCategory::getSortOrder)
                .orderByDesc(MaterialCategory::getCreateTime);

        Page<MaterialCategory> page = new Page<>(current, size);
        Page<MaterialCategory> resultPage = materialCategoryMapper.selectPage(page, wrapper);

        List<MaterialCategoryVO> voList = convertToVOList(resultPage.getRecords());
        return new PageResult<>(resultPage.getTotal(), voList, (long) current, (long) size);
    }

    @Override
    public MaterialCategoryVO getCategoryById(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        MaterialCategory category = materialCategoryMapper.selectById(categoryId);
        if (category == null) {
            return null;
        }
        List<MaterialCategoryVO> voList = convertToVOList(Collections.singletonList(category));
        return voList.isEmpty() ? null : voList.get(0);
    }

    @Override
    public MaterialCategoryVO createCategory(MaterialCategoryCreateDTO createDTO) {
        // 校验编码唯一性
        LambdaQueryWrapper<MaterialCategory> codeCheck = new LambdaQueryWrapper<>();
        codeCheck.eq(MaterialCategory::getCategoryCode, createDTO.getCategoryCode());
        if (materialCategoryMapper.selectCount(codeCheck) > 0) {
            throw new IllegalArgumentException("分类编码已存在: " + createDTO.getCategoryCode());
        }

        // 校验父分类存在性
        if (createDTO.getParentId() != null) {
            MaterialCategory parent = materialCategoryMapper.selectById(createDTO.getParentId());
            if (parent == null) {
                throw new IllegalArgumentException("父分类不存在: " + createDTO.getParentId());
            }
        }

        MaterialCategory category = new MaterialCategory();
        category.setCategoryCode(createDTO.getCategoryCode());
        category.setCategoryName(createDTO.getCategoryName());
        category.setParentId(createDTO.getParentId());
        category.setDescription(createDTO.getDescription());
        category.setSortOrder(createDTO.getSortOrder() != null ? createDTO.getSortOrder() : 0);
        category.setStatus(1); // 默认启用
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());
        category.setDeleted(0);

        materialCategoryMapper.insert(category);
        return getCategoryById(category.getCategoryId());
    }

    @Override
    public MaterialCategoryVO updateCategory(Long categoryId, MaterialCategoryUpdateDTO updateDTO) {
        MaterialCategory existing = materialCategoryMapper.selectById(categoryId);
        if (existing == null) {
            throw new IllegalArgumentException("分类不存在: " + categoryId);
        }

        // 校验父分类存在性 + 防止循环引用（父分类不能是自己或自己的子分类）
        if (updateDTO.getParentId() != null) {
            if (updateDTO.getParentId().equals(categoryId)) {
                throw new IllegalArgumentException("不能将自身设为父分类");
            }
            MaterialCategory parent = materialCategoryMapper.selectById(updateDTO.getParentId());
            if (parent == null) {
                throw new IllegalArgumentException("父分类不存在: " + updateDTO.getParentId());
            }
            // 校验父分类不是当前分类的子孙
            if (isDescendant(updateDTO.getParentId(), categoryId)) {
                throw new IllegalArgumentException("不能将子分类设为父分类（会形成循环引用）");
            }
        }

        // 部分更新
        if (updateDTO.getCategoryName() != null) {
            existing.setCategoryName(updateDTO.getCategoryName());
        }
        if (updateDTO.getParentId() != null) {
            existing.setParentId(updateDTO.getParentId());
        }
        if (updateDTO.getDescription() != null) {
            existing.setDescription(updateDTO.getDescription());
        }
        if (updateDTO.getSortOrder() != null) {
            existing.setSortOrder(updateDTO.getSortOrder());
        }
        if (updateDTO.getStatus() != null) {
            existing.setStatus(updateDTO.getStatus());
        }
        existing.setUpdateTime(LocalDateTime.now());

        materialCategoryMapper.updateById(existing);
        return getCategoryById(categoryId);
    }

    @Override
    public void deleteCategory(Long categoryId) throws IllegalArgumentException {
        MaterialCategory existing = materialCategoryMapper.selectById(categoryId);
        if (existing == null) {
            throw new IllegalArgumentException("分类不存在: " + categoryId);
        }

        // 校验：有子分类时禁止删除
        int childrenCount = materialCategoryMapper.countChildrenByParentId(categoryId);
        if (childrenCount > 0) {
            throw new IllegalArgumentException("存在 " + childrenCount + " 个子分类，请先删除子分类");
        }

        // 校验：被商品档案引用时禁止删除
        List<Long> referencedIds = materialCategoryMapper.findReferencedCategoryIds(Collections.singletonList(categoryId));
        if (!referencedIds.isEmpty()) {
            throw new IllegalArgumentException("该分类被商品档案引用，无法删除（请先迁移或删除相关商品档案）");
        }

        materialCategoryMapper.deleteById(categoryId);
    }

    @Override
    public void updateStatus(Long categoryId, Integer status) throws IllegalArgumentException {
        if (status == null || (status != 0 && status != 1)) {
            throw new IllegalArgumentException("无效的状态值: " + status + "（仅支持 0/1）");
        }
        MaterialCategory existing = materialCategoryMapper.selectById(categoryId);
        if (existing == null) {
            throw new IllegalArgumentException("分类不存在: " + categoryId);
        }
        existing.setStatus(status);
        existing.setUpdateTime(LocalDateTime.now());
        materialCategoryMapper.updateById(existing);
    }

    @Override
    public List<MaterialCategoryVO> getEnabledList() {
        LambdaQueryWrapper<MaterialCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaterialCategory::getStatus, 1)
                .orderByAsc(MaterialCategory::getSortOrder)
                .orderByAsc(MaterialCategory::getCategoryName);
        List<MaterialCategory> list = materialCategoryMapper.selectList(wrapper);
        return convertToVOList(list);
    }

    @Override
    public List<MaterialCategoryVO> getAllList() {
        LambdaQueryWrapper<MaterialCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(MaterialCategory::getSortOrder)
                .orderByAsc(MaterialCategory::getCategoryName);
        List<MaterialCategory> list = materialCategoryMapper.selectList(wrapper);
        return convertToVOList(list);
    }

    // ==================== 内部辅助方法 ====================

    /**
     * 检查 candidateId 是否是 ancestorId 的子孙（递归向上查 parent_id 链）
     * 如果 candidate 的某个祖先等于 ancestor，则返回 true
     */
    private boolean isDescendant(Long candidateId, Long ancestorId) {
        Long currentParentId = candidateId;
        int maxDepth = 20; // 防止循环引用导致死循环
        while (currentParentId != null && maxDepth-- > 0) {
            MaterialCategory current = materialCategoryMapper.selectById(currentParentId);
            if (current == null || current.getParentId() == null) {
                return false;
            }
            if (current.getParentId().equals(ancestorId)) {
                return true;
            }
            currentParentId = current.getParentId();
        }
        return false;
    }

    /**
     * 批量将 Entity 列表转为 VO 列表（自动填充 parentName + materialCount）
     */
    private List<MaterialCategoryVO> convertToVOList(List<MaterialCategory> categories) {
        if (categories == null || categories.isEmpty()) {
            return Collections.emptyList();
        }

        // 收集父分类ID
        Set<Long> parentIds = categories.stream()
                .map(MaterialCategory::getParentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 收集所有分类ID
        List<Long> categoryIds = categories.stream()
                .map(MaterialCategory::getCategoryId)
                .collect(Collectors.toList());

        // 批量查询父分类名（避免 N+1）
        Map<Long, String> parentNameMap = new HashMap<>();
        if (!parentIds.isEmpty()) {
            List<MaterialCategory> parents = materialCategoryMapper.selectBatchIds(parentIds);
            for (MaterialCategory p : parents) {
                parentNameMap.put(p.getCategoryId(), p.getCategoryName());
            }
        }

        // 批量统计商品数量（避免 N+1）
        Map<Long, Integer> materialCountMap = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            List<Map<String, Object>> counts = materialCategoryMapper.countMaterialsByCategoryIds(categoryIds);
            if (counts != null) {
                for (Map<String, Object> row : counts) {
                    Object idVal = row.get("categoryId");
                    Object countVal = row.get("materialCount");
                    if (idVal != null && countVal != null) {
                        materialCountMap.put(
                                ((Number) idVal).longValue(),
                                ((Number) countVal).intValue()
                        );
                    }
                }
            }
        }

        // 转换为 VO
        return categories.stream()
                .map(c -> convertToVO(c, parentNameMap, materialCountMap))
                .collect(Collectors.toList());
    }

    /**
     * 单个 Entity → VO 转换
     */
    private MaterialCategoryVO convertToVO(MaterialCategory category,
                                           Map<Long, String> parentNameMap,
                                           Map<Long, Integer> materialCountMap) {
        MaterialCategoryVO vo = new MaterialCategoryVO();
        vo.setCategoryId(category.getCategoryId());
        vo.setCategoryCode(category.getCategoryCode());
        vo.setCategoryName(category.getCategoryName());
        vo.setParentId(category.getParentId());
        vo.setRemark(category.getDescription()); // 前端字段名 remark，后端字段 description
        vo.setSortOrder(category.getSortOrder());
        vo.setStatus(category.getStatus());
        vo.setStatusName(statusToName(category.getStatus()));
        vo.setCreateTime(category.getCreateTime());
        vo.setUpdateTime(category.getUpdateTime());

        // 填充父分类名
        if (category.getParentId() != null) {
            vo.setParentName(parentNameMap.get(category.getParentId()));
        }

        // 填充商品数量
        vo.setMaterialCount(materialCountMap.getOrDefault(category.getCategoryId(), 0));

        return vo;
    }

    /**
     * 状态码 → 状态名
     */
    private String statusToName(Integer status) {
        if (status == null) {
            return "未知";
        }
        return status == 1 ? "启用" : "停用";
    }
}
