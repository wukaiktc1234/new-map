package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.dto.DictCreateDTO;
import com.foodtraceability.dto.DictItemCreateDTO;
import com.foodtraceability.dto.DictItemUpdateDTO;
import com.foodtraceability.dto.DictUpdateDTO;
import com.foodtraceability.entity.SysDict;
import com.foodtraceability.entity.SysDictItem;
import com.foodtraceability.mapper.SysDictItemMapper;
import com.foodtraceability.mapper.SysDictMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SysDictServiceImpl 单元测试类
 * 测试字典管理的核心业务逻辑
 *
 * 测试覆盖范围：
 * 1. 字典CRUD操作（创建、更新、删除、查询）
 * 2. 字典项CRUD操作
 * 3. 业务规则验证（编码唯一性、系统内置保护、默认值互斥）
 * 4. 启用/禁用功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("字典管理Service单元测试")
class SysDictServiceImplTest {

    @Mock
    private SysDictMapper sysDictMapper;

    @Mock
    private SysDictItemMapper sysDictItemMapper;

    private SysDictServiceImpl sysDictService;

    private static final String TEST_USER_ID = "test_user_001";
    private static final String TEST_USERNAME = "test_user";

    @BeforeEach
    void setUp() {
        sysDictService = new SysDictServiceImpl(sysDictMapper, sysDictItemMapper);
    }

    // ==================== 字典类型操作测试 ====================

    @Nested
    @DisplayName("字典创建操作")
    class CreateDictTests {

        @Test
        @DisplayName("正常创建字典 - 成功场景")
        void testCreateDict_success() {
            // 准备测试数据
            DictCreateDTO dto = new DictCreateDTO();
            dto.setDictName("用户状态");
            dto.setDictCode("user_status");
            dto.setDictGroup("system");
            dto.setDescription("用户账户状态");

            // Mock: 编码不存在
            when(sysDictMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            // Mock: 插入成功
            when(sysDictMapper.insert(any(SysDict.class))).thenReturn(1);

            // 执行测试
            SysDict result = sysDictService.createDict(dto, TEST_USER_ID, TEST_USERNAME);

            // 验证结果
            assertNotNull(result);
            assertEquals("用户状态", result.getDictName());
            assertEquals("user_status", result.getDictCode());
            assertEquals(1, result.getStatus()); // 默认启用
            assertEquals(0, result.getIsSystem()); // 默认非系统内置
            assertNotNull(result.getCreateTime());
            assertNotNull(result.getUpdateTime());

            // 验证Mapper调用
            verify(sysDictMapper).insert(any(SysDict.class));
        }

        @Test
        @DisplayName("重复编码 - 应抛出BusinessException")
        void testCreateDict_duplicateCode() {
            // 准备测试数据
            DictCreateDTO dto = new DictCreateDTO();
            dto.setDictName("用户状态");
            dto.setDictCode("user_status"); // 重复编码

            // Mock: 编码已存在
            when(sysDictMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            // 执行测试并验证异常
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                sysDictService.createDict(dto, TEST_USER_ID, TEST_USERNAME);
            });

            assertEquals(400, exception.getCode());
            assertTrue(exception.getMessage().contains("字典编码已存在"));

            // 验证不应该调用插入方法
            verify(sysDictMapper, never()).insert(any());
        }
    }

    @Nested
    @DisplayName("字典更新操作")
    class UpdateDictTests {

        @Test
        @DisplayName("更新系统内置字典的分类 - 应记录警告但不阻止")
        void testUpdateDict_systemProtected() {
            // 准备测试数据
            Long dictId = 1L;
            SysDict existingDict = new SysDict();
            existingDict.setDictId(dictId);
            existingDict.setDictName("用户状态");
            existingDict.setDictCode("user_status");
            existingDict.setIsSystem(1); // 系统内置
            existingDict.setDeleted(0);

            DictUpdateDTO dto = new DictUpdateDTO();
            dto.setDictName("用户状态(修改)");
            dto.setDictGroup("modified_group"); // 尝试修改分类

            // Mock: 字典存在
            when(sysDictMapper.selectById(dictId)).thenReturn(existingDict);
            when(sysDictMapper.updateById(any(SysDict.class))).thenReturn(1);

            // 执行测试
            boolean result = sysDictService.updateDict(dictId, dto, TEST_USER_ID, TEST_USERNAME);

            // 验证结果 - 应该成功（只是警告）
            assertTrue(result);
            verify(sysDictMapper).updateById(any(SysDict.class));
        }
    }

    @Nested
    @DisplayName("字典删除操作")
    class DeleteDictTests {

        @Test
        @DisplayName("删除字典时级联删除关联项")
        void testDeleteDict_cascadeDeleteItems() {
            // 准备测试数据
            Long dictId = 1L;
            SysDict dict = new SysDict();
            dict.setDictId(dictId);
            dict.setDictName("测试字典");
            dict.setIsSystem(0); // 非系统内置
            dict.setDeleted(0);

            // Mock: 字典存在且非系统内置
            when(sysDictMapper.selectById(dictId)).thenReturn(dict);
            // Mock: 有关联项
            when(sysDictItemMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L);
            // Mock: 删除成功
            when(sysDictItemMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(5);
            when(sysDictMapper.deleteById(dictId)).thenReturn(1);

            // 执行测试
            boolean result = sysDictService.deleteDict(dictId);

            // 验证结果
            assertTrue(result);

            // 验证级联删除被调用
            verify(sysDictItemMapper).delete(any(LambdaQueryWrapper.class));
            verify(sysDictMapper).deleteById(dictId);
        }

        @Test
        @DisplayName("删除系统内置字典 - 应抛出异常")
        void testDeleteDict_systemProtected() {
            Long dictId = 1L;
            SysDict dict = new SysDict();
            dict.setDictId(dictId);
            dict.setIsSystem(1); // 系统内置
            dict.setDeleted(0);

            // Mock
            when(sysDictMapper.selectById(dictId)).thenReturn(dict);

            // 执行并验证异常
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                sysDictService.deleteDict(dictId);
            });

            assertEquals(400, exception.getCode());
            assertTrue(exception.getMessage().contains("系统内置字典不允许删除"));

            // 不应该执行删除
            verify(sysDictMapper, never()).deleteById(anyLong());
        }
    }

    @Nested
    @DisplayName("启用/禁用操作")
    class EnableDisableTests {

        @Test
        @DisplayName("启用字典 - 成功")
        void testEnableDict_success() {
            Long dictId = 1L;
            SysDict dict = new SysDict();
            dict.setDictId(dictId);
            dict.setStatus(0); // 当前禁用
            dict.setDeleted(0);

            when(sysDictMapper.selectById(dictId)).thenReturn(dict);
            when(sysDictMapper.updateById(any(SysDict.class))).thenReturn(1);

            boolean result = sysDictService.enableDict(dictId);

            assertTrue(result);
            assertEquals(1, dict.getStatus()); // 状态已变为启用
        }

        @Test
        @DisplayName("禁用字典 - 同时禁用所有字典项")
        void testDisableDict_withItems() {
            Long dictId = 1L;
            SysDict dict = new SysDict();
            dict.setDictId(dictId);
            dict.setStatus(1); // 当前启用
            dict.setDeleted(0);

            // 模拟有启用的字典项
            SysDictItem item1 = new SysDictItem();
            item1.setItemId(1L);
            item1.setStatus(1);

            SysDictItem item2 = new SysDictItem();
            item2.setItemId(2L);
            item2.setStatus(1);

            List<SysDictItem> items = List.of(item1, item2);

            when(sysDictMapper.selectById(dictId)).thenReturn(dict);
            when(sysDictItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(items);
            when(sysDictItemMapper.updateById(any(SysDictItem.class))).thenReturn(1);
            when(sysDictMapper.updateById(any(SysDict.class))).thenReturn(1);

            boolean result = sysDictService.disableDict(dictId);

            assertTrue(result);
            assertEquals(0, dict.getStatus()); // 字典已禁用

            // 验证所有字典项都被禁用了
            verify(sysDictItemMapper, times(2)).updateById(any(SysDictItem.class));
        }
    }

    // ==================== 字典项操作测试 ====================

    @Nested
    @DisplayName("字典项创建操作")
    class CreateDictItemTests {

        @Test
        @DisplayName("设置默认项 - 清除其他默认标记")
        void testCreateDictItem_defaultMutualExclusion() {
            Long dictId = 1L;

            // 准备DTO - 设置为默认项
            DictItemCreateDTO dto = new DictItemCreateDTO();
            dto.setDictId(dictId);
            dto.setItemLabel("启用");
            dto.setItemValue("active");
            dto.setIsDefault(1); // 设置为默认项

            // Mock: 字典存在
            SysDict dict = new SysDict();
            dict.setDictId(dictId);
            dict.setDeleted(0);
            when(sysDictMapper.selectById(dictId)).thenReturn(dict);

            // Mock: 值不重复
            when(sysDictItemMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            // Mock: 插入成功
            when(sysDictItemMapper.insert(any(SysDictItem.class))).thenReturn(1);

            // 执行测试
            SysDictItem result = sysDictService.createDictItem(dto, TEST_USER_ID, TEST_USERNAME);

            // 验证结果
            assertNotNull(result);
            assertEquals(1, result.getIsDefault());

            // 验证clearDefaultFlag被调用（通过验证update方法是否被调用来间接验证）
            // clearDefaultFlag内部会调用update方法来清除其他默认项
            verify(sysDictItemMapper).update(any(), any(LambdaQueryWrapper.class));
        }
    }

    @Nested
    @DisplayName("字典项更新操作")
    class UpdateDictItemTests {

        @Test
        @DisplayName("修改默认项 - 清除旧默认标记")
        void testUpdateDictItem_changeDefault() {
            Long itemId = 1L;
            Long dictId = 1L;

            // 准备现有数据
            SysDictItem existingItem = new SysDictItem();
            existingItem.setItemId(itemId);
            existingItem.setDictId(dictId);
            existingItem.setItemLabel("禁用");
            existingItem.setItemValue("inactive");
            existingItem.setIsDefault(0); // 当前非默认

            // 准备DTO - 修改为默认项
            DictItemUpdateDTO dto = new DictItemUpdateDTO();
            dto.setItemLabel("禁用(修改)");
            dto.setIsDefault(1); // 改为默认项

            when(sysDictItemMapper.selectById(itemId)).thenReturn(existingItem);
            when(sysDictItemMapper.updateById(any(SysDictItem.class))).thenReturn(1);

            boolean result = sysDictService.updateDictItem(itemId, dto, TEST_USER_ID, TEST_USERNAME);

            assertTrue(result);
            assertEquals(1, existingItem.getIsDefault()); // 已改为默认项

            // 验证清除了其他默认项
            verify(sysDictItemMapper).update(any(), any(LambdaQueryWrapper.class));
        }
    }

    // ==================== 边界情况测试 ====================

    @Nested
    @DisplayName("边界情况和异常处理")
    class EdgeCaseTests {

        @Test
        @DisplayName("查询不存在的字典 - 返回null")
        void testGetDictById_notFound() {
            Long nonExistentId = 99999L;
            when(sysDictMapper.selectById(nonExistentId)).thenReturn(null);

            SysDict result = sysDictService.getDictById(nonExistentId);

            assertNull(result);
        }

        @Test
        @DisplayName("删除已删除的字典 - 应抛出异常")
        void testDeleteDict_alreadyDeleted() {
            Long dictId = 1L;
            SysDict dict = new SysDict();
            dict.setDictId(dictId);
            dict.setDeleted(1); // 已逻辑删除

            when(sysDictMapper.selectById(dictId)).thenReturn(dict);

            BusinessException exception = assertThrows(BusinessException.class, () -> {
                sysDictService.deleteDict(dictId);
            });

            assertTrue(exception.getMessage().contains("已被删除"));
        }

        @Test
        @DisplayName("获取统计信息 - 返回完整统计数据")
        void testGetDictStats_returnsCompleteStats() {
            // Mock各种计数
            when(sysDictMapper.selectCount(any(LambdaQueryWrapper.class)))
                .thenReturn(10L);  // 总字典数

            // 启用字典数单独mock
            LambdaQueryWrapper<SysDict> enabledWrapper = any(LambdaQueryWrapper.class);
            when(sysDictMapper.selectCount(enabledWrapper))
                .thenReturn(8L);  // 启用字典数

            when(sysDictItemMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(50L); // 总字典项数

            // Mock分组列表
            SysDict dict1 = new SysDict();
            dict1.setDictGroup("system");
            SysDict dict2 = new SysDict();
            dict2.setDictGroup("business");

            when(sysDictMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(dict1, dict2));

            var stats = sysDictService.getDictStats();

            assertNotNull(stats);
            assertEquals(10L, stats.get("totalDicts"));
            assertEquals(8L, stats.get("enabledDicts"));
            assertEquals(50L, stats.get("totalItems"));
            assertEquals(2, stats.get("groupCount"));
        }
    }
}
