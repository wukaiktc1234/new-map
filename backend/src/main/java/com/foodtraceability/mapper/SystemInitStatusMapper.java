package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.SystemInitStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 系统初始化状态Mapper接口
 * 提供系统初始化状态的数据库操作方法
 */
@Mapper
public interface SystemInitStatusMapper extends BaseMapper<SystemInitStatus> {

    /**
     * 获取当前初始化状态
     * @return 初始化状态
     */
    @Select("SELECT * FROM system_init_status ORDER BY id DESC LIMIT 1")
    SystemInitStatus selectLatest();

    /**
     * 更新步骤状态
     * @param step 当前步骤
     * @param isCompleted 是否完成
     * @return 更新数量
     */
    @Update("UPDATE system_init_status SET step = #{step}, is_completed = #{isCompleted}, updated_at = NOW() WHERE id = (SELECT id FROM (SELECT id FROM system_init_status ORDER BY id DESC LIMIT 1) AS tmp)")
    int updateStepStatus(@Param("step") String step, @Param("isCompleted") Boolean isCompleted);

    /**
     * 检查系统是否已初始化完成
     * @return 是否已初始化
     */
    @Select("SELECT COUNT(*) > 0 FROM system_init_status WHERE is_completed = true")
    boolean isSystemInitialized();
}
