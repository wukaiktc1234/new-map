package com.foodtraceability.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis Plus 元数据处理器，用于自动填充创建时间、更新时间、创建人、更新人等字段
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        // 部分实体（如 pos_shifts / PosShift）把列 create_time/update_time 映射到 createdAt/updatedAt 属性，
        // 原实现只填 createTime/updateTime → 这些实体插入时写 NULL，撞 NOT NULL 约束
        // （2026-10-23 交班复核发现：补建班次 500）。strictInsertFill 对不存在的属性自动跳过，安全。
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "createdBy", Long.class, 1L);
        this.strictInsertFill(metaObject, "updatedBy", Long.class, 1L);
        this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
        this.strictInsertFill(metaObject, "version", Integer.class, 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "updatedBy", Long.class, 1L);
    }
}
