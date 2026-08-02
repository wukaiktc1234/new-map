package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.RoleStore;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色门店关联Mapper接口
 * 提供角色门店关联的数据库操作方法
 */
@Mapper
public interface RoleStoreMapper extends BaseMapper<RoleStore> {

    /**
     * 根据角色ID查询门店ID列表
     * @param roleId 角色ID
     * @return 门店ID列表
     */
    @Select("SELECT store_id FROM role_stores WHERE role_id = #{roleId}")
    List<String> selectStoreIdsByRoleId(@Param("roleId") String roleId);

    /**
     * 根据角色ID删除所有门店关联
     * @param roleId 角色ID
     * @return 删除数量
     */
    @Delete("DELETE FROM role_stores WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 批量插入角色门店关联
     * @param roleStores 角色门店关联列表
     * @return 插入数量
     */
    int batchInsert(@Param("list") List<RoleStore> roleStores);

    /**
     * 根据门店ID查询角色ID列表
     * @param storeId 门店ID
     * @return 角色ID列表
     */
    @Select("SELECT role_id FROM role_stores WHERE store_id = #{storeId}")
    List<String> selectRoleIdsByStoreId(@Param("storeId") String storeId);

    /**
     * 批量查询多个角色关联的门店ID列表（性能优化）
     * 使用IN查询替代循环查询，解决N+1问题
     *
     * @param roleIds 角色ID列表
     * @return 门店ID列表（可能包含重复，调用方需去重）
     */
    @Select("<script>" +
            "SELECT store_id FROM role_stores " +
            "WHERE role_id IN " +
            "<foreach item='roleId' collection='roleIds' open='(' separator=',' close=')'>" +
            "#{roleId}" +
            "</foreach>" +
            "</script>")
    List<String> selectStoreIdsByRoleIds(@Param("roleIds") List<String> roleIds);
}
