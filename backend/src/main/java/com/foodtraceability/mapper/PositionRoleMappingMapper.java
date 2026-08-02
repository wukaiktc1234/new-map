package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.PositionRoleMapping;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 职位-角色映射Mapper接口
 */
@Mapper
public interface PositionRoleMappingMapper extends BaseMapper<PositionRoleMapping> {

    /**
     * 根据职位ID查询关联的角色映射
     * @param positionId 职位ID
     * @return 角色映射列表
     */
    @Select("SELECT prm.*, p.position_name, p.position_code, r.ROLE_NAME as role_name, r.ROLE_CODE as role_code " +
            "FROM position_role_mapping prm " +
            "LEFT JOIN positions p ON prm.position_id = p.id " +
            "LEFT JOIN roles r ON prm.role_id = r.ID " +
            "WHERE prm.position_id = #{positionId} AND prm.deleted = 0 AND prm.status = 1 " +
            "ORDER BY prm.is_primary DESC, prm.priority DESC")
    List<PositionRoleMapping> selectByPositionId(@Param("positionId") Long positionId);

    /**
     * 根据角色ID查询关联的职位映射
     * @param roleId 角色ID
     * @return 职位映射列表
     */
    @Select("SELECT prm.*, p.position_name, p.position_code, r.ROLE_NAME as role_name, r.ROLE_CODE as role_code " +
            "FROM position_role_mapping prm " +
            "LEFT JOIN positions p ON prm.position_id = p.id " +
            "LEFT JOIN roles r ON prm.role_id = r.ID " +
            "WHERE prm.role_id = #{roleId} AND prm.deleted = 0 AND prm.status = 1 " +
            "ORDER BY prm.is_primary DESC, prm.priority DESC")
    List<PositionRoleMapping> selectByRoleId(@Param("roleId") Long roleId);
}
