package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.DishComboNew;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 套餐Mapper
 */
@Mapper
public interface DishComboNewMapper extends BaseMapper<DishComboNew> {

    /**
     * 根据套餐编码查询
     */
    @Select("SELECT * FROM dish_combos WHERE combo_code = #{comboCode} AND deleted = 0")
    DishComboNew selectByComboCode(@Param("comboCode") String comboCode);

    /**
     * 查询在售套餐列表
     */
    @Select("SELECT * FROM dish_combos WHERE status = 1 AND deleted = 0 ORDER BY sort_order ASC, combo_id DESC")
    java.util.List<DishComboNew> selectOnSaleCombos();
}
