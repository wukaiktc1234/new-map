package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.SysDictItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 字典项明细Mapper接口
 */
@Mapper
public interface SysDictItemMapper extends BaseMapper<SysDictItem> {

    /**
     * 分页查询字典项列表
     * @param page 分页参数
     * @param dictId 字典类型ID
     * @param itemLabel 字典项标签（模糊查询）
     * @param status 状态
     * @return 分页结果
     */
    IPage<SysDictItem> selectItemPage(
        Page<SysDictItem> page,
        @Param("dictId") Long dictId,
        @Param("itemLabel") String itemLabel,
        @Param("status") Integer status
    );

    /**
     * 根据字典ID查询所有启用的字典项（按排序序号升序）
     * @param dictId 字典类型ID
     * @return 字典项列表
     */
    List<SysDictItem> selectEnabledItemsByDictId(@Param("dictId") Long dictId);

    /**
     * 根据字典编码查询所有启用的字典项
     * @param dictCode 字典编码
     * @return 字典项列表
     */
    List<SysDictItem> selectEnabledItemsByDictCode(@Param("dictCode") String dictCode);
}
