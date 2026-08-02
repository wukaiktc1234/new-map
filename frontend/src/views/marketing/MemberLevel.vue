<script setup lang="ts">
/**
 * 会员等级管理页面
 * 等级体系完全自定义：可新增、编辑、删除等级。等级只升不降，积分年度重置但等级永久保留。
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { InfoFilled, Plus, Delete } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { memberLevelApi } from '@/api/marketing'
import type {
  MemberLevelInfo, MemberLevelForm, LevelCriteria, LevelBenefit,
  PointsResetConfig, CriteriaType, CriteriaOperator, BenefitType,
} from '@/types/member-level'
import {
  LevelCodeSuggestions, LevelColorSuggestions,
  CriteriaTypeOptions, CriteriaOperatorOptions, BenefitTypeOptions,
  PointsResetCycleOptions, DefaultPointsResetConfig,
} from '@/types/member-level'

const loading = ref(false)
const levelList = ref<MemberLevelInfo[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('新建等级')
const editingId = ref<string | null>(null)
const formRef = ref()
/** 升级消费金额（el-input-number 需要数字类型，表单提交时转为字符串） */
const minConsumptionNum = ref(0)
/** 积分重置配置对话框 */
const resetConfigVisible = ref(false)
const resetConfig = reactive<PointsResetConfig>({ ...DefaultPointsResetConfig })

/** 获取等级颜色：支持 CSS 变量、十六进制色值，以及旧式编码回退 */
function getLevelColor(color: string): string {
  if (!color) return 'var(--fts-primary)'
  if (color.startsWith('var(') || color.startsWith('#')) return color
  const map: Record<string, string> = {
    NORMAL: 'var(--fts-info)', SILVER: 'var(--fts-text-secondary)',
    GOLD: 'var(--fts-warning)', DIAMOND: 'var(--fts-primary)',
    BRONZE: '#CD7F32', PLATINUM: 'var(--fts-success)',
  }
  return map[color] || 'var(--fts-primary)'
}

/** 表单初始值 */
const defaultForm = (): MemberLevelForm => ({
  levelName: '', levelCode: '', levelColor: 'var(--fts-info)', levelIcon: 'star',
  minConsumption: '0', discountRate: 1.0, pointsRate: 1.0,
  birthdayDiscountRate: 0.95, birthdayBonusPoints: 0,
  benefitsDescription: '', sortOrder: 1, status: 'active',
  criteria: [], benefits: [],
})
const form = reactive<MemberLevelForm & { minConsumption: string }>(defaultForm())

const formRules = {
  levelName: [{ required: true, message: '请输入等级名称', trigger: 'blur' }],
  levelCode: [{ required: true, message: '请输入等级编码', trigger: 'blur' }],
  minConsumption: [{ required: true, message: '请输入升级消费金额', trigger: 'blur' }],
  discountRate: [{ required: true, message: '请输入折扣率', trigger: 'blur' }],
}

/** 按 sortOrder 排序的等级列表 */
const sortedLevels = computed(() => [...levelList.value].sort((a, b) => a.sortOrder - b.sortOrder))

/** 重置周期标签映射 */
const cycleLabelMap = computed<Record<string, string>>(() => {
  const map: Record<string, string> = {}
  PointsResetCycleOptions.forEach(item => { map[item.value] = item.label })
  return map
})
const resetRuleLabelMap: Record<string, string> = {
  all: '全部清零', keep_half: '保留一半', keep_rate_based: '按等级比例保留',
}

async function loadLevels() {
  loading.value = true
  try {
    levelList.value = await memberLevelApi.getList()
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载等级列表失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  dialogTitle.value = '新建等级'
  Object.assign(form, defaultForm())
  minConsumptionNum.value = 0
  dialogVisible.value = true
}

function openEdit(level: MemberLevelInfo) {
  editingId.value = level.levelId
  dialogTitle.value = '编辑等级'
  Object.assign(form, {
    levelName: level.levelName, levelCode: level.levelCode,
    levelColor: level.levelColor || '', levelIcon: level.levelIcon || 'star',
    minConsumption: level.minConsumption, discountRate: level.discountRate,
    pointsRate: level.pointsRate ?? 1.0, birthdayDiscountRate: level.birthdayDiscountRate,
    birthdayBonusPoints: level.birthdayBonusPoints ?? 0,
    benefitsDescription: level.benefitsDescription,
    sortOrder: level.sortOrder, status: level.status,
    criteria: level.criteria ? level.criteria.map(c => ({ ...c })) : [],
    benefits: level.benefits ? level.benefits.map(b => ({ ...b })) : [],
  })
  minConsumptionNum.value = parseFloat(level.minConsumption) || 0
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  form.minConsumption = String(minConsumptionNum.value)
  try {
    if (editingId.value) {
      await memberLevelApi.update(editingId.value, { ...form })
      ElMessage.success('等级更新成功')
    } else {
      await memberLevelApi.create({ ...form })
      ElMessage.success('等级创建成功')
    }
    dialogVisible.value = false
    await loadLevels()
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '操作失败')
  }
}

// 高级配置：升级条件 / 权益 行操作
function addCriteria() {
  form.criteria.push({
    criteriaId: String(Date.now()), levelId: editingId.value || '',
    criteriaType: 'total_consumption' as CriteriaType, criteriaValue: '',
    operator: 'GTE' as CriteriaOperator, periodDays: 0, logicGroup: 1,
    sortOrder: form.criteria.length + 1, status: 'active',
  })
}
function removeCriteria(idx: number) { form.criteria.splice(idx, 1) }
function addBenefit() {
  form.benefits.push({
    benefitId: String(Date.now()), levelId: editingId.value || '',
    benefitType: 'discount' as BenefitType, benefitName: '', benefitValue: '',
    benefitConfig: {}, sortOrder: form.benefits.length + 1, status: 'active',
  })
}
function removeBenefit(idx: number) { form.benefits.splice(idx, 1) }

async function toggleStatus(level: MemberLevelInfo) {
  const action = level.status === 'active' ? '停用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${action}等级「${level.levelName}」吗？`, '确认操作',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    await memberLevelApi.toggleStatus(level.levelId)
    ElMessage.success(`已${action}等级「${level.levelName}」`)
    await loadLevels()
  } catch { /* 用户取消操作 */ }
}

async function deleteLevel(level: MemberLevelInfo) {
  if (level.memberCount > 0) {
    ElMessage.warning(`等级「${level.levelName}」下有 ${level.memberCount} 位会员，无法删除`)
    return
  }
  try {
    await ElMessageBox.confirm(`确定要删除等级「${level.levelName}」吗？删除后不可恢复。`, '确认删除',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' })
    await memberLevelApi.delete(level.levelId)
    ElMessage.success(`已删除等级「${level.levelName}」`)
    await loadLevels()
  } catch { /* 用户取消操作 */ }
}

// 积分重置规则
function openResetConfig() {
  Object.assign(resetConfig, DefaultPointsResetConfig)
  resetConfigVisible.value = true
}
function saveResetConfig() {
  // TODO: 积分重置规则保存接口待后端实现，当前显示功能开发中，禁止假成功
  ElMessage.info('积分重置规则保存功能开发中')
  resetConfigVisible.value = false
}

/** 格式化折扣率：0.95 → "9.5折" */
function formatDiscount(rate: number): string { return `${(rate * 10).toFixed(1)}折` }
/** 格式化金额显示 */
function formatConsumption(val: string): string {
  const num = parseFloat(val)
  if (isNaN(num) || num === 0) return '0'
  return num.toLocaleString('zh-CN')
}

onMounted(() => { loadLevels() })
</script>

<template>
  <div class="modern-page">
    <PageHeader title="会员等级" description="等级体系与升级条件配置">
      <template #extra>
        <el-button type="primary" @click="openCreate">新建等级</el-button>
      </template>
    </PageHeader>

    <!-- 升级说明提示 -->
    <div class="level-notice">
      <el-icon :size="16"><InfoFilled /></el-icon>
      <span>等级体系完全自定义：可新增、编辑、删除等级。等级只升不降，积分年度重置但等级永久保留。</span>
    </div>

    <!-- 等级卡片区域 -->
    <div v-loading="loading" class="level-cards">
      <div v-for="level in sortedLevels" :key="level.levelId" class="level-card">
        <div class="level-card__ribbon" :style="{ backgroundColor: getLevelColor(level.levelColor || level.levelCode) }" />
        <div class="level-card__header">
          <span class="level-card__indicator" :style="{ backgroundColor: getLevelColor(level.levelColor || level.levelCode) }" />
          <span class="level-card__name">{{ level.levelName }}</span>
          <span class="level-card__code-badge" :style="{ color: getLevelColor(level.levelColor || level.levelCode), borderColor: getLevelColor(level.levelColor || level.levelCode) }">{{ level.levelCode }}</span>
          <StatusTag :status="level.status === 'active' ? 'active' : 'inactive'" :label="level.status === 'active' ? '启用' : '停用'" size="small" />
        </div>
        <div class="level-card__metrics">
          <div class="level-card__metric"><span class="level-card__label">升级消费金额</span><span class="level-card__value">¥{{ formatConsumption(level.minConsumption) }}</span></div>
          <div class="level-card__metric"><span class="level-card__label">折扣率</span><span class="level-card__value">{{ formatDiscount(level.discountRate) }}</span></div>
          <div class="level-card__metric"><span class="level-card__label">积分倍率</span><span class="level-card__value">{{ (level.pointsRate ?? 1).toFixed(1) }}x</span></div>
          <div class="level-card__metric"><span class="level-card__label">生日权益</span><span class="level-card__value">{{ formatDiscount(level.birthdayDiscountRate) }}<template v-if="level.birthdayBonusPoints > 0"> + {{ level.birthdayBonusPoints }}分</template></span></div>
        </div>
        <div v-if="level.benefitsDescription" class="level-card__benefits">{{ level.benefitsDescription }}</div>
        <div class="level-card__count">{{ level.memberCount }} 位会员</div>
        <div class="level-card__actions">
          <el-button link type="primary" size="small" @click="openEdit(level)">编辑</el-button>
          <el-button link :type="level.status === 'active' ? 'danger' : 'primary'" size="small" @click="toggleStatus(level)">{{ level.status === 'active' ? '停用' : '启用' }}</el-button>
          <el-button link type="danger" size="small" @click="deleteLevel(level)">删除</el-button>
        </div>
      </div>
    </div>

    <!-- 积分重置规则 -->
    <el-card class="reset-card" shadow="never">
      <template #header>
        <div class="reset-card__header">
          <span class="reset-card__title">积分重置规则</span>
          <el-button link type="primary" size="small" @click="openResetConfig">编辑规则</el-button>
        </div>
      </template>
      <div class="reset-card__content">
        <div class="reset-card__item"><span class="reset-card__label">重置周期：</span><span>{{ cycleLabelMap[resetConfig.cycle] || resetConfig.cycle }}</span></div>
        <div class="reset-card__item"><span class="reset-card__label">重置前提醒：</span><span>{{ resetConfig.reminderDays }} 天（{{ resetConfig.reminderChannels.join('、') }}）</span></div>
        <div class="reset-card__item"><span class="reset-card__label">重置规则：</span><span>{{ resetRuleLabelMap[resetConfig.resetRule] || resetConfig.resetRule }}</span></div>
        <div class="reset-card__item"><span class="reset-card__label">等级是否重置：</span><span>{{ resetConfig.resetLevel ? '是' : '否（只升不降）' }}</span></div>
      </div>
    </el-card>

    <!-- 新建/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="680px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="120px" label-position="right">
        <el-form-item label="等级名称" prop="levelName">
          <el-input v-model="form.levelName" placeholder="请输入等级名称" maxlength="20" />
        </el-form-item>
        <el-form-item label="等级编码" prop="levelCode">
          <el-input v-model="form.levelCode" placeholder="请输入等级编码，如 VIP1、BRONZE 等" maxlength="20" />
          <div class="form-help-text">常用编码：
            <el-button v-for="sug in LevelCodeSuggestions" :key="sug.label" link type="primary" size="small" @click="form.levelCode = sug.label">{{ sug.label }}({{ sug.description }})</el-button>
          </div>
        </el-form-item>
        <el-form-item label="等级颜色">
          <el-select v-model="form.levelColor" :teleported="false" placeholder="请选择等级颜色" style="width: 100%">
            <el-option v-for="color in LevelColorSuggestions" :key="color.label" :label="color.label" :value="color.value">
              <span class="color-option"><span class="color-option__dot" :style="{ backgroundColor: color.value || 'var(--fts-border)' }" />{{ color.label }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="升级消费金额" prop="minConsumption">
          <el-input-number v-model="minConsumptionNum" :min="0" :step="100" :precision="0" controls-position="right" style="width: 100%" />
          <div class="form-help-text">会员累计消费达到此金额后自动升级</div>
        </el-form-item>
        <el-form-item label="折扣率" prop="discountRate">
          <el-input-number v-model="form.discountRate" :min="0.5" :max="1" :step="0.05" :precision="2" controls-position="right" style="width: 100%" />
          <div class="form-help-text">1.0=无折扣，0.95=95折，0.9=9折</div>
        </el-form-item>
        <el-form-item label="积分倍率">
          <el-input-number v-model="form.pointsRate" :min="1" :max="5" :step="0.1" :precision="1" controls-position="right" style="width: 100%" />
          <div class="form-help-text">1.0=标准，1.5=1.5倍积分</div>
        </el-form-item>
        <el-form-item label="生日折扣率">
          <el-input-number v-model="form.birthdayDiscountRate" :min="0.5" :max="1" :step="0.05" :precision="2" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="生日额外积分">
          <el-input-number v-model="form.birthdayBonusPoints" :min="0" :step="100" :precision="0" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="权益说明">
          <el-input v-model="form.benefitsDescription" type="textarea" :rows="3" placeholder="请输入等级权益说明" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="排序序号">
          <el-input-number v-model="form.sortOrder" :min="1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" active-value="active" inactive-value="inactive" active-text="启用" inactive-text="停用" />
        </el-form-item>
        <!-- 高级配置 -->
        <el-collapse class="advanced-config">
          <el-collapse-item title="高级配置（可选）" name="advanced">
            <div class="advanced-section">
              <div class="advanced-section__header">
                <span class="advanced-section__title">升级条件配置</span>
                <el-button link type="primary" size="small" @click="addCriteria"><el-icon><Plus /></el-icon>添加条件</el-button>
              </div>
              <div v-if="form.criteria.length === 0" class="advanced-section__empty">暂无升级条件，将使用上方"升级消费金额"</div>
              <div v-for="(item, idx) in form.criteria" :key="item.criteriaId" class="advanced-row">
                <el-select v-model="item.criteriaType" :teleported="false" placeholder="条件类型" style="width: 140px">
                  <el-option v-for="opt in CriteriaTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
                </el-select>
                <el-select v-model="item.operator" :teleported="false" placeholder="运算符" style="width: 110px">
                  <el-option v-for="opt in CriteriaOperatorOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
                </el-select>
                <el-input v-model="item.criteriaValue" placeholder="条件值" style="flex: 1" />
                <el-button link type="danger" size="small" @click="removeCriteria(idx)"><el-icon><Delete /></el-icon></el-button>
              </div>
            </div>
            <div class="advanced-section">
              <div class="advanced-section__header">
                <span class="advanced-section__title">权益配置</span>
                <el-button link type="primary" size="small" @click="addBenefit"><el-icon><Plus /></el-icon>添加权益</el-button>
              </div>
              <div v-if="form.benefits.length === 0" class="advanced-section__empty">暂无权益配置，将使用上方快捷字段</div>
              <div v-for="(item, idx) in form.benefits" :key="item.benefitId" class="advanced-row">
                <el-select v-model="item.benefitType" :teleported="false" placeholder="权益类型" style="width: 140px">
                  <el-option v-for="opt in BenefitTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
                </el-select>
                <el-input v-model="item.benefitName" placeholder="权益名称" style="flex: 1" />
                <el-input v-model="item.benefitValue" placeholder="权益值" style="width: 120px" />
                <el-button link type="danger" size="small" @click="removeBenefit(idx)"><el-icon><Delete /></el-icon></el-button>
              </div>
            </div>
          </el-collapse-item>
        </el-collapse>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 积分重置规则对话框 -->
    <el-dialog v-model="resetConfigVisible" title="积分重置规则" width="560px" destroy-on-close>
      <el-form label-width="140px" label-position="right">
        <el-form-item label="重置周期">
          <el-select v-model="resetConfig.cycle" :teleported="false" style="width: 100%">
            <el-option v-for="opt in PointsResetCycleOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="重置前提醒天数">
          <el-input-number v-model="resetConfig.reminderDays" :min="0" :max="90" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="提醒方式">
          <el-checkbox-group v-model="resetConfig.reminderChannels">
            <el-checkbox value="sms">短信</el-checkbox>
            <el-checkbox value="wechat">微信</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="重置规则">
          <el-radio-group v-model="resetConfig.resetRule">
            <el-radio value="all">全部清零</el-radio>
            <el-radio value="keep_half">保留一半</el-radio>
            <el-radio value="keep_rate_based">按等级比例保留</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="等级是否重置">
          <el-switch v-model="resetConfig.resetLevel" active-text="开启" inactive-text="关闭" />
          <div v-if="resetConfig.resetLevel" class="form-help-text form-help-text--warning">开启后等级也会重置，建议保持关闭</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetConfigVisible = false">取消</el-button>
        <el-button type="primary" @click="saveResetConfig">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.level-notice {
  display: flex; align-items: center; gap: var(--fts-space-2);
  padding: var(--fts-space-3) var(--fts-space-4);
  margin: var(--fts-space-4) var(--fts-space-6);
  background: var(--fts-info-light, rgba(144,147,153,0.08));
  border-radius: var(--fts-radius-md, 8px);
  font-size: var(--fts-font-size-sm);
  color: var(--fts-info);
}
.level-cards {
  display: flex; flex-wrap: wrap; gap: var(--fts-space-5);
  padding: var(--fts-space-4) var(--fts-space-6) var(--fts-space-6);
}
.level-card {
  position: relative; flex: 1 1 280px; max-width: 340px;
  display: flex; flex-direction: column;
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-lg, 12px);
  overflow: hidden;
  transition: box-shadow var(--fts-duration-fast, 150ms) ease, transform var(--fts-duration-fast, 150ms) ease;
  &:hover { box-shadow: var(--fts-shadow-md, 0 4px 12px rgba(0,0,0,0.1)); transform: translateY(-2px); }
  &__ribbon { height: 4px; width: 100%; }
  &__header { display: flex; align-items: center; gap: var(--fts-space-2); padding: var(--fts-space-4) var(--fts-space-4) 0; flex-wrap: wrap; }
  &__indicator { width: 12px; height: 12px; border-radius: 50%; flex-shrink: 0; }
  &__name { font-size: var(--fts-font-size-lg, 18px); font-weight: var(--fts-font-weight-bold, 600); color: var(--fts-text-primary); }
  &__code-badge { padding: 2px 8px; font-size: var(--fts-font-size-xs, 12px); border: 1px solid currentColor; border-radius: var(--fts-radius-sm, 4px); letter-spacing: 1px; }
  &__metrics { display: grid; grid-template-columns: 1fr 1fr; gap: var(--fts-space-2) var(--fts-space-3); padding: var(--fts-space-3) var(--fts-space-4); }
  &__metric { display: flex; flex-direction: column; gap: 2px; }
  &__benefits { padding: var(--fts-space-2) var(--fts-space-4); font-size: var(--fts-font-size-sm); color: var(--fts-text-secondary); line-height: 1.5; }
  &__count { padding: var(--fts-space-2) var(--fts-space-4); font-size: var(--fts-font-size-sm); color: var(--fts-text-secondary); }
  &__actions { display: flex; gap: var(--fts-space-2); padding: var(--fts-space-3) var(--fts-space-4) var(--fts-space-4); margin-top: auto; border-top: 1px solid var(--fts-border-secondary); }
  &__label { font-size: var(--fts-font-size-xs, 12px); color: var(--fts-text-tertiary, #999); }
  &__value { font-size: var(--fts-font-size-sm); color: var(--fts-text-primary); font-weight: var(--fts-font-weight-medium, 500); }
}
.reset-card {
  margin: 0 var(--fts-space-6) var(--fts-space-6);
  &__header { display: flex; justify-content: space-between; align-items: center; }
  &__title { font-size: var(--fts-font-size-base); font-weight: var(--fts-font-weight-bold, 600); color: var(--fts-text-primary); }
  &__content { display: grid; grid-template-columns: 1fr 1fr; gap: var(--fts-space-3); }
  &__item { display: flex; gap: var(--fts-space-2); font-size: var(--fts-font-size-sm); }
  &__label { color: var(--fts-text-tertiary, #999); flex-shrink: 0; }
}
.form-help-text {
  font-size: var(--fts-font-size-xs, 12px); color: var(--fts-text-tertiary, #999);
  line-height: 1.5; margin-top: 4px;
  &--warning { color: var(--fts-warning); }
}
.color-option {
  display: flex; align-items: center; gap: var(--fts-space-2);
  &__dot { width: 14px; height: 14px; border-radius: 50%; border: 1px solid var(--fts-border); flex-shrink: 0; }
}
.advanced-config { margin-top: var(--fts-space-4); border-top: 1px solid var(--fts-border-secondary); }
.advanced-section {
  margin-bottom: var(--fts-space-3);
  &__header { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--fts-space-2); }
  &__title { font-size: var(--fts-font-size-sm); font-weight: var(--fts-font-weight-medium, 500); color: var(--fts-text-primary); }
  &__empty { padding: var(--fts-space-3); font-size: var(--fts-font-size-xs, 12px); color: var(--fts-text-tertiary); text-align: center; background: var(--fts-bg-secondary, rgba(0,0,0,0.02)); border-radius: var(--fts-radius-sm, 4px); }
}
.advanced-row { display: flex; align-items: center; gap: var(--fts-space-2); margin-bottom: var(--fts-space-2); }
</style>
