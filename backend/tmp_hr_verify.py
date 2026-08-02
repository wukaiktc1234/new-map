import requests

BASE = 'http://localhost:8081/api'

def login(username, password):
    r = requests.post(f'{BASE}/v1/auth/login', json={'username': username, 'password': password}, timeout=10)
    return r.json()['data']['token']

token = login('admin', 'Admin@123')
headers = {'Authorization': f'Bearer {token}'}

endpoints = [
    ('GET', '/v1/departments/tree'),
    ('GET', '/v1/employees/page'),
    ('GET', '/v1/positions/page'),
    ('GET', '/v1/recruitment-requirements/page'),
    ('GET', '/v1/recruitment-requirements/statistics'),
    ('GET', '/v1/onboarding/archive/list'),
]

for method, path in endpoints:
    r = requests.request(method, f'{BASE}{path}', params={'size': 10}, headers=headers, timeout=10)
    data = r.json()
    status = 'OK' if data.get('code') == 0 else f"FAIL({data.get('code')})"
    records = data.get('data', {})
    total = records.get('total', '-') if isinstance(records, dict) else '-'
    print(f'{path}: {status}, total={total}')
