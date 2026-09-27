# Owner 确认单（2026-09-27 · 一页 · 三问）

> 背景：P1-LOCATION-MODEL-001 M1-M2（commit `a956cd4`，已推送）与 P1-STOMP-RECONNECT-001（commit `c975a8f`）已完成并暂停。
> 测试厨单已清理（C01/C02/C03 恢复 pending，厨单分布与测试前一致：pending 39）。
> ⚠️ `c975a8f` 因本机无法连通 github.com:443，**两次推送均失败，尚未上远端**（远端 HEAD = `a956cd4`）；网络恢复后执行 `git push origin master` 即可，无合并风险（远端无新提交）。

---

## Q1：STOMP 重连策略

修复后重连 = **无上限、固定间隔**（POS/KDS 3s，CallNumber/CallingDisplay 5s）。
原逻辑是"stompjs 内部重连 + 自定义 10 次上限（到顶永久放弃）"，已整体移除。

- **选项 A（推荐）**：接受固定间隔无上限。收银/后厨终端常开，3s 一次 TCP 连接尝试成本可忽略；任何上限都可能造成终端静默死亡。
- **选项 B**：改指数退避 + 上限（如 3s→2×→…→60s 封顶）。更省连接尝试，但恢复检测变慢。

## Q2：Location M1-M2 验收通过，是否授权 M3-M4？

M1-M2 已完成（locations + location_id_map 迁移、只读核心层、回滚演练通过）。
M3-M4 = **store_inventory + inventory 合并为单表 + 流水合并 + 污染数据全清回填**——本卡工作量主体。

- **选项 A（推荐）**：授权启动，但**先收口 §24.3f 再动工**（见下方背景）。
- **选项 B**：授权启动，与 §24.3f 收口明确并行边界（24.3f 只剩 InventoryLogMapper.xml 一文件，M3-M4 一期不碰该文件；但两者同属库存链回归面）。
- **选项 C**：暂缓。

**背景（事实陈述，非第四问）**：§24.3f（consumption stats 500）实施已完成（commit `9187286`，测试 5/5 绿），但任务板仍挂 **PENDING**，即 Owner 活体验证/收口未做。M3-M4 触及库存链，属 24.3f 邻域——建议先收口（活体截图核对 stats 接口）再开 M3-M4，或明确并行。

## Q3：`maxReconnectAttempts` 死配置

重连改为监督式后，`WebSocketConfig` 类型里的 `maxReconnectAttempts` 字段不再生效（保留是为避免扩动类型文件）。

- **选项 A（推荐）**：**删除**（含 types/websocket.ts 中字段与两处 defaultConfig 默认值）——死配置误导后来者。
- **选项 B**：注释保留并标注"已由监督重连取代"。

---

*回执方式：Q1/Q2/Q3 各选 A/B/C（可直接批注）。收到批复前不启动 M3-M4、不碰 users.store_id（其 VARCHAR→BIGINT 类型迁移将在 P1-USER-LOCATION-001 开工前单独设计）。*
