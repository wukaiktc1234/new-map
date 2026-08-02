<script setup lang="ts">
/**
 * 会员偏好编辑对话框子组件
 * 包含：饮食偏好、用餐偏好、沟通偏好 三组表单
 */

interface PrefEditForm {
  spiceLevel: string
  tasteTags: string[]
  allergies: string[]
  favoriteCategories: string[]
  preferredTimes: string[]
  preferredAreas: string[]
  smokeFree: boolean
  oftenWithChildren: boolean
  budgetRange: string
  smsEnabled: boolean
  wechatEnabled: boolean
  marketingEnabled: boolean
  birthdayReminderDays: number
}

const props = defineProps<{
  visible: boolean
  formData: PrefEditForm
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  submit: []
}>()
</script>

<template>
  <el-dialog
    :model-value="props.visible"
    title="编辑用户偏好"
    width="680px"
    destroy-on-close
    @update:model-value="emit('update:visible', $event)"
  >
    <el-form :model="props.formData" label-width="100px">
      <el-divider content-position="left">饮食偏好</el-divider>
      <el-form-item label="辣度">
        <el-select v-model="props.formData.spiceLevel" placeholder="请选择辣度" :teleported="false" style="width:100%">
          <el-option label="不吃辣" value="不吃辣" />
          <el-option label="微辣" value="微辣" />
          <el-option label="中辣" value="中辣" />
          <el-option label="重辣" value="重辣" />
        </el-select>
      </el-form-item>
      <el-form-item label="口味标签">
        <el-select v-model="props.formData.tasteTags" multiple allow-create filterable default-first-option :teleported="false" placeholder="输入后回车添加" style="width:100%">
          <el-option label="清淡" value="清淡" /><el-option label="鲜香" value="鲜香" />
          <el-option label="麻辣" value="麻辣" /><el-option label="酸辣" value="酸辣" />
          <el-option label="甜味" value="甜味" />
        </el-select>
      </el-form-item>
      <el-form-item label="禁忌食材">
        <el-select v-model="props.formData.allergies" multiple allow-create filterable default-first-option :teleported="false" placeholder="输入后回车添加" style="width:100%">
          <el-option label="花生" value="花生" /><el-option label="海鲜" value="海鲜" />
          <el-option label="牛奶" value="牛奶" /><el-option label="鸡蛋" value="鸡蛋" />
        </el-select>
      </el-form-item>
      <el-form-item label="常点分类">
        <el-select v-model="props.formData.favoriteCategories" multiple allow-create filterable default-first-option :teleported="false" placeholder="输入后回车添加" style="width:100%">
          <el-option label="川菜" value="川菜" /><el-option label="粤菜" value="粤菜" />
          <el-option label="海鲜" value="海鲜" /><el-option label="火锅" value="火锅" />
          <el-option label="日料" value="日料" /><el-option label="甜品" value="甜品" />
        </el-select>
      </el-form-item>

      <el-divider content-position="left">用餐偏好</el-divider>
      <el-form-item label="时段">
        <el-select v-model="props.formData.preferredTimes" multiple :teleported="false" placeholder="请选择" style="width:100%">
          <el-option label="午餐" value="午餐" /><el-option label="晚餐" value="晚餐" />
        </el-select>
      </el-form-item>
      <el-form-item label="区域">
        <el-select v-model="props.formData.preferredAreas" multiple allow-create filterable default-first-option :teleported="false" placeholder="请选择" style="width:100%">
          <el-option label="大厅" value="大厅" /><el-option label="包间" value="包间" />
          <el-option label="VIP包间" value="VIP包间" />
        </el-select>
      </el-form-item>
      <el-form-item label="无烟区">
        <el-switch v-model="props.formData.smokeFree" />
      </el-form-item>
      <el-form-item label="带儿童">
        <el-switch v-model="props.formData.oftenWithChildren" />
      </el-form-item>
      <el-form-item label="消费区间">
        <el-select v-model="props.formData.budgetRange" :teleported="false" placeholder="请选择" style="width:100%">
          <el-option label="100以内" value="0-100" /><el-option label="100-200" value="100-200" />
          <el-option label="200-300" value="200-300" /><el-option label="300-500" value="300-500" />
          <el-option label="500以上" value="500+" />
        </el-select>
      </el-form-item>

      <el-divider content-position="left">沟通偏好</el-divider>
      <el-form-item label="短信通知">
        <el-switch v-model="props.formData.smsEnabled" />
      </el-form-item>
      <el-form-item label="微信通知">
        <el-switch v-model="props.formData.wechatEnabled" />
      </el-form-item>
      <el-form-item label="营销推送">
        <el-switch v-model="props.formData.marketingEnabled" />
      </el-form-item>
      <el-form-item label="生日提醒">
        <el-input-number v-model="props.formData.birthdayReminderDays" :min="1" :max="30" /> 天前提醒
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="emit('update:visible', false)">取消</el-button>
      <el-button type="primary" @click="emit('submit')">保存</el-button>
    </template>
  </el-dialog>
</template>
