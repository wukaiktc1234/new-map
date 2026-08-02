package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.ComboInventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface ComboInventoryMapper extends BaseMapper<ComboInventory> {

    @Select("SELECT * FROM combo_inventory WHERE combo_id = #{comboId} AND deleted = 0")
    List<ComboInventory> selectByComboId(@Param("comboId") Long comboId);

    @Select("SELECT COALESCE(SUM(ci.quantity * i.cost_price), 0) " +
            "FROM combo_inventory ci " +
            "LEFT JOIN inventory i ON ci.inventory_id = i.id " +
            "WHERE ci.combo_id = #{comboId} AND ci.deleted = 0")
    BigDecimal calculateComboCost(@Param("comboId") Long comboId);
}
