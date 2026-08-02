<script setup lang="ts">
/**
 * TaskAssigneeSection - 任务接收人设置区域
 * 包含：目标类型切换、员工多选（按部门分组）、组织单元选择、分配方式、跨部门协作
 */
import StatusTag from '@/components/core/StatusTag.vue'

type TargetType = 'individual' | 'department' | 'store'
type AssignmentMode = 'designated' | 'open_pickup'

interface Employee {
  id: string
  name: string
  deptId: string
  deptName: string
  storeId: string
  storeName: string
  role: 'store_manager' | 'dept_head' | 'staff'
  avatar?: string
}

interface OrgUnit {
  id: string
  name: string
  type: 'department' | 'store'
  memberCount: number
  headName: string
  headId: string
  parentStoreId?: string
}

const props = defineProps<{
  category: string
  targetType: TargetType
  targetId: string
  assignmentMode: AssignmentMode
  allowCrossDept: boolean
  assigneeIds: string[]
  availableEmployees: Employee[]
  availableOrgUnits: OrgUnit[]
  availableTargetTypes: { value: TargetType; label: string; icon: string }[]
  userRoleLabel: string
  assigneeHintText: string
  isForceSingle: boolean
  showCrossDeptSection: boolean
  crossDeptOptions: OrgUnit[]
  crossDeptSelected: string[]
}>()

const emit = defineEmits<{
  (e: 'update:targetType', value: TargetType): void,
  (e: 'update:targetId', value: string): void,
  (e: 'update:assignmentMode', value: AssignmentMode): void,
  (e: 'update:allowCrossDept', value: boolean): void,
  (e: 'toggleEmployee', empId: string): void,
  (e: 'selectOrgUnit', orgId: string): void,
  (e: 'targetTypeChange'): void,
  (e: 'toggleCrossDept', deptId: string): void
}>()

function getRoleLabel(role: string): string {
  const labels: Record<string, string> = {
    store_manager: '店长',
    dept_head: '主管',
    staff: '员工',
  }
  return labels[role] || '员工'
}

// 按部门分组显示可用员工
interface EmployeeGroup {
  deptId: string
  deptName: string
  members: Employee[]
}

const groupedAvailableEmployees = computed<EmployeeGroup[]>(() => {
  const groups = new Map<string, EmployeeGroup>()
  for (const emp of props.availableEmployees) {
    if (!groups.has(emp.deptId)) {
      groups.set(emp.deptId, { deptId: emp.deptId, deptName: emp.deptName, members: [] })
    }
    groups.get(emp.deptId)!.members.push(emp)
  }
  return Array.from(groups.values())
})

// 当前选中的组织单元信息
const selectedOrgUnit = computed(() =>
  props.availableOrgUnits.find(ou => ou.id === props.targetId)
)
</script>

<template>
  <section v-if="category" class="assignee-section">
    <div class="assignee-section__header">
      <h4 class="assignee-section__title">接收人设置</h4>
      <span class="assignee-section__badge">{{ userRoleLabel }}</span>
    </div>
    <p class="assignee-section__hint">{{ assigneeHintText }}</p>

    <!-- 目标类型切换 -->
    <div v-if="availableTargetTypes.length > 1" class="target-type-switch">
      <button
        v-for="tt in availableTargetTypes"
        :key="tt.value"
        type="button"
        :class="['target-type-btn', { 'target-type-btn--active': targetType === tt.value }]"
        @click="emit('update:targetType', tt.value); emit('targetTypeChange')"
      >
        {{ tt.label }}
      </button>
    </div>

    <!-- 指定个人：员工多选（动态数据源） -->
    <div v-if="targetType === 'individual'" class="assignee-area">
      <!-- 权限不足提示 -->
      <div v-if="availableEmployees.length === 0" class="permission-empty">
        <span class="permission-icon">!</span>
        <p>当前角色暂无权指定他人，仅可发布自荐任务</p>
      </div>
      <template v-else>
        <!-- 按部门分组显示员工 -->
        <div v-for="deptGroup in groupedAvailableEmployees" :key="deptGroup.deptId" class="employee-group">
          <span class="employee-group__label">{{ deptGroup.deptName }}（{{ deptGroup.members.length }}人）</span>
          <div class="employee-list">
            <button
              v-for="emp in deptGroup.members"
              :key="emp.id"
              type="button"
              :class="[
                'employee-chip',
                { 'employee-chip--selected': assigneeIds.includes(emp.id) },
              ]"
              @click="emit('toggleEmployee', emp.id)"
            >
              <span class="employee-chip__name">{{ emp.name }}</span>
              <span class="employee-chip__role">{{ getRoleLabel(emp.role) }}</span>
              <span v-if="assigneeIds.includes(emp.id)" class="employee-chip__check">&#10003;</span>
            </button>
          </div>
        </div>
        <p v-if="isForceSingle && assigneeIds.length > 0" class="assignee-note">
          此类任务仅支持指定1人
        </p>
      </template>
    </div>

    <!-- 指定部门/门店（动态数据源） -->
    <div v-if="targetType === 'department' || targetType === 'store'" class="assignee-area">
      <!-- 权限不足提示 -->
      <div v-if="availableOrgUnits.length === 0" class="permission-empty">
        <span class="permission-icon">!</span>
        <p>当前角色无权发布组织级任务</p>
      </div>
      <template v-else>
        <div class="org-unit-list">
          <button
            v-for="ou in availableOrgUnits"
            :key="ou.id"
            type="button"
            :class="['org-card', { 'org-card--selected': targetId === ou.id }]"
            @click="emit('selectOrgUnit', ou.id)"
          >
            <div class="org-card__main">
              <span class="org-card__name">{{ ou.name }}</span>
              <span class="org-card__type">{{ ou.type === 'store' ? '门店' : '部门' }}</span>
            </div>
            <span class="org-card__meta">
              {{ ou.memberCount }}人 · 负责人：{{ ou.headName }}
            </span>
          </button>
        </div>

        <!-- 分配方式 + 跨部门协作 -->
        <div class="mode-settings">
          <div class="mode-row">
            <span class="mode-label">分配方式</span>
            <div class="mode-options">
              <button
                type="button"
                :class="['mode-chip', { 'mode-chip--active': assignmentMode === 'designated' }]"
                @click="emit('update:assignmentMode', 'designated')"
              >指定执行</button>
              <button
                type="button"
                :class="['mode-chip', { 'mode-chip--active': assignmentMode === 'open_pickup' }]"
                @click="emit('update:assignmentMode', 'open_pickup')"
              >公开接取</button>
            </div>
          </div>

          <!-- 跨部门协作区（仅店长/部门负责人可见） -->
          <div v-if="showCrossDeptSection" class="cross-dept-area">
            <div class="mode-row mode-row--toggle">
              <span class="mode-label">邀请其他{{ targetType === 'store' ? '门店' : '部门' }}协助</span>
              <label class="toggle-switch">
                <input
                  type="checkbox"
                  :checked="allowCrossDept"
                  class="toggle-input"
                  @change="emit('update:allowCrossDept', ($event.target as HTMLInputElement).checked)"
                />
                <span class="toggle-track" />
              </label>
            </div>
            <!-- 跨部门选择：显示可选的其他部门 -->
            <div v-if="allowCrossDept" class="cross-dept-options">
              <p class="cross-dept-hint">选择需要协助的部门（对方部门负责人将收到确认请求）：</p>
              <div class="cross-dept-list">
                <button
                  v-for="cd in crossDeptOptions"
                  :key="cd.id"
                  type="button"
                  :class="['cross-dept-chip', { 'cross-dept-chip--selected': crossDeptSelected.includes(cd.id) }]"
                  @click="emit('toggleCrossDept', cd.id)"
                >
                  {{ cd.name }}
                  <span class="cross-dept-chip__head">{{ cd.headName }}</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>
  </section>
</template>

<style scoped lang="scss">
// ====== 接收人设置区 ======
.assignee-section {
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-lg);
  padding: var(--fts-space-4);
  border: 1px solid var(--fts-border-secondary);

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--fts-space-2);
    margin-bottom: var(--fts-space-1);
  }

  &__title {
    font-size: var(--fts-font-size-base);
    font-weight: 600;
    color: var(--fts-text-primary);
    margin: 0;
  }

  &__badge {
    font-size: var(--fts-font-size-2xs);
    font-weight: 600;
    color: var(--fts-primary);
    padding: 2px 8px;
    border-radius: var(--fts-radius-full);
    background: rgba(var(--fts-primary-rgb), 0.10);
    flex-shrink: 0;
  }

  &__hint {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
    margin: 0 0 var(--fts-space-3);
  }
}

// 权限不足提示
.permission-empty {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  padding: var(--fts-space-4);
  text-align: center;
  justify-content: center;

  .permission-icon {
    width: 28px;
    height: 28px;
    border-radius: 50%;
    background: rgba(var(--fts-warning-rgb), 0.10);
    color: var(--fts-warning);
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 700;
    font-size: var(--fts-font-size-sm);
    flex-shrink: 0;
  }

  p {
    margin: 0;
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-tertiary);
  }
}

// 员工按部门分组
.employee-group {
  margin-bottom: var(--fts-space-3);

  &:last-child { margin-bottom: 0; }

  &__label {
    display: block;
    font-size: var(--fts-font-size-xs);
    font-weight: 600;
    color: var(--fts-text-quaternary);
    text-transform: uppercase;
    letter-spacing: 0.5px;
    margin-bottom: var(--fts-space-2);
    padding-left: var(--fts-space-1);
  }
}

// 目标类型切换按钮组
.target-type-switch {
  display: flex;
  gap: var(--fts-space-2);
  margin-bottom: var(--fts-space-3);
}

.target-type-btn {
  flex: 1;
  padding: var(--fts-space-2) var(--fts-space-3);
  font-size: var(--fts-font-size-sm);
  font-weight: 500;
  color: var(--fts-text-secondary);
  background: transparent;
  border: 1.5px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-md);
  cursor: pointer;
  transition: all 0.15s ease;

  &:hover:not(&--active) { border-color: var(--fts-border-hover); }

  &--active {
    border-color: var(--fts-primary);
    background: rgba(var(--fts-primary-rgb), 0.06);
    color: var(--fts-primary);
    font-weight: 600;
  }
}

// 员工选择列表
.employee-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--fts-space-2);
}

.employee-chip {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-2) var(--fts-space-3);
  border: 1.5px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-md);
  background: transparent;
  cursor: pointer;
  transition: all 0.15s ease;

  &:hover:not(&--selected) { border-color: var(--fts-border-hover); }

  &--selected {
    border-color: var(--fts-primary);
    background: rgba(var(--fts-primary-rgb), 0.06);
  }

  &__name {
    font-size: var(--fts-font-size-sm);
    font-weight: 500;
    color: var(--fts-text-primary);
  }

  &__role {
    font-size: var(--fts-font-size-2xs);
    color: var(--fts-text-quaternary);
    background: var(--fts-bg-tertiary);
    padding: 1px 6px;
    border-radius: var(--fts-radius-full);
  }

  &__check {
    color: var(--fts-primary);
    font-weight: 700;
    font-size: var(--fts-font-size-sm);
  }
}

.assignee-note {
  font-size: var(--fts-font-size-2xs);
  color: var(--fts-warning);
  margin: var(--fts-space-2) 0 0;
}

// 组织单元卡片（部门/门店）
.org-unit-list {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2);
  margin-bottom: var(--fts-space-3);
}

.org-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--fts-space-3) var(--fts-space-4);
  border: 1.5px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-md);
  background: transparent;
  cursor: pointer;
  transition: all 0.15s ease;

  &:hover:not(&--selected) { border-color: var(--fts-border-hover); }

  &--selected {
    border-color: var(--fts-primary);
    background: rgba(var(--fts-primary-rgb), 0.06);
  }

  &__main {
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);
  }

  &__name {
    font-size: var(--fts-font-size-base);
    font-weight: 500;
    color: var(--fts-text-primary);
  }

  &__type {
    font-size: var(--fts-font-size-2xs);
    color: var(--fts-text-quaternary);
    padding: 1px 6px;
    border-radius: var(--fts-radius-full);
    background: var(--fts-bg-tertiary);
  }

  &__meta {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
    flex-shrink: 0;
  }
}

// 跨部门协作区域
.cross-dept-area {
  margin-top: var(--fts-space-3);
  padding-top: var(--fts-space-3);
  border-top: 1px dashed var(--fts-border-secondary);
}

.cross-dept-options {
  margin-top: var(--fts-space-3);
}

.cross-dept-hint {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
  margin: 0 0 var(--fts-space-2);
}

.cross-dept-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--fts-space-2);
}

.cross-dept-chip {
  display: inline-flex;
  align-items: center;
  gap: var(--fts-space-1);
  padding: var(--fts-space-2) var(--fts-space-3);
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
  background: transparent;
  border: 1.5px dashed var(--fts-border-secondary);
  border-radius: var(--fts-radius-md);
  cursor: pointer;
  transition: all 0.15s ease;

  &:hover:not(&--selected) { border-color: var(--fts-border-hover); }

  &--selected {
    border-style: solid;
    border-color: var(--fts-primary);
    background: rgba(var(--fts-primary-rgb), 0.06);
    color: var(--fts-primary);
  }

  &__head {
    font-size: var(--fts-font-size-2xs);
    opacity: 0.7;
  }
}

// 分配方式 + 跨部门开关
.mode-settings {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3);
  padding-top: var(--fts-space-3);
  border-top: 1px solid var(--fts-border-secondary);
}

.mode-row {
  display: flex;
  justify-content: space-between;
  align-items: center;

  &--toggle {
    padding-top: var(--fts-space-2);
  }
}

.mode-label {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
  font-weight: 500;
}

.mode-options {
  display: flex;
  gap: var(--fts-space-2);
}

.mode-chip {
  padding: var(--fts-space-1) var(--fts-space-3);
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-secondary);
  background: transparent;
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-full);
  cursor: pointer;
  transition: all 0.15s ease;

  &:hover:not(&--active) { border-color: var(--fts-border-hover); }

  &--active {
    border-color: var(--fts-primary);
    background: rgba(var(--fts-primary-rgb), 0.06);
    color: var(--fts-primary);
    font-weight: 600;
  }
}

// 自定义 Toggle 开关
.toggle-switch {
  position: relative;
  display: inline-block;
  width: 44px;
  height: 24px;
  cursor: pointer;
}

.toggle-input {
  opacity: 0;
  width: 0;
  height: 0;
  position: absolute;

  &:checked + .toggle-track {
    background-color: var(--fts-primary);

    &::after {
      transform: translateX(20px);
    }
  }
}

.toggle-track {
  position: absolute;
  inset: 0;
  background-color: var(--fts-bg-tertiary);
  border: 1.5px solid var(--fts-border-primary);
  border-radius: 12px;
  transition: all 0.2s ease;

  &::after {
    content: '';
    position: absolute;
    top: 2px;
    left: 2px;
    width: 18px;
    height: 18px;
    border-radius: 50%;
    background-color: var(--fts-text-quaternary);
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
    transition: transform 0.2s ease;
  }
}
</style>
