package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.Product;

/**
 * 产品实体服务接口
 * 用于管理产品表(product)的业务逻辑
 */
public interface ProductEntityService extends IService<Product> {
    
    /**
     * 根据产品编码获取产品
     * @param code 产品编码
     * @return 产品实体
     */
    Product getByCode(String code);
    
    /**
     * 更新产品成本价
     * @param id 产品ID
     * @param costPrice 成本价
     * @return 是否成功
     */
    boolean updateCostPrice(Long id, java.math.BigDecimal costPrice);
    
    /**
     * 更新产品供应商信息
     * @param id 产品ID
     * @param supplierId 供应商ID
     * @param supplierName 供应商名称
     * @return 是否成功
     */
    boolean updateSupplierInfo(Long id, Long supplierId, String supplierName);
}
