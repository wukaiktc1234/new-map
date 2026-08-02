package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.PurchasePlanItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 采购计划明细 Mapper
 */
@Mapper
public interface PurchasePlanItemMapper extends BaseMapper<PurchasePlanItem> {

    /**
     * 根据 plan_id 批量查询明细（按 item_id 升序）
     */
    @Select("SELECT item_id, plan_id, material_id, material_name, specification, quantity, unit, " +
            "estimated_price, is_temp_material, supplier_id, supplier_name, remark, create_time, update_time, deleted " +
            "FROM purchase_plan_item " +
            "WHERE deleted = 0 AND plan_id IN " +
            "<foreach collection='planIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "ORDER BY item_id ASC")
    List<PurchasePlanItem> findByPlanIds(@Param("planIds") List<Long> planIds);
}
