<script setup lang="ts">
/**
 * 偏好设置对话框（收银端 · 个人 + 本机）
 *
 * 只承载展示层开关，全部本地生效并持久化（utils/usePosPreferences）：
 *   ① 深色模式            —— 复用全局 useTheme（父组件持有 isDark）
 *   ② 回车直接确认结账    —— 结算弹窗内的回车行为
 *   ③ 界面缩放（标准/大） —— 屏幕小、站立视距远时整体放大
 *   ④ 高危操作二次确认    —— 安全策略，恒开启且不可关闭（删除/清空/退款/退登）
 */
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { usePosPreferences, type FontScale } from '@/composables/usePosPreferences'

interface Props {
  modelValue: boolean
  isDark: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'toggleTheme'): void
}>()

const { prefs, setEnterConfirmCheckout, setFontScale, reset } = usePosPreferences()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

function handleFontScale(value: string | number | boolean | undefined): void {
  if (value !== 'large' && value !== 'normal') return
  setFontScale(value as FontScale)
  ElMessage.success(value === 'large' ? '界面已放大（刷新后保持）' : '界面已恢复标准大小')
}

/** 深色模式切换：交给父组件的 useTheme 落盘 + 改 documentElement[data-theme] */
function handleThemeToggle(): void {
  emit('toggleTheme')
  ElMessage.success(props.isDark ? '已切换到浅色模式' : '已切换到深色模式')
}

/** el-switch 的 change 载荷是 string|number|boolean，这里统一收敛为 boolean */
function handleEnterToggle(value: string | number | boolean | undefined): void {
  setEnterConfirmCheckout(Boolean(value))
}

function handleReset(): void {
  reset()
  // 深色模式归位由父组件的主题状态决定，这里只在需要时切换一次
  if (props.isDark) emit('toggleTheme')
  ElMessage.success('已恢复默认设置')
}
</script>

<template>
  <el-dialog
    v-model="visible"
    title="偏好设置"
    width="520px"
    :close-on-click-modal="false"
    :append-to-body="true"
    data-test="preference-dialog"
  >
    <div class="pref">
      <div class="pref__row">
        <div class="pref__label">
          <span class="pref__name">深色模式</span>
          <span class="pref__desc">夜间档口 / 弱光环境下降低眩光</span>
        </div>
        <el-switch
          :model-value="isDark"
          data-test="pref-dark-switch"
          @change="handleThemeToggle"
        />
      </div>

      <div class="pref__row">
        <div class="pref__label">
          <span class="pref__name">回车直接确认结账</span>
          <span class="pref__desc">关闭后需鼠标点「确认收款」，适合新收银员</span>
        </div>
        <el-switch
          :model-value="prefs.enterConfirmCheckout"
          data-test="pref-enter-switch"
          @change="handleEnterToggle"
        />
      </div>

      <div class="pref__row">
        <div class="pref__label">
          <span class="pref__name">界面缩放</span>
          <span class="pref__desc">「大」档整体放大 10%，触控热区同步变大（布局自适应不溢出）</span>
        </div>
        <el-radio-group
          :model-value="prefs.fontScale"
          data-test="pref-font-scale"
          @change="handleFontScale"
        >
          <el-radio-button value="normal">标准</el-radio-button>
          <el-radio-button value="large">大</el-radio-button>
        </el-radio-group>
      </div>

      <div class="pref__row">
        <div class="pref__label">
          <span class="pref__name">高危操作二次确认</span>
          <span class="pref__desc">清空购物车 / 删除单行 / 退款 / 退出登录必确认（安全策略，不可关闭）</span>
        </div>
        <el-switch :model-value="prefs.confirmDestructive" disabled data-test="pref-confirm-switch" />
      </div>
    </div>

    <template #footer>
      <el-button data-test="pref-reset" @click="handleReset">恢复默认</el-button>
      <el-button type="primary" @click="visible = false">完成</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.pref {
  display: flex;
  flex-direction: column;
  gap: 0;
}

/* 行距与左右留白按 8 的倍数：说明与控件之间固定 32px，垂直 16/16 */
.pref__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--pos-spacing-lg, 32px);
  min-height: 64px;
  padding: var(--pos-spacing-sm, 16px) 0;
  border-bottom: 1px solid var(--pos-border-light, #f1f5f9);
}

.pref__row:last-child {
  border-bottom: none;
}

.pref__label {
  display: flex;
  flex-direction: column;
  gap: var(--pos-spacing-xxs, 4px);
  min-width: 0;
  padding-right: var(--pos-spacing-sm, 16px);
}

/* 右侧控件区固定不压缩，避免被长说明挤压 */
.pref__row > :last-child {
  flex-shrink: 0;
}

.pref__name {
  font-size: 14.5px;
  font-weight: 600;
  color: var(--pos-text-primary, #1e293b);
}

.pref__desc {
  font-size: 12px;
  color: var(--pos-text-muted, #94a3b8);
}
</style>
