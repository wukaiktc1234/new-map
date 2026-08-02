<script setup lang="ts">
/**
 * 角色洞察信息盒
 *
 * 职责：
 * - 根据当前选中角色显示对应的业务洞察
 * - 提供快捷跳转按钮（切换角色 / 开启对比）
 *
 * 5 种洞察场景：
 * 1. ops_director — 跨域监控者
 * 2. employee — 最小权限原则
 * 3. store_manager — 门店自治者
 * 4. hr_director / finance_director — 专业域负责人
 * 5. 默认 — 通用洞察
 */
import { InfoFilled, Monitor, Warning, CircleCheckFilled } from '@element-plus/icons-vue'

interface Props {
  /** 当前选中角色代码 */
  selectedRole: string
  /** 当前选中角色名 */
  selectedRoleName: string
  /** 对比模式开关 */
  compareMode: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'update:selectedRole', value: string): void
  (e: 'update:compareMode', value: boolean): void
}>()

/** 切换到指定角色 */
function selectRole(code: string): void {
  emit('update:selectedRole', code)
}

/** 切换对比模式 */
function toggleCompareMode(): void {
  emit('update:compareMode', !props.compareMode)
}

/** 与对比目标对比（HR↔财务） */
function compareWith(targetRole: string): void {
  emit('update:compareMode', true)
  emit('update:selectedRole', props.selectedRole)
  // 注意：对比角色由父组件根据当前角色推导，此处仅触发对比模式开启
  void targetRole
}
</script>

<template>
  <div class="insight-boxes">
    <!-- 运营总监 -->
    <div class="insight-box" v-if="selectedRole === 'ops_director'">
      <el-icon><InfoFilled /></el-icon>
      <div class="insight-content">
        <strong>运营总监 — 跨域监控者</strong>
        <p>对 product/order/purchase/warehouse/finance 域为<strong>只读监控(READ_ONLY)</strong>，可查看数据报表但不能执行写操作。这是 D-002 四级权限模型的核心应用场景：让管理层拥有全局视野但不干预业务执行。</p>
        <div class="insight-actions">
          <el-button size="small" type="primary" link @click="selectRole('store_manager')">对比店长权限 →</el-button>
        </div>
      </div>
    </div>

    <!-- 普通员工 -->
    <div class="insight-box insight-box--warning" v-if="selectedRole === 'employee'">
      <el-icon><Warning /></el-icon>
      <div class="insight-content">
        <strong>普通员工 — 最小权限原则</strong>
        <p>仅对 store-ops 域有<strong>受限访问(LIMITED)</strong>，其余8个域完全隐藏。符合最小权限原则——员工仅需完成POS端操作，管理端仅提供极简自助服务（排班/工资条）。</p>
        <div class="insight-actions">
          <el-button size="small" link type="warning" @click="selectRole('team_leader')">对比组长权限 →</el-button>
        </div>
      </div>
    </div>

    <!-- 店长 -->
    <div class="insight-box insight-box--success" v-if="selectedRole === 'store_manager'">
      <el-icon><CircleCheckFilled /></el-icon>
      <div class="insight-content">
        <strong>店长 — 门店自治者</strong>
        <p>对 store-ops/product/order 三个域拥有<strong>完全访问(FULL)</strong>，覆盖日常运营全流程。但无法看到运营、采购、财务等公司级模块，体现"门店高度自治"的集中式单店模板设计。</p>
        <div class="insight-actions">
          <el-button size="small" link type="success" @click="selectRole('ops_director')">对比运营总监 →</el-button>
          <el-button size="small" link type="success" @click="selectRole('employee')">对比员工权限 →</el-button>
        </div>
      </div>
    </div>

    <!-- HR总监 / 财务总监 -->
    <div class="insight-box insight-box--info" v-if="selectedRole === 'hr_director' || selectedRole === 'finance_director'">
      <el-icon><InfoFilled /></el-icon>
      <div class="insight-content">
        <strong>{{ selectedRole === 'hr_director' ? 'HR总监' : '财务总监' }} — 专业域负责人</strong>
        <p>仅在 hr(或 finance) 域有完全权限，对其他业务域均为只读或隐藏。体现职能部门"管好自己的专业领域"的职责边界设计。</p>
        <div class="insight-actions">
          <el-button
            size="small"
            link
            type="primary"
            @click="compareWith(selectedRole === 'hr_director' ? 'finance_director' : 'hr_director')"
          >
            与{{ selectedRole === 'hr_director' ? '财务' : 'HR' }}总监对比 →
          </el-button>
        </div>
      </div>
    </div>

    <!-- 默认洞察（其他角色） -->
    <div class="insight-box insight-box--default" v-if="!['ops_director', 'employee', 'store_manager', 'hr_director', 'finance_director'].includes(selectedRole)">
      <el-icon><Monitor /></el-icon>
      <div class="insight-content">
        <strong>{{ selectedRoleName }}</strong>
        <p>点击矩阵中的单元格可切换该角色的各域访问级别，下方菜单预览面板会实时反映变化。开启对比模式可快速查看两个角色之间的权限差异。</p>
        <div class="insight-actions">
          <el-button size="small" link @click="toggleCompareMode">{{ compareMode ? '退出对比模式' : '开启对比模式' }} →</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.insight-boxes {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3);
}

.insight-box {
  display: flex;
  gap: var(--fts-space-2);
  padding: var(--fts-space-3) var(--fts-space-4);
  background: rgba(var(--fts-primary-rgb, 64, 158, 255), 0.06);
  border-left: 3px solid var(--fts-primary);
  border-radius: 0 var(--fts-radius-md) var(--fts-radius-md) 0;
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
  line-height: 1.6;

  &--warning {
    background: rgba(var(--fts-warning-rgb, 230, 162, 60), 0.08);
    border-color: var(--fts-warning);
  }

  &--success {
    background: rgba(var(--fts-success-rgb, 103, 194, 58), 0.08);
    border-color: var(--fts-success);
  }

  &--info {
    background: rgba(var(--fts-info-rgb, 144, 147, 153), 0.06);
    border-color: var(--fts-info);
  }

  &--default {
    background: var(--fts-bg-tertiary);
    border-color: var(--fts-border-primary);
    border-style: dashed;
  }
}

.insight-content {
  strong {
    display: block;
    color: var(--fts-text-primary);
    margin-bottom: var(--fts-space-1);
  }

  p {
    margin: 0;
  }
}

.insight-actions {
  display: flex;
  gap: var(--fts-space-2);
  margin-top: var(--fts-space-2);
  padding-top: var(--fts-space-2);
  border-top: 1px solid var(--fts-border-secondary);
}
</style>
