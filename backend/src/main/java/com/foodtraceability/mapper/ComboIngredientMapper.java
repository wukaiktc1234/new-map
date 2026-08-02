package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.ComboIngredient;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ComboIngredientMapper extends BaseMapper<ComboIngredient> {

    /**
     * 根据套餐ID查询成分
     * @param comboId 套餐ID
     * @return 成分列表
     */
    List<ComboIngredient> selectByComboId(@Param("comboId") Long comboId);

    /**
     * 批量插入成分
     * @param ingredients 成分列表
     * @return 影响行数
     */
    int batchInsert(@Param("ingredients") List<ComboIngredient> ingredients);

    /**
     * 批量删除成分
     * @param comboId 套餐ID
     * @return 影响行数
     */
    int deleteByComboId(@Param("comboId") Long comboId);

    /**
     * 根据套餐ID和菜品ID查询成分
     * @param comboId 套餐ID
     * @param foodId 菜品ID
     * @return 成分
     */
    ComboIngredient selectByComboIdAndFoodId(@Param("comboId") Long comboId, @Param("foodId") String foodId);
}
