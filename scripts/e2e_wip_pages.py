#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
收编页面自动联调自查（ReceivingMobile / VerifyCodePage / BankAccountManage）

覆盖三件事：
  1. 收货签名存证：POST /v1/receipt-confirmations/{id}/signatures（传 base64 图片）→ 200 且落库
  2. 防伪码查验：GET  /v1/receipt-confirmations/verify/{code} → 无效码 valid=false；有效码 valid=true
  3. 银行账户：POST 新建 → PUT 启停（BankAccountUpdateDTO.status）→ GET 复核 → DELETE 清理

鉴权：用 dev jwt.secret 自签 admin token（与 scripts/e2e_pos_kitchen.py --mint 同一套算法）。
用法：python scripts/e2e_wip_pages.py
"""
import argparse
import base64
import hashlib
import hmac
import json
import os
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

DEFAULT_BASE = "http://127.0.0.1:8081/api"
DEFAULT_JWT_SECRET = (
    "dev-only-not-for-production-use-this-key-must-be-at-least-64-bytes-long-for-hs512-algorithm-security"
)
DEFAULT_ISSUER = "food-traceability-system"
BASE = DEFAULT_BASE

# 1x1 透明 PNG（最小合法图片）
PNG_1PX = (
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="
)


def b64u(b):
    return base64.urlsafe_b64encode(b).rstrip(b"=").decode("ascii")


def mint_jwt(user_id, username, role_codes, permissions, secret=DEFAULT_JWT_SECRET, issuer=DEFAULT_ISSUER):
    now = int(time.time())
    header = {"alg": "HS512", "typ": "JWT"}
    payload = {
        "userId": str(user_id), "username": username, "roles": role_codes,
        "permissions": permissions, "mfaEnabled": False, "mfaVerified": True,
        "tokenId": "selfcheck-" + str(now), "sub": username, "iss": issuer,
        "iat": now, "exp": now + 3600, "jti": "selfcheck-" + str(now),
    }
    signing_input = b64u(json.dumps(header, separators=(",", ":")).encode()) + "." + \
                    b64u(json.dumps(payload, separators=(",", ":")).encode())
    sig = hmac.new(secret.encode("utf-8"), signing_input.encode("ascii"), hashlib.sha512).digest()
    return signing_input + "." + b64u(sig)


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
                return resp.status, {"_raw": raw[:300]}
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", "replace")
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {"_raw": raw[:300]}
    except Exception as e:
        return -1, {"error": str(e)}


def ok_code(body):
    return isinstance(body, dict) and body.get("code") in (0, 200)


class Checker:
    def __init__(self, a):
        self.a = a
        self.results = []
        self.skips = []
        self.token = mint_jwt(
            "2080170081205395463", "selfcheck-admin", ["admin"],
            ["*", "finance:bank:create", "finance:bank:update", "finance:bank:delete", "finance:bank:view"],
        )

    def step(self, name, ok, detail, skip=False):
        state = "SKIP" if skip else ("PASS" if ok else "FAIL")
        self.results.append((name, state, detail))
        if skip:
            self.skips.append((name, detail))
        print("  [%s] %s -> %s" % (state, name, detail))

    def psql(self, sql):
        try:
            out = subprocess.run(
                [self.a.psql, "-h", self.a.pg_host, "-p", str(self.a.pg_port), "-U", self.a.pg_user,
                 "-d", self.a.pg_db, "-t", "-A", "-c", sql],
                capture_output=True, text=True, timeout=25,
                env=dict(os.environ, PGPASSWORD=self.a.pg_password))
            return out.returncode, (out.stdout or "").strip(), (out.stderr or "").strip()
        except Exception as e:
            return -1, "", str(e)

    # ---------- 1. 收货签名存证 ----------
    def check_signature(self):
        rc, out, err = self.psql(
            "select confirmation_id from receipt_confirmations where deleted = 0 order by create_time desc limit 1")
        if rc != 0 or not out:
            self.step("收货签名存证", False, "活库无收货确认单可测（rc=%s %s）" % (rc, err[:120]), skip=True)
            return
        confirmation_id = out.strip()

        rc0, before, _ = self.psql(
            "select count(*) from receipt_confirmation_signatures where confirmation_id = %s and deleted = 0" % confirmation_id)
        before_cnt = int(before or 0)

        payload = {
            "signerType": "RECEIVER",
            "signerName": "自查-收货员",
            "signatureImage": "data:image/png;base64," + PNG_1PX,
        }
        st, body = call("POST", "/v1/receipt-confirmations/%s/signatures" % confirmation_id,
                        token=self.token, body=payload)
        ok = st == 200 and ok_code(body)
        self.step("POST 签名存证（base64 图片）", ok,
                  "HTTP %s confirmationId=%s %s" % (st, confirmation_id,
                                                    "" if ok else json.dumps(body, ensure_ascii=False)[:160]))

        rc, after, err = self.psql(
            "select count(*) from receipt_confirmation_signatures where confirmation_id = %s and deleted = 0" % confirmation_id)
        after_cnt = int(after or 0)
        self.step("签名落库 receipt_confirmation_signatures", after_cnt > before_cnt,
                  "confirmationId=%s 记录数 %d → %d" % (confirmation_id, before_cnt, after_cnt))

        st, body = call("GET", "/v1/receipt-confirmations/%s/signatures" % confirmation_id, token=self.token)
        rows = body.get("data") if isinstance(body, dict) else None
        ok = st == 200 and isinstance(rows, list) and len(rows) >= 1
        self.step("GET 签名列表", ok,
                  "HTTP %s 返回 %d 条" % (st, len(rows) if isinstance(rows, list) else -1))

    # ---------- 2. 防伪码查验 ----------
    def check_verify(self):
        bogus = "ZZZZZZZZZZZZZZZZ"
        st, body = call("GET", "/v1/receipt-confirmations/verify/%s" % bogus)
        d = body.get("data") if isinstance(body, dict) else None
        ok = st == 200 and isinstance(d, dict) and d.get("valid") is False
        self.step("防伪码-无效码返回 valid=false", ok,
                  "HTTP %s valid=%s message=%s" % (st, (d or {}).get("valid"), (d or {}).get("message")))

        rc, code, err = self.psql(
            "select verify_code from receipt_confirmations where verify_code is not null and verify_code <> '' limit 1")
        if rc != 0 or not code:
            self.step("防伪码-有效码校验", False, "活库暂无 verify_code 样本，跳过（%s）" % err[:100], skip=True)
            return
        st, body = call("GET", "/v1/receipt-confirmations/verify/%s" % code.strip())
        d = body.get("data") if isinstance(body, dict) else None
        ok = st == 200 and isinstance(d, dict) and d.get("valid") is True
        self.step("防伪码-有效码返回 valid=true", ok,
                  "HTTP %s verifyCode=%s confirmationCode=%s" % (st, code.strip(), (d or {}).get("confirmationCode")))

    # ---------- 3. 银行账户 CRUD ----------
    def check_bank_account(self):
        suffix = str(int(time.time() * 1000))[-8:]
        payload = {
            "accountName": "自查-基本户-" + suffix,
            "bankName": "自查银行",
            "accountNumber": "62220202020" + suffix,
            "accountType": 1,          # 1=基本户（后端数字编码）
            "openDate": "2026-10-23",
            "remark": "自检脚本创建，结束后删除",
        }
        st, body = call("POST", "/v1/finance/bank-accounts", token=self.token, body=payload)
        data = body.get("data") if isinstance(body, dict) else None
        account_id = None
        if isinstance(data, dict):
            account_id = data.get("accountId") or data.get("id")
        self.step("POST 新建银行账户", st == 200 and ok_code(body) and account_id is not None,
                  "HTTP %s accountId=%s %s" % (st, account_id,
                                               "" if ok_code(body) else json.dumps(body, ensure_ascii=False)[:160]))
        if not account_id:
            return

        st, body = call("PUT", "/v1/finance/bank-accounts/%s" % account_id, token=self.token,
                        body={"accountName": payload["accountName"], "bankName": payload["bankName"],
                              "accountType": 1, "status": 3, "remark": "停用自查"})
        self.step("PUT 启停（BankAccountUpdateDTO.status=3）", st == 200 and ok_code(body),
                  "HTTP %s %s" % (st, "" if ok_code(body) else json.dumps(body, ensure_ascii=False)[:160]))

        rc, out, err = self.psql(
            "select status from bank_accounts where account_id = %s and deleted = 0" % account_id)
        self.step("落库状态=3（停用）", rc == 0 and out.strip() == "3", "status=%s" % (out or err[:100]))

        st, body = call("DELETE", "/v1/finance/bank-accounts/%s" % account_id, token=self.token)
        self.step("DELETE 清理自查账户", st == 200 and ok_code(body),
                  "HTTP %s accountId=%s" % (st, account_id))

    # ---------- 4. 退款前置放宽（已支付未全退即可退，不要求「已完成」） ----------
    def check_refund(self):
        rc, row, err = self.psql(
            "select order_id || '|' || order_number from orders "
            "where payment_status = 2 and coalesce(refund_amount,0) < coalesce(paid_amount,0) "
            "and order_status not in (5) and deleted = 0 "
            "order by (order_status in (0,1)) desc, create_time desc limit 1")
        if rc != 0 or not row.strip():
            self.step("退款前置放宽（已支付未全退可退）", False, "无可用已支付订单样本，跳过", skip=True)
            return
        order_id, order_number = row.strip().split("|")

        rc, before, _ = self.psql(
            "select coalesce(paid_amount,0)::text || '|' || coalesce(refund_amount,0)::text || '|' || order_status "
            "from orders where order_id = '%s'" % order_id)
        paid, refunded, status = (before.strip().split("|") + ["", "", ""])[:3]

        reason = urllib.parse.quote("自检-未完成订单退款")
        st, body = call("POST", "/v1/pos/orders/order/%s/refund?refundReason=%s"
                        % (order_number, reason), token=self.token)
        ok = st == 200 and ok_code(body)
        self.step("未完成订单直接退款（order_status=%s）" % status, ok,
                  "HTTP %s orderNumber=%s paid=%s refunded=%s %s"
                  % (st, order_number, paid, refunded, "" if ok else json.dumps(body, ensure_ascii=False)[:140]))

        # 数值比较交给 SQL（refund_amount 是 numeric，与 paid 的整型字符串不能直接比）
        rc, after, err = self.psql(
            "select case when coalesce(refund_amount,0) = coalesce(paid_amount,0) then 'EQ' else 'NE' end "
            "|| '|' || order_status || '|' || "
            "(select count(*) from order_refund_records r where r.order_id = '%s' and r.deleted = 0) "
            "|| '|' || coalesce(refund_amount,0)::text "
            "from orders where order_id = '%s'" % (order_id, order_id))
        parts = (after.strip().split("|") + ["", "", "", ""])[:4]
        self.step("退款落库（refund_amount=paid、order_status=5、退款单 1 条）",
                  rc == 0 and parts[0] == "EQ" and parts[1] == "5" and parts[2] != "0",
                  "refund_amount=%s(=paid) order_status=%s refund_records=%s" % (parts[3], parts[1], parts[2]))

    def run(self):
        print("=" * 78)
        print("收编页面自动联调自查   base=%s" % BASE)
        print("=" * 78)
        self.check_signature()
        self.check_verify()
        self.check_bank_account()
        self.check_refund()
        fails = [r for r in self.results if r[1] == "FAIL"]
        print("\n" + "=" * 78)
        print("结果: %s   PASS=%d FAIL=%d SKIP=%d" % (
            "全部通过" if not fails else "存在失败",
            len([r for r in self.results if r[1] == "PASS"]), len(fails), len(self.skips)))
        for name, state, detail in self.results:
            print("  %-38s %s" % (name, state))
        if self.skips:
            print("\n跳过项:")
            for name, detail in self.skips:
                print("  - %s: %s" % (name, detail))
        print("=" * 78)
        return 0 if not fails else 1


def main():
    global BASE
    p = argparse.ArgumentParser()
    p.add_argument("--base", default=DEFAULT_BASE)
    p.add_argument("--psql", default=r"P:\my-new-project\PostgreSQL\18\bin\psql.exe")
    p.add_argument("--pg-host", default="localhost")
    p.add_argument("--pg-port", type=int, default=5432)
    p.add_argument("--pg-user", default="postgres")
    p.add_argument("--pg-password", default="123456")
    p.add_argument("--pg-db", default="food_traceability")
    a = p.parse_args()
    BASE = a.base
    return Checker(a).run()


if __name__ == "__main__":
    sys.exit(main())
