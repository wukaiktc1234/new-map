package com.foodtraceability.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RT-1 [P0] 回归：限流键源不得被客户端请求头伪造。
 *
 * <p>R7 现场：同一 IP 直连 8081，把 {@code X-Real-IP} 每次换成 10.7.i.1，
 * 6 次错误密码<b>全部 200</b>（阈值 5 完全失效）。修后必须：</p>
 * <ul>
 *   <li>直连（remoteAddr 不在白名单）→ 键恒为 remoteAddr，轮换任何头都改不了键 → 第 6 次 429；</li>
 *   <li>只有 remoteAddr ∈ trusted-proxies 时才解析 XFF / X-Real-IP，且 XFF 取「最右侧非代理地址」；</li>
 *   <li>内存键表有界（窗口清扫 + 硬上限淘汰）。</li>
 * </ul>
 */
class RateLimitFilterTest {

    private static final String LOGIN_URI = "/api/v1/auth/login";
    private static final String LOCAL = "127.0.0.1";

    private RateLimitFilter newFilter(String trustedProxies) {
        RateLimitFilter filter = new RateLimitFilter(new ObjectMapper());
        filter.init();                                   // 生产同款启动自检（字段未注入 → 空白名单）
        filter.configureTrustedProxies(trustedProxies);
        return filter;
    }

    private int hit(RateLimitFilter filter, String remoteAddr, String headerName, String headerValue) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", LOGIN_URI);
        req.setRemoteAddr(remoteAddr);
        if (headerName != null) {
            req.addHeader(headerName, headerValue);
        }
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());
        return res.getStatus();
    }

    @Test
    @DisplayName("直连 + 轮换 X-Real-IP（R7 的绕过手法）→ 第 6 次必须 429")
    void rotatingRealIpHeaderCannotBypass() throws Exception {
        RateLimitFilter filter = newFilter("");           // 空白名单 = 一个代理都不信
        int[] codes = new int[6];
        for (int i = 1; i <= 6; i++) {
            codes[i - 1] = hit(filter, LOCAL, "X-Real-IP", "10.7." + i + ".1");
        }
        for (int i = 0; i < 5; i++) {
            assertEquals(200, codes[i], "第 " + (i + 1) + " 次应放行（阈值 5）；实际序列=" + java.util.Arrays.toString(codes));
        }
        assertEquals(429, codes[5], "第 6 次必须是 429 —— 伪造 X-Real-IP 不得产生新限流键；实际序列="
                + java.util.Arrays.toString(codes));
    }

    @Test
    @DisplayName("直连 + 轮换 X-Forwarded-For → 同样不能绕过（第 6 次 429）")
    void rotatingForwardedForHeaderCannotBypass() throws Exception {
        RateLimitFilter filter = newFilter("");
        int last = 0;
        for (int i = 1; i <= 6; i++) {
            last = hit(filter, LOCAL, "X-Forwarded-For", "203.0.113." + i);
        }
        assertEquals(429, last, "XFF 在非可信代理链上必须被忽略，第 6 次应为 429");
    }

    @Test
    @DisplayName("可信代理转发：按 XFF 中最右侧非代理地址分桶（不同客户端互不串号）")
    void trustedProxyHonoursForwardedFor() throws Exception {
        RateLimitFilter filter = newFilter(LOCAL);        // 只有本机是 Nginx
        // 客户端 A（198.51.100.7）：6 连发 → 第 6 次 429
        int lastA = 0;
        for (int i = 1; i <= 6; i++) {
            lastA = hit(filter, LOCAL, "X-Forwarded-For", "198.51.100.7, " + LOCAL);
        }
        assertEquals(429, lastA, "可信代理链上应识别出真实客户端并独立计数");

        // 客户端 B（198.51.100.8）：第 1 次必须 200（不能被 A 的计数连坐）
        int firstB = hit(filter, LOCAL, "X-Forwarded-For", "198.51.100.8, " + LOCAL);
        assertEquals(200, firstB, "不同真实客户端必须各自计数");
    }

    @Test
    @DisplayName("伪造 XFF 前缀无效：左侧段由客户端自带，取最右侧非代理地址")
    void spoofedLeftmostForwardedForIsIgnored() throws Exception {
        RateLimitFilter filter = newFilter(LOCAL);
        String real = "198.51.100.9";
        for (int i = 1; i <= 5; i++) {
            // 攻击者每次伪造最左段，右侧由 Nginx 追加真实地址
            int code = hit(filter, LOCAL, "X-Forwarded-For", "10.9.9." + i + ", " + real + ", " + LOCAL);
            assertEquals(200, code, "第 " + i + " 次应放行");
        }
        int sixth = hit(filter, LOCAL, "X-Forwarded-For", "10.9.9.99, " + real + ", " + LOCAL);
        assertEquals(429, sixth, "最左段伪造不得改变限流键（应仍按真实的 " + real + " 计数）");
    }

    @Test
    @DisplayName("内存键表有界：超过上限触发淘汰，不会无界增长")
    void counterMapIsBounded() throws Exception {
        RateLimitFilter filter = newFilter("");
        filter.setMaxCounterKeysForTest(10);
        for (int i = 0; i < 40; i++) {
            hit(filter, "10.1.0." + i, null, null);
        }
        assertTrue(filter.counterKeyCount() <= 10,
                "键数量必须被硬上限压住，实际=" + filter.counterKeyCount());
    }

    @Test
    @DisplayName("窗口过期清扫：窗口结束后的键必须被移除（防内存泄漏）")
    void expiredKeysAreSwept() throws Exception {
        RateLimitFilter filter = newFilter("");
        for (int i = 0; i < 5; i++) {
            hit(filter, "10.2.0." + i, null, null);
        }
        assertTrue(filter.counterKeyCount() > 0);
        filter.sweepExpired(System.currentTimeMillis() + 61_000L);   // 受控时刻：窗口已过
        assertEquals(0, filter.counterKeyCount(), "窗口过期后所有键都应被清扫");
    }

    @Test
    @DisplayName("IP 文本校验：非法头值不得成为限流键（防超大键 DoS）")
    void invalidHeaderValueIsRejected() {
        assertNull(RateLimitFilter.normalizeIp("not-an-ip"));
        assertNull(RateLimitFilter.normalizeIp(""));
        assertNull(RateLimitFilter.normalizeIp("999.1.1.1"));
        assertEquals("1.2.3.4", RateLimitFilter.normalizeIp("1.2.3.4:5678"));
        assertEquals("::1", RateLimitFilter.normalizeIp("[::1]:8080"));
        assertEquals("10.0.0.7", RateLimitFilter.normalizeIp("::ffff:10.0.0.7"), "IPv4-mapped IPv6 必须还原为 IPv4");
    }
}
