package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.AssetCategoryNew;
import com.foodtraceability.mapper.AssetCategoryNewMapper;
import com.foodtraceability.service.AssetCategoryNewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 资产分类服务实现类
 */
@Service
public class AssetCategoryNewServiceImpl extends ServiceImpl<AssetCategoryNewMapper, AssetCategoryNew>
        implements AssetCategoryNewService {

    private static final Logger log = LoggerFactory.getLogger(AssetCategoryNewServiceImpl.class);

    private final AssetCategoryNewMapper assetCategoryNewMapper;

    public AssetCategoryNewServiceImpl(AssetCategoryNewMapper assetCategoryNewMapper) {
        this.assetCategoryNewMapper = assetCategoryNewMapper;
    }

    @Override
    public List<AssetCategoryNew> getCategoryTree() {
        // 获取所有顶级分类
        List<AssetCategoryNew> topCategories = assetCategoryNewMapper.selectTopLevelCategories();
        // 为每个顶级分类递归加载子分类
        for (AssetCategoryNew category : topCategories) {
            loadChildren(category);
        }
        return topCategories;
    }

    @Override
    public List<AssetCategoryNew> getChildrenByParentId(Long parentId) {
        return assetCategoryNewMapper.selectByParentId(parentId);
    }

    @Override
    public boolean isCodeAvailable(String categoryCode, Long excludeId) {
        int count = assetCategoryNewMapper.countByCode(categoryCode, excludeId);
        return count == 0;
    }

    /**
     * 递归加载子分类
     * @param parent 父分类对象
     */
    private void loadChildren(AssetCategoryNew parent) {
        List<AssetCategoryNew> children = assetCategoryNewMapper.selectByParentId(parent.getCategoryId());
        if (children != null && !children.isEmpty()) {
            for (AssetCategoryNew child : children) {
                loadChildren(child); // 递归加载
            }
            // 注意：如果需要在实体中维护子节点列表，可以在这里设置
            // 但为了避免循环引用，通常在VO层处理树形结构
        }
    }
}
