package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.AssetCategoryNew;

import java.util.List;

/**
 * 资产分类服务接口
 */
public interface AssetCategoryNewService extends IService<AssetCategoryNew> {

    /**
     * 获取分类树形结构
     * @return 分类树形列表
     */
    List<AssetCategoryNew> getCategoryTree();

    /**
     * 根据父级ID获取子分类
     * @param parentId 父级ID
     * @return 子分类列表
     */
    List<AssetCategoryNew> getChildrenByParentId(Long parentId);

    /**
     * 检查分类编码是否可用
     * @param categoryCode 分类编码
     * @param excludeId 排除的分类ID
     * @return true-可用 false-已存在
     */
    boolean isCodeAvailable(String categoryCode, Long excludeId);
}
