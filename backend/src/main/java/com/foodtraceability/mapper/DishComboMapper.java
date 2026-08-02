package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.DishCombo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface DishComboMapper extends BaseMapper<DishCombo> {

    /**
     * 分页查询套餐
     * @param page 分页对象
     * @param params 查询参数
     * @return 分页结果
     */
    Page<DishCombo> selectComboPage(Page<DishCombo> page, @Param("params") Map<String, Object> params);

    /**
     * 根据状态查询套餐
     * @param status 状态
     * @return 套餐列表
     */
    List<DishCombo> selectByStatus(@Param("status") String status);

    /**
     * 批量更新套餐状态
     * @param ids 套餐ID列表
     * @param status 状态
     * @return 影响行数
     */
    int batchUpdateStatus(@Param("ids") List<Long> ids, @Param("status") String status);

    /**
     * 查询套餐数量
     * @param params 查询参数
     * @return 套餐数量
     */
    Long selectComboCount(@Param("params") Map<String, Object> params);
}
