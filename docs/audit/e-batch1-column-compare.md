# E 第 1 批：实体字段 ↔ DDL 列 逐列对照（2026-10-28）

## 1. `contract_document`（16 列）

| 实体字段 | 实体列名 | DDL 列 | 一致 |
|---|---|---|---|
| `id` | `id` | `id` | ✔ |
| `contractId` | `contract_id` | `contract_id` | ✔ |
| `htmlContent` | `html_content` | `html_content` | ✔ |
| `templateId` | `template_id` | `template_id` | ✔ |
| `templateVersion` | `template_version` | `template_version` | ✔ |
| `documentHash` | `document_hash` | `document_hash` | ✔ |
| `version` | `version` | `version` | ✔ |
| `sourceType` | `source_type` | `source_type` | ✔ |
| `sourceDesc` | `source_desc` | `source_desc` | ✔ |
| `createdBy` | `created_by` | `created_by` | ✔ |
| `createTime` | `create_time` | `create_time` | ✔ |
| `updatedBy` | `updated_by` | `updated_by` | ✔ |
| `updateTime` | `update_time` | `update_time` | ✔ |
| `isCurrent` | `is_current` | `is_current` | ✔ |
| `editRemark` | `edit_remark` | `edit_remark` | ✔ |
| `deleted` | `deleted` | `deleted` | ✔ |

→ 16/16 一致（实体 16 个字段 = DDL 16 列，无多无少）

## 2. `file_attachment`（24 列）

实体字段：attachmentId, tenantId, businessType, businessId, originalName, storedName, filePath, fileExtension, contentType, fileSize, fileHashMd5, fileHashSha256, storageType, thumbnailPath, width, height, uploadUserId, uploadUsername, downloadCount, status, deleted, createdAt, updatedAt, deletedAt

DDL 列：attachment_id, tenant_id, business_type, business_id, original_name, stored_name, file_path, file_extension, content_type, file_size, file_hash_md5, file_hash_sha256, storage_type, thumbnail_path, width, height, upload_user_id, upload_username, download_count, status, deleted, create_time, update_time, deleted_at

→ **24/24 一致**。注意 `createdAt/updatedAt` 映射到 `create_time/update_time`（实体注解 `@TableField(value="create_time")` 显式指定），不是 `created_at`。

## 3. `finance_report`（10 列）

| 实体 | DDL |
|---|---|
| `report_id` *（或显式）* | `report_id` |
| `report_name` *（或显式）* | `report_name` |
| `report_type` *（或显式）* | `report_type` |
| `report_period` *（或显式）* | `report_period` |
| `status` *（或显式）* | `status` |
| `generate_by` *（或显式）* | `generate_by` |
| `generate_time` *（或显式）* | `generate_time` |
| `create_time` *（或显式）* | `create_time` |
| `update_time` *（或显式）* | `update_time` |
| `deleted` *（或显式）* | `deleted` |

→ 10/10 一致。**注意 `generate_time/create_time/update_time` 实体类型是 `Date`（不是 LocalDateTime）** —— 与新表 `timestamp` 兼容（JDBC 按 Timestamp 映射）。

## 4. `hardware_config`（18 列）

实体字段：id, deviceType, deviceName, deviceModel, connectionType, ipAddress, port, baudRate, portName, apiKey, configJson, status, remark, storeId, createdBy, updatedBy, createdAt, updatedAt

DDL 列：id, device_type, device_name, device_model, connection_type, ip_address, port, baud_rate, port_name, api_key, config_json, status, remark, store_id, created_by, updated_by, create_time, update_time

→ **18/18 一致**（`createdAt/updatedAt` → `create_time/update_time`，实体用 `@TableField` 显式指定过）。**`store_id` 已建索引**（外键列必索引）。

## 5. `hardware_config_version`（18 列）

实体字段：id, hardwareId, deviceType, deviceName, deviceModel, connectionType, ipAddress, port, baudRate, apiKey, remark, status, configJson, storeId, version, createdBy, createdAt, versionDesc

DDL 列：id, hardware_id, device_type, device_name, device_model, connection_type, ip_address, port, baud_rate, api_key, remark, status, config_json, store_id, version, created_by, create_time, version_desc

→ **18/18 一致**。`hardware_id` 建了索引 + `(hardware_id, version)` 唯一索引。

## 6. 自动审计复核

```text
entity_column_audit.py 全库合计：154 → 137（本批 5 张表新增后，相关不一致 1 条 = finance_report_detail，属「不建」名单）
→ 本批 5 张表：**0 条列不一致**
```
