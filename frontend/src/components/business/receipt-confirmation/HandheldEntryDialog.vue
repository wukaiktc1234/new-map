<script setup lang="ts">
/**
 * 手持收货入口对话框
 * 生成手持收货链接 + 二维码，手机扫码直达；附各手机浏览器安装说明
 */
import { ref, computed, watch } from 'vue'
import QRCode from 'qrcode'
import { get } from '@/api/request'
import type { ReceiverType } from '@/types/purchase-arrival'

interface Props {
  modelValue: boolean
  receiverType: ReceiverType
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const qrDataUrl = ref('')

const receiverLabel = computed(() => (props.receiverType === 'STORE' ? '门店收货' : '仓库入库'))

const link = ref('')

/** 获取手机可访问的局域网链接：优先后端返回的本机 IP，失败回退当前 hostname */
async function buildLink(): Promise<string> {
  const { protocol, hostname, port } = window.location
  try {
    const res = await get<string[] | null>('/v1/system/net/local-ip')
    const ips = Array.isArray(res) ? res : []
    if (ips.length > 0) {
      return `${protocol}//${ips[0]}${port ? `:${port}` : ''}/store-management/receiving-mobile?receiverType=${props.receiverType}`
    }
  } catch {
    // 接口不可用时回退
  }
  const host = hostname === 'localhost' || hostname === '127.0.0.1' ? window.location.host : `${hostname}${port ? `:${port}` : ''}`
  return `${protocol}//${host}/store-management/receiving-mobile?receiverType=${props.receiverType}`
}

watch(
  () => props.modelValue,
  async (visible) => {
    if (!visible) return
    link.value = await buildLink()
    try {
      qrDataUrl.value = await QRCode.toDataURL(link.value, {
        width: 220,
        margin: 1,
        color: { dark: '#1f2937', light: '#ffffff' },
      })
    } catch {
      qrDataUrl.value = ''
    }
  },
)

function copyLink(): void {
  navigator.clipboard?.writeText(link.value).then(
    () => {
      // 复制成功提示由调用方样式承接，这里简单反馈
      const el = document.querySelector('.entry-dialog .copy-feedback')
      if (el) el.textContent = '已复制'
    },
    () => {
      const el = document.querySelector('.entry-dialog .copy-feedback')
      if (el) el.textContent = '复制失败，请手动复制'
    },
  )
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="`移动端收货 · ${receiverLabel}`"
    width="420px"
    class="entry-dialog"
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div class="entry-body">
      <div class="entry-body__qr">
        <img v-if="qrDataUrl" :src="qrDataUrl" alt="移动端收货二维码" class="qr-img" />
        <div v-else class="qr-loading">二维码生成中...</div>
        <div class="qr-tip">手机浏览器扫码直达移动端收货</div>
      </div>

      <div class="entry-body__link">
        <div class="link-label">访问地址</div>
        <div class="link-box">
          <span class="link-text">{{ link }}</span>
          <el-button link type="primary" size="small" @click="copyLink">复制</el-button>
        </div>
        <div v-if="link.includes('localhost')" class="link-warn">
          提示：链接为 localhost，手机无法访问。请用电脑的局域网 IP 访问系统后再打开本弹窗，或手动将 localhost 替换为电脑 IP（如 192.168.0.106）。
        </div>
        <span class="copy-feedback" />
      </div>

      <div class="entry-body__guide">
        <div class="guide-title">手机安装到桌面（全屏无地址栏）</div>
        <ul class="guide-list">
          <li><b>iPhone</b>：Safari 分享按钮 → 添加到主屏幕</li>
          <li><b>安卓</b>：浏览器菜单 → 添加到主屏幕（或"安装应用"）</li>
        </ul>
      </div>
    </div>
  </el-dialog>
</template>

<style scoped lang="scss">
.entry-body {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);

  &__qr {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--fts-space-2);
  }

  &__link {
    .link-label {
      font-size: var(--fts-font-size-sm);
      color: var(--fts-text-secondary);
      margin-bottom: var(--fts-space-1);
    }
  }
}

.qr-img {
  width: 220px;
  height: 220px;
}

.qr-loading {
  width: 220px;
  height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--fts-text-tertiary);
  font-size: var(--fts-font-size-sm);
}

.qr-tip {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
}

.link-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--fts-space-2);
  padding: var(--fts-space-2) var(--fts-space-3);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-base);
  background: var(--fts-bg-subtle);
}

.link-text {
  font-size: 12px;
  color: var(--fts-text-primary);
  word-break: break-all;
  user-select: all;
}

.link-warn {
  margin-top: var(--fts-space-2);
  padding: var(--fts-space-2) var(--fts-space-3);
  border-radius: var(--fts-radius-sm);
  background: var(--fts-warning-bg);
  color: var(--fts-warning);
  font-size: 12px;
  line-height: 1.6;
}

.copy-feedback {
  display: block;
  margin-top: var(--fts-space-1);
  font-size: 12px;
  color: var(--fts-success);
  min-height: 16px;
}

.guide-title {
  font-size: var(--fts-font-size-sm);
  font-weight: var(--fts-font-weight-semibold);
  color: var(--fts-text-primary);
  margin-bottom: var(--fts-space-2);
}

.guide-list {
  margin: 0;
  padding-left: 18px;
  font-size: 13px;
  color: var(--fts-text-secondary);
  line-height: 2;
}
</style>
