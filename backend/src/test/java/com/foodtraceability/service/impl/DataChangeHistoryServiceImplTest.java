package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.DataChangeHistory;
import com.foodtraceability.mapper.DataChangeHistoryMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DataChangeHistoryServiceImpl 单元测试（数据变更历史：按记录查、按表查、写入）。
 *
 * <p>两个查询都必须「按操作时间倒序」，否则变更追溯会把新旧顺序颠倒。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DataChangeHistoryServiceImplTest {

    @Mock
    private DataChangeHistoryMapper dataChangeHistoryMapper;

    @InjectMocks
    private DataChangeHistoryServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, DataChangeHistory.class);
    }

    @BeforeEach
    void injectBaseMapper() throws Exception {
        Field field = ServiceImpl.class.getDeclaredField("baseMapper");
        field.setAccessible(true);
        field.set(service, dataChangeHistoryMapper);
    }

    private static DataChangeHistory history(String tableName, String recordId) {
        DataChangeHistory h = new DataChangeHistory();
        h.setHistoryId(1L);
        h.setTableName(tableName);
        h.setRecordId(recordId);
        h.setChangeType("UPDATE");
        h.setOperationTime(LocalDateTime.of(2026, 10, 23, 10, 0));
        return h;
    }

    @Test
    @DisplayName("getHistoryByRecordId：record_id + table_name 双条件，按 operation_time 倒序")
    void getHistoryByRecordId_filtersAndDesc() {
        when(dataChangeHistoryMapper.selectList(any()))
                .thenReturn(List.of(history("orders", "O1")));

        List<DataChangeHistory> result = service.getHistoryByRecordId("O1", "orders");

        assertEquals(1, result.size());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<DataChangeHistory>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(dataChangeHistoryMapper).selectList(captor.capture());
        String sql = captor.getValue().getCustomSqlSegment().toLowerCase();
        assertTrue(sql.contains("record_id"), "必须按 record_id 过滤，实际: " + sql);
        assertTrue(sql.contains("table_name"), "必须按 table_name 过滤，实际: " + sql);
        assertTrue(sql.contains("order by") && sql.contains("desc"), "必须按时间倒序，实际: " + sql);
    }

    @Test
    @DisplayName("getHistoryByRecordId：无记录返回空列表")
    void getHistoryByRecordId_empty() {
        when(dataChangeHistoryMapper.selectList(any())).thenReturn(new ArrayList<>());
        assertTrue(service.getHistoryByRecordId("NOPE", "orders").isEmpty());
    }

    @Test
    @DisplayName("getHistoryByTableName：只按 table_name 过滤并按时间倒序")
    void getHistoryByTableName_filtersAndDesc() {
        when(dataChangeHistoryMapper.selectList(any()))
                .thenReturn(List.of(history("orders", "O1"), history("orders", "O2")));

        List<DataChangeHistory> result = service.getHistoryByTableName("orders");

        assertEquals(2, result.size());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<DataChangeHistory>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(dataChangeHistoryMapper).selectList(captor.capture());
        String sql = captor.getValue().getCustomSqlSegment().toLowerCase();
        assertTrue(sql.contains("table_name"), "必须按 table_name 过滤，实际: " + sql);
        assertFalse(sql.contains("record_id"), "不应额外带 record_id 条件，实际: " + sql);
        assertTrue(sql.contains("desc"), "必须倒序，实际: " + sql);
    }

    @Test
    @DisplayName("saveHistory：写库成功返回 true")
    void saveHistory_success() {
        when(dataChangeHistoryMapper.insert(any(DataChangeHistory.class))).thenReturn(1);
        assertTrue(service.saveHistory(history("orders", "O1")));
        verify(dataChangeHistoryMapper).insert(any(DataChangeHistory.class));
    }

    @Test
    @DisplayName("saveHistory：写库影响 0 行时如实返回 false")
    void saveHistory_noRows_returnsFalse() {
        when(dataChangeHistoryMapper.insert(any(DataChangeHistory.class))).thenReturn(0);
        assertFalse(service.saveHistory(history("orders", "O1")));
    }
}
