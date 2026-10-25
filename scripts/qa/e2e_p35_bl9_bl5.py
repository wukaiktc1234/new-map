#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
P3-5：BL-9 时区偏移导入（AC-54） + A25 烧号台账 + 并发取号自洽

覆盖：
  1) AC-54（BL-9）：脚本内生成 3 行带偏移 CSV（Z / +08:00 / 带毫秒 +08:00，同一时刻
     2026-10-25T00:30+08:00）走 I1 import→commit，断言 bank_transactions.txn_time
     三行都是 2026-10-25 00:30:00；
     再导一行**不带偏移**的 2026-10-25 08:15:00，断言仍落 08:15:00（不因修复被误转）。
  2) A25：fund_flow_no_burn_ledger 表存在性 + archived_flag=false 行数，
     并与 consistency_check 输出里的 A25 值对拍（脚本不改断言，只贴原值）。
  3) 并发取号：若存在 scripts/qa/seq_concurrency_check_p35.py 则调用，断言
     退出码 0 且「不重复 rule_no / 落库数=成功数 / 烧号数=取号数−落库数」自洽。

前置：e2e_setup.ensure_ready()（服务探测 + 库存重置）。
本脚本以接口 + DB 为主，不需要 Playwright。
退出码：0 全绿 / 1 断言失败 / 2 环境未就绪（后端/表/巡检断言未落地）
"""
from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
import urllib.error
import urllib.request
import uuid

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

try:
    from e2e_setup import ensure_ready
except Exception as e:  # pragma: no cover
    print("[FATAL] 无法导入 e2e_setup：%s" % e)
    sys.exit(2)

DEFAULT_API = "http://localhost:8081/api"
DEFAULT_ADMIN = "http://localhost:3002"
DEFAULT_POS = "http://localhost:3001"
DEFAULT_PSQL = r"P:\my-new-project\PostgreSQL\18\bin\psql.exe"
TZ_UTC_TO_SH = "2026-10-25 00:30:00"   # UTC 16:30 → 上海次日 00:30
LOCAL_TARGET = "2026-10-25 08:15:00"


def http(method, url, token=None, body=None, raw=None, content_type=None, timeout=60):
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


def ok(body):
    return isinstance(body, dict) and body.get("code") in (0, 200)


def data_of(body):
    return body.get("data") if isinstance(body, dict) else None


def multipart(fields, filename, content):
    b = "----p35" + uuid.uuid4().hex
    parts = []
    for k, v in fields.items():
        parts.append(("--%s\r\nContent-Disposition: form-data; name=\"%s\"\r\n\r\n%s\r\n"
                      % (b, k, v)).encode("utf-8"))
    parts.append(("--%s\r\nContent-Disposition: form-data; name=\"file\"; filename=\"%s\"\r\n"
                  "Content-Type: text/csv\r\n\r\n" % (b, filename)).encode("utf-8"))
    parts.append(content.encode("utf-8"))
    parts.append(("\r\n--%s--\r\n" % b).encode("utf-8"))
    return b"".join(parts), "multipart/form-data; boundary=%s" % b


class P35:
    def __init__(self, a):
        self.a = a
        self.results = []
        self.token = None
        self.work = os.path.join(a.repo, ".verify-tmp", "p35")
        os.makedirs(self.work, exist_ok=True)

    def step(self, name, okk, detail, state=None):
        st = state or ("PASS" if okk else "FAIL")
        self.results.append((name, st, detail))
        print("  [%s] %s -> %s" % (st, name, detail))
        return okk

    def not_ready(self, name, detail):
        return self.step(name, False, detail, state="NOT_READY")

    # ---------- DB ----------
    def psql(self, sql):
        try:
            o = subprocess.run([self.a.psql, "-h", self.a.pg_host, "-p", str(self.a.pg_port),
                                "-U", self.a.pg_user, "-d", self.a.pg_db, "-t", "-A", "-F", "|", "-c", sql],
                               capture_output=True, text=True, timeout=60, encoding="utf-8", errors="replace",
                               env=dict(os.environ, PGPASSWORD=self.a.pg_password, PGCLIENTENCODING="UTF8"))
            return o.returncode, (o.stdout or "").strip(), (o.stderr or "").strip()
        except Exception as e:
            return -1, "", str(e)

    def rows(self, sql):
        rc, out, err = self.psql(sql)
        if rc != 0:
            raise RuntimeError("psql rc=%s %s" % (rc, (err or out)[:200]))
        return [l.split("|") for l in out.splitlines() if l.strip()]

    def table_exists(self, name):
        rc, out, _ = self.psql("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public'"
                               " AND table_name='%s'" % name)
        return rc == 0 and out.strip() not in ("", "0")

    # ---------- 登录 ----------
    def login(self):
        st, b = http("POST", self.a.api + "/v1/auth/login",
                     body={"username": self.a.user, "password": self.a.password})
        self.token = (data_of(b) or {}).get("token") if ok(b) else None
        return self.step("登录 %s" % self.a.user, bool(self.token), "HTTP %s %s" % (
            st, "" if self.token else json.dumps(b, ensure_ascii=False)[:140]))

    # ---------- AC-54 ----------
    def _import_csv(self, name, lines, label, expect_errors=None):
        content = "\r\n".join(lines) + "\r\n"
        path = os.path.join(self.work, name)
        with open(path, "w", encoding="utf-8", newline="") as f:
            f.write(content)
        body, ctype = multipart({"sourceType": "BANK", "accountId": str(self.a.account_id)},
                                name, content)
        st, b = http("POST", self.a.api + "/v1/finance/bank-transactions/import", self.token,
                     raw=body, content_type=ctype)
        if not ok(b):
            return self.not_ready("导入 %s" % label, "I1 HTTP %s %s" % (st, json.dumps(b, ensure_ascii=False)[:160]))
        bid = (data_of(b) or {}).get("batchId")
        st2, b2 = http("POST", self.a.api + "/v1/finance/imports/%s/commit" % bid, self.token, {})
        rows = self.rows("SELECT row_no, status, COALESCE(error_code,''), COALESCE(error_message,'')"
                         " FROM import_rows WHERE batch_id=%s ORDER BY row_no" % bid)
        errs = [(r[0], r[2], r[3][:60]) for r in rows if r[1] != "OK"]
        allowed = set(str(x) for x in (expect_errors or []))
        unexpected = [e for e in errs if str(e[0]) not in allowed]
        self.step("导入 %s（batchId=%s）" % (label, bid), ok(b2) and not unexpected,
                  "行数=%d OK=%d ERROR=%d（预期内错误行=%s）%s commit=%s CSV=%s" % (
                      len(rows), len(rows) - len(errs), len(errs), sorted(allowed),
                      ("；非预期=" + str(unexpected)) if unexpected else "", ok(b2), path))
        return bid, errs

    def ac54(self):
        """AC-54（BL-9）：按 ISO-8601 时区语义，六个 case 逐一断言 + 「同一时刻三种写法」一致性。

        期望（校准依据：统一换算到 Asia/Shanghai）：
          2026-10-24T16:30:00Z          → 2026-10-25 00:30:00（UTC 16:30 = 上海次日 00:30）
          2026-10-24T16:30:00+08:00     → 2026-10-24 16:30:00（本身即上海时间，不变）
          2026-10-24T16:30:00.000+08:00 → 2026-10-24 16:30:00（毫秒忽略）
          2026-10-25 08:15:00（无偏移）  → 2026-10-25 08:15:00（回归不变）
          2026-10-25（仅日期）           → 行级 NEEDS_REVIEW / TIME_UNAVAILABLE
          not-a-time（非法）             → 行级 ERROR / TXN_TIME_INVALID
        同一时刻三种写法（本质）：Z / +08:00 / -08:00 → 三者都应落 2026-10-25 00:30:00
        """
        tag = "P35TZ" + uuid.uuid4().hex[:6]
        header = "交易时间,交易流水号,借方金额,贷方金额,余额,对方户名,对方账号,摘要,交易类型"
        rows = [header]
        rows.append("2026-10-24T16:30:00Z,%s-Z,100.00,,1000.00,UTC样本,,TZ-Z,转入" % tag)
        rows.append("2026-10-24T16:30:00+08:00,%s-OFF,100.00,,1000.00,偏移样本,,TZ-OFF,转入" % tag)
        rows.append("2026-10-24T16:30:00.000+08:00,%s-MS,100.00,,1000.00,毫秒样本,,TZ-MS,转入" % tag)
        rows.append("2026-10-25 08:15:00,%s-LOCAL,100.00,,1000.00,无偏移样本,,TZ-LOCAL,转入" % tag)
        rows.append("2026-10-25,%s-DATEONLY,100.00,,1000.00,仅日期,,TZ-DATEONLY,转入" % tag)
        rows.append("not-a-time,%s-BAD,100.00,,1000.00,非法时间,,TZ-BAD,转入" % tag)
        rows.append("2026-10-24T16:30:00Z,%s-SAME-Z,100.00,,1000.00,同时刻Z,,SAME-Z,转入" % tag)
        rows.append("2026-10-25T00:30:00+08:00,%s-SAME-OFF,100.00,,1000.00,同时刻+08,,SAME-OFF,转入" % tag)
        rows.append("2026-10-24T08:30:00-08:00,%s-SAME-NEG,100.00,,1000.00,同时刻-08,,SAME-NEG,转入" % tag)
        bid, _errs = self._import_csv("ac54_tz.csv", rows, "AC-54 时区六 case + 同时刻三写法",
                                     expect_errors=[5, 6])   # 第5行仅日期、第6行非法：预期行级错误

        # 1) 四条应落库的期望值
        # external_txn_id 落库后形如 BANK:<tag>-Z，取「-」后的后缀作 key
        want = {
            "Z": "2026-10-25 00:30:00",
            "OFF": "2026-10-24 16:30:00",
            "MS": "2026-10-24 16:30:00",
            "LOCAL": "2026-10-25 08:15:00",
        }
        got = {}
        for ref, t in self.rows(
                "SELECT external_txn_id, to_char(txn_time,'YYYY-MM-DD HH24:MI:SS')"
                " FROM bank_transactions WHERE deleted=0 AND external_txn_id LIKE '%%%s%%'" % tag):
            # 导入会加 BANK: 前缀，这里取「-XXX」后缀做 key
            suffix = ref.split("-", 1)[1] if "-" in ref else ref
            got[suffix] = t
            got[ref] = t
        bad = ["%s 落库=%s 期望=%s" % (k, got.get(k), v) for k, v in want.items() if got.get(k) != v]
        self.step("AC-54a Z→上海次日 00:30；+08:00 与毫秒+08:00→16:30；无偏移不变",
                  not bad,
                  "落库明细=%s%s" % (json.dumps(got, ensure_ascii=False),
                                     "" if not bad else "；偏差=" + "；".join(bad)))

        # 2) 仅日期 → 行级 NEEDS_REVIEW / TIME_UNAVAILABLE；非法 → 行级 ERROR / TXN_TIME_INVALID
        row_info = {}
        for r in self.rows("SELECT row_no, status, COALESCE(error_code,'') FROM import_rows"
                           " WHERE batch_id=%s ORDER BY row_no" % bid):
            row_info[int(r[0])] = (r[1], r[2])
        dateonly = row_info.get(5, ("?", ""))
        invalid = row_info.get(6, ("?", ""))
        ok_dateonly = dateonly[0] in ("NEEDS_REVIEW", "ERROR") and (
            dateonly[1] in ("TIME_UNAVAILABLE", "") or dateonly[1].startswith("TIME_"))
        ok_invalid = invalid[0] == "ERROR" and invalid[1].startswith("TXN_TIME_INVALID")
        self.step("AC-54b 仅日期 → NEEDS_REVIEW/TIME_UNAVAILABLE；非法 → ERROR/TXN_TIME_INVALID",
                  ok_dateonly and ok_invalid,
                  "第5行(仅日期)=%s；第6行(非法)=%s" % (dateonly, invalid))

        # 3) 同一时刻三种写法必须一致
        same = {}
        for ref, t in self.rows(
                "SELECT external_txn_id, to_char(txn_time,'YYYY-MM-DD HH24:MI:SS')"
                " FROM bank_transactions WHERE deleted=0 AND external_txn_id LIKE '%%%s-SAME-%%'" % tag):
            same[ref] = t
        vals = sorted(set(same.values()))
        ok_same = len(same) == 3 and vals == ["2026-10-25 00:30:00"]
        return self.step("AC-54c 同一时刻三种写法（Z / +08:00 / -08:00）都落 2026-10-25 00:30:00",
                         ok_same, "明细=%s%s" % (json.dumps(same, ensure_ascii=False),
                                                "" if ok_same else "；期望三者都=2026-10-25 00:30:00"))

    # ---------- A25 ----------
    def a25(self):
        exists = self.table_exists("fund_flow_no_burn_ledger")
        if not exists:
            return self.not_ready("A25 烧号台账表存在", "fund_flow_no_burn_ledger 不存在（P3-5 后端未落盘）")
        # 口径（与 consistency A25 一致）：archived_flag=false 且该号**未落库**（fund_flows 里不存在）
        rc, out, err = self.psql(
            "SELECT COUNT(*) FROM fund_flow_no_burn_ledger l"
            " WHERE l.archived_flag = false"
            " AND NOT EXISTS (SELECT 1 FROM fund_flows f WHERE f.flow_no = l.flow_no)")
        db_cnt = int(out.strip()) if rc == 0 and out.strip().isdigit() else -1
        rc0, out0, _ = self.psql(
            "SELECT COUNT(*) FROM fund_flow_no_burn_ledger WHERE archived_flag = false")
        raw_cnt = int(out0.strip()) if rc0 == 0 and out0.strip().isdigit() else -1
        rc2, out2, _ = self.psql("SELECT COUNT(*) FROM fund_flow_no_burn_ledger")
        all_cnt = int(out2.strip()) if rc2 == 0 and out2.strip().isdigit() else -1
        self.step("A25 烧号台账表存在", True,
                  "fund_flow_no_burn_ledger 存在；archived_flag=false 原始行数=%d，其中**未落库**=%d"
                  "（A25 口径）；总行数=%d" % (raw_cnt, db_cnt, all_cnt))
        # 跑 consistency_check，抓 A24 / A25 两行（不改断言，只贴原值）
        cc = os.path.join(self.a.repo, "scripts", "qa", "consistency_check.py")
        try:
            o = subprocess.run([sys.executable, cc], capture_output=True, text=True, timeout=600,
                               encoding="utf-8", errors="replace", cwd=self.a.repo)
            text = (o.stdout or "") + (o.stderr or "")
            tail = [l for l in text.splitlines() if l.strip()][-1:] or [""]
            lines = [l.strip() for l in text.splitlines()
                     if ("A24" in l or "A25" in l) and ("[PASS]" in l or "[FAIL]" in l or "[INFO]" in l)]
            self.step("consistency_check 执行", True,
                      "exit=%s；末行=%s；A24/A25 输出=%s" % (o.returncode, tail[0][:120],
                                                           lines if lines else "（脚本中暂无 A24/A25 断言）"))
            m = None
            for l in lines:
                if "A25" in l:
                    m = re.search(r"rows=(\d+)", l)
            if m:
                cc_cnt = int(m.group(1))
                okk = cc_cnt == db_cnt
                self.step("A25 巡检值 == DB archived_flag=false 行数", okk,
                          "consistency A25 rows=%d；DB=%d%s" % (cc_cnt, db_cnt, "" if okk else "；偏差"))
            else:
                self.not_ready("A25 巡检值对拍", "consistency_check 输出中未找到 A25（断言未落盘）")
        except Exception as e:
            self.not_ready("consistency_check 执行", str(e)[:150])

    # ---------- 并发取号 ----------
    def concurrency(self):
        path = os.path.join(self.a.repo, "scripts", "qa", "seq_concurrency_check_p35.py")
        if not os.path.exists(path):
            return self.not_ready("并发取号自洽", "seq_concurrency_check_p35.py 不存在（call-number 未提供）")
        try:
            o = subprocess.run([sys.executable, path], capture_output=True, text=True, timeout=1200,
                               encoding="utf-8", errors="replace", cwd=self.a.repo)
        except Exception as e:
            return self.not_ready("并发取号自洽", str(e)[:150])
        text = (o.stdout or "") + (o.stderr or "")
        num = {}
        for key, pat in (("duplicate_rule_no", r"重复\s*rule_no\s*[=:：]\s*(\d+)"),
                         ("landed", r"落库(?:数|条数)\s*[=:：]\s*(\d+)"),
                         ("success", r"成功(?:数|条数)\s*[=:：]\s*(\d+)"),
                         ("burned", r"烧号(?:数|条数)?\s*[=:：]\s*(\d+)"),
                         ("issued", r"取号(?:数|条数)\s*[=:：]\s*(\d+)")):
            m = re.search(pat, text)
            if m:
                num[key] = int(m.group(1))
        okk = o.returncode == 0
        detail = "exit=%s；解析=%s" % (o.returncode, num or "未解析到数字（贴原文）")
        if num:
            if "duplicate_rule_no" in num and num["duplicate_rule_no"] != 0:
                okk = False
            if "landed" in num and "success" in num and num["landed"] != num["success"]:
                okk = False
            if all(k in num for k in ("burned", "issued", "landed")) and \
                    num["burned"] != num["issued"] - num["landed"]:
                okk = False
        else:
            tail = [l for l in text.splitlines() if l.strip()][-6:]
            detail += "；末几行=" + " | ".join(x[:80] for x in tail)
        return self.step("并发取号自洽（不重复 rule_no / 落库=成功 / 烧号=取号−落库）", okk, detail)

    # ---------- 主流程 ----------
    def run(self):
        print("=" * 88)
        print("P3-5：AC-54 时区偏移导入（BL-9） + A25 烧号台账 + 并发取号自洽")
        print("=" * 88)
        if not ensure_ready(api=self.a.api, admin=self.a.admin, pos=self.a.pos):
            self.not_ready("前置就绪（e2e_setup.ensure_ready）", "服务/库存前置未通过，见上方输出")
            return self.summary()
        if not self.login():
            return self.summary()
        self.ac54()
        self.a25()
        self.concurrency()
        return self.summary()

    def summary(self):
        fails = [r for r in self.results if r[1] == "FAIL"]
        nr = [r for r in self.results if r[1] == "NOT_READY"]
        print("\n" + "=" * 88)
        print("结果: %s  PASS=%d FAIL=%d%s" % (
            "全绿" if not fails and not nr else ("未就绪" if not fails else "存在失败"),
            len([r for r in self.results if r[1] == "PASS"]), len(fails),
            ("  NOT_READY=%d" % len(nr)) if nr else ""))
        for name, st, _ in self.results:
            print("  %-58s %s" % (name, st))
        if fails or nr:
            print("-" * 88)
            for name, st, detail in fails + nr:
                print("  * [%s] %s\n    %s" % (st, name, detail))
        print("=" * 88)
        if fails:
            return 1
        return 2 if nr else 0


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--api", default=DEFAULT_API)
    p.add_argument("--admin", default=DEFAULT_ADMIN)
    p.add_argument("--pos", default=DEFAULT_POS)
    p.add_argument("--user", default="qa_admin")
    p.add_argument("--password", default="Admin@123456")
    p.add_argument("--account-id", type=int, default=24)
    p.add_argument("--repo", default=r"P:\my-new-project")
    p.add_argument("--psql", default=DEFAULT_PSQL)
    p.add_argument("--pg-host", default="localhost")
    p.add_argument("--pg-port", type=int, default=5432)
    p.add_argument("--pg-user", default="postgres")
    p.add_argument("--pg-password", default="123456")
    p.add_argument("--pg-db", default="food_traceability")
    a = p.parse_args()
    try:
        return P35(a).run()
    except Exception as e:
        print("执行错误: %s" % e, file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
