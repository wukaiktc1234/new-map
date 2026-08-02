package com.foodtraceability.mapper.seal;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.seal.SealUsageLog;
import org.springframework.stereotype.Repository;

/**
 * 印章使用记录 Mapper 接口
 * 提供印章使用记录基础 CRUD 操作（由 MyBatis-Plus BaseMapper 提供）
 */
@Repository
public interface SealUsageLogMapper extends BaseMapper<SealUsageLog> {
}
