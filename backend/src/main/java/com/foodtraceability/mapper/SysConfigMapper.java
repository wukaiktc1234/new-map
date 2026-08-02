package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.SysConfig;
import com.foodtraceability.entity.SysConfigHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> {

    IPage<SysConfig> selectConfigPage(
        Page<SysConfig> page,
        @Param("configKey") String configKey,
        @Param("configName") String configName,
        @Param("configGroup") String configGroup,
        @Param("isEnabled") Integer isEnabled,
        @Param("isSensitive") Integer isSensitive
    );

    List<SysConfig> selectByGroup(@Param("configGroup") String configGroup);
}
