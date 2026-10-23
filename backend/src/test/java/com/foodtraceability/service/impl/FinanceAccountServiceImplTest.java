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
 * <p><b>已知缺陷（特征化固化，不修）：</b>{@code buildAccountTree} 递归算出 children 却从不挂回
 * account，导致返回的树是「只有顶层节点、不含子节点」的扁平列表；且 {@code parentId} 比较为
 * {@code parentId.equals(...)}，顶层父级必须恰好是 0L。修这棵树属业务语义变更（前端如何消费
 * 该结构未定），按无人值守纪律只固化现状并登记 WORKER_LOG。</p>
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
    @DisplayName("getAccountTree：只返回 parentId=0 的顶层科目（子节点未挂载，特征化现状）")
    void getAccountTree_onlyTopLevel() {
        when(financeAccountMapper.selectAllActive()).thenReturn(List.of(
                account(1L, 0L, "1001", "ASSET"),
                account(2L, 1L, "100101", "ASSET"),
                account(3L, 0L, "2001", "LIABILITY")
        ));

        List<FinanceAccount> tree = service.getAccountTree();

        assertEquals(2, tree.size(), "当前实现只返回顶层节点");
        assertEquals(List.of(1L, 3L), tree.stream().map(FinanceAccount::getId).toList());
        verify(financeAccountMapper).selectAllActive();
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
