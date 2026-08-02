package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.NotificationUserPreference;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NotificationUserPreferenceMapper extends BaseMapper<NotificationUserPreference> {

    List<NotificationUserPreference> selectByUserId(@Param("userId") Long userId);

    NotificationUserPreference selectByUserAndTypeAndChannel(@Param("userId") Long userId,
                                                              @Param("notificationType") String notificationType,
                                                              @Param("channel") String channel);
}
