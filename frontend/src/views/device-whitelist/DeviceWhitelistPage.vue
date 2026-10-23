<script setup lang="ts">
/**
 * APK 设备白名单管理
 * 生成激活码、设备列表、禁用/启用
 */
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deviceRegistrationApi, type DeviceRegistrationInfo } from '@/api/device-registration'
import { formatters } from '@/utils/formatters'

const loading = ref(false)
const records = ref<DeviceRegistrationInfo[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(20)
const keyword = ref('')

const statusMap: Record<string, { label: string; type: string }> = {
  PENDING: { label: '待激活', type: 'warning' },
  ACTIVE: { label: '已激活', type: 'success' },
  DISABLED: { label: '已禁用', type: 'danger' },
}

async function loadPage(): Promise<void> {
  loading.value = true
  try {
    const res = await deviceRegistrationApi.getPage(current.value, size.value, keyword.value || undefined)
    records.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error((err as { message?: string })?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function generateCode(): Promise<void> {
  try {
    const res = await deviceRegistrationApi.generateCode()
    ElMessageBox.alert(
      `<p>激活码：<b style="font-size:22px;letter-spacing:2px;color:#1a5fb4">${res.activationCode}</b></p>
       <p style="color:#6b7a88;font-size:12px;margin-top:6px">有效期 24 小时，一次性使用。将激活码交给现场安装人员输入即可。</p>`,
      '激活码已生成',
      { dangerouslyUseHTMLString: true, confirmButtonText: '复制并关闭' },
    ).then(() => {
      void navigator.clipboard?.writeText(res.activationCode)
    })
    await loadPage()
  } catch (err) {
    ElMessage.error((err as { message?: string })?.message || '生成失败')
  }
}

async function toggle(row: DeviceRegistrationInfo): Promise<void> {
  const disabling = row.status === 'ACTIVE'
  try {
    await ElMessageBox.confirm(
      disabling
        ? `禁用后设备 ${row.deviceName || row.deviceId} 将无法访问系统，确定？`
        : `恢复启用设备 ${row.deviceName || row.deviceId}？`,
      disabling ? '禁用设备' : '启用设备',
      { type: disabling ? 'warning' : 'info' },
    )
    if (disabling) {
      await deviceRegistrationApi.disable(row.registrationId)
    } else {
      await deviceRegistrationApi.enable(row.registrationId)
    }
    ElMessage.success(disabling ? '已禁用' : '已启用')
    await loadPage()
  } catch (err) {
    if (err === 'cancel' || err === 'close') return
    ElMessage.error((err as { message?: string })?.message || '操作失败')
  }
}

function statusTag(status: string): { label: string; type: string } {
  return statusMap[status] || { label: status, type: 'info' }
}

onMounted(loadPage)
</script>

<template>
  <div class="device-whitelist-page">
    <div class="page-header">
      <div>
        <h2>APK 设备白名单</h2>
        <p class="sub">未激活的设备无法访问业务接口。生成激活码后交给现场人员输入激活。</p>
      </div>
      <el-button type="primary" @click="generateCode">生成激活码</el-button>
    </div>

    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索设备ID/名称/激活码/绑定用户"
        clearable
        style="width: 280px"
        @keyup.enter="current = 1; loadPage()"
        @clear="current = 1; loadPage()"
      />
      <el-button @click="current = 1; loadPage()">查询</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe>
      <el-table-column prop="registrationId" label="ID" width="70" />
      <el-table-column label="设备" min-width="200">
        <template #default="{ row }">
          <div class="dev-cell">
            <span class="dev-name">{{ row.deviceName || '未绑定' }}</span>
            <span class="dev-id">{{ row.deviceId || '-' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="activationCode" label="激活码" width="120" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status).type" size="small">{{ statusTag(row.status).label }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="绑定用户" width="120">
        <template #default="{ row }">{{ row.boundUsername || '-' }}</template>
      </el-table-column>
      <el-table-column label="激活码有效期至" width="170">
        <template #default="{ row }">{{ row.codeExpireAt ? formatters.datetime(row.codeExpireAt) : '-' }}</template>
      </el-table-column>
      <el-table-column label="激活时间" width="170">
        <template #default="{ row }">{{ row.activatedAt ? formatters.datetime(row.activatedAt) : '-' }}</template>
      </el-table-column>
      <el-table-column label="最近访问" width="170">
        <template #default="{ row }">{{ row.lastSeenAt ? formatters.datetime(row.lastSeenAt) : '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'ACTIVE'"
            type="danger"
            size="small"
            text
            @click="toggle(row)"
          >禁用</el-button>
          <el-button
            v-else-if="row.status === 'DISABLED'"
            type="primary"
            size="small"
            text
            @click="toggle(row)"
          >启用</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="current"
      v-model:page-size="size"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      style="margin-top: 16px; justify-content: flex-end"
      @current-change="loadPage"
      @size-change="current = 1; loadPage()"
    />
  </div>
</template>

<style scoped lang="scss">
.device-whitelist-page {
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 16px;

  h2 {
    font-size: 20px;
    margin-bottom: 4px;
  }

  .sub {
    color: #6b7a88;
    font-size: 13px;
  }
}

.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}

.dev-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;

  .dev-name {
    font-weight: 600;
  }

  .dev-id {
    font-size: 12px;
    color: #8b98a5;
    font-family: Consolas, Menlo, monospace;
  }
}
</style>
