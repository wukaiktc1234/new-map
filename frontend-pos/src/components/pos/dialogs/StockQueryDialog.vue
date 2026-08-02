<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="emit('update:modelValue', $event)"
    title="库存查询"
    width="700px"
    class="stock-query-dialog"
    :lock-scroll="false"
  >
    <div class="stock-query-content">
      <div class="stock-warning-section">
        <h4>⚠️ 低库存预警 (库存 &lt; 10)</h4>
        <el-table
          :data="lowStockItems"
          size="small"
          border
          style="width: 100%"
          max-height="400"
        >
          <el-table-column prop="name" label="菜品名称" min-width="150" />
          <el-table-column prop="stock" label="当前库存" width="100" align="center">
            <template #default="{ row }">
              <span :class="{ 'low-stock': row.stock < 10, 'out-of-stock': row.stock === 0 }">
                {{ row.stock }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag
                v-if="row.stock === 0"
                type="danger"
                size="small"
              >售罄</el-tag>
              <el-tag
                v-else-if="row.stock < 10"
                type="warning"
                size="small"
              >库存不足</el-tag>
              <el-tag
                v-else
                type="success"
                size="small"
              >正常</el-tag>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="lowStockItems.length === 0" class="empty-stock">
          <span>🎉 所有菜品库存充足！</span>
        </div>
      </div>
    </div>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
interface LowStockItem {
  name: string
  stock: number
}

interface Props {
  modelValue: boolean
  lowStockItems: Array<LowStockItem>
}

defineProps<Props>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()
</script>

<style scoped>
.stock-query-content {
  padding: 8px 0;
}

.stock-warning-section h4 {
  margin: 0 0 16px 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--pos-text-primary);
}

.low-stock {
  color: var(--pos-warning);
  font-weight: 700;
}

.out-of-stock {
  color: var(--pos-danger);
  font-weight: 700;
}

.empty-stock {
  text-align: center;
  padding: 40px;
  font-size: 15px;
  color: var(--pos-success);
  font-weight: 500;
}

.stock-query-dialog {
  :deep(.el-dialog) {
    background-color: var(--pos-bg-secondary) !important;
  }

  :deep(.el-dialog__header) {
    border-bottom: 1px solid var(--pos-border-light);
  }

  :deep(.el-dialog__title) {
    color: var(--pos-text-primary) !important;
    font-weight: 600;
  }

  :deep(.el-dialog__body) {
    color: var(--pos-text-primary) !important;
  }

  :deep(.el-button--primary) {
    color: #fff;
  }

  :deep(.el-button--text) {
    color: var(--pos-primary) !important;
  }

  :deep(.el-button) {
    color: var(--pos-text-primary) !important;
  }

  :deep(.el-input__wrapper) {
    color: var(--pos-text-primary) !important;
    background-color: var(--pos-bg-secondary) !important;
    box-shadow: 0 0 0 1px var(--pos-border-color) inset !important;
  }

  :deep(.el-input__inner) {
    color: var(--pos-text-primary) !important;
  }

  :deep(.el-input__placeholder) {
    color: var(--pos-text-muted) !important;
  }

  :deep(.el-form-item__label) {
    color: var(--pos-text-primary) !important;
  }

  :deep(.el-table th) {
    background-color: var(--pos-bg-secondary);
    color: var(--pos-text-primary) !important;
  }

  :deep(.el-table td) {
    color: var(--pos-text-secondary) !important;
  }
}
</style>
