# 采购订单详情状态戳设计草图（方案 E）

> 需求：将采购订单详情对话框顶端的状态栏改为“圆形图章/盖戳”样式，位置固定在右上角，文字以顺时针方向向右倾斜。

---

## 通用结构

在详情对话框的 `.status-banner` 内部右上角追加一个 `.order-stamp` 元素：

```html
<div class="status-banner" :class="`status-banner--${currentDetail.status}`">
  <StatusTag ... />
  <span class="status-banner__order-no">{{ currentDetail.orderNo }}</span>
  <div class="order-stamp" :class="`order-stamp--${currentDetail.status}`">
    {{ purchaseOrderConverter.toStatusLabel(currentDetail.status) }}
  </div>
</div>
```

`.status-banner` 需要 `position: relative`，`.order-stamp` 使用 `position: absolute; top/right` 定位。

状态色统一复用项目已有的 `--fts-*` 变量：

| 状态 | 主色变量 | 背景/浅色变量 |
|------|----------|---------------|
| draft | `--fts-text-secondary` | `--fts-bg-tertiary` |
| pending | `--fts-warning` | `--fts-warning-bg` |
| approved | `--fts-info` | `--fts-info-bg` |
| ordered/shipped | `--fts-primary` | `--fts-primary-bg` |
| received | `--fts-info` | `--fts-info-bg` |
| rejected/terminated | `--fts-error` | `--fts-error-bg` |
| completed | `--fts-success` | `--fts-success-bg` |
| partial_received | `--fts-warning` | `--fts-warning-bg` |
| cancelled | `--fts-info` | `--fts-info-bg` |

---

## 草图 E-1：经典双线描边图章（推荐）

特点：外圈实线 + 内圈细线，形成“公章”质感；文字横向排列，整体顺时针旋转 18°。

```scss
.order-stamp {
  position: absolute;
  top: 12px;
  right: 20px;
  width: 88px;
  height: 88px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--fts-font-size-base);
  font-weight: var(--fts-font-weight-bold);
  letter-spacing: 2px;
  color: var(--stamp-color, var(--fts-primary));
  border: 3px solid var(--stamp-color, var(--fts-primary));
  background: var(--stamp-bg, var(--fts-bg-card));
  transform: rotate(18deg);
  transform-origin: center;
  pointer-events: none;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);

  &::before {
    content: '';
    position: absolute;
    inset: 5px;
    border-radius: 50%;
    border: 1px solid var(--stamp-color, var(--fts-primary));
    opacity: 0.6;
  }
}
```

---

## 草图 E-2：纯色低饱和填充图章

特点：低饱和度半透明背景 + 同色系粗边框，文字使用深色以保证可读性；整体顺时针旋转 12°。

```scss
.order-stamp {
  position: absolute;
  top: 12px;
  right: 20px;
  width: 84px;
  height: 84px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--fts-font-size-base);
  font-weight: var(--fts-font-weight-bold);
  letter-spacing: 1px;
  color: var(--stamp-text, var(--fts-text-primary));
  border: 3px solid var(--stamp-color, var(--fts-primary));
  background: var(--stamp-bg, var(--fts-primary-bg));
  transform: rotate(12deg);
  transform-origin: center;
  pointer-events: none;
}
```

建议将 `--stamp-bg` 设为 `color-mix(in srgb, var(--stamp-color) 15%, transparent)` 或直接使用 `--fts-*-bg` 变量。

---

## 草图 E-3：虚线边框轮廓图章

特点：简洁的虚线外圈，无填充，更轻量化；文字顺时针旋转 22°，视觉更活泼。

```scss
.order-stamp {
  position: absolute;
  top: 12px;
  right: 20px;
  width: 86px;
  height: 86px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--fts-font-size-base);
  font-weight: var(--fts-font-weight-bold);
  letter-spacing: 2px;
  color: var(--stamp-color, var(--fts-primary));
  border: 2px dashed var(--stamp-color, var(--fts-primary));
  background: transparent;
  transform: rotate(22deg);
  transform-origin: center;
  pointer-events: none;
  opacity: 0.9;
}
```

---

## 按状态覆盖示例

```scss
.order-stamp {
  &--draft {
    --stamp-color: var(--fts-text-secondary);
    --stamp-bg: var(--fts-bg-tertiary);
    --stamp-text: var(--fts-text-primary);
  }
  &--pending {
    --stamp-color: var(--fts-warning);
    --stamp-bg: var(--fts-warning-bg);
    --stamp-text: #7a4b00; /* 仅当需要手动指定深色文字时使用 */
  }
  &--approved,
  &--received,
  &--cancelled {
    --stamp-color: var(--fts-info);
    --stamp-bg: var(--fts-info-bg);
    --stamp-text: var(--fts-text-primary);
  }
  &--ordered,
  &--shipped {
    --stamp-color: var(--fts-primary);
    --stamp-bg: var(--fts-primary-bg);
    --stamp-text: var(--fts-text-primary);
  }
  &--rejected,
  &--terminated {
    --stamp-color: var(--fts-error);
    --stamp-bg: var(--fts-error-bg);
    --stamp-text: var(--fts-text-primary);
  }
  &--completed {
    --stamp-color: var(--fts-success);
    --stamp-bg: var(--fts-success-bg);
    --stamp-text: var(--fts-text-primary);
  }
  &--partial_received {
    --stamp-color: var(--fts-warning);
    --stamp-bg: var(--fts-warning-bg);
    --stamp-text: var(--fts-text-primary);
  }
}
```

---

## 推荐结论

- 当前需求强调“盖戳”感，**草图 E-1 经典双线描边**最能表达图章语义，且与现有 StatusTag 不冲突。
- 旋转角度建议 **12°–18°**，既能看出倾斜，又不会导致文字难以阅读。
- 右上角定位建议 `top: 12px; right: 20px`，大小 84–88px，既醒目又不遮挡标题和重要字段。
