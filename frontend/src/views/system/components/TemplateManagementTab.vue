<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Setting, Select, InfoFilled, Loading, House, School, OfficeBuilding, Flag } from '@element-plus/icons-vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { usePermissionStore } from '@/stores/permission'

/**
 * 权限中心 4 种模式 - TemplateManagementTab
 *
 * 数据来源：后端 /v1/permission-templates/system (PermissionTemplateController)
 * - 4 种系统模式：centralized-single / standard-chain / large-chain / custom
 * - 后端返回 roleConfig JSON 字符串，由前端 Store 解析为 domainMatrix
 *
 * 前端补充字段（scenario / features）：作为 UI 展示元数据，根据 templateCode 派生
 * 这些字段不存储在后端，因为它们仅用于前端展示，与业务逻辑无关
 */

interface PermissionTemplate {
  templateCode: string
  templateName: string
  description: string
  scenario: string
  features: string[]
  domainMatrix: Record<string, string[]>
}

/**
 * 前端元数据映射表：根据 templateCode 派生 scenario + features
 * 这些字段是 UI 展示用的描述性元数据，不参与业务逻辑
 */
const TEMPLATE_META: Record<string, { scenario: string; features: string[] }> = {
  'centralized-single': {
    scenario: '老板兼厨师兼采购，极简运营，聚焦进货与临期管理',
    features: [
      '聚焦核心营业：门店运营、产品、订单、运营',
      '简化后台管理：隐藏采购/仓储/财务/HR/资产',
      '保留食品溯源与设备监测',
      '5-50人团队，年营收50万-500万',
    ],
  },
  'standard-chain': {
    scenario: '门店半自治，采购和财务由区域/总部集中管控',
    features: [
      '全业务域开放（14个域）',
      '支持2-5家门店跨店管理',
      '财务/HR总监权限受限（仅本域）',
      '运营总监管理采购+仓储',
    ],
  },
  'large-chain': {
    scenario: '总部（即店主）管控一切，店长仅负责日常运营执行',
    features: [
      '全业务域开放（14个域）',
      '多门店大型连锁，年营收2000万+',
      '财务总监兼管资产',
      '运营总监扩展管理会员+溯源',
      '所有角色权限完整',
    ],
  },
  'custom': {
    scenario: '从零开始，不预设任何域权限',
    features: [
      'admin 全开，其他角色由用户自行编辑',
      '完全自由的权限组合',
      '支持精细化的角色×域权限配置',
      '适合特殊业务场景',
    ],
  },
}

const permissionStore = usePermissionStore()

const loading = ref(false)
const selectedTemplate = ref('')
const previewVisible = ref(false)
const applyConfirmVisible = ref(false)
const previewTemplate = ref<PermissionTemplate | null>(null)

/** 当前激活的模板code（直接使用Store的currentTemplate） */
const currentTemplateCode = computed(() => permissionStore.currentTemplate)

/**
 * 完整模板列表：合并后端数据 + 前端元数据
 * 后端 permissionTemplates: { templateCode, templateName, description, domainMatrix }
 * 前端补充：scenario, features
 */
const templateList = computed<PermissionTemplate[]>(() => {
  return permissionStore.permissionTemplates.map(tpl => {
    const meta = TEMPLATE_META[tpl.templateCode] || {
      scenario: '通用模式',
      features: [],
    }
    return {
      templateCode: tpl.templateCode,
      templateName: tpl.templateName,
      description: tpl.description,
      scenario: meta.scenario,
      features: meta.features,
      domainMatrix: tpl.domainMatrix,
    }
  })
})

/** 带激活状态的模板列表（用于 UI 渲染） */
const templateListWithStatus = computed(() => templateList.value.map(t => ({
  ...t,
  isActive: t.templateCode === currentTemplateCode.value,
  roleCount: Object.keys(t.domainMatrix).length,
})))

function getTemplateByCode(code: string): PermissionTemplate | undefined {
  return templateList.value.find(t => t.templateCode === code)
}

/** 异步加载模板数据 */
async function loadTemplates(): Promise<void> {
  await permissionStore.fetchPermissionTemplates()
}

onMounted(() => {
  loadTemplates().catch(err => {
    console.error('[TemplateManagement] 加载模板失败:', err)
  })
})

function handleApply(templateCode: string) {
  const template = getTemplateByCode(templateCode)
  if (!template) {
    ElMessage.warning('模板数据加载中，请稍后重试')
    return
  }

  selectedTemplate.value = templateCode
  applyConfirmVisible.value = true
}

async function confirmApply() {
  const template = getTemplateByCode(selectedTemplate.value)
  if (!template) {
    ElMessage.error('模板不存在')
    return
  }

  loading.value = true
  try {
    permissionStore.applyTemplate(selectedTemplate.value)
    permissionStore.getVisibleMenus()

    if (selectedTemplate.value === 'custom') {
      permissionStore.suggestTab('domains')
      ElMessage.success({
        message: '已切换为「完全自定义」模式，请前往「域权限配置」标签页编辑权限矩阵',
        duration: 5000,
        showClose: true,
      })
    } else {
      ElMessage.success(`已成功应用「${template.templateName}」模板，菜单已刷新`)
    }
    applyConfirmVisible.value = false
  } catch (storeError: unknown) {
    console.error('[Template] Store层应用失败:', storeError)
    ElMessage.error('应用模板失败，请重试')
  } finally {
    loading.value = false
  }
}

function showPreview(template: PermissionTemplate) {
  previewTemplate.value = template
  previewVisible.value = true
}

/**
 * 跳转到域权限配置编辑器（完全自定义模板的核心入口）
 */
function goToDomainEditor(template: PermissionTemplate): void {
  permissionStore.suggestTab('domains')
  ElMessage.info({
    message: `已跳转到「域权限配置」—— 在此编辑「${template.templateName}」的 ${Object.keys(template.domainMatrix || {}).length} 个角色 × 14 个业务域的完整权限矩阵`,
    duration: 5000,
    showClose: true,
  })
}

function getScenarioColor(scenario: string): string {
  const colorMap: Record<string, string> = {
    '老板兼厨师兼采购，极简运营，聚焦进货与临期管理': 'error',
    '门店高度自治，类似准独立法人': 'primary',
    '门店半自治，采购和财务由区域/总部集中管控': 'success',
    '总部（即店主）管控一切，店长仅负责日常运营执行': 'info',
    '从零开始，不预设任何域权限': 'warning',
  }
  return colorMap[scenario] || 'info'
}

const roleDisplayNames: Record<string, string> = {
  admin: '超级管理员',
  owner: '老板',
  ops_director: '运营总监',
  finance_director: '财务总监',
  hr_director: 'HR总监',
  store_manager: '店长',
  team_leader: '组长',
  employee: '普通员工',
}

function getRoleDisplayName(roleCode: string): string {
  return roleDisplayNames[roleCode] || roleCode
}

const readonlyDomainsForOps = ['product', 'order', 'purchase', 'warehouse', 'finance', 'member', 'traceability']

function getPreviewAccessLevel(role: string, domain: string, domains: string[]): string {
  const hasAccess = domains.includes(domain)

  if (!hasAccess) return 'HIDDEN'

  if (role === 'ops_director' && readonlyDomainsForOps.includes(domain)) {
    const templateCode = previewTemplate.value?.templateCode
    if (templateCode === 'centralized-single' || templateCode === 'standard-chain') {
      return 'READ_ONLY'
    }
  }

  if (role === 'employee' && domain === 'store-management') return 'LIMITED'
  if (role === 'finance_director' && domain === 'order') return 'LIMITED'
  if (role === 'hr_director' && domain === 'finance') return 'LIMITED'

  return 'FULL'
}

function getPreviewCellValue(role: string, domain: string, domains: string[]): string {
  const level = getPreviewAccessLevel(role, domain, domains)
  switch (level) {
    case 'FULL': return '✓全'
    case 'READ_ONLY': return '👁读'
    case 'LIMITED': return '◐限'
    default: return '—'
  }
}

function getPreviewCellClass(role: string, domain: string, domains: string[]): Record<string, boolean> {
  const level = getPreviewAccessLevel(role, domain, domains)
  return {
    [`cell-${level.toLowerCase()}`]: true,
  }
}

/** 动态域列表：从Store注册的菜单组生成 */
const allDomainCodes = computed(() => {
  return permissionStore.registeredMenus
    .map(m => m.id)
    .sort((a, b) => {
      const orderA = permissionStore.registeredMenus.find(x => x.id === a)?.order ?? 999
      const orderB = permissionStore.registeredMenus.find(x => x.id === b)?.order ?? 999
      return orderA - orderB
    })
})

function getDomainDisplayName(domainCode: string): string {
  const menu = permissionStore.registeredMenus.find(m => m.id === domainCode)
  return menu?.title || domainCode
}

function getDomainDisplayLabel(domainCode: string): string {
  const cn = getDomainDisplayName(domainCode)
  return cn !== domainCode ? `${cn}\n${domainCode}` : domainCode
}

/**
 * 根据模板编码返回对应图标组件
 */
function getTemplateIcon(templateCode: string) {
  const iconMap: Record<string, typeof House> = {
    'centralized-single': House,
    'standard-chain': School,
    'large-chain': OfficeBuilding,
    'custom': Flag,
  }
  return iconMap[templateCode] || Setting
}
</script>

<template>
  <div class="template-management">
    <div class="section-header">
      <div class="section-desc">预设权限模板，快速适配不同规模企业的组织架构</div>
      <StatusTag
        v-if="currentTemplateCode"
        :status="'success'"
        :label="`当前：${getTemplateByCode(currentTemplateCode)?.templateName || '未知'}`"
        size="medium"
      />
    </div>

    <div v-if="permissionStore.templatesLoading" class="loading-state">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      <span>正在加载权限模板...</span>
    </div>

    <div v-else-if="templateListWithStatus.length === 0" class="empty-state">
      <el-icon :size="48"><InfoFilled /></el-icon>
      <p>暂无可用的权限模板</p>
      <el-button type="primary" size="small" @click="loadTemplates">重新加载</el-button>
    </div>

    <div v-else class="template-grid">
      <div
        v-for="template in templateListWithStatus"
        :key="template.templateCode"
        :class="['template-card', { 'template-card--active': template.isActive }]"
      >
        <div class="card-header">
          <div class="card-icon">
            <el-icon :size="28">
              <component :is="getTemplateIcon(template.templateCode)" />
            </el-icon>
          </div>
          <div class="card-title-group">
            <h3 class="card-title">{{ template.templateName }}</h3>
            <span v-if="template.isActive" class="active-badge">当前使用</span>
          </div>
        </div>

        <p class="card-desc">{{ template.description }}</p>

        <div class="card-scenario">
          <span class="scenario-label">适用场景：</span>
          <span>{{ template.scenario }}</span>
        </div>

        <ul class="feature-list">
          <li v-for="(feature, idx) in template.features" :key="idx">
            <el-icon :size="14" color="var(--fts-success)"><Select /></el-icon>
            {{ feature }}
          </li>
        </ul>

        <div class="card-stats">
          <div class="stat-item">
            <span class="stat-value">{{ template.roleCount }}</span>
            <span class="stat-label">角色</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-value">{{ Object.values(template.domainMatrix).flat().length }}</span>
            <span class="stat-label">域分配</span>
          </div>
        </div>

        <div class="card-actions">
          <!-- 完全自定义模板：提供直达域权限编辑器的入口 -->
          <template v-if="template.templateCode === 'custom'">
            <el-button type="primary" size="small" @click="goToDomainEditor(template)">
              <el-icon><Setting /></el-icon>
              编辑权限矩阵
            </el-button>
            <el-button 
              size="small"
              :loading="selectedTemplate === template.templateCode"
              @click="handleApply(template.templateCode)"
            >
              应用并生效
            </el-button>
          </template>
          <!-- 其他预设模板：预览 + 应用 -->
          <template v-else>
            <el-button size="small" @click="showPreview(template)">预览矩阵</el-button>
            <el-button 
              v-if="!template.isActive" 
              type="primary" 
              size="small" 
              :loading="selectedTemplate === template.templateCode"
              @click="handleApply(template.templateCode)"
            >
              应用模板
            </el-button>
            <StatusTag v-else status="success" label="已应用" size="small" />
          </template>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="previewVisible"
      :title="`${previewTemplate?.templateName} - 权限矩阵预览`"
      width="92vw"
      :style="{ maxWidth: '1440px' }"
      top="2vh"
    >
      <div v-if="previewTemplate" class="matrix-preview">
        <div class="preview-legend">
          <span class="legend-item"><span class="dot dot-full"></span>完全(FULL)</span>
          <span class="legend-item"><span class="dot dot-readonly"></span>只读(RO)</span>
          <span class="legend-item"><span class="dot dot-limited"></span>受限(LIM)</span>
          <span class="legend-item"><span class="dot dot-hidden"></span>隐藏(—)</span>
        </div>
        <table class="preview-table">
          <thead>
            <tr>
              <th>角色</th>
              <th v-for="domain in allDomainCodes" :key="domain" class="domain-header-cell">
                <span class="domain-cn">{{ getDomainDisplayName(domain) }}</span>
                <span class="domain-en">{{ domain }}</span>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(domains, role) in previewTemplate.domainMatrix" :key="role">
              <td class="role-cell">{{ getRoleDisplayName(role) }}</td>
              <td 
                v-for="domain in allDomainCodes" 
                :key="`${role}-${domain}`"
                :class="getPreviewCellClass(role, domain, domains)"
              >
                {{ getPreviewCellValue(role, domain, domains) }}
              </td>
            </tr>
          </tbody>
        </table>
        <div class="preview-note" v-if="previewTemplate.templateCode === 'centralized-single' || previewTemplate.templateCode === 'standard-chain'">
          <el-icon><InfoFilled /></el-icon>
          此模板会自动将运营总监对 product/order/purchase/warehouse/finance/member/traceability 域设为<strong>只读监控(READ_ONLY)</strong>
        </div>
      </div>
    </el-dialog>

    <el-dialog
      v-model="applyConfirmVisible"
      title="确认应用模板"
      width="440px"
      :close-on-click-modal="false"
      append-to-body
      @closed="selectedTemplate = ''"
    >
      <div style="display: flex; gap: var(--fts-space-3); align-items: flex-start;">
        <el-icon :size="22" style="color: var(--fts-error); flex-shrink: 0; margin-top: 2px;">
          <InfoFilled />
        </el-icon>
        <div>
          <p style="color: var(--fts-text-secondary); margin: 0 0 var(--fts-space-2) 0; line-height: 1.6;">
            应用「<strong>{{ getTemplateByCode(selectedTemplate)?.templateName || '未知模板' }}</strong>」将<strong style="color: var(--fts-error);">重置所有角色的域权限配置</strong>。
          </p>
          <p style="color: var(--fts-error); margin: 0; font-size: var(--fts-font-size-sm);">
            此操作不可撤销，当前在「域权限配置」中的手动修改将全部丢失！
          </p>
        </div>
      </div>
      <template #footer>
        <el-button @click="applyConfirmVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="confirmApply">确认覆盖</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.template-management {
  width: 100%;
}

.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--fts-space-3);
  padding: var(--fts-space-8) var(--fts-space-4);
  color: var(--fts-text-tertiary);
  background: var(--fts-bg-card);
  border: 1px dashed var(--fts-border-secondary);
  border-radius: var(--fts-radius-lg);

  .el-icon.is-loading {
    color: var(--fts-primary);
  }

  p {
    margin: 0;
    font-size: var(--fts-font-size-sm);
  }
}

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

.template-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: var(--fts-space-4);
}

.template-card {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-lg);
  padding: var(--fts-space-5);
  transition: all 0.2s ease;

  &:hover {
    border-color: var(--fts-primary);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  }

  &--active {
    border-color: var(--fts-success);
    background: linear-gradient(135deg, rgba(var(--fts-success-rgb, 103, 194, 58), 0.03), transparent);
    
    .card-icon {
      background: rgba(var(--fts-success-rgb, 103, 194, 58), 0.1);
      color: var(--fts-success);
    }
  }
}

.card-header {
  display: flex;
  align-items: flex-start;
  gap: var(--fts-space-3);
  margin-bottom: var(--fts-space-3);
}

.card-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--fts-radius-md);
  background: var(--fts-bg-tertiary);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--fts-text-secondary);
  flex-shrink: 0;
}

.card-title-group {
  flex: 1;
}

.card-title {
  font-size: var(--fts-font-size-lg);
  font-weight: 600;
  color: var(--fts-text-primary);
  margin: 0;
}

.active-badge {
  display: inline-block;
  padding: 2px 8px;
  font-size: var(--fts-font-size-xs);
  background: var(--fts-success);
  color: var(--fts-text-inverse, #fff);
  border-radius: var(--fts-radius-full);
  margin-left: var(--fts-space-2);
}

.card-desc {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
  line-height: 1.6;
  margin-bottom: var(--fts-space-3);
}

.card-scenario {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
  padding: var(--fts-space-2) var(--fts-space-3);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-sm);
  margin-bottom: var(--fts-space-3);

  .scenario-label {
    color: var(--fts-text-secondary);
  }
}

.feature-list {
  list-style: none;
  padding: 0;
  margin: 0 0 var(--fts-space-4);

  li {
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-secondary);
    padding: var(--fts-space-1) 0;
  }
}

.card-stats {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--fts-space-4);
  padding: var(--fts-space-3) 0;
  border-top: 1px solid var(--fts-border-primary);
  border-bottom: 1px solid var(--fts-border-primary);
  margin-bottom: var(--fts-space-4);
}

.stat-item {
  text-align: center;

  .stat-value {
    display: block;
    font-size: var(--fts-font-size-xl);
    font-weight: 600;
    color: var(--fts-text-primary);
  }

  .stat-label {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
  }
}

.stat-divider {
  width: 1px;
  height: 32px;
  background: var(--fts-border-primary);
}

.card-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.matrix-preview {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3);
  overflow-x: auto;
  max-height: calc(80vh - 120px);
  overflow-y: auto;
}

.preview-legend {
  display: flex;
  gap: var(--fts-space-5);
  padding: var(--fts-space-2) var(--fts-space-3);
  margin-bottom: var(--fts-space-3);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-sm);
  flex-wrap: wrap;

  .legend-item {
    display: flex;
    align-items: center;
    gap: var(--fts-space-1);
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-secondary);

    .dot {
      width: 10px;
      height: 10px;
      border-radius: 3px;
      display: inline-block;
    }

    .dot-full { background: var(--fts-primary); }
    .dot-readonly { background: var(--fts-success); }
    .dot-limited { background: var(--fts-warning); }
    .dot-hidden { background: var(--fts-border-primary); }
  }
}

.preview-note {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  margin-top: var(--fts-space-3);
  padding: var(--fts-space-2) var(--fts-space-3);
  background: rgba(var(--fts-primary-rgb, 64, 158, 255), 0.06);
  border-radius: var(--fts-radius-sm);
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-secondary);

  strong {
    color: var(--fts-text-primary);
  }
}

.preview-table {
  width: 100%;
  border-collapse: collapse;
  font-size: var(--fts-font-size-xs);

  th, td {
    padding: 5px 6px;
    border: 1px solid var(--fts-border-secondary);
    text-align: center;
  }

  th {
    background: var(--fts-bg-tertiary);
    font-weight: 500;
    font-size: 10px;
  }

  .domain-header-cell {
    min-width: 64px;

    .domain-cn {
      display: block;
      font-size: var(--fts-font-size-xs);
      color: var(--fts-text-primary);
      line-height: 1.3;
    }

    .domain-en {
      display: block;
      font-size: 9px;
      color: var(--fts-text-quaternary);
      font-family: monospace;
      line-height: 1.1;
    }
  }

  .role-cell {
    text-align: left;
    font-weight: 500;
    background: var(--fts-bg-secondary);
    white-space: nowrap;
    width: 90px;
  }

  td:not(.role-cell) {
    min-width: 44px;
    font-weight: 500;
  }

  .cell-full {
    background: rgba(var(--fts-primary-rgb, 64, 158, 255), 0.12);
    color: var(--fts-primary);
  }

  .cell-read_only {
    background: rgba(var(--fts-success-rgb, 103, 194, 58), 0.1);
    color: var(--fts-success);
  }

  .cell-limited {
    background: rgba(var(--fts-warning-rgb, 230, 162, 60), 0.1);
    color: var(--fts-warning);
  }

  .cell-hidden {
    opacity: 0.35;
    color: var(--fts-text-quaternary);
  }
}
</style>
