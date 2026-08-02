package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.EnterpriseConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 企业配置Mapper接口
 * 提供企业配置的数据库操作方法
 */
@Mapper
public interface EnterpriseConfigMapper extends BaseMapper<EnterpriseConfig> {

    /**
     * 获取企业配置
     * @return 企业配置
     */
    @Select("SELECT * FROM enterprise_config ORDER BY id DESC LIMIT 1")
    EnterpriseConfig selectLatest();

    /**
     * 更新初始化状态
     * @param initCompleted 是否完成初始化
     * @return 更新数量
     */
    @Update("UPDATE enterprise_config SET init_completed = #{initCompleted}, init_completed_at = NOW(), updated_at = NOW() WHERE id = (SELECT id FROM (SELECT id FROM enterprise_config ORDER BY id DESC LIMIT 1) AS tmp)")
    int updateInitStatus(@Param("initCompleted") Boolean initCompleted);

    /**
     * 检查是否已初始化
     * @return 是否已初始化
     */
    @Select("SELECT COUNT(*) > 0 FROM enterprise_config WHERE init_completed = true")
    boolean isInitialized();

    /**
     * 更新企业信息
     * @param enterpriseName 企业名称
     * @param enterpriseType 企业类型
     * @param scale 企业规模
     * @return 更新数量
     */
    @Update("UPDATE enterprise_config SET enterprise_name = #{enterpriseName}, enterprise_type = #{enterpriseType}, scale = #{scale}, updated_at = NOW() WHERE id = (SELECT id FROM (SELECT id FROM enterprise_config ORDER BY id DESC LIMIT 1) AS tmp)")
    int updateEnterpriseInfo(@Param("enterpriseName") String enterpriseName, @Param("enterpriseType") String enterpriseType, @Param("scale") String scale);
}
