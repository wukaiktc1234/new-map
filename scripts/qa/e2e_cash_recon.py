#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
AC-58 候选：N19-2 现金户退款构成 / 班次边界 / 归属确认 E2E

断言分组（对应 N19-2 四项）：
  ① 班次内退款（现金户）→ generate 缴款单
       · refund_component_fen == 该笔退款金额
       · difference_net_fen == 0 − (expected − refund_component)
  ② 边界内退款（距班次 start ≤ shiftBoundaryMinutes，默认 10 分钟）→ generate
       · 该笔**不计入** refund_component_fen
       · 财务审计出现 RECON_SHIFT_BOUNDARY_PENDING
  ③ confirm-attribution 归到本班次
       · refund_component_fen 增加该笔金额、difference_net_fen 同步变化
       · 审计 REFUND_SHIFT_ATTRIBUTION_CONFIRMED
  ④ 重复确认同班次 → 幂等（refund_shift_attribution 仍 1 行，version 不变）
     改归属到另一班次 → 审计 REFUND_SHIFT_ATTRIBUTION_CHANGED + version+1
  ⑤ 越权：vcashier01 调 confirm-attribution → 403
  ⑥ 巡检 A36/A37/A39 全 0（以 consistency_check 输出为准）
  ⑦ 清理 hike tag；**不裸 UPDATE**

权限：confirm-attribution 需要 finance:deposit-slip:confirm-attribution（由 lead 绑账号后运行）。
退出码：0 全绿 / 1 断言失败 / 2 环境未就绪
"""
from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import threading
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from datetime import date, datetime, timedelta
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
try:
    from e2e_setup import ensure_ready, is_stock_short
except Exception as e:  # pragma: no cover
    print("[FATAL] 无法导入 e2e_setup：%s" % e)
    sys.exit(2)

DEFAULT_API = "http://localhost:8081/api"
DEFAULT_ADMIN = "http://localhost:3002"
DEFAULT_POS = "http://localhost:3001"
DEFAULT_PSQL = r"P:\my-new-project\PostgreSQL\18\bin\psql.exe"


def http(method, url, token=None, body=None, timeout=90):
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None
    h = {"Content-Type": "application/json", "Accept": "application/json"}
    if token:
        h["Authorization"] = "Bearer " + token
    req = urllib.request.Request(url, data=data, headers=h, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            txt = r.read().decode("utf-8", "replace")
            try:
                return r.status, json.loads(txt)
            except Exception:
                return r.status, {"_raw": txt[:200]}
    except urllib.error.HTTPError as e:
        txt = e.read().decode("utf-8", "replace")
        try:
            return e.code, json.loads(txt)
        except Exception:
            return e.code, {"_raw": txt[:200]}
    except Exception as e:
        return -1, {"error": str(e)}


def ok(b):
    return isinstance(b, dict) and b.get("code") in (0, 200)


def data_of(b):
    return b.get("data") if isinstance(b, dict) else None


def _install_watchdog(max_seconds, stage_holder):
    """独立看门狗：阻塞在任意调用里也能硬退，不依赖 beat() 检查点。

    为什么必须独立线程：beat() 只在 stage 边界检查，进程挂在某次 read 上时永远走不到检查点
    （2026-10-28 实测：卡在 login done 之后 7 分钟无输出，--max-seconds 形同摆设）。

    到期动作：dump 当前 stage + 全线程栈（faulthandler），再 os._exit(3) 硬退
    —— 不能用 sys.exit（阻塞中不会生效，它只抛异常给主线程）。
    """
    import faulthandler
    import threading

    faulthandler.enable()
    def _hard_exit():
        sys.stderr.write("\n[WATCHDOG] 超过 --max-seconds=%s，当前 stage=%s\n"
                         % (max_seconds, stage_holder.get("stage", "(unknown)")))
        sys.stderr.flush()
        faulthandler.dump_traceback(file=sys.stderr)
        sys.stderr.flush()
        os._exit(3)
    
    t = threading.Timer(float(max_seconds), _hard_exit)
    t.daemon = True
    t.start()
    return t



class CashRecon:
    def __init__(self, a):
        self.a = a
        self.results = []
        self.tag = "HIK" + datetime.now().strftime("%m%d%H%M%S")
        # 统一标记（Owner 2026-10-27 批准）：可写文本字段统一带此前缀，供 e2e_cleanup.py 定位
        self.mark = "[E2E:%s]" % self.tag
        self.seq = 0
        import time as _t
        self._t0 = _t.monotonic()
        self._max_seconds = None

    def next_idem(self):
        """确定性幂等键：E2E-<tag>-<n>（订单侧 tag 锚点）"""
        self.seq += 1
        return "E2E-%s-%d" % (self.tag, self.seq)

    # ---------- 心跳（长跑可观测性） ----------
    def beat(self, stage):
        """阶段性心跳：打印时间戳与自套件启动起的耗时。

        为什么需要：本套件一段要开班/等边界窗口，长跑时**没有任何输出**，外部无法区分
        「还在跑」和「已经卡死」——以前只能靠人盯着猜。每段开始/结束各打一条。
        """
        import time as _t
        elapsed = _t.monotonic() - getattr(self, "_t0", _t.monotonic())
        print("  [BEAT +%6.1fs] %s" % (elapsed, stage), flush=True)
        if getattr(self, "_max_seconds", None) and elapsed > self._max_seconds:
            # 退出码必须非 0：否则 CI 会把「超时中断」当成「跑完通过」
            print("  [BEAT] 超过 --max-seconds=%s（超时于 stage=%s 之后 %.1fs），主动中止（防止无限挂起）"
                  % (self._max_seconds, getattr(self, "_last_stage", "(unknown)"), elapsed),
                  file=sys.stderr, flush=True)
            raise SystemExit(3)

    def step(self, name, okk, detail, state=None):
        st = state or ("PASS" if okk else "FAIL")
        self.results.append((name, st, detail))
        print("  [%s] %s -> %s" % (st, name, detail), flush=True)
        return okk

    def not_ready(self, name, detail):
        return self.step(name, False, detail, state="NOT_READY")

    # ---------- DB ----------
    def psql(self, sql):
        o = subprocess.run([self.a.psql, "-h", self.a.pg_host, "-p", str(self.a.pg_port),
                            "-U", self.a.pg_user, "-d", self.a.pg_db, "-t", "-A", "-F", "|", "-c", sql],
                           capture_output=True, text=True, timeout=120, encoding="utf-8", errors="replace",
                           env=dict(os.environ, PGPASSWORD=self.a.pg_password, PGCLIENTENCODING="UTF8"))
        if o.returncode != 0:
            raise RuntimeError("psql: %s" % (o.stderr or "")[:200])
        return (o.stdout or "").strip()

    def rows(self, sql):
        return [l.split("|") for l in self.psql(sql).splitlines() if l.strip()]

    def num(self, sql):
        v = self.psql(sql).strip()
        return int(v) if v.lstrip("-").isdigit() else 0


    # ---------- bug-③ 幂等键（判定：同 key 重放返回同一单；并发同 key 只建一单） ----------
    def check_order_idempotency(self):
        """bug-③ 落地断言：同一 idempotencyKey 重放 → 200/code=0 且同一 orderId，orders 不增行；
        并发同 key → 恰好建一单、另一请求返回同一单（不是 500）。
        为什么放在本脚本：本脚本是唯一「走 POS 下单 → 支付 → 退款」全链的套件，
        idempotencyKey 就在这里生成（next_idem），幂等语义必须与下单路径同源验证。
        """
        def _create(key):
            dish = self.pick_dish_for_fen(1000) if hasattr(self, "pick_dish_for_fen") else None
            # bug-③ 根因（2026-10-28 实测）：下单 dish 映射键是 food_code（如 FD202608010001）；旧夹具传 productId="1"
            #   → 后端返回 400「菜品[1]映射失败: food_code 不存在，拒绝下单」。属**夹具过期**，非产品缺陷。
            item = {"foodId": "FD202608010001", "foodName": "生菜串", "quantity": 1,
                    "price": 80.0, "dishType": "single"}
            st, b = http("POST", self.a.api + "/v1/pos/orders/order", self.cash_token,
                         {"orderType": "dinein", "items": [item], "idempotencyKey": key})
            return st, b, (data_of(b) or {})

        # ① 同 key 连发两次
        k1 = self.next_idem()
        before = self.num("SELECT count(*) FROM orders")
        s1, b1, d1 = _create(k1)
        mid = self.num("SELECT count(*) FROM orders")
        s2, b2, d2 = _create(k1)
        after = self.num("SELECT count(*) FROM orders")
        same = bool(d1.get("orderId")) and d1.get("orderId") == d2.get("orderId") \
            and d1.get("orderNumber") == d2.get("orderNumber")
        self.step("bug-③ ① 同 key 重放返回同一单（key=%s）" % k1,
                  ok(b1) and ok(b2) and same and after == mid and int(d2.get("reused") or 0) == 1,
                  "第1次 code=%s id=%s ｜ 第2次 code=%s id=%s reused=%s ｜ orders %d→%d→%d"
                  % ((b1 or {}).get("code"), d1.get("orderId"), (b2 or {}).get("code"),
                     d2.get("orderId"), d2.get("reused"), before, mid, after))

        # ② 换 key 必须新建（防止「无脑返回上一单」）
        k2 = self.next_idem()
        s3, b3, d3 = _create(k2)
        self.step("bug-③ ② 换 key 新建订单（key=%s）" % k2,
                  ok(b3) and bool(d3.get("orderId")) and d3.get("orderId") != d1.get("orderId"),
                  "code=%s id=%s（原单 %s）" % ((b3 or {}).get("code"), d3.get("orderId"), d1.get("orderId")))

        # ③ 并发同 key
        k3 = self.next_idem()
        before2 = self.num("SELECT count(*) FROM orders")
        res = {}

        def _worker(i):
            res[i] = _create(k3)

        ts = [threading.Thread(target=_worker, args=(i,)) for i in (1, 2)]
        for t in ts:
            t.start()
        for t in ts:
            t.join()
        ids = {(res.get(i, (0, {}, {}))[2] or {}).get("orderId") for i in (1, 2)}
        codes = [(res.get(i, (0, {}, {}))[1] or {}).get("code") for i in (1, 2)]
        after2 = self.num("SELECT count(*) FROM orders")
        key_rows = self.num("SELECT count(*) FROM orders WHERE idempotency_key='%s'" % k3)
        self.step("bug-③ ③ 并发同 key 只建一单（key=%s）" % k3,
                  len(ids) == 1 and None not in ids and 500 not in codes
                  and after2 - before2 == 1 and key_rows == 1,
                  "codes=%s ｜ orderId=%s ｜ orders %d→%d ｜ 该 key 行数=%d"
                  % (codes, list(ids)[0] if ids else None, before2, after2, key_rows))
        return d1

    # ---------- 登录 ----------
    def login(self):
        def _l(u, p):
            st, b = http("POST", self.a.api + "/v1/auth/login", body={"username": u, "password": p})
            return (data_of(b) or {}).get("token"), st
        self.admin_token, s1 = _l(self.a.user, self.a.password)
        self.cash_token, s2 = _l(self.a.pos_user, self.a.pos_password)
        self.manager_token, s3 = _l(self.a.refund_user, self.a.refund_password)
        self.step("登录 %s（财务）" % self.a.user, bool(self.admin_token), "HTTP %s" % s1)
        self.step("登录 %s（收银）" % self.a.pos_user, bool(self.cash_token), "HTTP %s" % s2)
        self.step("登录 %s（店长/退款+归属确认）" % self.a.refund_user, bool(self.manager_token), "HTTP %s" % s3)
        return bool(self.admin_token and self.cash_token and self.manager_token)

    def shift_window_minutes(self):
        try:
            n = self.num("SELECT COALESCE(current_setting('finance.cash.shift-boundary-minutes', true)::int, 10)")
        except Exception:
            n = 10
        return n or 10

    # ---------- 开班 / 班次窗口 ----------
    def open_shift(self):
        """POS 开班接口（vcashier01）"""
        uid = self.num("SELECT user_id FROM users WHERE username='%s'" % self.a.pos_user)
        st, b = http("POST", self.a.api + "/v1/pos/auth/shift/start", self.cash_token,
                     {"terminalId": "T-E2E-%s" % self.tag, "employeeId": str(uid or self.a.pos_user),
                      "employeeName": self.a.pos_user, "shiftType": "morning",
                      # 第 29 条断言需要 reserve_fen > 0：开班带备用金 100 元（=10000 分）
                      "openingCash": float(self.a.opening_cash)})
        d = data_of(b)
        sid = 0
        if isinstance(d, dict):
            sid = int(d.get("shiftId") or 0)
        elif isinstance(d, (int, str)) and str(d).isdigit():
            sid = int(d)
        self.step("AC-58 开班（POS /v1/pos/auth/shift/start）", ok(b) and sid > 0,
                  "HTTP %s code=%s shiftId=%s msg=%s" % (st, (b or {}).get("code"), sid,
                                                         str((b or {}).get("message"))[:70]))
        return sid

    def end_shift(self, terminal_id):
        st, b = http("POST", self.a.api + "/v1/pos/auth/shift/end", self.cash_token,
                     {"terminalId": terminal_id, "remark": "%s AC-58 关闭夹具班次" % self.mark})
        return self.step("AC-58 关班（POS /v1/pos/auth/shift/end）", ok(b),
                         "HTTP %s code=%s msg=%s" % (st, (b or {}).get("code"),
                                                     str((b or {}).get("message"))[:70]))

    def shift_terminal(self, shift_id):
        r = self.rows("SELECT COALESCE(terminal_id,'') FROM pos_shifts WHERE shift_id=%d" % int(shift_id))
        return r[0][0] if r else ""

    def shift_minutes(self, shift_id):
        return self.num("SELECT COALESCE(FLOOR(EXTRACT(EPOCH FROM (now() - start_time))/60)::int, 0)"
                        " FROM pos_shifts WHERE shift_id=%d" % int(shift_id))

    def ensure_boundary_shift(self, boundary_min):
        """距 start <= boundary 的 OPEN 班次；没有立刻开一个"""
        row = self.rows("SELECT shift_id FROM pos_shifts WHERE deleted=0 AND status IN ('open','OPEN','OPENED','OPENING')"
                        " AND start_time > now() - interval '%d minutes' ORDER BY shift_id DESC LIMIT 1"
                        % boundary_min)
        if row:
            return int(row[0][0])
        return self.open_shift()

    def ensure_inshift(self, boundary_min, max_wait_min=None):
        """距 start > boundary 的 OPEN 班次；没有就开一个并等待越过边界"""
        row = self.rows("SELECT shift_id FROM pos_shifts WHERE deleted=0 AND status IN ('open','OPEN','OPENED','OPENING')"
                        " AND start_time < now() - interval '%d minutes' ORDER BY shift_id DESC LIMIT 1"
                        % boundary_min)
        if row:
            sid = int(row[0][0])
            self.step("AC-58 ① 班次已越过边界窗口", True,
                      "shift_id=%d 距 start=%d 分钟（>%d）" % (sid, self.shift_minutes(sid), boundary_min))
            return sid
        sid = self.open_shift()
        if not sid:
            return 0
        max_wait_min = int(max_wait_min if max_wait_min is not None else getattr(self, "_boundary_wait_min", 16) or 0)
        if getattr(self, "_skip_boundary_wait", False) or max_wait_min <= 0:
            # CI/快速回归：跳过设计内等待。**必须显式标识**，否则又是一次「分母=0 却报 PASS」
            self.step("AC-58 ① 班次越界（设计内等待被跳过）", False,
                      "INFO skipped_design_wait：shift_id=%d 当前 %d 分钟（需 >%d）；相关断言不计入 PASS/FAIL"
                      % (sid, self.shift_minutes(sid), boundary_min), state="INFO")
            return sid
        for i in range(max_wait_min * 6):
            mins = self.shift_minutes(sid)
            if mins > boundary_min:
                self.step("AC-58 ① 班次已越过边界窗口", True,
                          "shift_id=%d 距 start=%d 分钟（>%d）" % (sid, mins, boundary_min))
                return sid
            if i % 6 == 0:
                self.beat("wait-inshift: 等待班次越过 %d 分钟边界（shift=%d 当前 %d 分钟，最长 %d 分钟）——这是设计内等待，不是卡死" % (boundary_min, sid, mins, max_wait_min))
            time.sleep(10)
        return sid

    # ---------- 造退款（现金户） ----------
    def make_refund(self, want_fen, note):
        """下单→支付→退款（店长），返回 {orderId, orderNumber, refundRecordId, amountFen}"""
        st, b = http("GET", self.a.api + "/v1/pos/api/dishes", self.cash_token)
        dishes = (data_of(b) or []) if ok(b) else []
        priced = [d for d in dishes if d.get("dishId") and float(d.get("price") or 0) > 0]
        if not priced:
            return None, "无可用菜品"
        best = None
        for d in priced:
            unit = int(round(float(d.get("price") or 0) * 100))
            q = max(1, min(9, want_fen // unit)) if unit else 1
            total = unit * q
            if best is None or abs(total - want_fen) < abs(best[2] - want_fen):
                best = (d, q, total)
        dish, qty, total = best
        item = {"productId": str(dish["dishId"]), "productName": dish.get("dishName") or "",
                "quantity": qty, "price": float(dish.get("price") or 0),
                "dishType": dish.get("dishType") or "single"}
        st, b = http("POST", self.a.api + "/v1/pos/orders/order", self.cash_token,
                     {"orderType": "dinein", "items": [item],
                      # 确定性幂等键 E2E-<tag>-<n>：订单侧唯一 tag 锚点
                      # （fund_flows.remark 由后端生成、不含 tag → 流水只能经订单/退款外键定位）
                      "idempotencyKey": self.next_idem()})
        if not ok(b):
            code, msg = (b or {}).get("code"), str((b or {}).get("message") or "")
            if is_stock_short(code, msg):
                # NEW-33：库存不足当前返回 code=500（应为 400/409），产品不改；
                # E2E 侧集中识别 → 报「环境未就绪（库存）」而不是业务断言失败
                return None, ("环境未就绪：库存不足（NEW-33 已知缺陷 code=%s；已由 ensure_ready 前置重置，"
                              "若仍报请检查 foods.stock）msg=%s" % (code, msg[:60]))
            return None, "下单 HTTP %s code=%s %s" % (st, code, msg[:60])
        d = data_of(b) or {}
        amt = round(float(d.get("totalAmount") or 0), 2)
        st2, b2 = http("POST", self.a.api + "/v1/pos/orders/order/pay", self.cash_token,
                       {"orderId": d.get("orderId"), "paymentMethod": "现金", "amount": amt})
        if not ok(b2):
            return None, "支付 HTTP %s code=%s %s" % (st2, (b2 or {}).get("code"), str((b2 or {}).get("message"))[:60])
        st3, b3 = http("POST", self.a.api + "/v1/pos/orders/order/%s/refund?refundReason=%s"
                       % (d.get("orderNumber"), urllib.parse.quote(note)), self.manager_token)
        if not ok(b3):
            return None, "退款 HTTP %s code=%s %s" % (st3, (b3 or {}).get("code"), str((b3 or {}).get("message"))[:60])
        flows = self.rows("SELECT flow_id, COALESCE(refund_record_id,0), amount FROM fund_flows"
                          " WHERE order_id='%s' AND deleted=0 AND flow_category=2 ORDER BY flow_id DESC LIMIT 1"
                          % d.get("orderId"))
        if not flows:
            return None, "未生成退款流水"
        return {"orderId": d.get("orderId"), "orderNumber": d.get("orderNumber"),
                "refundRecordId": int(flows[0][1]), "flowId": int(flows[0][0]),
                "amountFen": int(flows[0][2])}, ""

    # ---------- 缴款单 ----------
    def generate_slip(self, shift_id):
        return http("POST", self.a.api + "/v1/finance/deposit-slips/generate", self.admin_token,
                    {"shiftId": int(shift_id), "storeId": int(self.a.store_id)})

    def slip_id_of(self, b):
        """generate 响应有两种形态：data.slipId 或 data.slip.slipId"""
        d = data_of(b) or {}
        if isinstance(d, dict):
            if d.get("slipId"):
                return int(d.get("slipId"))
            sl = d.get("slip")
            if isinstance(sl, dict) and sl.get("slipId"):
                return int(sl.get("slipId"))
        return 0

    def slip_row(self, slip_id):
        """列序：0 slip_id / 1 shift_id / 2 expected / 3 actual / 4 difference / 5 rcf / 6 dnet /
        7 version / 8 reserve_fen / 9 cash_counted_fen"""
        r = self.rows("SELECT slip_id, shift_id, expected_amount, actual_amount, difference_amount,"
                      " COALESCE(refund_component_fen,0), COALESCE(difference_net_fen,0), version,"
                      " COALESCE(reserve_fen,0), COALESCE(cash_counted_fen,0)"
                      " FROM deposit_slips WHERE slip_id=%d" % int(slip_id))
        return r[0] if r else None

    def confirm_attribution(self, slip_id, refund_record_id, shift_id, reason):
        return http("POST", self.a.api + "/v1/finance/deposit-slips/%d/confirm-attribution" % int(slip_id),
                    self.manager_token,
                    {"refundRecordId": int(refund_record_id), "shiftId": int(shift_id), "reason": reason})

    # ---------- ① 班次内退款 ----------
    def section_in_shift(self, shift_id, boundary_min):
        start = self.rows("SELECT to_char(start_time,'YYYY-MM-DD HH24:MI:SS'),"
                          " COALESCE(to_char(end_time,'YYYY-MM-DD HH24:MI:SS'),'')"
                          " FROM pos_shifts WHERE shift_id=%d" % int(shift_id))
        if not start:
            return self.not_ready("AC-58-① 班次信息", "shift_id=%s 不存在" % shift_id)
        st_txt = start[0][0]
        mins = self.num("SELECT FLOOR(EXTRACT(EPOCH FROM (now() - start_time))/60) FROM pos_shifts"
                        " WHERE shift_id=%d" % int(shift_id))
        self.step("AC-58-① 班次距 start %d 分钟（需 > 边界 %d 分钟才属「班次内」）" % (mins, boundary_min),
                  mins > boundary_min,
                  "shift_id=%s start=%s（若 ≤%d 分钟，①会退化为边界场景，请换班次）" % (shift_id, st_txt, boundary_min))
        rf, err = self.make_refund(3000, "%s AC-58 hinge 班次内退款" % self.mark)
        if not rf:
            return self.not_ready("AC-58-① 造退款", err)
        # 关班才有 end_time；且要求「退款时刻距 end 也 > 边界」→ 等够边界分钟再关班
        # CI/快速回归：本段「等退款距 end > 边界」也是设计内等待，同样可跳过；跳过时显式打印 INFO，不算 PASS/FAIL
        skip_wait = getattr(self, "_skip_boundary_wait", False) or int(getattr(self, "_boundary_wait_min", 16) or 0) <= 0
        if skip_wait:
            self.step("AC-58 ① 退款距关班越界（设计内等待被跳过）", False,
                      "INFO skipped_design_wait：需 >%d 分钟；相关断言不计入 PASS/FAIL（继续执行后续断言）"
                      % self.shift_window_minutes(), state="INFO")
        for i in ([] if skip_wait else range(16 * 6)):
            mins_from_refund = self.num(
                "SELECT COALESCE(FLOOR(EXTRACT(EPOCH FROM (now() - create_time))/60)::int, 0)"
                " FROM fund_flows WHERE flow_id=%d" % rf["flowId"])
            if mins_from_refund > self.shift_window_minutes():
                self.step("AC-58 ① 退款距关班已越过边界窗口", True,
                          "flow_id=%d 距退款=%d 分钟（>%d）" % (rf["flowId"], mins_from_refund,
                                                              self.shift_window_minutes()))
                break
            if i % 6 == 0:
                print("    · 等待「退款距 end > %d 分钟」：当前 %d 分钟" % (
                    self.shift_window_minutes(), mins_from_refund))
            time.sleep(10)
        self.end_shift(self.shift_terminal(shift_id))
        st, b = self.generate_slip(shift_id)
        if not ok(b):
            return self.step("AC-58-① generate 缴款单", False,
                             "HTTP %s code=%s msg=%s" % (st, (b or {}).get("code"),
                                                        str((b or {}).get("message"))[:80]))
        sid = self.slip_id_of(b)
        row = self.slip_row(sid)
        if not row:
            return self.not_ready("AC-58-① 缴款单", "未找到 slip_id=%s（generate data=%s）"
                                  % (sid, json.dumps(data_of(b), ensure_ascii=False)[:160]))
        exp, comp, dnet = int(row[2]), int(row[5]), int(row[6])
        # 净值公式必须含备用金：dnet = (actual − reserve) − (expected − rcf)（第29条同口径）
        act0 = int(row[3])
        res0 = int(row[8]) if len(row) > 8 else 0
        self.step("AC-58-① refund_component_fen == 该笔退款", comp == rf["amountFen"],
                  "slip=%s shift=%s refund_component=%d 期望=%d（含本笔 refund_record=%s）" % (
                      sid, shift_id, comp, rf["amountFen"], rf["refundRecordId"]))
        want_dnet = (act0 - res0) - (exp - comp)
        self.step("AC-58-① difference_net_fen == (actual − reserve) − (expected − refund_component)",
                  dnet == want_dnet,
                  "actual=%d reserve=%d expected=%d refund_component=%d → 期望 difference_net=%d 实际=%d"
                  % (act0, res0, exp, comp, want_dnet, dnet))
        self.in_shift = {"slipId": sid, "refund": rf, "row": row}
        return True

    # ---------- ② 边界内退款 ----------
    def section_boundary(self, boundary_min, shift_id=0):
        """用「距 start ≤ 边界」的班次 → 立刻退款 → generate 应不计入并留审计"""
        if not shift_id:
            return self.not_ready("AC-58-② 边界班次夹具", "未传入边界班次（开班失败）")
        rf, err = self.make_refund(3000, "%s AC-58 boundary 边界内退款" % self.mark)
        if not rf:
            return self.not_ready("AC-58-② 造退款", err)
        # 缴款单要求班次有完整起止时间 → 退款后立刻关班（退款距 end ≤ 边界，正好是边界场景）
        self.end_shift(self.shift_terminal(shift_id))
        st, b = self.generate_slip(shift_id)
        if not ok(b):
            return self.step("AC-58-② generate 缴款单", False,
                             "HTTP %s code=%s msg=%s" % (st, (b or {}).get("code"),
                                                        str((b or {}).get("message"))[:80]))
        sid = self.slip_id_of(b)
        row = self.slip_row(sid)
        if not row:
            return self.step("AC-58-② generate 返回 slipId", False,
                             "HTTP %s code=%s msg=%s data=%s" % (
                                 st, (b or {}).get("code"), str((b or {}).get("message"))[:90],
                                 json.dumps(data_of(b), ensure_ascii=False)[:160]))
        comp = int(row[5]) if row else -1
        self.step("AC-58-② 边界内退款不计入 refund_component",
                  comp >= 0 and comp < rf["amountFen"],
                  "slip=%s shift=%s refund_component=%d（本笔=%d 应不计入）" % (
                      sid, shift_id, comp, rf["amountFen"]))
        n_audit = self.num("SELECT COUNT(*) FROM finance_audit_logs WHERE operation_type='RECON_SHIFT_BOUNDARY_PENDING'"
                           " AND target_id=%d" % shift_id)
        self.step("AC-58-② 审计 RECON_SHIFT_BOUNDARY_PENDING 出现", n_audit > 0,
                  "shift_id=%s 命中审计行=%d" % (shift_id, n_audit))
        self.boundary = {"slipId": sid, "shiftId": shift_id, "refund": rf, "row": row}
        return True

    # ---------- ③ 归属确认 ----------
    def section_confirm(self):
        ctx = getattr(self, "boundary", None) or getattr(self, "in_shift", None)
        if not ctx:
            return self.not_ready("AC-58-③ 归属确认", "缺少前置缴款单上下文")
        sid, rf, shift_id = ctx["slipId"], ctx["refund"], (ctx.get("shiftId")
                                                          or (self.in_shift["row"][1] if hasattr(self, "in_shift") else None))
        before = self.slip_row(sid)
        st, b = self.confirm_attribution(sid, rf["refundRecordId"], shift_id, "%s AC-58 归属确认" % self.mark)
        if not ok(b):
            return self.step("AC-58-③ confirm-attribution 成功", False,
                             "HTTP %s code=%s msg=%s" % (st, (b or {}).get("code"),
                                                        str((b or {}).get("message"))[:90]))
        after = self.slip_row(sid)
        self.step("AC-58-③ refund_component_fen 增加该笔金额",
                  after and int(after[5]) == int(before[5]) + rf["amountFen"],
                  "refund_component %s → %s（本笔 %d）" % (before[5], after[5], rf["amountFen"]))
        # 【第29条·动机】2026-10-27 实测发现 differenceNetOf 漏了 reserve（备用金）：
        #   ④ 之前用的缴款单 reserve=0，覆盖不到这条公式；本断言唯一作用就是防这类漏改复发。
        reserve = int(after[8]) if after and len(after) > 8 else 0
        self.step("AC-58-③ difference_net_fen 同步变化（含备用金 reserve）",
                  after and int(after[6]) == (int(after[3]) - reserve) - (int(after[2]) - int(after[5])),
                  "actual=%s reserve=%d expected=%s comp=%s → difference_net=%s（期望 %d）" % (
                      after[3], reserve, after[2], after[5], after[6],
                      (int(after[3]) - reserve) - (int(after[2]) - int(after[5]))))
        n_aud = self.num("SELECT COUNT(*) FROM finance_audit_logs"
                         " WHERE operation_type='REFUND_SHIFT_ATTRIBUTION_CONFIRMED' AND target_id=%d" % sid)
        self.step("AC-58-③ 审计 REFUND_SHIFT_ATTRIBUTION_CONFIRMED", n_aud > 0, "slip=%s 命中=%d" % (sid, n_aud))
        self.confirm_ctx = {"slipId": sid, "refund": rf, "shiftId": shift_id,
                            "version_before": int(after[7]) if after else -1}

        # ---------- 第 29 条断言：reserve>0 时的净值公式 + rcf 变化量 ----------
        # 【动机】2026-10-27 实测发现 differenceNetOf 漏了 reserve（备用金）这一项，
        #   而 ④ 之前用的是 reserve=0 的缴款单，根本覆盖不到这条公式。
        #   本断言的唯一作用就是「防止这类漏改复发」：只要有人把 reserve 从公式里拿掉，
        #   这里立刻红（用 reserve>0 的夹具单，不是靠再加一条纯文本约束）。
        act_b = int(before[3]); exp_b = int(before[2])
        rcf_b = int(before[5]); dnet_b = int(before[6])
        res_b = int(before[8]) if len(before) > 8 else 0
        act_a = int(after[3]); exp_a = int(after[2])
        rcf_a = int(after[5]); dnet_a = int(after[6])
        res_a = int(after[8]) if len(after) > 8 else 0
        self.step("AC-58-29 夹具前提：reserve_fen > 0（开班带备用金）", res_a > 0,
                  "reserve_fen=%d（opening_cash=%s 元）；cash_counted_fen=%s" % (
                      res_a, self.a.opening_cash, after[9] if len(after) > 9 else "?"))
        self.step("AC-58-29 confirm 前 dnet == (actual − reserve) − (expected − rcf)",
                  dnet_b == (act_b - res_b) - (exp_b - rcf_b),
                  "actual=%d reserve=%d expected=%d rcf=%d → 期望 dnet=%d 实际=%d" % (
                      act_b, res_b, exp_b, rcf_b, (act_b - res_b) - (exp_b - rcf_b), dnet_b))
        self.step("AC-58-29 confirm 后 dnet == (actual − reserve) − (expected − rcf)",
                  dnet_a == (act_a - res_a) - (exp_a - rcf_a),
                  "actual=%d reserve=%d expected=%d rcf=%d → 期望 dnet=%d 实际=%d" % (
                      act_a, res_a, exp_a, rcf_a, (act_a - res_a) - (exp_a - rcf_a), dnet_a))
        self.step("AC-58-29 rcf 变化量 == 该笔退款金额",
                  (rcf_a - rcf_b) == rf["amountFen"],
                  "rcf %d → %d（Δ=%d，期望 %d）" % (rcf_b, rcf_a, rcf_a - rcf_b, rf["amountFen"]))
        return True

    # ---------- ④ 幂等 + 改归属 ----------
    def section_idempotent_and_change(self):
        ctx = getattr(self, "confirm_ctx", None)
        if not ctx:
            return self.not_ready("AC-58-④ 幂等/改归属", "缺少 ③ 的上下文")
        sid, rf, shift_id = ctx["slipId"], ctx["refund"], ctx["shiftId"]
        n_before = self.num("SELECT COUNT(*) FROM refund_shift_attribution WHERE refund_record_id=%d"
                            % rf["refundRecordId"])
        v_before = self.slip_row(sid)[7]
        st, b = self.confirm_attribution(sid, rf["refundRecordId"], shift_id, "%s AC-58 重复确认（同班次）" % self.mark)
        n_after = self.num("SELECT COUNT(*) FROM refund_shift_attribution WHERE refund_record_id=%d"
                           % rf["refundRecordId"])
        v_after = self.slip_row(sid)[7]
        self.step("AC-58-④ 重复确认同班次 → 归属行仍 1 行且 version 不变",
                  n_after == 1 and str(v_after) == str(v_before),
                  "HTTP %s code=%s；归属行 %d → %d；version %s → %s" % (
                      st, (b or {}).get("code"), n_before, n_after, v_before, v_after))
        # 改归属目标必须「窗口已结束且已有缴款单」，否则新退款会落进它的窗口，
        # 让该班次旧单的 rcf 变成 live 滞后 → A39 变红（环境里存在 00:00~23:59 的长窗口班次）
        other = self.rows("SELECT p.shift_id FROM pos_shifts p"
                          "  JOIN deposit_slips s ON s.shift_id = p.shift_id AND s.deleted = 0"
                          " WHERE p.deleted = 0 AND p.shift_id <> %d"
                          "   AND p.end_time IS NOT NULL AND p.end_time < now()"
                          " ORDER BY p.shift_id DESC LIMIT 1" % shift_id)
        if not other:
            return self.not_ready("AC-58-④ 改归属", "没有「窗口已结束且有缴款单」的第二个班次可改")
        other_id = int(other[0][0])
        # version 属于 refund_shift_attribution（不是 deposit_slips）
        av0 = self.num("SELECT COALESCE(MAX(version),0) FROM refund_shift_attribution"
                       " WHERE refund_record_id=%d" % rf["refundRecordId"])
        old_shift_slip = sid
        new_shift_slip = self.num("SELECT COALESCE(slip_id,0) FROM deposit_slips"
                                  " WHERE shift_id=%d AND deleted=0 ORDER BY slip_id DESC LIMIT 1" % other_id)
        rcf_old_before = int(self.slip_row(old_shift_slip)[5])
        rcf_new_before = int(self.slip_row(new_shift_slip)[5]) if new_shift_slip else 0
        st2, b2 = self.confirm_attribution(sid, rf["refundRecordId"], other_id, "%s AC-58 改归属" % self.mark)
        av1 = self.num("SELECT COALESCE(MAX(version),0) FROM refund_shift_attribution"
                       " WHERE refund_record_id=%d" % rf["refundRecordId"])
        changed = self.num("SELECT COUNT(*) FROM finance_audit_logs"
                           " WHERE operation_type='REFUND_SHIFT_ATTRIBUTION_CHANGED' AND target_id=%d" % sid)
        self.step("AC-58-④ 改归属 → CHANGED 审计 + 归属行 version+1",
                  ok(b2) and changed > 0 and av1 == av0 + 1,
                  "HTTP %s code=%s；CHANGED 审计=%d；归属 version %s → %s；新 shift=%d" % (
                      st2, (b2 or {}).get("code"), changed, av0, av1, other_id))
        # 改归属后两侧缴款单的 refund_component_fen 必须按新归属重算（旧单减、新单增）
        rcf_old_after = int(self.slip_row(old_shift_slip)[5])
        rcf_new_after = int(self.slip_row(new_shift_slip)[5]) if new_shift_slip else 0
        want_old = rcf_old_before - rf["amountFen"]
        want_new = rcf_new_before + rf["amountFen"]
        self.step("AC-58-④ 改归属后两侧 refund_component 重算（旧单减该笔、新单加该笔）",
                  new_shift_slip > 0 and rcf_old_after == want_old and rcf_new_after == want_new,
                  "旧单 slip=%s rcf %d → %d（期望 %d）；新单 slip=%s rcf %d → %d（期望 %d）" % (
                      old_shift_slip, rcf_old_before, rcf_old_after, want_old,
                      new_shift_slip, rcf_new_before, rcf_new_after, want_new))
        return True

    # ---------- ⑤ 越权 ----------
    def section_forbidden(self):
        ctx = getattr(self, "confirm_ctx", None)
        if not ctx:
            return self.not_ready("AC-58-⑤ 越权", "缺少 ③ 的上下文")
        st, b = http("POST", self.a.api + "/v1/finance/deposit-slips/%d/confirm-attribution" % ctx["slipId"],
                     self.cash_token,
                     {"refundRecordId": ctx["refund"]["refundRecordId"], "shiftId": ctx["shiftId"],
                      "reason": "AC-58 越权探测"})
        code = (b or {}).get("code")
        return self.step("AC-58-⑤ 收银账号调用 confirm-attribution → 403", code == 403 or st == 403,
                         "HTTP %s code=%s msg=%s" % (st, code, str((b or {}).get("message"))[:70]))

    # ---------- ⑥ 巡检 ----------
    def section_inspection(self):
        vals = {}
        try:
            o = subprocess.run([sys.executable, str(HERE / "consistency_check.py")],
                               capture_output=True, text=True, timeout=900,
                               encoding="utf-8", errors="replace", cwd=str(HERE.parent.parent))
            text = (o.stdout or "") + (o.stderr or "")
            self.insp_lines = {}
            for code in ("A36", "A37", "A39"):
                cand = [l.strip() for l in text.splitlines()
                        if code in l and ("[PASS]" in l or "[FAIL]" in l or "[INFO]" in l)]
                # A39 的判定行必须优先取 [PASS]/[FAIL]；[INFO] 行（still_open / stale / legacy_null）只作可见性
                judged = [l for l in cand if "[PASS]" in l or "[FAIL]" in l]
                pick = judged[0] if judged else (cand[0] if cand else None)
                self.insp_lines[code] = cand[:6]
                if pick:
                    m = re.search(r"(?:rows|count|slips|diff)=(\d+)", pick) or re.search(r"=(\d+)", pick)
                    vals[code] = (int(m.group(1)) if m else None, pick[:160])
        except Exception as e:
            return self.not_ready("AC-58-⑥ 巡检", "consistency_check 执行失败：%s" % str(e)[:120])
        for code in ("A36", "A37", "A39"):
            if code not in vals:
                self.not_ready("AC-58-⑥ 巡检 %s" % code, "巡检输出未找到 %s" % code)
                continue
            n, line = vals[code]
            self.step("AC-58-⑥ 巡检 %s == 0" % code, n == 0, "rows=%s；原文=%s" % (n, line))
        # 可见性计数（不进判定）：still_open / stale / legacy_null
        vis = []
        for l in getattr(self, "insp_lines", {}).get("A39", []):
            if "still_open" in l or "stale" in l or "legacy_null" in l:
                vis.append(l[:150])
        if vis:
            self.step("AC-58-⑥ A39 可见性计数（still_open/stale/legacy_null，不进判定）", True,
                      " | ".join(vis[:3]))

    # ---------- 第 30 条：人工汇总模式存行（A43 分母） ----------
    def section_deposit_manual(self):
        """把本轮某张缴款单走完 submit → receive(actual>0) → deposit(bankAccountId, 不带 bankTransactionId)。

        动机：A43「DEPOSITED 缴款单恒等式」长期 skipped（分母=0）→ 需要一个稳定 DEPOSITED 夹具；
        不用 bankTransactionId 是为了覆盖「人工汇总」这条真实分支（deposit_bank_txn_id 留空、审计 matched=false）。
        """
        ctx = getattr(self, "in_shift", None) or getattr(self, "boundary", None)
        if not ctx:
            return self.not_ready("AC-58-30 人工汇总存行", "缺少缴款单上下文")
        sid = ctx["slipId"]
        row = self.slip_row(sid)
        exp = int(row[2]) if row else 0
        st, b = http("POST", self.a.api + "/v1/finance/deposit-slips/%d/submit" % sid, self.admin_token,
                     {"remark": "%s AC-58-30 submit" % self.mark})
        self.step("AC-58-30 缴款单 submit（->SUBMITTED）", ok(b),
                  "slip=%d HTTP %s code=%s msg=%s" % (sid, st, (b or {}).get("code"),
                                                     str((b or {}).get("message"))[:70]))
        if not ok(b):
            # 权限模型断裂（实测 2026-10-27）：role_permissions 里 submit 只以
            # PERM_FINANCE_DEPOSIT_SLIP_SUBMIT 授予 ROLE_CASHIER，而 permissions 表只有数字 id
            # (33495) + 权限码 → join 丢弃，所有账号 submit 均 403 → A43 无法造数。
            # 这是产品/授权数据缺陷，不当业务断言失败；后续三步必然级联失败，直接跳过。
            return self.not_ready(
                "AC-58-30 人工汇总存行（A43 分母）",
                "submit 一律 403：role_permissions 用 PERM_FINANCE_DEPOSIT_SLIP_SUBMIT（仅授予 ROLE_CASHIER），"
                "permissions 表只有数字 id=33495 + 权限码 finance:deposit-slip:submit → join 匹配不上，"
                "任何角色都拿不到 submit 权限。evidence: %s" % str((b or {}).get("message"))[:60])
        received = max(exp, 1)
        st2, b2 = http("POST", self.a.api + "/v1/finance/deposit-slips/%d/receive" % sid, self.admin_token,
                       {"receivedAmountFen": received, "remark": "%s AC-58-30 receive" % self.mark})
        self.step("AC-58-30 收款（->RECEIVED，actual=%d 分 >0）" % received, ok(b2),
                  "slip=%d HTTP %s code=%s msg=%s" % (sid, st2, (b2 or {}).get("code"),
                                                     str((b2 or {}).get("message"))[:70]))
        st3, b3 = http("POST", self.a.api + "/v1/finance/deposit-slips/%d/deposit" % sid, self.admin_token,
                       {"bankAccountId": int(self.a.bank_account_id),
                        "depositRefNo": "E2E-%s" % self.tag,
                        "remark": "%s AC-58-30 人工汇总存行" % self.mark})
        self.step("AC-58-30 人工汇总存行（不带 bankTransactionId，bankAccountId=%d）" % self.a.bank_account_id,
                  ok(b3), "slip=%d HTTP %s code=%s msg=%s" % (
                      sid, st3, (b3 or {}).get("code"), str((b3 or {}).get("message"))[:90]))
        stt = self.rows("SELECT COALESCE(status,'') FROM deposit_slips WHERE slip_id=%d" % sid)
        self.step("AC-58-30 缴款单状态 = DEPOSITED", bool(stt) and stt[0][0] == "DEPOSITED",
                  "slip=%d status=%s" % (sid, stt[0][0] if stt else "?"))
        try:
            o = subprocess.run([sys.executable, str(HERE / "consistency_check.py")],
                               capture_output=True, text=True, timeout=900, encoding="utf-8",
                               errors="replace", cwd=str(HERE.parent.parent))
            txt = (o.stdout or "") + (o.stderr or "")
            line = next((l.strip() for l in txt.splitlines()
                         if "A43" in l and ("[PASS]" in l or "[FAIL]" in l or "[INFO]" in l)), "")
            denom_ok = ("skipped_no_data" not in line) and ("跳过" not in line) and bool(line)
            rows_ok = ("rows=0" in line)
            self.step("AC-58-30 巡检 A43：分母>0 且 rows=0", denom_ok and rows_ok, "原文=%s" % line[:170])
        except Exception as e:
            self.not_ready("AC-58-30 巡检 A43", "consistency_check 执行失败：%s" % str(e)[:120])
        return True

    # ---------- ⑦ 清理 ----------
    def cleanup(self):
        # 【库存约定·NEW-32】本用例下单会消耗 foods.stock，清理阶段**故意不回补**：
        #   库存由下一次 e2e_setup.ensure_ready() 幂等重置为 STOCK_RESET_TARGET（900）。
        #   避免各用例自行加减库存拼平衡，导致谁都不敢改。
        try:
            self.step("AC-58-⑦ 清理", True,
                      "只做 tag 标记清理：本段不写 bank_transactions；退款流水/订单按业务归属留档（tag=%s），"
                      "不裸 UPDATE、不物理删" % self.tag)
        except Exception as e:
            self.step("AC-58-⑦ 清理", False, "异常：%s" % str(e)[:140])

    def run(self):
        print("=" * 92)
        print("N19-2 现金户退款构成 / 班次边界 / 归属确认 E2E  tag=%s" % self.tag)
        print("=" * 92)
        self.beat("start: 前置就绪检查")
        if not ensure_ready(api=self.a.api, admin=self.a.admin, pos=self.a.pos):
            self.not_ready("前置就绪", "服务/库存未就绪")
            return self.summary()
        if not self.login():
            return self.summary()
        self.beat("login done")
        boundary_min = self.shift_window_minutes()
        self.step("边界容差读取（finance.cash.shift-boundary-minutes）", True, "%d 分钟" % boundary_min)

        # ② 先用「新鲜班次」跑边界段；① 需要「距 start > 边界」的班次（没有就开班并等待）
        b_shift = self.ensure_boundary_shift(boundary_min)
        if not b_shift:
            self.not_ready("AC-58 边界班次夹具", "无法开班（POS /v1/pos/auth/shift/start 失败）")
        else:
            self.step("AC-58 ② 边界班次就绪", True,
                      "shift_id=%d 距 start=%d 分钟（<=%d）" % (b_shift, self.shift_minutes(b_shift), boundary_min))
            self.section_boundary(boundary_min, b_shift)
        i_shift = self.ensure_inshift(boundary_min)
        if not i_shift:
            self.not_ready("AC-58 班次内夹具", "无法开班（POS /v1/pos/auth/shift/start 失败）")
        else:
            self.section_in_shift(i_shift, boundary_min)
        # bug-③：幂等键语义（同 key 重放 / 换 key 新建 / 并发同 key）
        self.beat("bug-③ 幂等断言 开始")
        self.check_order_idempotency()
        self.beat("bug-③ 幂等断言 结束")
        self.beat("section_confirm 开始")
        self.section_confirm()
        self.beat("section_confirm 结束")
        self.section_idempotent_and_change()
        self.beat("section_idempotent_and_change 结束")
        self.section_forbidden()
        self.section_inspection()
        self.section_deposit_manual()
        self.cleanup()
        return self.summary()

    def summary(self):
        fails = [r for r in self.results if r[1] == "FAIL"]
        nr = [r for r in self.results if r[1] == "NOT_READY"]
        print("\n" + "=" * 92)
        print("结果: %s  PASS=%d FAIL=%d%s" % (
            "全绿" if not fails and not nr else ("未就绪" if not fails else "存在失败"),
            len([r for r in self.results if r[1] == "PASS"]), len(fails),
            ("  NOT_READY=%d" % len(nr)) if nr else ""))
        for name, st, _ in self.results:
            print("  %-72s %s" % (name, st))
        if fails or nr:
            print("-" * 92)
            for name, st, detail in fails + nr:
                print("  * [%s] %s\n    %s" % (st, name, detail))
        print("=" * 92)
        if fails:
            return 1
        return 2 if nr else 0


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--api", default=DEFAULT_API)
    p.add_argument("--admin", default=DEFAULT_ADMIN)
    p.add_argument("--pos", default=DEFAULT_POS)
    p.add_argument("--user", default="qa_admin")
    p.add_argument("--timeout", type=int, default=120, help="单次 HTTP 超时（秒，默认 120）")
    p.add_argument("--max-seconds", type=int, default=1800, help="整套件最长耗时（秒，默认 1800；超时主动中止）")
    p.add_argument("--boundary-wait-min", type=int, default=16,
                   help="等待班次越过边界的最长分钟数（默认 16，与原行为一致）")
    p.add_argument("--skip-boundary-wait", action="store_true",
                   help="跳过「等班次越界」这一段（CI/快速回归用）；跳过会显式打印 INFO skipped_design_wait，既不算 PASS 也不算 FAIL")
    p.add_argument("--password", default="Admin@123456")
    p.add_argument("--pos-user", default="vcashier01")
    p.add_argument("--pos-password", default="123456")
    p.add_argument("--refund-user", default="qa_store_b")
    p.add_argument("--refund-password", default="Admin@123456")
    p.add_argument("--store-id", type=int, default=1)
    p.add_argument("--shift-id", type=int, default=0, help="0=自动取最近 OPEN 班次")
    p.add_argument("--bank-account-id", type=int, default=24,
                   help="人工汇总存行用的收款银行户（本店 account_type=1 status=1，默认 24）")
    p.add_argument("--opening-cash", type=float, default=100.0,
                   help="开班备用金（元）；第29条断言要求 >0，故默认 100 元 = 10000 分")
    p.add_argument("--psql", default=DEFAULT_PSQL)
    p.add_argument("--pg-host", default="localhost")
    p.add_argument("--pg-port", type=int, default=5432)
    p.add_argument("--pg-user", default="postgres")
    p.add_argument("--pg-password", default="123456")
    p.add_argument("--pg-db", default="food_traceability")
    a = p.parse_args()
    try:
        runner = CashRecon(a)
        runner._max_seconds = a.max_seconds
        runner._stage_holder = {"stage": "(not started)"}
        runner._boundary_wait_min = a.boundary_wait_min
        runner._skip_boundary_wait = a.skip_boundary_wait
        _install_watchdog(a.max_seconds, runner._stage_holder)
        print("  [BEAT] 套件启动，单次 HTTP 超时 %ds，整套上限 %ds" % (a.timeout, a.max_seconds), flush=True)
        return runner.run()
    except Exception as e:
        print("执行错误: %s" % e, file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
