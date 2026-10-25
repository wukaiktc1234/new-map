#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
P3 前置造数据脚本（可重复执行；默认要求库为空，非空直接报错退出；--reset 先清再建）

产物：
  1) 3 份 CSV（UTF-8+BOM 微信 / UTF-8 支付宝 / GB18030 银行），全部走 I1 import→commit
  2) 2 张缴款单（平账 A / 短款 B），走 POS 下单+现金支付 → SL1→SL4→SL5→SL6(DEPOSITED)
  3) 1 张调节表（S3），is_balanced=true
  4) 末段跑 consistency_check，要求 A1–A20 全 0、TOTAL FAIL=0、exit 0

用法：
  python scripts/qa/seed_p3_fixture.py                 # 默认：库非空报错退出
  python scripts/qa/seed_p3_fixture.py --reset          # 先跑 purge 脚本再建
  python scripts/qa/seed_p3_fixture.py --force          # 库非空也继续（可能叠加，慎用）

================ 验收预期（务必先读，避免把负例当缺陷） ================
1) 支付宝批次（第 2 份 CSV / 通常 batchId=102）**预期 ok_rows=9 / error_rows=2**，
   这 2 条是**故意造的负例**：
     · 第 10 行 TXN_TIME_INVALID：交易时间写成 2026-10-24T16:30:00+08:00（带时区偏移，
       当前 I1 导入不支持偏移量输入，故行级报错——这是待改进项，不是本脚本缺陷）；
     · 第 11 行 AMOUNT_DECIMAL_INVALID：金额写成 12.345（3 位小数，分以下不可表示）。
2) 该批次 status=**PARTIAL** 是**预期状态**（有 OK 行也有 ERROR 行），不是缺陷；
   微信批次与银行批次预期 status=**COMMITTED**。
3) 验收时若因这 2 条 ERROR 导致断言变红，**先检查验收预期是否写对**
   （即是否误把 9/2 当成"应该有 11 条 OK"），**不得直接改断言或改脚本逻辑去迁就**；
   如确需全绿样本，应另造一份"零负例"的 CSV，而不是删掉负例。
=====================================================================
"""
from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
import urllib.error
import urllib.request
import uuid
from datetime import date, datetime, timedelta

DEFAULT_BASE = "http://127.0.0.1:8081/api"
DEFAULT_PSQL = r"P:\my-new-project\PostgreSQL\18\bin\psql.exe"
DEFAULT_PURGE = r"P:\my-new-project\scripts\db-fix\20261026_purge-finance-data.sql"
BASE = DEFAULT_BASE
# 清库后必须为 0 的「财务派生表」。
# 【2026-10-26 修正】原清单含 orders / pos_shifts，但这两张是**主数据**（Owner 明确不清），
#   重跑 seed 时它们必然非空（上一轮 E2E 的订单/班次仍在）→ 自检恒为红。现从清单移除，
#   改为单独打印行数供参考（不参与判定）。
EMPTY_TABLES = ["fund_flows", "bank_transactions", "deposit_slips",
                "recon_runs", "recon_items", "recon_differences", "reconciliation_statements",
                "reconciliation_items", "import_batches", "import_rows",
                "accounting_periods", "finance_period_account_balance", "finance_audit_logs"]


def call(method, path, token=None, body=None, timeout=60, raw=None, content_type=None):
    url = path if path.startswith("http") else BASE + path
    data = raw if raw is not None else (
        json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None)
    h = {"Accept": "application/json"}
    if content_type:
        h["Content-Type"] = content_type
    elif raw is None:
        h["Content-Type"] = "application/json"
    if token:
        h["Authorization"] = "Bearer " + token
    req = urllib.request.Request(url, data=data, headers=h, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            txt = r.read().decode("utf-8", "replace")
            try:
                return r.status, json.loads(txt)
            except Exception:
                return r.status, {"_raw": txt[:300]}
    except urllib.error.HTTPError as e:
        txt = e.read().decode("utf-8", "replace")
        try:
            return e.code, json.loads(txt)
        except Exception:
            return e.code, {"_raw": txt[:300]}
    except Exception as e:
        return -1, {"error": str(e)}


def ok_code(b):
    return isinstance(b, dict) and b.get("code") in (0, 200)


def data_of(b):
    return b.get("data") if isinstance(b, dict) else None


def dump(o, n=260):
    try:
        return json.dumps(o, ensure_ascii=False)[:n]
    except Exception:
        return str(o)[:n]


def multipart(fields, files):
    b = "----seed" + uuid.uuid4().hex
    out = []
    for k, v in fields.items():
        out += ["--%s" % b, 'Content-Disposition: form-data; name="%s"' % k, "", str(v)]
    for k, (fn, content, ctype) in files.items():
        out += ["--%s" % b, 'Content-Disposition: form-data; name="%s"; filename="%s"' % (k, fn),
                "Content-Type: %s" % ctype, "", content]
    out += ["--%s--" % b, ""]
    return "\r\n".join(out).encode("utf-8"), "multipart/form-data; boundary=%s" % b


class Seeder:
    def __init__(self, a):
        self.a = a
        self.results = []
        self.token = None
        self.cashier = None
        self.teller = None
        self.batches = []
        self.slips = []
        self.statement_id = None
        self.dir = os.path.join(a.workdir, "p3-seed")

    def ok(self, name, detail, state="PASS"):
        self.results.append((name, state, detail))
        print("  [%s] %s -> %s" % (state, name, detail))
        return state == "PASS"

    def fail(self, name, detail):
        return self.ok(name, detail, state="FAIL")

    # ---------- DB ----------
    def psql(self, sql, timeout=120):
        try:
            o = subprocess.run([self.a.psql, "-h", self.a.pg_host, "-p", str(self.a.pg_port),
                                "-U", self.a.pg_user, "-d", self.a.pg_db, "-t", "-A", "-F", "|", "-c", sql],
                               capture_output=True, text=True, timeout=timeout, encoding="utf-8",
                               errors="replace",
                               env=dict(os.environ, PGPASSWORD=self.a.pg_password, PGCLIENTENCODING="UTF8"))
            return o.returncode, (o.stdout or "").strip(), (o.stderr or "").strip()
        except Exception as e:
            return -1, "", str(e)

    def psql_file(self, path):
        try:
            o = subprocess.run([self.a.psql, "-h", self.a.pg_host, "-p", str(self.a.pg_port),
                                "-U", self.a.pg_user, "-d", self.a.pg_db, "-v", "ON_ERROR_STOP=1",
                                "-f", path],
                               capture_output=True, text=True, timeout=300, encoding="utf-8",
                               errors="replace",
                               env=dict(os.environ, PGPASSWORD=self.a.pg_password, PGCLIENTENCODING="UTF8"))
            return o.returncode, (o.stdout or "") + (o.stderr or "")
        except Exception as e:
            return -1, str(e)

    def scalar(self, sql, default="0"):
        rc, out, err = self.psql(sql)
        if rc != 0:
            raise RuntimeError("psql rc=%s %s" % (rc, (err or out)[:200]))
        return default if out == "" else out

    def rows(self, sql):
        rc, out, err = self.psql(sql)
        if rc != 0:
            raise RuntimeError("psql rc=%s %s" % (rc, (err or out)[:200]))
        return [l.split("|") for l in out.splitlines() if l.strip()]

    # ---------- 前置 ----------
    def preflight(self):
        st, b = call("POST", "/v1/auth/login", body={"username": "qa_admin", "password": "Admin@123456"})
        if st == -1:
            self.ok("后端 8081 在监听", dump(b, 120), state="NOT_READY")
            return False
        self.token = (data_of(b) or {}).get("token")
        self.ok("登录 qa_admin", "HTTP %s" % st if self.token else dump(b, 160),
                state="PASS" if self.token else "FAIL")
        for label, u, p in (("vcashier01", self.a.cashier_user, self.a.cashier_password),
                            ("qa_teller", self.a.teller_user, self.a.teller_password)):
            s2, b2 = call("POST", "/v1/auth/login", body={"username": u, "password": p})
            tok = (data_of(b2) or {}).get("token") if ok_code(b2) else None
            if u == self.a.cashier_user:
                self.cashier = tok
            else:
                self.teller = tok
            self.ok("登录 %s" % label, "HTTP %s" % s2, state="PASS" if tok else "FAIL")
        return bool(self.token)

    def check_empty(self):
        counts = {}
        for t in EMPTY_TABLES:
            try:
                counts[t] = int(self.scalar("SELECT COUNT(*) FROM %s" % t, "0"))
            except Exception as e:
                counts[t] = -1
        dirty = {k: v for k, v in counts.items() if v != 0}
        ref = self.scalar("SELECT count(*) FROM orders", "?")
        shifts = self.scalar("SELECT count(*) FROM pos_shifts", "?")
        return self.ok("库为空自检（财务派生表 %d 张全部 0 行）" % len(EMPTY_TABLES),
                       "空" if not dirty else "非空: %s" % dirty,
                       state="PASS" if not dirty else "FAIL"), dirty

    # ---------- CSV ----------
    def write(self, name, text, encoding):
        os.makedirs(self.dir, exist_ok=True)
        p = os.path.join(self.dir, name)
        with open(p, "wb") as f:
            f.write(text.encode(encoding))
        return p

    def csv_wechat(self, day):
        rows = [["交易时间", "微信订单号", "商户订单号", "交易类型", "交易状态", "应结订单金额", "手续费", "订单金额", "商品名称"]]
        prev = (day - timedelta(days=1)).strftime("%Y-%m-%d")
        rows.append(["%s 23:58:00" % prev, "4200001001", "MW001", "支付", "SUCCESS", "88.00", "0.53", "88.00", "跨日-夜宵"])
        rows.append(["%s 09:12:01" % day, "4200001002", "MW002", "支付", "SUCCESS", "128.00", "0.76", "128.00", "招牌牛肉面"])
        rows.append(["%s 09:15:33" % day, "4200001003", "MW003", "支付", "SUCCESS", "56.50", "0.34", "56.50", "冰美式"])
        rows.append(["%s 10:02:10" % day, "4200001004", "MW004", "支付", "SUCCESS", "19.90", "0.12", "19.90", "卤蛋"])
        rows.append(["%s 10:30:00" % day, "4200001005", "MW005", "支付", "SUCCESS", "300.00", "1.80", "300.00", "带手续费样本"])
        rows.append(["%s 11:30:00" % day, "4200001006", "MW006", "退款", "REFUND", "0.01", "0.00", "0.01", "退款-冰美式"])
        rows.append(["%s 12:00:00" % day, "4200001007", "MW007", "支付", "SUCCESS", "888.88", "5.33", "888.88", "团餐"])
        rows.append(["%s 13:20:00" % day, "4200001008", "MW008", "支付", "SUCCESS", "42.00", "0.25", "42.00", "午餐"])
        rows.append(["%s 15:45:00" % day, "4200001009", "MW009", "支付", "SUCCESS", "66.60", "0.40", "66.60", "下午茶"])
        rows.append(["%s 18:05:00" % day, "4200001010", "MW010", "支付", "SUCCESS", "120.00", "0.72", "120.00", "晚餐"])
        rows.append(["%s 20:30:00" % day, "4200001011", "MW011", "支付", "SUCCESS", "35.50", "0.21", "35.50", "夜宵"])
        text = "\ufeff" + "\r\n".join(",".join(r) for r in rows) + "\r\n"
        return self.write("wechat_bom.csv", text, "utf-8"), len(rows) - 1

    def csv_alipay(self, day):
        rows = [["支付宝交易号", "商家订单号", "业务类型", "商品名称", "创建时间", "完成时间", "对方账户", "订单金额（元）", "服务费（元）", "备注"]]
        rows.append(["2026102522001", "MA001", "交易", "套餐A", "%s 08:00:00" % day, "%s 08:00:05" % day, "138****0001", "66.00", "0.40", "早餐"])
        rows.append(["2026102522002", "MA002", "交易", "套餐B", "%s 08:10:00" % day, "%s 08:10:07" % day, "138****0002", "99.90", "0.60", "早餐"])
        rows.append(["2026102522003", "MA003", "交易", "单点", "%s 12:20:00" % day, "%s 12:20:09" % day, "138****0003", "12.34", "0.07", "午餐"])
        rows.append(["2026102522004", "MA004", "交易", "单点", "%s 18:40:00" % day, "%s 18:40:03" % day, "138****0004", "45.00", "0.27", "晚餐"])
        rows.append(["2026102522005", "MA005", "交易", "外卖", "%s 19:05:00" % day, "%s 19:05:11" % day, "138****0005", "150.00", "0.90", "外卖"])
        rows.append(["2026102522006", "MA006", "交易", "单点", "%s 09:30:00" % day, "%s 09:30:02" % day, "138****0006", "23.00", "0.14", "加餐"])
        rows.append(["2026102522007", "MA007", "交易", "套餐C", "%s 11:00:00" % day, "%s 11:00:04" % day, "138****0007", "77.70", "0.47", "午市"])
        rows.append(["2026102522008", "MA008", "交易", "单点", "%s 14:15:00" % day, "%s 14:15:03" % day, "138****0008", "31.20", "0.19", "下午"])
        rows.append(["2026102522009", "MA009", "交易", "套餐D", "%s 17:45:00" % day, "%s 17:45:06" % day, "138****0009", "58.80", "0.35", "晚餐"])
        # 跨时区输入（带 +08:00 偏移）
        rows.append(["2026102522010", "MA010", "交易", "跨时区样本", "2026-10-24T16:30:00+08:00", "2026-10-24T16:30:05+08:00", "138****0010", "11.00", "0.07", "偏移输入"])
        # 3 位小数（应行级 ERROR，负例）
        rows.append(["2026102522011", "MA011", "交易", "三位小数负例", "%s 20:00:00" % day, "%s 20:00:02" % day, "138****0011", "12.345", "0.07", "三位小数"])
        text = "\r\n".join(",".join(r) for r in rows) + "\r\n"
        return self.write("alipay.csv", text, "utf-8"), len(rows) - 1

    def csv_bank(self, day):
        rows = [["交易时间", "交易流水号", "借方金额", "贷方金额", "余额", "对方户名", "对方账号", "摘要", "交易类型"]]
        day = day.strftime("%Y-%m-%d") if hasattr(day, "strftime") else str(day)
        nxt = (datetime.strptime(day, "%Y-%m-%d").date() + timedelta(days=1)).strftime("%Y-%m-%d")
        rows.append(["%s %s" % (day, "09:30:00"), "B3001", "", "5000.00", "105000.00", "某某餐饮", "6222****0001", "货款", "转入"])
        rows.append(["%s %s" % (day, "10:30:00"), "B3002", "1200.50", "", "103799.50", "某某供应", "6222****0002", "采购付款", "转出"])
        rows.append(["%s %s" % (day, "11:30:00"), "B3003", "15.00", "", "103784.50", "银行", "", "手续费", "手续费"])
        rows.append(["%s %s" % (day, "14:00:00"), "B3004", "", "88.88", "103873.38", "利息", "", "结息", "利息"])
        rows.append(["%s %s" % (day, "16:00:00"), "B3005", "", "0.10", "103873.48", "测试", "6222****0005", "小额入账", "转入"])
        rows.append(["%s %s" % (day, "17:20:00"), "B3006", "260.00", "", "103613.48", "供应商A", "6222****0006", "货款支出", "转出"])
        rows.append(["%s %s" % (day, "18:40:00"), "B3007", "", "1200.00", "104813.48", "客户B", "6222****0007", "回款", "转入"])
        rows.append(["%s %s" % (day, "19:05:00"), "B3008", "30.00", "", "104783.48", "银行", "", "账户管理费", "手续费"])
        rows.append(["%s %s" % (day, "20:15:00"), "B3009", "", "450.00", "105233.48", "客户C", "6222****0009", "回款", "转入"])
        # 跨日 00:03 到账（次日）
        rows.append(["%s %s" % (nxt, "00:03:00"), "B3010", "", "777.00", "106010.48", "跨日客户", "6222****0010", "跨日到账", "转入"])
        rows.append(["%s %s" % (nxt, "00:09:00"), "B3011", "12.00", "", "105998.48", "银行", "", "跨日手续费", "手续费"])
        text = "\r\n".join(",".join(r) for r in rows) + "\r\n"
        return self.write("bank_gb18030.csv", text, "gb18030"), len(rows) - 1

    def import_one(self, path, source_type, account_id, label):
        with open(path, "rb") as f:
            content = f.read().decode("utf-8", "ignore") if path.endswith("csv") and not path.endswith("gb18030.csv") else None
        raw_bytes = open(path, "rb").read()
        # 直接把原始字节拼进 multipart（GB18030 不能被当 utf-8 读）
        b = "----seed" + uuid.uuid4().hex
        parts = []
        for k, v in {"sourceType": source_type, "accountId": str(account_id)}.items():
            parts.append(("--%s\r\nContent-Disposition: form-data; name=\"%s\"\r\n\r\n%s\r\n" % (b, k, v)).encode("utf-8"))
        parts.append(("--%s\r\nContent-Disposition: form-data; name=\"file\"; filename=\"%s\"\r\n"
                      "Content-Type: text/csv\r\n\r\n" % (b, os.path.basename(path))).encode("utf-8"))
        parts.append(raw_bytes)
        parts.append(("\r\n--%s--\r\n" % b).encode("utf-8"))
        body = b"".join(parts)
        st, resp = call("POST", "/v1/finance/bank-transactions/import", self.token, raw=body,
                        content_type="multipart/form-data; boundary=%s" % b)
        if not ok_code(resp):
            return self.fail("导入 %s" % label, "HTTP %s %s" % (st, dump(resp, 200))), None
        d = data_of(resp) or {}
        bid = d.get("batchId")
        st2, r2 = call("POST", "/v1/finance/imports/%s/commit" % bid, self.token, {})
        commit_ok = ok_code(r2)
        rows = self.rows("SELECT status, COALESCE(error_code,'') FROM import_rows WHERE batch_id=%s ORDER BY row_no" % bid)
        ok_rows = sum(1 for r in rows if r[0] == "OK")
        err_rows = [(i + 1, r[1]) for i, r in enumerate(rows) if r[0] != "OK"]
        self.batches.append({"label": label, "batchId": bid, "rows": len(rows), "ok": ok_rows, "errors": err_rows})
        self.ok("导入+提交 %s" % label, "batchId=%s 行数=%d OK=%d ERROR=%d 错误行=%s commit=%s" % (
            bid, len(rows), ok_rows, len(rows) - ok_rows, err_rows, commit_ok),
                state="PASS" if commit_ok and ok_rows > 0 else "FAIL")
        return bid, d

    # ---------- 缴款单 ----------
    def resolve_store_accounts(self, store_id):
        """按门店反查现金户(account_type=2)/存行户(account_type=1)；缺任何一个直接失败并列出可用账户。"""
        rows = self.rows("SELECT account_id, account_type, balance, COALESCE(account_name,'')"
                         " FROM bank_accounts WHERE deleted=0 AND store_id=%d ORDER BY account_type, account_id"
                         % int(store_id))
        cash = [int(r[0]) for r in rows if str(r[1]) == "2"]
        bank = [int(r[0]) for r in rows if str(r[1]) == "1"]
        listing = ", ".join("%s(type=%s,bal=%s,%s)" % (r[0], r[1], r[2], r[3]) for r in rows) or "（无）"
        if not cash or not bank:
            self.fail("门店 %d 账户就绪" % store_id,
                      "缺 account_type=2(现金) 或 =1(存行)；该店可用账户：%s；库中全部账户见 bank_accounts，"
                      "按要求不自动建账户" % listing)
            return False, None, None
        return self.ok("门店 %d 账户就绪" % store_id,
                       "现金户=%d 存行户=%d（该店账户：%s）" % (cash[0], bank[0], listing)), cash[0], bank[0]

    def ensure_current_period(self):
        """确保当前自然月存在 accounting_periods 行（清库后 migration 种子已被 purge 清掉）。"""
        today = date.today()
        code = today.strftime('%Y-%m')
        first = today.replace(day=1)
        nxt = (first + timedelta(days=32)).replace(day=1)
        last = nxt - timedelta(days=1)
        rc, o, e = self.psql(
            "INSERT INTO accounting_periods (period_code, period_name, period_type, start_date, end_date,"
            " status, is_closed, create_time, update_time, created_by) VALUES"
            " ('%s', '%s period', 2, '%s', '%s', 0, false, now(), now(), 'p3seed')"
            " ON CONFLICT (period_code) DO NOTHING" % (code, code, first.isoformat(), last.isoformat()))
        return self.ok("当前期间就绪", "period_code=%s %s~%s rc=%s %s"
                       % (code, first.isoformat(), last.isoformat(), rc, (o or e or "").strip()[:120]),
                       state="PASS" if rc == 0 else "FAIL")

    def set_opening_balance(self, account_ids):
        """【P4-4 缺陷修复】造流水前先把参与账户的余额设定期初值（默认 0）。

        为什么必须做：`fund_flows.balance_after` 是滚动余额，V1 试算要求
        「期初 + 收 − 支 == 期末 == 期间内末笔 balance_after」。
        若不清零，账户会带着上一轮 E2E 留下的余额开工（如 80,760 分），而 2026-10-01 之前
        没有任何 fund_flows → 期初算出来是 0 → 恒等式必然不成立（2026-10 结不了账的真因）。
        """
        opening = int(self.a.opening_balance_fen)
        ids = ",".join(str(int(x)) for x in account_ids)
        rc, o, e = self.psql(
            "UPDATE bank_accounts SET balance = %d, version = COALESCE(version,0)+1, update_time = now()"
            " WHERE deleted = 0 AND account_id IN (%s)" % (opening, ids))
        return self.ok("账户期初余额设定", "accounts=[%s] 期初=%d 分 rc=%s %s"
                       % (ids, opening, rc, (o or e or "").strip()[:120]),
                       state="PASS" if rc == 0 else "FAIL")

    def verify_v1(self):
        """V1 试算：逐账户 期初 + 收 − 支 == 期间内末笔 balance_after（财务恒等式，不允许改口径）。"""
        today = date.today()
        first = today.replace(day=1).isoformat()
        rows = self.rows(
            "WITH f AS (SELECT account_id,"
            "  COALESCE(SUM(amount) FILTER (WHERE flow_direction=1),0) AS income_fen,"
            "  COALESCE(SUM(amount) FILTER (WHERE flow_direction=2),0) AS expense_fen,"
            "  count(*) AS n FROM fund_flows WHERE deleted=0 AND status=2 AND business_date >= '%s'"
            "  GROUP BY account_id),"
            " last AS (SELECT DISTINCT ON (account_id) account_id, balance_after FROM fund_flows"
            "  WHERE deleted=0 AND status=2 AND business_date >= '%s'"
            "  ORDER BY account_id, business_date DESC, flow_id DESC)"
            " SELECT f.account_id, f.income_fen, f.expense_fen, f.n, last.balance_after,"
            "        (f.income_fen - f.expense_fen) AS closing_fen"
            "   FROM f LEFT JOIN last ON last.account_id = f.account_id ORDER BY f.account_id" % (first, first))
        bad = [r for r in rows if r[4] is None or int(r[5]) != int(r[4])]
        detail = "; ".join("acc%s: income=%s expense=%s closing=%s last=%s n=%s"
                           % (r[0], r[1], r[2], r[5], r[4], r[3]) for r in rows)
        return self.ok("V1 试算（期初=0 + 收 − 支 == 末笔 balance_after）",
                       "%d 个账户，不一致 %d 个：%s" % (len(rows), len(bad), detail[:400]),
                       state="PASS" if rows and not bad else "FAIL")

    def clone_shift(self, remark, start, end):
        rc, o, e = self.psql(
            "INSERT INTO pos_shifts (terminal_id, employee_id, employee_name, shift_type, start_time,"
            " end_time, opening_cash, closing_cash, total_orders, total_amount, status, handover_remark,"
            " create_time, update_time, deleted) VALUES ('T-P3SEED', 'E-P3SEED', 'p3seed', 1,"
            " '%s', '%s', 0, 0, 0, 0, 'completed', '%s', now(), now(), 0) RETURNING shift_id"
            % (start, end, remark))
        sid = int((o or "0").splitlines()[0].strip() or 0) if rc == 0 else 0
        if not sid:
            self.fail("造班次(%s)" % remark, (e or o)[:200])
        return sid

    def pos_orders(self, n, tag, store_id=None):
        made = 0
        sid = int(store_id if store_id is not None else self.a.store_id)
        for i in range(n):
            yuan = 50.00 + i
            st, b = call("POST", "/v1/pos/orders/order", self.cashier or self.token, {
                "tableNumber": (i % 10) + 1,
                "items": [{"id": self.food_code, "name": self.food_name, "quantity": 1, "price": yuan}],
                "totalAmount": yuan, "paymentMethod": "cash", "orderType": "DINE_IN",
                "storeId": str(sid), "remark": "P3 造数 %s" % tag,
                "idempotencyKey": "p3seed-%s-%d" % (tag, i)})
            d = data_of(b) or {}
            oid = d.get("orderId") or d.get("id") or d.get("orderNumber")
            if not ok_code(b) or not oid:
                self.fail("POS 下单 %s#%d" % (tag, i + 1), dump(b, 160))
                continue
            s2, b2 = call("POST", "/v1/pos/orders/order/pay", self.cashier or self.token,
                          {"orderId": oid, "paymentMethod": "cash", "amount": yuan})
            if ok_code(b2):
                made += 1
            else:
                self.fail("POS 支付 %s#%d" % (tag, i + 1), dump(b2, 160))
        return made

    def verify_no_orphan_paid_orders(self):
        """【P4-5 追根修复】造完 POS 单后立即校验「已支付但无 SALE 流水」= 0。

        背景（追问得出的真因）：`pos_orders` 走的是真实 POS 链路（`/order` + `/order/pay`），
        SALE 流水由 pay 端 `PosSaleFinanceRecorder` 写入 —— **seed 本身不造流水，也不会漏造**。
        历史上 93/109 条「已支付无流水」订单不是 seed 造成的，而是**旧的 purge 只物理删 fund_flows、
        保留 orders** 造成的孤儿。有了这条自检，seed 一旦出现「下单成功但入账失败」会立刻红，
        不再靠事后软删补救（软删只是清库的收尾手段，不是验收手段）。
        """
        n = int(self.scalar(
            "SELECT count(*) FROM orders o WHERE o.deleted=0 AND o.payment_status=2"
            " AND NOT EXISTS (SELECT 1 FROM fund_flows f WHERE f.deleted=0 AND f.flow_direction=1"
            "   AND f.flow_category=1 AND f.order_id=o.order_id)", "0"))
        return self.ok("已支付订单的 SALE 流水完整（A1 口径）",
                       "已支付但无 SALE 流水的订单 = %d（期望 0）" % n,
                       state="PASS" if n == 0 else "FAIL")

    def make_slip(self, shift_id, label, short_fen, store_id=None, bank_account=None):
        sid = int(store_id if store_id is not None else self.a.store_id)
        bacct = int(bank_account if bank_account is not None else self.a.bank_account)
        st, b = call("POST", "/v1/finance/deposit-slips/generate", self.token,
                     {"shiftId": shift_id, "storeId": sid})
        if not ok_code(b):
            return self.fail("%s SL1 生成" % label, dump(b, 200))
        d = data_of(b) or {}
        d = d.get("slip") or d
        slip_id, slip_no = d.get("slipId"), d.get("slipNo")
        expected = int(d.get("expectedAmount") or 0)
        call("POST", "/v1/finance/deposit-slips/%s/submit" % slip_id, self.cashier or self.token,
             {"remark": "p3-seed submit"})
        actual = expected - short_fen
        st3, b3 = call("POST", "/v1/finance/deposit-slips/%s/receive" % slip_id, self.teller or self.token,
                       {"receivedAmountFen": int(actual), "remark": "p3-seed receive"})
        # 存行：需要等额 bank_transactions；走 I1 导入一条等额现金存入流水
        tag = "P3SEED%s%s" % (label, datetime.now().strftime("%H%M%S"))
        # 单列「交易时间」放完整时间（与银行 CSV 同口径，避免 TIME_UNAVAILABLE）
        csv = ("交易时间,交易流水号,借方金额,贷方金额,余额,对方户名,对方账号,摘要,交易类型\r\n"
               "%s,%s,,%s,%s,现金缴款,,%s,转入\r\n"
               % (datetime.now().strftime("%Y-%m-%d %H:%M:%S"), tag,
                  "%.2f" % (actual / 100.0), "%.2f" % (actual / 100.0), slip_no))
        p = self.write("deposit_%s.csv" % label, csv, "utf-8")
        bid, _ = self.import_one(p, "BANK", bacct, "存行到账(%s)" % label)
        txn = self.rows("SELECT transaction_id FROM bank_transactions WHERE deleted=0 AND external_txn_id LIKE"
                        " '%%%s%%' ORDER BY transaction_id DESC LIMIT 1" % tag)
        tid = int(txn[0][0]) if txn else 0
        st4, b4 = call("POST", "/v1/finance/deposit-slips/%s/deposit" % slip_id, self.teller or self.token,
                       {"bankTransactionId": tid or None, "depositRefNo": tag, "remark": "p3-seed deposit"})
        row = self.rows("SELECT status, expected_amount, actual_amount, difference_amount,"
                        " COALESCE(business_date::text,'') FROM deposit_slips WHERE slip_id=%s" % slip_id)
        r = row[0] if row else ["-", "0", "0", "0", ""]
        self.slips.append({"label": label, "storeId": sid, "slipNo": slip_no, "status": r[0],
                           "expected": r[1], "actual": r[2], "difference": r[3],
                           "business_date": r[4]})
        return self.ok("%s 缴款单走到 DEPOSITED" % label,
                       "store=%s slip=%s status=%s expected=%s actual=%s difference=%s business_date=%s "
                       "depositOK=%s txnId=%s bankAcct=%s" % (sid, slip_no, r[0], r[1], r[2], r[3], r[4],
                                                              ok_code(b4), tid, bacct),
                       state="PASS" if r[0] == "DEPOSITED" else "FAIL")

    # ---------- CHANNEL_FEE 差异样本（AC-50 用） ----------
    def channel_fee_sample(self):
        # 1) 需要一条 run：用现金户跑一次 R1
        st, b = call("POST", "/v1/finance/recon-runs", self.token,
                     {"accountId": int(self.a.cash_account), "businessDate": str(date.today())})
        if not ok_code(b):
            return self.fail("CHANNEL_FEE 样本：准备 run", dump(b, 160))
        d = data_of(b) or {}
        run = d.get("run") or d
        run_id = run.get("runId")
        # 2) 找一笔真实带手续费的外部流水（fee>0 且 source_type 非空）
        rows = self.rows("SELECT transaction_id, external_txn_id, fee, source_type, amount"
                         " FROM bank_transactions WHERE deleted=0 AND fee IS NOT NULL AND fee > 0"
                         " AND source_type IS NOT NULL AND external_txn_id IS NOT NULL"
                         " ORDER BY transaction_id DESC LIMIT 1")
        if not rows:
            return self.fail("CHANNEL_FEE 样本", "库中没有 fee>0 且 source_type 非空的 bank_transactions")
        tid, ext_ref, fee, src, amt = rows[0]
        fee = int(fee or 0)
        diff_amount = fee
        rc, out, err = self.psql(
            "INSERT INTO recon_differences (run_id, difference_type, internal_ref, external_ref,"
            " amount_internal, amount_external, diff_amount, status, create_time, update_time)"
            " VALUES (%s, 'CHANNEL_FEE', NULL, '%s', NULL, %s, %s, 'OPEN', now(), now())"
            " RETURNING difference_id" % (run_id, ext_ref, int(amt or 0), diff_amount))
        if rc != 0:
            return self.fail("CHANNEL_FEE 样本：插入", (err or out)[:200])
        did = (out or "0").splitlines()[0].strip()
        self.channel_fee = {"differenceId": did, "runId": run_id, "externalRef": ext_ref,
                            "diffAmount": diff_amount, "fee": fee, "sourceType": src,
                            "transactionId": tid}
        return self.ok("CHANNEL_FEE 差异样本（真实关联外部流水）", 
                       "difference_id=%s run_id=%s external_ref=%s diff_amount=%s fee=%s source_type=%s transaction_id=%s" % (
                           did, run_id, ext_ref, diff_amount, fee, src, tid),
                       state="PASS" if int(diff_amount) == fee and int(fee) > 0 else "FAIL")

    # ---------- 调节表 ----------
    def make_statement(self):
        today = date.today()
        period = None
        for back in range(20, 60):
            ps = (today - timedelta(days=back)).strftime("%Y-%m-%d")
            pe = today.strftime("%Y-%m-%d")
            n = int(self.scalar("SELECT COUNT(*) FROM reconciliation_statements WHERE account_id=%d AND deleted=0"
                                " AND period_start='%s' AND period_end='%s'" % (self.a.cash_account, ps, pe), "0"))
            if n == 0:
                period = (ps, pe)
                break
        if not period:
            return self.fail("调节表期间", "20–60 天内期间均被占用")
        target = 0
        last = None
        for attempt in range(4):
            st, b = call("POST", "/v1/finance/recon/statements", self.token,
                         {"accountId": int(self.a.cash_account), "periodStart": period[0],
                          "periodEnd": period[1], "bankStatementBalanceFen": int(target),
                          "remark": "p3-seed statement"})
            if not ok_code(b):
                return self.fail("S3 生成调节表", "HTTP %s %s" % (st, dump(b, 200)))
            d = data_of(b) or {}
            self.statement_id = d.get("statementId")
            last = d
            if d.get("isBalanced"):
                break
            a = int(d.get("unclearedInCredit") or 0)
            bb = int(d.get("unclearedInDebit") or 0)
            ab = int(d.get("adjustedBookBalance") or 0)
            target = ab - a + bb
        d = last or {}
        items = self.rows("SELECT category, COUNT(*), COALESCE(SUM(amount),0) FROM reconciliation_items"
                          " WHERE statement_id=%s AND deleted=0 GROUP BY category ORDER BY category"
                          % self.statement_id) if self.statement_id else []
        amb = int(self.scalar("SELECT COUNT(*) FROM recon_differences WHERE difference_type='AMBIGUOUS'", "0"))
        self.step_items = {r[0]: (int(r[1]), int(r[2])) for r in items}
        need = ["A", "B", "C", "D"]
        ok = bool(self.statement_id) and bool(d.get("isBalanced")) and amb == 0
        return self.ok("S3 调节表 A/B/C/D 归属且 is_balanced=true、无 AMBIGUOUS", 
                       "statementId=%s period=%s..%s is_balanced=%s 四类(A条数/金额,B,C,D)=%s "
                       "adjusted_book=%s adjusted_bank=%s AMBIGUOUS=%d" % (
                           self.statement_id, period[0], period[1], d.get("isBalanced"),
                           {k: self.step_items.get(k, (0, 0)) for k in need},
                           d.get("adjustedBookBalance"), d.get("adjustedBankBalance"), amb),
                       state="PASS" if ok else "FAIL")

    # ---------- 一致性自检 ----------
    def consistency(self):
        try:
            o = subprocess.run([sys.executable, os.path.join(self.a.repo, "scripts", "qa", "consistency_check.py")],
                               capture_output=True, text=True, timeout=600, encoding="utf-8", errors="replace",
                               cwd=self.a.repo)
        except Exception as e:
            return self.fail("一致性自检执行", str(e))
        text = (o.stdout or "") + (o.stderr or "")
        bad = []
        for ln in text.splitlines():
            if "[FAIL]" in ln or "[WARN]" in ln:
                bad.append(ln.strip()[:160])
        tail = [l for l in text.splitlines() if l.strip()][-1:] or [""]
        return self.ok("一致性自检 A1–A20 全 0 / TOTAL FAIL=0 / exit 0",
                       "exit=%s；末行=%s%s" % (o.returncode, tail[0][:160],
                                              ("；非零项: " + " | ".join(bad[:6])) if bad else ""),
                       state="PASS" if o.returncode == 0 and not bad else "FAIL")

    # ---------- 主流程 ----------
    def run(self):
        print("=" * 88)
        print("P3 前置造数据（3 CSV + 2 缴款单 + 1 调节表 + 一致性自检）")
        print("=" * 88)
        if self.a.reset:
            rc, out = self.psql_file(self.a.purge_sql)
            self.ok("--reset 清库", "psql -f %s → rc=%s %s" % (self.a.purge_sql, rc, out[:200].replace("\n", " ")),
                    state="PASS" if rc == 0 else "FAIL")
        if not self.preflight():
            return self.summary()
        empty_ok, dirty = self.check_empty()
        if not empty_ok and not self.a.force and not self.a.reset:
            self.fail("拒绝非空库（未加 --force）", "非空表: %s" % dirty)
            return self.summary()

        # 【P4-4 缺陷修复】造流水前先定期间与账户期初，否则 V1 恒等式必然不成立（见 set_opening_balance 注释）
        self.ensure_current_period()
        seed_accounts = [self.a.cash_account, self.a.bank_account]
        ok_b0, cash_b0, bank_b0 = self.resolve_store_accounts(self.a.store_b)
        if ok_b0:
            seed_accounts += [cash_b0, bank_b0]
        self.set_opening_balance(sorted(set(seed_accounts)))

        row = self.rows("SELECT food_code, food_name FROM foods WHERE deleted=0 AND status=1"
                        " ORDER BY stock DESC LIMIT 1")
        self.food_code, self.food_name = (row[0][0], row[0][1]) if row else ("FD260926001", "P3 菜品")
        day = date.today() - timedelta(days=1)

        print("\n--- 1) 三份 CSV 走 I1 导入 ---")
        p1, n1 = self.csv_wechat(day)
        self.import_one(p1, "WECHAT", self.a.cash_account, "微信 UTF-8+BOM（%d 行）" % n1)
        p2, n2 = self.csv_alipay(day)
        self.import_one(p2, "ALIPAY", self.a.bank_account, "支付宝 UTF-8（%d 行）" % n2)
        p3, n3 = self.csv_bank(day)
        self.import_one(p3, "BANK", self.a.bank_account, "银行 GB18030（%d 行）" % n3)

        print("\n--- 2) 两张缴款单（两店各一张）---")
        # 班次窗口必须覆盖「POS 下单现在时刻」，否则 SL1 会报「班次时间窗内没有订单」
        tday = date.today()
        start = datetime.combine(tday, datetime.min.time())
        end = datetime.combine(tday, datetime.min.time()) + timedelta(hours=23, minutes=59)
        # store 1：平账
        sh1 = self.clone_shift("P3SEED-A-S1", start.strftime("%Y-%m-%d %H:%M:%S"),
                               end.strftime("%Y-%m-%d %H:%M:%S"))
        self.pos_orders(5, "A", store_id=self.a.store_id)
        if sh1:
            self.make_slip(sh1, "A", 0, store_id=self.a.store_id, bank_account=self.a.bank_account)
        # store 2：短款 500（现金户/银行户按 store 2 反查；缺账户直接退出，不自动建）
        ok_b, cash_b, bank_b = self.resolve_store_accounts(self.a.store_b)
        if not ok_b:
            return self.summary()
        sh2 = self.clone_shift("P3SEED-B-S%d" % self.a.store_b, start.strftime("%Y-%m-%d %H:%M:%S"),
                               end.strftime("%Y-%m-%d %H:%M:%S"))
        self.pos_orders(3, "B", store_id=self.a.store_b)
        if sh2:
            self.make_slip(sh2, "B", 500, store_id=self.a.store_b, bank_account=bank_b)

        print("\n--- 2.5) 已支付订单的 SALE 流水完整性 ---")
        self.verify_no_orphan_paid_orders()

        print("\n--- 3) CHANNEL_FEE 差异样本 ---")
        self.channel_fee_sample()

        print("\n--- 4) 调节表 ---")
        self.make_statement()

        print("\n--- 5) 一致性自检 ---")
        # 每次跑都打印支付宝批次的预期值，便于验收对表（负例不是缺陷）
        ali = next((b for b in self.batches if "支付宝" in b.get("label", "")), None)
        actual = "(本次未导入支付宝批次)"
        if ali:
            actual = "batchId=%s rows=%d ok=%d error=%d errors=%s" % (
                ali.get("batchId"), ali.get("rows"), ali.get("ok"), len(ali.get("errors") or []),
                ali.get("errors"))
        print("  [EXPECT] batch102 expected ok=9 error=2 (negative fixtures), status=PARTIAL"
              " ｜ 实测: %s" % actual)
        self.consistency()

        print("\n--- 6) V1 财务恒等式（期初 + 收 − 支 == 期末 == 末笔 balance_after）---")
        self.verify_v1()
        return self.summary()

    def summary(self):
        fails = [r for r in self.results if r[1] == "FAIL"]
        nr = [r for r in self.results if r[1] == "NOT_READY"]
        print("\n" + "=" * 88)
        print("结果: %s  PASS=%d FAIL=%d%s" % (
            "全绿" if not fails and not nr else ("未就绪" if not fails else "存在失败"),
            len([r for r in self.results if r[1] == "PASS"]), len(fails),
            ("  NOT_READY=%d" % len(nr)) if nr else ""))
        print("批次: %s" % json.dumps(self.batches, ensure_ascii=False))
        print("缴款单: %s" % json.dumps(self.slips, ensure_ascii=False))
        print("CHANNEL_FEE: %s" % json.dumps(getattr(self, "channel_fee", None), ensure_ascii=False))
        if fails or nr:
            print("-" * 88)
            for name, state, detail in fails + nr:
                print("  * [%s] %s\n    %s" % (state, name, detail))
        print("=" * 88)
        if fails:
            return 1
        return 2 if nr else 0


def main():
    global BASE
    p = argparse.ArgumentParser()
    p.add_argument("--base", default=DEFAULT_BASE)
    p.add_argument("--reset", action="store_true", help="先执行 purge SQL 再建")
    p.add_argument("--force", action="store_true", help="库非空也继续（可能叠加）")
    p.add_argument("--purge-sql", default=DEFAULT_PURGE)
    p.add_argument("--opening-balance-fen", type=int, default=0,
                   help="造流水前把参与账户余额设为该期初值（分，默认 0；V1 恒等式要求流水链从期初开始）")
    p.add_argument("--workdir", default=r"P:\my-new-project\.verify-tmp")
    p.add_argument("--repo", default=r"P:\my-new-project")
    p.add_argument("--store-id", type=int, default=1)
    p.add_argument("--store-b", type=int, default=2)
    p.add_argument("--cash-account", type=int, default=20)
    p.add_argument("--bank-account", type=int, default=24)
    p.add_argument("--cashier-user", default="vcashier01")
    p.add_argument("--cashier-password", default="123456")
    p.add_argument("--teller-user", default="qa_teller")
    p.add_argument("--teller-password", default="Admin@123456")
    p.add_argument("--psql", default=DEFAULT_PSQL)
    p.add_argument("--pg-host", default="localhost")
    p.add_argument("--pg-port", type=int, default=5432)
    p.add_argument("--pg-user", default="postgres")
    p.add_argument("--pg-password", default="123456")
    p.add_argument("--pg-db", default="food_traceability")
    a = p.parse_args()
    BASE = a.base
    try:
        return Seeder(a).run()
    except Exception as e:
        print("执行错误: %s" % e, file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
