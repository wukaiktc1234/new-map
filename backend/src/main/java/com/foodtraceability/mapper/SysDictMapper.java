package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.SysDict;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 字典类型Mapper接口
 */
@Mapper
public interface SysDictMapper extends BaseMapper<SysDict> {

    /**
     * 分页查询字典类型列表
     * @param page 分页参数
     * @param dictName 字典名称（模糊查询）
     * @param dictCode 字典编码（模糊查询）
     * @param dictGroup 字典分组
     * @param status 状态
     * @return 分页结果
     */
    IPage<SysDict> selectDictPage(
        Page<SysDict> page,
        @Param("dictName") String dictName,
        @Param("dictCode") String dictCode,
        @Param("dictGroup") String dictGroup,
        @Param("status") Integer status
    );
}
