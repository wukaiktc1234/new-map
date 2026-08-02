package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.PermissionTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 权限模板Mapper接口
 * 提供权限模板的数据库操作方法
 */
@Mapper
public interface PermissionTemplateMapper extends BaseMapper<PermissionTemplate> {

    /**
     * 根据模板编码查询模板
     * @param code 模板编码
     * @return 权限模板
     */
    @Select("SELECT * FROM permission_templates WHERE code = #{code} AND status = 1")
    PermissionTemplate selectByCode(@Param("code") String code);

    /**
     * 查询所有启用的系统模板
     * @return 权限模板列表
     */
    @Select("SELECT * FROM permission_templates WHERE is_system = true AND status = 1 ORDER BY id")
    List<PermissionTemplate> selectSystemTemplates();

    /**
     * 根据企业类型和规模查询匹配的模板
     * @param enterpriseType 企业类型
     * @param scaleRange 规模范围
     * @return 权限模板列表
     */
    @Select("SELECT * FROM permission_templates WHERE status = 1 AND (enterprise_type = #{enterpriseType} OR enterprise_type = 'all') AND (scale_range = #{scaleRange} OR scale_range = 'all') ORDER BY id")
    List<PermissionTemplate> selectByEnterpriseTypeAndScale(@Param("enterpriseType") String enterpriseType, @Param("scaleRange") String scaleRange);

    /**
     * 查询所有启用的模板
     * @return 权限模板列表
     */
    @Select("SELECT * FROM permission_templates WHERE status = 1 ORDER BY id")
    List<PermissionTemplate> selectAllEnabled();
}
