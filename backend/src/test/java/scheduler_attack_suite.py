#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
============================================================
定时任务调度模块 - 对抗性验证攻击脚本集
目标: Phase 1 后端实现 (15个文件)
生成时间: 2026-04-04
============================================================

使用方法:
  1. 启动后端服务: mvn spring-boot:run
  2. 获取JWT Token (通过登录接口)
  3. 运行: python scheduler_attack_suite.py <BASE_URL> <TOKEN>

依赖: pip install requests

攻击维度:
  A1. 竞态条件 - 并发创建重复任务编码
  A2. 权限绕过 - 批量操作删除内置任务
  A3. 输入溢出 - 超长字段DoS
  A4. CRON注入 - 恶意CRON表达式
  A5. SQL注入探测 - LIKE语句特殊字符
  A6. 类型混淆 - 非法枚举值注入
  A7. 架构违规验证 - Controller直连Mapper探测
"""

import requests
import json
import threading
import time
import sys
import random
import string
from concurrent.futures import ThreadPoolExecutor, as_completed

# ==================== 配置 ====================
BASE_URL = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8081"
TOKEN = sys.argv[2] if len(sys.argv) > 2 else "YOUR_JWT_TOKEN_HERE"
API_PREFIX = f"{BASE_URL}/api/v1/scheduler"

HEADERS = {
    "Authorization": f"Bearer {TOKEN}",
    "Content-Type": "application/json",
    "Accept": "application/json"
}

# 攻击结果记录
attack_results = []


def log_attack(attack_id, name, status, detail, severity="HIGH"):
    """记录攻击结果"""
    result = {
        "id": attack_id,
        "name": name,
        "status": status,  # VULNERABLE / RESISTED / ERROR / SKIPPED
        "detail": detail,
        "severity": severity
    }
    attack_results.append(result)
    icon = "[!!!]" if status == "VULNERABLE" else ("[+++]" if status == "RESISTED" else "[???]")
    print(f"{icon} [{attack_id}] {name}: {status}")
    if detail:
        print(f"     -> {detail}")


# ============================================================
# 攻击A1: 竞态条件 - 并发创建相同taskCode的任务
# 目标: ScheduledTaskServiceImpl.create() 第99-104行
# 原因: 先查询后插入(check-then-act)存在TOCTOU竞态窗口
# 影响: 可能创建重复taskCode的任务，违反唯一约束或导致不可预期行为
# ============================================================
def attack_a1_race_condition():
    """A1: 并发创建相同taskCode的竞态条件攻击"""
    attack_id = "A1"
    attack_name = "竞态条件 - 并发创建重复任务编码"

    test_task_code = f"race-test-{int(time.time())}"
    payload = {
        "taskName": "竞态测试任务",
        "taskCode": test_task_code,
        "jobHandler": "com.example.demo.scheduler.handler.TestHandler",
        "taskType": 3,  # ONE_TIME
        "description": "竞态条件测试"
    }

    success_count = [0]
    error_count = [0]
    lock = threading.Lock()

    def create_task(thread_id):
        try:
            resp = requests.post(
                f"{API_PREFIX}/tasks",
                headers=HEADERS,
                json=payload,
                timeout=10
            )
            with lock:
                if resp.status_code in [200, 201]:
                    success_count[0] += 1
                else:
                    error_count[0] += 1
            return thread_id, resp.status_code, resp.json()
        except Exception as e:
            with lock:
                error_count[0] += 1
            return thread_id, -1, str(e)

    # 发起10个并发请求，使用相同的taskCode
    threads = []
    with ThreadPoolExecutor(max_workers=10) as executor:
        futures = [executor.submit(create_task, i) for i in range(10)]
        for future in as_completed(futures):
            pass

    if success_count[0] > 1:
        log_attack(attack_id, attack_name, "VULNERABLE",
                   f"严重! {success_count[0]}个并发请求全部成功，创建了{success_count[0]}个相同taskCode的任务!"
                   f"确认存在TOCTOU竞态条件漏洞。位置: ScheduledTaskServiceImpl.java:99-104")
    elif success_count[0] == 1 and error_count[0] == 9:
        log_attack(attack_id, attack_name, "RESISTED",
                   "数据库唯一约束成功阻止了重复插入，但应用层仍存在竞态窗口(可能抛出异常)")
    else:
        log_attack(attack_id, attack_name, "PARTIAL",
                   f"成功:{success_count[0]}, 失败:{error_count[0]}, 需要人工分析响应内容")

    # 清理测试数据
    try:
        # 查询刚创建的任务并删除
        resp = requests.get(
            f"{API_PREFIX}/tasks/code/{test_task_code}",
            headers=HEADERS,
            timeout=5
        )
        if resp.status_code == 200:
            task_data = resp.json().get("data", {})
            task_id = task_data.get("taskId")
            if task_id:
                requests.delete(
                    f"{API_PREFIX}/tasks/{task_id}",
                    headers=HEADERS,
                    timeout=5
                )
    except:
        pass


# ============================================================
# 攻击A2: 权限绕过 - 批量操作删除内置任务
# 目标: batchDelete/batchDisable 方法
# 原因: 批量操作未过滤内置任务(isBuiltin=1)
# 影响: 可批量删除/禁用系统内置的关键任务(日志清理、备份等)
# ============================================================
def attack_a2_builtin_bypass():
    """A2: 批量操作绕过内置任务保护"""
    attack_id = "A2"
    attack_name = "权限绕过 - 批量操作内置任务"

    try:
        # 步骤1: 获取所有内置任务列表
        resp = requests.get(
            f"{API_PREFIX}/tasks/builtin",
            headers=HEADERS,
            timeout=5
        )

        if resp.status_code != 200:
            log_attack(attack_id, attack_name, "ERROR",
                      f"无法获取内置任务列表: HTTP {resp.status_code}")
            return

        builtin_tasks = resp.json().get("data", [])
        if not builtin_tasks:
            log_attack(attack_id, attack_name, "SKIPPED", "无内置任务数据")
            return

        builtin_ids = [t["taskId"] for t in builtin_tasks[:3]]  # 取前3个测试

        # 步骤2: 尝试批量删除内置任务 (应该被拒绝!)
        resp = requests.delete(
            f"{API_PREFIX}/tasks/batch-delete",
            headers=HEADERS,
            json=builtin_ids,
            timeout=10
        )

        result_data = resp.json()
        success_count = result_data.get("data", {}).get("successCount", 0) if resp.status_code == 200 else 0

        if success_count > 0:
            log_attack(attack_id, attack_name, "VULNERABLE",
                      f"严重安全漏洞! 成功批量删除了{success_count}个内置任务!"
                      f"IDs: {builtin_ids}. 内置任务保护完全失效!")
        elif resp.status_code in [200, 201]:
            log_attack(attack_id, attack_name, "PARTIAL",
                      f"返回200但successCount=0，说明单条删除时抛出异常被catch吞掉了."
                      f"问题: 异常被静默处理，用户不知道哪些任务被保护/跳过.")
                      f"位置: ScheduledTaskServiceImpl.java:461-469")

        # 步骤3: 尝试批量禁用内置任务
        resp = requests.put(
            f"{API_PREFIX}/tasks/batch-disable",
            headers=HEADERS,
            json=builtin_ids,
            timeout=10
        )

        if resp.status_code in [200, 201]:
            disable_result = resp.json().get("data", {})
            if disable_result.get("successCount", 0) > 0:
                log_attack(attack_id, attack_name + "(禁用)", "VULNERABLE",
                          f"成功批量禁用了{disable_result['successCount']}个内置任务!")

    except Exception as e:
        log_attack(attack_id, attack_name, "ERROR", str(e))


# ============================================================
# 攻击A3: 输入溢出 - 超长字段DoS攻击
# 目标: 所有字符串字段的@Size/@Length限制缺失
# 原因: DTO中只有@NotBlank，没有@Size(max=xxx)限制
# 影响: 可提交超大字符串导致内存溢出、存储异常、日志爆炸
# ============================================================
def attack_a3_input_overflow():
    """A3: 超长输入DoS攻击"""
    attack_id = "A3"
    attack_name = "输入溢出 - 超长字段DoS"

    # 生成各种超长测试字符串
    long_str_100k = "A" * 100000  # 100KB
    long_str_1m = "X" * 1000000   # 1MB (如果100KB没拦住)

    test_cases = [
        {
            "name": "taskName超长(100KB)",
            "payload": {
                "taskName": long_str_100k,
                "taskCode": f"overflow-name-{int(time.time())}",
                "jobHandler": "com.example.demo.handler.TestHandler",
                "taskType": 3
            },
            "field": "taskName",
            "limit": 100  # 数据库VARCHAR(100)
        },
        {
            "name": "taskCode超长(100KB)",
            "payload": {
                "taskName": "OverflowTest",
                "taskCode": long_str_100k,
                "jobHandler": "com.example.demo.handler.TestHandler",
                "taskType": 3
            },
            "field": "taskCode",
            "limit": 100  # 数据库VARCHAR(100)
        },
        {
            "name": "jobHandler超长(100KB)",
            "payload": {
                "taskName": "OverflowTest",
                "taskCode": f"overflow-handler-{int(time.time())}",
                "jobHandler": long_str_100k,
                "taskType": 3
            },
            "field": "jobHandler",
            "limit": 200  # 数据库VARCHAR(200)
        },
        {
            "name": "description超长(100KB)",
            "payload": {
                "taskName": "OverflowTest",
                "taskCode": f"overflow-desc-{int(time.time())}",
                "jobHandler": "com.example.demo.handler.TestHandler",
                "description": long_str_100k,
                "taskType": 3
            },
            "field": "description",
            "limit": 500  # 数据库VARCHAR(500)
        }
    ]

    vulnerable_fields = []
    for tc in test_cases:
        try:
            resp = requests.post(
                f"{API_PREFIX}/tasks",
                headers=HEADERS,
                json=tc["payload"],
                timeout=30
            )

            if resp.status_code in [200, 201]:
                vulnerable_fields.append(tc["field"])
                log_attack(attack_id, f"{attack_name}-{tc['name']}", "VULNERABLE",
                          f"字段'{tc['field']}'接受{len(long_str_100k)}字符输入且成功创建!"
                          f"数据库限制为VARCHAR({tc['limit']}), 应在DTO层添加@Size(max={tc['limit']})")
            elif resp.status_code == 400:
                # 被框架拦截了(可能是Hibernate Validator或其他机制)
                log_attack(attack_id, f"{attack_name}-{tc['name']}", "RESISTED",
                          f"HTTP 400 - 输入被拒绝")
            else:
                log_attack(attack_id, f"{attack_name}-{tc['name']}", "PARTIAL",
                          f"HTTP {resp.status_code}: {resp.text[:200]}")
        except requests.exceptions.Timeout:
            vulnerable_fields.append(tc["field"])
            log_attack(attack_id, f"{attack_name}-{tc['name']}", "VULNERABLE",
                      f"请求超时! 可能导致服务器DoS (输入大小: {len(long_str_100kb)} bytes)")
        except Exception as e:
            log_attack(attack_id, f"{attack_name}-{tc['name']}", "ERROR", str(e))

    if vulnerable_fields:
        log_attack(attack_id, attack_name + "_SUMMARY", "VULNERABLE",
                  f"以下字段缺少长度限制: {vulnerable_fields}. "
                  f"修复: 在ScheduledTaskCreateDTO.java对应字段添加@Size(max=N)注解")


# ============================================================
# 攻击A4: CRON表达式注入
# 目标: cronExpression字段
# 原因: 仅校验非空性，不校验格式合法性
# 影响: 可注入恶意CRON表达式导致任务异常调度
# ============================================================
def attack_a4_cron_injection():
    """A4: CRON表达式注入攻击"""
    attack_id = "A4"
    attack_name = "CRON注入 - 恶意CRON表达式"

    malicious_cron_expressions = [
        ("空字符串", ""),
        ("仅空格", "   "),
        ("非法字符", "0 0 0 * * $"),
        ("SQL注入尝试", "0 0; DROP TABLE scheduled_task; -- * ?"),
        ("路径遍历", "../../../etc/passwd * * ?"),
        ("XSS尝试", "<script>alert(1)</script> * * ?"),
        ("极端频率", "* * * * * ?"),  # 每秒执行
        ("超长表达式", " ".join(["*"] * 100)),
        ("null字节", "0 0 0 \x00 * ?"),
        ("Unicode混淆", "００２＊＊？")  # 全角数字
    ]

    accepted_malicious = []

    for name, cron_expr in malicious_cron_expressions:
        payload = {
            "taskName": f"CRON注入测试-{name}",
            "taskCode": f"cron-inject-{int(time.time())}-{random.randint(1000,9999)}",
            "jobHandler": "com.example.demo.handler.TestHandler",
            "taskType": 1,  # CRON类型
            "cronExpression": cron_expr
        }

        try:
            resp = requests.post(
                f"{API_PREFIX}/tasks",
                headers=HEADERS,
                json=payload,
                timeout=10
            )

            if resp.status_code in [200, 201]:
                accepted_malicious.append(name)
                log_attack(attack_id, f"{attack_name}-{name}", "VULNERABLE",
                          f"恶意CRON表达式被接受: '{repr(cron_expr)[:50]}'. "
                          f"可能导致调度引擎崩溃或异常行为.")
            elif resp.status_code == 400:
                error_msg = resp.json().get("message", "")
                log_attack(attack_id, f"{attack_name}-{name}", "RESISTED",
                          f"被拒绝: {error_msg[:100]}")
            else:
                log_attack(attack_id, f"{attack_name}-{name}", "PARTIAL",
                          f"HTTP {resp.status_code}")

        except Exception as e:
            log_attack(attack_id, f"{attack_name}-{name}", "ERROR", str(e))

    if accepted_malicious:
        log_attack(attack_id, attack_name + "_SUMMARY", "VULNERABLE",
                  f"以下恶意CRON表达式未被拦截: {accepted_malicious}. "
                  f"修复: 在ScheduledTaskServiceImpl.validateTaskTypeParams()添加CRON格式正则校验")


# ============================================================
# 攻击A5: SQL注入探测 - LIKE语句特殊字符
# 目标: selectTaskPage/selectLogPage 的LIKE查询
# 原因: 使用CONCAT('%', #{param}, '%') 参数化查询(安全)
# 但需要验证是否真的参数化了
# ============================================================
def attack_a5_sql_injection():
    """A5: SQL注入探测 - LIKE语句"""
    attack_id = "A5"
    attack_name = "SQL注入 - LIKE特殊字符"

    sql_injection_payloads = [
        ("单引号", "'"),
        ("双引号", '"'),
        ("注释符", "--"),
        ("分号", ";"),
        ("UNION尝试", "' UNION SELECT 1,2,3--"),
        ("百分号通配符", "%"),
        ("下划线通配符", "_"),
        ("反斜杠", "\\"),
        ("LIKE注入", "%' OR '1'='1"),
        ("括号注入", "') OR ('1'='1"),
    ]

    injection_detected = False

    for name, payload in sql_injection_payloads:
        params = {
            "current": 1,
            "size": 10,
            "taskName": payload
        }

        try:
            resp = requests.get(
                f"{API_PREFIX}/tasks",
                headers=HEADERS,
                params=params,
                timeout=10
            )

            if resp.status_code == 500:
                # 500错误可能是SQL语法错误
                log_attack(attack_id, f"{attack_name}-{name}", "PARTIAL",
                          f"HTTP 500 - 可能触发SQL错误: {resp.text[:200]}")
                injection_detected = True
            elif resp.status_code in [200, 201]:
                # 正常返回，检查是否有异常数据泄露
                data = resp.json()
                records = data.get("data", {}).get("records", [])
                if any("UNION" in str(r) or "SELECT" in str(r) or "error" in str(r).lower()
                       for r in records):
                    log_attack(attack_id, f"{attack_name}-{name}", "VULNERABLE",
                              "检测到可能的SQL注入响应!")
                    injection_detected = True
                else:
                    log_attack(attack_id, f"{attack_name}-{name}", "RESISTED",
                              "正常返回，参数化查询生效")
            else:
                log_attack(attack_id, f"{attack_name}-{name}", "RESISTED",
                          f"HTTP {resp.status_code} - 安全")

        except Exception as e:
            log_attack(attack_id, f"{attack_name}-{name}", "ERROR", str(e))

    if not injection_detected:
        log_attack(attack_id, attack_name + "_SUMMARY", "PASSED",
                  "所有SQL注入载荷均被正确参数化处理. XML中使用#{param}是安全的.")


# ============================================================
# 攻击A6: 类型混淆 - 非法枚举值注入
# 目标: status/taskType/concurrentPolicy等SMALLINT字段
# 原因: DTO和Service只做基本非空校验，不做范围校验
# 影响: 可写入非法状态值导致状态机混乱
# ============================================================
def attack_a6_type_confusion():
    """A6: 枚举值越界攻击"""
    attack_id = "A6"
    attack_name = "类型混淆 - 非法枚举值注入"

    illegal_values = [
        ("负数status", {"taskType": 3, "status": -1}),
        ("超大status", {"taskType": 3, "status": 999}),
        ("零值taskType", {"taskType": 0}),
        ("超大taskType", {"taskType": 999}),
        ("负数concurrentPolicy", {"taskType": 3, "concurrentPolicy": -5}),
        ("超大retryCount", {"taskType": 3, "maxRetryCount": 999999}),
        ("负数intervalSeconds", {"taskType": 2, "intervalSeconds": -100}),
        ("零intervalSeconds(FIXED_RATE)", {"taskType": 2, "intervalSeconds": 0}),
        ("浮点数taskType", {"taskType": 1.5}),  # JSON数字
        ("布尔值taskType", {"taskType": True}),  # JSON布尔
    ]

    accepted_illegal = []

    for name, extra_fields in illegal_values:
        base_payload = {
            "taskName": f"类型混淆测试-{name}",
            "taskCode": f"type-confusion-{int(time.time())}-{random.randint(1000,9999)}",
            "jobHandler": "com.example.demo.handler.TestHandler",
        }
        base_payload.update(extra_fields)

        try:
            resp = requests.post(
                f"{API_PREFIX}/tasks",
                headers=HEADERS,
                json=base_payload,
                timeout=10
            )

            if resp.status_code in [200, 201]:
                accepted_illegal.append(name)
                log_attack(attack_id, f"{attack_name}-{name}", "VULNERABLE",
                          f"非法值被接受: {extra_fields}. 可能导致数据库约束违反或业务逻辑异常.")
            elif resp.status_code == 400:
                log_attack(attack_id, f"{attack_name}-{name}", "RESISTED",
                          f"被拒绝(HTTP 400)")
            else:
                log_attack(attack_id, f"{attack_name}-{name}", "PARTIAL",
                          f"HTTP {resp.status_code}")

        except Exception as e:
            log_attack(attack_id, f"{attack_name}-{name}", "ERROR", str(e))

    if accepted_illegal:
        log_attack(attack_id, attack_name + "_SUMMARY", "VULNERABLE",
                  f"以下非法枚举值被接受: {accepted_illegal}. "
                  f"修复: DTO添加@Min/@Max注解或Service层添加白名单校验")


# ============================================================
# 攻击A7: 架构违规探测 - Controller直连Mapper影响评估
# 目标: ScheduledTaskController第47行直接注入TaskExecutionLogMapper
# 原因: 违反分层架构(Controller->Service->Mapper)，可能绕过Service层的
#       事务控制、权限检查、缓存逻辑
# 影响: getLogDetail端点直接访问Mapper，绕过可能的Service层逻辑
# ============================================================
def attack_a7_architecture_violation():
    """A7: 架构违规 - Controller直连Mapper探测"""
    attack_id = "A7"
    attack_name = "架构违规 - Controller直连Mapper"

    try:
        # 测试1: 访问不存在的日志ID
        fake_log_id = 999999999

        resp = requests.get(
            f"{API_PREFIX}/logs/{fake_log_id}",
            headers=HEADERS,
            timeout=5
        )

        if resp.status_code == 404:
            log_attack(attack_id, f"{attack_name}-不存在ID", "INFO",
                      "返回404 - 但这是Controller处理的还是Mapper抛出的?")
        elif resp.status_code == 500:
            log_attack(attack_id, f"{attack_name}-不存在ID", "PARTIAL",
                      "返回500 - Mapper异常未被正确包装. 应通过Service层统一处理.")

        # 测试2: 访问边界值ID
        boundary_ids = [0, -1, 2147483647, -2147483648]

        for bid in boundary_ids:
            resp = requests.get(
                f"{API_PREFIX}/logs/{bid}",
                headers=HEADERS,
                timeout=5
            )
            if resp.status_code == 500:
                log_attack(attack_id, f"{attack_name}-边界ID({bid})", "PARTIAL",
                          f"边界值ID={bid}导致HTTP 500. 缺少输入校验.")
            elif resp.status_code == 400:
                log_attack(attack_id, f"{attack_name}-边界ID({bid})", "RESISTED",
                          "边界值被正确拒绝")
            else:
                log_attack(attack_id, f"{attack_name}-边界ID({bid})", "INFO",
                          f"HTTP {resp.status_code}")

        # 静态代码证据
        log_attack(attack_id, attack_name + "_STATIC", "CONFIRMED",
                  "静态分析确认: ScheduledTaskController.java:47 直接注入TaskExecutionLogMapper."
                  "第336行直接调用executionLogMapper.selectById(logId)."
                  "这违反项目规范'禁止Controller中直接注入Mapper'和分层架构原则."
                  "风险: 绕过Service层的事务管理、缓存、审计日志等横切关注点.",
                  severity="MEDIUM")

    except Exception as e:
        log_attack(attack_id, attack_name, "ERROR", str(e))


# ============================================================
# 主执行入口
# ============================================================
def main():
    print("=" * 70)
    print("定时任务调度模块 - 对抗性验证攻击套件")
    print(f"目标: {API_PREFIX}")
    print(f"时间: {time.strftime('%Y-%m-%d %H:%M:%S')}")
    print("=" * 70)
    print()

    # 连通性测试
    try:
        resp = requests.get(f"{API_PREFIX}/statistics", headers=HEADERS, timeout=5)
        if resp.status_code in [200, 401, 403]:
            print(f"[OK] 服务连通正常 (HTTP {resp.status_code})")
        else:
            print(f"[WARN] 服务返回异常: HTTP {resp.status_code}")
    except requests.exceptions.ConnectionError:
        print("[FATAL] 无法连接到服务器! 请确保后端服务已启动.")
        print(f"       预期地址: {BASE_URL}")
        sys.exit(1)
    except Exception as e:
        print(f"[ERROR] 连通性测试失败: {e}")
        sys.exit(1)

    print()
    print("-" * 70)
    print("开始执行攻击向量...")
    print("-" * 70)
    print()

    # 执行所有攻击
    attack_a1_race_condition()
    print()

    attack_a2_builtin_bypass()
    print()

    attack_a3_input_overflow()
    print()

    attack_a4_cron_injection()
    print()

    attack_a5_sql_injection()
    print()

    attack_a6_type_confusion()
    print()

    attack_a7_architecture_violation()
    print()

    # ==================== 汇总报告 ====================
    print("=" * 70)
    print("攻击执行完毕 - 汇总报告")
    print("=" * 70)

    vulnerable = [r for r in attack_results if r["status"] == "VULNERABLE"]
    resisted = [r for r in attack_results if r["status"] == "RESISTED"]
    partial = [r for r in attack_results if r["status"] in ["PARTIAL", "CONFIRMED"]]
    errors = [r for r in attack_results if r["status"] == "ERROR"]

    print(f"\n总攻击向量: {len(attack_results)}")
    print(f"[!!!] 存在漏洞 (VULNERABLE): {len(vulnerable)}")
    print(f"[+++] 已抵抗 (RESISTED):    {len(resisted)}")
    print(f"[???] 部分问题 (PARTIAL):   {len(partial)}")
    print(f"[ERR] 执行错误 (ERROR):      {len(errors)}")

    if vulnerable:
        print("\n" + "-" * 70)
        print("严重漏洞清单:")
        print("-" * 70)
        for v in vulnerable:
            print(f"  [{v['id']}] {v['name']}")
            print(f"         -> {v['detail']}")
            print()

    if partial:
        print("-" * 70)
        print("部分问题清单:")
        print("-" * 70)
        for p in partial:
            print(f"  [{p['id']}] {p['name']}")
            print(f"         -> {p['detail']}")
            print()

    print("=" * 70)
    if vulnerable:
        print("[VERDICT] FAIL - 发现生产级安全漏洞，必须修复后再上线")
    elif partial:
        print("[VERDICT] PARTIAL - 存在需关注的问题，建议修复")
    else:
        print("[VERDICT] PASS - 所有攻击向量均被有效防御")
    print("=" * 70)


if __name__ == "__main__":
    main()
