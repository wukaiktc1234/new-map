package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.Product;
import com.foodtraceability.mapper.ProductMapper;
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
import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

/**
 * ProductEntityServiceImpl 单元测试（product 表：按编码查、成本价更新、供应商信息更新）。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductEntityServiceImplTest {

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductEntityServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, Product.class);
    }

    @BeforeEach
    void injectBaseMapper() throws Exception {
        Field field = ServiceImpl.class.getDeclaredField("baseMapper");
        field.setAccessible(true);
        field.set(service, productMapper);
    }

    private static Product product(Long id) {
        Product p = new Product();
        p.setId(id);
        p.setCode("P001");
        p.setName("土豆");
        p.setCostPrice(new BigDecimal("3.50"));
        p.setUpdatedAt(new Date(0L));
        return p;
    }

    @Test
    @DisplayName("getByCode：按 code 等值查询并返回实体")
    void getByCode_delegates() {
        // ServiceImpl.getOne(wrapper) 调用的是 baseMapper.selectOne(wrapper, throwEx) 双参重载
        when(productMapper.selectOne(any(), anyBoolean())).thenReturn(product(1L));
        when(productMapper.selectList(any())).thenReturn(new java.util.ArrayList<>(java.util.List.of(product(1L))));

        Product result = service.getByCode("P001");

        assertNotNull(result);
        assertEquals(1L, result.getId());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<Product>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(productMapper).selectOne(captor.capture(), anyBoolean());
        assertTrue(captor.getValue().getCustomSqlSegment().toLowerCase().contains("code"),
                "必须按 code 过滤，实际: " + captor.getValue().getCustomSqlSegment());
    }

    @Test
    @DisplayName("getByCode：查不到返回 null")
    void getByCode_missing_returnsNull() {
        when(productMapper.selectOne(any(), anyBoolean())).thenReturn(null);
        when(productMapper.selectList(any())).thenReturn(new java.util.ArrayList<>());
        assertNull(service.getByCode("NOPE"));
    }

    @Test
    @DisplayName("updateCostPrice：产品不存在返回 false，不写库")
    void updateCostPrice_missing_false() {
        when(productMapper.selectById(404L)).thenReturn(null);

        assertFalse(service.updateCostPrice(404L, new BigDecimal("9.99")));
        verify(productMapper, never()).updateById(any(Product.class));
    }

    @Test
    @DisplayName("updateCostPrice：写入成本价并刷新 updatedAt")
    void updateCostPrice_setsCostAndTimestamp() {
        Product stored = product(1L);
        when(productMapper.selectById(1L)).thenReturn(stored);
        when(productMapper.updateById(any(Product.class))).thenReturn(1);

        assertTrue(service.updateCostPrice(1L, new BigDecimal("8.88")));

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).updateById(captor.capture());
        assertEquals(0, new BigDecimal("8.88").compareTo(captor.getValue().getCostPrice()));
        assertTrue(captor.getValue().getUpdatedAt().getTime() > 0L, "updatedAt 必须刷新");
        assertNotEquals(0L, captor.getValue().getUpdatedAt().getTime());
    }

    @Test
    @DisplayName("updateCostPrice：写库返回 0 时如实返回 false")
    void updateCostPrice_mapperRejects_returnsFalse() {
        when(productMapper.selectById(1L)).thenReturn(product(1L));
        when(productMapper.updateById(any(Product.class))).thenReturn(0);

        assertFalse(service.updateCostPrice(1L, BigDecimal.ONE));
    }

    @Test
    @DisplayName("updateSupplierInfo：产品不存在返回 false，不写库")
    void updateSupplierInfo_missing_false() {
        when(productMapper.selectById(404L)).thenReturn(null);

        assertFalse(service.updateSupplierInfo(404L, 9L, "供应商A"));
        verify(productMapper, never()).updateById(any(Product.class));
    }

    @Test
    @DisplayName("updateSupplierInfo：写入供应商 ID 与名称")
    void updateSupplierInfo_setsFields() {
        Product stored = product(1L);
        when(productMapper.selectById(1L)).thenReturn(stored);
        when(productMapper.updateById(any(Product.class))).thenReturn(1);

        assertTrue(service.updateSupplierInfo(1L, 9L, "供应商A"));

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).updateById(captor.capture());
        assertEquals(9L, captor.getValue().getSupplierId());
        assertEquals("供应商A", captor.getValue().getSupplierName());
    }
}
