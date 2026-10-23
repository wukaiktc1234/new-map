#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
核心链路 E2E（点单 -> 支付 -> 出餐 -> 扣料）—— 绕过 orders_legacy 支付路由

背景：scripts/e2e_pos_kitchen.py 的支付步骤 POST /v1/pos/orders/order/pay 走
      PosOrderPaymentServiceImpl，查询的是 orders_legacy 表（Order 实体
      @TableName("orders_legacy")），而 POS 快速下单落 orders 表，必然"订单不存在"。
      新单的正确支付路由是 POST /v1/pos/orders/{orderId}/quick-pay。
      同理 POS 订单详情 GET /v1/pos/orders/order/{orderNumber} 也读 orders_legacy，
      故本脚本对订单状态与扣料流水改用直连活库断言（等价于直接调用接口的绕过）。

用法：
  python scripts/e2e_core_chain.py --password 123456 --tray auto
  python scripts/e2e_core_chain.py --password 123456 --tray none
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

DEFAULT_BASE = "http://127.0.0.1:8081/api"
DEFAULT_KDS_TOKEN = "kds_1_kds-store1-dev-secret"
BASE = DEFAULT_BASE


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


class Runner:
    def __init__(self, a):
        self.a = a
        self.results = []
        self.skips = []
        self.token = None
        self.order_id = None
        self.order_number = None
        self.kitchen_order_id = None

    def step(self, name, ok, detail, skip=False):
        state = "SKIP" if skip else ("PASS" if ok else "FAIL")
        self.results.append((name, state, detail))
        if skip:
            self.skips.append((name, detail))
        print("  [%s] %s -> %s" % (state, name, detail))

    # ---------- 1 登录 ----------
    def login(self):
        st, body = call("POST", "/v1/pos/auth/login", body={
            "employeeId": self.a.username, "password": self.a.password,
            "terminalId": self.a.terminal_id})
        token = ((body.get("data") or {}) if ok_code(body) else {}).get("token")
        self.token = token
        self.step("POS 登录", bool(token),
                  "HTTP %s token=%s" % (st, ("%d 字符" % len(token)) if token else "-"))
        return bool(token)

    # ---------- 2 下单 ----------
    def create_order(self):
        amount = round(self.a.food_price * self.a.quantity, 2)
        st, body = call("POST", "/v1/pos/orders/order", token=self.token, body={
            "tableNumber": self.a.table_number,
            "items": [{"id": self.a.food_code, "name": self.a.food_name,
                       "quantity": self.a.quantity, "price": self.a.food_price}],
            "totalAmount": amount, "paymentMethod": "cash", "orderType": "DINE_IN",
            "storeId": str(self.a.store_id), "remark": "E2E 核心链路",
            "idempotencyKey": "e2e-core-%d" % int(time.time() * 1000)})
        data = body.get("data") if isinstance(body, dict) else None
        if isinstance(data, dict):
            self.order_id = str(data.get("orderId") or data.get("id") or "")
            self.order_number = str(data.get("orderNumber") or data.get("orderNo") or "")
        self.step("POS 下单", ok_code(body) and bool(self.order_id),
                  "HTTP %s orderId=%s orderNumber=%s %s" % (
                      st, self.order_id or "-", self.order_number or "-",
                      "" if ok_code(body) else json.dumps(body, ensure_ascii=False)[:180]))
        return bool(self.order_id)

    # ---------- 3 支付（quick-pay 新路由） ----------
    def pay_order(self):
        cents = int(round(self.a.food_price * self.a.quantity * 100))
        st, body = call("POST", "/v1/pos/orders/%s/quick-pay" % self.order_id, token=self.token,
                        body={"payments": [{"paymentMethod": 1, "amount": cents}]})
        ok = ok_code(body)
        self.step("POS 支付 quick-pay（现金 %d 分）" % cents, ok,
                  "HTTP %s %s" % (st, "支付成功" if ok else json.dumps(body, ensure_ascii=False)[:180]))
        return ok

    # ---------- 4 订单落库断言（绕过 legacy 详情接口） ----------
    def _psql(self, sql):
        try:
            out = subprocess.run(
                [self.a.psql, "-h", self.a.pg_host, "-p", str(self.a.pg_port), "-U", self.a.pg_user,
                 "-d", self.a.pg_db, "-t", "-A", "-c", sql],
                capture_output=True, text=True, timeout=25,
                env=dict(os.environ, PGPASSWORD=self.a.pg_password))
            return out.returncode, (out.stdout or "").strip(), (out.stderr or "").strip()
        except Exception as e:
            return -1, "", str(e)

    def verify_pos_detail(self):
        """POS 详情接口必须能查到 orders 新表订单（改造前固定读 orders_legacy，必然"订单不存在"）"""
        key = self.order_number or self.order_id
        st, body = call("GET", "/v1/pos/orders/order/%s" % key, token=self.token)
        d = body.get("data") if isinstance(body, dict) else None
        ok = st == 200 and ok_code(body) and isinstance(d, dict) and bool(d.get("orderNumber"))
        self.step("POS 详情可查 orders 新表订单", ok,
                  "HTTP %s code=%s orderNumber=%s totalAmount=%s status=%s" % (
                      st, (body or {}).get("code"), (d or {}).get("orderNumber"),
                      (d or {}).get("totalAmount"), (d or {}).get("status")))
        return ok

    def verify_order_status(self):
        rc, out, err = self._psql(
            "select order_status || ',' || payment_status from orders where order_id = '%s'" % self.order_id)
        if rc != 0 or not out:
            self.step("订单落库状态", False, "psql rc=%s %s" % (rc, (err or out)[:160]))
            return False
        parts = (out.split(",") + ["", ""])[:2]
        # 此时只应校验「已支付」；order_status 要等出餐（后厨 complete-make）才变 2
        ok = parts[1] == "2"
        self.step("订单落库 payment_status=2（已支付）", ok,
                  "orderId=%s 实际 order_status=%s payment_status=%s" % (self.order_id, parts[0], parts[1]))
        return ok

    # ---------- 5 后厨单 ----------
    def acquire_kitchen_order(self):
        key = self.order_id or self.order_number
        st, body = call("GET", "/v1/kitchen/orders/order/%s" % key, token=self.a.kds_token)
        d = body.get("data") if isinstance(body, dict) else None
        existing = None
        if isinstance(d, list) and d:
            existing = d[0]
        elif isinstance(d, dict):
            existing = d
        if existing:
            self.kitchen_order_id = str(existing.get("kitchenOrderId") or existing.get("id") or "")
            self.step("复用自动生成的后厨单", bool(self.kitchen_order_id),
                      "kitchenOrderId=%s status=%s" % (self.kitchen_order_id, existing.get("status")))
            return bool(self.kitchen_order_id)
        st, body = call("POST", "/v1/kitchen/orders/create", token=self.a.kds_token, body={
            "orderId": key, "orderNumber": self.order_number or key, "orderType": 1,
            "tableNumber": str(self.a.table_number),
            "dishItems": [{"dishId": self.a.food_code, "dishName": self.a.food_name,
                           "quantity": self.a.quantity, "remark": "E2E"}],
            "priority": 0, "storeId": self.a.store_id, "storeName": "E2E-STORE",
            "remark": "E2E 核心链路"})
        d = body.get("data") if isinstance(body, dict) else None
        if isinstance(d, dict):
            self.kitchen_order_id = str(d.get("kitchenOrderId") or d.get("id") or "")
        elif isinstance(d, str):
            self.kitchen_order_id = d
        self.step("创建后厨单", bool(self.kitchen_order_id),
                  "HTTP %s id=%s" % (st, self.kitchen_order_id or "-"))
        return bool(self.kitchen_order_id)

    # ---------- 6 托盘（失败即跳过，不阻塞核心链路） ----------
    def bind_tray(self):
        if self.a.tray in ("none", ""):
            self.step("托盘绑定", True, "按 --tray none 跳过", skip=True)
            return
        tray_code = self.a.tray
        if tray_code.lower() == "auto":
            st, b = call("GET", "/v1/tray/idle", token=self.a.kds_token)
            items = b.get("data") if isinstance(b, dict) else None
            picked = None
            if isinstance(items, list):
                for it in items:
                    if int(it.get("storeId") or 0) == self.a.store_id:
                        picked = it.get("trayCode")
                        break
            if not picked:
                self.step("托盘绑定", False, "门店 %s 无空闲托盘（非核心链路，跳过）" % self.a.store_id, skip=True)
                return
            tray_code = picked
        st, body = call("POST", "/v1/tray/bind-order", token=self.a.kds_token, body={
            "trayCode": tray_code, "orderId": self.order_id or self.order_number,
            "kitchenOrderId": int(self.kitchen_order_id) if (self.kitchen_order_id or "").isdigit() else None})
        ok = ok_code(body)
        self.step("托盘绑定 %s" % tray_code, ok,
                  "HTTP %s %s" % (st, "" if ok else json.dumps(body, ensure_ascii=False)[:150]), skip=not ok)

    # ---------- 7 后厨流转 ----------
    def kitchen_status(self):
        st, body = call("GET", "/v1/kitchen/orders/%s" % self.kitchen_order_id, token=self.a.kds_token)
        d = body.get("data") if isinstance(body, dict) else None
        return d if isinstance(d, dict) else None

    def kitchen_flow(self):
        steps = [
            ("receive?chefId=%s&chefName=%s" % (self.a.chef_id, urllib.parse.quote(self.a.chef_name)),
             "接单", ("pending",)),
            ("start-make", "开始制作", ("received", "making")),
            ("complete-make", "完成制作/出餐", ("making",)),
        ]
        all_ok = True
        for action, label, allowed in steps:
            cur = (self.kitchen_status() or {}).get("status")
            if cur is not None and cur not in allowed:
                self.step("后厨 %s" % label, True, "SKIP（当前状态=%s）" % cur, skip=True)
                continue
            st, body = call("POST", "/v1/kitchen/orders/%s/%s" % (self.kitchen_order_id, action),
                            token=self.a.kds_token)
            ok = ok_code(body) and body.get("data") is not False
            if not ok:
                after = (self.kitchen_status() or {}).get("status")
                if after is not None and after not in allowed:
                    self.step("后厨 %s" % label, True, "SKIP（状态已变 %s）" % after, skip=True)
                    continue
            self.step("后厨 %s" % label, ok,
                      "HTTP %s %s" % (st, "" if ok else json.dumps(body, ensure_ascii=False)[:150]))
            all_ok = all_ok and ok
        return all_ok

    # ---------- 8 三端一致 + 扣料 ----------
    def verify_consistency(self):
        kds = self.kitchen_status() or {}
        ok = kds.get("status") == "completed" and kds.get("materialConsumed") == 1
        self.step("后厨单 completed + material_consumed=1", ok,
                  "status=%s materialConsumed=%s" % (kds.get("status"), kds.get("materialConsumed")))
        # 出餐后主订单应闭环：order_status=2（已完成）+ payment_status=2（已支付）
        rc, out, err = self._psql(
            "select order_status || ',' || payment_status from orders where order_id = '%s'" % self.order_id)
        if rc == 0 and out:
            parts = (out.split(",") + ["", ""])[:2]
            ok2 = parts[0] == "2" and parts[1] == "2"
            self.step("出餐后主订单 order_status=2 / payment_status=2", ok2,
                      "实际 order_status=%s payment_status=%s" % (parts[0], parts[1]))
            ok = ok and ok2
        else:
            self.step("出餐后主订单状态", False, "psql rc=%s %s" % (rc, (err or out)[:140]))
            ok = False
        return ok

    def verify_movement(self):
        # 扣料流水写在 inventory_movement.source_ref（形如「KDS完成制作扣料 - 订单:T2026...」）
        key = self.order_number or self.order_id
        rc, out, err = self._psql(
            "select count(*) from inventory_movement "
            "where source_type = 'SALE_DEDUCT' and source_ref like '%%%s%%'" % key)
        if rc != 0:
            self.step("扣料流水", False, "psql rc=%s %s" % (rc, err[:160]))
            return False
        cnt = int(out or 0)
        self.step("扣料流水 inventory_movement(SALE_DEDUCT)", cnt > 0,
                  "订单 %s 命中 %d 条出库流水" % (key, cnt))
        return cnt > 0

    def run(self):
        print("=" * 78)
        print("核心链路 E2E：点单 -> 支付 -> 出餐 -> 扣料   base=%s" % BASE)
        print("=" * 78)
        critical = (self.login, self.create_order, self.pay_order, self.verify_order_status,
                    self.verify_pos_detail, self.acquire_kitchen_order, self.kitchen_flow,
                    self.verify_consistency, self.verify_movement)
        for i, fn in enumerate(critical, 1):
            if i == 6:  # 托盘插在后厨单之后
                self.bind_tray()
            if not fn():
                print("\n[中断] 关键步骤失败：%s" % fn.__name__)
                return self.summary(False)
        return self.summary(True)

    def summary(self, ok):
        fails = [r for r in self.results if r[1] == "FAIL"]
        print("\n" + "=" * 78)
        print("结果: %s    PASS=%d FAIL=%d SKIP=%d" % (
            "核心链路全绿" if ok and not fails else "存在失败",
            len([r for r in self.results if r[1] == "PASS"]), len(fails), len(self.skips)))
        for name, state, detail in self.results:
            print("  %-34s %s" % (name, state))
        if self.skips:
            print("\n跳过项（按规则吞掉，不影响核心链路判定）:")
            for name, detail in self.skips:
                print("  - %s: %s" % (name, detail))
        print("=" * 78)
        return 0 if ok and not fails else 1


def main():
    global BASE
    p = argparse.ArgumentParser()
    p.add_argument("--base", default=DEFAULT_BASE)
    p.add_argument("--username", default="vcashier01")
    p.add_argument("--password", required=True)
    p.add_argument("--terminal-id", default="E2E-CORE-01")
    p.add_argument("--store-id", type=int, default=1)
    p.add_argument("--tray", default="auto")
    p.add_argument("--food-code", default="FD260925001")
    p.add_argument("--food-name", default="QA-红烧土豆")
    p.add_argument("--food-price", type=float, default=18.00)
    p.add_argument("--quantity", type=int, default=1)
    p.add_argument("--table-number", type=int, default=1)
    p.add_argument("--kds-token", default=DEFAULT_KDS_TOKEN)
    p.add_argument("--chef-id", type=int, default=2080155339590037512)
    p.add_argument("--chef-name", default="员工J")
    p.add_argument("--psql", default=r"P:\my-new-project\PostgreSQL\18\bin\psql.exe")
    p.add_argument("--pg-host", default="localhost")
    p.add_argument("--pg-port", type=int, default=5432)
    p.add_argument("--pg-user", default="postgres")
    p.add_argument("--pg-password", default="123456")
    p.add_argument("--pg-db", default="food_traceability")
    a = p.parse_args()
    BASE = a.base
    return Runner(a).run()


if __name__ == "__main__":
    sys.exit(main())
