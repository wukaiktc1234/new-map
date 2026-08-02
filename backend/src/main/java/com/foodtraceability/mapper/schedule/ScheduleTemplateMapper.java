package com.foodtraceability.mapper.schedule;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.schedule.ScheduleTemplate;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 排班模板Mapper接口
 */
@Mapper
public interface ScheduleTemplateMapper extends BaseMapper<ScheduleTemplate> {

    /**
     * 查询门店的所有模板
     * @param storeId 门店ID
     * @return 模板列表
     */
    List<ScheduleTemplate> selectByStoreId(Long storeId);

    /**
     * 查询门店的启用模板
     * @param storeId 门店ID
     * @return 启用的模板列表
     */
    List<ScheduleTemplate> selectActiveByStoreId(Long storeId);

    /**
     * 查询门店的默认模板
     * @param storeId 门店ID
     * @return 默认模板(每个门店仅一个)
     */
    ScheduleTemplate selectDefaultByStoreId(Long storeId);

    /**
     * 更新模板使用次数(+1)
     * @param templateId 模板ID
     * @return 影响行数
     */
    int incrementUseCount(String templateId);
}
