package com.foodtraceability.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * constant-time 登录与「账号枚举防护」的契约单测（Lead 2026-10-30）。
 *
 * <p>为什么用源码断言而不是启动 Spring：这两条契约是「不许写错」的硬约束，
 * 启动整套上下文会让单测变慢且依赖 DB；源码级断言同样能防回潮。</p>
 */
class ConstantTimeLoginContractTest {

    private static final String AUTH_SRC =
            "src/main/java/com/foodtraceability/service/impl/AuthServiceImpl.java";
    private static final String POS_SRC =
            "src/main/java/com/foodtraceability/service/impl/PosAuthServiceImpl.java";

    private String read(String rel) throws IOException {
        Path p = Path.of(rel);
        if (!Files.exists(p)) {
            p = Path.of("backend").resolve(rel);
        }
        return Files.readString(p, StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("B 类：文案必须统一，且不得出现任何「不存在」字样（防账号枚举）")
    void bClassMessagesMustBeIdentical() throws IOException {
        String auth = read(AUTH_SRC);
        // ★ 只断言【登录方法体】内不泄露，不对整个文件断言：
        //   文件里其它方法（改密 / 重置密码，行 385/560/689/741/975）确实有「用户不存在」，
        //   但那些是**已认证后的内部操作**，不是登录路径，不构成枚举面。
        int loginStart = auth.indexOf("public LoginResponse login(LoginRequest");
        assertTrue(loginStart > 0, "必须能定位 login 方法");
        int loginEnd = auth.indexOf("public ", loginStart + 10);
        String loginBody = auth.substring(loginStart, loginEnd > 0 ? loginEnd : auth.length());
        int occurrences = loginBody.split("用户名或密码错误", -1).length - 1;
        assertTrue(occurrences >= 2,
                "login 方法内「用户名或密码错误」应至少出现 2 次（查无用户 / 密码错），实际 " + occurrences);
        assertFalse(loginBody.contains("用户不存在"),
                "login 方法内不得出现「用户不存在」——这是账号枚举泄露");
        assertFalse(loginBody.contains("账号不存在"),
                "login 方法内不得出现「账号不存在」——这是账号枚举泄露");
        String pos = read(POS_SRC);
        assertFalse(pos.contains("用户不存在或已被删除"),
                "PosAuthServiceImpl 不得出现「用户不存在或已被删除」——账号枚举泄露");
    }

    @Test
    @DisplayName("dummy 哈希：必须 cost=12 + 关闭开关要有 ERROR 横幅 + prod fail-fast")
    void dummyHashMustBeValidBcrypt12() throws IOException {
        String auth = read(AUTH_SRC);
        assertTrue(auth.contains("BCryptPasswordEncoder(12)"),
                "dummy 必须用 cost=12 的 encoder，否则时间与真实校验不等");
        assertTrue(auth.contains("DUMMY_BCRYPT_HASH"), "必须存在 dummy 哈希常量");
        assertTrue(auth.contains("constant_time_login=DISABLED"),
                "关闭开关必须有 ERROR 横幅（安全开关不许静默关闭）");
        assertTrue(auth.contains("拒绝启动"),
                "prod/pg profile 下关闭 constant-time 必须 fail-fast");
    }

    @Test
    @DisplayName("登录顺序：哈希必须前置到所有 early-return 之前")
    void hashMustPrecedeEarlyReturns() throws IOException {
        String auth = read(AUTH_SRC);
        int hashIdx = auth.indexOf("final boolean passwordMatches = PasswordUtils.matches");
        int nullCheck = auth.indexOf("if (user == null)", hashIdx);
        assertTrue(hashIdx > 0, "必须有无条件 bcrypt 校验");
        assertTrue(nullCheck > hashIdx,
                "查无用户的分支必须在哈希之后（顺序即语义），否则时间侧信道回来");
    }

    @Test
    @DisplayName("启动自检：dummy 失败必须 fail-fast（不许静默降级）")
    void dummySelfCheckMustFailFast() throws IOException {
        String auth = read(AUTH_SRC);
        assertTrue(auth.contains("dummy_hash 初始化失败"), "dummy 非法时必须抛异常拒绝启动");
        assertTrue(auth.contains("$2a$12$"), "必须校验 bcrypt cost 前缀");
    }
}
