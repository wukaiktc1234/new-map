package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.UserPermissionOverride;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户权限覆盖 Mapper
 */
@Mapper
public interface UserPermissionOverrideMapper extends BaseMapper<UserPermissionOverride> {
}
