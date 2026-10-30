package com.foodtraceability.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 请求限流过滤器（内存计数器，按 客户端IP + 接口路径 统计）。
 *
 * <p><b>RT-1 [P0] 修复（R7 红队审计）</b>：修前 {@code getClientIpFromSocket()} 首选
 * {@code X-Real-IP} 请求头 —— 该头完全由客户端控制，攻击者每次换一个假 IP 即可让
 * 每个限流键都是新的，<b>5/60s 的登录阈值形同虚设（实测 6/6 全 200 绕过）</b>。</p>
 *
 * <p>修后规则（三条，缺一不可）：</p>
 * <ol>
 *   <li><b>默认只信 socket</b>：{@code req.getRemoteAddr()} 是 TCP 对端地址，客户端无法伪造；</li>
 *   <li><b>只有来自可信代理的请求才解析转发头</b>：{@code remoteAddr} 命中
 *       {@code rate-limit.trusted-proxies}（IP 或 CIDR 列表）时，才允许读取
 *       {@code X-Forwarded-For} / {@code X-Real-IP}，且 XFF 按 RFC 7239 语义
 *       <b>从右往左</b>跳过可信代理取第一个非代理地址（左起第一段可被客户端任意伪造，禁止直接取）；</li>
 *   <li><b>内存 Map 有界</b>：每个窗口做一次全量过期清扫（{@link #sweepExpired}），
 *       超过 {@link #MAX_COUNTER_KEYS} 时按窗口起点淘汰最旧键 —— 防止洪泛下
 *       {@code computeIfAbsent} 无界增长把堆打爆（R7 RT-5）。</li>
 * </ol>
 *
 * <p>启动日志打 {@code [SECURITY] rate_limit trusted_proxies=...}，让"到底信不信转发头"
 * 在运行时可见（不是靠读代码猜）。</p>
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);

    private final ObjectMapper objectMapper;

    /** 内存限流器，按 IP+URI 维度计数 */
    private final ConcurrentHashMap<String, RateLimitCounter> memoryRateLimit = new ConcurrentHashMap<>();

    /** 限流时间窗口（毫秒） */
    private static final long TIME_WINDOW_MS = 60_000L;

    /** 单窗口最大请求数 */
    private static final int MAX_REQUESTS_PER_WINDOW = 100;

    /**
     * 计数器键的硬上限（RT-5：无界 Map = 可被洪泛打爆的 DoS 自伤面）。
     * 正常业务下 60s 内的「IP+URI」键远小于此值；超限说明正在被扫，此时按最旧淘汰并打 metric。
     */
    static final int MAX_COUNTER_KEYS = 50_000;

    /** 上次全量清扫时间（毫秒）；CAS 保证同一窗口只有一个线程扫 */
    private final AtomicLong lastSweepMillis = new AtomicLong(System.currentTimeMillis());

    /** 可信反向代理白名单原文（IP 或 CIDR，逗号分隔）；空 = 一个都不信 */
    @Value("${rate-limit.trusted-proxies:}")
    private String trustedProxiesRaw;

    /** 生效的键数量上限（单测可下调；生产恒为 {@link #MAX_COUNTER_KEYS}） */
    private int maxCounterKeys = MAX_COUNTER_KEYS;

    /** 解析后的白名单匹配器（不可变，volatile 发布） */
    private volatile List<IpAddressMatcher> trustedProxyMatchers = List.of();

    /** 实际生效的白名单（日志/自检用） */
    private volatile List<String> trustedProxySpecs = List.of();

    /**
     * ③（Lead 2026-10-30 P0 止血）：按 URI 后缀配置阈值 —— 敏感端点必须单独收紧。
     *
     * <p>修前实测：**8 次错误密码全部放行**，因为统一阈值是 100 次/分钟，
     * 而 {@code @RateLimit(5/60s)} 注解所在的切面被 TODO 短路 → 登录实际几乎无限流（可撞库）。</p>
     *
     * <p>规则：按「包含即匹配」判定；表用 LinkedHashMap 保证顺序可读，长的 key 写在前面。</p>
     */
    private static final LinkedHashMap<String, Integer> PATH_LIMITS = new LinkedHashMap<>();
    static {
        // 默认 5/60s；可用环境变量 RATE_LIMIT_LOGIN 覆盖（仅用于计时测量等特殊场景，勿在生产调高）
        int loginLimit = 5;
        try {
            String env = System.getenv("RATE_LIMIT_LOGIN");
            if (env != null && !env.isBlank()) {
                loginLimit = Integer.parseInt(env.trim());
            }
        } catch (NumberFormatException ignored) {
            loginLimit = 5;
        }
        PATH_LIMITS.put("/auth/login", loginLimit);   // 管理端 /v1/auth/login 与 POS /v1/pos/auth/login 都命中
        PATH_LIMITS.put("/auth/captcha", 10);   // 原注解 10/60s
        PATH_LIMITS.put("/pos/orders", 60);     // POS 开单/支付/退款
    }

    /** 解析该 URI 适用的阈值（未命中则用默认 100） */
    private static int limitFor(String uri) {
        if (uri == null) {
            return MAX_REQUESTS_PER_WINDOW;
        }
        for (Map.Entry<String, Integer> e : PATH_LIMITS.entrySet()) {
            if (uri.contains(e.getKey())) {
                return e.getValue();
            }
        }
        return MAX_REQUESTS_PER_WINDOW;
    }

    public RateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 启动自检：把「信不信转发头」变成可观测事实。
     *
     * <p>R7 的教训是「注解在、Bean 在、逻辑被短路，且没有任何日志」。这里把生效白名单
     * 与模式直接打进启动日志，任何人一眼能看出当前是否处于「只信 socket」模式。</p>
     */
    @PostConstruct
    public void init() {
        configureTrustedProxies(trustedProxiesRaw);
        logger.info("[SECURITY] rate_limit enabled=true mechanism=RateLimitFilter windowMs={} defaultLimit={} pathLimits={}",
                TIME_WINDOW_MS, MAX_REQUESTS_PER_WINDOW, PATH_LIMITS);
        if (trustedProxySpecs.isEmpty()) {
            logger.info("[SECURITY] rate_limit keySource=remoteAddr-only trusted_proxies=[] "
                    + "(X-Real-IP / X-Forwarded-For 一律忽略 —— 直连客户端伪造无效)");
        } else {
            logger.info("[SECURITY] rate_limit keySource=remoteAddr trusted_proxies={} "
                    + "(仅这些地址转发的 X-Forwarded-For/X-Real-IP 会被解析)", trustedProxySpecs);
        }
        logger.info("[SECURITY] rate_limit note=内存计数，仅单实例有效；多实例部署需换共享存储（tech-debt SEC-1）");
    }

    /** 解析并发布可信代理白名单（{@code @PostConstruct} 与单测共用） */
    void configureTrustedProxies(String raw) {
        List<String> specs = new ArrayList<>();
        List<IpAddressMatcher> matchers = new ArrayList<>();
        if (raw != null) {
            for (String part : raw.split(",")) {
                String spec = part.trim();
                if (spec.isEmpty()) {
                    continue;
                }
                try {
                    matchers.add(new IpAddressMatcher(spec));
                    specs.add(spec);
                } catch (RuntimeException e) {
                    // 配置写错不能静默：写错的代理白名单要么放行伪造头（更糟），要么整条链路失效
                    logger.error("[SECURITY] rate_limit trusted-proxies 配置项非法，已忽略：'{}' err={}", spec, e.getMessage());
                }
            }
        }
        this.trustedProxyMatchers = List.copyOf(matchers);
        this.trustedProxySpecs = List.copyOf(specs);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // ★ RT-1：限流键源只认 socket，除非对端是白名单内的可信代理
        String clientIp = resolveClientIp(request);
        String requestUri = request.getRequestURI();
        String rateLimitKey = "rate_limit:" + clientIp + ":" + requestUri;

        int limit = limitFor(requestUri);
        if (!isRateLimited(rateLimitKey, limit)) {
            chain.doFilter(request, response);
            return;
        }

        logger.warn("限流拦截 - IP: {}, URI: {}, limit={}", clientIp, requestUri, limit);
        // ★（Lead 2026-10-30）429 必须带标准 **Retry-After** 头：
        //   前端已按「服务端原文 + Retry-After 计算等待时长」实现，只回 body.message 时
        //   「约 N 秒后可重试」永远拿不到真实值。
        RateLimitCounter counter = memoryRateLimit.get(rateLimitKey);
        final long retryAfterSec = (counter == null) ? 1L : counter.secondsUntilReset();
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(retryAfterSec));   // 与 body.retryAfter **同源同值**
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("code", 429);
        payload.put("message", "请求过于频繁，请稍后再试");
        payload.put("timestamp", System.currentTimeMillis());
        payload.put("retryAfter", retryAfterSec);      // ← 同一个变量，禁止再算第二次
        response.getWriter().write(objectMapper.writeValueAsString(payload));
    }

    // ==================== 键源解析（RT-1 修复主体） ====================

    /**
     * 解析限流键所用的客户端 IP。
     *
     * <p><b>默认只返回 {@code req.getRemoteAddr()}</b>。只有对端地址命中
     * {@code rate-limit.trusted-proxies} 时，才解析转发头 —— 因为只有那种情况下
     * 转发头才是由我们自己的代理追加的，客户端伪造的值已经被代理覆盖/追加在其右侧。</p>
     */
    String resolveClientIp(HttpServletRequest req) {
        String remoteAddr = normalizeIp(req.getRemoteAddr());
        if (remoteAddr == null) {
            return "unknown";
        }
        if (!isTrustedProxy(remoteAddr)) {
            // 直连：任何 X-Real-IP / X-Forwarded-For 都是攻击者可控数据，直接丢弃
            return remoteAddr;
        }
        String fromXff = clientFromForwardedFor(req.getHeader("X-Forwarded-For"));
        if (fromXff != null) {
            return fromXff;
        }
        String realIp = normalizeIp(req.getHeader("X-Real-IP"));
        return realIp != null ? realIp : remoteAddr;
    }

    /**
     * 从 {@code X-Forwarded-For} 中取真实客户端地址。
     *
     * <p>XFF 是「代理逐跳追加」的列表：{@code client, proxy1, proxy2}。左起第一段是
     * <b>客户端自己写进来的</b>（可任意伪造），只有右侧由可信代理追加的段才可信。
     * 因此从右往左走，跳过可信代理，第一个非代理地址才是真实客户端。</p>
     */
    private String clientFromForwardedFor(String xff) {
        if (xff == null || xff.isBlank()) {
            return null;
        }
        String[] hops = xff.split(",");
        String leftmostValid = null;
        for (int i = hops.length - 1; i >= 0; i--) {
            String ip = normalizeIp(hops[i]);
            if (ip == null) {
                continue;
            }
            leftmostValid = ip;
            if (isTrustedProxy(ip)) {
                continue;   // 这一段是我们自己的代理追加的，继续往左找真实客户端
            }
            return ip;
        }
        return leftmostValid;
    }

    /** 对端是否为可信反向代理 */
    boolean isTrustedProxy(String ip) {
        if (ip == null) {
            return false;
        }
        for (IpAddressMatcher matcher : trustedProxyMatchers) {
            try {
                if (matcher.matches(ip)) {
                    return true;
                }
            } catch (RuntimeException ignored) {
                // 单个匹配器异常不影响其它白名单项
            }
        }
        return false;
    }

    /**
     * 归一化并校验 IP 文本：去掉引号/方括号/端口/zone-id，非法格式直接返回 null。
     *
     * <p>目的不是完整解析 IP，而是**拒绝把任意长字符串当限流键**（否则
     * {@code X-Real-IP: <1MB 垃圾>} 就能往 Map 里塞任意键）。</p>
     */
    static String normalizeIp(String raw) {
        if (raw == null) {
            return null;
        }
        String ip = raw.trim();
        if (ip.isEmpty() || ip.length() > 64 || "unknown".equalsIgnoreCase(ip)) {
            return null;
        }
        // IPv4-mapped IPv6（::ffff:10.0.0.1）：双栈监听下 remoteAddr 常见形态，必须还原成 IPv4 再校验，
        // 否则会被判为非法 → 所有客户端挤进 "unknown" 同一个桶（误伤式限流）。
        String lower = ip.toLowerCase();
        if (lower.startsWith("::ffff:") && lower.indexOf('.') > 0) {
            ip = ip.substring(7);
        }
        if (ip.startsWith("[")) {                      // [::1] 或 [::1]:1234
            int end = ip.indexOf(']');
            if (end < 0) {
                return null;
            }
            ip = ip.substring(1, end);
        } else if (ip.indexOf(':') >= 0 && ip.indexOf('.') >= 0 && ip.indexOf(':') == ip.lastIndexOf(':')) {
            ip = ip.substring(0, ip.indexOf(':'));      // 1.2.3.4:5678
        }
        int zone = ip.indexOf('%');
        if (zone >= 0) {
            ip = ip.substring(0, zone);
        }
        if (ip.isEmpty() || ip.length() > 45) {
            return null;
        }
        return isIpLiteral(ip) ? ip : null;
    }

    private static boolean isIpLiteral(String s) {
        int colon = s.indexOf(':');
        if (colon < 0) {
            // IPv4：四段 0-255
            String[] parts = s.split("\\.", -1);
            if (parts.length != 4) {
                return false;
            }
            for (String p : parts) {
                if (p.isEmpty() || p.length() > 3) {
                    return false;
                }
                int v;
                try {
                    v = Integer.parseInt(p);
                } catch (NumberFormatException e) {
                    return false;
                }
                if (v < 0 || v > 255) {
                    return false;
                }
            }
            return true;
        }
        // IPv6：只做字符集与段数校验（足够挡垃圾键；真实匹配交给 IpAddressMatcher）
        int groups = 0;
        for (String g : s.split(":", -1)) {
            if (g.isEmpty()) {
                continue;
            }
            if (g.length() > 4) {
                return false;
            }
            for (char c : g.toCharArray()) {
                boolean hex = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
                if (!hex) {
                    return false;
                }
            }
            groups++;
        }
        return groups >= 1;
    }

    // ==================== 计数与内存治理 ====================

    /**
     * 判断是否触发限流（内存实现）。
     *
     * <p>修前 {@code computeIfAbsent} 只增不减（每个新键都留到进程重启），
     * 洪泛下 Map 无界增长；这里补「窗口清扫 + 硬上限淘汰」两层保护。</p>
     */
    private boolean isRateLimited(String key, int limit) {
        long now = System.currentTimeMillis();

        sweepIfDue(now);

        RateLimitCounter counter = memoryRateLimit.get(key);
        if (counter == null) {
            enforceKeyCap(now);   // 先控总量，再插入新键
            counter = memoryRateLimit.computeIfAbsent(key, k -> new RateLimitCounter(now));
        }
        return counter.incrementAndGet(now, limit);
    }

    /** 每窗口一次全量过期清扫（CAS 抢占，避免每个请求都遍历整张表） */
    private void sweepIfDue(long now) {
        long last = lastSweepMillis.get();
        if (now - last < TIME_WINDOW_MS) {
            return;
        }
        if (!lastSweepMillis.compareAndSet(last, now)) {
            return;
        }
        int before = memoryRateLimit.size();
        sweepExpired(now);
        int after = memoryRateLimit.size();
        if (before != after) {
            logger.info("[METRIC] rate_limit_sweep removed={} remaining={}", before - after, after);
        }
    }

    /** 删除所有「窗口已过期」的键；包级可见以便单测传入受控时刻 */
    void sweepExpired(long now) {
        memoryRateLimit.entrySet().removeIf(e -> e.getValue().isExpired(now));
    }

    /** 硬上限保护：超过 {@link #MAX_COUNTER_KEYS} 时先清扫，仍超则按窗口起点淘汰最旧键 */
    private void enforceKeyCap(long now) {
        if (memoryRateLimit.size() < maxCounterKeys) {
            return;
        }
        sweepExpired(now);
        int size = memoryRateLimit.size();
        if (size < maxCounterKeys) {
            return;
        }
        int target = (int) (maxCounterKeys * 0.8);
        int toRemove = size - target;
        List<Map.Entry<String, RateLimitCounter>> entries = new ArrayList<>(memoryRateLimit.entrySet());
        entries.sort((a, b) -> Long.compare(a.getValue().getWindowStart(), b.getValue().getWindowStart()));
        int removed = 0;
        for (Map.Entry<String, RateLimitCounter> e : entries) {
            if (removed >= toRemove) {
                break;
            }
            if (memoryRateLimit.remove(e.getKey(), e.getValue())) {
                removed++;
            }
        }
        logger.warn("[METRIC] rate_limit_key_cap_evict size={} evicted={} cap={}", size, removed, maxCounterKeys);
    }

    /** 供单测断言 Map 是否有界 */
    int counterKeyCount() {
        return memoryRateLimit.size();
    }

    /** 供单测触发一次清扫 */
    void sweepNow() {
        lastSweepMillis.set(0L);
        sweepIfDue(System.currentTimeMillis());
    }

    /** 供单测下调键上限 */
    void setMaxCounterKeysForTest(int cap) {
        this.maxCounterKeys = cap;
    }

    /**
     * 限流计数器（线程安全）：窗口内计数，窗口过期自动重置。
     */
    private static class RateLimitCounter {
        private long windowStart;
        private int requestCount = 0;

        RateLimitCounter(long windowStart) {
            this.windowStart = windowStart;
        }

        /** 计数 +1，返回本次是否应被拦截（窗口内第 limit+1 次起拦截） */
        synchronized boolean incrementAndGet(long now, int limit) {
            if (now - windowStart > TIME_WINDOW_MS) {
                windowStart = now;
                requestCount = 0;
            }
            requestCount++;
            return requestCount > limit;
        }

        /** 该键所在窗口是否已整体过期（可安全删除） */
        synchronized boolean isExpired(long now) {
            return now - windowStart > TIME_WINDOW_MS;
        }

        /** 距离窗口重置还有多少秒（用于 Retry-After 头） */
        synchronized long secondsUntilReset() {
            long remain = windowStart + TIME_WINDOW_MS - System.currentTimeMillis();
            return Math.max(1L, (remain + 999) / 1000);
        }

        synchronized long getWindowStart() {
            return windowStart;
        }
    }
}
