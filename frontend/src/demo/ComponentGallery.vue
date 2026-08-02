<template>
  <div class="component-gallery-page">
    <PageHeader title="组件库展示" description="食品溯源系统 UI 组件体系一览" />

    <!-- 统计卡片 -->
    <div class="gallery-section">
      <h2 class="section-title">统计卡片 (StatCard)</h2>
      <div class="stats-grid">
        <StatCard icon="Tickets" label="待办事项" value="128" colorType="primary" :trend="12" />
        <StatCard icon="CircleCheck" label="已处理" value="1,256" colorType="success" :trend="8" />
        <StatCard icon="Loading" label="进行中" value="56" colorType="warning" :trend="-3" />
        <StatCard icon="WarningFilled" label="逾期未处理" value="8" colorType="error" :trend="-15" />
      </div>
    </div>

    <!-- 内容卡片 -->
    <div class="gallery-section">
      <h2 class="section-title">内容卡片 (ContentCard)</h2>
      <div class="cards-grid">
        <ContentCard title="基础卡片">
          <p>这是一个基础的内容卡片，用于包裹页面内容区块。</p>
        </ContentCard>
        <ContentCard title="带操作按钮">
          <template #actions>
            <el-button type="primary" size="small">操作</el-button>
          </template>
          <p>卡片可以包含右上角的操作按钮。</p>
        </ContentCard>
      </div>
    </div>

    <!-- 状态标签 -->
    <div class="gallery-section">
      <h2 class="section-title">状态标签 (StatusTag)</h2>
      <ContentCard title="状态标签展示">
        <div class="tag-list">
          <div class="tag-item">
            <StatusTag status="active" />
            <StatusTag status="inactive" />
            <StatusTag status="pending" />
            <StatusTag status="error" />
          </div>
          <div class="tag-item">
            <StatusTag status="success" />
            <StatusTag status="warning" />
            <StatusTag status="info" />
            <StatusTag status="draft" />
          </div>
        </div>
      </ContentCard>
    </div>

    <!-- 数据表格 -->
    <div class="gallery-section">
      <h2 class="section-title">数据表格 (DataTable)</h2>
      <ContentCard title="员工信息表示例">
        <DataTable
          :columns="tableColumns"
          :data="tableData"
          :loading="false"
        >
          <template #status="{ row }">
            <StatusTag :status="row.status" />
          </template>
          <template #operation="{ row }">
            <el-button link type="primary" size="small">查看</el-button>
            <el-button link type="primary" size="small">编辑</el-button>
            <el-button link type="danger" size="small">删除</el-button>
          </template>
        </DataTable>
      </ContentCard>
    </div>

    <!-- 按钮样式 -->
    <div class="gallery-section">
      <h2 class="section-title">按钮样式</h2>
      <ContentCard title="Element Plus 按钮">
        <div class="button-group">
          <el-button type="primary">主要按钮</el-button>
          <el-button>默认按钮</el-button>
          <el-button type="success">成功按钮</el-button>
          <el-button type="warning">警告按钮</el-button>
          <el-button type="danger">危险按钮</el-button>
          <el-button type="info">信息按钮</el-button>
        </div>
        <div class="button-group">
          <el-button type="primary" plain>主要按钮</el-button>
          <el-button plain>默认按钮</el-button>
          <el-button type="success" plain>成功按钮</el-button>
          <el-button type="warning" plain>警告按钮</el-button>
          <el-button type="danger" plain>危险按钮</el-button>
        </div>
        <div class="button-group">
          <el-button link type="primary">链接按钮</el-button>
          <el-button size="small">小按钮</el-button>
          <el-button size="large">大按钮</el-button>
        </div>
      </ContentCard>
    </div>

    <!-- 表单元素 -->
    <div class="gallery-section">
      <h2 class="section-title">表单元素</h2>
      <ContentCard title="基础表单">
        <el-form :model="formData" label-width="100px">
          <el-form-item label="用户名">
            <el-input v-model="formData.username" placeholder="请输入用户名" />
          </el-form-item>
          <el-form-item label="部门">
            <el-select v-model="formData.department" placeholder="请选择部门">
              <el-option label="技术部" value="tech" />
              <el-option label="市场部" value="market" />
              <el-option label="人事部" value="hr" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-switch v-model="formData.status" />
          </el-form-item>
          <el-form-item label="日期">
            <el-date-picker v-model="formData.date" type="date" placeholder="选择日期" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary">提交</el-button>
            <el-button>重置</el-button>
          </el-form-item>
        </el-form>
      </ContentCard>
    </div>

    <!-- 消息提示 -->
    <div class="gallery-section">
      <h2 class="section-title">消息提示</h2>
      <ContentCard title="Alert 提示">
        <el-alert title="成功提示" type="success" description="操作已成功完成" show-icon />
        <el-alert title="警告提示" type="warning" description="请注意检查输入内容" show-icon />
        <el-alert title="错误提示" type="error" description="操作失败，请稍后重试" show-icon />
        <el-alert title="信息提示" type="info" description="这是一条普通的信息提示" show-icon />
      </ContentCard>
    </div>

    <!-- 空状态 -->
    <div class="gallery-section">
      <h2 class="section-title">空状态</h2>
      <ContentCard title="EmptyState">
        <EmptyState description="暂无数据" />
      </ContentCard>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import ContentCard from '@/components/core/ContentCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import DataTable from '@/components/core/DataTable.vue'
import EmptyState from '@/components/core/EmptyState.vue'

const formData = ref({
  username: '',
  department: '',
  status: true,
  date: ''
})

const tableColumns = [
  { prop: 'id', label: '工号', minWidth: 100 },
  { prop: 'name', label: '姓名', minWidth: 120 },
  { prop: 'department', label: '部门', minWidth: 150 },
  { prop: 'position', label: '职位', minWidth: 150 },
  { prop: 'status', label: '状态', minWidth: 100 },
  { prop: 'hireDate', label: '入职日期', minWidth: 150 },
]

const tableData = [
  { id: 'EMP001', name: '张三', department: '技术部', position: '高级工程师', status: 'active', hireDate: '2023-01-15' },
  { id: 'EMP002', name: '李四', department: '市场部', position: '市场经理', status: 'active', hireDate: '2022-06-20' },
  { id: 'EMP003', name: '王五', department: '人事部', position: '人事专员', status: 'probation', hireDate: '2024-03-01' },
  { id: 'EMP004', name: '赵六', department: '财务部', position: '会计', status: 'inactive', hireDate: '2021-11-10' },
]
</script>

<style scoped lang="scss">
.component-gallery-page {
  padding: var(--fts-space-5);
}

.gallery-section {
  margin-bottom: var(--fts-space-6);
}

.section-title {
  font-size: var(--fts-font-size-lg);
  font-weight: 600;
  color: var(--fts-text-primary);
  margin: 0 0 var(--fts-space-4) 0;
  padding-left: var(--fts-space-3);
  border-left: 4px solid var(--fts-primary);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: var(--fts-space-4);
}

.cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: var(--fts-space-4);
}

.tag-list {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3);
}

.tag-item {
  display: flex;
  gap: var(--fts-space-3);
  flex-wrap: wrap;
}

.button-group {
  display: flex;
  gap: var(--fts-space-3);
  margin-bottom: var(--fts-space-3);
  flex-wrap: wrap;
}

:deep(.el-alert) {
  margin-bottom: var(--fts-space-3);

  &:last-child {
    margin-bottom: 0;
  }
}
</style>
