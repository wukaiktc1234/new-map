<script setup lang="ts">
/**
 * 到货单表单对话框
 * 支持新增（从采购订单创建）和编辑物流信息两种模式
 */
import { ref, reactive, computed, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import type {
  PurchaseArrivalFormData,
  PurchaseArrivalUpdateForm,
  PurchaseArrivalInfo,
  ShipmentStatus,
} from '@/types/purchase-arrival'
import { ShipmentStatusOptions, TransportModeOptions, VehicleTypeOptions } from '@/types/purchase-arrival'
import type { PurchaseOrderInfo } from '@/types/purchase-order'

interface Props {
  modelValue: boolean
  mode: 'create' | 'edit'
  initialData?: PurchaseArrivalInfo | null
  initialOrderId?: string
  orderOptions: PurchaseOrderInfo[]
  orderOptionsLoading: boolean
}

const props = withDefaults(defineProps<Props>(), {
  initialData: null,
  initialOrderId: '',
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  create: [data: PurchaseArrivalFormData]
  update: [data: PurchaseArrivalUpdateForm]
}>()

const formRef = ref<FormInstance>()
const submitLoading = ref(false)

const emptyForm: PurchaseArrivalFormData = {
  orderId: '',
  shipmentStatus: '' as ShipmentStatus,
  logisticsNo: '',
  logisticsCompany: '',
  transportMode: '',
  vehiclePlateNo: '',
  vehicleType: '',
  driverName: '',
  driverPhone: '',
  freightAmount: undefined,
  estimatedArrivalDate: '',
  remark: '',
}

const formData = reactive<PurchaseArrivalFormData>({ ...emptyForm })

const dialogTitle = computed(() => (props.mode === 'create' ? '新增到货单' : '编辑物流信息'))

const selectedOrder = computed(() =>
  props.orderOptions.find((o) => o.purchaseOrderId === formData.orderId),
)

const formRules: FormRules = {
  orderId: [{ required: true, message: '请选择采购订单', trigger: 'change' }],
  // 物流信息为纯记录字段，不参与业务流转，均设为可选
}

function resetForm() {
  Object.assign(formData, emptyForm)
  formRef.value?.resetFields()
}

function handleClose() {
  emit('update:modelValue', false)
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (props.mode === 'create') {
      emit('create', { ...formData })
    } else {
      emit('update', {
        shipmentStatus: formData.shipmentStatus,
        logisticsNo: formData.logisticsNo,
        logisticsCompany: formData.logisticsCompany,
        transportMode: formData.transportMode,
        vehiclePlateNo: formData.vehiclePlateNo,
        vehicleType: formData.vehicleType,
        driverName: formData.driverName,
        driverPhone: formData.driverPhone,
        freightAmount: formData.freightAmount,
        estimatedArrivalDate: formData.estimatedArrivalDate,
        remark: formData.remark,
      })
    }
  } finally {
    submitLoading.value = false
  }
}

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      if (props.mode === 'edit' && props.initialData) {
        Object.assign(formData, {
          orderId: props.initialData.orderId,
          shipmentStatus: props.initialData.shipmentStatus ?? '',
          logisticsNo: props.initialData.logisticsNo ?? '',
          logisticsCompany: props.initialData.logisticsCompany ?? '',
          transportMode: props.initialData.transportMode ?? '',
          vehiclePlateNo: props.initialData.vehiclePlateNo ?? '',
          vehicleType: props.initialData.vehicleType ?? '',
          driverName: props.initialData.driverName ?? '',
          driverPhone: props.initialData.driverPhone ?? '',
          freightAmount: props.initialData.freightAmount,
          estimatedArrivalDate: props.initialData.estimatedArrivalDate ?? '',
          remark: props.initialData.remark ?? '',
        })
      } else {
        resetForm()
        if (props.initialOrderId) {
          formData.orderId = props.initialOrderId
        }
      }
    }
  },
)
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="dialogTitle"
    width="1100px"
    class="fts-dialog--wide"
    :close-on-click-modal="false"
    destroy-on-close
    lock-scroll="false"
    @update:model-value="emit('update:modelValue', $event)"
    @closed="resetForm"
  >
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px">
      <el-form-item v-if="mode === 'create'" label="采购订单" prop="orderId">
        <el-select
          v-model="formData.orderId"
          placeholder="请选择采购订单"
          filterable
          :teleported="false"
          :loading="orderOptionsLoading"
          style="width: 100%"
        >
          <el-option
            v-for="o in orderOptions"
            :key="o.purchaseOrderId"
            :label="`${o.orderNo} - ${o.supplierName}`"
            :value="o.purchaseOrderId"
          />
        </el-select>
      </el-form-item>

      <el-form-item v-if="mode === 'edit' && selectedOrder" label="采购订单">
        <el-input :model-value="`${selectedOrder.orderNo} - ${selectedOrder.supplierName}`" disabled />
      </el-form-item>

      <el-form-item label="发货状态" prop="shipmentStatus">
        <el-select
          v-model="formData.shipmentStatus"
          placeholder="请选择发货状态"
          :teleported="false"
          style="width: 100%"
        >
          <el-option
            v-for="opt in ShipmentStatusOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="物流单号">
        <el-input v-model="formData.logisticsNo" placeholder="请输入物流单号" clearable />
      </el-form-item>

      <el-form-item label="物流公司">
        <el-input v-model="formData.logisticsCompany" placeholder="请输入物流公司" clearable />
      </el-form-item>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="运输方式">
            <el-select
              v-model="formData.transportMode"
              placeholder="请选择运输方式"
              :teleported="false"
              style="width: 100%"
              clearable
            >
              <el-option
                v-for="opt in TransportModeOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="车辆类型">
            <el-select
              v-model="formData.vehicleType"
              placeholder="请选择车辆类型"
              :teleported="false"
              style="width: 100%"
              clearable
            >
              <el-option
                v-for="opt in VehicleTypeOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="车牌号">
            <el-input v-model="formData.vehiclePlateNo" placeholder="请输入车牌号" clearable />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="运费（元）">
            <el-input-number
              v-model="formData.freightAmount"
              :min="0"
              :precision="2"
              :controls="false"
              placeholder="请输入运费"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="司机姓名">
            <el-input v-model="formData.driverName" placeholder="请输入司机姓名" clearable />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="司机电话">
            <el-input v-model="formData.driverPhone" placeholder="请输入司机电话" clearable />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="预计到货日期" prop="estimatedArrivalDate">
        <el-date-picker
          v-model="formData.estimatedArrivalDate"
          type="date"
          placeholder="选择日期"
          :teleported="false"
          style="width: 100%"
          value-format="YYYY-MM-DD"
        />
      </el-form-item>

      <el-form-item label="备注">
        <el-input v-model="formData.remark" type="textarea" :rows="3" placeholder="请输入备注" />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
/* 表单对话框仅依赖 Element Plus 默认布局，无额外样式 */
</style>
