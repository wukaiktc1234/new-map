<script setup lang="ts">
// 【保留 · 暂未接入路由】P1-APK-DEVICE-001 待接线：STATUS.md 明确要补 router/index.ts 路由 + SecurityConfig permitAll，
// 当前 frontend/src 内无任何引用（不走路由、无组件导入），保留勿删。
/**
 * APK 设备激活页
 * 未白名单的设备在原生 App 内打开时，强制进入本页输入激活码
 */
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { deviceRegistrationApi } from '@/api/device-registration'
import { getDeviceId } from '@/utils/device-id'

const router = useRouter()

const code = ref('')
const loading = ref(false)
const activating = ref(false)
const deviceStatus = ref('')

async function checkStatus(): Promise<void> {
  try {
    deviceStatus.value = await deviceRegistrationApi.getStatus(await getDeviceId())
  } catch {
    deviceStatus.value = 'UNKNOWN'
  }
}

void checkStatus()

async function handleActivate(): Promise<void> {
  const trimmed = code.value.trim().toUpperCase()
  if (!trimmed) {
    ElMessage.warning('请输入激活码')
    return
  }
  if (!/^[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{8}$/.test(trimmed)) {
    ElMessage.warning('激活码格式不正确（应为 8 位字母数字）')
    return
  }
  activating.value = true
  try {
    await deviceRegistrationApi.activate(trimmed, await getDeviceId(), '收货验收 App')
    sessionStorage.setItem('fts_device_active', '1')
    ElMessage.success('激活成功，正在进入系统…')
    setTimeout(() => {
      void router.push('/login')
    }, 800)
  } catch (err) {
    const msg = (err as { message?: string })?.message || ''
    ElMessage.error(msg.includes('激活') || msg.includes('过期') || msg.includes('禁用') ? msg : `激活失败：${msg || '请稍后重试'}`)
  } finally {
    activating.value = false
  }
}
</script>

<template>
  <div class="activation-page">
    <div class="activation-card">
      <div class="brand-icon">✓</div>
      <h2>设备激活</h2>
      <p class="sub">
        当前设备未加入使用白名单。<br />
        请联系管理员获取激活码后输入。
      </p>
      <p v-if="deviceStatus === 'DISABLED'" class="disabled-tip">该设备已被禁用，请联系管理员</p>

      <el-input
        v-model="code"
        placeholder="输入 8 位激活码"
        maxlength="8"
        size="large"
        style="text-transform: uppercase"
        @keyup.enter="handleActivate"
      />
      <el-button
        type="primary"
        size="large"
        :loading="activating"
        class="activate-btn"
        @click="handleActivate"
      >
        激活设备
      </el-button>

      <p class="foot">白名单设备限制仅在公司内部使用</p>
    </div>
  </div>
</template>

<style scoped lang="scss">
.activation-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: linear-gradient(160deg, #1a5fb4 0%, #123a6b 100%);
}

.activation-card {
  width: 100%;
  max-width: 400px;
  background: #fff;
  border-radius: 16px;
  padding: 40px 32px;
  text-align: center;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.25);
}

.brand-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  border-radius: 50%;
  background: #1a5fb4;
  color: #fff;
  font-size: 30px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

h2 {
  font-size: 20px;
  margin-bottom: 8px;
  color: #1d2733;
}

.sub {
  color: #6b7a88;
  font-size: 13px;
  line-height: 1.7;
  margin-bottom: 20px;
}

.disabled-tip {
  color: #c0392b;
  font-size: 13px;
  margin-bottom: 12px;
}

.activate-btn {
  width: 100%;
  margin-top: 16px;
}

.foot {
  margin-top: 20px;
  font-size: 12px;
  color: #98a6b3;
}
</style>
