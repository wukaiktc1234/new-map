<script setup lang="ts">
/**
 * 域权限矩阵 Tab（主组件）
 *
 * 职责：仅负责布局编排和组件组合
 * - 顶部 section-header（模板切换选择器、同步按钮、保存按钮、角色快速定位）
 * - level-legend-bar（4种级别图例）
 * - scroll-hint（滚动提示）
 * - 加载中：Skeleton 骨架屏
 * - 加载失败：EmptyState 错误状态
 * - 主内容：DomainMatrixTable + DomainRoleCards + DomainInsightBoxes + DomainMenuPreview
 *
 * 业务逻辑全部委托给 useDomainPermission composable
 * 数据源：后端 /v1/permission-templates (4 种系统模板 + 自定义模板)
 */
import { onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { RefreshRight, DataLine } from '@element-plus/icons-vue'
import Skeleton from '@/components/core/Skeleton.vue'
import EmptyState from '@/components/core/EmptyState.vue'
import DomainMatrixTable from './domain-permission/DomainMatrixTable.vue'
import DomainRoleCards from './domain-permission/DomainRoleCards.vue'
import DomainMenuPreview from './domain-permission/DomainMenuPreview.vue'
import DomainInsightBoxes from './domain-permission/DomainInsightBoxes.vue'
import {
  useDomainPermission,
  businessDomains,
  DomainAccessLevelMeta,
  getLevelStyle,
  resolveIcon,
} from '@/composables/useDomainPermission'
import type { DomainAccessLevel } from '@/types/domain-permission'

const {
  // 状态
  currentTemplateCode,
  templateOptions,
  loading,
  loadError,
  syncing,
  savingCustom,
  selectedRole,
  compareMode,
  compareRole,
  // 计算属性
  roles,
  nonAdminRoles,
  matrixData,
  menuPreview,
  visibleMenuCount,
  readOnlyMenuCount,
  fullAccessMenuCount,
  totalMenuCount,
  totalDomainCount,
  compareDiff,
  roleOverviewStats,
  // 矩阵读取
  getRoleDomainAccessLevel,
  getRoleDomains,
  getFullRoleDomainMatrix,
  // 矩阵编辑
  cycleRoleDomainAccess,
  // 模板加载与切换
  loadTemplates,
  selectTemplate,
  // 同步与保存
  syncToMenu,
  saveAsCustomTemplate,
} = useDomainPermission()

// ========== 选中角色名（用于菜单预览和洞察盒） ==========
function findRoleName(code: string): string {
  return roles.value.find(r => r.code === code)?.name || code
}

// ========== 单元格点击：循环切换访问级别 ==========
function handleCellClick(roleCode: string, domainCode: string): void {
  // admin/owner 角色禁止修改（composable 内已保护，此处仅给用户提示）
  if (roleCode === 'admin' || roleCode === 'owner') {
    ElMessage.warning('超级管理员拥有所有域的完全访问权限，不可修改')
    return
  }

  // 调用 composable 的循环切换方法（内部会递增 matrixVersion 触发响应式更新）
  const next = cycleRoleDomainAccess(roleCode, domainCode)
  const meta = DomainAccessLevelMeta[next]
  ElMessage.success(`已切换为「${meta.label}」：${meta.description}`)
}

// ========== 获取单元格访问级别（响应式） ==========
function getLevel(domainCode: string, roleCode: string): DomainAccessLevel {
  return getRoleDomainAccessLevel(roleCode, domainCode)
}

// ========== 模板切换 ==========
function onTemplateChange(code: string): void {
  selectTemplate(code).catch((error) => {
    console.error('[DomainPermissionTab] 切换模板失败:', error)
    ElMessage.error('切换模板失败')
  })
}

// ========== 加载失败重试 ==========
function handleRetry(): void {
  loadTemplates()
}

// ========== 初始化：加载模板 ==========
onMounted(() => {
  loadTemplates()
})
</script>

<template>
  <div class="domain-permission">
    <!-- ========== 顶部操作区：模板切换 + 同步 + 保存 + 角色定位 ========== -->
    <div class="section-header">
      <div class="section-desc">配置每个角色对各业务域的访问级别（点击单元格循环切换）</div>
      <div class="header-actions">
        <el-select
          :model-value="currentTemplateCode"
          placeholder="选择权限模板"
          size="default"
          style="width: 180px"
          @update:model-value="onTemplateChange"
        >
          <el-option
            v-for="opt in templateOptions"
            :key="opt.code"
            :label="opt.name"
            :value="opt.code"
          >
            <span>{{ opt.name }}</span>
            <span class="option-desc">{{ opt.description }}</span>
          </el-option>
        </el-select>
        <el-select
          v-model="selectedRole"
          placeholder="快速定位角色"
          size="default"
          style="width: 160px"
        >
          <el-option
            v-for="role in roles"
            :key="role.code"
            :label="role.name"
            :value="role.code"
          />
        </el-select>
        <el-button
          type="primary"
          size="default"
          :loading="syncing"
          :disabled="selectedRole === 'admin' || selectedRole === 'owner'"
          @click="syncToMenu"
        >
          <el-icon><RefreshRight /></el-icon>
          同步到菜单
        </el-button>
        <el-button
          type="success"
          size="default"
          :loading="savingCustom"
          @click="saveAsCustomTemplate"
        >
          <el-icon><DataLine /></el-icon>
          保存为自定义模板
        </el-button>
      </div>
    </div>

    <!-- ========== 级别图例栏 ========== -->
    <div class="level-legend-bar">
      <div
        v-for="(meta, level) in DomainAccessLevelMeta"
        :key="level"
        class="legend-item"
      >
        <span
          class="legend-dot"
          :style="{ background: getLevelStyle(level as DomainAccessLevel).color }"
        ></span>
        <span class="legend-label">{{ meta.label }}</span>
        <span class="legend-desc">{{ meta.description }}</span>
      </div>
    </div>

    <!-- ========== 滚动提示 ========== -->
    <div class="scroll-hint">
      <span class="hint-icon">↔</span>
      使用鼠标滚轮可横向滚动表格
    </div>

    <!-- ========== 加载中：骨架屏 ========== -->
    <Skeleton
      v-if="loading"
      type="table"
      :rows="6"
    />

    <!-- ========== 加载失败：空状态 ========== -->
    <EmptyState
      v-else-if="loadError"
      type="error"
      title="权限模板加载失败"
      description="无法从后端加载权限模板，请检查网络或后端服务后重试"
      action-text="重新加载"
      @action="handleRetry"
    />

    <!-- ========== 主内容区 ========== -->
    <template v-else>
      <!-- 权限矩阵表格 -->
      <DomainMatrixTable
        :business-domains="businessDomains"
        :matrix-data="matrixData"
        :selected-role="selectedRole"
        :get-level="getLevel"
        :get-level-style="getLevelStyle"
        :resolve-icon="resolveIcon"
        :level-meta="DomainAccessLevelMeta"
        @cell-click="handleCellClick"
      />

      <!-- 角色卡片对比面板（含摘要栏、对比模式、洞察盒） -->
      <DomainRoleCards
        :business-domains="businessDomains"
        :non-admin-roles="nonAdminRoles"
        :roles="roles"
        :compare-diff="compareDiff"
        :role-overview-stats="roleOverviewStats"
        :total-domain-count="totalDomainCount"
        :get-level="getLevel"
        :get-role-domains="getRoleDomains"
        :get-full-role-domain-matrix="getFullRoleDomainMatrix"
        :level-meta="DomainAccessLevelMeta"
        v-model:selected-role="selectedRole"
        v-model:compare-mode="compareMode"
        v-model:compare-role="compareRole"
      />

      <!-- 角色洞察信息盒 -->
      <DomainInsightBoxes
        :selected-role-name="findRoleName(selectedRole)"
        v-model:selected-role="selectedRole"
        v-model:compare-mode="compareMode"
      />

      <!-- 菜单实时预览面板 -->
      <DomainMenuPreview
        :menu-preview="menuPreview"
        :visible-menu-count="visibleMenuCount"
        :full-access-menu-count="fullAccessMenuCount"
        :read-only-menu-count="readOnlyMenuCount"
        :total-menu-count="totalMenuCount"
        :selected-role-name="findRoleName(selectedRole)"
        :level-meta="DomainAccessLevelMeta"
        :resolve-icon="resolveIcon"
      />
    </template>
  </div>
</template>

<style scoped lang="scss">
.domain-permission {
  width: 100%;
}

// ========== 顶部操作区 ==========
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--fts-space-4);
  flex-wrap: wrap;
  gap: var(--fts-space-3);
}

.section-desc {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-tertiary);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  flex-wrap: wrap;
}

// 模板选择器选项描述
.option-desc {
  margin-left: var(--fts-space-2);
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
}

// ========== 级别图例栏 ==========
.level-legend-bar {
  display: flex;
  gap: var(--fts-space-6);
  padding: var(--fts-space-3) var(--fts-space-4);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-md);
  margin-bottom: var(--fts-space-4);
  flex-wrap: wrap;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);

  .legend-dot {
    width: 14px;
    height: 14px;
    border-radius: 4px;
    flex-shrink: 0;
  }

  .legend-label {
    font-size: var(--fts-font-size-sm);
    font-weight: 500;
    color: var(--fts-text-primary);
    white-space: nowrap;
  }

  .legend-desc {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
  }
}

// ========== 滚动提示 ==========
.scroll-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--fts-space-1);
  padding: var(--fts-space-2) 0;
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-quaternary);

  .hint-icon {
    font-size: var(--fts-font-size-sm);
    animation: scroll-hint-bounce 2s ease-in-out infinite;
  }
}

@keyframes scroll-hint-bounce {
  0%, 100% { transform: translateX(0); opacity: 0.5; }
  50% { transform: translateX(4px); opacity: 1; }
}
</style>
