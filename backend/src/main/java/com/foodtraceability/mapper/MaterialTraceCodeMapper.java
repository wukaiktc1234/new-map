package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.MaterialTraceCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 原料追溯码Mapper接口
 */
@Mapper
public interface MaterialTraceCodeMapper extends BaseMapper<MaterialTraceCode> {

    @Select("SELECT * FROM material_trace_code WHERE trace_code = #{traceCode} AND deleted = 0")
    MaterialTraceCode selectByTraceCode(@Param("traceCode") String traceCode);

    @Select("SELECT * FROM material_trace_code WHERE product_id = #{productId} AND deleted = 0 ORDER BY create_time DESC")
    List<MaterialTraceCode> selectByProductId(@Param("productId") Long productId);

    @Select("SELECT * FROM material_trace_code WHERE purchase_stockin_id = #{stockinId} AND deleted = 0")
    List<MaterialTraceCode> selectByStockinId(@Param("stockinId") Long stockinId);

    @Select("SELECT * FROM material_trace_code WHERE status = #{status} AND deleted = 0 ORDER BY create_time DESC")
    List<MaterialTraceCode> selectByStatus(@Param("status") String status);

    @Select("SELECT * FROM material_trace_code WHERE expiry_date <= CURRENT_DATE AND status = 'in_stock' AND deleted = 0")
    List<MaterialTraceCode> selectExpiredCodes();

    @Select("SELECT * FROM material_trace_code WHERE expiry_date <= CURRENT_DATE + CAST(#{days} AS INTEGER) * INTERVAL '1 day' AND expiry_date > CURRENT_DATE AND status = 'in_stock' AND deleted = 0")
    List<MaterialTraceCode> selectExpiringSoon(@Param("days") int days);

    @Select("SELECT COUNT(*) FROM material_trace_code WHERE status = #{status} AND deleted = 0")
    int countByStatus(@Param("status") String status);
}
