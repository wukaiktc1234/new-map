package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.SysPrintTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 打印模板Mapper接口
 */
@Mapper
public interface SysPrintTemplateMapper extends BaseMapper<SysPrintTemplate> {

    /**
     * 根据门店ID查询模板
     */
    @Select("SELECT * FROM sys_print_templates WHERE store_id = #{storeId} OR store_id IS NULL AND is_enabled = 1")
    List<SysPrintTemplate> selectByStoreId(@Param("storeId") Long storeId);

    /**
     * 根据模板类型查询
     */
    @Select("SELECT * FROM sys_print_templates WHERE template_type = #{templateType} AND is_enabled = 1")
    List<SysPrintTemplate> selectByType(@Param("templateType") String templateType);

    /**
     * 查询默认模板
     */
    @Select("SELECT * FROM sys_print_templates WHERE template_type = #{templateType} AND is_default = 1 AND is_enabled = 1 LIMIT 1")
    SysPrintTemplate selectDefaultTemplate(@Param("templateType") String templateType);
}
