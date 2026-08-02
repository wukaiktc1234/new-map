package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.VoucherIdempotent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 凭证幂等控制Mapper
 * 用于操作voucher_idempotent表，提供幂等性检查和记录管理功能
 * 防止重复生成相同业务键的凭证
 * @author example
 * @since 2026-04-04
 */
@Mapper
public interface VoucherIdempotentMapper extends BaseMapper<VoucherIdempotent> {
    
    // 继承BaseMapper后自动具备CRUD基础能力
    // 可根据需要添加自定义查询方法
}
