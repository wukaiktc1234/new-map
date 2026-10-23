package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.OperationLogEntity;
import com.foodtraceability.mapper.OperationLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * OperationLogServiceImpl 单元测试（操作日志写入与条件查询）。
 *
 * <p>写入失败必须被吞掉（日志系统故障不能连带业务失败），但异常不得向上抛。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OperationLogServiceImplTest {

    @Mock
    private OperationLogMapper operationLogMapper;

    @InjectMocks
    private OperationLogServiceImpl service;

    @BeforeEach
    void injectBaseMapper() throws Exception {
        Field field = ServiceImpl.class.getDeclaredField("baseMapper");
        field.setAccessible(true);
        field.set(service, operationLogMapper);
    }

    private static OperationLogEntity log(String module) {
        OperationLogEntity e = new OperationLogEntity();
        e.setOperationModule(module);
        e.setOperationType("CREATE");
        return e;
    }

    @Test
    @DisplayName("logOperation：正常写入一条日志")
    void logOperation_inserts() {
        when(operationLogMapper.insert(any(OperationLogEntity.class))).thenReturn(1);

        service.logOperation(log("采购"));

        verify(operationLogMapper).insert(any(OperationLogEntity.class));
    }

    @Test
    @DisplayName("logOperation：写库异常被吞掉，调用方不受影响")
    void logOperation_swallowsException() {
        when(operationLogMapper.insert(any(OperationLogEntity.class)))
                .thenThrow(new RuntimeException("db down"));

        assertDoesNotThrow(() -> service.logOperation(log("库存")),
                "日志写入失败必须吞掉，不能反向影响业务流程");
    }

    @Test
    @DisplayName("queryLogs：五个条件原样下传 mapper")
    void queryLogs_passesAllFilters() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 23, 23, 59);
        when(operationLogMapper.selectByConditions("采购", "CREATE", "张三", start, end))
                .thenReturn(List.of(log("采购")));

        List<OperationLogEntity> result = service.queryLogs("采购", "CREATE", "张三", start, end);

        assertEquals(1, result.size());
        verify(operationLogMapper).selectByConditions("采购", "CREATE", "张三", start, end);
    }

    @Test
    @DisplayName("queryLogs：全空条件也照常下传（由 SQL 动态拼接处理）")
    void queryLogs_nullFilters() {
        when(operationLogMapper.selectByConditions(null, null, null, null, null)).thenReturn(List.of());

        assertTrue(service.queryLogs(null, null, null, null, null).isEmpty());
        verify(operationLogMapper).selectByConditions(null, null, null, null, null);
    }
}
