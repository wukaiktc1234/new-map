<template>
  <div class="table-management">
    <div class="page-header">
      <div class="header-left">
        <el-button text @click="goBack" class="back-btn">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <h1>桌台管理</h1>
      </div>
      <div class="header-right">
        <div class="stats-bar">
          <div class="stat-item">
            <span class="stat-label">总桌台</span>
            <span class="stat-value total">{{ tables.length }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">空闲</span>
            <span class="stat-value available">{{ availableCount }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">占用</span>
            <span class="stat-value occupied">{{ occupiedCount }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">预定</span>
            <span class="stat-value reserved">{{ reservedCount }}</span>
          </div>
        </div>
        <el-select v-model="selectedArea" placeholder="选择区域" clearable class="area-select">
          <el-option v-for="area in areas" :key="area" :label="area" :value="area" />
        </el-select>
        <el-button type="primary" @click="showAddDialog">
          <el-icon><Plus /></el-icon>
          添加桌台
        </el-button>
      </div>
    </div>

    <div class="page-content">
      <div v-if="loading" class="loading-container">
        <el-icon class="is-loading" :size="40"><Loading /></el-icon>
        <span>加载中...</span>
      </div>

      <div v-else-if="tables.length === 0" class="empty-container">
        <div class="empty-icon">🪑</div>
        <p>暂无桌台数据</p>
        <el-button type="primary" @click="showAddDialog">
          <el-icon><Plus /></el-icon>
          添加桌台
        </el-button>
      </div>

      <div v-else class="table-grid">
        <div 
          v-for="table in filteredTables" 
          :key="table.id"
          class="table-card"
          :class="table.status"
          @click="handleTableClick(table)"
        >
          <div class="card-top">
            <div class="table-number">{{ table.tableNumber }}</div>
            <el-tag :type="getStatusType(table.status)" size="small" effect="dark">
              {{ getStatusText(table.status) }}
            </el-tag>
          </div>
          
          <div class="table-name">{{ table.tableName }}</div>
          
          <div class="table-info">
            <div class="info-item">
              <el-icon><User /></el-icon>
              <span>{{ table.capacity }}人</span>
            </div>
            <el-tag size="small" type="info">{{ table.area }}</el-tag>
          </div>
          
          <div v-if="table.status === 'occupied'" class="table-detail">
            <div v-if="table.guestCount" class="detail-row">
              <span class="label">就餐人数:</span>
              <span class="value">{{ table.guestCount }}人</span>
            </div>
            <div v-if="table.seatedAt" class="detail-row">
              <span class="label">入座时间:</span>
              <span class="value">{{ formatTime(table.seatedAt) }}</span>
            </div>
          </div>
          
          <div class="card-actions">
            <el-button size="small" @click.stop="showQrCode(table)">
              <el-icon><PictureFilled /></el-icon>
            </el-button>
            <el-button size="small" @click.stop="editTable(table)">
              <el-icon><Edit /></el-icon>
            </el-button>
            <el-button 
              size="small" 
              type="danger" 
              @click.stop="deleteTable(table)"
              :disabled="table.status === 'occupied'"
            >
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑桌台' : '添加桌台'" width="450px" :lock-scroll="false">
      <el-form :model="formData" :rules="tableFormRules" ref="tableFormRef" label-width="80px">
        <el-form-item label="桌号" prop="tableNumber">
          <el-input v-model="formData.tableNumber" placeholder="如: A01" size="large" />
        </el-form-item>
        <el-form-item label="桌台名称" prop="tableName">
          <el-input v-model="formData.tableName" placeholder="如: A区1号桌" size="large" />
        </el-form-item>
        <el-form-item label="座位数" prop="capacity">
          <el-input-number v-model="formData.capacity" :min="1" :max="50" size="large" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="区域" prop="area">
          <el-select v-model="formData.area" placeholder="选择区域" allow-create filterable size="large" style="width: 100%;">
            <el-option v-for="area in areas" :key="area" :label="area" :value="area" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false" size="large">取消</el-button>
        <el-button type="primary" @click="saveTable" size="large" :loading="saving">
          保存
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="qrDialogVisible" title="桌台二维码" width="400px" :lock-scroll="false">
      <div class="qr-content" v-if="selectedTable">
        <div class="qr-header">
          <div class="qr-table-number">{{ selectedTable.tableNumber }}</div>
          <div class="qr-table-name">{{ selectedTable.tableName }}</div>
        </div>
        <div class="qr-wrapper">
          <qrcode-vue :value="getQrUrl(selectedTable)" :size="220" level="M" />
        </div>
        <div class="qr-tip">
          <el-icon><Document /></el-icon>
          扫描二维码进行点餐
        </div>
        <div class="qr-info">
          <span class="label">二维码编码:</span>
          <span class="code">{{ selectedTable.qrCode }}</span>
        </div>
        <div class="qr-actions">
          <el-button @click="copyQrCode">
            <el-icon><DocumentCopy /></el-icon>
            复制链接
          </el-button>
          <el-button type="primary" @click="regenerateQrCode" :loading="regenerating">
            <el-icon><Refresh /></el-icon>
            重新生成
          </el-button>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="actionDialogVisible" :title="`桌台 ${selectedTable?.tableNumber}`" width="450px" :lock-scroll="false">
      <div class="action-content" v-if="selectedTable">
        <div class="table-preview">
          <div class="preview-number">{{ selectedTable.tableNumber }}</div>
          <div class="preview-name">{{ selectedTable.tableName }}</div>
          <div class="preview-tags">
            <el-tag :type="getStatusType(selectedTable.status)" size="large">
              {{ getStatusText(selectedTable.status) }}
            </el-tag>
            <el-tag size="large" type="info">{{ selectedTable.area }}</el-tag>
            <el-tag size="large" type="info">{{ selectedTable.capacity }}人</el-tag>
          </div>
        </div>
        
        <el-divider content-position="left">操作</el-divider>
        
        <div class="action-buttons">
          <el-button 
            v-if="selectedTable.status === 'available'" 
            type="success" 
            size="large" 
            @click="occupyTable"
          >
            <el-icon><SuccessFilled /></el-icon>
            占用桌台
          </el-button>
          <el-button 
            v-if="selectedTable.status === 'occupied'" 
            type="warning" 
            size="large" 
            @click="releaseTable"
          >
            <el-icon><CircleCloseFilled /></el-icon>
            释放桌台
          </el-button>
          <el-button 
            v-if="selectedTable.status === 'occupied'" 
            type="primary" 
            size="large" 
            @click="openTransferDialog"
          >
            <el-icon><Switch /></el-icon>
            换台
          </el-button>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="transferDialogVisible" title="换台" width="480px" :lock-scroll="false">
      <div class="transfer-content">
        <div class="transfer-step">
          <div class="step-num">1</div>
          <div class="step-info">
            <div class="step-title">当前桌台</div>
            <div class="step-detail">
              <span class="table-num">{{ transferFrom?.tableNumber }}</span>
              <span class="table-name">{{ transferFrom?.tableName }}</span>
              <el-tag :type="transferFrom?.status === 'occupied' ? 'danger' : 'success'" size="large">
                {{ getStatusText(transferFrom?.status || '') }}
              </el-tag>
            </div>
          </div>
        </div>
        
        <div class="transfer-divider">
          <el-icon><Switch /></el-icon>
        </div>
        
        <div class="transfer-step">
          <div class="step-num">2</div>
          <div class="step-info">
            <div class="step-title">目标桌台</div>
            <el-select 
              v-model="transferToId" 
              placeholder="选择空闲桌台" 
              size="large" 
              style="width: 100%;"
            >
              <el-option 
                v-for="table in availableTables" 
                :key="table.id" 
                :label="`${table.tableNumber} - ${table.tableName} (${table.capacity}人)`" 
                :value="table.id" 
              />
            </el-select>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="transferDialogVisible = false" size="large">取消</el-button>
        <el-button type="primary" @click="confirmTransfer" size="large" :loading="transferring">
          确认换台
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { 
  Plus, Edit, Delete, User, ArrowLeft, PictureFilled, Loading, 
  Refresh, DocumentCopy, Switch, SuccessFilled, CircleCloseFilled, Document
} from '@element-plus/icons-vue'
import QrcodeVue from 'qrcode.vue'
import request from '../api/request'

interface DiningTable {
  id: number
  tableNumber: string
  tableName: string
  capacity: number
  area: string
  status: 'available' | 'occupied' | 'reserved'
  qrCode: string
  currentOrderId: number | null
  guestCount: number | null
  seatedAt: string | null
  createdAt: string
  updatedAt: string
}

const router = useRouter()
const loading = ref(true)
const saving = ref(false)
const regenerating = ref(false)
const transferring = ref(false)
const tables = ref<DiningTable[]>([])
const areas = ref<string[]>([])
const selectedArea = ref('')
const dialogVisible = ref(false)
const qrDialogVisible = ref(false)
const actionDialogVisible = ref(false)
const transferDialogVisible = ref(false)
const isEdit = ref(false)
const selectedTable = ref<DiningTable | null>(null)
const transferFrom = ref<DiningTable | null>(null)
const transferToId = ref<number | null>(null)

const formData = ref({
  tableNumber: '',
  tableName: '',
  capacity: 4,
  area: ''
})

/** 桌台表单引用 */
const tableFormRef = ref<FormInstance>()

/** 桌台表单校验规则 */
const tableFormRules: FormRules = {
  tableNumber: [
    { required: true, message: '请输入桌号', trigger: 'blur' },
    { min: 1, max: 20, message: '桌号长度1-20个字符', trigger: 'blur' }
  ],
  capacity: [
    { required: true, message: '请输入座位数', trigger: 'blur' }
  ],
  area: [
    { max: 20, message: '区域名称最多20个字符', trigger: 'blur' }
  ]
}

const filteredTables = computed(() => {
  if (!selectedArea.value) return tables.value
  return tables.value.filter(t => t.area === selectedArea.value)
})

const availableTables = computed(() => {
  return tables.value.filter(t => t.status === 'available')
})

const availableCount = computed(() => {
  return tables.value.filter(t => t.status === 'available').length
})

const occupiedCount = computed(() => {
  return tables.value.filter(t => t.status === 'occupied').length
})

const reservedCount = computed(() => {
  return tables.value.filter(t => t.status === 'reserved').length
})

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    available: '空闲',
    occupied: '占用',
    reserved: '预定'
  }
  return map[status] || status
}

const getStatusType = (status: string) => {
  const map: Record<string, 'success' | 'danger' | 'warning' | 'info'> = {
    available: 'success',
    occupied: 'danger',
    reserved: 'warning'
  }
  return map[status] || 'info'
}

const formatTime = (time: string) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN', { 
    year: 'numeric', 
    month: '2-digit', 
    day: '2-digit',
    hour: '2-digit', 
    minute: '2-digit' 
  })
}

const getQrUrl = (table: DiningTable) => {
  return `${window.location.origin}/customer/order?table=${table.qrCode}`
}

const goBack = () => {
  router.push('/')
}

const loadTables = async () => {
  loading.value = true
  try {
    const tablesRes = await request.get('/v1/pos/tables') as any
    const areaRes = await request.get('/v1/pos/tables/areas') as any
    
    tables.value = Array.isArray(tablesRes) ? tablesRes : (tablesRes?.data || [])
    areas.value = Array.isArray(areaRes) ? areaRes : (areaRes?.data || [])
  } catch (error) {
    console.error('加载桌台失败:', error)
    ElMessage.error('加载桌台失败')
  } finally {
    loading.value = false
  }
}

const showAddDialog = () => {
  isEdit.value = false
  formData.value = {
    tableNumber: '',
    tableName: '',
    capacity: 4,
    area: ''
  }
  dialogVisible.value = true
  nextTick(() => {
    tableFormRef.value?.clearValidate()
  })
}

const editTable = (table: DiningTable) => {
  isEdit.value = true
  formData.value = {
    tableNumber: table.tableNumber,
    tableName: table.tableName,
    capacity: table.capacity,
    area: table.area
  }
  selectedTable.value = table
  dialogVisible.value = true
  nextTick(() => {
    tableFormRef.value?.clearValidate()
  })
}

const saveTable = async () => {
  try {
    // 表单验证
    await tableFormRef.value?.validate()

    saving.value = true
    
    if (isEdit.value && selectedTable.value) {
      await request.put(`/v1/pos/tables/${selectedTable.value.id}`, formData.value)
      ElMessage.success('更新成功')
    } else {
      await request.post('/v1/pos/tables', formData.value)
      ElMessage.success('添加成功')
    }
    
    dialogVisible.value = false
    loadTables()
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const deleteTable = async (table: DiningTable) => {
  if (table.status === 'occupied') {
    ElMessage.warning('占用中的桌台无法删除')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定删除桌台 ${table.tableNumber} 吗？`, 
      '删除确认', 
      { 
        type: 'warning',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger'
      }
    )
    
    await request.delete(`/v1/pos/tables/${table.id}`)
    ElMessage.success('删除成功')
    loadTables()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

const handleTableClick = (table: DiningTable) => {
  selectedTable.value = table
  actionDialogVisible.value = true
}

const occupyTable = async () => {
  if (!selectedTable.value) return
  
  try {
    await request.post(`/v1/pos/tables/${selectedTable.value.id}/occupy`)
    ElMessage.success('桌台已占用')
    actionDialogVisible.value = false
    loadTables()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

const releaseTable = async () => {
  if (!selectedTable.value) return
  
  try {
    await ElMessageBox.confirm(
      `确定释放桌台 ${selectedTable.value.tableNumber} 吗？`,
      '释放确认',
      {
        type: 'warning',
        confirmButtonText: '确认释放',
        cancelButtonText: '取消'
      }
    )
    
    await request.post(`/v1/pos/tables/${selectedTable.value.id}/release`)
    ElMessage.success('桌台已释放')
    actionDialogVisible.value = false
    loadTables()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '操作失败')
    }
  }
}

const showQrCode = (table: DiningTable) => {
  selectedTable.value = table
  qrDialogVisible.value = true
}

const copyQrCode = async () => {
  if (!selectedTable.value) return
  
  try {
    await navigator.clipboard.writeText(getQrUrl(selectedTable.value))
    ElMessage.success('链接已复制到剪贴板')
  } catch (error) {
    ElMessage.error('复制失败')
  }
}

const regenerateQrCode = async () => {
  if (!selectedTable.value) return
  
  try {
    await ElMessageBox.confirm(
      '确定要重新生成二维码吗？原有二维码将失效。',
      '重新生成',
      {
        type: 'warning',
        confirmButtonText: '确认',
        cancelButtonText: '取消'
      }
    )
    
    regenerating.value = true
    await request.post(`/v1/pos/tables/${selectedTable.value.id}/regenerate-qrcode`)
    ElMessage.success('二维码已重新生成')
    loadTables()
    qrDialogVisible.value = false
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '重新生成失败')
    }
  } finally {
    regenerating.value = false
  }
}

const openTransferDialog = () => {
  if (!selectedTable.value) return
  transferFrom.value = selectedTable.value
  transferToId.value = null
  actionDialogVisible.value = false
  transferDialogVisible.value = true
}

const confirmTransfer = async () => {
  if (!transferFrom.value || !transferToId.value) {
    ElMessage.warning('请选择目标桌台')
    return
  }
  
  try {
    transferring.value = true
    await request.post('/v1/pos/tables/transfer', null, {
      params: {
        fromId: transferFrom.value.id,
        toId: transferToId.value
      }
    })
    ElMessage.success('换台成功')
    transferDialogVisible.value = false
    loadTables()
  } catch (error: any) {
    ElMessage.error(error.message || '换台失败')
  } finally {
    transferring.value = false
  }
}

onMounted(() => {
  loadTables()
})
</script>

<style scoped>
.table-management {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: white;
  border-bottom: 1px solid #e4e7ed;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-left h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stats-bar {
  display: flex;
  gap: 16px;
  padding: 8px 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.stat-label {
  font-size: 12px;
  color: #909399;
}

.stat-value {
  font-size: 18px;
  font-weight: 600;
}

.stat-value.total {
  color: #303133;
}

.stat-value.available {
  color: #67c23a;
}

.stat-value.occupied {
  color: #f56c6c;
}

.stat-value.reserved {
  color: #e6a23c;
}

.area-select {
  width: 140px;
}

.page-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

.loading-container,
.empty-container {
  height: 400px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  color: #909399;
}

.empty-icon {
  font-size: 64px;
  opacity: 0.5;
}

.table-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
}

.table-card {
  background: white;
  border-radius: 8px;
  padding: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid #e4e7ed;
  position: relative;
}

.table-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: #e4e7ed;
  border-radius: 8px 8px 0 0;
}

.table-card.available::before {
  background: #67c23a;
}

.table-card.occupied::before {
  background: #f56c6c;
}

.table-card.reserved::before {
  background: #e6a23c;
}

.table-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  border-color: #ea580c;
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.table-number {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
  line-height: 1;
}

.table-name {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.table-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #606266;
}

.table-detail {
  padding: 8px;
  background: #f5f7fa;
  border-radius: 6px;
  margin-bottom: 8px;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: #606266;
  padding: 2px 0;
}

.detail-row .label {
  color: #909399;
}

.detail-row .value {
  font-weight: 500;
  color: #303133;
}

.card-actions {
  display: flex;
  gap: 4px;
}

.card-actions .el-button {
  flex: 1;
  padding: 5px 8px;
}

.qr-content {
  text-align: center;
  padding: 8px;
}

.qr-header {
  margin-bottom: 20px;
}

.qr-table-number {
  font-size: 32px;
  font-weight: 700;
  color: #ea580c;
  margin-bottom: 4px;
}

.qr-table-name {
  font-size: 14px;
  color: #606266;
}

.qr-wrapper {
  padding: 20px;
  background: white;
  border-radius: 12px;
  display: inline-block;
  margin-bottom: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.qr-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 16px;
  font-size: 14px;
  color: #606266;
}

.qr-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 16px;
}

.qr-info .label {
  font-size: 12px;
  color: #909399;
}

.qr-info .code {
  font-family: 'SF Mono', 'Monaco', monospace;
  font-size: 12px;
  color: #303133;
}

.qr-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.action-content {
  padding: 8px 0;
}

.table-preview {
  text-align: center;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 12px;
  margin-bottom: 8px;
}

.preview-number {
  font-size: 40px;
  font-weight: 700;
  color: #ea580c;
  margin-bottom: 8px;
}

.preview-name {
  font-size: 16px;
  color: #606266;
  margin-bottom: 16px;
}

.preview-tags {
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}

.action-buttons {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.action-buttons .el-button {
  flex: 1;
}

.transfer-content {
  padding: 8px 0;
}

.transfer-step {
  display: flex;
  gap: 16px;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 12px;
  border: 2px solid #e4e7ed;
}

.transfer-step:first-of-type {
  border-color: #f56c6c;
}

.transfer-step:last-of-type {
  border-color: #67c23a;
}

.step-num {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ea580c 0%, #f97316 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
  flex-shrink: 0;
}

.step-info {
  flex: 1;
}

.step-title {
  font-size: 14px;
  color: #909399;
  margin-bottom: 12px;
}

.step-detail {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.table-num {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.table-name {
  font-size: 14px;
  color: #606266;
}

.transfer-divider {
  display: flex;
  justify-content: center;
  padding: 12px 0;
  color: #ea580c;
  font-size: 24px;
}

@media (max-width: 768px) {
  .stats-bar {
    display: none;
  }
  
  .table-grid {
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 16px;
  }
}
</style>
