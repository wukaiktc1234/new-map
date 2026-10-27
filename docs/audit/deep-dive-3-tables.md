# 深查结果：inventory_code / scan_device / weighing_device（2026-10-28）

## 1. `inventory_code` —— 结论：**库存编码（溯源用），不是设备编码；两处写入都在库存业务**

| 写入位置 | 写入值 | 用途 |
|----------|--------|------|
| `HardwareDeviceServiceImpl.java:85` `inventoryCodeMapper.updateById(code)` | `status=1`（已使用） | **扫码枪扫库存码**后，把该编码标记为已用（`scanInventoryCode()`，:`44-93`）；同时扣减 1 个库存（`inventory.setQuantity(currentStock-1)`） |
| `SalesOrderServiceImpl.java:304` `inventoryCodeMapper.insert(inventoryCode)` | `inventoryId`(账行)、`codeType='CUSTOM'`、`uniqueCode`(明细里的码)、`qrCode='QR:xxx'`、`status=1`、`storeId`、`createdBy` | **销售出库**时按订单明细生成一条库存码，绑定到对应账行 |

判定依据（值域）：`InventoryCode` 的字段是 `inventoryId / uniqueCode / qrCode / status / storeId`，**全部指向库存账行与门店**，没有 `deviceId` 之类设备字段；
唯一与「设备」有关的是扫码枪**读取**它，写入内容的语义仍是库存。
→ 结论：**它是库存编码（一物一码/溯源码），不是设备编码**；「设备」只是它的读取方之一。

## 2. `scan_device` —— 结论：**只读配置表（设备注册），本服务无写入 → 写入方在别处**

```
ScanDeviceServiceImpl.java:39/41/47 注入 ScanDeviceMapper
ScanDeviceServiceImpl.java:62  ScanDevice device = getById(deviceId);   ← IService 读，无 insert/update
全仓 grep scanDeviceMapper.insert/update/delete = 0 处
```
→ 属「设备注册表」：本服务只读（连接/断开/状态查询），写入方未在本仓库（外部登记或手工作业）。
  若生产库无此表 → 扫码设备页 `POST /v1/scan/device/{id}/connect` 会在 `getById` 处炸（当前已被 501 兜住）。

## 3. `weighing_device` —— 结论：**只读配置表**，与 scan_device 同型

```
WeighingDeviceServiceImpl.java:30/31/35 注入 WeighingDeviceMapper
全仓 grep weighingDeviceMapper.insert/update/delete = 0 处（仅 getById/selectList 类读取）
WeighingDeviceServiceImpl.java:197 写的是 weighing_record（另一张表，已在 501 清单）
```
→ 只读配置表；写入方在本仓库之外。
