package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.PurchaseRequestItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 采购申请明细Mapper接口
 */
@Mapper
public interface PurchaseRequestItemMapper extends BaseMapper<PurchaseRequestItem> {
    
    /**
     * 根据申请ID查询明细列表
     * @param requestId 申请ID
     * @return 明细列表
     */
    @Select("SELECT * FROM purchase_request_item WHERE request_id = #{requestId} AND deleted = 0 ORDER BY create_time")
    List<PurchaseRequestItem> selectByRequestId(@Param("requestId") String requestId);
}
