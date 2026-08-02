package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.Appeal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 申诉 Mapper 接口
 */
@Mapper
public interface AppealMapper extends BaseMapper<Appeal> {

    /**
     * 查询申诉详情（含基本信息）
     *
     * @param appealId 申诉ID
     * @return 申诉实体
     */
    @Select("SELECT * FROM appeals WHERE appeal_id = #{appealId} AND deleted = 0")
    Appeal selectDetailById(@Param("appealId") String appealId);
}
