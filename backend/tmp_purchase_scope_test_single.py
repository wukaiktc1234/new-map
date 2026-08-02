import requests
import json
import os
import sys
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

# 加载 token 缓存
tokens = {}
if os.path.exists(TOKEN_CACHE_FILE):
    try:
        with open(TOKEN_CACHE_FILE, 'r', encoding='utf-8') as f:
            tokens = json.load(f)
    except Exception:
        tokens = {}

# 命令行参数: username password desc
if len(sys.argv) < 4:
    print('Usage: python tmp_purchase_scope_test_single.py <username> <password> <desc>')
    sys.exit(1)

username = sys.argv[1]
password = sys.argv[2]
desc = sys.argv[3]

if username not in tokens:
    token = login(username, password)
    if token:
        tokens[username] = token
        with open(TOKEN_CACHE_FILE, 'w', encoding='utf-8') as f:
            json.dump(tokens, f)
    time.sleep(1)

print('=' * 100)
token = tokens.get(username)
if not token:
    print(f'账号: {username:12s} ({desc}) -> 无可用 token')
    sys.exit(0)

result = fetch_page(token)
if result is None:
    sys.exit(0)

records = result.get('records', [])
total = result.get('total', 0)
print(f'账号: {username:12s} ({desc})')
print(f'  看到总数: {total}, 本页条数: {len(records)}')
for rec in records[:20]:
    print(f'    {rec.get("requestNo")} | 部门:{rec.get("departmentId")}-{rec.get("departmentName")} | 申请人:{rec.get("applicantId")}-{rec.get("applicantName")} | 门店:{rec.get("storeId")} | 状态:{rec.get("status")}')
if len(records) > 20:
    print(f'    ... 还有 {len(records)-20} 条')
print('-' * 100)
