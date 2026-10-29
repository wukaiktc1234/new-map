#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""安全回归 S1~S6（R1-R5 攻击用例 + 正常对照）—— 发布门禁用。

一条命令跑完：  python scripts/qa/security_regression.py

设计原则（三条，缺一条就会退化成「恒真断言」）：
  1) **每组都有正常对照**：攻击用例为红时，对照必须为绿，否则说明「本来就坏」，不能算防护生效；
  2) **开跑前自检**（[SELF-CHECK]）：我要用的表必须存在、关联键必须能查到样例 —— 读错表会造出假缺陷；
  3) **夹具按 tag 清理**，并打印清理后残留数（必须 0）。

用例：
  S1 价格篡改     price=0.01 → code=0 且 DB total_amount=DB价（非 1 分）+ price_override 打点；对照：合法价
  S2 售罄强攻     status=2 菜品 → code=400 且 orders 行数不变；对照：status=1 菜品 → code=0
  S3 伪造开班     vcashier01 + body.employeeId=admin → 403 且 pos_shifts 无新增；对照：用自己的 employeeId → 200
  S4 跨人交班     同 terminal 两 active 班次 → 只关自己那个；对照：单班次交班 → completed
  S5 退款越权     vcashier01 退款 → 403；且权限含 refund:view、不含 order:refund/order:refund:initiate/refund:manage；
                  对照：店长调同一接口 → 非 403
  S6 响应价回显   篡改价下单 → response.orderItems[0].price == DB 价（不是 0.01）

退出码：0 全 PASS / 1 存在 FAIL / 2 自检未通过或后端连不上（未做断言）。
"""
from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PSQL = str(ROOT / 'PostgreSQL' / '18' / 'bin' / 'psql.exe')
API = 'http://localhost:8081/api'
PWD_DEFAULT = 'Admin@123456'

TABLES_REQUIRED = ['orders', 'order_items', 'foods', 'pos_shifts', 'fund_flows',
                   'refund_shift_attribution', 'deposit_slips', 'inventory_movement', 'kitchen_order']


def sql(q, db='food_traceability'):
    env = dict(os.environ, PGPASSWORD='123456', PGCLIENTENCODING='UTF8')
    p = subprocess.run([PSQL, '-h', 'localhost', '-p', '5432', '-U', 'postgres', '-d', db,
                        '-t', '-A', '-F', '|', '-c', q], capture_output=True, text=True,
                       encoding='utf-8', errors='replace', env=env, timeout=180)
    if p.returncode != 0:
        return [(p.stderr or '').strip()[:200]]
    return [ln for ln in (p.stdout or '').splitlines() if ln.strip()]


def num(q):
    r = sql(q)
    if not r:
        return 0
    try:
        return int(float(r[0].split('|')[0]))
    except ValueError:
        return 0


def http(method, path, token=None, body=None, timeout=90):
    data = json.dumps(body).encode('utf-8') if body is not None else None
    headers = {'Content-Type': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    req = urllib.request.Request(API + path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return resp.status, json.loads(resp.read().decode('utf-8') or '{}')
    except urllib.error.HTTPError as e:
        try:
            return e.code, json.loads(e.read().decode('utf-8') or '{}')
        except Exception:
            return e.code, {}
    except Exception as e:
        return -1, {'error': str(e)}


def data_of(b):
    return (b or {}).get('data')


def ok(b):
    return isinstance(b, dict) and b.get('code') in (0, 200)


def preflight_selfcheck():
    """开跑前证明「我看的是对的数据」：表存在 + 关联键有样例。"""
    problems = []
    for t in TABLES_REQUIRED:
        r = sql("SELECT to_regclass('public.%s') IS NOT NULL" % t)
        if not r or r[0].strip() != 't':
            problems.append('表不存在：%s' % t)
    sample = sql("SELECT o.order_number || ' / unit_price=' || i.unit_price"
                 " FROM orders o JOIN order_items i ON i.order_id = o.order_id LIMIT 1")
    if not sample:
        problems.append('关联键无样例：orders.order_id ↔ order_items.order_id')
    else:
        print('[SELF-CHECK] 关联键样例：%s' % sample[0][:90])
    dishes = num("SELECT COUNT(*) FROM foods WHERE deleted=0 AND status IN (1,2)")
    if dishes < 2:
        problems.append('可售/售罄菜品不足（需要至少各 1 个，当前 %d）' % dishes)
    print('[SELF-CHECK] 有明细订单=%d；菜品(可售/售罄)=%d/%d；问题=%d' % (
        num("SELECT COUNT(*) FROM orders o JOIN order_items i ON i.order_id=o.order_id WHERE o.deleted=0"),
        num("SELECT COUNT(*) FROM foods WHERE deleted=0 AND status=1"),
        num("SELECT COUNT(*) FROM foods WHERE deleted=0 AND status=2"),
        len(problems)))
    if problems:
        return False, '；'.join(problems[:4])
    return True, 'ok'


class Sec:
    def __init__(self, args):
        self.a = args
        self.tag = 'SEC' + datetime.now().strftime('%m%d%H%M%S')
        self.rows = []
        self.tokens = {}
        self.created_orders = []

    # ---------- 基础 ----------
    def step(self, group, name, passed, detail):
        self.rows.append((group, name, 'PASS' if passed else 'FAIL', detail))
        print('  [%s] %s -> %s' % ('PASS' if passed else 'FAIL', name, detail))
        return passed

    def info(self, group, name, detail):
        self.rows.append((group, name, 'INFO', detail))
        print('  [INFO] %s -> %s' % (name, detail))

    def tk(self, who):
        if who in self.tokens:
            return self.tokens[who]
        pwd = {'vcashier01': '123456'}.get(who, PWD_DEFAULT)
        st, b = http('POST', '/v1/auth/login', body={'username': who, 'password': pwd})
        d = data_of(b) or {}
        self.tokens[who] = d.get('token')
        self.tokens[who + ':perms'] = ((d.get('userInfo') or {}).get('permissions') or [])
        return self.tokens[who]

    def perms(self, who):
        self.tk(who)
        return self.tokens.get(who + ':perms') or []

    def dish(self, status=1):
        code = sql("SELECT food_code FROM foods WHERE deleted=0 AND status=%d"
                   " AND sale_price > 0 ORDER BY food_id LIMIT 1" % status)
        if not code:
            return None
        code = code[0].split('|')[0]
        price_fen = num("SELECT sale_price FROM foods WHERE food_code='%s'" % code)
        _, db = http('GET', '/v1/pos/api/dishes', self.tk('vcashier01'))
        items = data_of(db) or []
        return {'code': code, 'priceFen': price_fen, 'inMenu': any(str(x.get('dishId')) == code for x in items)}

    def place(self, dish, price_yuan, remark_tag='S'):
        item = {'productId': str(dish['code']), 'productName': 'sec-probe',
                'quantity': 1, 'price': price_yuan, 'dishType': 'single'}
        st, b = http('POST', '/v1/pos/orders/order', self.tk('vcashier01'),
                     {'orderType': 'dinein', 'items': [item],
                      'remark': '[SEC:%s] %s price=%s' % (self.tag, remark_tag, price_yuan),
                      'idempotencyKey': 'SEC-%s-%s-%s' % (self.tag, remark_tag, price_yuan)})
        dd = data_of(b) if isinstance(data_of(b), dict) else {}
        no = dd.get('orderNumber') or dd.get('orderNo')
        if no:
            self.created_orders.append(str(no))
        return st, b, dd, no

    # ---------- S1 价格篡改 ----------
    def s1(self):
        g = 'S1 价格篡改'
        d = self.dish(1)
        if not d:
            return self.step(g, 'S1 前置：取可售菜品', False, '没有 status=1 的菜品')
        before = num('SELECT COUNT(*) FROM orders')
        st, b, dd, no = self.place(d, 0.01, 'S1-hack')
        db_total = num("SELECT total_amount FROM orders WHERE order_number='%s'" % no) if no else -1
        self.step(g, 'S1 price=0.01 不被采信（DB total_amount == DB 价，不是 1 分）',
                  ok(b) and db_total == d['priceFen'],
                  'code=%s order=%s DB total=%s 期望 %d 分（本单 0.01 元=1 分，若采信则为 1）' % (
                      (b or {}).get('code'), no, db_total, d['priceFen']))
        hit = 0
        for lf in sorted((ROOT / '.verify-tmp').glob('boot-*.log'), key=lambda p: p.stat().st_mtime,
                         reverse=True)[:3] + sorted(ROOT.glob('food-traceability.*.log'),
                                                    key=lambda p: p.stat().st_mtime, reverse=True)[:2]:
            try:
                if any(k in lf.read_text(encoding='utf-8', errors='replace')
                       for k in ('price_override', '价格差异检测')):
                    hit += 1
            except OSError:
                pass
        self.step(g, 'S1 后端留 price_override 打点', hit > 0, '命中日志文件数=%d' % hit)
        # 对照
        st2, b2, dd2, no2 = self.place(d, d['priceFen'] / 100.0, 'S1-legal')
        db2 = num("SELECT total_amount FROM orders WHERE order_number='%s'" % no2) if no2 else -1
        self.step(g, 'S1 对照：合法价下单 → code=0 且 DB 金额一致',
                  ok(b2) and db2 == d['priceFen'],
                  'code=%s order=%s DB total=%s 期望 %d' % ((b2 or {}).get('code'), no2, db2, d['priceFen']))
        self.info(g, 'S1 orders 行数', '%d → %d' % (before, num('SELECT COUNT(*) FROM orders')))
        return True

    # ---------- S2 售罄强攻 ----------
    def s2(self):
        g = 'S2 售罄强攻'
        sold_out = self.dish(2)
        if not sold_out:
            return self.step(g, 'S2 前置：取售罄菜品', False, '没有 status=2 的菜品')
        before = num('SELECT COUNT(*) FROM orders')
        st, b, dd, no = self.place(sold_out, sold_out['priceFen'] / 100.0, 'S2-soldout')
        after = num('SELECT COUNT(*) FROM orders')
        code = (b or {}).get('code')
        self.step(g, 'S2 售罄菜品下单 → 400 且 orders 行数不变',
                  code == 400 and after == before,
                  '菜品=%s(在售罄清单=%s) code=%s msg=%s；orders %d → %d' % (
                      sold_out['code'], sold_out['inMenu'], code,
                      str((b or {}).get('message'))[:50], before, after))
        d = self.dish(1)
        before2 = num('SELECT COUNT(*) FROM orders')
        st2, b2, dd2, no2 = self.place(d, d['priceFen'] / 100.0, 'S2-onsale')
        after2 = num('SELECT COUNT(*) FROM orders')
        self.step(g, 'S2 对照：可售菜品下单 → code=0 且新增 1 单',
                  ok(b2) and after2 == before2 + 1,
                  'code=%s；orders %d → %d' % ((b2 or {}).get('code'), before2, after2))
        return True

    # ---------- S3 伪造开班 ----------
    def s3(self):
        g = 'S3 伪造开班'
        term = 'T-SEC-%s-S3' % self.tag
        before = num('SELECT COUNT(*) FROM pos_shifts')
        st, b = http('POST', '/v1/pos/auth/shift/start', self.tk('vcashier01'),
                     {'terminalId': term + '-IMP', 'employeeId': 'admin',
                      'employeeName': 'admin', 'shiftType': 'morning', 'openingCash': 0})
        after = num('SELECT COUNT(*) FROM pos_shifts')
        code = (b or {}).get('code')
        self.step(g, 'S3 伪造 employeeId=admin 开班 → 403 且 pos_shifts 无新增',
                  code == 403 and after == before,
                  'HTTP %s code=%s msg=%s；pos_shifts %d → %d' % (
                      st, code, str((b or {}).get('message'))[:40], before, after))
        st2, b2 = http('POST', '/v1/pos/auth/shift/start', self.tk('vcashier01'),
                       {'terminalId': term + '-SELF', 'employeeId': 'vcashier01',
                        'employeeName': 'vcashier01', 'shiftType': 'morning', 'openingCash': 0})
        self.step(g, 'S3 对照：用自己的 employeeId 开班 → 200/code=0',
                  ok(b2), 'HTTP %s code=%s msg=%s' % (st2, (b2 or {}).get('code'),
                                                      str((b2 or {}).get('message'))[:40]))
        return True

    # ---------- S4 跨人交班 ----------
    def s4(self):
        g = 'S4 跨人交班'
        self.tk('qa_store_b')
        term = 'T-SEC-%s-S4' % self.tag
        st, b = http('POST', '/v1/pos/auth/shift/start', self.tk('vcashier01'),
                     {'terminalId': term, 'employeeId': 'vcashier01', 'employeeName': 'vcashier01',
                      'shiftType': 'morning', 'openingCash': 0})
        if not ok(b):
            return self.step(g, 'S4 前置：开班', False, 'code=%s' % (b or {}).get('code'))
        mine = num("SELECT shift_id FROM pos_shifts WHERE terminal_id='%s' AND deleted=0"
                   " AND COALESCE(end_time::text,'')='' ORDER BY shift_id ASC LIMIT 1" % term)
        # 同 terminal 第二行：产品 API 会拦（同 terminal 不允许二次开班）→ 用夹具直插（造数据，不改产品）
        sql("INSERT INTO pos_shifts (terminal_id, employee_id, employee_name, shift_type, start_time,"
            " opening_cash, total_orders, total_amount, status, create_time, update_time, deleted)"
            " VALUES ('%s', 'other', 'other-user', 'morning', now(), 0, 0, 0, 'active', now(), now(), 0)" % term)
        other = num("SELECT shift_id FROM pos_shifts WHERE terminal_id='%s'"
                    " AND COALESCE(end_time::text,'')='' ORDER BY shift_id DESC LIMIT 1" % term)
        self.info(g, 'S4 夹具', 'terminal=%s mine=%s other=%s' % (term, mine, other))
        se, be = http('POST', '/v1/pos/auth/shift/end', self.tk('vcashier01'),
                      {'terminalId': term, 'remark': 'SEC S4'})
        mine_closed = num("SELECT COUNT(*) FROM pos_shifts WHERE shift_id=%d AND end_time IS NOT NULL" % mine)
        other_open = num("SELECT COUNT(*) FROM pos_shifts WHERE shift_id=%d AND end_time IS NULL" % other)
        self.step(g, 'S4 同 terminal 两 active 班次 → 只关自己那个，另一个仍 active',
                  ok(be) and mine_closed == 1 and other_open == 1,
                  'end code=%s；mine(%s) closed=%d；other(%s) still_open=%d' % (
                      (be or {}).get('code'), mine, mine_closed, other, other_open))
        # 对照：单班次交班
        term2 = 'T-SEC-%s-S4B' % self.tag
        st2, b2 = http('POST', '/v1/pos/auth/shift/start', self.tk('vcashier01'),
                       {'terminalId': term2, 'employeeId': 'vcashier01', 'employeeName': 'vcashier01',
                        'shiftType': 'morning', 'openingCash': 0})
        se2, be2 = http('POST', '/v1/pos/auth/shift/end', self.tk('vcashier01'),
                        {'terminalId': term2, 'remark': 'SEC S4 control'})
        status2 = sql("SELECT status FROM pos_shifts WHERE terminal_id='%s' ORDER BY shift_id DESC LIMIT 1" % term2)
        self.step(g, 'S4 对照：单班次交班 → completed',
                  ok(b2) and ok(be2) and status2 and status2[0].split('|')[0] in ('completed', 'CLOSED', 'closed'),
                  'start code=%s；end code=%s；status=%s' % ((b2 or {}).get('code'), (be2 or {}).get('code'),
                                                            status2[0] if status2 else '?'))
        return True

    # ---------- S5 退款越权 ----------
    def s5(self):
        g = 'S5 退款越权'
        perms = self.perms('vcashier01')
        need_absent = ['order:refund', 'order:refund:initiate', 'refund:manage']
        has_view = 'refund:view' in perms
        leaked = [p for p in need_absent if p in perms]
        self.step(g, 'S5 vcashier01 权限集：含 refund:view、不含退款写权限',
                  has_view and not leaked,
                  '权限数=%d；refund:view=%s；越权码=%s' % (len(perms), has_view, leaked or '无'))
        d = self.dish(1)
        st, b, dd, no = self.place(d, d['priceFen'] / 100.0, 'S5-order')
        if not ok(b):
            return self.step(g, 'S5 前置：造一笔已支付订单', False, 'code=%s' % (b or {}).get('code'))
        http('POST', '/v1/pos/orders/order/pay', self.tk('vcashier01'),
             {'orderId': dd.get('orderId'), 'paymentMethod': '现金', 'amount': dd.get('totalAmount')})
        st2, b2 = http('POST', '/v1/pos/orders/order/%s/refund?refundReason=%s'
                       % (no, urllib.parse.quote('[SEC:%s] S5 越权尝试' % self.tag)),
                       self.tk('vcashier01'))
        code2 = (b2 or {}).get('code')
        self.step(g, 'S5 vcashier01 调退款 → 403',
                  st2 == 403 or code2 == 403,
                  'HTTP %s code=%s msg=%s' % (st2, code2, str((b2 or {}).get('message'))[:50]))
        # 对照：店长不是 403
        st3, b3 = http('POST', '/v1/pos/orders/order/%s/refund?refundReason=%s'
                       % (no, urllib.parse.quote('[SEC:%s] S5 店长对照' % self.tag)),
                       self.tk('qa_store_b'))
        self.step(g, 'S5 对照：店长调同一接口 → 非 403（业务失败可以，权限 403 不行）',
                  st3 != 403 and (b3 or {}).get('code') != 403,
                  'HTTP %s code=%s msg=%s' % (st3, (b3 or {}).get('code'), str((b3 or {}).get('message'))[:50]))
        return True

    # ---------- S6 响应价回显 ----------
    def s6(self):
        g = 'S6 响应价回显'
        d = self.dish(1)
        st, b, dd, no = self.place(d, 0.01, 'S6-echo')
        items = (dd.get('orderItems') or dd.get('items') or [])
        resp_price = None
        if items and isinstance(items[0], dict):
            resp_price = items[0].get('price')
        db_unit = num("SELECT COALESCE(MAX(i.unit_price),0) FROM order_items i"
                      " JOIN orders o ON o.order_id=i.order_id WHERE o.order_number='%s'" % no) if no else -1
        resp_fen = None if resp_price is None else round(float(resp_price) * 100)
        self.step(g, 'S6 篡改价下单 → 响应明细价 == DB 价（不是 0.01）',
                  resp_fen is not None and int(resp_fen) == db_unit,
                  '响应 orderItems[0].price=%s 元 → %s 分；DB unit_price=%s 分（期望相等，若回显前端价会是 1 分）' % (
                      resp_price, resp_fen, db_unit))
        # 对照：合法价下单时，响应价与 DB 价必然一致
        st2, b2, dd2, no2 = self.place(d, d['priceFen'] / 100.0, 'S6-legal')
        items2 = (dd2.get('orderItems') or dd2.get('items') or [])
        rp2 = items2[0].get('price') if items2 and isinstance(items2[0], dict) else None
        db2 = num("SELECT COALESCE(MAX(i.unit_price),0) FROM order_items i"
                  " JOIN orders o ON o.order_id=i.order_id WHERE o.order_number='%s'" % no2) if no2 else -1
        self.step(g, 'S6 对照：合法价下单 → 响应价 == DB 价',
                  rp2 is not None and int(round(float(rp2) * 100)) == db2,
                  '响应=%s 元 → %s 分；DB=%s 分' % (rp2, None if rp2 is None else round(float(rp2) * 100), db2))
        return True

    # ---------- 清理 ----------
    def cleanup(self):
        if self.a.keep:
            self.info('CLEANUP', '保留夹具', 'tag=%s' % self.tag)
            return
        # 订单夹具（先删明细再删主表，避免 FK 挡住）
        if self.created_orders:
            lst = ','.join("'" + o.replace("'", "''") + "'" for o in self.created_orders)
            sql("DELETE FROM order_items WHERE order_id IN (SELECT order_id FROM orders WHERE order_number IN (%s))" % lst)
            sql("DELETE FROM kitchen_order WHERE order_number IN (%s)" % lst)
            sql("DELETE FROM fund_flows WHERE order_id IN (SELECT order_id FROM orders WHERE order_number IN (%s))" % lst)
            sql("DELETE FROM orders WHERE order_number IN (%s)" % lst)
        # 兜底：按 remark tag
        sql("DELETE FROM order_items WHERE order_id IN (SELECT order_id FROM orders"
            " WHERE remark LIKE '%%[SEC:%s]%%')" % self.tag)
        sql("DELETE FROM kitchen_order WHERE order_number IN (SELECT order_number FROM orders"
            " WHERE remark LIKE '%%[SEC:%s]%%')" % self.tag)
        sql("DELETE FROM orders WHERE remark LIKE '%%[SEC:%s]%%'" % self.tag)
        # 班次夹具
        sql("DELETE FROM pos_shifts WHERE terminal_id LIKE 'T-SEC-%s%%'" % self.tag)
        left_orders = num("SELECT COUNT(*) FROM orders WHERE remark LIKE '%%[SEC:%s]%%'" % self.tag)
        left_shifts = num("SELECT COUNT(*) FROM pos_shifts WHERE terminal_id LIKE 'T-SEC-%s%%'" % self.tag)
        self.info('CLEANUP', '夹具清理', 'orders_left=%d；shifts_left=%d（tag=%s）' % (
            left_orders, left_shifts, self.tag))

    # ---------- 汇总 ----------
    def summary(self):
        print()
        print('=' * 96)
        groups = {}
        for grp, name, st, detail in self.rows:
            cur = groups.setdefault(grp, {'PASS': 0, 'FAIL': 0, 'INFO': 0})
            cur[st] = cur.get(st, 0) + 1
        print('%-18s %-8s %s' % ('用例', '结果', '关键数值'))
        print('-' * 96)
        any_fail = False
        for grp, cnt in groups.items():
            verdict = 'FAIL' if cnt['FAIL'] else 'PASS'
            if cnt['FAIL']:
                any_fail = True
            first = next((r for r in self.rows if r[0] == grp and r[2] in ('PASS', 'FAIL')), None)
            print('%-18s %-8s PASS=%d FAIL=%d %s' % (grp, verdict, cnt['PASS'], cnt['FAIL'],
                                                      (first[3][:58] if first else '')))
        print('-' * 96)
        print('总计：PASS=%d FAIL=%d' % (sum(c['PASS'] for c in groups.values()),
                                        sum(c['FAIL'] for c in groups.values())))
        return 1 if any_fail else 0


def main():
    ap = argparse.ArgumentParser(description='安全回归 S1~S6（发布门禁，EXIT=0 才允许发版）')
    ap.add_argument('--only', choices=['S1', 'S2', 'S3', 'S4', 'S5', 'S6'], help='只跑一组')
    ap.add_argument('--keep', action='store_true', help='保留夹具（默认按 tag 清理）')
    a = ap.parse_args()

    okc, why = preflight_selfcheck()
    if not okc:
        print('[NOT_READY] 自检未通过：%s → EXIT=2（不做断言，避免读出假缺陷）' % why)
        return 2
    st, _ = http('GET', '/v1/actuator/health')
    if st == -1:
        print('[ERROR] 后端连不上（HTTP -1）→ EXIT=2')
        return 2

    r = Sec(a)
    order = [a.only] if a.only else ['S1', 'S2', 'S3', 'S4', 'S5', 'S6']
    print('=' * 96)
    print('安全回归  tag=%s  用例=%s' % (r.tag, ','.join(order)))
    print('=' * 96)
    fn = {'S1': r.s1, 'S2': r.s2, 'S3': r.s3, 'S4': r.s4, 'S5': r.s5, 'S6': r.s6}
    for name in order:
        print('\n--- %s ---' % name)
        try:
            fn[name]()
        except Exception as e:
            r.step(name, '%s 执行异常' % name, False, str(e)[:140])
    r.cleanup()
    rc = r.summary()
    print('EXIT=%d' % rc)
    return rc


if __name__ == '__main__':
    sys.exit(main())
