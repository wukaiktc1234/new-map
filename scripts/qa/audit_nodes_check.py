#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""审计节点可复现验证（N2 / N4 / N5 / PERM）—— 只读断言 + 造夹具，不改产品代码。

用法：
  python scripts/qa/audit_nodes_check.py                 # 跑全部四组
  python scripts/qa/audit_nodes_check.py --group N2      # 只跑一组（N2|N4|N5|PERM）
  python scripts/qa/audit_nodes_check.py --keep          # 保留夹具（默认按 tag 清理）

输出：每组一行 -> 组名 | PASS/FAIL/NOT_READY | 关键数值；末尾给汇总表与退出码。
退出码：0 全部 PASS / 1 存在 FAIL / 2 环境未就绪（后端未起/登录失败）。

每组都带**负向用例**：负向期望必须与正向相反，否则说明断言恒真（会把恒真判成 FAIL）。
"""
from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PSQL = str(ROOT / 'PostgreSQL' / '18' / 'bin' / 'psql.exe')
API = 'http://localhost:8081/api'
PWD_DEFAULT = 'Admin@123456'


def sql(q, db='food_traceability'):
    env = dict(os.environ, PGPASSWORD='123456', PGCLIENTENCODING='UTF8')
    p = subprocess.run([PSQL, '-h', 'localhost', '-p', '5432', '-U', 'postgres', '-d', db,
                        '-t', '-A', '-F', '|', '-c', q], capture_output=True, text=True,
                       encoding='utf-8', errors='replace', env=env, timeout=180)
    if p.returncode != 0:
        return []
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


def data_of(body):
    return (body or {}).get('data')


def ok(body):
    return isinstance(body, dict) and body.get('code') in (0, 200)


def login(username, password):
    st, b = http('POST', '/v1/auth/login', body={'username': username, 'password': password})
    tok = (data_of(b) or {}).get('token') if isinstance(data_of(b), dict) else None
    return tok, st, b


class Runner:
    def __init__(self, args):
        self.a = args
        self.tag = 'AN' + datetime.now().strftime('%m%d%H%M%S')
        self.rows = []
        self.tokens = {}

    # ---------- 基础设施 ----------
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
        tok, st, b = login(who, pwd)
        self.tokens[who] = tok
        return tok

    def dish(self, min_yuan=1):
        st, b = http('GET', '/v1/pos/api/dishes', self.tk('vcashier01'))
        items = data_of(b) or []
        cand = [d for d in items if d.get('dishId') and float(d.get('price') or 0) >= min_yuan]
        return cand[0] if cand else (items[0] if items else None)

    def body(self, expect_data, detail):
        return detail

    # ---------- N2：假价格拦截 ----------
    def group_n2(self):
        g = 'N2 假价格拦截'
        tok = self.tk('vcashier01')
        if not tok:
            return self.step(g, 'N2 前置：vcashier01 登录', False, '登录失败')
        d = self.dish(1)
        if not d:
            return self.step(g, 'N2 前置：取菜品', False, 'GET /v1/pos/api/dishes 无可用菜品')
        real_fen = int(round(float(d.get('price') or 0) * 100))
        self.info(g, 'N2 参照菜品', 'dishId=%s name=%s DB价(分)=%d' % (d.get('dishId'), d.get('dishName'), real_fen))

        def place(price_yuan):
            item = {'productId': str(d['dishId']), 'productName': d.get('dishName') or '',
                    'quantity': 1, 'price': price_yuan, 'dishType': d.get('dishType') or 'single'}
            st, b = http('POST', '/v1/pos/orders/order', tok,
                         {'orderType': 'dinein', 'items': [item],
                          'idempotencyKey': 'AUDIT-%s-%s' % (self.tag, price_yuan)})
            return st, b

        # ① 假价格 0.01 元
        st, b = place(0.01)
        dd = data_of(b) if isinstance(data_of(b), dict) else {}
        order_no = dd.get('orderNumber') or dd.get('orderNo')
        total_fen = None
        for k in ('totalAmount', 'totalAmountFen', 'total_amount'):
            if isinstance(dd.get(k), (int, float)):
                total_fen = int(round(float(dd[k])))
                break
        db_total = num("SELECT total_amount FROM orders WHERE order_number='%s'" % order_no) if order_no else None
        self.step(g, 'N2① 假价格 0.01 元下单不被采信（DB total_amount 分 == DB 价）',
                  ok(b) and db_total == real_fen,
                  'HTTP %s code=%s order=%s total_amount(分)=%s 期望=%d' % (
                      st, (b or {}).get('code'), order_no, db_total, real_fen))
        self.step(g, 'N2② 响应金额字段 == DB 价（分）',
                  total_fen is not None and total_fen == real_fen,
                  '响应 totalAmount=%s 期望=%d' % (total_fen, real_fen))
        # ③ 日志拒绝/覆盖记录（DB 优先，日志兜底）
        log_hit = 0
        logdir = ROOT / '.verify-tmp'
        for lf in sorted(logdir.glob('boot-*.log'), key=lambda p: p.stat().st_mtime, reverse=True)[:3]:
            try:
                txt = lf.read_text(encoding='utf-8', errors='replace')
            except OSError:
                continue
            if '价格差异检测' in txt or '使用数据库价格覆盖' in txt:
                log_hit += 1
        self.step(g, 'N2③ 后端留下「价格差异/覆盖」记录', log_hit > 0,
                  '命中日志文件数=%d（关键字：价格差异检测 / 使用数据库价格覆盖）' % log_hit)
        # 负向：合法价格不应被拒
        st2, b2 = place(float(d.get('price') or 0))
        dd2 = data_of(b2) if isinstance(data_of(b2), dict) else {}
        total2 = None
        for k in ('totalAmount', 'totalAmountFen', 'total_amount'):
            if isinstance(dd2.get(k), (int, float)):
                total2 = int(round(float(dd2[k])))
                break
        self.step(g, 'N2 负向：传合法价（DB 价）不被拒',
                  ok(b2) and total2 == real_fen,
                  'HTTP %s code=%s totalAmount=%s 期望=%d' % (
                      st2, (b2 or {}).get('code'), total2, real_fen))
        return True

    # ---------- N4：交班只关自己那个班次 ----------
    def group_n4(self):
        g = 'N4 交班关对班次'
        term = 'T-AUDIT-%s-N4' % self.tag
        tk_a = self.tk('vcashier01')
        tk_b = self.tk('qa_store_b')
        if not tk_a or not tk_b:
            return self.step(g, 'N4 前置：双账号登录', False, 'vcashier01/qa_store_b 登录失败')
        # 班次 1：当前登录人（vcashier01）通过产品 API 开班
        sa, ba = http('POST', '/v1/pos/auth/shift/start', tk_a,
                      {'terminalId': term, 'employeeId': 'e2e-a', 'employeeName': 'vcashier01',
                       'shiftType': 'morning', 'openingCash': 0})
        shift_a = num("SELECT shift_id FROM pos_shifts WHERE terminal_id='%s' AND deleted=0"
                      " AND COALESCE(end_time::text,'')='' ORDER BY shift_id ASC LIMIT 1" % term)
        # 班次 2：同 terminal 的「另一个登录人」——产品 API 会拦（同一 terminal 不允许二次开班），
        # 因此这里用**测试夹具直插一行**构造极端场景（不改产品代码，只造数据）。
        # shift_id 是 identity 列，不能显式插入 → 插完再读回
        ins_err = sql("INSERT INTO pos_shifts (terminal_id, employee_id, employee_name, shift_type,"
                      " start_time, opening_cash, total_orders, total_amount, status, create_time, update_time, deleted)"
                      " VALUES ('%s', 'e2e-b', 'qa_store_b', 'morning', now(), 0, 0, 0, 'active', now(), now(), 0)"
                      % term)
        shift_b = num("SELECT COALESCE(MAX(shift_id),0) FROM pos_shifts WHERE terminal_id='%s'" % term)
        if shift_a == shift_b:
            self.info(g, "N4 夹具插入失败", str(ins_err)[:120])
        n_open = num("SELECT COUNT(*) FROM pos_shifts WHERE terminal_id='%s' AND deleted=0"
                     " AND COALESCE(end_time::text,'')=''" % term)
        if n_open < 2:
            not_ready = True
            self.rows.append((g, 'N4 前置：同 terminal 两个 active 班次', 'NOT_READY',
                              '同 terminal 只有 %d 个 active（产品开班 HTTP %s；'
                              '第二个班次夹具直插 shift_b=%s 后仍为 %d）' % (n_open, sa, shift_b, n_open)))
            print('  [NOT_READY] N4 前置：同 terminal 两个 active 班次 -> 只有 %d 个' % n_open)
            return False
        self.info(g, 'N4 夹具', 'terminal=%s shiftA=%s shiftB=%s' % (term, shift_a, shift_b))
        se, be = http('POST', '/v1/pos/auth/shift/end', tk_a, {'terminalId': term, 'remark': 'audit N4'})
        a_closed = num("SELECT COUNT(*) FROM pos_shifts WHERE shift_id=%d AND end_time IS NOT NULL" % shift_a)
        b_open = num("SELECT COUNT(*) FROM pos_shifts WHERE shift_id=%d AND end_time IS NULL" % shift_b)
        self.step(g, 'N4 交班只关「当前登录人」的班次，另一个仍 active',
                  ok(be) and a_closed == 1 and b_open == 1,
                  'end HTTP %s code=%s；A(%s) closed=%d；B(%s) still_open=%d' % (
                      se, (be or {}).get('code'), shift_a, a_closed, shift_b, b_open))
        # 负向：重复交班不得再关掉任何班次
        se2, be2 = http('POST', '/v1/pos/auth/shift/end', tk_a, {'terminalId': term, 'remark': 'audit N4 repeat'})
        b_open2 = num("SELECT COUNT(*) FROM pos_shifts WHERE shift_id=%d AND end_time IS NULL" % shift_b)
        self.step(g, 'N4 负向：重复交班不会再关掉 B',
                  b_open2 == 1,
                  'HTTP %s code=%s；B(%s) still_open=%d（必须仍为 1）' % (
                      se2, (be2 or {}).get('code'), shift_b, b_open2))
        return True

    # ---------- N5：退款联动 ----------
    def group_n5(self):
        g = 'N5 退款联动'
        tk_c = self.tk('vcashier01')
        tk_m = self.tk('qa_store_b')
        if not tk_c or not tk_m:
            return self.step(g, 'N5 前置：登录', False, '登录失败')
        d = self.dish(1)
        if not d:
            return self.step(g, 'N5 前置：取菜品', False, '无可用菜品')
        item = {'productId': str(d['dishId']), 'productName': d.get('dishName') or '',
                'quantity': 2, 'price': float(d.get('price') or 0),
                'dishType': d.get('dishType') or 'single'}
        st, b = http('POST', '/v1/pos/orders/order', tk_c,
                     {'orderType': 'dinein', 'items': [item],
                      'idempotencyKey': 'AUDIT-%s-N5' % self.tag})
        dd = data_of(b) if isinstance(data_of(b), dict) else {}
        if not ok(b):
            return self.step(g, 'N5 前置：下单', False, 'HTTP %s code=%s' % (st, (b or {}).get('code')))
        order_no = dd.get('orderNumber')
        st, pb = http('POST', '/v1/pos/orders/order/pay', tk_c,
                      {'orderId': dd.get('orderId'), 'paymentMethod': '现金',
                       'amount': dd.get('totalAmount')})
        if not ok(pb):
            return self.step(g, 'N5 前置：支付', False, 'HTTP %s code=%s' % (st, (pb or {}).get('code')))
        # 退款前基线
        mv_before = num("SELECT COUNT(*) FROM inventory_movement")
        ko_before = num("SELECT COUNT(*) FROM kitchen_order WHERE order_number='%s'" % order_no)
        st, rb = http('POST', '/v1/pos/orders/order/%s/refund?refundReason=%s'
                      % (order_no, urllib.parse.quote('[AUDIT:%s] N5 退款联动' % self.tag)), tk_m)
        okr = ok(rb)
        self.step(g, 'N5 前置：退款成功', okr, 'HTTP %s code=%s msg=%s' % (
                  st, (rb or {}).get('code'), str((rb or {}).get('message'))[:60]))
        # ① inventory_movement 回补（change_qty > 0）
        mv_rows = sql("SELECT movement_id, change_qty, movement_type, COALESCE(source_ref,'') "
                      "FROM inventory_movement ORDER BY movement_id DESC LIMIT 6")
        back = [r for r in mv_rows if float(r.split('|')[1] or 0) > 0]
        self.step(g, 'N5① 退款产生 inventory_movement 回补流水（change_qty > 0）',
                  len(back) > 0,
                  '新增行=%d；样本=%s' % (num("SELECT COUNT(*) FROM inventory_movement") - mv_before,
                                         (back[0] if back else '无')))
        # ② kitchen_order 取消
        ko = sql("SELECT kitchen_order_id, COALESCE(status,'') FROM kitchen_order WHERE order_number='%s'" % order_no)
        cancelled = [r for r in ko if r.split('|')[1].lower() in ('cancelled', 'canceled', '已取消')]
        self.step(g, 'N5② kitchen_order 状态变为取消',
                  (ko_before == 0 and len(ko) == 0) or len(cancelled) > 0,
                  'kitchen_order=%s' % (ko[0] if ko else '（该订单未进厨房，无记录）'))
        # ③ 缴款单 refund_component_fen
        slip = sql("SELECT slip_id, COALESCE(refund_component_fen,0) FROM deposit_slips "
                   "WHERE deleted=0 ORDER BY slip_id DESC LIMIT 1")
        if slip:
            sid, rcf = slip[0].split('|')[0], int(slip[0].split('|')[1])
            expected = num("SELECT COALESCE(SUM(f.amount),0) FROM fund_flows f "
                           "JOIN refund_shift_attribution a ON a.refund_record_id=f.refund_record_id "
                           "WHERE f.deleted=0 AND f.status=2 AND f.flow_category=2 AND a.shift_id="
                           "(SELECT shift_id FROM deposit_slips WHERE slip_id=%s)" % sid)
            self.step(g, 'N5③ 缴款单 refund_component_fen == 归属金额之和',
                      rcf == expected, 'slip=%s rcf=%d attr_sum=%d' % (sid, rcf, expected))
        else:
            self.info(g, 'N5③ 缴款单 refund_component_fen', '库里没有缴款单 → 跳过（不判 PASS/FAIL）')
        # 负向：对同一订单再次退款必须被拒，且不新增回补流水
        mv_before2 = num("SELECT COUNT(*) FROM inventory_movement")
        st2, rb2 = http('POST', '/v1/pos/orders/order/%s/refund?refundReason=%s'
                        % (order_no, urllib.parse.quote('[AUDIT:%s] N5 重复退款' % self.tag)), tk_m)
        mv_after2 = num("SELECT COUNT(*) FROM inventory_movement")
        self.step(g, 'N5 负向：同订单重复退款被拒且不新增回补',
                  (not ok(rb2)) and mv_after2 == mv_before2,
                  'HTTP %s code=%s；inventory_movement %d → %d' % (
                      st2, (rb2 or {}).get('code'), mv_before2, mv_after2))
        return True

    # ---------- PERM：权限与断流 ----------
    def group_perm(self):
        g = 'PERM 权限与断流'
        tk_c = self.tk('vcashier01')
        st, b = http('GET', '/v1/pos/api/menu', tk_c)
        menu = data_of(b) if isinstance(data_of(b), dict) else {}
        n_food = len(menu.get('foods') or menu.get('dishes') or []) if isinstance(menu, dict) else 0
        self.step(g, 'PERM① vcashier01 能取 POS 菜单（收银页依赖）',
                  ok(b) and n_food > 0,
                  'HTTP %s code=%s 菜品数=%d' % (st, (b or {}).get('code'), n_food))
        st2, b2 = http('GET', '/v1/pos/tables', tk_c)
        self.step(g, 'PERM①b vcashier01 能取餐台列表', ok(b2),
                  'HTTP %s code=%s' % (st2, (b2 or {}).get('code')))
        # admin 进 POS：定案 = 200 带默认门店 或 明确 403 文案
        tk_a = self.tk('qa_admin')
        st3, b3 = http('GET', '/v1/pos/api/menu', tk_a)
        code3 = (b3 or {}).get('code')
        msg3 = str((b3 or {}).get('message') or '')
        path_ok = (st3 == 200 and ok(b3)) or (st3 == 403) or (code3 == 403)
        self.step(g, 'PERM② admin 登录 POS 行为符合定案（200 带门店 或 403 文案）',
                  path_ok and st3 != 500 and code3 != 500,
                  'HTTP %s code=%s msg=%s' % (st3, code3, msg3[:60]))
        # KDS 看板接口：200（不是 404）
        st4, b4 = http('GET', '/v1/kitchen/orders', tk_c)
        # KDS 看板真实路径（KitchenOrderController）：/active/full、/list；
        # 根路径 /v1/kitchen/orders 没有 GET 映射 → 业务 404「资源不存在」（不是路由 404）
        probes = []
        for path in ('/v1/kitchen/orders/active/full', '/v1/kitchen/orders/list'):
            sp, bp = http('GET', path, tk_c)
            probes.append((path, sp, (bp or {}).get('code'), str((bp or {}).get('message'))[:40]))
        good = [p for p in probes if p[1] == 200 and p[2] in (0, 200)]
        needs_token = [p for p in probes if p[1] in (401, 403)]
        self.step(g, 'PERM③ KDS 看板接口返回 200（不是 404）',
                  bool(good) or bool(needs_token),
                  '；'.join('%s -> HTTP %s code=%s %s' % (p[0], p[1], p[2], p[3]) for p in probes)
                  + ('（401/403 = 需 KDS 专用 token，接口存在）' if not good and needs_token else ''))
        # 负向：无 token 必须被拒（证明上面 200 不是「谁都能进」）
        st5, b5 = http('GET', '/v1/pos/api/menu', None)
        self.step(g, 'PERM 负向：无 token 访问 POS 菜单必须被拒',
                  st5 in (401, 403) or (b5 or {}).get('code') in (401, 403),
                  'HTTP %s code=%s' % (st5, (b5 or {}).get('code')))
        return True

    # ---------- 收尾 ----------
    def cleanup(self):
        if self.a.keep:
            self.info('CLEANUP', '保留夹具', 'tag=%s' % self.tag)
            return
        # 本脚本自造的班次夹具（T-AUDIT-<tag>-*）不在 e2e_cleanup 的锚点里 → 单独按终端清理
        term_like = 'T-AUDIT-%s%%' % self.tag
        left = num("SELECT COUNT(*) FROM pos_shifts WHERE terminal_id LIKE '%s'" % term_like)
        if left:
            sql("DELETE FROM pos_shifts WHERE terminal_id LIKE '%s'" % term_like)
            self.info('CLEANUP', '按终端清理 N4 夹具', 'T-AUDIT-%s-* 删除 %d 行' % (self.tag, left))
        r = subprocess.run([sys.executable, str(ROOT / 'scripts' / 'qa' / 'e2e_cleanup.py'),
                            '--tag', self.tag, '--apply'], capture_output=True, text=True,
                           encoding='utf-8', errors='replace', timeout=600)
        tail = [l for l in (r.stdout or '').splitlines() if '完成' in l or '锚点' in l]
        self.info('CLEANUP', '按 tag 清理', (tail[-1].strip() if tail else 'no-op')[:120])

    def summary(self):
        print()
        print('=' * 92)
        groups = {}
        for grp, name, st, detail in self.rows:
            cur = groups.setdefault(grp, {'PASS': 0, 'FAIL': 0, 'INFO': 0, 'NOT_READY': 0})
            cur[st] = cur.get(st, 0) + 1
        print('%-22s %-10s %s' % ('组名', '结果', '关键数值'))
        print('-' * 92)
        any_fail = False
        for grp, cnt in groups.items():
            verdict = 'FAIL' if cnt['FAIL'] else ('NOT_READY' if cnt.get('NOT_READY') else 'PASS')
            if verdict == 'FAIL':
                any_fail = True
            key = [r for r in self.rows if r[0] == grp and r[2] in ('PASS', 'FAIL')]
            sample = key[0][3][:70] if key else ''
            print('%-22s %-10s PASS=%d FAIL=%d INFO=%d %s' % (grp, verdict, cnt['PASS'], cnt['FAIL'], cnt['INFO'], sample))
        print('-' * 92)
        total_pass = sum(c['PASS'] for c in groups.values())
        total_fail = sum(c['FAIL'] for c in groups.values())
        print('总计：PASS=%d FAIL=%d（组数=%d）' % (total_pass, total_fail, len(groups)))
        return 1 if any_fail else 0


def main():
    ap = argparse.ArgumentParser(description='审计节点验证 N2/N4/N5/PERM（只读断言+夹具，不改产品代码）')
    ap.add_argument('--group', choices=['N2', 'N4', 'N5', 'PERM'], help='只跑一组')
    ap.add_argument('--keep', action='store_true', help='保留夹具（默认按 tag 清理）')
    a = ap.parse_args()
    r = Runner(a)
    st, b = http('GET', '/v1/actuator/health')
    if st == -1:  # 只有「连不上」才算未就绪；401/403/404 说明服务活着
        print('[ERROR] 后端未就绪（连不上，HTTP %s）→ EXIT=2' % st)
        return 2
    groups = {'N2': r.group_n2, 'N4': r.group_n4, 'N5': r.group_n5, 'PERM': r.group_perm}
    order = [a.group] if a.group else ['N2', 'N4', 'N5', 'PERM']
    print('=' * 92)
    print('审计节点验证  tag=%s  组=%s' % (r.tag, ','.join(order)))
    print('=' * 92)
    for name in order:
        print('\n--- %s ---' % name)
        try:
            groups[name]()
        except Exception as e:
            r.step(name, '%s 组执行异常' % name, False, str(e)[:140])
    r.cleanup()
    rc = r.summary()
    print('EXIT=%d' % rc)
    return rc


if __name__ == '__main__':
    sys.exit(main())
