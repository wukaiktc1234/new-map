package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.EventOutbox;
import org.apache.ibatis.annotations.Mapper;

/**
 * 事件发件箱 Mapper
 * 用于事件发布器内部对 event_outbox 表的 CRUD 操作。
 *
 * @author foodtraceability
 * @since 2026-07-17
 */
@Mapper
public interface EventOutboxMapper extends BaseMapper<EventOutbox> {
}
