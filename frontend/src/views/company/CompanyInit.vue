<script setup lang="ts">
/**
 * 公司初始化向导页面
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】引导系统管理员完成公司基础信息初始化
 * 【依赖】L3(PageHeader) 及 Element Plus 表单组件
 *
 * 流程：
 * 1. 校验系统未初始化
 * 2. 填写公司信息
 * 3. 填写默认门店信息
 * 4. 确认并提交初始化
 */
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { OfficeBuilding, Shop, Check, ArrowRight, ArrowLeft } from '@element-plus/icons-vue'

import PageHeader from '@/components/core/PageHeader.vue'
import { companyInitApi } from '@/api/company-init'
import type { CompanyInitFormData } from '@/types/company-init'

const router = useRouter()

// 当前步骤（0: 公司信息, 1: 门店信息, 2: 确认初始化）
const activeStep = ref(0)
const submitLoading = ref(false)
const checking = ref(true)
const initialized = ref(false)

const companyFormRef = ref<FormInstance>()
const storeFormRef = ref<FormInstance>()

const formData = reactive<CompanyInitFormData & { storeName: string; storeAddress: string }>({
  companyName: '',
  companyCode: '',
  legalPerson: '',
  contactPhone: '',
  address: '',
  remark: '',
  storeName: '',
  storeAddress: '',
})

const companyRules: FormRules = {
  companyName: [
    { required: true, message: '请输入公司名称', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在 2 到 100 个字符之间', trigger: 'blur' },
  ],
  companyCode: [
    { min: 2, max: 50, message: '长度在 2 到 50 个字符之间', trigger: 'blur' },
  ],
  legalPerson: [
    { max: 50, message: '长度不超过 50 个字符', trigger: 'blur' },
  ],
  contactPhone: [
    { pattern: /^1[3-9]\d{9}$|^0\d{2,3}-?\d{7,8}$/, message: '请输入正确的联系电话', trigger: 'blur' },
  ],
}

const storeRules: FormRules = {
  storeName: [
    { required: true, message: '请输入默认门店名称', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在 2 到 100 个字符之间', trigger: 'blur' },
  ],
  storeAddress: [
    { max: 200, message: '长度不超过 200 个字符', trigger: 'blur' },
  ],
}

/** 检查初始化状态 */
async function checkInitStatus(): Promise<void> {
  checking.value = true
  try {
    const completed = await companyInitApi.checkInitialized()
    initialized.value = completed
    if (completed) {
      ElMessage.warning('系统已完成初始化，禁止重复初始化')
    }
  } catch (error: unknown) {
    if (error instanceof Error) {
      ElMessage.error(error.message || '检查初始化状态失败')
    }
  } finally {
    checking.value = false
  }
}

/** 下一步 */
async function nextStep(): Promise<void> {
  if (activeStep.value === 0) {
    const valid = await companyFormRef.value?.validate().catch(() => false)
    if (!valid) return
    activeStep.value = 1
    return
  }

  if (activeStep.value === 1) {
    const valid = await storeFormRef.value?.validate().catch(() => false)
    if (!valid) return
    activeStep.value = 2
  }
}

/** 上一步 */
function prevStep(): void {
  if (activeStep.value > 0) {
    activeStep.value -= 1
  }
}

/** 提交初始化 */
async function submitInit(): Promise<void> {
  submitLoading.value = true
  try {
    const success = await companyInitApi.initializeCompany({
      companyName: formData.companyName,
      companyCode: formData.companyCode,
      legalPerson: formData.legalPerson,
      contactPhone: formData.contactPhone,
      address: formData.address,
      remark: formData.remark,
    })
    if (success) {
      await companyInitApi.completeInit()
      ElMessage.success('公司初始化成功，正在进入系统')
      router.push('/home')
    } else {
      ElMessage.error('初始化失败，请稍后重试')
    }
  } catch (error: unknown) {
    if (error instanceof Error) {
      ElMessage.error(error.message || '初始化失败')
    }
  } finally {
    submitLoading.value = false
  }
}

/** 跳过初始化 */
async function skipInit(): Promise<void> {
  try {
    await ElMessageBox.confirm(
      '跳过初始化后可在系统设置中手动维护基础数据，是否继续？',
      '跳过初始化',
      {
        confirmButtonText: '继续跳过',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    await companyInitApi.skipInit()
    ElMessage.info('已跳过初始化')
    router.push('/home')
  } catch (error: unknown) {
    if (error !== 'cancel' && error instanceof Error) {
      ElMessage.error(error.message || '跳过初始化失败')
    }
  }
}

onMounted(() => {
  checkInitStatus()
})
</script>

<template>
  <div class="modern-page">
    <PageHeader title="公司初始化向导" description="首次使用系统，请完成公司与默认门店的基础信息初始化" />

    <section class="init-container">
      <!-- 已完成初始化提示 -->
      <el-result
        v-if="!checking && initialized"
        icon="success"
        title="系统已完成初始化"
        sub-title="如需重新初始化，请联系系统管理员"
      >
        <template #extra>
          <el-button type="primary" @click="router.push('/home')">
            进入工作台
          </el-button>
        </template>
      </el-result>

      <!-- 初始化向导主体 -->
      <template v-else-if="!checking">
        <el-steps :active="activeStep" finish-status="success" class="init-steps">
          <el-step title="公司信息">
            <template #icon>
              <el-icon><OfficeBuilding /></el-icon>
            </template>
          </el-step>
          <el-step title="默认门店">
            <template #icon>
              <el-icon><Shop /></el-icon>
            </template>
          </el-step>
          <el-step title="确认初始化">
            <template #icon>
              <el-icon><Check /></el-icon>
            </template>
          </el-step>
        </el-steps>

        <div class="init-form-wrapper">
          <!-- 步骤1：公司信息 -->
          <el-form
            v-show="activeStep === 0"
            ref="companyFormRef"
            :model="formData"
            :rules="companyRules"
            label-width="120px"
          >
            <el-row :gutter="24">
              <el-col :span="12">
                <el-form-item label="公司名称" prop="companyName">
                  <el-input v-model="formData.companyName" placeholder="请输入公司名称" maxlength="100" show-word-limit />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="公司编码" prop="companyCode">
                  <el-input v-model="formData.companyCode" placeholder="可选，用于系统内部标识" maxlength="50" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="24">
              <el-col :span="12">
                <el-form-item label="法人代表" prop="legalPerson">
                  <el-input v-model="formData.legalPerson" placeholder="请输入法人代表" maxlength="50" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="联系电话" prop="contactPhone">
                  <el-input v-model="formData.contactPhone" placeholder="请输入联系电话" maxlength="20" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="24">
              <el-col :span="24">
                <el-form-item label="公司地址" prop="address">
                  <el-input v-model="formData.address" placeholder="请输入公司地址" maxlength="200" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="24">
              <el-col :span="24">
                <el-form-item label="备注" prop="remark">
                  <el-input
                    v-model="formData.remark"
                    type="textarea"
                    :rows="3"
                    placeholder="请输入备注信息"
                    maxlength="500"
                    show-word-limit
                  />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>

          <!-- 步骤2：默认门店信息 -->
          <el-form
            v-show="activeStep === 1"
            ref="storeFormRef"
            :model="formData"
            :rules="storeRules"
            label-width="120px"
          >
            <el-row :gutter="24">
              <el-col :span="12">
                <el-form-item label="门店名称" prop="storeName">
                  <el-input
                    v-model="formData.storeName"
                    placeholder="请输入默认门店名称"
                    maxlength="100"
                    show-word-limit
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="门店地址" prop="storeAddress">
                  <el-input v-model="formData.storeAddress" placeholder="请输入门店地址" maxlength="200" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-alert
              title="初始化说明"
              type="info"
              :closable="false"
              description="提交后将自动创建默认门店、基础部门（总经办、财务部、采购部、仓储部、厨房/生产部、前厅部）、常见岗位及基础物料/供应商分类。"
              class="init-tip"
            />
          </el-form>

          <!-- 步骤3：确认初始化 -->
          <div v-show="activeStep === 2" class="confirm-panel">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="公司名称">{{ formData.companyName }}</el-descriptions-item>
              <el-descriptions-item label="公司编码">{{ formData.companyCode || '-' }}</el-descriptions-item>
              <el-descriptions-item label="法人代表">{{ formData.legalPerson || '-' }}</el-descriptions-item>
              <el-descriptions-item label="联系电话">{{ formData.contactPhone || '-' }}</el-descriptions-item>
              <el-descriptions-item label="公司地址" :span="2">{{ formData.address || '-' }}</el-descriptions-item>
              <el-descriptions-item label="默认门店">{{ formData.storeName }}</el-descriptions-item>
              <el-descriptions-item label="门店地址">{{ formData.storeAddress || '-' }}</el-descriptions-item>
              <el-descriptions-item label="备注" :span="2">{{ formData.remark || '-' }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </div>

        <!-- 底部操作按钮 -->
        <div class="init-actions">
          <el-button v-if="activeStep > 0" @click="prevStep">
            <el-icon><ArrowLeft /></el-icon>上一步
          </el-button>
          <el-button v-if="activeStep < 2" type="primary" @click="nextStep">
            下一步<el-icon><ArrowRight /></el-icon>
          </el-button>
          <el-button
            v-if="activeStep === 2"
            type="primary"
            :loading="submitLoading"
            @click="submitInit"
          >
            确认初始化
          </el-button>
          <el-button link type="info" @click="skipInit">
            跳过初始化
          </el-button>
        </div>
      </template>
    </section>
  </div>
</template>

<style scoped lang="scss">
.modern-page {
  min-height: 100vh;
  background: var(--fts-bg-page);
  overflow-x: clip;
}

.init-container {
  max-width: 960px;
  margin: var(--fts-space-6) auto;
  padding: var(--fts-space-6);
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-card-radius);
}

.init-steps {
  margin-bottom: var(--fts-space-8);

  :deep(.el-step__icon) {
    background: var(--fts-bg-card);
  }
}

.init-form-wrapper {
  min-height: 240px;
}

.init-tip {
  margin-top: var(--fts-space-4);
}

.confirm-panel {
  padding: var(--fts-space-4);
  background: var(--fts-bg-page);
  border-radius: var(--fts-radius-md);
}

.init-actions {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: var(--fts-space-3);
  margin-top: var(--fts-space-8);
  padding-top: var(--fts-space-4);
  border-top: 1px solid var(--fts-border-secondary);
}

@media (max-width: 768px) {
  .init-container {
    margin: var(--fts-space-4);
    padding: var(--fts-space-4);
  }

  .init-actions {
    flex-wrap: wrap;
  }
}
</style>
