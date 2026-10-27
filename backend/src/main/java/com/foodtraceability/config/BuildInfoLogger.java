package com.foodtraceability.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * 启动身份日志：打印 git 短哈希 + 构建时间（jar/classes 的 mtime）。
 *
 * <p>为什么：2026-10-28 两次「假验证」都源于「旧进程冒充新代码」/「子集冒充全量」。
 * 验证前先看一眼这行日志，就能确认跑的是不是刚构建的代码 —— 比比对时间戳可靠（增量编译会蒙时间戳）。</p>
 *
 * <p>git 只读（rev-parse），任何失败都退化为 {@code build=unknown}，<b>绝不影响启动</b>。</p>
 */
@Configuration
public class BuildInfoLogger {

    private static final Logger log = LoggerFactory.getLogger(BuildInfoLogger.class);

    @Bean
    public ApplicationRunner buildInfoRunner() {
        return args -> {
            String hash = shortGitHash();
            String built = buildTime();
            log.info("[BOOT] build={} at {}", hash, built);
        };
    }

    private String shortGitHash() {
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "rev-parse", "--short", "HEAD");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            if (!p.waitFor(3, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return "unknown";
            }
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            return (p.exitValue() == 0 && !out.isEmpty()) ? out : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String buildTime() {
        try {
            Path marker = Path.of(BuildInfoLogger.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            File f = marker.toFile();
            return Instant.ofEpochMilli(f.lastModified()).toString();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
