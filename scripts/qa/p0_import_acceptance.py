#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
P0-C 独立验收：CSV 导入（AC-1～AC-6）+ AC-37 既有防线回归 + 「无写账」证据

设计契约来源：docs/design/finance-reconciliation-design.md §1 / §5.1–§5.7 / §8
  · 接口：POST /v1/finance/bank-transactions/import（multipart：file/sourceType/accountId）
          GET  /v1/finance/imports/{batchId}
          POST /v1/finance/imports/{batchId}/commit
          GET  /v1/finance/bank-transactions
  · 金额：DB 一律「分」BIGINT；CSV 元值 ×100 必须是整数（千分位 / 3 位小数 / 全角数字 → 行级 ERROR）
  · P0 只读导入：**不做任何写账动作**（fund_flows / bank_accounts.balance 必须零变化）

断言：
  AC-1 三类样例（微信 UTF-8+BOM / 支付宝 UTF-8 / 银行 GB18030）→ import_batches.total_rows == 人工计数
  AC-2 抽 ≤100 行：bank_transactions.amount == round(CSV 元 × 100)，不符行数 = 0
  AC-3 同一文件导两次：第二次 dup_rows == total_rows，且 bank_transactions 行数不变
  AC-4 5 类错误行 → error_rows = 5，且每条 error_code 非空
  AC-5 缺必需列 → status = PARSED_FAILED，bank_transactions 增量 = 0
  AC-6 审计可回溯：batch_id / create_user_id 非空，孤儿行数 = 0
  AC-37 consistency_check.py 5 PASS + ui_e2e_payment.py 7/7 + schema_guard.py 5/5
  额外 无写账：导入前后 fund_flows 行数、bank_accounts.balance 合计零变化

用法：
  python scripts/qa/p0_import_acceptance.py --token <自签 JWT，含 finance 权限>
  python scripts/qa/p0_import_acceptance.py --admin-user qa_admin --admin-password Admin@123456
  python scripts/qa/p0_import_acceptance.py --no-ac37        # 只跑 AC-1~AC-6
退出码：0 全 PASS / 1 有 FAIL / 2 执行错误
"""
from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
import time
import urllib.error
import urllib.request
import uuid
from datetime import datetime, timedelta

DEFAULT_BASE = "http://127.0.0.1:8081/api"
DEFAULT_PSQL = r"P:\my-new-project\PostgreSQL\18\bin\psql.exe"
BASE = DEFAULT_BASE


# ---------------------------------------------------------------- HTTP
def call(method, path, token=None, body=None, timeout=60):
    url = path if path.startswith("http") else BASE + path
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None
    headers = {"Content-Type": "application/json", "Accept": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            raw = resp.read().decode("utf-8", "replace")
            try:
                return resp.status, json.loads(raw)
            except Exception:
                return resp.status, {"_raw": raw[:400]}
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", "replace")
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {"_raw": raw[:400]}
    except Exception as e:
        return -1, {"error": str(e)}


def upload_csv(path, filename, token, source_type, account_id, timeout=120):
    """multipart/form-data 上传 CSV（不用 requests）"""
    boundary = "----P0Acceptance" + uuid.uuid4().hex
    with open(path, "rb") as fp:
        content = fp.read()
    parts = []
    for key, val in (("sourceType", source_type), ("accountId", str(account_id))):
        parts.append(("--" + boundary + "\r\n").encode())
        parts.append(('Content-Disposition: form-data; name="%s"\r\n\r\n' % key).encode())
        parts.append((str(val) + "\r\n").encode())
    parts.append(("--" + boundary + "\r\n").encode())
    parts.append(('Content-Disposition: form-data; name="file"; filename="%s"\r\n' % filename).encode())
    parts.append(b"Content-Type: text/csv\r\n\r\n")
    parts.append(content + b"\r\n")
    parts.append(("--" + boundary + "--\r\n").encode())
    body = b"".join(parts)
    req = urllib.request.Request(BASE + "/v1/finance/bank-transactions/import", data=body, method="POST")
    req.add_header("Content-Type", "multipart/form-data; boundary=" + boundary)
    req.add_header("Accept", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            raw = resp.read().decode("utf-8", "replace")
            try:
                return resp.status, json.loads(raw)
            except Exception:
                return resp.status, {"_raw": raw[:400]}
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", "replace")
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {"_raw": raw[:400]}
    except Exception as e:
        return -1, {"error": str(e)}


def ok_code(body):
    return isinstance(body, dict) and body.get("code") in (0, 200)


FORBIDDEN_HINT = (
    "权限码未绑定角色，请先执行权限脚本"
    "（把 finance:bank:view / finance:bank:import / finance:bank:recon 等授给 ROLE_FINANCE_DIRECTOR 后重跑；"
    "C 卡之后权限是实时查库，token claims 不生效）"
)


def is_forbidden(status, body):
    """403 判定：HTTP 403，或统一响应体 code=403 / message 含「权限不足」。"""
    if status == 403:
        return True
    if isinstance(body, dict):
        if body.get("code") == 403:
            return True
        if "权限不足" in str(body.get("message") or ""):
            return True
    return False


def is_endpoint_missing(status, body):
    """端点未部署判定：HTTP 404/405/501，或项目统一响应体里 code=404「资源不存在」。"""
    if status in (-1, 404, 405, 501):
        return True
    if isinstance(body, dict):
        code = body.get("code")
        if code in (404, 405, 501):
            return True
        msg = str(body.get("message") or "")
        if ("资源不存在" in msg) or ("Not Found" in msg) or ("not found" in msg.lower()):
            return True
    return False


def data_of(body):
    return body.get("data") if isinstance(body, dict) else None


def dump(obj, limit=400):
    try:
        return json.dumps(obj, ensure_ascii=False)[:limit]
    except Exception:
        return str(obj)[:limit]


# ---------------------------------------------------------------- CSV 样例
def write_text(path, text, encoding):
    with open(path, "w", encoding=encoding, newline="") as fp:
        fp.write(text)


def _tag(x, tag):
    return ("%s%s" % (x, tag)) if tag else x


def sample_wechat(path, day, tag=""):
    """微信：UTF-8 + BOM；列名按附录 A.1"""
    rows = [
        ["交易时间", "微信订单号", "商户订单号", "交易类型", "交易状态", "应结订单金额", "手续费", "订单金额", "商品名称"],
        ["%s 09:12:01" % day, _tag("4200001", tag), _tag("M20261024001", tag), "支付", "SUCCESS", "128.00", "0.76", "128.00", "招牌牛肉面"],
        ["%s 09:15:33" % day, _tag("4200002", tag), _tag("M20261024002", tag), "支付", "SUCCESS", "56.50", "0.34", "56.50", "冰美式"],
        ["%s 10:02:10" % day, _tag("4200003", tag), _tag("M20261024003", tag), "支付", "SUCCESS", "19.90", "0.12", "19.90", "卤蛋"],
        ["%s 11:30:00" % day, _tag("4200004", tag), _tag("M20261024004", tag), "退款", "REFUND", "0.01", "0.00", "0.01", "退款-冰美式"],
        ["%s 12:00:00" % day, _tag("4200005", tag), _tag("M20261024005", tag), "支付", "SUCCESS", "888.88", "5.33", "888.88", "团餐"],
    ]
    text = "\ufeff" + "\r\n".join(",".join(r) for r in rows) + "\r\n"
    write_text(path, text, "utf-8")
    return len(rows) - 1


def sample_alipay(path, day, tag=""):
    rows = [
        ["支付宝交易号", "商家订单号", "业务类型", "商品名称", "创建时间", "完成时间", "对方账户", "订单金额（元）", "服务费（元）", "备注"],
        [_tag("2026102422001", tag), _tag("A20261024001", tag), "交易", "套餐A", "%s 08:00:00" % day, "%s 08:00:05" % day, "138****0001", "66.00", "0.40", "早餐"],
        [_tag("2026102422002", tag), _tag("A20261024002", tag), "交易", "套餐B", "%s 08:10:00" % day, "%s 08:10:07" % day, "138****0002", "99.90", "0.60", "早餐"],
        [_tag("2026102422003", tag), _tag("A20261024003", tag), "交易", "单点", "%s 12:20:00" % day, "%s 12:20:09" % day, "138****0003", "12.34", "0.07", "午餐"],
        [_tag("2026102422004", tag), _tag("A20261024004", tag), "交易", "单点", "%s 18:40:00" % day, "%s 18:40:03" % day, "138****0004", "45.00", "0.27", "晚餐"],
        [_tag("2026102422005", tag), _tag("A20261024005", tag), "交易", "外卖", "%s 19:05:00" % day, "%s 19:05:11" % day, "138****0005", "150.00", "0.90", "外卖"],
    ]
    text = "\r\n".join(",".join(r) for r in rows) + "\r\n"
    write_text(path, text, "utf-8")
    return len(rows) - 1


def sample_bank(path, day, tag=""):
    """银行：GB18030（银行常见编码）；借方=支出、贷方=收入（附录 A.3）"""
    rows = [
        ["交易日期", "交易时间", "交易流水号", "借方金额", "贷方金额", "余额", "对方户名", "对方账号", "摘要", "交易类型"],
        [day, "09:30:00", _tag("B20261024001", tag), "", "5000.00", "105000.00", "某某餐饮", _tag("6222****0001", tag), "货款", "转入"],
        [day, "10:30:00", _tag("B20261024002", tag), "1200.50", "", "103799.50", "某某供应", _tag("6222****0002", tag), "采购付款", "转出"],
        [day, "11:30:00", _tag("B20261024003", tag), "15.00", "", "103784.50", "银行", "", "手续费", "手续费"],
        [day, "14:00:00", _tag("B20261024004", tag), "", "88.88", "103873.38", "利息", "", "结息", "利息"],
        [day, "16:00:00", _tag("B20261024005", tag), "", "0.10", "103873.48", "测试", _tag("6222****0005", tag), "小额入账", "转入"],
    ]
    text = "\r\n".join(",".join(r) for r in rows) + "\r\n"
    write_text(path, text, "gb18030")
    return len(rows) - 1


def sample_errors(path, day):
    """AC-4：5 类错误行（千分位 / 3 位小数 / 未来时间 / 缺必需字段值 / 无法判方向）"""
    future = (datetime.strptime(day, "%Y-%m-%d") + timedelta(days=365)).strftime("%Y-%m-%d")
    rows = [
        ["交易日期", "交易时间", "交易流水号", "借方金额", "贷方金额", "余额", "对方户名", "对方账号", "摘要", "交易类型"],
        [day, "09:00:00", "E1-THOUSAND", "1,234.56", "", "1000.00", "A", "", "千分位", "转出"],
        [day, "09:01:00", "E2-3DEC", "12.345", "", "1000.00", "B", "", "三位小数", "转出"],
        [future, "09:02:00", "E3-FUTURE", "", "10.00", "1000.00", "C", "", "未来时间", "转入"],
        [day, "09:03:00", "E4-NOAMT", "", "", "1000.00", "D", "", "缺金额", "转入"],
        [day, "09:04:00", "E5-NODIR", "10.00", "10.00", "1000.00", "E", "", "借贷同时有值-无法判方向", "未知"],
    ]
    text = "\r\n".join(",".join(r) for r in rows) + "\r\n"
    write_text(path, text, "utf-8")
    return len(rows) - 1


def sample_missing_column(path, day):
    """AC-5：缺必需列（没有「交易流水号」列 → 整批 PARSED_FAILED）"""
    rows = [
        ["交易日期", "交易时间", "借方金额", "贷方金额", "余额", "对方户名", "对方账号", "摘要", "交易类型"],
        [day, "09:00:00", "10.00", "", "1000.00", "X", "", "缺流水号列", "转出"],
        [day, "09:01:00", "", "20.00", "1020.00", "Y", "", "缺流水号列2", "转入"],
    ]
    text = "\r\n".join(",".join(r) for r in rows) + "\r\n"
    write_text(path, text, "utf-8")
    return len(rows) - 1


# ---------------------------------------------------------------- Runner
class Runner:
    def __init__(self, a):
        self.a = a
        self.results = []
        self.token = None
        self.samples = {}
        self.batches = {}
        self.not_ready = []
        self.perm_hint_shown = False

    def step(self, name, ok, detail, state=None):
        st = state or ("PASS" if ok else "FAIL")
        self.results.append((name, st, detail))
        if st == "NOT_READY":
            self.not_ready.append(name)
        print("  [%s] %s -> %s" % (st, name, detail))
        return ok

    def not_ready_step(self, name, detail):
        return self.step(name, False, detail, state="NOT_READY")

    def forbidden_step(self, name, status, body):
        """403：单独打一行权限提示，按 NOT_READY 归类（不算验收 FAIL）。"""
        if not self.perm_hint_shown:
            print("  [HINT] %s" % FORBIDDEN_HINT)
            self.perm_hint_shown = True
        return self.not_ready_step(name, "HTTP %s 403 权限不足 → %s %s" % (
            status, FORBIDDEN_HINT, dump(body, 160)))

    # ---------- psql ----------
    def psql(self, sql):
        try:
            out = subprocess.run(
                [self.a.psql, "-h", self.a.pg_host, "-p", str(self.a.pg_port), "-U", self.a.pg_user,
                 "-d", self.a.pg_db, "-t", "-A", "-F", "|", "-c", sql],
                capture_output=True, text=True, timeout=30,
                encoding="utf-8", errors="replace",
                env=dict(os.environ, PGPASSWORD=self.a.pg_password, PGCLIENTENCODING="UTF8"))
            return out.returncode, (out.stdout or "").strip(), (out.stderr or "").strip()
        except Exception as e:
            return -1, "", str(e)

    def scalar(self, sql, default=None):
        rc, out, err = self.psql(sql)
        if rc != 0:
            raise RuntimeError("psql rc=%s %s" % (rc, (err or out)[:200]))
        return default if out == "" else out

    def table_exists(self, name):
        try:
            return int(self.scalar(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public' AND table_name='%s'" % name, "0")) > 0
        except Exception:
            return False

    # ---------- 登录 ----------
    def login(self):
        if self.a.token:
            self.token = self.a.token.strip()
            return self.step("使用自签 token（--token）", True, "token=%d 字符" % len(self.token))
        st, body = call("POST", "/v1/auth/login", body={
            "username": self.a.admin_user, "password": self.a.admin_password})
        self.token = (data_of(body) or {}).get("token") if ok_code(body) else None
        return self.step("管理端登录（%s）" % self.a.admin_user, bool(self.token),
                         "HTTP %s %s" % (st, "" if self.token else dump(body)))

    # ---------- 前置：表与基线 ----------
    def precheck(self):
        need = ["import_batches", "import_rows", "bank_transactions",
                "recon_runs", "recon_items", "recon_differences", "deposit_slips"]
        missing = [t for t in need if not self.table_exists(t)]
        self.step("P0 七张表存在（§5.1–§5.7）", not missing,
                  "缺表: %s" % (",".join(missing) if missing else "无"))
        # §5.10 两表不应存在（P0 不碰）
        extra = [t for t in ("reconciliation_statements", "reconciliation_items") if self.table_exists(t)]
        self.step("§5.10 两表未创建（P0 范围锁）", not extra,
                  "不应存在却存在: %s" % (",".join(extra) if extra else "无"))
        try:
            self.fund_flows_before = int(self.scalar("SELECT COUNT(*) FROM fund_flows", "0"))
            self.balance_before = int(self.scalar("SELECT COALESCE(SUM(balance),0) FROM bank_accounts", "0"))
            self.txn_before = int(self.scalar("SELECT COUNT(*) FROM bank_transactions", "0")) if not missing else 0
        except Exception as e:
            return self.step("取写账基线", False, str(e))
        self.step("取写账基线（fund_flows 行数 / 账户余额合计）", True,
                  "fund_flows=%d bank_accounts.balance 合计=%d 分 bank_transactions=%d" % (
                      self.fund_flows_before, self.balance_before, self.txn_before))
        return not missing

    # ---------- 导入工具 ----------
    def do_import(self, path, source_type):
        st, body = upload_csv(path, os.path.basename(path), self.token, source_type, self.a.account_id)
        data = data_of(body) or {}
        batch_id = data.get("batchId") or data.get("id")
        return st, body, data, batch_id

    def batch_detail(self, batch_id):
        st, body = call("GET", "/v1/finance/imports/%s" % batch_id, token=self.token)
        return (data_of(body) or {}) if ok_code(body) else {}

    def commit(self, batch_id):
        st, body = call("POST", "/v1/finance/imports/%s/commit" % batch_id, token=self.token, body={})
        return st, body, (data_of(body) or {})

    def batch_rows(self, batch_id, status=None, size=200):
        q = "/v1/finance/imports/%s/rows?page=1&size=%d" % (batch_id, size)
        if status:
            q += "&status=%s" % status
        st, body = call("GET", q, token=self.token)
        data = data_of(body) or {}
        return data.get("records") or [], data.get("total")

    # ---------- 服务就绪预检 ----------
    def preflight_backend(self):
        """8081 未监听时明确报「服务未就绪」，不当成验收失败。"""
        st, body = call("POST", "/v1/auth/login", body={"username": "_probe_", "password": "_probe_"}, timeout=10)
        if st == -1:
            self.not_ready_step("后端 8081 服务就绪", "连接失败：%s" % dump(body, 160))
            return False
        return True

    # ---------- 端点就绪预检（I1–I6）----------
    def preflight_endpoints(self):
        """探活 I1–I6；未部署时明确报「端点未就绪」，绝不让后续断言伪造 PASS。"""
        probes = [
            ("I6 列表 GET /v1/finance/bank-transactions", lambda: call(
                "GET", "/v1/finance/bank-transactions?page=1&size=1", token=self.token)),
            ("I1 导入 POST /v1/finance/bank-transactions/import", None),  # 用下面的一次最小上传探活
        ]
        ready = True
        st, body = probes[0][1]()
        if is_forbidden(st, body):
            self.forbidden_step("%s 403 权限不足" % probes[0][0], st, body)
            self.not_ready_step("I1–I6 端点就绪（等待权限授权）", "403 → 端点已部署但权限码未绑定角色")
            return False
        ok_list = not is_endpoint_missing(st, body)
        if ok_list:
            ready = self.step("%s 已部署" % probes[0][0], True, "HTTP %s code=0" % st) and ready
        else:
            ready = self.not_ready_step("%s 未就绪" % probes[0][0],
                                        "HTTP %s code=%s %s" % (st, (body or {}).get("code"), dump(body, 160))) and ready

        # 最小 CSV 探活 I1（1 行数据）：只判断端点是否 404/405/501，不校验业务
        probe_dir = self.a.work_dir
        os.makedirs(probe_dir, exist_ok=True)
        probe_path = os.path.join(probe_dir, "_probe.csv")
        write_text(probe_path, "交易日期,交易时间,交易流水号,借方金额,贷方金额,余额\r\n%s,00:00:00,PROBE-1,1.00,,0\r\n"
                   % self.a.business_date, "utf-8")
        pst, pbody = upload_csv(probe_path, "_probe.csv", self.token, "BANK", self.a.account_id)
        if is_forbidden(pst, pbody):
            self.forbidden_step("%s 403 权限不足" % probes[1][0], pst, pbody)
            self.not_ready_step("I1–I6 端点就绪（等待权限授权）", "403 → 端点已部署但权限码未绑定角色")
            return False
        ok_import = not is_endpoint_missing(pst, pbody)
        if ok_import:
            ready = self.step("%s 已部署" % probes[1][0], True, "HTTP %s code=0" % pst) and ready
        else:
            ready = self.not_ready_step("%s 未就绪" % probes[1][0],
                                        "HTTP %s code=%s %s" % (pst, (pbody or {}).get("code"), dump(pbody, 160))) and ready
        if not ready:
            self.not_ready_step("I1–I6 端点就绪（等待 fund-500 落盘）", "命中 404/405/501 → 端点未就绪，跳过全部 AC 断言")
        return ready

    # ---------- AC-1 ----------
    def ac1(self):
        day = self.a.business_date
        work = self.a.work_dir
        os.makedirs(work, exist_ok=True)
        specs = [
            ("WECHAT", os.path.join(work, "p0_wechat_bom.csv"), sample_wechat, "UTF-8+BOM"),
            ("ALIPAY", os.path.join(work, "p0_alipay.csv"), sample_alipay, "UTF-8"),
            ("BANK", os.path.join(work, "p0_bank_gb18030.csv"), sample_bank, "GB18030"),
        ]
        ok_all = True
        for source, path, fn, enc in specs:
            expect = fn(path, day, self.a.id_tag)
            st, body, data, batch_id = self.do_import(path, source)
            if is_forbidden(st, body):
                self.forbidden_step("AC-1 %s 导入 403" % source, st, body)
                return False
            if is_endpoint_missing(st, body):
                self.not_ready_step("AC-1 %s 导入端点未就绪" % source, "HTTP %s %s" % (st, dump(body, 160)))
                return False
            got = data.get("totalRows", data.get("total_rows"))
            encoding_used = data.get("encodingUsed") or data.get("encoding_used")
            ok = ok_code(body) and batch_id and got is not None and int(got) == expect
            self.samples[source] = {"path": path, "expect": expect, "batch_id": batch_id}
            ok_all = self.step("AC-1 %s（%s）total_rows == %d" % (source, enc, expect), ok,
                               "HTTP %s batchId=%s totalRows=%s 期望=%d 编码实测=%s %s" % (
                                   st, batch_id, got, expect, encoding_used,
                                   "" if ok else dump(body))) and ok_all
            # 提交，让事实行落库供 AC-2/3/6 使用
            if ok and batch_id:
                cst, cbody, cdata = self.commit(batch_id)
                self.step("AC-1 %s 提交批次" % source, ok_code(cbody),
                          "HTTP %s status=%s ok=%s dup=%s err=%s %s" % (
                              cst, cdata.get("status"), cdata.get("okRows", cdata.get("ok_rows")),
                              cdata.get("dupRows", cdata.get("dup_rows")),
                              cdata.get("errorRows", cdata.get("error_rows")),
                              "" if ok_code(cbody) else dump(cbody)))
        return ok_all

    # ---------- AC-2 ----------
    def ac2(self):
        """bank_transactions.amount(分) == round(CSV 元 × 100)，不符行数 = 0"""
        expected = {}
        day = self.a.business_date
        for path in (self.samples.get("WECHAT", {}).get("path"),
                     self.samples.get("ALIPAY", {}).get("path"),
                     self.samples.get("BANK", {}).get("path")):
            if not path or not os.path.exists(path):
                continue
            enc = "gb18030" if "gb18030" in path else "utf-8-sig"
            with open(path, encoding=enc) as fp:
                rows = [r for r in fp.read().splitlines() if r.strip()]
            header = rows[0].split(",")
            for line in rows[1:]:
                cols = line.split(",")
                rec = dict(zip(header, cols))
                ext = (rec.get("微信订单号") or rec.get("支付宝交易号") or rec.get("交易流水号") or "").strip()
                if not ext:
                    continue
                yuan = rec.get("订单金额") or rec.get("订单金额（元）") or ""
                if not yuan:
                    cr = (rec.get("贷方金额") or "").strip()
                    dr = (rec.get("借方金额") or "").strip()
                    yuan = cr or dr
                yuan = (yuan or "").strip()
                if not yuan:
                    continue
                expected[ext] = int(round(float(yuan) * 100))
        if not expected:
            return self.step("AC-2 金额换算无损（元×100=分）", False, "未能从样例 CSV 推出期望金额")
        ext_list = ",".join("'%s'" % e.replace("'", "''") for e in list(expected)[:100])
        # 注意：库内 external_txn_id 带来源前缀（WX:/ALI:/BANK:，跨来源不互认 TD-13），
        # 而样例 CSV 给的是渠道原号；因此比对时去掉前缀再 IN，否则 LIKE 与 IN 两个谓词互相排斥（恒为 0 命中）。
        rc, out, err = self.psql(
            "SELECT external_txn_id, amount FROM bank_transactions "
            "WHERE deleted = 0 AND (CASE WHEN position(':' in external_txn_id) > 0 "
            "THEN split_part(external_txn_id, ':', 2) ELSE external_txn_id END) IN (%s)" % ext_list)
        if rc != 0:
            return self.step("AC-2 金额换算无损（元×100=分）", False, "psql rc=%s %s" % (rc, (err or out)[:200]))
        seen, bad = 0, []
        for line in (out or "").splitlines():
            if not line.strip():
                continue
            ext_id, amt = (line.split("|") + ["", ""])[:2]
            key = ext_id.split(":", 1)[1] if ":" in ext_id else ext_id
            if key in expected:
                seen += 1
                if int(amt) != expected[key]:
                    bad.append("%s db=%s 期望=%s" % (ext_id, amt, expected[key]))
        ok = seen > 0 and not bad
        return self.step("AC-2 金额换算无损（元×100=分，抽 %d 行）" % min(100, len(expected)), ok,
                         "命中=%d 不符=%d %s" % (seen, len(bad), "; ".join(bad[:5])))

    # ---------- AC-3 ----------
    def ac3(self):
        info = self.samples.get("WECHAT") or {}
        path = info.get("path")
        if not path:
            return self.step("AC-3 同一文件导两次幂等", False, "缺少 WECHAT 样例")
        before = int(self.scalar("SELECT COUNT(*) FROM bank_transactions", "0"))
        st, body, data, batch_id = self.do_import(path, "WECHAT")
        total = data.get("totalRows", data.get("total_rows"))
        dup = data.get("dupRows", data.get("dup_rows"))
        after = int(self.scalar("SELECT COUNT(*) FROM bank_transactions", "0"))
        # 文件级去重（file_sha256）允许直接返回"已导过"，此时若返回原批次也视为幂等
        note = ""
        if not ok_code(body):
            note = "接口拒绝（可能为 file_sha256 文件级去重）：" + dump(body, 200)
            ok = "已导" in json.dumps(body, ensure_ascii=False) or "duplicate" in json.dumps(body).lower()
            return self.step("AC-3 同一文件导两次幂等", ok and after == before,
                             "第二次导入未产生批次；bank_transactions %d→%d；%s" % (before, after, note))
        ok = (dup is not None and total is not None and int(dup) == int(total) and after == before)
        return self.step("AC-3 同一文件导两次幂等", ok,
                         "第二次 batchId=%s total=%s dup=%s；bank_transactions %d→%d（增量 %d）" % (
                             batch_id, total, dup, before, after, after - before))

    # ---------- AC-4 ----------
    def ac4(self):
        path = os.path.join(self.a.work_dir, "p0_errors_5.csv")
        expect = sample_errors(path, self.a.business_date)
        st, body, data, batch_id = self.do_import(path, "BANK")
        err_rows = data.get("errorRows", data.get("error_rows"))
        records, _ = self.batch_rows(batch_id, status="ERROR") if batch_id else ([], 0)
        codes = [(r.get("errorCode") or r.get("error_code")) for r in records]
        if batch_id and not records:
            records, _ = self.batch_rows(batch_id)
            codes = [(r.get("errorCode") or r.get("error_code")) for r in records
                     if str(r.get("status")) == "ERROR"]
        non_empty = [c for c in codes if c]
        ok = ok_code(body) and err_rows is not None and int(err_rows) == expect and len(non_empty) == expect
        return self.step("AC-4 行级错误可读（5 类）", ok,
                         "HTTP %s batchId=%s error_rows=%s 期望=%d；error_code 非空 %d/%d：%s %s" % (
                             st, batch_id, err_rows, expect, len(non_empty), len(codes),
                             ",".join(str(c) for c in non_empty[:5]),
                             "" if ok else dump(body, 300)))

    # ---------- AC-5 ----------
    def ac5(self):
        path = os.path.join(self.a.work_dir, "p0_missing_column.csv")
        sample_missing_column(path, self.a.business_date)
        before = int(self.scalar("SELECT COUNT(*) FROM bank_transactions", "0"))
        st, body, data, batch_id = self.do_import(path, "BANK")
        status = (data.get("status") or "").upper()
        after = int(self.scalar("SELECT COUNT(*) FROM bank_transactions", "0"))
        if batch_id and ok_code(body):
            detail = self.batch_detail(batch_id)
            status = (detail.get("status") or status).upper()
        ok = status == "PARSED_FAILED" and after == before
        return self.step("AC-5 缺必需列整批失败且零落库", ok,
                         "HTTP %s batchId=%s status=%s 期望=PARSED_FAILED；bank_transactions %d→%d %s" % (
                             st, batch_id, status or "-", before, after,
                             "" if ok else dump(body, 300)))

    # ---------- AC-6 ----------
    def ac6(self):
        total = int(self.scalar("SELECT COUNT(*) FROM bank_transactions WHERE deleted = 0", "0"))
        orphan = int(self.scalar(
            "SELECT COUNT(*) FROM bank_transactions bt WHERE bt.deleted = 0 AND ("
            " bt.batch_id IS NULL OR bt.create_user_id IS NULL"
            " OR NOT EXISTS (SELECT 1 FROM import_batches ib WHERE ib.batch_id = bt.batch_id)"
            " OR NOT EXISTS (SELECT 1 FROM bank_accounts ba WHERE ba.account_id = bt.account_id))", "0"))
        sample = ""
        if total:
            rc, out, err = self.psql(
                "SELECT transaction_id, batch_id, create_user_id, amount, external_txn_id "
                "FROM bank_transactions WHERE deleted = 0 ORDER BY transaction_id DESC LIMIT 1")
            sample = (out or "").splitlines()[0] if rc == 0 and out else ""
        return self.step("AC-6 审计可回溯 / 孤儿行 = 0", total > 0 and orphan == 0,
                         "总行=%d 孤儿=%d 抽样(transaction|batch|user|amount|ext)=%s" % (total, orphan, sample or "无"))

    # ---------- 无写账 ----------
    def no_write_accounting(self):
        try:
            ff = int(self.scalar("SELECT COUNT(*) FROM fund_flows", "0"))
            bal = int(self.scalar("SELECT COALESCE(SUM(balance),0) FROM bank_accounts", "0"))
        except Exception as e:
            return self.step("P0 无写账（fund_flows / 账户余额零变化）", False, str(e))
        ok = ff == self.fund_flows_before and bal == self.balance_before
        return self.step("P0 无写账（fund_flows / 账户余额零变化）", ok,
                         "fund_flows %d→%d（差 %d）；balance 合计 %d→%d（差 %d 分）" % (
                             self.fund_flows_before, ff, ff - self.fund_flows_before,
                             self.balance_before, bal, bal - self.balance_before))

    # ---------- AC-37 ----------
    def ac37(self):
        checks = [
            ("AC-37 consistency_check.py", ["python", "scripts/qa/consistency_check.py"], "5 PASS"),
            ("AC-37 ui_e2e_payment.py", ["python", "scripts/qa/ui_e2e_payment.py"], "7/7"),
            ("AC-37 schema_guard.py", ["python", "scripts/qa/schema_guard.py"], "5/5"),
        ]
        ok_all = True
        for name, cmd, want in checks:
            try:
                out = subprocess.run(cmd, capture_output=True, text=True, timeout=300,
                                     encoding="utf-8", errors="replace", cwd=self.a.repo)
                text = (out.stdout or "") + (out.stderr or "")
                tail = [ln for ln in text.splitlines() if ln.strip()][-1:] or [""]
                ok = out.returncode == 0
                ok_all = self.step("%s（%s）" % (name, want), ok,
                                   "exit=%s 末行=%s" % (out.returncode, tail[0][:160])) and ok_all
            except Exception as e:
                ok_all = self.step("%s（%s）" % (name, want), False, "执行失败: %s" % e) and ok_all
        return ok_all

    # ---------- 汇总 ----------
    def run(self):
        print("=" * 82)
        print("P0-C 独立验收（CSV 导入 AC-1~AC-6 + AC-37 + 无写账）  base=%s  account=%s" % (
            BASE, self.a.account_id))
        print("=" * 82)
        if not self.preflight_backend():
            return self.summary()
        if not self.login():
            return self.summary()
        if not self.precheck():
            return self.summary()
        if not self.preflight_endpoints():
            return self.summary()
        self.ac1()
        self.ac2()
        self.ac3()
        self.ac4()
        self.ac5()
        self.ac6()
        self.no_write_accounting()
        if not self.a.no_ac37:
            self.ac37()
        return self.summary()

    def summary(self):
        fails = [r for r in self.results if r[1] == "FAIL"]
        nr = [r for r in self.results if r[1] == "NOT_READY"]
        print("\n" + "=" * 82)
        if nr and not fails:
            print("结果: 端点未就绪    NOT_READY=%d PASS=%d FAIL=0" % (
                len(nr), len(self.results) - len(nr)))
        else:
            print("结果: %s    PASS=%d FAIL=%d%s" % (
                "全绿" if not fails else "存在失败",
                len([r for r in self.results if r[1] == "PASS"]), len(fails),
                ("  NOT_READY=%d" % len(nr)) if nr else ""))
        for name, state, detail in self.results:
            print("  %-56s %s" % (name, state))
        if fails:
            print("-" * 82)
            print("失败详情：")
            for name, state, detail in fails:
                print("  * %s\n    %s" % (name, detail))
        print("=" * 82)
        if nr and not fails:
            return 2
        return 0 if not fails else 1


def main():
    global BASE
    p = argparse.ArgumentParser()
    p.add_argument("--base", default=DEFAULT_BASE)
    p.add_argument("--token", default=None, help="自签 JWT（含 finance:import 等权限）")
    p.add_argument("--admin-user", default="qa_admin")
    p.add_argument("--admin-password", default="Admin@123456")
    p.add_argument("--account-id", default="20", help="导入目标银行账户（bank_accounts.account_id）")
    p.add_argument("--business-date", default=datetime.now().strftime("%Y-%m-%d"))
    p.add_argument("--id-tag", default=datetime.now().strftime("%H%M%S"),
                   help="外部单号后缀，保证每次终轮导入的是全新行（AC-2 才校验本次写入）；传空串则复用固定单号")
    p.add_argument("--work-dir", default=r"P:\my-new-project\.verify-tmp\p0-csv")
    p.add_argument("--repo", default=r"P:\my-new-project")
    p.add_argument("--no-ac37", action="store_true")
    p.add_argument("--psql", default=DEFAULT_PSQL)
    p.add_argument("--pg-host", default="localhost")
    p.add_argument("--pg-port", type=int, default=5432)
    p.add_argument("--pg-user", default="postgres")
    p.add_argument("--pg-password", default="123456")
    p.add_argument("--pg-db", default="food_traceability")
    a = p.parse_args()
    BASE = a.base
    try:
        return Runner(a).run()
    except Exception as e:
        print("执行错误: %s" % e, file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
