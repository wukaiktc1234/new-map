package com.foodtraceability.security.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * bug-㉑：权限清单的进程内缓存（JWT 不再携带权限，改由服务端按角色实时查）。
 *
 * <h2>为什么手写、不用 Caffeine</h2>
 * <p>本项目**没有** caffeine 依赖，且构建链是 <b>mvn -o 离线模式</b> —— 引新依赖会让离线构建直接失败，
 * 这比「用不用成熟库」更重要。本类只需「按 userId 缓存 + TTL + 主动失效」三件事，几十行即可，行为完全可预测。</p>
 *
 * <h2>失效机制（两条路径，必须有第二条）</h2>
 * <ol>
 *   <li><b>主动</b>：{@link #invalidateAll()} / {@link #invalidate(String)} —— 由
 *       {@code PermissionCacheInvalidator} 在角色-权限绑定变更后调用（RoleController#assignPermissions 已接入）。</li>
 *   <li><b>兜底（版本比对，定时）</b>：{@link #refreshVersionPeriodically()} 每秒查一次
 *       「权限目录版本」（role_permissions 的 count + max(create_time)），变化则整体失效。
 *       这样即使有人**直接改库**绕过接口，撤权也会在 ~1.5 秒内生效。</li>
 * </ol>
 *
 * <p><b>关键设计（Lead 2026-10-28 明确要求）</b>：版本比对放在<b>调度器</b>里，
 * <b>请求路径不做任何 DB 查询</b>，只做本地内存比对 —— 否则等于「token 变小了、DB QPS 暴涨」。</p>
 */
@Component
public class UserPermissionCache {

    private static final Logger log = LoggerFactory.getLogger(UserPermissionCache.class);

    private final Map<String, Entry> cache = new ConcurrentHashMap<>();

    /** 缓存存活时间（秒）：30–60s 区间，默认 45s */
    @Value("${security.permission-cache.ttl-seconds:45}")
    private long ttlSeconds = 45L;

    private record Entry(List<String> permissions, long expireAtMillis) { }

    /** 权限目录版本（由调度器每秒刷新，请求路径只读本字段） */
    private volatile String cachedVersion = null;
    private volatile long versionRefreshedAt = 0L;

    /** 最近一次**真正执行**版本查询的时间（cacheMode 判据，防假信号） */
    private volatile long lastPollAt = 0L;

    /** 累计真正轮询次数（自检日志用） */
    private volatile long pollCount = 0L;

    @Autowired(required = false)
    private com.foodtraceability.mapper.UserMapper versionSource;

    /** 取权限；未命中或已过期则用 loader 回源。**请求路径不做 DB 版本查询**。 */
    public List<String> get(String userId, Function<String, List<String>> loader) {
        if (userId == null) {
            return List.of();
        }
        long now = System.currentTimeMillis();
        Entry hit = cache.get(userId);
        if (hit != null && hit.expireAtMillis() > now) {
            return hit.permissions();
        }
        List<String> fresh = loader.apply(userId);
        List<String> safe = fresh == null ? List.of() : fresh;
        cache.put(userId, new Entry(safe, now + ttlSeconds * 1000L));
        return safe;
    }

    /** 是否已在启动时检查过 versionSource（避免每轮刷屏） */
    private volatile boolean sourceChecked = false;

    /**
     * 缓存模式（Lead 2026-10-29）：
     * VERSION_POLLING = 版本轮询可用（直接改库 ~1s 生效）；
     * TTL_ONLY = versionSource 缺失，只能等 TTL（≤45s）。
     */
    public String cacheMode() {
        // ★ ④（Lead 2026-10-29）：「Bean 注入成功」**不等于**轮询真的在跑 ——
        //   之前 cacheMode 只看 versionSource != null，是**假信号**（调度被 enabled:false 关掉时仍报 VERSION_POLLING）。
        //   现在改成看 lastPollAt：最近 5 秒内真的轮询过才算 VERSION_POLLING。
        if (versionSource == null) {
            return "TTL_ONLY";
        }
        long since = System.currentTimeMillis() - lastPollAt;
        return since <= 5000L ? "VERSION_POLLING" : "TTL_ONLY";
    }

    /** 最近一次真正轮询到版本的时间（0 = 从未轮询） */
    public long lastPollAt() {
        return lastPollAt;
    }

    /** 启动后 10s 自检：若从未轮询到版本 → ERROR（调度被关 or 线程未注册） */
    @jakarta.annotation.PostConstruct
    public void selfCheckPolling() {
        // ★ ②（Lead 2026-10-29）：**检查窗口必须大于最慢启动时间** ——
        //   上一版 10s 检查、而应用启动耗时 28s → 报「轮询已死」其实是误报（假信号 2.0）。
        //   现在：60s 后仍未轮询才报；且若期间已轮询成功，直接静默退出。
        new java.util.Timer(true).schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                if (lastPollAt == 0L) {
                    log.error("[METRIC] permission_version_polling_dead cacheMode=TTL_ONLY（启动 60s 内版本轮询从未执行：app.scheduling.enabled=false 或调度线程未注册 → 权限变更只能等 TTL，安全处置请走应用层撤权）");
                } else {
                    log.info("[PERM-CACHE] 自检通过：启动 60s 内已轮询 {} 次（lastPollAt>0）", pollCount);
                }
                cancel();
            }
        }, 60000L);
    }

    /** 定时（每秒）刷新权限目录版本；变化则整体失效。DB 查询恒为 1/秒，与业务流量无关。 */
    @Scheduled(fixedRate = 1000L, initialDelay = 1000L)
    public void refreshVersionPeriodically() {
        if (versionSource == null) {
            // ★ ②（Lead 2026-10-29）：**不许静默降级**。versionSource 缺失 = 版本轮询失效，
            //   权限变更只能等 TTL（≤45s）；直接改库「以为立刻生效」会导致安全处置失败。
            if (!sourceChecked) {
                sourceChecked = true;
                log.error("[METRIC] permission_version_source_missing cacheMode=TTL_ONLY（版本轮询不可用：UserMapper 未注入，权限变更最长 45s 才生效，安全处置必须走应用层撤权）");
            }
            return;
        }
        if (!sourceChecked) {
            sourceChecked = true;
            log.info("[PERM-CACHE] cacheMode=VERSION_POLLING（版本轮询可用，每 1s 比对 permissionVersion）");
        }
        versionRefreshedAt = System.currentTimeMillis();
        try {
            String v = versionSource.permissionVersion();
            lastPollAt = System.currentTimeMillis();   // 只有真查询成功才算「轮询在跑」
            pollCount++;
            if (v != null && !v.equals(cachedVersion)) {
                if (cachedVersion != null) {
                    log.info("[PERM-CACHE] 权限目录版本变更 {} → {}，整体失效（定时比对）", cachedVersion, v);
                    cache.clear();
                }
                cachedVersion = v;
            }
        } catch (Exception e) {
            log.debug("[PERM-CACHE] 版本查询失败（忽略，走 TTL）: {}", e.getMessage());
        }
    }

    /** 单个用户失效（改某人的权限后调用）。 */
    public void invalidate(String userId) {
        if (userId != null) {
            cache.remove(userId);
            log.info("[PERM-CACHE] invalidate userId={}", userId);
        }
    }

    /** 全量失效（角色-权限绑定变更后调用），保证撤权**立即**生效，不等 TTL。 */
    public void invalidateAll() {
        int n = cache.size();
        cache.clear();
        log.info("[PERM-CACHE] invalidateAll, cleared {} entries", n);
    }

    public int size() {
        return cache.size();
    }

    /** 仅用于诊断：最近一次版本刷新时间（毫秒时间戳）。 */
    public long lastVersionRefreshAt() {
        return versionRefreshedAt;
    }
}
