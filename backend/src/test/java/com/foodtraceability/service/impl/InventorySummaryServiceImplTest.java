package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.InventorySummary;
import com.foodtraceability.mapper.InventorySummaryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link InventorySummaryServiceImpl} 纯 Mockito 单元测试（不连数据库）。
 * 仅有一层分页委托，断言分页对象与关键字原样透传。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventorySummaryServiceImplTest {

    @Mock
    private InventorySummaryMapper inventorySummaryMapper;

    @InjectMocks
    private InventorySummaryServiceImpl service;

    @Test
    @DisplayName("getSummaryPage：分页对象与商品名关键字原样委托 mapper")
    void getSummaryPage_delegates() {
        Page<InventorySummary> page = new Page<>(2, 20);
        when(inventorySummaryMapper.selectSummaryPage(page, "牛肉")).thenReturn(page);

        IPage<InventorySummary> result = service.getSummaryPage(page, "牛肉");

        assertSame(page, result);
        verify(inventorySummaryMapper).selectSummaryPage(page, "牛肉");
    }

    @Test
    @DisplayName("getSummaryPage：关键字为 null 时也如实透传（不做二次处理）")
    void getSummaryPage_nullKeyword_passesThrough() {
        Page<InventorySummary> page = new Page<>(1, 10);
        when(inventorySummaryMapper.selectSummaryPage(page, null)).thenReturn(page);

        assertSame(page, service.getSummaryPage(page, null));
        verify(inventorySummaryMapper).selectSummaryPage(page, null);
    }
}
