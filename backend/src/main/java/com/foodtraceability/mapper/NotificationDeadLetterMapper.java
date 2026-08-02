package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.NotificationDeadLetter;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知死信 Mapper 接口
 *
 * <p>继承 MyBatis-Plus BaseMapper，提供通用 CRUD 方法。
 * 暂无自定义方法，使用通用方法即可满足需求。</p>
 *
 * <p>对应 plan.md 第 6.2.5 节和 T-004 任务。</p>
 */
@Mapper
public interface NotificationDeadLetterMapper extends BaseMapper<NotificationDeadLetter> {
    // 暂无自定义方法，使用 MyBatis-Plus 通用方法
}
