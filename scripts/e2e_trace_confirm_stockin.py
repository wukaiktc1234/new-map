#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
扫码确认入库 → 库存/流水/成本/入库单/状态 全链路 E2E（可复跑）

链路：
  1) 管理端登录 → POST /v1/material-trace-code/generate 造一条 pending 追溯码
     （或 --trace-code 指定已存在的 pending 码）
  2) 记录调用前基线：inventory.quantity（按 material_id/可选 location_id）、
     inventory_movement / cost_records / purchase_stockins 行数、追溯码状态
  3) POST /v1/material-trace-code/confirm-stockin?traceCode=&operatorName=
  4) 断言：
     A. inventory.quantity 增加量 == 追溯码 weight（容差 0.001）
     B. inventory_movement 新增 >=1 条，且新行 source_type 含 TRACE_CONFIRM
     C. cost_records 按「单价解析链」校对（DATA-007 收尾后的真实口径）：
        解析链 = a) 追溯码自身（当前无单价列）→ b) 入库明细 unit_price → c) 物料主档参考价
        material_archives.reference_price（单位：分）。本断言直接读 DB 的参考价判定：
          · reference_price > 0 → 本次窗口内新增**恰好 1 条**，amount == weight × reference_price（分，容差 1）
          · reference_price 空/0 → 本次**不新增成本行**（禁止落「金额 0 的错账」），且日志出现
            WARN「成本待补：无单价」
     D. purchase_stockins 新增 >=1 条，最新一行 stockin_type=2 且 status=1
     E. 后端日志出现「处理采购入库完成事件」（日志不可读时退化为「C 已写入」间接证明）
     F. 追溯码状态 == in_stock
  任一失败打印五项 before/after 数字与接口响应体。

用法：
  python scripts/e2e_trace_confirm_stockin.py --password 123456
  python scripts/e2e_trace_confirm_stockin.py --password 123456 --weight 5 --unit-price 12.5 --material-id 1
  python scripts/e2e_trace_confirm_stockin.py --password 123456 --trace-code TC202610240001XX
"""
import argparse
import json
import os
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from decimal import Decimal

DEFAULT_BASE = "http://127.0.0.1:8081/api"
DEFAULT_PSQL = r"P:\my-new-project\PostgreSQL\18\bin\psql.exe"
BASE = DEFAULT_BASE
TOL = Decimal("0.001")


def call(method, path, token=None, body=None, timeout=30):
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


def ok_code(body):
    return isinstance(body, dict) and body.get("code") in (0, 200)


def data_of(body):
    return body.get("data") if isinstance(body, dict) else None


def dump(obj, limit=400):
    try:
        return json.dumps(obj, ensure_ascii=False)[:limit]
    except Exception:
        return str(obj)[:limit]


def _repair_mojibake(text):
    """修复「UTF-8 字节被按 GBK 解码」的双重编码乱码（PowerShell 5.1 控制台重定向典型症状）。

    形如「鎵爜纭璁ゅ叆搴? / 閲囪喘鍏ュ簱」：先 encode('gbk') 还原原始字节，再按 UTF-8 解码。
    只有修复结果看起来更像中文（命中常见业务词）时才采用，避免误伤正常文本。
    """
    if not text:
        return text
    if not any(ch in text for ch in ("锛", "鎵", "閲", "纭", "搴", "璁")):
        return text
    try:
        repaired = text.encode("gbk", "ignore").decode("utf-8", "ignore")
    except Exception:
        return text
    markers = ("采购", "入库", "成本", "订单", "确认", "处理")
    if any(m in repaired for m in markers) and repaired.count("?") <= text.count("?"):
        return repaired
    return text


def dec(v, default="0"):
    if v is None or v == "":
        return Decimal(default)
    try:
        return Decimal(str(v).strip())
    except Exception:
        return Decimal(default)


class Runner:
    def __init__(self, a):
        self.a = a
        self.results = []
        self.token = None
        self.trace_code = a.trace_code or ""
        self.weight = dec(a.weight)
        self.unit_price = dec(a.unit_price)
        self.before = {}
        self.after = {}
        self.confirm_body = None
        self.log_offset = 0

    # ---------- 断言记录 ----------
    def step(self, name, ok, detail):
        state = "PASS" if ok else "FAIL"
        self.results.append((name, state, detail))
        print("  [%s] %s -> %s" % (state, name, detail))
        return ok

    # ---------- psql ----------
    def psql(self, sql):
        try:
            out = subprocess.run(
                [self.a.psql, "-h", self.a.pg_host, "-p", str(self.a.pg_port), "-U", self.a.pg_user,
                 "-d", self.a.pg_db, "-t", "-A", "-F", "|", "-c", sql],
                capture_output=True, text=True, timeout=25,
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

    # ---------- 1. 登录 ----------
    def login(self):
        if self.a.admin_token:
            self.token = self.a.admin_token.strip()
            return self.step("使用自签管理端 token（--admin-token）", bool(self.token),
                             "token=%d 字符（跳过 qa_admin 登录）" % len(self.token))
        st, body = call("POST", "/v1/auth/login", body={
            "username": self.a.admin_user, "password": self.a.admin_password})
        self.token = (data_of(body) or {}).get("token") if ok_code(body) else None
        return self.step("管理端登录（%s）" % self.a.admin_user, bool(self.token),
                         "HTTP %s token=%s %s" % (
                             st, ("%d 字符" % len(self.token)) if self.token else "-",
                             "" if ok_code(body) else dump(body)))

    # ---------- 2. 造 pending 追溯码 ----------
    def prepare_trace_code(self):
        if self.trace_code:
            st, body = call("GET", "/v1/material-trace-code/query/%s" %
                            urllib.parse.quote(self.trace_code), token=self.token)
            code = data_of(body) or {}
            self.material_id = str(code.get("materialId") or self.a.material_id)
            self.material_name = str(code.get("materialName") or self.a.material_name)
            if code.get("weight") is not None:
                self.weight = dec(code.get("weight"))
            return self.step("复用指定追溯码", bool(code) and str(code.get("status")) == "pending",
                             "traceCode=%s status=%s weight=%s materialId=%s" % (
                                 self.trace_code, code.get("status"), self.weight, self.material_id))

        payload = {
            "materialId": self.a.material_id,
            "materialName": self.a.material_name,
            "weight": float(self.a.weight),
            "weightUnit": self.a.weight_unit,
            "quantity": float(self.a.weight),
            "unit": self.a.weight_unit,
            "unitPrice": float(self.a.unit_price),
            "storeId": str(self.a.store_id),
            "storeName": self.a.store_name,
            "entryType": "quick",
            "generateCount": 1,
            "remark": "E2E 扫码确认入库",
        }
        st, body = call("POST", "/v1/material-trace-code/generate", token=self.token, body=payload)
        codes = data_of(body) or []
        code = codes[0] if isinstance(codes, list) and codes else {}
        self.trace_code = str(code.get("traceCode") or "")
        self.material_id = str(code.get("materialId") or self.a.material_id)
        self.material_name = str(code.get("materialName") or self.a.material_name)
        if code.get("weight") is not None:
            self.weight = dec(code.get("weight"))
        ok = ok_code(body) and bool(self.trace_code)
        return self.step("生成 pending 追溯码", ok,
                         "HTTP %s traceCode=%s materialId=%s weight=%s%s %s" % (
                             st, self.trace_code or "-", self.material_id, self.weight,
                             self.a.weight_unit, "" if ok else dump(body)))

    # ---------- 3. 基线 ----------
    def snapshot(self):
        mid = str(self.material_id).replace("'", "''")
        try:
            inv = dec(self.scalar(
                "SELECT COALESCE(SUM(quantity),0) FROM inventory WHERE material_id = '%s' AND deleted = 0" % mid, "0"))
            inv_rows = self.scalar(
                "SELECT COALESCE(string_agg(location_id::text || ':' || quantity::text, ','), '') "
                "FROM inventory WHERE material_id = '%s' AND deleted = 0" % mid, "") or ""
            mv = int(self.scalar("SELECT COUNT(*) FROM inventory_movement", "0"))
            mv_max = int(self.scalar("SELECT COALESCE(MAX(movement_id),0) FROM inventory_movement", "0"))
            cost = int(self.scalar("SELECT COUNT(*) FROM cost_records", "0"))
            cost_max = int(self.scalar("SELECT COALESCE(MAX(cost_id),0) FROM cost_records", "0"))
            si = int(self.scalar("SELECT COUNT(*) FROM purchase_stockins", "0"))
            si_max = int(self.scalar("SELECT COALESCE(MAX(stockin_id),0) FROM purchase_stockins", "0"))
            status = self.scalar(
                "SELECT status FROM material_trace_code WHERE trace_code = '%s' AND deleted = 0" %
                self.trace_code.replace("'", "''"), "")
        except Exception as e:
            raise RuntimeError("取基线失败: %s" % e)
        return {"inv": inv, "inv_rows": inv_rows, "mv": mv, "mv_max": mv_max,
                "cost": cost, "cost_max": cost_max, "si": si, "si_max": si_max,
                "status": status or ""}

    # ---------- 4. 确认入库 ----------
    def confirm(self):
        self.log_offset = self._log_size()
        try:
            self.t0 = self.scalar("SELECT to_char(now(), 'YYYY-MM-DD HH24:MI:SS.US')", "") or ""
        except Exception:
            self.t0 = ""
        path = "/v1/material-trace-code/confirm-stockin?traceCode=%s&operatorName=%s" % (
            urllib.parse.quote(self.trace_code), urllib.parse.quote(self.a.operator_name))
        st, body = call("POST", path, token=self.token)
        self.confirm_body = body
        ok = ok_code(body) and data_of(body) is True
        return self.step("调用 confirm-stockin", ok,
                         "HTTP %s code=%s data=%s %s" % (
                             st, (body or {}).get("code"), (body or {}).get("data"),
                             "" if ok else dump(body)))

    def resolve_reference_price(self):
        """物料主档参考价（单位：分）。返回 (price_fen, note)。"""
        mid = str(self.material_id or self.a.material_id).replace("'", "''")
        try:
            val = self.scalar(
                "SELECT COALESCE(reference_price,0) FROM material_archives "
                "WHERE material_id = %s AND deleted = 0" % mid, "0")
            return int(Decimal(str(val or "0")).to_integral_value()), "material_archives.reference_price"
        except Exception as e:
            return 0, "读取 material_archives.reference_price 失败: %s" % e

    def _read_log_after_offset(self):
        """读「本次 confirm 之后」的日志文本。

        兼容两种落盘编码：PowerShell 的 *> 重定向写 UTF-16LE（带 BOM），
        普通重定向/日志框架多为 UTF-8。UTF-16 时整文件解码后取尾部 20000 行。
        """
        path = self.a.backend_log
        if not os.path.exists(path):
            return ""
        with open(path, "rb") as fp:
            head = fp.read(2)
            fp.seek(0)
            data = fp.read()
        if head in (b"\xff\xfe", b"\xfe\xff"):
            text = data.decode("utf-16", "replace")
            return "\n".join(_repair_mojibake(text).splitlines()[-20000:])
        return _repair_mojibake(data[self.log_offset:].decode("utf-8", "replace"))

    def scan_log(self, token):
        """在本次 confirm 之后的日志片段里找关键字，返回最后一条命中行（截断）或空串。"""
        try:
            chunk = self._read_log_after_offset()
            hits = [ln for ln in chunk.splitlines() if token in ln]
            return hits[-1][-200:] if hits else ""
        except Exception:
            return ""

    def _log_size(self):
        try:
            return os.path.getsize(self.a.backend_log) if os.path.exists(self.a.backend_log) else 0
        except Exception:
            return 0

    # ---------- 5. 五项断言 ----------
    def assert_a_inventory(self):
        before = self.before["inv"]
        after = self.after["inv"]
        delta = after - before
        ok = abs(delta - self.weight) <= TOL
        return self.step("A inventory 库存增加量 == 追溯码 weight", ok,
                         "before=%s after=%s delta=%s 期望=%s（rows: %s）" % (
                             before, after, delta, self.weight, self.after.get("inv_rows")))

    def assert_b_movement(self):
        before = self.before["mv"]
        after = self.after["mv"]
        grew = after - before
        rows = []
        try:
            rc, out, err = self.psql(
                "SELECT movement_id, movement_type, source_type, change_qty, source_ref "
                "FROM inventory_movement WHERE movement_id > %d ORDER BY movement_id" % self.before["mv_max"])
            if rc == 0 and out:
                rows = [line.split("|") for line in out.splitlines() if line.strip()]
        except Exception:
            pass
        has_trace = any("TRACE_CONFIRM" in (r[2] or "").upper() for r in rows)
        ok = grew >= 1 and has_trace
        detail = "before=%d after=%d 新增=%d 新行=%s" % (
            before, after, grew,
            "; ".join("%s|type=%s|src=%s|qty=%s|ref=%s" % (r[0], r[1], r[2], r[3], (r[4] or "")[:40]) for r in rows[:5]) or "无")
        return self.step("B inventory_movement 新增且 source_type 含 TRACE_CONFIRM", ok, detail)

    def assert_c_cost(self):
        before = self.before["cost"]
        after = self.after["cost"]
        ref_fen, ref_note = self.resolve_reference_price()
        rows = []
        try:
            # 归因窗口：cost_type=7（采购入库成本）且 create_time >= 本次 confirm 开始时刻。
            # 用时间窗而非「cost_id > 最大值」纯粹计数，避免并发同跑时把别人的行算成自己的。
            rc, out, err = self.psql(
                "SELECT cost_id, cost_type, amount, quantity, unit_price, remark FROM cost_records "
                "WHERE cost_type = 7 AND create_time >= '%s' ORDER BY cost_id" % (self.t0 or "1970-01-01"))
            if rc == 0 and out:
                rows = [line.split("|") for line in out.splitlines() if line.strip()]
        except Exception:
            pass
        # 只认本次窗口（create_time >= confirm 开始时刻）内写入的 cost_type=7（采购入库成本）行
        ours = [r for r in rows if str(r[1]).strip() == "7"]
        row_txt = "; ".join("id=%s amt=%s up=%s remark=%s" % (
            r[0], r[2], r[4], (r[5] or "")[:28]) for r in ours[:3]) or "无"

        if ref_fen > 0:
            # 真实口径：amount(分) == weight × reference_price(分)，容差 1 分
            expected = self.weight * Decimal(ref_fen)
            actual = dec(ours[0][2]) if ours else None
            ok = len(ours) == 1 and actual is not None and abs(actual - expected) <= Decimal("1")
            detail = ("解析来源=物料主档参考价（%s，material_id=%s，reference_price=%s 分）× weight %s = %s 分；"
                      "before=%d after=%d 窗口新增=%d 条 amount=%s 期望=%s（容差 1 分）；"
                      "--unit-price=%s 仅打印参考、不参与断言；行=%s") % (
                ref_note, self.material_id, ref_fen, self.weight, expected,
                before, after, len(ours), actual, expected, self.a.unit_price, row_txt)
            return self.step("C cost_records 按单价解析链校对（参考价×weight）", ok, detail)

        # 参考价缺失：必须「不落成本行」+ 日志 WARN，禁止用金额 0 冒充已入账
        warn_line = self.scan_log("成本待补")
        log_readable = os.path.exists(self.a.backend_log)
        ok = len(ours) == 0 and bool(warn_line)
        detail = ("参考价缺失（%s，material_id=%s，reference_price=%s）；before=%d after=%d 窗口新增=%d 条（期望 0）；"
                  "WARN「成本待补」命中=%s%s；行=%s") % (
            ref_note, self.material_id, ref_fen, before, after, len(ours),
            ("是" if warn_line else "否"),
            "" if log_readable else "（日志不可读，无法验证 WARN）",
            warn_line or row_txt)
        return self.step("C 参考价缺失：不落成本行 + WARN「成本待补」", ok, detail)

    def assert_d_stockin(self):
        before = self.before["si"]
        after = self.after["si"]
        grew = after - before
        rows = []
        try:
            rc, out, err = self.psql(
                "SELECT stockin_id, stockin_code, stockin_type, status, total_quantity "
                "FROM purchase_stockins WHERE create_time >= '%s' ORDER BY stockin_id" % (self.t0 or "1970-01-01"))
            if rc == 0 and out:
                rows = [line.split("|") for line in out.splitlines() if line.strip()]
        except Exception:
            pass
        good = [r for r in rows if str(r[2]).strip() == "2" and str(r[3]).strip() == "1"]
        ok = len(good) >= 1
        return self.step("D purchase_stockins 新增且 stockin_type=2/status=1", ok,
                         "before=%d after=%d 窗口内新增=%d（type=2&status=1 的 %d 条）行=%s" % (
                             before, after, len(rows), len(good),
                             "; ".join("%s|%s|t=%s|s=%s|qty=%s" % (r[0], r[1], r[2], r[3], r[4]) for r in rows[:4]) or "无"))

    def assert_e_event(self):
        token = "处理采购入库完成事件"
        hit = False
        tail = ""
        try:
            chunk = self._read_log_after_offset()
            lines = [ln for ln in chunk.splitlines() if token in ln or "采购入库完成事件" in ln]
            hit = bool(lines)
            tail = lines[-1][-160:] if lines else ""
        except Exception as e:
            tail = "检查日志失败: %s" % e
        if hit:
            return self.step("E 后端日志出现「处理采购入库完成事件」", True, tail)
        # 退化断言：本次窗口内已写入 cost_type=7 的成本行（事件副作用的直接证据）
        cost_new = 0
        try:
            rc, out, err = self.psql(
                "SELECT COUNT(*) FROM cost_records WHERE cost_type = 7 AND create_time >= '%s'"
                % (self.t0 or "1970-01-01"))
            cost_new = int((out or "0").strip() or 0) if rc == 0 else 0
        except Exception:
            cost_new = 0
        return self.step("E 采购入库完成事件（日志不可见，退化断言 cost_records 已写入）", cost_new >= 1,
                         "日志未命中「%s」；窗口内 cost_type=7 新增=%d；日志片段=%s" % (
                             token, cost_new, tail or "（无）"))

    def assert_f_status(self):
        st, body = call("GET", "/v1/material-trace-code/query/%s" %
                        urllib.parse.quote(self.trace_code), token=self.token)
        code = data_of(body) or {}
        status = str(code.get("status") or "")
        ok = status == "in_stock"
        return self.step("F 追溯码状态 == in_stock", ok,
                         "HTTP %s status=%s scanTime=%s" % (st, status or "-", code.get("scanTime")))

    # ---------- 主流程 ----------
    def run(self):
        print("=" * 78)
        print("扫码确认入库全链路 E2E   base=%s  material=%s(%s) weight=%s%s unit_price=%s" % (
            BASE, self.a.material_id, self.a.material_name, self.a.weight, self.a.weight_unit,
            self.a.unit_price))
        print("=" * 78)
        if not self.login():
            return self.summary()
        if not self.prepare_trace_code():
            return self.summary()
        try:
            self.before = self.snapshot()
        except Exception as e:
            return self.step("取调用前基线", False, str(e)) and self.summary()
        print("  [data] 调用前基线 %s" % json.dumps(
            {k: str(v) for k, v in self.before.items()}, ensure_ascii=False))
        if not self.confirm():
            self.after = self.before
            return self.summary()
        try:
            self.after = self.snapshot()
        except Exception as e:
            return self.step("取调用后基线", False, str(e)) and self.summary()
        print("  [data] 调用后基线 %s" % json.dumps(
            {k: str(v) for k, v in self.after.items()}, ensure_ascii=False))
        self.assert_a_inventory()
        self.assert_b_movement()
        self.assert_c_cost()
        self.assert_d_stockin()
        self.assert_e_event()
        self.assert_f_status()
        return self.summary()

    def summary(self):
        fails = [r for r in self.results if r[1] == "FAIL"]
        print("\n" + "=" * 78)
        print("结果: %s    PASS=%d FAIL=%d" % (
            "全绿" if not fails else "存在失败", len(self.results) - len(fails), len(fails)))
        for name, state, detail in self.results:
            print("  %-52s %s" % (name, state))
        if fails:
            print("-" * 78)
            print("失败详情（含 before/after 与响应体）：")
            for name, state, detail in fails:
                print("  * %s\n    %s" % (name, detail))
            print("  before=%s" % json.dumps({k: str(v) for k, v in self.before.items()}, ensure_ascii=False))
            print("  after =%s" % json.dumps({k: str(v) for k, v in self.after.items()}, ensure_ascii=False))
            print("  confirm 响应=%s" % dump(self.confirm_body, 600))
            print("  traceCode=%s materialId=%s weight=%s" % (self.trace_code, self.a.material_id, self.a.weight))
        print("=" * 78)
        return 0 if not fails else 1


def main():
    global BASE
    p = argparse.ArgumentParser()
    p.add_argument("--base", default=DEFAULT_BASE)
    p.add_argument("--admin-user", default="qa_admin")
    p.add_argument("--admin-password", default="Admin@123456")
    p.add_argument("--admin-token", default=None,
                   help="直接使用自签 JWT（需含 trace:create/trace:update/trace:query），跳过登录")
    p.add_argument("--password", default=None,
                   help="兼容其他脚本的入参习惯（POS/通用密码）；管理端密码请用 --admin-password")
    p.add_argument("--trace-code", default=None, help="复用已存在的 pending 追溯码")
    p.add_argument("--material-id", default="1")
    p.add_argument("--material-name", default="娃娃菜")
    p.add_argument("--store-id", type=int, default=1)
    p.add_argument("--store-name", default="门店1")
    p.add_argument("--weight", default="5")
    p.add_argument("--weight-unit", default="kg")
    p.add_argument("--unit-price", default="12.5",
                   help="仅打印参考，不参与断言（真实单价走 material_archives.reference_price 解析链）")
    p.add_argument("--operator-name", default="qa_admin")
    p.add_argument("--backend-log", default=r"P:\my-new-project\.verify-tmp\boot-regress.log")
    p.add_argument("--psql", default=DEFAULT_PSQL)
    p.add_argument("--pg-host", default="localhost")
    p.add_argument("--pg-port", type=int, default=5432)
    p.add_argument("--pg-user", default="postgres")
    p.add_argument("--pg-password", default="123456")
    p.add_argument("--pg-db", default="food_traceability")
    a = p.parse_args()
    # 注意：--password 是「兼容其他脚本」的入参，**不得**覆盖管理端密码，
    # 否则 python ... --password 123456 会把 qa_admin 的密码顶成 123456 → 401。
    if a.password and a.admin_password == "Admin@123456":
        pass
    BASE = a.base
    return Runner(a).run()


if __name__ == "__main__":
    sys.exit(main())
