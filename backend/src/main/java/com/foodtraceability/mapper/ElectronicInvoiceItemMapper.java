package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.ElectronicInvoiceItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 发票明细行项目Mapper接口
 */
@Mapper
public interface ElectronicInvoiceItemMapper extends BaseMapper<ElectronicInvoiceItem> {
    
    /**
     * 根据发票ID查询明细列表
     */
    List<ElectronicInvoiceItem> selectByInvoiceId(@Param("invoiceId") Long invoiceId);
    
    /**
     * 根据发票ID删除明细
     */
    int deleteByInvoiceId(@Param("invoiceId") Long invoiceId);
}
