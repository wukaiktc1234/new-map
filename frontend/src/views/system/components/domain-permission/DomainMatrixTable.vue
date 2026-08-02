<script setup lang="ts">
/**
 * 域权限矩阵表格
 *
 * 职责：
 * - 渲染 角色×域 的权限矩阵表格
 * - 支持横向滚动（鼠标滚轮转水平滚动）
 * - 单元格点击循环切换访问级别
 * - 角色行头和域列头 sticky 定位
 * - 选中角色行高亮
 */
import { ref } from 'vue'
import type { Component } from 'vue'
import type { BusinessDomain, DomainAccessLevel, DomainAccessLevelMetaMap, RoleRow } from '@/types/domain-permission'

interface Props {
  /** 业务域列表（列头） */
  businessDomains: BusinessDomain[]
  /** 矩阵行数据（角色 + 可见域） */
  matrixData: RoleRow[]
  /** 当前选中角色代码 */
  selectedRole: string
  /** 获取单元格访问级别（响应式） */
  getLevel: (domainCode: string, roleCode: string) => DomainAccessLevel
  /** 获取访问级别样式 */
  getLevelStyle: (level: DomainAccessLevel) => Record<string, string>
  /** 解析图标名为组件 */
  resolveIcon: (iconName: string | undefined) => Component
  /** 访问级别元数据 */
  levelMeta: DomainAccessLevelMetaMap
}

defineProps<Props>()

const emit = defineEmits<{
  (e: 'cell-click', roleCode: string, domainCode: string): void
}>()

/** 矩阵滚动容器引用 */
const matrixContainerRef = ref<HTMLElement>()

/**
 * 鼠标滚轮事件：将垂直滚动转为水平滚动
 * 仅在表格有横向滚动空间时拦截
 */
function onMatrixWheel(event: WheelEvent): void {
  const container = matrixContainerRef.value
  if (!container) return
  const canScroll = container.scrollWidth > container.clientWidth
  if (!canScroll) return
  event.preventDefault()
  container.scrollLeft += event.deltaY || event.deltaX
}

/** 单元格点击处理 */
function handleCellClick(roleCode: string, domainCode: string): void {
  emit('cell-click', roleCode, domainCode)
}

/** 获取单元格短文字（全/读/限/—） */
function getLevelShortText(level: DomainAccessLevel): string {
  if (level === 'FULL') return '全'
  if (level === 'READ_ONLY') return '读'
  if (level === 'LIMITED') return '限'
  return ''
}

/** 获取单元格图标符号（✓/👁/◐） */
function getLevelBadge(level: DomainAccessLevel): string {
  if (level === 'FULL') return '✓'
  if (level === 'READ_ONLY') return '👁'
  if (level === 'LIMITED') return '◐'
  return ''
}
</script>

<template>
  <div
    ref="matrixContainerRef"
    class="matrix-container"
    @wheel="onMatrixWheel"
  >
    <div class="matrix-wrapper">
      <table class="permission-matrix">
        <thead>
          <tr>
            <th class="corner-cell">
              <span>角色 \ 域</span>
            </th>
            <th v-for="domain in businessDomains" :key="domain.domainCode" class="domain-header">
              <div class="domain-info">
                <el-icon :size="16"><component :is="resolveIcon(domain.icon)" /></el-icon>
                <span>{{ domain.domainName }}</span>
              </div>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="row in matrixData"
            :key="row.code"
            :class="{ 'row--highlight': selectedRole === row.code }"
          >
            <td class="role-cell">
              <div class="role-info">
                <span class="role-name">{{ row.name }}</span>
                <span class="role-count">{{ row.domains.length }}个域可见</span>
              </div>
            </td>
            <td
              v-for="domain in businessDomains"
              :key="`${row.code}-${domain.domainCode}`"
              :class="[
                'access-cell',
                `cell--${getLevel(domain.domainCode, row.code).toLowerCase()}`,
                {
                  'cell--locked': row.code === 'admin' || row.code === 'owner',
                  'cell--selected': selectedRole === row.code,
                }
              ]"
              :style="getLevelStyle(getLevel(domain.domainCode, row.code))"
              @click="handleCellClick(row.code, domain.domainCode)"
            >
              <div class="cell-content">
                <template v-if="getLevel(domain.domainCode, row.code) !== 'HIDDEN'">
                  <span class="level-badge">{{ getLevelBadge(getLevel(domain.domainCode, row.code)) }}</span>
                  <span class="level-text-short">{{ getLevelShortText(getLevel(domain.domainCode, row.code)) }}</span>
                </template>
                <template v-else>
                  <span class="dash">—</span>
                </template>
              </div>
              <el-tooltip
                v-if="getLevel(domain.domainCode, row.code) !== 'HIDDEN'"
                :content="levelMeta[getLevel(domain.domainCode, row.code)].description"
                placement="top"
                :show-after="300"
              >
                <div class="tooltip-trigger"></div>
              </el-tooltip>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped lang="scss">
.matrix-container {
  overflow-x: auto;
  overflow-y: visible;
  border-radius: var(--fts-radius-md);
  border: 1px solid var(--fts-border-primary);
  background: var(--fts-bg-card);

  &::-webkit-scrollbar {
    height: 6px;
  }
  &::-webkit-scrollbar-track {
    background: transparent;
  }
  &::-webkit-scrollbar-thumb {
    background: var(--fts-border-primary);
    border-radius: 3px;
    &:hover {
      background: var(--fts-text-tertiary);
    }
  }
}

.matrix-wrapper {
  min-width: 100%;
}

.permission-matrix {
  width: 100%;
  border-collapse: collapse;
  font-size: var(--fts-font-size-sm);

  th, td {
    padding: 0;
    text-align: center;
    border: 1px solid var(--fts-border-secondary);
  }

  .corner-cell {
    position: sticky;
    left: 0;
    z-index: 10;
    background: var(--fts-bg-card);
    min-width: 150px;
    padding: var(--fts-space-3) var(--fts-space-4);
    font-weight: 600;
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-secondary);
  }

  .domain-header {
    background: var(--fts-bg-tertiary);
    min-width: 90px;
    padding: var(--fts-space-3) var(--fts-space-2);

    .domain-info {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 2px;

      span {
        font-size: var(--fts-font-size-xs);
        white-space: nowrap;
      }
    }
  }

  .role-cell {
    position: sticky;
    left: 0;
    z-index: 5;
    background: var(--fts-bg-card);
    min-width: 150px;
    padding: var(--fts-space-3) var(--fts-space-4);

    .role-info {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      gap: 2px;
    }

    .role-name {
      font-weight: 500;
      color: var(--fts-text-primary);
    }

    .role-count {
      font-size: var(--fts-font-size-xs);
      color: var(--fts-text-tertiary);
    }
  }

  .access-cell {
    min-width: 72px;
    padding: var(--fts-space-2) var(--fts-space-1);
    cursor: pointer;
    transition: box-shadow 0.15s ease, background-color 0.15s ease;
    position: relative;

    &:hover:not(.cell--locked):not(.cell--hidden) {
      z-index: 3;
      box-shadow: 0 0 0 2px var(--fts-primary), 0 4px 12px rgba(0, 0, 0, 0.1);
    }

    .cell-content {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 36px;
      gap: 1px;
    }

    .level-badge {
      font-size: 14px;
      line-height: 1;
    }

    .level-text-short {
      font-size: 10px;
      font-weight: 600;
      letter-spacing: 0.5px;
    }

    .dash {
      color: var(--fts-text-quaternary);
      font-size: 18px;
    }

    &--hidden {
      cursor: default;
      opacity: 0.4;

      &:hover {
        transform: none;
        box-shadow: none;
      }
    }

    &--locked {
      cursor: not-allowed;
      opacity: 0.75;

      &:hover {
        transform: none;
        box-shadow: none;
      }
    }

    &--selected {
      .role-cell &,
      &.role-cell {
        box-shadow: inset 4px 0 0 var(--fts-primary);
      }
      &:not(.role-cell) {
        background-color: rgba(var(--fts-primary-rgb, 64, 158, 255), 0.06);
      }
    }
  }

  .tooltip-trigger {
    position: absolute;
    inset: 0;
  }

  .row--highlight {
    .role-cell {
      background: var(--fts-bg-card);
      box-shadow: inset 4px 0 0 var(--fts-primary);
    }
  }
}
</style>
