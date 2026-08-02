package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.ElectronicInvoice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 电子发票Mapper接口
 */
@Mapper
public interface ElectronicInvoiceMapper extends BaseMapper<ElectronicInvoice> {
    
    /**
     * 根据凭证ID查询发票
     */
    ElectronicInvoice selectByVoucherId(@Param("voucherId") Long voucherId);
    
    /**
     * 根据发票代码和号码查询
     */
    ElectronicInvoice selectByInvoiceNo(
            @Param("invoiceCode") String invoiceCode,
            @Param("invoiceNo") String invoiceNo,
            @Param("tenantId") Long tenantId);
    
    /**
     * 根据发票代码和号码查询所有匹配记录（用于重复检测）
     */
    List<ElectronicInvoice> findByCodeAndNo(
            @Param("tenantId") Long tenantId,
            @Param("invoiceCode") String invoiceCode,
            @Param("invoiceNo") String invoiceNo);
}
