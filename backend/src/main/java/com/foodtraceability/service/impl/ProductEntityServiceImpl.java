package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.Product;
import com.foodtraceability.mapper.ProductMapper;
import com.foodtraceability.service.ProductEntityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 产品实体服务实现类
 * 实现产品表(product)的业务逻辑
 */
@Service
public class ProductEntityServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductEntityService {

    @Override
    public Product getByCode(String code) {
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("code", code);
        return this.getOne(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateCostPrice(Long id, java.math.BigDecimal costPrice) {
        Product product = this.getById(id);
        if (product == null) {
            return false;
        }
        product.setCostPrice(costPrice);
        product.setUpdatedAt(new Date());
        return this.updateById(product);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSupplierInfo(Long id, Long supplierId, String supplierName) {
        Product product = this.getById(id);
        if (product == null) {
            return false;
        }
        product.setSupplierId(supplierId);
        product.setSupplierName(supplierName);
        product.setUpdatedAt(new Date());
        return this.updateById(product);
    }
}
