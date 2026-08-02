package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.MaterialCategory;
import com.foodtraceability.entity.MaterialTemplate;
import com.foodtraceability.mapper.MaterialCategoryMapper;
import com.foodtraceability.mapper.MaterialTemplateMapper;
import com.foodtraceability.service.MaterialTemplateService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MaterialTemplateServiceImpl extends ServiceImpl<MaterialTemplateMapper, MaterialTemplate> implements MaterialTemplateService {


    public MaterialTemplateServiceImpl(MaterialCategoryMapper materialCategoryMapper) {
        this.materialCategoryMapper = materialCategoryMapper;
    }

    private final MaterialCategoryMapper materialCategoryMapper;

    @Override
    public List<MaterialTemplate> getByCategory(String category) {
        LambdaQueryWrapper<MaterialTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaterialTemplate::getCategory, category)
               .eq(MaterialTemplate::getStatus, "active")
               .orderByAsc(MaterialTemplate::getMaterialName);
        return list(wrapper);
    }

    @Override
    public List<MaterialTemplate> searchByName(String name) {
        LambdaQueryWrapper<MaterialTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(MaterialTemplate::getMaterialName, name)
               .eq(MaterialTemplate::getStatus, "active")
               .orderByAsc(MaterialTemplate::getMaterialName);
        return list(wrapper);
    }

    @Override
    public List<String> getAllCategories() {
        LambdaQueryWrapper<MaterialCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaterialCategory::getStatus, 1)
               .orderByAsc(MaterialCategory::getSortOrder);
        return materialCategoryMapper.selectList(wrapper).stream()
                .map(MaterialCategory::getCategoryName)
                .collect(Collectors.toList());
    }

    @Override
    public MaterialTemplate getByBarcode(String barcode) {
        LambdaQueryWrapper<MaterialTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaterialTemplate::getBarcode, barcode)
               .eq(MaterialTemplate::getStatus, "active");
        return getOne(wrapper);
    }
}
