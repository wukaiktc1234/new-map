# 第九轮：订单查询弹窗「只有表格内滚」

> 反馈：浏览器非全屏时，订单查询弹窗整块内容（标题、筛选栏）跟着滚，右侧出现一条贯穿弹窗的滚动条；期望"只有表格里的订单内容纵向滚动"。
> 验证：八套 E2E 共 **133 项断言全绿**（含 1440×900 与 1280×720 两种视口）。

---

## 一、根因

全局 `dialog.css` 里有一刀切的规则：

```css
body .el-dialog__body { max-height: 70vh; overflow-y: auto; }
```

订单查询的 body 里塞了「标题之外的一切」（筛选栏 + 表格），所以窗口一矮，**body 整体滚动**：
标题被顶上去、筛选栏跟着滚、右侧一条滚动条压着整个弹窗。

同时 `el-table` 又写了 `max-height="440"`，于是**双层滚动**（外层 body + 内层表格）。

## 二、改法：给弹窗"确定高度"，把滚动权只交给表格

```css
/* 1) 弹窗：确定高度 + flex 竖排 + 不滚动 */
body .order-query-dialog.el-dialog {
  height: min(760px, 86vh);   /* 关键：确定高度，内部 flex 才有分配空间 */
  max-height: 86vh;
  margin-top: 6vh !important;
  display: flex; flex-direction: column; overflow: hidden;
}

/* 2) 头部 / 主体：固定与弹性分开 */
body .order-query-dialog .el-dialog__header { flex: 0 0 auto; }
body .order-query-dialog .el-dialog__body {
  flex: 1 1 auto; min-height: 0; display: flex; flex-direction: column;
  overflow: hidden !important; max-height: none !important;
}

/* 3) 筛选栏固定，列表吃掉剩余高度 */
body .order-query-dialog .query-filter { flex: 0 0 auto; }
body .order-query-dialog .order-list { flex: 1 1 auto; min-height: 0; height: 0; overflow: hidden; }

/* 4) 表格吃满剩余高度，内部自己滚 */
body .order-query-dialog .el-table { flex: 1 1 auto; min-height: 0; height: 100% !important; }
```

模板侧：`<el-table height="100%">`（去掉写死的 `max-height="440"`）。

### 踩的两个坑

1. **只给 `max-height` 不给 `height`** → 弹窗高度仍由内容决定，`flex:1` 的表格拿不到可用空间，会被压成 0 或撑破弹窗。实测：`height` 缺失时弹窗 h=228、表格可视高度 0。
2. **`el-table__body-wrapper` 不是滚动容器** —— Element Plus 2.x 表格内部是 `el-scrollbar`，真正的滚动元素是 `.el-scrollbar__wrap`。一开始量错了元素，误判成"没有滚动"。

## 三、验证（两种视口）

| 断言 | 1440×900 | 1280×720 |
|---|---|---|
| 弹窗完整落在视口内 | y=54 h=760 ✅ | y=43 h=619 ✅ |
| 弹窗主体不自己滚动 | overflowY=hidden ✅ | ✅ |
| 标题固定在顶部（未被滚走） | y=71 ✅ | y=60 ✅ |
| 筛选栏常驻可见 | y=157 ✅ | y=146 ✅ |
| 表格内部纵向可滚动（24 条） | scroll 972 / client 360，rows=24 ✅ | scroll 972 / client 351 ✅ |
| 表格无横向截断 | 1038 = 1038 ✅ | 1038 = 1038 ✅ |
| 页面本身无纵向溢出 | 900 = 900 ✅ | 720 = 720 ✅ |

## 四、回归

| 套件 | 项数 | 结果 |
|---|---|---|
| `pos-order-query-scroll.cjs`（新增） | 14 | 全绿 |
| `pos-round8-verify.cjs` | 15 | 全绿 |
| `pos-visual-verify.cjs` | 16 | 全绿 |
| `pos-hold-dialog-verify.cjs` | 16 | 全绿 |
| `pos-round2-verify.cjs` | 42 | 全绿 |
| `pos-ui-verify.cjs` | 20 | 全绿 |
| `pos-negative-cash.cjs` | 5 | 全绿 |
| `pos-payment-double-enter.cjs` | 1 组 | PASS |

截图：`round9-order-query-full.png`（1440×900）、`round9-order-query-small.png`（1280×720，即"非全屏"场景）。

## 五、变更文件

| 文件 | 改动 |
|---|---|
| `styles/pos/components/dialog.css` | 订单查询弹窗全局布局：确定高度 + 头部/筛选栏固定 + 表格内滚 |
| `components/pos/dialogs/OrderQueryDialog.vue` | 表格改 `height="100%"`，移除写死 `max-height` 与组件内旧 body 规则 |

## 六、遗留

- 弹窗高度固定为 `min(760px, 86vh)`：订单少时下方会有留白（换来窗口变矮时布局稳定）。如果更希望"内容少时自动收缩"，可以改成按行数动态计算高度，但要多维护一套逻辑。
