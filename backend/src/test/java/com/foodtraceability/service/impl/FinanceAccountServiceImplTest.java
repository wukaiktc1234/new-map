package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.foodtraceability.entity.finance.FinanceAccount;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.mapper.FinanceAccountMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * FinanceAccountServiceImpl 单元测试（会计科目：树、按类型/父级、按编码）。
 *
 * <p>科目树：{@code buildAccountTree} 已修复为「递归算出子节点后挂回 children」，
 * 顶层父级约定为 0L。修复前它只返回顶层节点（children 从未挂载），本类改为断言真实树形结构。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FinanceAccountServiceImplTest {

    @Mock
    private FinanceAccountMapper financeAccountMapper;

    @InjectMocks
    private FinanceAccountServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, FinanceAccount.class);
    }

    @BeforeEach
    void injectBaseMapper() throws Exception {
        Field field = ServiceImpl.class.getDeclaredField("baseMapper");
        field.setAccessible(true);
        field.set(service, financeAccountMapper);
    }

    private static FinanceAccount account(Long id, Long parentId, String code, String type) {
        FinanceAccount a = new FinanceAccount();
        a.setId(id);
        a.setParentId(parentId);
        a.setAccountCode(code);
        a.setAccountName("科目" + code);
        a.setAccountType(type);
        return a;
    }

    @Test
    @DisplayName("getAccountTree：父子节点正确挂载（三级树），叶子节点 children 为空列表")
    void getAccountTree_buildsNestedTree() {
        when(financeAccountMapper.selectAllActive()).thenReturn(List.of(
                account(1L, 0L, "1001", "ASSET"),
                account(2L, 1L, "100101", "ASSET"),
                account(4L, 2L, "10010101", "ASSET"),
                account(3L, 0L, "2001", "LIABILITY")
        ));

        List<FinanceAccount> tree = service.getAccountTree();

        assertEquals(2, tree.size(), "顶层应只有 1001 与 2001 两个一级科目");
        assertEquals(List.of(1L, 3L), tree.stream().map(FinanceAccount::getId).toList());

        FinanceAccount level1 = tree.get(0);
        assertEquals(1, level1.getChildren().size(), "1001 下应挂 100101");
        assertEquals(2L, level1.getChildren().get(0).getId());

        FinanceAccount level2 = level1.getChildren().get(0);
        assertEquals(1, level2.getChildren().size(), "100101 下应挂 10010101（三级）");
        assertEquals(4L, level2.getChildren().get(0).getId());

        assertTrue(level2.getChildren().get(0).getChildren().isEmpty(), "叶子节点 children 必须是空列表而非 null");
        assertTrue(tree.get(1).getChildren().isEmpty(), "2001 无子科目");

        verify(financeAccountMapper).selectAllActive();
    }

    @Test
    @DisplayName("getAccountTree：父节点缺失的孤儿节点不会被挂到任何父级（不丢不报错）")
    void getAccountTree_orphanIgnored() {
        when(financeAccountMapper.selectAllActive()).thenReturn(List.of(
                account(1L, 0L, "1001", "ASSET"),
                account(9L, 999L, "999999", "ASSET")
        ));

        List<FinanceAccount> tree = service.getAccountTree();

        assertEquals(1, tree.size(), "孤儿节点（parentId 指向不存在的科目）当前不会出现在树里");
        assertEquals(1L, tree.get(0).getId());
    }

    @Test
    @DisplayName("getAccountTree：空数据返回空列表，不抛异常")
    void getAccountTree_empty() {
        when(financeAccountMapper.selectAllActive()).thenReturn(new ArrayList<>());
        assertTrue(service.getAccountTree().isEmpty());
    }

    @Test
    @DisplayName("getAccountsByType：原样透传科目类型")
    void getAccountsByType_delegates() {
        when(financeAccountMapper.selectByType("ASSET"))
                .thenReturn(List.of(account(1L, 0L, "1001", "ASSET")));

        List<FinanceAccount> result = service.getAccountsByType("ASSET");

        assertEquals(1, result.size());
        verify(financeAccountMapper).selectByType("ASSET");
    }

    @Test
    @DisplayName("getAccountsByParentId：parentId 为 null 也原样下传（不做兜底）")
    void getAccountsByParentId_passesThrough() {
        when(financeAccountMapper.selectByParentId(null)).thenReturn(new ArrayList<>());
        assertTrue(service.getAccountsByParentId(null).isEmpty());
        verify(financeAccountMapper).selectByParentId(null);
    }

    @Test
    @DisplayName("getByAccountCode：按科目编码等值查，命中返回科目")
    void getByAccountCode_delegates() {
        when(financeAccountMapper.selectOne(any())).thenReturn(account(1L, 0L, "1001", "ASSET"));

        FinanceAccount result = service.getByAccountCode("1001");

        assertNotNull(result);
        assertEquals("1001", result.getAccountCode());
        @SuppressWarnings("unchecked")
        var captor = org.mockito.ArgumentCaptor.forClass(Wrapper.class);
        verify(financeAccountMapper).selectOne(captor.capture());
        assertTrue(captor.getValue().getCustomSqlSegment().toLowerCase().contains("account_code"),
                "必须按 account_code 过滤，实际: " + captor.getValue().getCustomSqlSegment());
    }

    @Test
    @DisplayName("getByAccountCode：查不到返回 null")
    void getByAccountCode_missing_returnsNull() {
        when(financeAccountMapper.selectOne(any())).thenReturn(null);
        assertNull(service.getByAccountCode("NOPE"));
    }
}
