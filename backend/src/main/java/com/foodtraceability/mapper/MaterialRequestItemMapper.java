package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.MaterialRequestItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 物资需求提报明细 Mapper
 */
@Mapper
public interface MaterialRequestItemMapper extends BaseMapper<MaterialRequestItem> {

    /**
     * 根据提报ID查询明细列表（按 item_id 升序）
     * @param requestId 提报ID
     * @return 明细列表
     */
    @Select("SELECT item_id, request_id, material_id, material_name, specification, quantity, unit, " +
            "estimated_price, subtotal_amount, remark, create_time, update_time, deleted " +
            "FROM material_request_item " +
            "WHERE deleted = 0 AND request_id = #{requestId} " +
            "ORDER BY item_id ASC")
    List<MaterialRequestItem> selectByRequestId(@Param("requestId") Long requestId);
}
