package com.foodtraceability.service.impl;

import com.foodtraceability.dto.CaptchaResponse;
import com.foodtraceability.service.CacheService;
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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CaptchaServiceImpl} 纯 Mockito 单元测试（不连数据库、不起 Web 上下文）。
 *
 * <p>覆盖点：生成（UUID 唯一、4 位文本、PNG data URL、5 分钟缓存）、校验的入参空值、
 * 失败次数上限 5 的清空锁定、不区分大小写匹配、成功后清理缓存、失败后计数递增与 TTL、
 * 以及脏计数（非数字）容错。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CaptchaServiceImplTest {

    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final String FAIL_PREFIX = "captcha:fail:";

    @Mock
    private CacheService cacheService;

    @InjectMocks
    private CaptchaServiceImpl service;

    @BeforeEach
    void setUp() {
        when(cacheService.get(anyString())).thenReturn(null);
    }

    @Test
    @DisplayName("generateCaptcha：返回 UUID + PNG data URL，文本 4 位并以 5 分钟 TTL 缓存")
    void generateCaptcha_createsAndCaches() {
        CaptchaResponse response = service.generateCaptcha();

        assertNotNull(response.getCaptchaId());
        UUID.fromString(response.getCaptchaId());
        assertNotNull(response.getCaptchaImage());
        assertTrue(response.getCaptchaImage().startsWith("data:image/png;base64,"),
                "图片应为 PNG data URL，实际前缀: " + response.getCaptchaImage().substring(0, Math.min(30, response.getCaptchaImage().length())));

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(cacheService).put(eq(CAPTCHA_PREFIX + response.getCaptchaId()), textCaptor.capture(), eq(5L));
        assertEquals(4, textCaptor.getValue().length());
        assertTrue(textCaptor.getValue().matches("[A-Za-z0-9]{4}"));
    }

    @Test
    @DisplayName("generateCaptcha：两次生成 ID 不同")
    void generateCaptcha_idsAreUnique() {
        assertNotEquals(service.generateCaptcha().getCaptchaId(), service.generateCaptcha().getCaptchaId());
    }

    @Test
    @DisplayName("validateCaptcha：id 或验证码为空直接 false，不读写缓存")
    void validateCaptcha_nullArgs() {
        assertFalse(service.validateCaptcha(null, "abcd"));
        assertFalse(service.validateCaptcha("id", null));
        verify(cacheService, never()).get(anyString());
    }

    @Test
    @DisplayName("validateCaptcha：失败次数达到 5 次即锁定，清空验证码与计数")
    void validateCaptcha_lockedAfterMaxFailures() {
        when(cacheService.get(FAIL_PREFIX + "id")).thenReturn("5");

        assertFalse(service.validateCaptcha("id", "abcd"));

        verify(cacheService).delete(CAPTCHA_PREFIX + "id");
        verify(cacheService).delete(FAIL_PREFIX + "id");
        verify(cacheService, never()).get(CAPTCHA_PREFIX + "id");
    }

    @Test
    @DisplayName("validateCaptcha：缓存中无验证码返回 false")
    void validateCaptcha_missingCaptcha() {
        assertFalse(service.validateCaptcha("id", "abcd"));
        verify(cacheService).get(CAPTCHA_PREFIX + "id");
    }

    @Test
    @DisplayName("validateCaptcha：不区分大小写匹配成功，并清理验证码与失败计数")
    void validateCaptcha_success_clearsCache() {
        when(cacheService.get(CAPTCHA_PREFIX + "id")).thenReturn("AbCd");

        assertTrue(service.validateCaptcha("id", "abcd"));

        verify(cacheService).delete(CAPTCHA_PREFIX + "id");
        verify(cacheService).delete(FAIL_PREFIX + "id");
        verify(cacheService, never()).put(eq(FAIL_PREFIX + "id"), anyString(), anyLong());
    }

    @Test
    @DisplayName("validateCaptcha：不匹配时失败计数 +1 并以 5 分钟 TTL 回写")
    void validateCaptcha_mismatch_incrementsFailCount() {
        when(cacheService.get(CAPTCHA_PREFIX + "id")).thenReturn("ABCD");
        when(cacheService.get(FAIL_PREFIX + "id")).thenReturn("3");

        assertFalse(service.validateCaptcha("id", "zzzz"));

        verify(cacheService).put(FAIL_PREFIX + "id", "4", 5L);
        verify(cacheService, never()).delete(CAPTCHA_PREFIX + "id");
    }

    @Test
    @DisplayName("validateCaptcha：首次失败从 1 开始计数")
    void validateCaptcha_firstFailure_startsAtOne() {
        when(cacheService.get(CAPTCHA_PREFIX + "id")).thenReturn("ABCD");

        assertFalse(service.validateCaptcha("id", "zzzz"));

        verify(cacheService).put(FAIL_PREFIX + "id", "1", 5L);
    }

    @Test
    @DisplayName("validateCaptcha：脏计数（非数字）按 0 处理，仍可正常校验")
    void validateCaptcha_corruptFailCount_treatedAsZero() {
        when(cacheService.get(FAIL_PREFIX + "id")).thenReturn("not-a-number");
        when(cacheService.get(CAPTCHA_PREFIX + "id")).thenReturn("ABCD");

        assertTrue(service.validateCaptcha("id", "abcd"));
        verify(cacheService).delete(CAPTCHA_PREFIX + "id");
    }

    @Test
    @DisplayName("validateCaptcha：失败计数 4 仍可校验（未达上限）")
    void validateCaptcha_belowLimit_notLocked() {
        when(cacheService.get(FAIL_PREFIX + "id")).thenReturn("4");
        when(cacheService.get(CAPTCHA_PREFIX + "id")).thenReturn("ABCD");

        assertFalse(service.validateCaptcha("id", "zzzz"));

        verify(cacheService).put(FAIL_PREFIX + "id", "5", 5L);
        verify(cacheService, never()).delete(CAPTCHA_PREFIX + "id");
    }
}
