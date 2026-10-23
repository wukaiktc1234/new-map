<script setup lang="ts">
/**
 * 财务口径标注条（P1-UI-TRACE-CL01，纯展示层文字标注）
 *
 * 内容（与决策池 PD-031/032 文本一致，产品决策池 product-decision-backlog.md:47-48）：
 * - 「分/元」金额单位说明：数据库按分存储，页面展示为元（不改变金额展示现状，Batch0-方案2 金额单位治理不触碰）
 * - 「管理台账口径」标签（PD-031）：科目余额 accounting_subjects.balance（分）= 管理口径真相；
 *   account_balance（元，人工）冻结停用
 * - 「管理口径」标签（PD-032）：计价方法 = 最新入库价为当前口径；移动加权平均 = Level 3 演进，不迁移历史数据
 *
 * 视觉同源：仅使用 Element Plus 基础组件 + frontend/src/styles/global/_tokens.scss（var(--fts-*)），无自造样式。
 * 本组件无任何数据依赖、无接口调用、无金额口径变更（纯展示层）。
 */
import { InfoFilled } from '@element-plus/icons-vue'
</script>

<template>
  <div class="caliber-note-bar">
    <el-icon class="caliber-note-bar__icon" :size="14"><InfoFilled /></el-icon>
    <span class="caliber-note-bar__text">金额单位：元（数据库按分存储，页面展示为元）</span>
    <el-tooltip
      content="PD-031：科目余额 accounting_subjects.balance（分）= 管理口径真相；account_balance（元，人工）冻结停用"
      placement="top"
    >
      <el-tag size="small" effect="plain" type="info">管理台账口径</el-tag>
    </el-tooltip>
    <el-tooltip
      content="PD-032：计价方法 = 最新入库价为当前口径；移动加权平均 = Level 3 演进，不迁移历史数据"
      placement="top"
    >
      <el-tag size="small" effect="plain" type="info">管理口径</el-tag>
    </el-tooltip>
  </div>
</template>

<style scoped lang="scss">
.caliber-note-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--fts-space-2);
  padding: var(--fts-space-2) var(--fts-space-4);
  margin-bottom: var(--fts-space-4);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-md);
  color: var(--fts-text-secondary);
  font-size: var(--fts-font-size-sm);

  &__icon {
    color: var(--fts-text-tertiary);
    flex-shrink: 0;
  }

  &__text {
    color: var(--fts-text-secondary);
    margin-right: var(--fts-space-2);
  }
}
</style>