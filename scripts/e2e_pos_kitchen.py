#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
POS -> 后厨 -> 出餐 端到端（E2E）链路验证脚本

链路：
  1. POS 登录（vcashier01）拿 JWT            POST /v1/pos/auth/login
  2. POS 下单，生成订单                       POST /v1/pos/orders/order
  3. 创建后厨单（KDS 视角看到该订单）          POST /v1/kitchen/orders/create
  4. 托盘绑定订单（使用 KDS 设备凭证）          POST /v1/tray/bind-order
  5. 后厨接单 -> 开始制作 -> 完成制作（出餐）   POST /v1/kitchen/orders/{id}/{receive|start-make|complete-make}

鉴权说明（两种模式）：
  A) --password 模式：走真实 POS 登录，拿真实 JWT（推荐日常使用）
  B) --mint 模式：用 application.yml 里的 dev jwt.secret 自签一个 token
     （仅用于无法拿到明文密码时打通链路；生产绝不可用）

KDS 设备凭证：
  Authorization: Bearer kds_{storeId}_{secret}
  secret 来自 application.yml 的 app.kds.tokens（本地默认门店 1 = kds-store1-dev-secret）

用法：
  python scripts/e2e_pos_kitchen.py --password 你的密码
  python scripts/e2e_pos_kitchen.py --mint --user-id 2080170081205395463
"""

import argparse
import base64
import hashlib
import hmac
import json
import sys
import time
import urllib.error
import urllib.request

DEFAULT_BASE = "http://127.0.0.1:8081/api"
DEFAULT_JWT_SECRET = (
    "dev-only-not-for-production-use-this-key-must-be-at-least-64-bytes-long-for-hs512-algorithm-security"
)
DEFAULT_ISSUER = "food-traceability-system"
DEFAULT_KDS_TOKEN = "kds_1_kds-store1-dev-secret"


# ---------------------------------------------------------------- HTTP helper
def call(method, path, token=None, body=None, timeout=30):
    url = path if path.startswith("http") else DEFAULT_BASE + path
    data = None
    headers = {"Content-Type": "application/json", "Accept": "application/json"}
    if body is not None:
        data = json.dumps(body, ensure_ascii=False).encode("utf-8")
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            raw = resp.read().decode("utf-8", "replace")
            return resp.status, _safe_json(raw)
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", "replace")
        return e.code, _safe_json(raw)
    except Exception as e:  # 连接失败等
        return -1, {"error": str(e)}


def _safe_json(raw):
    try:
        return json.loads(raw)
    except Exception:
        return {"_raw": raw[:500]}


def brief(obj, limit=260):
    s = json.dumps(obj, ensure_ascii=False) if not isinstance(obj, str) else obj
    return s if len(s) <= limit else s[:limit] + "..."


# ---------------------------------------------------------------- JWT minting
def b64u(b):
    return base64.urlsafe_b64encode(b).rstrip(b"=").decode("ascii")


def mint_jwt(user_id, username, role_codes, permissions, secret=DEFAULT_JWT_SECRET, issuer=DEFAULT_ISSUER):
    """按 JwtUtils.generateToken 的结构自签 HS512 token。"""
    now = int(time.time())
    header = {"alg": "HS512", "typ": "JWT"}
    payload = {
        "userId": str(user_id),
        "username": username,
        "roles": [r.lower() for r in role_codes],   # JwtUtils 写 claims 时统一小写
        "permissions": permissions,
        "mfaEnabled": False,
        "mfaVerified": True,
        "tokenId": "e2e-" + str(now),
        "sub": username,
        "iss": issuer,
        "iat": now,
        "exp": now + 28800,
        "jti": "e2e-" + str(now),
    }
    signing_input = b64u(json.dumps(header, separators=(",", ":")).encode()) + "." + \
                    b64u(json.dumps(payload, separators=(",", ":")).encode())
    sig = hmac.new(secret.encode("utf-8"), signing_input.encode("ascii"), hashlib.sha512).digest()
    return signing_input + "." + b64u(sig)


# ---------------------------------------------------------------- steps
class Runner:
    def __init__(self, args):
        self.a = args
        self.results = []
        self.token = None
        self.order_id = None
        self.order_number = None
        self.kitchen_order_id = None

    def step(self, name, ok, detail):
        self.results.append((name, "PASS" if ok else "FAIL", detail))
        print(("  [%s] %s -> %s" % ("PASS" if ok else "FAIL", name, detail)))

    # 1. 登录
    def login(self):
        print("\n[1/5] POS 登录")
        if self.a.password:
            st, body = call("POST", "/v1/pos/auth/login", body={
                "employeeId": self.a.username,
                "password": self.a.password,
                "terminalId": self.a.terminal_id,
            })
            data = body.get("data") if isinstance(body, dict) else None
            token = (data or {}).get("token") if isinstance(data, dict) else None
            if st == 200 and token:
                self.token = token
                self.step("POS 登录", True, "HTTP 200，拿到 JWT（%d 字符）" % len(token))
            else:
                self.step("POS 登录", False, "HTTP %s %s" % (st, brief(body)))
        else:
            self.token = mint_jwt(self.a.user_id, self.a.username, self.a.roles, self.a.permissions)
            self.step("自签 JWT（--mint 模式）", True, "userId=%s roles=%s" % (self.a.user_id, self.a.roles))
        return bool(self.token)

    # 2. 下单
    def create_order(self):
        print("\n[2/5] POS 下单")
        payload = {
            "tableNumber": self.a.table_number,
            "items": [{
                "id": self.a.food_code,
                "name": self.a.food_name,
                "quantity": self.a.quantity,
                "price": self.a.food_price,
            }],
            "totalAmount": round(self.a.food_price * self.a.quantity, 2),
            "paymentMethod": "cash",
            "orderType": "DINE_IN",
            "storeId": str(self.a.store_id),
            "remark": "E2E 自动验证单",
            "idempotencyKey": "e2e-%d" % int(time.time() * 1000),
        }
        st, body = call("POST", "/v1/pos/orders/order", token=self.token, body=payload)
        data = body.get("data") if isinstance(body, dict) else None
        if isinstance(data, dict):
            self.order_id = str(data.get("orderId") or data.get("id") or "")
            self.order_number = str(data.get("orderNumber") or data.get("orderNo") or "")
        ok = st == 200 and bool(self.order_id or self.order_number)
        self.step("创建 POS 订单", ok, "HTTP %s orderId=%s orderNumber=%s" %
                  (st, self.order_id or "-", self.order_number or "-"))
        if not ok:
            print("       返回: " + brief(body))
        return ok

    # 3. 后厨单：POS 下单时后端会自动生成一条，优先复用；没有才创建
    def create_kitchen_order(self):
        print("\n[3/5] 获取后厨单（KDS 视角）")
        key = self.order_id or self.order_number
        st, body = call("GET", "/v1/kitchen/orders/order/%s" % key, token=self.a.kds_token)
        data = body.get("data") if isinstance(body, dict) else None
        existing = None
        if isinstance(data, list) and data:
            existing = data[0]
        elif isinstance(data, dict):
            existing = data
        if existing:
            self.kitchen_order_id = str(existing.get("kitchenOrderId") or existing.get("id") or "")
            self.step("复用 POS 下单自动生成的后厨单", True,
                      "HTTP %s kitchenOrderId=%s status=%s" %
                      (st, self.kitchen_order_id, existing.get("status")))
            return bool(self.kitchen_order_id)
        payload = {
            "orderId": self.order_id or self.order_number,
            "orderNumber": self.order_number or self.order_id,
            "orderType": 1,
            "tableNumber": str(self.a.table_number),
            "dishItems": [{
                "dishId": self.a.food_code,
                "dishName": self.a.food_name,
                "quantity": self.a.quantity,
                "remark": "E2E",
            }],
            "priority": 0,
            "storeId": self.a.store_id,
            "storeName": "E2E-STORE",
            "remark": "E2E 自动创建",
        }
        st, body = call("POST", "/v1/kitchen/orders/create", token=self.a.kds_token, body=payload)
        data = body.get("data") if isinstance(body, dict) else None
        if isinstance(data, dict):
            self.kitchen_order_id = str(data.get("kitchenOrderId") or data.get("id") or "")
        elif isinstance(data, str):
            self.kitchen_order_id = data
        ok = st == 200 and bool(self.kitchen_order_id)
        self.step("创建后厨单", ok, "HTTP %s kitchenOrderId=%s" % (st, self.kitchen_order_id or "-"))
        if not ok:
            print("       返回: " + brief(body))
        return ok

    # 4. 托盘绑定
    def bind_tray(self):
        print("\n[4/5] 托盘绑定订单（KDS 设备凭证）")
        tray_code = self.a.tray
        if tray_code in ("auto", "AUTO"):
            st0, b0 = call("GET", "/v1/tray/idle", token=self.a.kds_token)
            items = b0.get("data") if isinstance(b0, dict) else None
            picked = None
            if isinstance(items, list):
                for it in items:
                    if int(it.get("storeId") or 0) == self.a.store_id:
                        picked = it.get("trayCode")
                        break
            if not picked:
                self.step("自动挑选空闲托盘", False, "门店 %s 无空闲托盘，请先释放" % self.a.store_id)
                return False
            tray_code = picked
            self.step("自动挑选空闲托盘", True, tray_code)
        st, body = call("POST", "/v1/tray/bind-order", token=self.a.kds_token, body={
            "trayCode": tray_code,
            "orderId": self.order_id or self.order_number,
            "kitchenOrderId": int(self.kitchen_order_id) if self.kitchen_order_id.isdigit() else None,
        })
        data = body.get("data") if isinstance(body, dict) else None
        ok = st == 200 and isinstance(body, dict) and body.get("code") in (0, 200)
        self.step("绑定托盘 %s" % tray_code, ok,
                  "HTTP %s %s" % (st, brief(data if data is not None else body, 160)))
        if not ok:
            print("       返回: " + brief(body))
        return ok

    # 5. 后厨流转
    def kitchen_flow(self):
        print("\n[5/5] 后厨流转：接单 -> 开始制作 -> 完成制作（出餐）")
        ok_all = True
        # receive 需要 chefId + chefName（@RequestParam 必填），其余两步无需额外参数
        steps = [
            ("receive?chefId=%s&chefName=%s" % (self.a.chef_id, urllib.parse.quote(self.a.chef_name)), "接单"),
            ("start-make", "开始制作"),
            ("complete-make", "完成制作/出餐"),
        ]
        for action, label in steps:
            st, body = call("POST", "/v1/kitchen/orders/%s/%s" % (self.kitchen_order_id, action),
                            token=self.a.kds_token)
            data = body.get("data") if isinstance(body, dict) else None
            code = body.get("code") if isinstance(body, dict) else None
            # data=False 表示服务层拒绝（状态不允许），同样视为失败
            ok = st == 200 and code in (0, 200) and data is not False
            ok_all = ok_all and ok
            self.step("后厨 %s" % label, ok,
                      "HTTP %s code=%s data=%s %s" % (st, code, data,
                                                      "" if ok else brief(body.get("message") if isinstance(body, dict) else body, 120)))
        return ok_all

    def run(self):
        print("=" * 78)
        print("POS -> 后厨 -> 出餐 端到端验证")
        print("base=%s  user=%s  store=%s  tray=%s  kds_token=%s" %
              (DEFAULT_BASE, self.a.username, self.a.store_id, self.a.tray, self.a.kds_token))
        print("=" * 78)
        if not self.login():
            return self.summary(False)
        if not self.create_order():
            return self.summary(False)
        if not self.create_kitchen_order():
            return self.summary(False)
        if not self.bind_tray():
            return self.summary(False)
        if not self.kitchen_flow():
            return self.summary(False)
        return self.summary(True)

    def summary(self, ok):
        print("\n" + "=" * 78)
        print("结果汇总: " + ("全链路 PASS" if ok else "存在失败步骤，见上"))
        for name, state, detail in self.results:
            print("  %-24s %s" % (name, state))
        print("=" * 78)
        return 0 if ok else 1


def main():
    global DEFAULT_BASE
    p = argparse.ArgumentParser(description="POS -> 后厨 -> 出餐 E2E 验证")
    p.add_argument("--base", default=DEFAULT_BASE)
    p.add_argument("--username", default="vcashier01")
    p.add_argument("--password", default=None, help="POS 登录密码；不给则需 --mint")
    p.add_argument("--mint", action="store_true", help="用 dev jwt.secret 自签 token")
    p.add_argument("--user-id", default="2080170081205395463")
    p.add_argument("--roles", nargs="*", default=["ROLE_CASHIER"])
    p.add_argument("--permissions", nargs="*", default=["*"])
    p.add_argument("--terminal-id", default="E2E-TERMINAL-01")
    p.add_argument("--store-id", type=int, default=1)
    p.add_argument("--tray", default="KDS-TEST-001")
    p.add_argument("--food-id", type=int, default=2)
    p.add_argument("--food-code", default="FD260925001", help="菜品业务编码（POS 下单此字段必填）")
    p.add_argument("--food-name", default="QA-红烧土豆")
    p.add_argument("--food-price", type=float, default=18.00)
    p.add_argument("--quantity", type=int, default=1)
    p.add_argument("--table-number", type=int, default=0)
    p.add_argument("--kds-token", default=DEFAULT_KDS_TOKEN)
    p.add_argument("--chef-id", type=int, default=1, help="后厨接单必填：厨师ID")
    p.add_argument("--chef-name", default="E2E-CHEF", help="后厨接单必填：厨师姓名")
    args = p.parse_args()

    DEFAULT_BASE = args.base
    if not args.password and not args.mint:
        print("请提供 --password（真实登录）或 --mint（自签 token）")
        return 2
    return Runner(args).run()


if __name__ == "__main__":
    sys.exit(main())
