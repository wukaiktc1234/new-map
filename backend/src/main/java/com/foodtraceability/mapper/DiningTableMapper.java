package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.DiningTable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface DiningTableMapper extends BaseMapper<DiningTable> {
    
    @Select("SELECT * FROM dining_table WHERE status = #{status} ORDER BY table_number")
    List<DiningTable> findByStatus(String status);
    
    @Select("SELECT * FROM dining_table WHERE area = #{area} ORDER BY table_number")
    List<DiningTable> findByArea(String area);
    
    @Select("SELECT DISTINCT area FROM dining_table WHERE area IS NOT NULL")
    List<String> findAllAreas();
    
    @Select("SELECT * FROM dining_table WHERE table_number = #{tableNumber}")
    DiningTable findByTableNumber(String tableNumber);
}
