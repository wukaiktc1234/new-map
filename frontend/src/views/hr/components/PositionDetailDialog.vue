<script setup lang="ts">
/**
 * 岗位详情对话框
 * 从 HRPosition.vue 中提取，减少页面文件体积
 */
import StatusTag from '@/components/core/StatusTag.vue'
import type { PositionItem } from '@/api/hr/position'

interface Props {
  modelValue: boolean
  data: PositionItem | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:modelValue': [visible: boolean]
}>()

function handleClose() {
  emit('update:modelValue', false)
}
</script>

<template>
  <el-dialog :model-value="modelValue" title="岗位详情" width="680px" class="fts-dialog--md" destroy-on-close lock-scroll="false" @update:model-value="handleClose">
    <template v-if="data">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="岗位编码">{{ data.positionCode }}</el-descriptions-item>
        <el-descriptions-item label="岗位名称">{{ data.positionName }}</el-descriptions-item>
        <el-descriptions-item label="所属部门">{{ data.departmentName }}</el-descriptions-item>
        <el-descriptions-item label="编制人数">{{ data.maxCount }}人</el-descriptions-item>
        <el-descriptions-item label="在职人数">{{ data.currentCount }}人</el-descriptions-item>
        <el-descriptions-item label="状态">
          <StatusTag
            :status="data.status === 'active' ? 'active' : 'inactive'"
            :label="data.status === 'active' ? '启用' : '停用'"
            size="small"
          />
        </el-descriptions-item>
        <el-descriptions-item label="平均薪资">¥{{ data.avgSalary.toLocaleString() }}</el-descriptions-item>
        <el-descriptions-item label="岗位描述" :span="2">{{ data.description || '-' }}</el-descriptions-item>
      </el-descriptions>
    </template>
  </el-dialog>
</template>
