package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.MaterialTemplate;

import java.util.List;

public interface MaterialTemplateService extends IService<MaterialTemplate> {
    
    List<MaterialTemplate> getByCategory(String category);
    
    List<MaterialTemplate> searchByName(String name);
    
    List<String> getAllCategories();
    
    MaterialTemplate getByBarcode(String barcode);
}
