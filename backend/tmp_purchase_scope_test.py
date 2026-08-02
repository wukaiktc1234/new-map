import requests
import json
import os
import time

BASE = 'http://localhost:8081/api'
LOGIN_URL = f'{BASE}/v1/auth/login'
PAGE_URL = f'{BASE}/v1/purchase/requests/page'
TOKEN_CACHE_FILE = 'tmp_purchase_tokens.json'

def login(username, password):
    r = requests.post(LOGIN_URL, json={'username': username, 'password': password}, timeout=10)
    data = r.json()
    if data.get('code') != 0:
        print(f'登录失败 {username}: code={data.get("code")}, msg={data.get("message")}')
        return None
    return data['data']['token']

def fetch_page(token, page=1, size=100):
    r = requests.get(PAGE_URL, params={'page': page, 'size': size}, headers={'Authorization': f'Bearer {token}'}, timeout=10)
    data = r.json()
    if data.get('code') != 0:
        print(f'查询失败: code={data.get("code")}, msg={data.get("message")}')
        return None
    return data['data']

accounts = [
    ('admin', 'Admin@123', '超级管理员 (all)'),
    ('emp-f', 'Test@123456', '采购部经理 purchase_manager (all, 采购部)'),
    ('emp-h', 'Test@123456', '部门经理 department_manager (department, 运营部)'),
    ('emp-a', 'Test@123456', '部门经理 department_manager (department, 人事部)'),
    ('emp-d', 'Test@123456', '普通员工 employee (self, 财务部)'),
    ('emp-i', 'Test@123456', '普通员工 employee (self, 运营部)'),
    ('emp-j', 'Test@123456', '店长 store_manager (department, 运营部/门店A)'),
    ('emp-k', 'Test@123456', '普通员工 employee (self, 运营部/门店A)'),
    ('emp-m', 'Test@123456', '店长 store_manager (department, 运营部/门店B)'),
]

# 加载 token 缓存
tokens = {}
if os.path.exists(TOKEN_CACHE_FILE):
    try:
        with open(TOKEN_CACHE_FILE, 'r', encoding='utf-8') as f:
            tokens = json.load(f)
    except Exception:
        tokens = {}

# 尝试登录缺失或失败的账号（带限流保护）
for username, pwd, desc in accounts:
    if username not in tokens:
        token = login(username, pwd)
        if token:
            tokens[username] = token
        time.sleep(2)

# 保存 token 缓存
with open(TOKEN_CACHE_FILE, 'w', encoding='utf-8') as f:
    json.dump(tokens, f)

print('=' * 100)
for username, pwd, desc in accounts:
    token = tokens.get(username)
    if not token:
        print(f'账号: {username:12s} ({desc}) -> 无可用 token')
        continue
    result = fetch_page(token)
    if result is None:
        continue
    records = result.get('records', [])
    total = result.get('total', 0)
    print(f'账号: {username:12s} ({desc})')
    print(f'  看到总数: {total}, 本页条数: {len(records)}')
    for rec in records[:10]:
        print(f'    {rec.get("requestNo")} | 部门:{rec.get("departmentId")}-{rec.get("departmentName")} | 申请人:{rec.get("applicantId")}-{rec.get("applicantName")} | 门店:{rec.get("storeId")} | 状态:{rec.get("status")}')
    if len(records) > 10:
        print(f'    ... 还有 {len(records)-10} 条')
    print('-' * 100)
