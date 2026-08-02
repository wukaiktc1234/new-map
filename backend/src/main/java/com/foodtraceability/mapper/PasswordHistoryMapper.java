package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.PasswordHistory;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 密码历史Mapper接口
 */
public interface PasswordHistoryMapper extends BaseMapper<PasswordHistory> {

    /**
     * 查询用户最近的N条密码历史
     */
    @Select("SELECT * FROM password_history WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{limit}")
    List<PasswordHistory> findRecentByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 查询用户的所有密码历史
     */
    @Select("SELECT * FROM password_history WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<PasswordHistory> findAllByUserId(@Param("userId") Long userId);

    /**
     * 删除用户旧的密码历史（保留最近N条）
     */
    @Select("DELETE FROM password_history WHERE user_id = #{userId} AND id NOT IN (" +
            "SELECT id FROM (SELECT id FROM password_history WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{keepCount}) tmp" +
            ")")
    int deleteOldHistory(@Param("userId") Long userId, @Param("keepCount") int keepCount);
}