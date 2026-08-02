package com.foodtraceability.service.finance.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.foodtraceability.entity.finance.AccountingSubject;
import com.foodtraceability.mapper.finance.AccountingSubjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AccountingSubjectServiceImpl 单元测试
 *
 * <p>Sprint 3.1 P0 T-018 (TDD)：验证 getByCode() 按 subjectCode 查询科目，
 * 依赖 @TableLogic 自动过滤 deleted=0；科目不存在时返回 null。</p>
 *
 * <p>注意：AccountingSubjectServiceImpl 继承 ServiceImpl，baseMapper 是父类 protected 字段，
 * Mockito @InjectMocks 无法注入，需通过 ReflectionTestUtils 手动设置。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AccountingSubjectServiceImpl 单元测试 - getByCode")
class AccountingSubjectServiceTest {

    @Mock
    private AccountingSubjectMapper accountingSubjectMapper;

    private AccountingSubjectServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AccountingSubjectServiceImpl();
        // 通过反射设置 ServiceImpl 的 baseMapper 字段
        ReflectionTestUtils.setField(service, "baseMapper", accountingSubjectMapper);
    }

    @Test
    @DisplayName("主流程：getByCode('1001') 返回真实科目（非null）")
    void getByCode_success() {
        // given
        AccountingSubject subject = new AccountingSubject();
        subject.setSubjectId(1L);
        subject.setSubjectCode("1001");
        subject.setSubjectName("库存现金");
        subject.setSubjectType(1);
        subject.setDirection(1);
        when(accountingSubjectMapper.selectOne(any(Wrapper.class)))
                .thenReturn(subject);

        // when
        AccountingSubject result = service.getByCode("1001");

        // then
        assertNotNull(result, "getByCode 应返回真实科目（非null）");
        assertEquals(1L, result.getSubjectId());
        assertEquals("1001", result.getSubjectCode());
        assertEquals("库存现金", result.getSubjectName());
        verify(accountingSubjectMapper).selectOne(any(Wrapper.class));
    }

    @Test
    @DisplayName("边界：getByCode 不存在的编码返回 null")
    void getByCode_notFound_returnsNull() {
        // given
        when(accountingSubjectMapper.selectOne(any(Wrapper.class)))
                .thenReturn(null);

        // when
        AccountingSubject result = service.getByCode("9999");

        // then
        assertNull(result, "科目不存在时应返回 null（由调用方决定是否抛异常）");
    }

    @Test
    @DisplayName("边界：getByCode 传入 null 时抛 IllegalArgumentException")
    void getByCode_nullCode_throwsException() {
        // when & then
        assertThrows(IllegalArgumentException.class, () -> service.getByCode(null));
    }

    @Test
    @DisplayName("边界：getByCode 传入空字符串时抛 IllegalArgumentException")
    void getByCode_emptyCode_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> service.getByCode(""));
    }
}
