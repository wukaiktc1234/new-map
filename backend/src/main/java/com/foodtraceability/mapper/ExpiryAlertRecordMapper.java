package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.ExpiryAlertRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 临期预警记录Mapper接口
 * 依赖MyBatis-Plus的@TableLogic自动处理逻辑删除，查询无需手动添加deleted=0条件
 */
@Mapper
public interface ExpiryAlertRecordMapper extends BaseMapper<ExpiryAlertRecord> {
}
