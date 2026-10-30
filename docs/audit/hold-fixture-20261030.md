# 挂单联调夹具存档（2026-10-30，删除前备份）

来源：前端联调 + 探针写入，`pos_held_orders` 表。删除裁定：Lead 2026-10-30（含 2 行 COMPLETED，已证无下游副作用）。

## 无副作用证据

```
orders 20:30+ = 0 ｜ fund_flows 20:30+ = 0
order_payment_records 20:30+ = 0 ｜ exception_ops 20:30+ = 0
checkout 代码只执行 UPDATE pos_held_orders SET status='COMPLETED'
```

## 12 行全字段

```
6 | HOLD8e9b8102090d30718590b23c | 2080170081205395463 | T-HOLD-PROBE | 1 | HOLD | 1234 | 2026-10-30 23:59:59 | 2026-10-30 20:29:45.581111 | {"items": [{"id": "1", "name": "测试菜", "price": 12.34, "quantity": 1}]}
7 | HOLDa65143cfac5d1a38805afdad | 2080170081205395463 | pos-127.0.0.1-1793363611619 | NULL | HOLD | 1234 | 2026-10-30 23:59:59 | 2026-10-30 20:33:36.96886 | {"items": [{"id": "d1", "name": "挂单测试菜A", "price": 12.34, "dishType": "main", "quantity": 1}], "remark": "断言用A"}
8 | HOLD543a98f85515639a56bbb12b | 2080170081205395463 | pos-127.0.0.1-1793363611619 | NULL | HOLD | 1332 | 2026-10-30 23:59:59 | 2026-10-30 20:33:36.986718 | {"items": [{"id": "d2", "name": "挂单测试菜B", "price": 6.66, "dishType": "main", "quantity": 2}], "remark": "断言用B"}
9 | HOLDe870cce37eed5ed9b5ef0833 | 2080170081205395463 | pos-127.0.0.1-1793363742070 | NULL | HOLD | 1234 | 2026-10-30 23:59:59 | 2026-10-30 20:35:47.427062 | {"items": [{"id": "d1", "name": "挂单测试菜A", "price": 12.34, "dishType": "main", "quantity": 1}], "remark": "断言用A"}
10 | HOLDd9e7af77017ea92f09990251 | 2080170081205395463 | pos-127.0.0.1-1793363742070 | NULL | HOLD | 1332 | 2026-10-30 23:59:59 | 2026-10-30 20:35:47.441039 | {"items": [{"id": "d2", "name": "挂单测试菜B", "price": 6.66, "dishType": "main", "quantity": 2}], "remark": "断言用B"}
11 | HOLD26718b1b4f60bdc0770975c8 | 2080170081205395463 | pos-127.0.0.1-1793363939964 | NULL | COMPLETED | 1234 | 2026-10-30 23:59:59 | 2026-10-30 20:39:05.303479 | {"items": [{"id": "k1", "name": "验收菜A", "price": 12.34, "dishType": "main", "quantity": 1}], "remark": "验收A"}
12 | HOLD9f0aedbe203b8350bfbf3905 | 2080170081205395463 | pos-127.0.0.1-1793363939964 | NULL | CANCELLED | 1332 | 2026-10-30 23:59:59 | 2026-10-30 20:39:05.320658 | {"items": [{"id": "k2", "name": "验收菜B", "price": 6.66, "dishType": "main", "quantity": 2}], "remark": "验收B"}
13 | HOLD8f217aa8406cb061beb71a94 | 2080170081205395463 | pos-127.0.0.1-1793364120917 | NULL | HOLD | 1234 | 2026-10-30 23:59:59 | 2026-10-30 20:42:06.267106 | {"items": [{"id": "v1", "name": "验收菜A", "price": 12.34, "dishType": "main", "quantity": 1}], "remark": "验收A"}
14 | HOLD4e4a5e0eae57bb85bf841059 | 2080170081205395463 | pos-127.0.0.1-1793364120917 | NULL | COMPLETED | 1332 | 2026-10-30 23:59:59 | 2026-10-30 20:42:06.28308 | {"items": [{"id": "v2", "name": "验收菜B", "price": 6.66, "dishType": "main", "quantity": 2}], "remark": "验收B"}
15 | HOLDbeeda2b22a9945cb87209490 | 2080170081205395463 | pos-127.0.0.1-1793364264910 | NULL | HOLD | 990 | 2026-10-30 23:59:59 | 2026-10-30 20:44:30.326862 | {"items": [{"id": "off1", "name": "离线菜", "price": 9.9, "dishType": "main", "quantity": 1}], "remark": "离线挂单"}
16 | HOLDfc38882f8ef8cf840d7b8138 | 2080170081205395463 | pos-127.0.0.1-2eb6c42a37814ae1 | NULL | HOLD | 1234 | 2026-10-30 23:59:59 | 2026-10-30 20:49:24.718816 | {"items": [{"id": "p1", "name": "崩溃前菜A", "price": 12.34, "dishType": "main", "quantity": 1}], "remark": "崩溃前A"}
17 | HOLD940caf90e819dcebfc44139c | 2080170081205395463 | pos-127.0.0.1-2eb6c42a37814ae1 | NULL | HOLD | 1332 | 2026-10-30 23:59:59 | 2026-10-30 20:49:24.734435 | {"items": [{"id": "p2", "name": "崩溃前菜B", "price": 6.66, "dishType": "main", "quantity": 2}], "remark": "崩溃前B"}
```
