<script setup lang="ts">
/**
 * 会员详情对话框子组件
 * 包含：基本信息、用户偏好、消费记录、储值记录 四个Tab
 */
import StatusTag from '@/components/core/StatusTag.vue'
import { memberConverter } from '@/api/marketing'
import type { MemberInfo, MemberConsumeRecord, MemberRechargeRecord } from '@/types/member'
import type { MemberLevelInfo } from '@/types/member-level'
import { MemberGenderText, RegisterChannelText, PaymentMethodText } from '@/types/member'

const props = defineProps<{
  visible: boolean
  member: MemberInfo | null
  loading: boolean
  consumeRecords: MemberConsumeRecord[]
  rechargeRecords: MemberRechargeRecord[]
  levelOptions: MemberLevelInfo[]
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  'edit-preference': []
  'open-partial-refund': [row: MemberConsumeRecord]
  'open-discount': [row: MemberConsumeRecord]
  'open-recharge-refund': [row: MemberRechargeRecord]
}>()

/* ===== 工具函数 ===== */
function formatDate(dateStr: string): string {
  if (!dateStr) return '-'
  return dateStr.replace('T', ' ').substring(0, 19)
}

function getGenderLabel(gender: string): string {
  return MemberGenderText[gender] || '未知'
}

function getChannelLabel(channel: string): string {
  return RegisterChannelText[channel] || '其他'
}

function getPaymentLabel(method: string): string {
  return PaymentMethodText[method] || '其他'
}
</script>

<template>
  <el-dialog
    :model-value="props.visible"
    :title="props.member ? `会员详情 - ${props.member.nickname}` : '会员详情'"
    width="1000px"
    destroy-on-close
    :append-to-body="false"
    @update:model-value="emit('update:visible', $event)"
  >
    <div v-loading="props.loading" class="detail-dialog-content">
      <el-tabs v-if="props.member" type="border-card">
        <!-- Tab 1: 基本信息 -->
        <el-tab-pane label="基本信息">
          <el-descriptions :column="3" border size="default" class="detail-section">
            <template #title><span class="section-title">基本信息</span></template>
            <el-descriptions-item label="昵称">{{ props.member.nickname }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ props.member.phone }}</el-descriptions-item>
            <el-descriptions-item label="性别">{{ getGenderLabel(props.member.gender) }}</el-descriptions-item>
            <el-descriptions-item label="生日">{{ props.member.birthday || '-' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ props.member.email || '-' }}</el-descriptions-item>
          </el-descriptions>

          <el-descriptions :column="3" border size="default" class="detail-section">
            <template #title><span class="section-title">会员信息</span></template>
            <el-descriptions-item label="等级">
              <StatusTag :status="memberConverter.toLevelStatus(props.member.levelCode)" :label="props.member.levelName" size="small" />
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <StatusTag :status="memberConverter.toStatusTagStatus(props.member.status)" :label="memberConverter.toStatusLabel(props.member.status)" size="small" />
            </el-descriptions-item>
            <el-descriptions-item label="注册渠道">{{ getChannelLabel(props.member.registerChannel) }}</el-descriptions-item>
            <el-descriptions-item label="注册时间">{{ formatDate(props.member.createdAt) }}</el-descriptions-item>
            <el-descriptions-item label="最后到店">{{ formatDate(props.member.lastVisitTime) }}</el-descriptions-item>
          </el-descriptions>

          <el-descriptions :column="3" border size="default" class="detail-section">
            <template #title><span class="section-title">消费信息</span></template>
            <el-descriptions-item label="余额(元)">{{ props.member.balance }}</el-descriptions-item>
            <el-descriptions-item label="累计充值">{{ props.member.totalRecharge }}</el-descriptions-item>
            <el-descriptions-item label="累计消费">{{ props.member.totalConsume }}</el-descriptions-item>
            <el-descriptions-item label="订单数">{{ props.member.orderCount }}</el-descriptions-item>
            <el-descriptions-item label="客户分层">
              <StatusTag :status="memberConverter.toSegmentStatus(props.member.customerSegment)" :label="memberConverter.toSegmentLabel(props.member.customerSegment)" size="small" />
            </el-descriptions-item>
          </el-descriptions>

          <el-descriptions :column="3" border size="default" class="detail-section">
            <template #title><span class="section-title">RFM评分</span></template>
            <el-descriptions-item label="R(最近消费)">
              <el-rate v-model="props.member.rScore" disabled />
            </el-descriptions-item>
            <el-descriptions-item label="F(消费频率)">
              <el-rate v-model="props.member.fScore" disabled />
            </el-descriptions-item>
            <el-descriptions-item label="M(消费金额)">
              <el-rate v-model="props.member.mScore" disabled />
            </el-descriptions-item>
          </el-descriptions>

          <el-descriptions :column="1" border size="default" class="detail-section">
            <template #title><span class="section-title">标签</span></template>
            <el-descriptions-item>
              <StatusTag v-for="tag in props.member.tags" :key="tag" status="info" :label="tag" size="small" class="member-tag" />
              <span v-if="!props.member.tags?.length">暂无标签</span>
            </el-descriptions-item>
          </el-descriptions>

          <el-descriptions :column="1" border size="default" class="detail-section">
            <template #title><span class="section-title">备注</span></template>
            <el-descriptions-item>{{ props.member.remark || '暂无备注' }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>

        <!-- Tab 2: 用户偏好 -->
        <el-tab-pane label="用户偏好">
          <template v-if="props.member.preferences">
            <el-descriptions :column="2" border size="default" class="detail-section">
              <template #title><span class="section-title">饮食偏好</span></template>
              <el-descriptions-item label="辣度">{{ props.member.preferences.food?.spiceLevel || '-' }}</el-descriptions-item>
              <el-descriptions-item label="口味标签">
                <template v-if="props.member.preferences.food?.tasteTags?.length">
                  <StatusTag v-for="t in props.member.preferences.food.tasteTags" :key="t" status="info" :label="t" size="small" class="member-tag" />
                </template>
                <span v-else>-</span>
              </el-descriptions-item>
              <el-descriptions-item label="禁忌食材">
                <template v-if="props.member.preferences.food?.allergies?.length">
                  <StatusTag v-for="a in props.member.preferences.food.allergies" :key="a" status="error" :label="a" size="small" class="member-tag" />
                </template>
                <span v-else>-</span>
              </el-descriptions-item>
              <el-descriptions-item label="常点分类">
                <template v-if="props.member.preferences.food?.favoriteCategories?.length">
                  <StatusTag v-for="c in props.member.preferences.food.favoriteCategories" :key="c" status="info" :label="c" size="small" class="member-tag" />
                </template>
                <span v-else>-</span>
              </el-descriptions-item>
            </el-descriptions>

            <el-descriptions :column="2" border size="default" class="detail-section">
              <template #title><span class="section-title">用餐偏好</span></template>
              <el-descriptions-item label="时段">
                {{ props.member.preferences.dining?.preferredTimes?.join('、') || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="区域">
                {{ props.member.preferences.dining?.preferredAreas?.join('、') || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="无烟区">{{ props.member.preferences.dining?.smokeFree ? '是' : '否' }}</el-descriptions-item>
              <el-descriptions-item label="带儿童">{{ props.member.preferences.dining?.oftenWithChildren ? '是' : '否' }}</el-descriptions-item>
              <el-descriptions-item label="消费区间">{{ props.member.preferences.dining?.budgetRange || '-' }}</el-descriptions-item>
            </el-descriptions>

            <el-descriptions :column="2" border size="default" class="detail-section">
              <template #title><span class="section-title">沟通偏好</span></template>
              <el-descriptions-item label="短信通知">{{ props.member.preferences.communication?.smsEnabled ? '已开启' : '已关闭' }}</el-descriptions-item>
              <el-descriptions-item label="微信通知">{{ props.member.preferences.communication?.wechatEnabled ? '已开启' : '已关闭' }}</el-descriptions-item>
              <el-descriptions-item label="营销推送">{{ props.member.preferences.communication?.marketingEnabled ? '已开启' : '已关闭' }}</el-descriptions-item>
              <el-descriptions-item label="生日提醒">{{ props.member.preferences.communication?.birthdayReminderDays ? `提前${props.member.preferences.communication.birthdayReminderDays}天` : '-' }}</el-descriptions-item>
            </el-descriptions>

            <div class="pref-source">
              数据来源：{{ props.member.preferences.source || '-' }} | 更新时间：{{ formatDate(props.member.preferences.updatedAt) }} | 更新人：{{ props.member.preferences.updatedBy || '-' }}
            </div>
          </template>
          <el-empty v-else description="暂无偏好数据" />

          <div class="pref-action">
            <el-button type="primary" @click="emit('edit-preference')">编辑偏好</el-button>
          </div>
        </el-tab-pane>

        <!-- Tab 3: 消费记录 -->
        <el-tab-pane label="消费记录">
          <el-table :data="props.consumeRecords" stripe size="default" style="width:100%">
            <el-table-column prop="orderNo" label="订单号" width="160" />
            <el-table-column prop="amount" label="消费金额" width="100" align="right" />
            <el-table-column prop="paymentMethod" label="支付方式" width="100">
              <template #default="{ row }">{{ getPaymentLabel(row.paymentMethod) }}</template>
            </el-table-column>
            <el-table-column prop="balanceDeducted" label="余额抵扣" width="100" align="right" />
            <el-table-column prop="storeName" label="门店" width="100" />
            <el-table-column prop="consumeTime" label="时间" width="160">
              <template #default="{ row }">{{ formatDate(row.consumeTime) }}</template>
            </el-table-column>
            <el-table-column prop="operatorName" label="操作人" width="80" />
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button link type="danger" size="small" @click="emit('open-partial-refund', row)">部分退款</el-button>
                <el-button link type="primary" size="small" @click="emit('open-discount', row)">折扣</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- Tab 4: 储值记录 -->
        <el-tab-pane label="储值记录">
          <el-table :data="props.rechargeRecords" stripe size="default" style="width:100%">
            <el-table-column prop="recordNo" label="流水号" width="160" />
            <el-table-column prop="planName" label="充值方案" width="100" />
            <el-table-column prop="rechargeAmount" label="充值金额" width="100" align="right" />
            <el-table-column prop="bonusAmount" label="赠送金额" width="100" align="right" />
            <el-table-column prop="paymentMethod" label="支付方式" width="100">
              <template #default="{ row }">{{ getPaymentLabel(row.paymentMethod) }}</template>
            </el-table-column>
            <el-table-column prop="paymentStatus" label="支付状态" width="100">
              <template #default="{ row }">
                <StatusTag :status="memberConverter.toRechargePaymentStatus(row.paymentStatus)" :label="memberConverter.toRechargePaymentLabel(row.paymentStatus)" size="small" />
              </template>
            </el-table-column>
            <el-table-column prop="operateUserName" label="操作人" width="80" />
            <el-table-column prop="createTime" label="时间" width="160">
              <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="80" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.paymentStatus === 'paid'" link type="danger" size="small" @click="emit('open-recharge-refund', row)">退款</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>
  </el-dialog>
</template>

<style scoped lang="scss">
.detail-dialog-content {
  :deep(.el-descriptions) {
    margin-bottom: var(--fts-space-4);
  }

  :deep(.el-tab-pane) {
    padding: var(--fts-space-3);
  }
}

.detail-section {
  margin-bottom: var(--fts-space-4);
}

.section-title {
  font-weight: 600;
  font-size: 14px;
  color: var(--fts-text-primary);
}

.member-tag {
  margin-right: var(--fts-space-2);
  margin-bottom: var(--fts-space-1);
}

.pref-source {
  margin: var(--fts-space-3) 0;
  font-size: 12px;
  color: var(--fts-text-secondary);
}

.pref-action {
  margin-top: var(--fts-space-4);
  text-align: right;
}
</style>
