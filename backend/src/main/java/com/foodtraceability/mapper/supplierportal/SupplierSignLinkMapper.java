package com.foodtraceability.mapper.supplierportal;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.supplierportal.SupplierSignLink;
import org.springframework.stereotype.Repository;

/**
 * 供应商签署链接 Mapper 接口
 * 提供签署链接的基础 CRUD 能力
 */
@Repository
public interface SupplierSignLinkMapper extends BaseMapper<SupplierSignLink> {
}
