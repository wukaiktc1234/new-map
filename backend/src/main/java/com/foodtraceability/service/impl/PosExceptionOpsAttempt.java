package com.foodtraceability.service.impl;

import com.foodtraceability.entity.ExceptionOps;
import com.foodtraceability.mapper.ExceptionOpsMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 保存点写入器（Lead 2026-10-29 设计稿 §3.3）。
 *
 * <p><b>为什么必须是独立 Bean：</b>{@code this.insert(...)} 自调用不经过 Spring 代理，
 * {@code @Transactional(propagation=NESTED)} 会失效（本项目实测过同类问题）。</p>
 *
 * <p><b>为什么不用 INSERT ... ON CONFLICT：</b>唯一键冲突会让 PostgreSQL 把<b>外层事务</b>标记为
 * aborted（SQLState 25P02），后续任何 SQL 都报 current transaction is aborted，
 * 接口表现成 500 且数据半写入。保存点只回滚这一段，外层事务继续可用。</p>
 */
@Component
public class PosExceptionOpsAttempt {

    private final ExceptionOpsMapper mapper;

    public PosExceptionOpsAttempt(ExceptionOpsMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(propagation = Propagation.NESTED, rollbackFor = Exception.class)
    public void insertWithinSavepoint(ExceptionOps ops) {
        mapper.insert(ops);
    }
}
