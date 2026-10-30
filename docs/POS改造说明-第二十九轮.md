# 第二十九轮：Autofill 浅蓝底色覆盖

> 一行 inset 阴影解决 Chrome 自动填充染色，并补了深色主题变体。19 套件 **308 项全绿**。

## 改动（`styles/pos/base.css`）

```css
input:-webkit-autofill,
input:-webkit-autofill:hover,
input:-webkit-autofill:focus,
input:-webkit-autofill:active,
textarea:-webkit-autofill,
select:-webkit-autofill {
  -webkit-box-shadow: 0 0 0 1000px var(--pos-bg-primary, #ffffff) inset !important;
  box-shadow: 0 0 0 1000px var(--pos-bg-primary, #ffffff) inset !important;
  -webkit-text-fill-color: var(--pos-text-primary, #1e293b) !important;
  caret-color: var(--pos-text-primary, #1e293b);
  transition: background-color 99999s ease-out 0s;
}
```

### 为什么不是只写 `1000px white inset`

1. **深色主题**：POS 夜间模式卡片是 `#2c2c2e`，写死白色会在深灰界面里出现一格格白块。这里改成跟随令牌：
   `[data-theme='dark']` 下用 `--pos-bg-secondary`。
2. **文字颜色**：UA 会把文字也染成深色，深色主题下几乎看不见 → 用 `-webkit-text-fill-color` 锁主题色。
3. **光标颜色**：`caret-color` 不跟随 `text-fill-color`，单独补。
4. **过渡漏色**：部分 Chrome 版本会用一个超长 transition 慢慢露出 UA 底色，`transition: background-color 99999s` 直接堵掉。
5. **登录页**：卡片是纯白且不在 `.fastfood-pos` 容器内，单独一条 `.login-page input:-webkit-autofill` 刷白。

## 验证（`test-review/pos-round29-verify.cjs`，6 项）

| 断言 | 结果 |
|---|---|
| A1 构建产物含 `input:-webkit-autofill` 规则 | ✅ |
| A2 使用 `0 0 0 1000px ... inset` | ✅ |
| A3 锁定 `-webkit-text-fill-color` | ✅ |
| A4 输入后底色为主题白（非 UA 蓝 `#e8f0fe`） | ✅ wrapper=`rgb(255,255,255)` |
| B1 POS 深色主题生效 | ✅ `data-theme=dark` |
| B2 深色有独立 autofill 规则 | ✅ |

## 回归

19 套件 **308 项全绿**（新增 round29 6 项）。

截图：round29-autofill-light.png、round29-autofill-dark.png。
