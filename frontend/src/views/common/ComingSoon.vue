<script setup lang="ts">
/**
 * 通用「V2.0 未上线」占位页
 *
 * 用途：后端表已建但功能未上线（如 takeout_locker / locker_slot / supplier_evaluations），
 * 菜单项已置灰；若用户直接输入 URL 访问，也会落到这里看到明确说明，
 * **不会出现空表格 + 0.00 的假数据**。
 */
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageHeader from '@/components/core/PageHeader.vue'

const route = useRoute()
const router = useRouter()

const title = computed<string>(() => (route.meta?.title as string) || '该功能')
/** 计划版本号：meta.planVersion 可覆盖，缺省 V2.0 */
const planVersion = computed<string>(() => (route.meta?.planVersion as string) || 'V2.0')

function goHome(): void {
  router.push('/home')
}
</script>

<template>
  <div class="modern-page coming-soon" data-test="coming-soon-page">
    <PageHeader :title="title" description="未上线功能占位说明" />
    <el-card shadow="never" class="coming-soon__card">
      <el-result icon="info" :title="title + ' 将于 ' + planVersion + ' 上线'">
        <template #sub-title>
          <p class="coming-soon__line">
            该功能<strong>当前版本未开放</strong>：后端数据表已建立，但接口与页面尚未交付，
            因此本页<strong>不展示任何金额或统计数据</strong>（避免把「未上线」误读成「无业务发生」）。
          </p>
          <p class="coming-soon__line">
            计划版本：{{ planVersion }} · 如需提前启用请联系产品/项目经理排期。
          </p>
        </template>
        <template #extra>
          <el-button type="primary" data-test="coming-soon-home" @click="goHome">返回工作台</el-button>
        </template>
      </el-result>
    </el-card>
  </div>
</template>

<style scoped>
.coming-soon__card {
  margin-top: var(--fts-space-4, 16px);
}

.coming-soon__line {
  margin: 4px 0;
  font-size: 13px;
  color: var(--fts-text-secondary, #606266);
  line-height: 1.7;
}
</style>
