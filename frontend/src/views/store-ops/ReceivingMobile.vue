<script setup lang="ts">
/**
 * 手持收货（H5 移动端）页面
 * 卡片式逐项验收：接收/拒收大按钮 + 称重数量 + 批次效期 + 质检，触控优先
 * 入口：门店收货/仓库入库页面「手持收货」按钮，或手机浏览器直接访问
 */
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, ArrowDown, Check, Close, Camera, Picture, Aim, Refresh, SwitchButton, Box, Clock, User, Delete, Shop, Upload, Filter } from '@element-plus/icons-vue'
import { Html5Qrcode } from 'html5-qrcode'
import { purchaseArrivalApi } from '@/api/purchase/arrival'
import { receiptConfirmationApi } from '@/api/receipt-confirmation'
import { authApi } from '@/api/auth'
import { supplierApi } from '@/api/purchase/supplier'
import SignaturePad from '@/components/business/receipt-confirmation/SignaturePad.vue'
import { materialArchiveApi } from '@/api/purchase/archive'
import { uploadImage } from '@/api/upload'
import { resolveUploadUrl } from '@/utils/upload-url'
import { readExif, requestGeolocation } from '@/utils/exif'
import type { ExifMeta } from '@/utils/exif'
import { post } from '@/api/request'
import { decodeEan13 } from '@/utils/barcode-ean13'
import {
  extractCorrectedDate,
  extractShelfLife,
  addDuration,
  isValidDateStr,
  diffDays,
} from '@/utils/date-corrector'
import type {
  PurchaseArrivalInfo,
  PurchaseArrivalItem,
  ReceiptConfirmationInfo,
  ReceiptConfirmationItemForm,
  ReceiptSignatureInfo,
  ReceiptEvidenceForm,
  ReceiverType,
} from '@/types/purchase-arrival'
import { QualityCheckResultOptions } from '@/types/purchase-arrival'

const route = useRoute()
const router = useRouter()

/** 手持端版本标记（用于确认手机加载的是最新代码） */
const APP_VERSION = 'v2.10.0'

const receiverType = (route.query.receiverType as ReceiverType) || 'STORE'

/** 当前登录用户的门店名称（/v1/auth/me 取到后精确显示，替代模糊的「门店收货」） */
const myStoreName = ref('')

/** 场景副标题：门店 → 显示确切门店名（取不到时回退「门店收货」）；仓库 → 「仓库入库」 */
const receiverLabel = computed(() => {
  if (receiverType === 'WAREHOUSE') return '仓库入库'
  return myStoreName.value || '门店收货'
})

/** 待收货到货单列表 */
const arrivalList = ref<PurchaseArrivalInfo[]>([])
const listLoading = ref(false)

/** 当前验收中的到货单与明细 */
const activeArrival = ref<PurchaseArrivalInfo | null>(null)
const items = ref<ReceiptConfirmationItemForm[]>([])
const remark = ref('')
const submitting = ref(false)

/** 收货确认成功后进入「双签存证」步骤（与收货确认单详情的双签一致） */
const signingConfirmation = ref<ReceiptConfirmationInfo | null>(null)
const signReceiverSig = ref('')
const signSupplierSig = ref('')
const signSupplierName = ref('')
const signing = ref(false)

/** 双签分步：1-收货方 2-供应商（独立步骤，不挤在同一页） */
const signStep = ref<1 | 2>(1)
const receiverSigned = ref(false)
const supplierSigned = ref(false)

/** 整单质检结果（由各商品质检结果聚合：全合格=1，含不合格=2，其余=3） */
const qualityCheckResult = computed<number | undefined>(() => {
  const set = items.value.map(i => i.qualityCheckResult).filter((v): v is number => v != null)
  if (set.length === 0) return undefined
  if (set.every(v => v === 1)) return 1
  if (set.some(v => v === 2)) return 2
  return 3
})

/** 整单条形码信息（批次号在外箱标签上，按商品逐一录入；此处为货物识别条码） */
const barcodeNo = ref('')
const barcodeMatch = ref('')
const matchedItemIndex = ref(-1)

/** 批次 OCR/相册识别当前作用的目标商品下标（每张商品卡内各自录入批次信息） */
const batchTargetIndex = ref(0)

/** 整单批次快捷录入：同车同批次时一次性应用到所有接收商品 */
const quickBatch = reactive({ batchNo: '', productionDate: '', expiryDate: '' })

function setBatchTarget(index: number): void {
  batchTargetIndex.value = index
}

// ==================== 草稿持久化（localStorage 断点恢复） ====================

/** 草稿存储 key（按收货方类型区分，避免门店/仓库相互污染） */
const DRAFT_KEY = `fts_receiving_draft_${receiverType}`

/** 图形化压缩：文件 → 小尺寸 JPEG DataURL（用于离线暂存，避免超出 localStorage 容量） */
function fileToCompressedDataUrl(file: File, maxWidth = 900, quality = 0.75): Promise<string> {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file)
    const img = new Image()
    img.onload = () => {
      URL.revokeObjectURL(url)
      const scale = img.naturalWidth > maxWidth ? maxWidth / img.naturalWidth : 1
      const canvas = document.createElement('canvas')
      canvas.width = Math.max(1, Math.round(img.naturalWidth * scale))
      canvas.height = Math.max(1, Math.round(img.naturalHeight * scale))
      const ctx = canvas.getContext('2d')
      if (!ctx) return resolve('')
      ctx.drawImage(img, 0, 0, canvas.width, canvas.height)
      resolve(canvas.toDataURL('image/jpeg', quality))
    }
    img.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('图片加载失败'))
    }
    img.src = url
  })
}

/** DataURL → File（重传离线暂存照片时构造文件） */
function dataUrlToFile(dataUrl: string, name: string): File {
  const comma = dataUrl.indexOf(',')
  const meta = comma > 0 ? dataUrl.slice(0, comma) : ''
  const mime = /data:(.*?);/.exec(meta)?.[1] || 'image/jpeg'
  const bin = atob(dataUrl.slice(comma + 1))
  const bytes = new Uint8Array(bin.length)
  for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i)
  return new File([bytes], name, { type: mime })
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

/** 上传失败重试（网络抖动自动重试，最多 3 次，指数退避） */
async function uploadWithRetry(file: File, fileName: string, retries = 3): Promise<string> {
  let lastErr: unknown
  for (let i = 0; i < retries; i++) {
    try {
      return await uploadImage(file, fileName)
    } catch (err) {
      lastErr = err
      await sleep(600 * (i + 1))
    }
  }
  throw lastErr
}

interface ReceiptDraft {
  arrivalId: string | number
  savedAt: number
  items: ReceiptConfirmationItemForm[]
  remark: string
  barcodeNo: string
  stepIndex: number
}

function saveDraft(): void {
  const a = activeArrival.value
  if (!a) return
  const draft: ReceiptDraft = {
    arrivalId: a.arrivalId,
    savedAt: Date.now(),
    items: items.value,
    remark: remark.value,
    barcodeNo: barcodeNo.value,
    stepIndex: stepIndex.value,
  }
  try {
    localStorage.setItem(DRAFT_KEY, JSON.stringify(draft))
  } catch {
    // localStorage 容量不足时静默放弃（正式上传后的照片不参与草稿）
  }
}

function loadDraft(): ReceiptDraft | null {
  try {
    const raw = localStorage.getItem(DRAFT_KEY)
    if (!raw) return null
    const draft = JSON.parse(raw) as ReceiptDraft
    if (!draft?.items?.length || !draft.arrivalId) return null
    return draft
  } catch {
    return null
  }
}

function clearDraft(): void {
  try {
    localStorage.removeItem(DRAFT_KEY)
  } catch {
    // 忽略清除异常
  }
}

/** 草稿恢复横幅（进入验收视图时有上次未提交的记录则提示继续） */
const restoreHint = ref('')

async function checkRestoreHint(arrivalId: string | number | undefined): Promise<void> {
  restoreHint.value = ''
  if (!arrivalId) return
  const draft = loadDraft()
  if (!draft || String(draft.arrivalId) !== String(arrivalId)) return
  const entered = draft.items.filter((i) => (i.quantity ?? 0) > 0 || i.accept === 'reject' || (i.evidence?.length ?? 0) > 0).length
  if (entered === 0) return
  restoreHint.value = `检测到上次未提交的录入进度（${entered} 项已录入），可继续完成收货`
}

function applyDraft(): void {
  const draft = loadDraft()
  if (!draft) return
  items.value = draft.items
  remark.value = draft.remark
  barcodeNo.value = draft.barcodeNo
  stepIndex.value = Math.min(draft.stepIndex, Math.max(0, draft.items.length - 1))
  activeIndex.value = stepIndex.value
  restoreHint.value = ''
  ElMessage.success('已恢复上次录入进度')
}

function discardDraft(): void {
  clearDraft()
  restoreHint.value = ''
  // 保留当前到货单，重新按空白明细开始录入，避免误操作清空页面
  if (activeArrival.value?.items?.length) {
    items.value = activeArrival.value.items.map(buildItem)
  }
  ElMessage.success('已放弃草稿，请重新录入')
}

/** 将草稿中「待上传」的离线照片重传到服务器（提交前/联网恢复时调用） */
async function flushPendingUploads(): Promise<boolean> {
  let all = true
  for (const item of items.value) {
    for (const ev of item.evidence ?? []) {
      if (!ev.pendingUpload || !ev.localDataUrl) continue
      const file = dataUrlToFile(ev.localDataUrl, `evidence-${Date.now()}.jpg`)
      try {
        const url = await uploadWithRetry(file, file.name)
        ev.photoUrl = url
        ev.pendingUpload = false
        ev.localDataUrl = undefined
      } catch {
        all = false
      }
    }
  }
  return all
}

function applyQuickBatch(): void {
  let applied = 0
  for (const item of items.value) {
    if (item.accept === 'reject') continue
    if (quickBatch.batchNo) item.batchNo = quickBatch.batchNo
    if (quickBatch.productionDate) item.productionDate = quickBatch.productionDate
    if (quickBatch.expiryDate) item.expiryDate = quickBatch.expiryDate
    applied++
  }
  if (applied > 0) {
    ElMessage.success(`已应用到 ${applied} 个接收商品`)
  } else {
    ElMessage.warning('当前没有待接收的商品')
  }
}

// ==================== 多商品导航（目录索引 / 进度 / 定位） ====================

const activeIndex = ref(-1)

/** 商品完成校验：接收=数量>0且批次+质检齐；拒收=数量>0且原因+留证齐 */
function isItemDone(item: ReceiptConfirmationItemForm): boolean {
  if (((item.quantity ?? 0) > 0) === false) return false
  if (item.accept === 'reject') return !!item.remark?.trim() && !!item.evidence?.length
  return !!item.accept && !!item.batchNo?.trim() && !!item.qualityCheckResult
}

const itemState = (item: ReceiptConfirmationItemForm): 'done' | 'pending' | 'reject' => {
  if (item.accept === 'reject') return 'reject'
  return isItemDone(item) ? 'done' : 'pending'
}

/** 已录入实收的项数（数量>0即计progress，实时反馈输入进度） */
const enteredCount = computed(() => items.value.filter(i => (i.quantity ?? 0) > 0).length)

const progressPercent = computed(() => {
  if (items.value.length === 0) return '0%'
  return `${Math.round((enteredCount.value / items.value.length) * 100)}%`
})

/** 点击目录定位到指定商品（单卡聚焦：切换 stepIndex 并滚动到卡片与目录项） */
function scrollToItem(index: number): void {
  if (index < 0 || index >= items.value.length) return
  gotoStep(index)
}

// ==================== 单卡聚焦模式（参考 PDA 手持收货 V4） ====================

/** 当前聚焦商品下标 */
const stepIndex = ref(0)

function gotoStep(index: number): void {
  if (index < 0 || index >= items.value.length) return
  stepIndex.value = index
  // 滚动卡片到可视区
  const card = document.getElementById(`item-card-${index}`)
  card?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  // 目录项自动滚入可视区（横向）
  const chip = document.getElementById(`chip-${index}`)
  chip?.scrollIntoView({ behavior: 'smooth', inline: 'nearest', block: 'nearest' })
}

/** 是否存在待处理项（未完成项） */
function isPending(item: ReceiptConfirmationItemForm): boolean {
  return !isItemDone(item)
}

/** 向前/向后跳转至最近的待处理项 */
const hasPrevPending = computed(() => {
  for (let i = stepIndex.value - 1; i >= 0; i--) {
    if (isPending(items.value[i])) return true
  }
  return false
})

const hasNextPending = computed(() => {
  for (let i = stepIndex.value + 1; i < items.value.length; i++) {
    if (isPending(items.value[i])) return true
  }
  return false
})

function gotoPending(direction: 1 | -1): void {
  for (let i = stepIndex.value + direction; i >= 0 && i < items.value.length; i += direction) {
    if (isPending(items.value[i])) {
      gotoStep(i)
      return
    }
  }
}

// ==================== 快速定位（扫码/搜索跳转） ====================

/** 顶部折叠面板展开状态 */
const quickBatchExpanded = ref(false)
const locatorExpanded = ref(false)

/** 快速定位：关键词（匹配商品名/规格） */
const locatorKeyword = ref('')
const locatorHint = ref('💡 扫码或输入关键词，匹配的商品将高亮并自动跳转')

const locatorMatches = computed(() => {
  const kw = locatorKeyword.value.trim().toLowerCase()
  if (!kw) return []
  return items.value
    .map((item, index) => ({ item, index }))
    .filter(({ item }) => {
      const name = (item.materialName || '').toLowerCase()
      const spec = (item.specification || '').toLowerCase()
      return name.includes(kw) || spec.includes(kw)
    })
})

function onLocatorInput(): void {
  const kw = locatorKeyword.value.trim().toLowerCase()
  if (!kw) {
    locatorHint.value = '💡 扫码或输入关键词，匹配的商品将高亮并自动跳转'
    return
  }
  const m = locatorMatches.value
  if (m.length === 0) {
    locatorHint.value = `❌ 未找到匹配「${locatorKeyword.value}」的商品，请重新输入或扫码`
  } else if (m.length === 1) {
    locatorHint.value = `✅ 匹配 1 个商品「${m[0].item.materialName}」，点击下方结果跳转`
  } else {
    locatorHint.value = `✅ 找到 ${m.length} 个匹配商品`
  }
}

function jumpToLocatorMatch(index: number): void {
  locatorKeyword.value = ''
  locatorExpanded.value = false
  scrollToItem(index)
}

/** 进入验收时定位面板默认收起（用户需要时点开，避免占屏） */
function flashLocatorHint(): void {
  // 保持折叠（快速定位面板）
}

/** 滚动高亮当前目录项（越顶定位） */
let spyRaf = 0
function onNavScroll(): void {
  if (spyRaf) return
  spyRaf = requestAnimationFrame(() => {
    spyRaf = 0
    const cards = document.querySelectorAll<HTMLElement>('.item-card')
    let found = -1
    for (let i = 0; i < cards.length; i++) {
      const rect = cards[i].getBoundingClientRect()
      if (rect.height === 0) continue // 过滤隐藏
      if (rect.top <= 140) found = i
      else break
    }
    if (found !== activeIndex.value) activeIndex.value = found
  })
}

/** 扫码/输入条形码后：按条码查物料档案，匹配到货明细行（快速对比货品） */
async function matchBarcodeToItem(code: string): Promise<void> {
  matchedItemIndex.value = -1
  barcodeMatch.value = ''
  const value = (code || '').trim()
  if (!value) return
  try {
    const res = await materialArchiveApi.getList({ barcode: value, page: 1, size: 5 })
    const matched = res.records.find((m) => m.barcode === value)
    if (!matched) {
      barcodeMatch.value = `未找到该条码的物料档案（散装/未建档商品可跳过）；如货物目视可对上，请在下方点「关联此物料」`
      return
    }
    const idx = items.value.findIndex((item) => String(item.materialId) === String(matched.materialId))
    if (idx >= 0) {
      matchedItemIndex.value = idx
      barcodeMatch.value = `已匹配：${matched.materialName}${matched.spec ? `（${matched.spec}）` : ''}`
      // 扫码命中：收起定位面板并自动跳转到匹配商品
      locatorKeyword.value = ''
      locatorExpanded.value = false
      scrollToItem(idx)
    } else {
      barcodeMatch.value = `条码对应物料「${matched.materialName}」不在本到货单明细中，可在下方点「关联此物料」人工确认`
    }
  } catch {
    barcodeMatch.value = '条码匹配查询失败，可点「关联此物料」人工确认'
  }
}

/** 人工关联：货物目视对得上但条码匹配失败时，手动把条码关联到指定明细行（必须拍照留证） */
const manualMatchDialog = ref(false)
const manualMatchIndex = ref(-1)
const manualMatchReason = ref('')
const manualMatchPhoto = ref<ReceiptEvidenceForm | null>(null)
const manualMatchUploading = ref(false)

function manualMatchItem(index: number): void {
  manualMatchIndex.value = index
  manualMatchReason.value = ''
  manualMatchPhoto.value = null
  manualMatchDialog.value = true
}

async function onManualCameraChange(e: Event): Promise<void> {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  manualMatchUploading.value = true
  try {
    const photo = await captureEvidence(file, 'MANUAL_MATCH')
    if (photo) manualMatchPhoto.value = photo
  } finally {
    manualMatchUploading.value = false
  }
}

async function confirmManualMatch(): Promise<void> {
  if (!manualMatchReason.value.trim()) {
    ElMessage.warning('请填写人工关联原因（如：货不对码，目视确认为该物料）')
    return
  }
  if (!manualMatchPhoto.value) {
    ElMessage.warning('必须现场拍照留证（只能调用相机拍摄，禁止相册照片）')
    return
  }
  const index = manualMatchIndex.value
  const item = items.value[index]
  if (!item) return
  item.manualMatched = true
  item.evidence = [...(item.evidence ?? []), manualMatchPhoto.value]
  if (!item.remark?.trim()) item.remark = manualMatchReason.value.trim()
  matchedItemIndex.value = index
  barcodeMatch.value = `已人工关联：${item.materialName}（条码 ${barcodeNo.value || ''}）已拍照留证`
  manualMatchDialog.value = false
  ElMessage.success(`已关联到：${item.materialName}`)
}

/** 是否显示手动关联按钮（条码匹配未成功时） */
function canManualMatch(): boolean {
  return !!barcodeMatch.value && matchedItemIndex.value < 0
}

/** 拒收项拍照留证 */
const rejectEvidenceTarget = ref<{ index: number; type: 'QC_REJECT' } | null>(null)

function startRejectEvidence(index: number): void {
  rejectEvidenceTarget.value = { index, type: 'QC_REJECT' }
}

async function onRejectEvidenceFileChange(e: Event): Promise<void> {
  const target = rejectEvidenceTarget.value
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  rejectEvidenceTarget.value = null
  if (!target || !file) return Promise.resolve()
  const item = items.value[target.index]
  if (!item) return Promise.resolve()
  return captureEvidence(file, 'QC_REJECT').then((photo) => {
    if (!photo) return
    item.evidence = [...(item.evidence ?? []), photo]
    ElMessage.success('拒收照片已留存')
  })
}

/** 把当前时刻格式化为 yyyy-MM-dd HH:mm:ss */
function formatNowDateTime(): string {
  const d = new Date()
  const pad = (n: number): string => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

/** 从 UA 提取终端机型（兜底设备型号，Win/Mac 通用显示） */
function detectDeviceModel(): string {
  const ua = navigator.userAgent || ''
  const iphone = ua.match(/iPhone[\sOS\s]?\d*[\sOS\s]*\d*/i)?.[0] || ''
  if (iphone) return `Apple ${iphone.replace(/\s+/g, ' ')}`
  const androidMatch = ua.match(/Android\s[\d.]+;/i)?.[0] || ''
  const deviceMatch = ua.match(/;/i) ? (ua.match(/; ([\w\s-]+?)(;|\sBuild\/|\)) ?/i)?.[1] ?? '') : ''
  if (androidMatch) {
    const device = deviceMatch.trim()
    return device ? `Android ${device}` : 'Android 设备'
  }
  if (ua.includes('Windows')) return 'Windows 设备'
  if (ua.includes('Macintosh')) return 'macOS 设备'
  return '未知终端'
}

/** 统一拍照留证：直接调系统相机拍摄 → 上传（失败自动重试/离线暂存本地） → 提取 Exif 元数据 */
async function captureEvidence(file: File, type: 'QC_REJECT' | 'MANUAL_MATCH'): Promise<ReceiptEvidenceForm | null> {
  try {
    const meta = await readExif(file).catch(() => ({}) as ExifMeta)
    const gps = meta.gps || (await requestGeolocation().catch(() => ''))
    // EXIF 兜底：多数 WebView/系统相机重编码会删除 EXIF，导致证据链"元数据缺失"。
    // 此处以"拍摄动作发生时刻 + 终端 UA 机型"兜底，保证证据列不空标。
    const fallbackTakenAt = formatNowDateTime()
    const fallbackDevice = detectDeviceModel()
    const evidenceMeta = {
      photoTakenAt: meta.takenAt || fallbackTakenAt,
      deviceModel: meta.deviceModel || fallbackDevice,
      gps: gps || undefined,
    }
    try {
      const url = await uploadWithRetry(file, file.name)
      return {
        evidenceType: type,
        photoUrl: url,
        ...evidenceMeta,
      }
    } catch {
      // 上传失败（离线/网络差）：压缩暂存本地，恢复后自动重传，不再丢照片
      const localDataUrl = await fileToCompressedDataUrl(file).catch(() => '')
      if (!localDataUrl) {
        ElMessage.error('照片上传失败，请重试')
        return null
      }
      ElMessage.warning('网络不可用，照片已暂存本地，恢复网络后自动上传')
      return {
        evidenceType: type,
        photoUrl: '',
        localDataUrl,
        pendingUpload: true,
        ...evidenceMeta,
      }
    }
  } catch {
    ElMessage.error('照片处理失败，请重试')
    return null
  }
}

function removeEvidence(item: ReceiptConfirmationItemForm, evIdx: number): void {
  item.evidence?.splice(evIdx, 1)
}

/** 识别结果疑似异常（网址/超长/含空格）时要求确认，防止误填 */
function isSuspiciousScan(text: string): boolean {
  const t = (text || '').trim().toLowerCase()
  if (!t) return true
  if (t.startsWith('http://') || t.startsWith('https://') || t.startsWith('www.')) return true
  if (t.includes(' ')) return true
  if (t.length > 60) return true
  return false
}

async function confirmScanResult(text: string): Promise<string | null> {
  const value = (text || '').trim()
  if (isSuspiciousScan(value)) {
    try {
      await ElMessageBox.confirm(
        `识别到疑似异常内容：「${value}」\n确认仍要填入条形码吗？`,
        '识别内容确认',
        { confirmButtonText: '仍要填入', cancelButtonText: '放弃', type: 'warning' },
      )
      return value
    } catch {
      return null
    }
  }
  return value
}

const pendingStatuses = ['pending', 'receiving', 'partial_received']

async function loadArrivals(): Promise<void> {
  listLoading.value = true
  try {
    const res = await purchaseArrivalApi.getList({
      receiverType,
      page: 1,
      size: 50,
    } as never)
    arrivalList.value = (res.records || []).filter((a) => pendingStatuses.includes(a.status))
  } catch {
    arrivalList.value = []
  } finally {
    listLoading.value = false
  }
}

/** 拉取当前用户的门店名称（/v1/auth/me），用于头部精确显示；失败静默回退「门店收货」 */
async function loadMyStoreName(): Promise<void> {
  try {
    const me = await authApi.getCurrentUser()
    if (me?.storeName) myStoreName.value = me.storeName
  } catch {
    // 静默：取不到真实门店名时沿用「门店收货」占位
  }
}

/** 草稿中待上传的离线照片数（「我的」页统计，来自 localStorage 草稿） */
const pendingUploadCount = ref(0)

function refreshPendingUploadCount(): void {
  const draft = loadDraft()
  const evidences = draft?.items?.flatMap((i) => i.evidence ?? []) ?? []
  pendingUploadCount.value = evidences.filter((e) => e.pendingUpload).length
}

/** 手动将草稿中的离线照片上传（成功回写草稿；单张失败不中断） */
let uploadingOffline = false
async function uploadOfflineNow(): Promise<void> {
  if (uploadingOffline) return
  uploadingOffline = true
  try {
    const draft = loadDraft()
    if (!draft) {
      ElMessage.info('当前没有离线数据')
      return
    }
    let uploaded = 0
    for (const item of draft.items) {
      for (const ev of item.evidence ?? []) {
        if (!ev.pendingUpload || !ev.localDataUrl) continue
        const file = dataUrlToFile(ev.localDataUrl, `evidence-${Date.now()}-${uploaded}.jpg`)
        try {
          const url = await uploadWithRetry(file, file.name)
          ev.photoUrl = url
          ev.localDataUrl = undefined
          ev.pendingUpload = false
          uploaded++
        } catch {
          // 单张失败不中断，其余继续尝试
        }
      }
    }
    if (uploaded > 0) {
      draft.savedAt = Date.now()
      try {
        localStorage.setItem(DRAFT_KEY, JSON.stringify(draft))
      } catch {
        // 忽略容量异常
      }
    }
    refreshPendingUploadCount()
    ElMessage.success(uploaded > 0 ? `已上传 ${uploaded} 张离线照片` : '无待上传照片')
  } finally {
    uploadingOffline = false
  }
}

// ==================== 历史收货查询 ====================

const listTab = ref<'pending' | 'history' | 'mine'>('pending')
const historyList = ref<ReceiptConfirmationInfo[]>([])
const historyLoading = ref(false)
const historyPage = ref(1)
const historyTotal = ref(0)
const historyFinished = ref(false)
const historyDetail = ref<ReceiptConfirmationInfo | null>(null)
const historyDetailSignatures = ref<ReceiptSignatureInfo[]>([])
const historyDetailSigLoading = ref(false)

/** 全屏图片查看器：独立手势状态（缩放不回正、放大后可拖动、单击关闭、双击放大） */
const photoViewer = ref<{ url: string; scale: number; translateX: number; translateY: number } | null>(null)
let pvGesture = { mode: 'none' as 'none' | 'pinch' | 'pan', startDist: 0, startScale: 1, startX: 0, startY: 0, startTx: 0, startTy: 0 }
let pvLastTap = 0

function openPhotoViewer(url: string): void {
  photoViewer.value = { url, scale: 1, translateX: 0, translateY: 0 }
}

function closePhotoViewer(): void {
  photoViewer.value = null
  pvGesture.mode = 'none'
}

function onPhotoViewerTouchStart(e: TouchEvent): void {
  if (!photoViewer.value) return
  if (e.touches.length >= 2) {
    pvGesture.mode = 'pinch'
    const dx = e.touches[0].clientX - e.touches[1].clientX
    const dy = e.touches[0].clientY - e.touches[1].clientY
    pvGesture.startDist = Math.sqrt(dx * dx + dy * dy)
    pvGesture.startScale = photoViewer.value.scale
  } else if (e.touches.length === 1) {
    if (photoViewer.value.scale > 1) {
      pvGesture.mode = 'pan'
      pvGesture.startX = e.touches[0].clientX
      pvGesture.startY = e.touches[0].clientY
      pvGesture.startTx = photoViewer.value.translateX
      pvGesture.startTy = photoViewer.value.translateY
    } else {
      pvGesture.mode = 'none'
      // 记录单击起点（用于 tap 判定，不缩放时允许单击关闭）
      pvGesture.startX = e.touches[0].clientX
      pvGesture.startY = e.touches[0].clientY
    }
  }
}

function onPhotoViewerTouchMove(e: TouchEvent): void {
  if (!photoViewer.value) return
  e.preventDefault()
  if (pvGesture.mode === 'pinch' && e.touches.length >= 2) {
    const dx = e.touches[0].clientX - e.touches[1].clientX
    const dy = e.touches[0].clientY - e.touches[1].clientY
    const dist = Math.sqrt(dx * dx + dy * dy)
    if (pvGesture.startDist > 0) {
      let next = pvGesture.startScale * (dist / pvGesture.startDist)
      next = Math.min(5, Math.max(1, next))
      photoViewer.value.scale = next
      // 缩放后限制平移范围
      clampPan()
    }
  } else if (pvGesture.mode === 'pan' && e.touches.length === 1) {
    photoViewer.value.translateX = pvGesture.startTx + (e.touches[0].clientX - pvGesture.startX)
    photoViewer.value.translateY = pvGesture.startTy + (e.touches[0].clientY - pvGesture.startY)
    clampPan()
  }
}

/** 限制平移范围：基于图片实际渲染尺寸（缩放后超出视口的部分可平移，但不能完全出屏） */
function clampPan(): void {
  const pv = photoViewer.value
  if (!pv) return
  const vw = window.innerWidth
  const vh = window.innerHeight
  const img = document.querySelector('.photo-viewer__img') as HTMLElement | null
  if (!img) return
  const iw = img.offsetWidth
  const ih = img.offsetHeight
  // 缩放后图片实际尺寸超出视口的量即最大平移（保留 10% 余量，不能完全出屏）
  const limitX = Math.max(0, (iw * pv.scale - vw) / 2) + vw * 0.1
  const limitY = Math.max(0, (ih * pv.scale - vh) / 2) + vh * 0.1
  pv.translateX = Math.min(limitX, Math.max(-limitX, pv.translateX))
  pv.translateY = Math.min(limitY, Math.max(-limitY, pv.translateY))
}

function onPhotoViewerTouchEnd(e: TouchEvent): void {
  if (!photoViewer.value) return
  // 双指结束 → 手势重置（不触发 tap）
  if (e.touches.length === 0 && pvGesture.mode === 'pinch') {
    pvGesture.mode = 'none'
    return
  }
  // 单指结束：判定 tap（无论是否 pan 模式，无移动即 tap）
  const touch = e.changedTouches[0]
  if (!touch) {
    pvGesture.mode = 'none'
    return
  }
  const moved = Math.abs(touch.clientX - pvGesture.startX) + Math.abs(touch.clientY - pvGesture.startY)
  if (moved < 10) {
    const now = Date.now()
    if (now - pvLastTap < 300) {
      // 双击：放大/缩小（放大状态双击缩小）
      pvLastTap = 0
      photoViewer.value.scale = photoViewer.value.scale > 1 ? 1 : 2.5
      photoViewer.value.translateX = 0
      photoViewer.value.translateY = 0
    } else {
      pvLastTap = now
      window.setTimeout(() => {
        if (pvLastTap === now) {
          photoViewer.value = null
        }
      }, 320)
    }
  }
  pvGesture.mode = 'none'
}

/** 横屏书写覆盖层：target = 'receiver' | 'supplier' */
const landscapePad = ref<'receiver' | 'supplier' | null>(null)
const landscapePadRef = ref<InstanceType<typeof SignaturePad> | null>(null)
/** 横屏签名区画布高度（rotate 容器内：原高=视觉宽度，占满横屏视觉宽） */
const landscapePadHeight = ref(300)
if (typeof window !== 'undefined') {
  landscapePadHeight.value = Math.max(240, Math.round(window.innerWidth - 90))
}

function openLandscapePad(target: 'receiver' | 'supplier'): void {
  landscapePad.value = target
  void lockOrientation(true)
}

function closeLandscapePad(): void {
  landscapePad.value = null
}

/** 横屏书写完成：回填对应签名并关闭覆盖层 */
function confirmLandscapePad(): void {
  if (!landscapePad.value) return
  landscapePad.value = null
  void lockOrientation(false)
}

/** 横屏书写：清空当前签名 */
function clearLandscapePad(): void {
  landscapePadRef.value?.clear()
  if (landscapePad.value === 'receiver') {
    signReceiverSig.value = ''
  } else {
    signSupplierSig.value = ''
  }
}

function switchTab(tab: 'pending' | 'history' | 'mine'): void {
  const order = ['pending', 'history', 'mine'] as const
  const fromIdx = order.indexOf(listTab.value as (typeof order)[number])
  const toIdx = order.indexOf(tab)
  // 记录滑动方向（左滑=前进，右滑=后退）用于翻页动画
  tabAnim.value = toIdx > fromIdx ? 'tab-slide-left' : 'tab-slide-right'
  listTab.value = tab
  if (tab === 'history' && historyList.value.length === 0) void loadHistory(true)
  if (tab === 'history') void loadHistorySupplierOptions()
  if (tab === 'mine') {
    computeCacheSize()
    refreshPendingUploadCount()
  }
}
/** tab 切换动画方向 */
const tabAnim = ref('tab-slide-left')

/** 底部导航左右滑动切换（待收货 → 历史 → 我的），主流 App 手势 */
let tabNavTouchStartX = 0
let tabNavTouchStartY = 0
let tabNavSwiping = false
function onTabNavTouchStart(e: TouchEvent): void {
  tabNavTouchStartX = e.touches[0].clientX
  tabNavTouchStartY = e.touches[0].clientY
  tabNavSwiping = false
}
function onTabNavTouchMove(e: TouchEvent): void {
  // 水平滑动超阈值标记（用于阻止随后 click 误触 + 判定滑动）
  const t = e.touches[0]
  if (!t) return
  if (Math.abs(t.clientX - tabNavTouchStartX) > 40) tabNavSwiping = true
}
function onTabNavTouchEnd(e: TouchEvent): void {
  const t = e.changedTouches[0]
  if (!t) return
  const dx = t.clientX - tabNavTouchStartX
  const dy = t.clientY - tabNavTouchStartY
  // 水平滑动且垂直偏移小（避免与页面滚动冲突）
  if (Math.abs(dx) > 60 && Math.abs(dy) < 40) {
    const order = ['pending', 'history', 'mine'] as const
    const idx = order.indexOf(listTab.value as (typeof order)[number])
    if (dx < 0 && idx < order.length - 1) {
      switchTab(order[idx + 1]) // 左滑 → 下一个 tab
    } else if (dx > 0 && idx > 0) {
      switchTab(order[idx - 1]) // 右滑 → 上一个 tab
    }
  }
}
/** 阻止滑动后的误触 click（如列表项点击） */
function onTabNavTouchCancel(e: TouchEvent): void {
  if (tabNavSwiping && e.cancelable) e.preventDefault()
}

// ==================== 我的（Android 原生风格） ====================

/** 手持端用户名（取本地登录信息，容错回退） */
const myUsername = ref((localStorage.getItem('username') || '').trim())

/** 缓存占用估算（localStorage 草稿/离线照片 + CacheStorage + sessionStorage） */
const cacheSize = ref(0)

async function estimateCacheBytes(): Promise<number> {
  let bytes = 0
  try {
    // localStorage / sessionStorage：UTF-16 双字节估算
    const localKeys: string[] = []
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i)
      if (key) localKeys.push(key)
    }
    for (const key of localKeys) {
      const value = localStorage.getItem(key) || ''
      bytes += (key.length + value.length) * 2
    }
    for (let i = 0; i < sessionStorage.length; i++) {
      const key = sessionStorage.key(i) || ''
      bytes += (key.length + (sessionStorage.getItem(key) || '').length) * 2
    }
  } catch {
    // 忽略容量异常
  }
  // Cache Storage（界面静态资源；失败静默）
  try {
    if ('caches' in window) {
      const names = await caches.keys()
      for (const name of names) {
        const cache = await caches.open(name)
        const keys = await cache.keys()
        for (const req of keys) {
          const res = await cache.match(req)
          if (res) bytes += (await res.arrayBuffer()).byteLength
        }
      }
    }
  } catch {
    // 忽略 Cache API 异常
  }
  return bytes
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(2)} MB`
}

const cacheSizeText = computed(() => formatBytes(cacheSize.value))

function computeCacheSize(): void {
  void estimateCacheBytes().then((b) => {
    cacheSize.value = b
  })
}

/** 清除缓存：保留登录态与当前验收草稿，重置会给出明确说明 */
let clearingCache = false
async function clearAppCache(): Promise<void> {
  if (clearingCache) return
  clearingCache = true
  try {
    await ElMessageBox.confirm(
      '清除缓存后将重新下载界面资源，已保存的验收草稿与登录状态会保留。继续？',
      '清除缓存',
      { confirmButtonText: '清除', cancelButtonText: '取消', type: 'warning' },
    )
    const beforeBytes = await estimateCacheBytes()
    // Cache Storage（界面静态资源）
    if ('caches' in window) {
      const names = await caches.keys()
      await Promise.all(names.map((name) => caches.delete(name)))
    }
    // 保留：token / refresh_token / user_id / username / draft 草稿 / rememberedUsername
    // 清除：可再生成的缓存（模板、菜单、权限解析、历史标签页、离线照片暂存）
    const pruneKeys: string[] = []
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i)
      if (!key) continue
      if (
        key.startsWith('fts_') ||
        key === 'menu-overrides' ||
        key === 'menu-template' ||
        key === 'permissions' ||
        key === 'roles' ||
        key === 'tab-bar-tabs'
      ) {
        pruneKeys.push(key)
      }
    }
    for (const key of pruneKeys) localStorage.removeItem(key)
    // sessionStorage 全部清除
    sessionStorage.clear()
    const afterBytes = await estimateCacheBytes()
    cacheSize.value = afterBytes
    ElMessage.success(`缓存已清除，释放约 ${formatBytes(Math.max(0, beforeBytes - afterBytes))} 空间`)
  } catch {
    // 用户取消或清除异常，静默
  } finally {
    clearingCache = false
  }
}

async function loadHistory(reset = false): Promise<void> {
  if (reset) {
    historyPage.value = 1
    historyList.value = []
    historyFinished.value = false
  }
  historyLoading.value = true
  try {
    const res = await receiptConfirmationApi.getList({
      receiverType,
      page: historyPage.value,
      size: 10,
      confirmDateStart: historyDateStart.value || undefined,
      confirmDateEnd: historyDateEnd.value || undefined,
      supplierName: historySupplier.value || undefined,
    } as never)
    const records = res?.records || []
    historyList.value = reset ? records : [...historyList.value, ...records]
    historyTotal.value = res?.total ?? 0
    historyFinished.value = historyList.value.length >= (res?.total ?? 0)
  } catch {
    historyList.value = []
  } finally {
    historyLoading.value = false
  }
}

/** 历史区间过滤状态（与 loadHistory 联动，重查时重置分页） */
const historyDateStart = ref('')
const historyDateEnd = ref('')
const historySupplier = ref('')
const historySupplierOptions = ref<Array<{ label: string; value: string }>>([])
/** 筛选区是否展开（收缩式，默认收起） */
const historyFilterOpen = ref(false)

/** 当前生效的筛选条件数量（角标提示） */
const historyFilterActiveCount = computed(() => {
  let n = 0
  if (historyDateStart.value || historyDateEnd.value) n++
  if (historySupplier.value) n++
  return n
})

async function loadHistorySupplierOptions(): Promise<void> {
  if (historySupplierOptions.value.length) return
  try {
    const { records } = await supplierApi.getList({ page: 1, size: 200, status: 'active' })
    historySupplierOptions.value = records.map((s) => ({ label: s.supplierName, value: s.supplierName }))
  } catch {
    historySupplierOptions.value = []
  }
}

function onHistoryFilterChange(): void {
  if (historyDateStart.value && historyDateEnd.value && historyDateStart.value > historyDateEnd.value) {
    ElMessage.warning('开始日期不能晚于结束日期')
    historyDateEnd.value = historyDateStart.value
  }
  void loadHistory(true)
}

function resetHistoryFilter(): void {
  historyDateStart.value = ''
  historyDateEnd.value = ''
  historySupplier.value = ''
  void loadHistory(true)
}

function loadMoreHistory(): void {
  historyPage.value += 1
  void loadHistory(false)
}

async function openHistoryDetail(conf: ReceiptConfirmationInfo): Promise<void> {
  historyDetail.value = conf
  historyDetailSignatures.value = []
  historyDetailSigLoading.value = true
  try {
    const detail = await receiptConfirmationApi.getById(conf.confirmationId)
    if (detail) historyDetail.value = detail
    historyDetailSignatures.value = await receiptConfirmationApi.getSignatures(conf.confirmationId)
  } catch {
    ElMessage.error('加载确认单详情失败')
  } finally {
    historyDetailSigLoading.value = false
  }
}

/**
 * 补签：历史确认单签名缺失/不完整时重新进入签字流程。
 * 收货业务已完成（库存/应付已生成），补签只补凭证存证，不重复收货。
 */
async function enterResign(conf: ReceiptConfirmationInfo | null): Promise<void> {
  if (!conf) return
  const sigs = historyDetailSignatures.value
  // 已完成的签名不重置（补缺失的部分）
  signReceiverSig.value = ''
  signSupplierSig.value = ''
  signSupplierName.value = ''
  signStep.value = sigs.some(s => s.signerType === 'RECEIVER') ? 2 : 1
  receiverSigned.value = sigs.some(s => s.signerType === 'RECEIVER')
  supplierSigned.value = sigs.some(s => s.signerType === 'SUPPLIER')
  // 司机名称：从关联到货单取（确认单本身无 driverName 字段）
  if (conf.arrivalId && !signSupplierName.value) {
    try {
      const arrival = await purchaseArrivalApi.getById(conf.arrivalId)
      signSupplierName.value = arrival?.driverName?.trim() ?? ''
    } catch {
      signSupplierName.value = ''
    }
  }
  // 收货方签名自动完成并自动存证：验收人即当前登录账号（同质检员逻辑）
  if (!receiverSigned.value) {
    autoSignReceiver()
  }
  // 签字视图依赖 activeArrival 非空（头部展示）——补签时构造占位，不重新收货
  activeArrival.value = {
    arrivalId: conf.arrivalId || '',
    arrivalCode: conf.confirmationCode || '',
    supplierName: conf.supplierName || '',
    items: conf.items || [],
  } as unknown as PurchaseArrivalInfo
  signingConfirmation.value = conf
  historyDetail.value = null
  // 进入签字：尝试强制横屏；补签不自动打开全屏覆盖层（避免覆盖层渲染问题导致白屏），
  // 用户可在司机签字面板点「横屏书写」进入全屏签名区
  void lockOrientation(true)
  landscapePad.value = null
  // 补签也入栈：左滑/返回键回到列表
  try {
    history.pushState({ ftsMobileDetail: true, arrivalId: conf.confirmationId }, '')
  } catch { /* 忽略 */ }
}

function formatDate(iso?: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function formatDateTime(iso?: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function getRemaining(item: ReceiptConfirmationItemForm): number {
  return Math.max(0, Number(((item.expectedQuantity ?? 0) - (item.receivedQuantity ?? 0)).toFixed(2)))
}

function buildItem(arrivalItem: PurchaseArrivalItem): ReceiptConfirmationItemForm {
  const expected = arrivalItem.expectedQuantity ?? 0
  const received = arrivalItem.receivedQuantity ?? 0
  return {
    arrivalItemId: arrivalItem.arrivalItemId,
    materialId: arrivalItem.materialId,
    materialName: arrivalItem.materialName,
    specification: arrivalItem.specification,
    unit: arrivalItem.unit,
    expectedQuantity: expected,
    receivedQuantity: received,
    unitPrice: arrivalItem.unitPrice ?? 0,
    accept: undefined,
    quantity: 0,
    batchNo: undefined,
    productionDate: undefined,
    expiryDate: undefined,
    remark: undefined,
    manualMatched: false,
    evidence: [],
    qualityCheckResult: undefined,
  }
}

async function openArrival(arrival: PurchaseArrivalInfo): Promise<void> {
  try {
    const detail = await purchaseArrivalApi.getById(arrival.arrivalId)
    if (!detail?.items || detail.items.length === 0) {
      ElMessage.warning('该到货单无明细数据，无法收货')
      return
    }
    activeArrival.value = detail
    items.value = detail.items.map(buildItem)
    remark.value = ''
    barcodeNo.value = ''
    barcodeMatch.value = ''
    matchedItemIndex.value = -1
    batchTargetIndex.value = 0
    quickBatch.batchNo = ''
    quickBatch.productionDate = ''
    quickBatch.expiryDate = ''
    // 单卡聚焦：默认定位到首个待录入/未完成项
    const firstPending = items.value.findIndex(i => !isItemDone(i))
    stepIndex.value = firstPending >= 0 ? firstPending : 0
    activeIndex.value = stepIndex.value
    // 快速定位面板闪烁提示后自动收起
    flashLocatorHint()
    // 存在上次未提交的草稿 → 顶部提示继续录入
    checkRestoreHint(arrival.arrivalId)
    // 详情入栈：系统返回键/边缘左滑触发 popstate → 回到列表（而非退出 App）
    try {
      history.pushState({ ftsMobileDetail: true, arrivalId: arrival.arrivalId }, '')
    } catch { /* history 不可用时忽略 */ }
  } catch {
    ElMessage.error('加载到货单详情失败')
  }
}

/** 是否有已录入但未提交的数据（返回前提醒，避免拍摄的留证照片等意外丢失） */
function hasInProgressData(): boolean {
  return items.value.some(
    (i) =>
      (i.quantity ?? 0) > 0 ||
      i.accept === 'reject' ||
      (i.evidence?.length ?? 0) > 0 ||
      !!i.batchNo?.trim() ||
      !!i.remark?.trim(),
  )
}

async function backToList(): Promise<void> {
  // 签名阶段：先保存签名草稿（含笔迹），返回后重新进入可恢复
  if (signingConfirmation.value) {
    saveSignatureDraft()
  }
  if (hasInProgressData() || restoreHint.value) {
    try {
      await ElMessageBox.confirm('当前已录入的数据（含现场照片）尚未提交，返回列表将全部丢弃。确定返回吗？', '放弃本次录入', {
        confirmButtonText: '放弃并返回',
        cancelButtonText: '继续录入',
        type: 'warning',
      })
    } catch {
      return
    }
  }
  clearDraft()
  activeArrival.value = null
  items.value = []
  restoreHint.value = ''
  signingConfirmation.value = null
  signReceiverSig.value = ''
  signSupplierSig.value = ''
  signSupplierName.value = ''
  signStep.value = 1
  receiverSigned.value = false
  supplierSigned.value = false
  // 退出签字：恢复竖屏
  void lockOrientation(false)
  // 弹栈（与 openArrival 的 pushState 对应）。
  // 系统返回/左滑已通过 popstate 弹栈（fromPopstate=true 时不再弹，避免重复触发 popstate）
  if (!fromPopstate) {
    try {
      if (history.state?.ftsMobileDetail) {
        history.back()
      }
    } catch { /* 忽略 */ }
  }
}

/** 是否由 popstate（系统返回/左滑）触发的返回——此时栈已弹出，不再主动 back() */
let fromPopstate = false
function popToBackList(): void {
  fromPopstate = true
  void backToList().finally(() => {
    fromPopstate = false
  })
}

/** 签名草稿（笔迹 + 司机名）本地暂存，返回列表后重新进入同一确认单可恢复 */
const SIGN_DRAFT_KEY = 'fts_sign_draft'

function saveSignatureDraft(): void {
  if (!signingConfirmation.value) return
  try {
    localStorage.setItem(
      SIGN_DRAFT_KEY,
      JSON.stringify({
        confirmationId: signingConfirmation.value.confirmationId,
        signStep: signStep.value,
        receiverSig: signReceiverSig.value,
        supplierSig: signSupplierSig.value,
        supplierName: signSupplierName.value,
        receiverSigned: receiverSigned.value,
        supplierSigned: supplierSigned.value,
        savedAt: Date.now(),
      }),
    )
  } catch { /* 存储失败忽略 */ }
}

function restoreSignatureDraft(): void {
  if (!signingConfirmation.value) return
  try {
    const raw = localStorage.getItem(SIGN_DRAFT_KEY)
    if (!raw) return
    const d = JSON.parse(raw)
    if (String(d.confirmationId) !== String(signingConfirmation.value.confirmationId)) return
    if (d.supplierName) signSupplierName.value = d.supplierName
    signStep.value = d.signStep ?? 1
    signReceiverSig.value = d.receiverSig ?? ''
    signSupplierSig.value = d.supplierSig ?? ''
    receiverSigned.value = !!d.receiverSigned
    supplierSigned.value = !!d.supplierSigned
  } catch { /* 解析失败忽略 */ }
}

/** 签名完成（全部完成或返回列表）后清除签名草稿 */
function clearSignatureDraft(): void {
  try {
    localStorage.removeItem(SIGN_DRAFT_KEY)
  } catch { /* 忽略 */ }
}

/**
 * 生成收货方自动签名图像：用账号姓名绘制手写风格笔迹（楷体倾斜）。
 * 收货方即登录的验收人员，无需手写，与"质检员"同源。
 */
function generateAutoSignature(name: string): string {
  const canvas = document.createElement('canvas')
  canvas.width = 480
  canvas.height = 160
  const ctx = canvas.getContext('2d')
  if (!ctx) return ''
  const dpr = window.devicePixelRatio || 1
  canvas.width = 480 * dpr
  canvas.height = 160 * dpr
  ctx.scale(dpr, dpr)
  // 手写笔迹效果：楷体 + 倾斜 + 下划线
  ctx.font = 'italic 56px "KaiTi", "STKaiti", "楷体", cursive'
  ctx.fillStyle = '#1f2937'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText(name || '验收人', 240, 70)
  // 签名下划线（手写风格弧线）
  ctx.strokeStyle = '#1f2937'
  ctx.lineWidth = 2
  ctx.beginPath()
  ctx.moveTo(120, 110)
  ctx.quadraticCurveTo(240, 122, 360, 112)
  ctx.stroke()
  return canvas.toDataURL('image/png')
}

/** 当前登录用户实名（优先 userInfo.name，回退登录名） */
const myRealName = ref('')
;(() => {
  try {
    const info = JSON.parse(localStorage.getItem('userInfo') || 'null')
    myRealName.value = (info?.name || localStorage.getItem('username') || '').trim()
  } catch {
    myRealName.value = (localStorage.getItem('username') || '').trim()
  }
})()

/** 自动完成收货方签名并自动存证：账号实名人员 = 验收人（同质检员逻辑），无需手写 */
function autoSignReceiver(): void {
  const name = myRealName.value || '验收人员'
  const image = generateAutoSignature(name)
  if (!signingConfirmation.value) return
  signReceiverSig.value = image
  // 自动提交存证（失败不阻塞，进入签字流程后可重试/补签）
  receiptConfirmationApi
    .addSignature(signingConfirmation.value.confirmationId, {
      signerType: 'RECEIVER',
      signerName: myRealName.value || undefined,
      signatureImage: image,
    })
    .then(() => {
      receiverSigned.value = true
      signReceiverSig.value = ''
    })
    .catch(() => {
      receiverSigned.value = false
    })
}

/** 强制屏幕方向：优先原生插件（setRequestedOrientation，鸿蒙可靠），回退 JS API */
async function lockOrientation(landscape: boolean): Promise<void> {
  try {
    const cap = (window as unknown as { Capacitor?: { isNativePlatform?: () => boolean; Plugins?: Record<string, unknown> } }).Capacitor
    if (cap?.isNativePlatform?.() && cap.Plugins) {
      const plugin = cap.Plugins.ScreenOrientation as { lockLandscape?: () => Promise<unknown>; lockPortrait?: () => Promise<unknown>; unlock?: () => Promise<unknown> } | undefined
      if (plugin) {
        if (landscape) {
          await plugin.lockLandscape?.()
        } else {
          await plugin.lockPortrait?.()
        }
        return
      }
    }
  } catch { /* 插件不可用，回退 JS API */ }
  // JS 回退：部分 WebView（PC 浏览器等）支持 screen.orientation.lock
  try {
    const so = screen.orientation as (ScreenOrientation & { lock?: (o: string) => Promise<void>; unlock?: () => void }) | undefined
    if (so?.lock) {
      await so.lock(landscape ? 'landscape' : 'portrait')
    }
  } catch { /* 不支持则忽略 */ }
}

/** 退出登录：清除凭证回登录页（带 redirect，登录后自动回到手持收货） */
function handleLogout(): void {
  const keys = ['token', 'refresh_token', 'user_id', 'username', 'token_expiry', 'permissions', 'roles', 'menu-overrides', 'menu-template']
  for (const key of keys) localStorage.removeItem(key)
  const redirect = encodeURIComponent(`/store-management/receiving-mobile?receiverType=${receiverType}`)
  window.location.href = `/login?redirect=${redirect}`
}

function setAccept(item: ReceiptConfirmationItemForm, accept: 'receive' | 'reject'): void {
  item.accept = accept
  if (accept === 'reject') {
    // 拒收默认不自动填满全部数量（避免误操作整单拒收），由用户输入或点"全部拒收"
    if (item.quantity == null) item.quantity = 0
    // 拒收项质检结果自动记为不合格
    item.qualityCheckResult = 2
  }
}

/** 一键拒收全部剩余数量 */
function rejectAll(item: ReceiptConfirmationItemForm): void {
  item.accept = 'reject'
  item.quantity = getRemaining(item)
  item.qualityCheckResult = 2
}

/** 整数单位（台/个/箱等计数单位）不显示小数 */
const INTEGER_UNITS = new Set(['台', '个', '箱', '包', '件', '桶', '袋', '瓶', '把', '只', '份', '双', '张', '块', '根'])
function isIntegerUnit(unit?: string): boolean {
  return !unit || INTEGER_UNITS.has(unit.trim())
}
function qtyPrecision(unit?: string): number {
  return isIntegerUnit(unit) ? 0 : 2
}
function qtyStep(unit?: string): number {
  return isIntegerUnit(unit) ? 1 : 0.1
}

/** 实收输入框聚焦：值为 0 时清空显示（便于直接输入），否则全选数字 */
function onQtyFocus(e: FocusEvent): void {
  const input = e.target as HTMLInputElement | null
  if (!input) return
  setTimeout(() => {
    if (input.value === '0' || input.value === '0.00' || input.value === '0.0') {
      input.value = ''
    } else {
      input.select()
    }
  }, 50)
}

/** 输入框聚焦时滚入可视区（配合键盘 adjustResize，避免输入法遮挡输入框） */
function scrollInputIntoView(e: FocusEvent): void {
  const el = e.target as HTMLElement | null
  if (!el) return
  // 延迟到键盘动画后滚动，保证可视区已重排
  setTimeout(() => {
    el.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }, 300)
}

/**
 * 拍照（强制调用系统相机）：
 * - Capacitor 原生环境：@capacitor/camera 的 Camera.getPhoto(source=Camera)，强制拉起相机
 *   （WebView 的 <input capture="environment"> 常被忽略而打开相册）
 * - 非原生（PC 浏览器）：回退 file input 由浏览器处理
 * 拍到的照片转成 File 交给 onFile 回调（与现有 file change 处理链共用）
 */
async function takePhoto(onFile: (file: File) => Promise<void> | void): Promise<void> {
  const cap = (window as unknown as { Capacitor?: { isNativePlatform?: () => boolean } }).Capacitor
  if (cap?.isNativePlatform?.()) {
    try {
      const { Camera, CameraResultType, CameraSource } = await import('@capacitor/camera')
      // 提示取景（系统相机无聚焦框，靠提示引导将条码/二维码置于画面中央）
      ElMessage.info('请将条码/二维码放入画面中央后拍摄')
      const photo = await Camera.getPhoto({
        quality: 90,
        resultType: CameraResultType.Base64,
        source: CameraSource.Camera,
        saveToGallery: false,
      })
      if (!photo.base64String) {
        ElMessage.warning('未获取到照片，请重试')
        return
      }
      const byteChars = atob(photo.base64String)
      const bytes = new Uint8Array(byteChars.length)
      for (let i = 0; i < byteChars.length; i++) bytes[i] = byteChars.charCodeAt(i)
      const file = new File([bytes], `camera-${Date.now()}.jpg`, { type: 'image/jpeg' })
      await onFile(file)
      return
    } catch (err) {
      const msg = (err as { message?: string })?.message || ''
      if (/cancel/i.test(msg)) return // 用户取消拍照
      ElMessage.error(`相机调用失败：${msg || '请重试'}`)
      return
    }
  }
  // 非原生：提示使用相册选择
  ElMessage.info('请在下方选择相册图片')
}

/**
 * 实时扫码（原生 ZXing 相机预览 + 取景框）：
 * - Capacitor 原生：window.FtsNative.startScan() JS 桥（比 Capacitor 插件分发在鸿蒙更可靠）
 * - 非原生/桥不可用：回退拍照识别
 * @returns 识别文本；取消返回 null
 */
async function scanBarcode(): Promise<string | null> {
  // ① 首选 JS 桥（window.FtsNative.startScan）——不依赖 isNativePlatform 判断
  const ftsNative = (window as unknown as { FtsNative?: { startScan?: () => void } }).FtsNative
  if (ftsNative?.startScan) {
    try {
      return await new Promise<string | null>((resolve) => {
        const timer = setTimeout(() => {
          cleanup()
          ElMessage.warning('扫码超时，请重试')
          resolve(null)
        }, 30000)
        const cleanup = () => clearTimeout(timer)
        ;(window as unknown as { __ftsScanDone?: (text: string | null, err?: string) => void }).__ftsScanDone =
          (text, err) => {
            cleanup()
            if (err && /cancel/i.test(err)) {
              resolve(null)
            } else if (text) {
              resolve(text)
            } else {
              ElMessage.warning('未识别到条码，请重试')
              resolve(null)
            }
          }
        // 这里已由上方 if (ftsNative?.startScan) 收窄，但闭包内 TS 会丢失收窄，显式断言
        ftsNative.startScan!()
      })
    } catch (err) {
      ElMessage.warning(`JS桥扫码失败（${String(err).slice(0, 30)}），尝试插件…`)
    }
  }
  // ② Capacitor 插件回退
  try {
    const { registerPlugin } = await import('@capacitor/core')
    const plugin = registerPlugin<{ scan: () => Promise<{ text: string }> }>('BarcodeScanner')
    const res = await plugin.scan()
    return res.text || null
  } catch (err) {
    const msg = (err as { message?: string })?.message || String(err)
    if (!/cancel/i.test(msg)) {
      ElMessage.warning(`扫码插件调用失败（${msg.slice(0, 30)}），已回退拍照识别`)
    }
  }
  // ③ 回退：拍照识别（强制相机，不依赖 isNativePlatform）
  return new Promise<string | null>((resolve) => {
    const doPhoto = async () => {
      try {
        const { Camera, CameraResultType, CameraSource } = await import('@capacitor/camera')
        ElMessage.info('请将条码/二维码放入画面中央后拍摄')
        const photo = await Camera.getPhoto({
          quality: 90,
          resultType: CameraResultType.Base64,
          source: CameraSource.Camera,
          saveToGallery: false,
        })
        if (!photo.base64String) {
          resolve(null)
          return
        }
        const byteChars = atob(photo.base64String)
        const bytes = new Uint8Array(byteChars.length)
        for (let i = 0; i < byteChars.length; i++) bytes[i] = byteChars.charCodeAt(i)
        const file = new File([bytes], `camera-${Date.now()}.jpg`, { type: 'image/jpeg' })
        const text = await recognizeBarcode(file)
        resolve(text)
      } catch (err) {
        const msg = (err as { message?: string })?.message || ''
        if (/cancel/i.test(msg)) {
          resolve(null)
        } else {
          ElMessage.warning(`相机调用失败（${msg.slice(0, 30)}）`)
          resolve(null)
        }
      }
    }
    void doPhoto()
  })
}

/**
 * 批次号扫码：原生实时扫码（条码/二维码）→ 文本直接填入批次号。
 * 扫码取消/无结果时回退拍照 OCR 识别（旧链路保留）。
 */
async function scanBatchBarcode(): Promise<void> {
  const text = await scanBarcode()
  if (!text) return // 取消或无结果
  const target = items.value[batchTargetIndex.value]
  if (!target) return
  const raw = text.trim()
  if (raw.length <= 60) {
    target.batchNo = raw
    ElMessage.success('已识别批次号')
    matchBarcodeToItem(raw)
  } else {
    ElMessage.warning('识别内容过长，可能不是批次号，请手动输入')
  }
}

/**
 * 扫防伪码（App 内）：原生实时扫码 → 解码 fts-receiving://verify/<码> 或纯 16 位码 → 查验页。
 */
async function scanVerifyCode(): Promise<void> {
  const text = await scanBarcode()
  if (!text) {
    ElMessage.info('已取消扫码')
    return
  }
  const raw = text.trim()
  const m = raw.match(/fts-receiving:\/\/verify\/([A-Za-z0-9]+)/)
  const code = m ? m[1] : raw
  if (!/^[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{16}$/.test(code)) {
    ElMessage.warning(`识别的码格式不正确（${raw.slice(0, 20)}），请确认扫描的是防伪码`)
    return
  }
  ElMessage.success('识别成功，正在查验…')
  window.location.href = `/verify-code/${code}`
}

const totalConfirmed = computed(() =>
  items.value.reduce((s, i) => s + (i.accept === 'reject' ? 0 : i.quantity ?? 0), 0),
)
const totalRejected = computed(() =>
  items.value.reduce((s, i) => {
    if (i.accept === 'reject') return s + (i.quantity ?? 0)
    if (i.accept === 'receive')
      return s + Math.max(0, Number((getRemaining(i) - (i.quantity ?? 0)).toFixed(2)))
    return s
  }, 0),
)
const partialCount = computed(
  () =>
    items.value.filter((i) => i.accept === 'receive' && (i.quantity ?? 0) < getRemaining(i)).length,
)
/** 全部拒收 → 按钮文案改为「确认拒收」 */
const allRejected = computed(() => items.value.length > 0 && items.value.every((i) => i.accept === 'reject'))

/** 校验失败定位（滚动到对应卡片并高亮，避免"上面操作、下面提示"的断层感） */
const invalidItemIndex = ref(-1)

function markItemInvalid(index: number): void {
  invalidItemIndex.value = index
  gotoStep(index)
  window.setTimeout(() => {
    if (invalidItemIndex.value === index) invalidItemIndex.value = -1
  }, 2600)
}

function validate(): { ok: boolean; failIndex: number } {
  for (let i = 0; i < items.value.length; i++) {
    const item = items.value[i]
    const qty = item.quantity ?? 0
    const remaining = getRemaining(item)
    if (qty <= 0) {
      ElMessage.warning(`${item.materialName} 数量必须大于 0`)
      return { ok: false, failIndex: i }
    }
    if (qty > remaining) {
      ElMessage.warning(`${item.materialName} 数量不能超过 ${remaining}`)
      return { ok: false, failIndex: i }
    }
    if (item.accept === 'reject') {
      if (!item.remark?.trim()) {
        ElMessage.warning(`第 ${i + 1} 项「${item.materialName}」已拒收，请填写拒收原因`)
        return { ok: false, failIndex: i }
      }
      if (!item.evidence?.length) {
        ElMessage.warning(`第 ${i + 1} 项「${item.materialName}」已拒收，必须现场拍照留证（拒收证据）`)
        return { ok: false, failIndex: i }
      }
      continue
    }
    // 接收商品：必须录入批次号 + 质检结果（可追溯）
    if (!item.batchNo?.trim()) {
      ElMessage.warning(`第 ${i + 1} 项「${item.materialName}」请录入批次号（外用标签扫码拍照或手动输入）`)
      return { ok: false, failIndex: i }
    }
    if (!item.qualityCheckResult) {
      ElMessage.warning(`第 ${i + 1} 项「${item.materialName}」请选择质检结果`)
      return { ok: false, failIndex: i }
    }
    if (item.manualMatched && !item.evidence?.length) {
      ElMessage.warning(`第 ${i + 1} 项「${item.materialName}」为人工关联，必须现场拍照留证（关联证据）`)
      return { ok: false, failIndex: i }
    }
  }
  return { ok: true, failIndex: -1 }
}

async function handleSubmit(): Promise<void> {
  if (!activeArrival.value) return
  const v = validate()
  if (!v.ok) {
    markItemInvalid(v.failIndex)
    return
  }
  submitting.value = true
  try {
    // 先把离线暂存的照片重传到服务器，全部成功后才有完整证据链
    if (!(await flushPendingUploads())) {
      ElMessage.error('仍有照片未上传成功，请检查网络后重试（照片已暂存本地不会丢失）')
      return
    }
    // 批次/效期质检均在商品卡片内各自录入，直接提交
    const submitItems = items.value.map((item) => ({
      ...item,
      batchNo: item.batchNo || undefined,
      productionDate: item.productionDate || undefined,
      expiryDate: item.expiryDate || undefined,
    }))
    const created = await receiptConfirmationApi.create({
      arrivalId: activeArrival.value.arrivalId ?? '',
      receiverType,
      storeId: activeArrival.value.storeId,
      warehouseId: activeArrival.value.warehouseId,
      items: submitItems,
      qualityCheckResult: qualityCheckResult.value,
      remark: remark.value || undefined,
      barcode: barcodeNo.value || undefined,
    })
    ElMessage.success('收货确认成功')
    // 提交成功：清除草稿，避免历史残留
    clearDraft()
    // 进入双签存证步骤（签名保存后可随时在确认单详情中查看）
    items.value = []
    signReceiverSig.value = ''
    signSupplierSig.value = ''
    // 司机名称自动带入：管理端到货单配置的 driverName（取不到则留空手输）
    signSupplierName.value = activeArrival.value?.driverName?.trim() ?? ''
    signStep.value = 2 // 收货方自动签名 → 直接进入司机签字步骤
    receiverSigned.value = false
    supplierSigned.value = false
    signingConfirmation.value = created
    // 若之前返回列表过且存在该确认单的签名草稿（笔迹/司机名），恢复继续签
    restoreSignatureDraft()
    // 收货方签名自动完成并自动存证：验收人即当前登录账号（同质检员逻辑）
    if (!receiverSigned.value) {
      autoSignReceiver()
    }
    // 收货方自动完成 → 签字流程只保留司机步骤；尝试强制横屏
    // （不自动打开全屏覆盖层——司机签字面板点「横屏书写」进入全屏签名区，避免覆盖层渲染白屏）
    signStep.value = 2
    void lockOrientation(true)
    landscapePad.value = null
  } catch (error) {
    // 真实原因已由请求拦截器弹窗（含后端业务信息），此处仅兜底提示，避免重复弹窗
    const msg = (error as Error)?.message
    if (!msg || msg === 'Network Error') {
      ElMessage.error('收货确认失败，请检查网络后重试')
    } else if (!/系统繁忙|服务器内部错误|请求超时|网络错误|服务暂时不可用/.test(msg)) {
      ElMessage.error(`收货确认失败：${msg}`)
    }
  } finally {
    submitting.value = false
  }
}

function finishSigning(): void {
  // 补签完成：清除签名草稿，避免下次误恢复
  clearSignatureDraft()
  void backToList()
  void loadArrivals()
}

function handleSignReceiver(): void {
  if (!signReceiverSig.value) {
    ElMessage.warning('请先在签名板上手写签名')
    return
  }
  const image = signReceiverSig.value
  const receiverName = myRealName.value || undefined
  signing.value = true
  receiptConfirmationApi
    .addSignature(signingConfirmation.value!.confirmationId, {
      signerType: 'RECEIVER',
      signerName: receiverName,
      signatureImage: image,
    })
    .then(() => {
      receiverSigned.value = true
      signReceiverSig.value = ''
      ElMessage.success('收货方签名已存证')
      // 自动进入第二步：供应商签字
      signStep.value = 2
    })
    .catch((error) => {
      const msg = (error as Error)?.message
      if (!msg || msg === 'Network Error') {
        ElMessage.error('签名保存失败，请检查网络后重试')
      } else if (!/系统繁忙|服务器内部错误|请求超时|网络错误|服务暂时不可用/.test(msg)) {
        ElMessage.error(`签名保存失败：${msg}`)
      }
    })
    .finally(() => {
      signing.value = false
    })
}

function handleSignSupplier(): void {
  if (!signSupplierName.value.trim()) {
    ElMessage.warning('请填写司机/送货员姓名')
    return
  }
  if (!signSupplierSig.value) {
    ElMessage.warning('请先在签名板上手写签名')
    return
  }
  const image = signSupplierSig.value
  const name = signSupplierName.value.trim()
  signing.value = true
  receiptConfirmationApi
    .addSignature(signingConfirmation.value!.confirmationId, {
      signerType: 'SUPPLIER',
      signerName: name,
      signatureImage: image,
    })
    .then(() => {
      supplierSigned.value = true
      signSupplierSig.value = ''
      ElMessage.success('供应商签名已存证')
      // 双签完成：清除签名草稿
      clearSignatureDraft()
    })
    .catch((error) => {
      const msg = (error as Error)?.message
      if (!msg || msg === 'Network Error') {
        ElMessage.error('签名保存失败，请检查网络后重试')
      } else if (!/系统繁忙|服务器内部错误|请求超时|网络错误|服务暂时不可用/.test(msg)) {
        ElMessage.error(`签名保存失败：${msg}`)
      }
    })
    .finally(() => {
      signing.value = false
    })
}

/** 1D/2D 条码格式（尽量精简到普遍支持的集合） */
const BARCODE_FORMATS = [
  'qr_code', 'ean_13', 'ean_8', 'code_128', 'code_39', 'code_93', 'itf', 'upc_a', 'upc_e', 'data_matrix', 'aztec', 'pdf417',
] as const

/** 摄像头实时条码扫码（html5-qrcode/ZXing，支持 1D+2D；需 HTTPS 或 localhost 安全上下文） */
const liveScanVisible = ref(false)
const liveScanError = ref('')
let liveScanner: Html5Qrcode | null = null

async function openLiveScanner(): Promise<void> {
  liveScanError.value = ''
  liveScanVisible.value = true
  await nextTick()
  try {
    liveScanner = new Html5Qrcode('live-scanner-region')
    await liveScanner.start(
      { facingMode: 'environment' },
      { fps: 10, qrbox: { width: 260, height: 160 } },
      async (text) => {
        const value = await confirmScanResult(text)
        if (value) {
          barcodeNo.value = value
          ElMessage.success('已识别货物条码')
          matchBarcodeToItem(value)
        }
        closeLiveScanner()
      },
      () => {
        // 未识别到条码时静默
      },
    )
  } catch {
    liveScanError.value = '无法调用摄像头：需 HTTPS 或 localhost 环境（浏览器安全限制）。请在电脑 localhost 测试，或使用扫码枪/手动输入。'
    ElMessage.error('摄像头不可用：非 HTTPS 环境被浏览器拦截')
  }
}

async function closeLiveScanner(): Promise<void> {
  liveScanVisible.value = false
  if (liveScanner) {
    try {
      await liveScanner.stop()
    } catch {
      // 摄像头已释放或从未启动
    }
    try {
      liveScanner.clear()
    } catch {
      // 忽略清理异常
    }
    liveScanner = null
  }
}

/** 最近一次识别的诊断信息（用于定位识别失败原因） */
const lastDiag = ref('')

/**
 * 图片统一加载为 canvas（自动处理 EXIF 方向；限制最大宽度 1600px 兼顾清晰度与性能）
 * 绕开 createImageBitmap/HEIC 等格式兼容问题，识别管线只依赖 canvas
 */
async function loadImageAsCanvas(file: File): Promise<HTMLCanvasElement | null> {
  const url = URL.createObjectURL(file)
  try {
    const img = new Image()
    img.src = url
    await new Promise<void>((resolve, reject) => {
      img.onload = () => resolve()
      img.onerror = () => reject(new Error('图片加载失败'))
    })
    const maxWidth = 2200
    const scale = img.naturalWidth > maxWidth ? maxWidth / img.naturalWidth : 1
    const canvas = document.createElement('canvas')
    canvas.width = Math.max(1, Math.round(img.naturalWidth * scale))
    canvas.height = Math.max(1, Math.round(img.naturalHeight * scale))
    const ctx = canvas.getContext('2d')
    if (!ctx) return null
    ctx.drawImage(img, 0, 0, canvas.width, canvas.height)
    return canvas
  } catch {
    return null
  } finally {
    URL.revokeObjectURL(url)
  }
}

/**
 * 条码识别：canvas → ① BarcodeDetector（原生，先无参构造最兼容，失败再带 formats）→ ② 转 JPEG 后 html5-qrcode(ZXing)
 * 图片仅在设备本地处理，识别完成即释放，不上传服务器
 * 失败时填充 lastDiag 诊断信息，便于定位问题
 */
async function recognizeBarcode(file: File): Promise<string | null> {
  const diag: string[] = []
  const canvas = await loadImageAsCanvas(file)
  if (!canvas) {
    lastDiag.value = 'img-load-fail'
    return null
  }
  diag.push(`size=${canvas.width}x${canvas.height}`)

  // 0) jsQR 解码（二维码/部分条码，纯 JS 可靠）
  try {
    const ctx0 = canvas.getContext('2d')
    if (ctx0) {
      const imgData = ctx0.getImageData(0, 0, canvas.width, canvas.height)
      const { default: jsQR } = await import('jsqr')
      const jr = jsQR(imgData.data, imgData.width, imgData.height, { inversionAttempts: 'dontInvert' })
      if (jr?.data) {
        diag.push(`jsQR=${jr.data.slice(0, 20)}`)
        return jr.data
      }
    }
  } catch { /* jsQR 不可用时继续 */ }

  const BarcodeDetectorApi = (window as unknown as {
    BarcodeDetector?: new (options?: { formats?: readonly string[] }) => {
      detect(image: ImageBitmap | HTMLImageElement | HTMLCanvasElement): Promise<{ rawValue: string }[]>
    }
  }).BarcodeDetector

  diag.push(`bd=${!!BarcodeDetectorApi}`)
  if (BarcodeDetectorApi) {
    // 先无参构造（最兼容），异常时再带 formats 重试
    const constructors: Array<{ formats?: readonly string[] } | undefined> = [undefined, { formats: BARCODE_FORMATS }]
    for (const opts of constructors) {
      try {
        const detector = opts ? new BarcodeDetectorApi(opts) : new BarcodeDetectorApi()
        const results = await detector.detect(canvas)
        diag.push(`bdR=${results.length}`)
        if (results.length > 0 && results[0].rawValue) {
          return results[0].rawValue
        }
        break
      } catch (e) {
        diag.push(`bdE=${e instanceof Error ? e.name : String(e)}`)
      }
    }
  }

  try {
    const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, 'image/jpeg', 0.92))
    if (!blob) {
      diag.push('blob=null')
      lastDiag.value = diag.join(' | ')
      return null
    }
    const jpegFile = new File([blob], 'barcode.jpg', { type: 'image/jpeg' })
    const fileScanner = new Html5Qrcode('batch-file-scanner-region')
    try {
      const result = await fileScanner.scanFileV2(jpegFile, false)
      diag.push(`zx=${result ? `${result.decodedText.slice(0, 20)}/${(result.result as { format?: { format?: string } } | null)?.format?.format ?? '?'}` : 'null'}`)
      if (result) {
        return result.decodedText
      }
    } finally {
      fileScanner.clear()
    }
  } catch (e) {
    diag.push(`zxE=${e instanceof Error ? e.name : String(e)}`)
  }

  // ③ 手写 EAN-13 解码器兜底（商品/书籍条码主流格式，精确按模块解码）
  try {
    const ean = decodeEan13(canvas)
    diag.push(`ean=${ean.code ?? 'null'}(s=${ean.scans},h=${ean.starts})`)
    if (ean.code) {
      return ean.code
    }
  } catch (e) {
    diag.push(`eanE=${e instanceof Error ? e.name : String(e)}`)
  }

  lastDiag.value = diag.join(' | ')
  return null
}

/** 裁剪重试：识别失败时框选条码区域，裁剪放大后二次识别（解决 quiet zone/条码占比小问题） */
const cropVisible = ref(false)
const cropImageUrl = ref('')
const cropAreaRef = ref<HTMLDivElement>()
const cropImageSize = ref({ w: 0, h: 0 })
const cropRect = reactive({ x: 0, y: 0, w: 0, h: 0 })
const cropMode = ref<'move' | 'resize' | 'none'>('none')
const cropStart = reactive({ x: 0, y: 0, rectX: 0, rectY: 0, rectW: 0, rectH: 0 })
let cropFile: File | null = null

async function openCrop(file: File): Promise<void> {
  cropFile = file
  if (cropImageUrl.value) URL.revokeObjectURL(cropImageUrl.value)
  cropImageUrl.value = URL.createObjectURL(file)
  cropVisible.value = true
  await nextTick()
  const area = cropAreaRef.value
  if (!area) return
  cropImageSize.value = { w: area.clientWidth, h: area.clientHeight }
  // 默认选区：画面中央 60% 宽、40% 高（细长条码）
  cropRect.x = cropImageSize.value.w * 0.2
  cropRect.w = cropImageSize.value.w * 0.6
  cropRect.y = cropImageSize.value.h * 0.3
  cropRect.h = cropImageSize.value.h * 0.4
}

function closeCrop(): void {
  cropVisible.value = false
  if (cropImageUrl.value) {
    URL.revokeObjectURL(cropImageUrl.value)
    cropImageUrl.value = ''
  }
  cropFile = null
}

function onCropPointerDown(e: PointerEvent, mode: 'move' | 'resize'): void {
  cropMode.value = mode
  cropStart.x = e.clientX
  cropStart.y = e.clientY
  cropStart.rectX = cropRect.x
  cropStart.rectY = cropRect.y
  cropStart.rectW = cropRect.w
  cropStart.rectH = cropRect.h
  ;(e.target as HTMLElement).setPointerCapture(e.pointerId)
}

function onCropPointerMove(e: PointerEvent): void {
  if (cropMode.value === 'none') return
  const dx = e.clientX - cropStart.x
  const dy = e.clientY - cropStart.y
  if (cropMode.value === 'move') {
    cropRect.x = Math.min(Math.max(0, cropStart.rectX + dx), cropImageSize.value.w - cropRect.w)
    cropRect.y = Math.min(Math.max(0, cropStart.rectY + dy), cropImageSize.value.h - cropRect.h)
  } else {
    const w = Math.min(Math.max(60, cropStart.rectW + dx), cropImageSize.value.w - cropRect.x)
    const h = Math.min(Math.max(40, cropStart.rectH + dy), cropImageSize.value.h - cropRect.y)
    cropRect.w = w
    cropRect.h = h
  }
}

function onCropPointerUp(): void {
  cropMode.value = 'none'
}

async function confirmCropAndRecognize(): Promise<void> {
  if (!cropFile || !cropImageUrl.value) return
  cropVisible.value = false
  try {
    const img = new Image()
    img.src = cropImageUrl.value
    await new Promise<void>((resolve, reject) => {
      img.onload = () => resolve()
      img.onerror = () => reject(new Error('图片加载失败'))
    })
    // 按选区比例裁剪原图
    const canvas = document.createElement('canvas')
    const sx = (cropRect.x / cropImageSize.value.w) * img.naturalWidth
    const sy = (cropRect.y / cropImageSize.value.h) * img.naturalHeight
    const sw = (cropRect.w / cropImageSize.value.w) * img.naturalWidth
    const sh = (cropRect.h / cropImageSize.value.h) * img.naturalHeight
    canvas.width = Math.max(1, Math.round(sw))
    canvas.height = Math.max(1, Math.round(sh))
    const ctx = canvas.getContext('2d')
    if (!ctx) throw new Error('canvas 不可用')
    ctx.drawImage(img, sx, sy, sw, sh, 0, 0, canvas.width, canvas.height)
    const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, 'image/jpeg', 0.95))
    if (!blob) throw new Error('图片转换失败')
    const result = await recognizeBarcode(new File([blob], 'crop.jpg', { type: 'image/jpeg' }))
    if (result) {
      const value = await confirmScanResult(result)
      if (value) {
        const target = items.value[batchTargetIndex.value]
        if (target) target.batchNo = value
        ElMessage.success('已识别批次号')
      }
    } else {
      ElMessage.warning('裁剪后仍未识别到条码，请重新框选（确保条码四周留少量空白、保持水平）')
      cropVisible.value = true
    }
  } catch {
    ElMessage.warning('识别失败，请重试')
  }
}

/** OCR 识别外箱标签：调后端 RapidOCR，提取批次号 + 生产日期 + 效期 */
const ocrLoading = ref(false)

interface OcrResult {
  success?: boolean
  rawText?: string
  productionDate?: string
  expiryDate?: string
  barcode?: string
  errorMessage?: string
}

async function ocrRecognize(canvas: HTMLCanvasElement): Promise<OcrResult | null> {
  try {
    const dataUrl = canvas.toDataURL('image/jpeg', 0.95)
    const base64 = dataUrl.split(',')[1] || ''
    const res = await post<OcrResult | null>('/v1/ocr/recognize', { imageBase64: base64 })
    return res
  } catch {
    return null
  }
}

/** 从 OCR 文本中提取批次号（外箱标签：批次/批号/LOT/BATCH 关键词后的编号） */
function extractBatchNo(text: string): string | null {
  const t = text || ''
  const patterns = [
    /(?:批次|批号|生产批号|生产批次)[号No.号]*\s*[:：=]?\s*([A-Za-z0-9][A-Za-z0-9\-_./]{2,40})/,
    /(?:LOT\s*NO|BATCH\s*NO|LOT\s*#|BATCH\s*#)[.:：]?\s*([A-Za-z0-9][A-Za-z0-9\-_./]{2,40})/i,
    /\b(?:LOT|BATCH)\b[.:：\s]*([A-Za-z0-9][A-Za-z0-9\-_./]{2,40})/i,
  ]
  for (const p of patterns) {
    const m = t.match(p)
    if (m && m[1]) return m[1].trim()
  }
  // 兜底：日期+序号组合（如 20260802-A01）
  const d = t.match(/(20\d{2}[01]\d[0-3]\d[-_./][A-Za-z0-9\-_]{1,24})/)
  if (d) return d[1]
  return null
}

/** 从 OCR 文本中按关键词提取日期（支持 2026-08-02 / 2026/8/2 / 2026年8月2日） */
function extractDateFromText(text: string, keywords: string[]): string | null {
  const t = text || ''
  for (const kw of keywords) {
    const m = t.match(new RegExp(`${kw}[^\\d]*?(\\d{4})[-/.年](\\d{1,2})[-/.月](\\d{1,2})日?`))
    if (m) return `${m[1]}-${m[2].padStart(2, '0')}-${m[3].padStart(2, '0')}`
  }
  // 兜底：任意完整日期
  const m = t.match(/(\d{4})[-/.年](\d{1,2})[-/.月](\d{1,2})日?/)
  if (m) return `${m[1]}-${m[2].padStart(2, '0')}-${m[3].padStart(2, '0')}`
  return null
}

/** OCR 识别结果确认弹窗：预填提取字段（可编辑）+ 全文复制 */
const ocrConfirmVisible = ref(false)
const ocrConfirmText = ref('')
const ocrBatch = ref('')
const ocrProdDate = ref('')
const ocrExpiryDate = ref('')
const ocrNote = ref('')

function openOcrConfirm(rawText: string, batch: string, prodDate: string, expiry: string, note: string): void {
  ocrConfirmText.value = rawText
  ocrBatch.value = batch
  ocrProdDate.value = prodDate
  ocrExpiryDate.value = expiry
  ocrNote.value = note
  ocrConfirmVisible.value = true
}

async function copyOcrText(): Promise<void> {
  try {
    await navigator.clipboard.writeText(ocrConfirmText.value)
    ElMessage.success('已复制识别文本')
  } catch {
    ElMessage.warning('复制失败，请长按文本手动复制')
  }
}

function applyOcrConfirm(): void {
  // 识别结果写入「当前目标商品」（每张商品卡内各自录入批次）
  const corrected = selfCorrectOcrFields(ocrConfirmText.value)
  const target = items.value[batchTargetIndex.value]
  if (!target) return
  if (ocrBatch.value.trim()) {
    target.batchNo = ocrBatch.value.trim()
  } else if (corrected.batch) {
    target.batchNo = corrected.batch
  }
  if (ocrProdDate.value) {
    target.productionDate = ocrProdDate.value
  } else if (corrected.prodDate) {
    target.productionDate = corrected.prodDate
  }
  if (ocrExpiryDate.value) {
    target.expiryDate = ocrExpiryDate.value
  } else if (corrected.expiry) {
    target.expiryDate = corrected.expiry
  }
  ocrConfirmVisible.value = false
  if (target.batchNo || target.productionDate || target.expiryDate) {
    ElMessage.success('已应用识别结果')
  } else {
    ElMessage.warning('未能提取到批次信息，请手动输入')
  }
}

/**
 * OCR 字段自我纠错：
 * 1. 日期字符混淆修正（O→0 等）+ 合法校验
 * 2. 生产日期 + 保质期（天/月）交叉推算保质期至
 */
function selfCorrectOcrFields(rawText: string): { batch: string | null; prodDate: string | null; expiry: string | null; note: string } {
  const batch = extractBatchNo(rawText)

  // 生产日期：关键词优先（年份 2000-今年+1），再任意
  const thisYear = new Date().getFullYear()
  let prodDate = extractCorrectedDate(rawText, ['生产日期', '生产', '制造', 'MFG', 'PROD'], {
    minYear: 2000,
    maxYear: thisYear + 1,
  })

  // 保质期至：关键词优先；随后交叉验证
  let expiry = extractCorrectedDate(rawText, ['保质期至', '有效期至', '到期日', 'EXP', '到期'], {
    minYear: 2000,
    maxYear: thisYear + 5,
  })
  const shelf = extractShelfLife(rawText)
  let note = ''
  if (shelf && prodDate) {
    const calc = addDuration(prodDate, shelf.value, shelf.unit)
    const calcValid = isValidDateStr(calc, 2000, thisYear + 5)
    if (calcValid) {
      if (!expiry) {
        // 没识别到保质期至 → 用推算值
        expiry = calc
        note = `保质期至按「生产日期 + 保质期${shelf.value}${shelf.unit === 'month' ? '个月' : '天'}」推算`
      } else if (Math.abs(diffDays(expiry, calc)) > 3) {
        // 识别值与推算值冲突（识别错误）→ 以推算为准
        expiry = calc
        note = `保质期至自动纠正：OCR 识别 ${expiry}，按「生产日期+保质期${shelf.value}${shelf.unit === 'month' ? '个月' : '天'}」修正为 ${calc}`
      }
    }
  }

  // 日期纠错说明（生产日期字符修正时）
  if (!note && prodDate) {
    const rawNear = rawText.match(/生产日期[\s\S]{0,20}?/)
    if (rawNear && /[OoDI1lTSGZBz]/.test(rawNear[0])) {
      note = '生产日期已按字符混淆自动修正'
    }
  }

  return { batch, prodDate, expiry, note }
}

/** 拍照/相册 → OCR 识别外箱标签（主方案），条码识别兜底 */
async function handleBatchFileChange(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  ocrLoading.value = true
  try {
    const canvas = await loadImageAsCanvas(file)
    if (!canvas) {
      ElMessage.warning('图片加载失败，请重试')
      return
    }
    const ocr = await ocrRecognize(canvas)
    const rawText = ocr?.rawText || ''
    if (ocr?.success !== false && rawText) {
      // 自我纠错：字符混淆修正 + 合法校验 + 保质期交叉推算
      const corrected = selfCorrectOcrFields(rawText)
      openOcrConfirm(
        rawText,
        corrected.batch || '',
        corrected.prodDate || '',
        corrected.expiry || '',
        corrected.note,
      )
      return
    }

    // OCR 无结果：回退条码识别（外箱标签批次号若以条码形式存在）
    const result = await recognizeBarcode(file)
    if (result) {
      const value = await confirmScanResult(result)
      if (value) {
        const target = items.value[batchTargetIndex.value]
        if (target) target.batchNo = value
        ElMessage.success('已识别批次号')
      }
    } else {
      ElMessage.warning('未识别到批次号，请手动输入或重拍清晰的标签照片')
    }
  } catch {
    ElMessage.warning('识别失败，请重试')
  } finally {
    ocrLoading.value = false
    input.value = ''
  }
}

/** 断线重连：自动重传离线暂存照片 */
function onWindowOnline(): void {
  if (!activeArrival.value) return
  void flushPendingUploads().then((all) => {
    if (all) ElMessage.success('网络已恢复，离线照片已全部上传')
  })
}

function onWindowPagehide(): void {
  if (activeArrival.value) {
    clearDraft()
    saveDraft()
  }
}

/**
 * 系统返回/边缘左滑（WebView 将其映射为 history.back → popstate）：
 * 详情/签字阶段 → 回列表；列表阶段 → 放行（退出 App）。
 */
function onWindowPopState(): void {
  if (activeArrival.value) {
    popToBackList()
  }
}

onMounted(() => {
  loadArrivals()
  loadMyStoreName()
  window.addEventListener('scroll', onNavScroll, { passive: true })
  window.addEventListener('online', onWindowOnline)
  window.addEventListener('pagehide', onWindowPagehide)
  window.addEventListener('popstate', onWindowPopState)
})

onBeforeUnmount(() => {
  closeLiveScanner()
  window.removeEventListener('scroll', onNavScroll)
  window.removeEventListener('online', onWindowOnline)
  window.removeEventListener('pagehide', onWindowPagehide)
  window.removeEventListener('popstate', onWindowPopState)
})

/** 进度自动存入草稿（防崩溃/切后台/误返回丢数据），仅在有录入数据时保存 */
watch(
  [items, remark, barcodeNo],
  () => {
    if (!activeArrival.value || !hasInProgressData()) return
    saveDraft()
  },
  { deep: true },
)
</script>

<template>
  <div
    class="mobile-page"
    :class="{ 'has-bottom-bar': !!activeArrival && !signingConfirmation }"
    @touchstart="onTabNavTouchStart"
    @touchmove="onTabNavTouchMove"
    @touchend="onTabNavTouchEnd"
    @touchcancel="onTabNavTouchCancel"
  >
    <!-- 列表视图 -->
    <template v-if="!activeArrival">
      <header class="mobile-header">
        <div class="mobile-header__left">
          <span class="mobile-header__title">收货验收</span>
          <span class="mobile-header__sub">{{ receiverLabel }}</span>
        </div>
        <div class="mobile-header__right">
          <button class="icon-btn" :class="{ 'is-loading': listLoading }" @click="loadArrivals">
            <el-icon :size="18"><Refresh /></el-icon>
          </button>
        </div>
      </header>

      <!-- 待收货/历史/我的：tab 切换带左右翻页动画 -->
      <transition :name="tabAnim" mode="out-in">
        <div v-if="listTab === 'pending'" v-loading="listLoading" class="arrival-list" key="pending">
        <div
          v-for="arrival in arrivalList"
          :key="arrival.arrivalId"
          class="arrival-card"
          @click="openArrival(arrival)"
        >
          <div class="arrival-card__head">
            <span class="arrival-card__code">{{ arrival.arrivalCode }}</span>
            <span class="arrival-card__status">待收货</span>
          </div>
          <div class="arrival-card__row">供应商：{{ arrival.supplierName || '-' }}</div>
          <div class="arrival-card__row">
            到货：{{ arrival.totalQuantity }}　预计到货：{{ formatDate(arrival.estimatedArrivalDate) }}
          </div>
        </div>
        <div v-if="!listLoading && arrivalList.length === 0" class="empty-tip">暂无待收货到货单</div>
      </div>

      <!-- 历史收货列表 -->
      <div v-else-if="listTab === 'history'" class="history-panel" key="history">
        <div class="history-filter">
          <button class="history-filter__toggle" @click="historyFilterOpen = !historyFilterOpen">
            <el-icon :size="15"><Filter /></el-icon>
            筛选条件
            <span v-if="historyFilterActiveCount" class="history-filter__badge">{{ historyFilterActiveCount }}</span>
            <el-icon :size="14" class="history-filter__arrow" :class="{ 'is-open': historyFilterOpen }">
              <ArrowDown />
            </el-icon>
          </button>
          <div v-if="historyFilterOpen" class="history-filter__body">
            <div class="history-filter__row">
              <label class="history-filter__label">日期</label>
              <div class="history-filter__dates">
                <input v-model="historyDateStart" type="date" class="native-date" @change="onHistoryFilterChange" />
                <span class="history-filter__sep">至</span>
                <input v-model="historyDateEnd" type="date" class="native-date" @change="onHistoryFilterChange" />
              </div>
            </div>
            <div class="history-filter__row">
              <label class="history-filter__label">供应商</label>
              <select v-model="historySupplier" class="native-select" @change="onHistoryFilterChange">
                <option value="">全部供应商</option>
                <option v-for="opt in historySupplierOptions" :key="opt.value" :value="opt.value">
                  {{ opt.label }}
                </option>
              </select>
            </div>
            <div class="history-filter__actions">
              <button class="history-filter__reset" @click="resetHistoryFilter">重置</button>
            </div>
          </div>
        </div>
        <div v-loading="historyLoading" class="arrival-list">
          <div
            v-for="conf in historyList"
            :key="conf.confirmationId"
            class="arrival-card"
            @click="openHistoryDetail(conf)"
          >
            <div class="arrival-card__head">
              <span class="arrival-card__code">{{ conf.confirmationCode }}</span>
              <span class="history-status" :class="`is-${conf.status}`">{{ conf.status === 2 ? '已拒收' : '已确认' }}</span>
            </div>
            <div class="arrival-card__row">供应商：{{ conf.supplierName || '-' }}</div>
            <div class="arrival-card__row">
              实收 {{ conf.totalQuantity }}　{{ formatDate(conf.confirmTime) }}　¥{{ (conf.totalAmount ?? 0).toFixed(2) }}
            </div>
          </div>
          <button v-if="historyList.length && !historyFinished && !historyLoading" class="load-more-btn" @click="loadMoreHistory">
            加载更多
          </button>
          <div v-if="!historyLoading && historyList.length === 0" class="empty-tip">暂无历史收货记录</div>
        </div>
      </div>

      <!-- 历史收货详情（移动端只读） -->
      <div v-if="historyDetail" class="history-detail">
        <div class="history-detail__bar">
          <span>{{ historyDetail.confirmationCode }}</span>
          <button class="icon-btn" @click="historyDetail = null"><el-icon :size="20"><Close /></el-icon></button>
        </div>
        <div class="history-detail__body">
          <div class="history-detail__info">
            <div class="info-row">
              <span class="info-row__label">供应商</span>
              <span class="info-row__value info-row__value--supplier">{{ historyDetail.supplierName || '-' }}</span>
            </div>
            <div class="info-row">
              <span class="info-row__label">确认时间</span>
              <span class="info-row__value">{{ formatDateTime(historyDetail.confirmTime) }}</span>
            </div>
            <div class="info-grid">
              <div class="info-grid__cell">
                <div class="info-grid__num info-grid__num--qty">{{ historyDetail.totalQuantity }}</div>
                <div class="info-grid__cap">实收数量</div>
              </div>
              <div class="info-grid__cell">
                <div class="info-grid__num info-grid__num--amount">¥{{ (historyDetail.totalAmount ?? 0).toFixed(2) }}</div>
                <div class="info-grid__cap">确认金额</div>
              </div>
            </div>
          </div>
          <div class="history-detail__items">
            <div v-for="(item, idx) in historyDetail.items || []" :key="idx" class="history-item">
              <div class="history-item__name">
                {{ item.materialName }}<span v-if="item.specification">（{{ item.specification }}）</span>
              </div>
              <div class="history-item__meta">
                实收 {{ item.confirmedQuantity }} {{ item.unit || '' }}　拒收 {{ item.rejectedQuantity }}　批次 {{ item.batchNo || '-' }}
              </div>
              <div v-if="item.evidence?.length" class="history-item__evi">
                <img
                  v-for="(ev, i) in item.evidence"
                  :key="i"
                  :src="resolveUploadUrl(ev.photoUrl)"
                  class="history-item__img"
                  alt="留证"
                  @click="openPhotoViewer(resolveUploadUrl(ev.photoUrl))"
                />
              </div>
            </div>
            <div v-if="!(historyDetail.items?.length)" class="empty-tip">无明细</div>
          </div>
          <div class="history-detail__sign-title">
            签名凭证
            <span v-if="!historyDetailSigLoading && historyDetailSignatures.length < 2" class="history-detail__sign-miss">
              （{{ historyDetailSignatures.length === 0 ? '待签名' : '签名不完整' }}）
            </span>
            <button
              v-if="!historyDetailSigLoading && historyDetailSignatures.length < 2"
              class="re-sign-btn"
              type="button"
              @click="enterResign(historyDetail)"
            >补签</button>
          </div>
          <div v-if="historyDetailSigLoading" class="history-detail__hint">加载中...</div>
          <div v-else-if="historyDetailSignatures.length === 0" class="history-detail__hint">暂无签名记录</div>
          <div v-else>
            <div v-for="sig in historyDetailSignatures" :key="sig.signatureId" class="history-detail__sign">
              <img :src="sig.signatureImage" class="history-detail__sign-img" alt="签名" />
              <div class="history-detail__sign-meta">
                <div class="history-detail__sign-name">
                  {{ sig.signerName }}
                  <span class="history-detail__sign-tag">{{ sig.signerType === 'RECEIVER' ? '收货方' : '供应商' }}</span>
                </div>
                <div class="history-detail__sign-time">{{ formatDateTime(sig.signTime) }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 我的（清除缓存 / 退出登录） -->
      <div v-if="listTab === 'mine'" class="mine-view" key="mine">
        <div class="mine-view__account">
          <div class="mine-avatar">{{ (myUsername || '收')[0].toUpperCase() }}</div>
          <div class="mine-user">
            <div class="mine-user__name">{{ myUsername || '未登录' }}</div>
            <div class="mine-user__role">{{ receiverLabel }}</div>
          </div>
        </div>

        <div class="mine-list">
          <div class="mine-item is-clickable" @click="scanVerifyCode">
            <div class="mine-item__icon"><el-icon :size="18"><FullScreen /></el-icon></div>
            <div class="mine-item__body">
              <div class="mine-item__title">扫防伪码</div>
              <div class="mine-item__desc">扫描确认单上的二维码，查验真伪与打印记录</div>
            </div>
            <div class="mine-item__value">›</div>
          </div>
          <div class="mine-item">
            <div class="mine-item__icon"><el-icon :size="18"><Shop /></el-icon></div>
            <div class="mine-item__body">
              <div class="mine-item__title">当前门店</div>
              <div class="mine-item__desc">收货确认为{{ myStoreName ? '门店' : '总部' }}收货</div>
            </div>
            <div class="mine-item__value">{{ myStoreName || '总部仓库' }}</div>
          </div>

          <div class="mine-item">
            <div class="mine-item__icon"><el-icon :size="18"><Upload /></el-icon></div>
            <div class="mine-item__body">
              <div class="mine-item__title">留证上传</div>
              <div class="mine-item__desc">草稿中的拒收/验收照片补传</div>
            </div>
            <div v-if="pendingUploadCount > 0" class="mine-item__value">{{ pendingUploadCount }} 张待传</div>
            <button class="mine-item__action" :disabled="uploadingOffline || pendingUploadCount === 0" @click="uploadOfflineNow">
              {{ uploadingOffline ? '上传中' : '立即上传' }}
            </button>
          </div>

          <div class="mine-item">
            <div class="mine-item__icon"><el-icon :size="18"><Delete /></el-icon></div>
            <div class="mine-item__body">
              <div class="mine-item__title">清除缓存</div>
              <div class="mine-item__desc">清除界面资源缓存，保留账号与草稿</div>
            </div>
            <div class="mine-item__value">{{ cacheSizeText }}</div>
            <button class="mine-item__action" :disabled="clearingCache" @click="clearAppCache">
              {{ clearingCache ? '清理中' : '清除' }}
            </button>
          </div>

          <div class="mine-item">
            <div class="mine-item__icon"><el-icon :size="18"><Box /></el-icon></div>
            <div class="mine-item__body">
              <div class="mine-item__title">关于系统</div>
              <div class="mine-item__desc">收货验收终端</div>
            </div>
            <div class="mine-item__value">{{ APP_VERSION }}</div>
          </div>
        </div>

        <button class="mine-logout" @click="handleLogout">
          <el-icon :size="16"><SwitchButton /></el-icon>
          退出登录
        </button>
      </div>
      </transition>

      <!-- 底部 Tab 导航（Android Material 风格） -->
      <nav class="tab-nav">
        <button
          class="tab-nav__item"
          :class="{ 'is-active': listTab === 'pending' }"
          @click="switchTab('pending')"
        >
          <span class="tab-nav__icon">
            <el-icon :size="20"><Box /></el-icon>
            <span v-if="arrivalList.length" class="tab-nav__badge">{{ arrivalList.length }}</span>
          </span>
          <span class="tab-nav__label">待收货</span>
        </button>
        <button
          class="tab-nav__item"
          :class="{ 'is-active': listTab === 'history' }"
          @click="switchTab('history')"
        >
          <span class="tab-nav__icon">
            <el-icon :size="20"><Clock /></el-icon>
          </span>
          <span class="tab-nav__label">历史</span>
        </button>
        <button
          class="tab-nav__item"
          :class="{ 'is-active': listTab === 'mine' }"
          @click="switchTab('mine')"
        >
          <span class="tab-nav__icon">
            <el-icon :size="20"><User /></el-icon>
          </span>
          <span class="tab-nav__label">我的</span>
        </button>
      </nav>
    </template>

    <!-- 验收视图（参考 PDA 手持收货 V4 精修版） -->
    <template v-else>
      <!-- 头部 -->
      <header class="mobile-header">
        <div class="mobile-header__left">
          <button class="icon-btn" @click="backToList"><el-icon :size="20"><ArrowLeft /></el-icon></button>
          <div class="mobile-header__info">
            <div class="mobile-header__code">{{ activeArrival.arrivalCode }}</div>
            <div class="mobile-header__sup">{{ activeArrival.supplierName }} · {{ items.length }}项</div>
          </div>
        </div>
        <span class="mobile-header__badge" :class="{ 'is-done': signingConfirmation && supplierSigned }">
          {{ signingConfirmation ? (supplierSigned ? '已签名' : '待签字') : '收货中' }}
        </span>
      </header>

      <!-- 草稿恢复横幅 -->
      <div v-if="restoreHint" class="restore-banner">
        <div class="restore-banner__text">{{ restoreHint }}</div>
        <div class="restore-banner__actions">
          <button class="restore-btn restore-btn--ghost" @click="discardDraft">放弃草稿</button>
          <button class="restore-btn" @click="applyDraft">继续录入</button>
        </div>
      </div>

      <!-- 签字存证步骤（收货方自动签名 → 司机手写） -->
      <div v-if="signingConfirmation" class="sign-step">
        <div class="sign-step__intro">
          <div class="sign-step__title">收货确认成功，请完成司机签字存证</div>
          <div class="sign-step__sub">收货方（{{ myRealName || myUsername || '当前账号' }}）已自动签名 · 司机签名作为收货凭据</div>
        </div>

        <!-- 司机签字（横置签名区） -->
        <div class="sign-panel">
          <div class="sign-panel__label">供应商（司机）签字<span class="req-mark">*</span></div>
          <el-input v-model="signSupplierName" class="sign-panel__name" placeholder="司机/送货员姓名" maxlength="30" />
          <SignaturePad v-model="signSupplierSig" :height="150" class="sign-panel__pad" placeholder="司机手写签名（横屏）" />
          <div class="sign-panel__actions">
            <span class="sign-panel__hint" :class="{ 'is-done': supplierSigned }">
              {{ supplierSigned ? '已签名存证 ✓' : signSupplierSig ? '已签名，可确认保存' : '请司机手写签名' }}
            </span>
            <button class="landscape-btn" type="button" @click="openLandscapePad('supplier')">横屏书写</button>
            <button class="sign-btn" type="button" :disabled="signing || !signSupplierSig || !signSupplierName.trim()" @click="handleSignSupplier">
              {{ signing ? '保存中...' : supplierSigned ? '确认存证' : '确认供应商签名' }}
            </button>
          </div>
        </div>

        <div class="sign-step__footer">
          <span class="sign-step__tip">司机未在可在「收货确认单」中补签</span>
          <button class="sign-btn sign-btn--ghost" type="button" :disabled="signing" @click="finishSigning">完成，返回列表</button>
        </div>
      </div>

      <template v-else>
      <!-- 折叠1：整单快捷设置批次（可选） -->
      <div class="collapsible">
        <div class="collapsible__head" @click="quickBatchExpanded = !quickBatchExpanded">
          <span>📋 整单快捷设置批次（可选）</span>
          <span class="collapsible__arrow" :class="{ 'is-open': quickBatchExpanded }">▼</span>
        </div>
        <div v-show="quickBatchExpanded" class="collapsible__body batch-body">
          <div class="collapsible__row">
            <label>批次号</label>
            <el-input v-model="quickBatch.batchNo" placeholder="同车同批时填写" class="flex-input" />
          </div>
          <div class="collapsible__row">
            <label>生产日期</label>
            <input v-model="quickBatch.productionDate" type="date" class="native-date" />
          </div>
          <div class="collapsible__row">
            <label>有效期至</label>
            <input v-model="quickBatch.expiryDate" type="date" class="native-date" />
          </div>
          <button
            class="collapsible__apply-btn"
            :disabled="!quickBatch.batchNo && !quickBatch.productionDate && !quickBatch.expiryDate"
            @click="applyQuickBatch"
          >
            应用到全部接收项
          </button>
          <div class="collapsible__tip">生鲜时令蔬菜通常无批次，可忽略此项；也可在各商品卡内单独录入</div>
          <div class="collapsible__row">
            <label>整单备注</label>
            <el-input v-model="remark" placeholder="可选" class="flex-input" />
          </div>
        </div>
      </div>

      <!-- 折叠2：快速定位商品（扫码/搜索跳转） -->
      <div class="collapsible collapsible--locator">
        <div class="collapsible__head" @click="locatorExpanded = !locatorExpanded">
          <span>🔎 快速定位商品（扫码/搜索跳转）</span>
          <span class="collapsible__arrow" :class="{ 'is-open': locatorExpanded }">▼</span>
        </div>
        <div v-show="locatorExpanded" class="collapsible__body">
          <div class="search-row">
            <button class="scan-btn" @click="openLiveScanner" title="摄像头扫码识别货物">
              <el-icon :size="18"><Aim /></el-icon>
            </button>
            <el-input
              v-model="locatorKeyword"
              placeholder="输入商品名 / 规格搜索..."
              class="flex-input"
              @input="onLocatorInput"
            />
          </div>
          <div class="search-result-hint">{{ locatorHint }}</div>
          <div v-if="locatorMatches.length" class="search-results">
            <button
              v-for="m in locatorMatches"
              :key="m.index"
              class="search-result-chip"
              @click="jumpToLocatorMatch(m.index)"
            >
              <span class="search-result-chip__dot" :class="`is-${itemState(m.item)}`"></span>
              <span class="search-result-chip__name">{{ m.item.materialName }}</span>
              <span class="search-result-chip__state">{{ itemState(m.item) === 'done' ? '已完成' : itemState(m.item) === 'reject' ? '已拒收' : '待录入' }}</span>
            </button>
          </div>
          <div v-if="barcodeMatch" class="search-result-hint" :class="{ 'search-result-hint--ok': matchedItemIndex >= 0 }">
            {{ barcodeMatch }}
          </div>
        </div>
      </div>

      <!-- 进度 + 智能目录 -->
      <div class="nav-sticky">
        <div class="progress-row">
          <span class="progress-row__label">进度</span>
          <div class="progress-track"><div class="progress-fill" :style="{ width: progressPercent }"></div></div>
          <span class="progress-text">{{ enteredCount }}/{{ items.length }}</span>
        </div>
        <div class="chip-scroll">
          <button
            v-for="(item, index) in items"
            :key="item.arrivalItemId"
            :id="`chip-${index}`"
            class="chip"
            :class="[
              { 'chip--active': index === stepIndex, 'chip--match': matchedItemIndex === index },
              `chip--${itemState(item)}`,
            ]"
            @click="scrollToItem(index)"
          >
            <span class="chip__dot"></span>
            <span class="chip__name">{{ item.materialName }}</span>
            <span v-if="index === stepIndex" class="chip__tag">当前</span>
            <span v-if="matchedItemIndex === index" class="chip__tag chip__tag--match">匹配</span>
          </button>
        </div>
      </div>

      <!-- 卡片区：单卡聚焦当前商品 -->
      <div class="card-area">
        <div
          :id="`item-card-${stepIndex}`"
          class="item-card"
          :class="[
            { 'is-matched': matchedItemIndex === stepIndex, 'is-invalid': invalidItemIndex === stepIndex },
            `is-${itemState(items[stepIndex])}`,
          ]"
        >
          <div class="card-top">
            <div>
              <div class="card-top__name">{{ items[stepIndex].materialName }}</div>
              <div class="card-top__spec">{{ items[stepIndex].specification || '无规格' }} · {{ items[stepIndex].unit || '-' }}</div>
            </div>
            <div class="card-top__order">
              <div class="card-top__order-lbl">下单</div>
              <div>
                <span class="card-top__order-num">{{ items[stepIndex].expectedQuantity }}</span>
                <span class="card-top__order-unit">{{ items[stepIndex].unit || '' }}</span>
              </div>
            </div>
          </div>

          <button v-if="canManualMatch()" class="match-btn" @click="manualMatchItem(stepIndex)">
            关联此物料
          </button>

          <!-- 实收行：标签与输入框同一行（整数单位不显示小数，重量单位保留 2 位） -->
          <div class="qty-row">
            <label>{{ items[stepIndex].accept === 'reject' ? '拒收' : '实收' }}</label>
            <el-input-number
              v-model="items[stepIndex].quantity"
              :min="0"
              :precision="qtyPrecision(items[stepIndex].unit)"
              :step="qtyStep(items[stepIndex].unit)"
              :controls="false"
              class="qty-input"
              @focus="onQtyFocus"
            />
            <span class="qty-row__unit">{{ items[stepIndex].unit || '' }}</span>
          </div>
          <div
            v-if="items[stepIndex].accept === 'receive' && (items[stepIndex].quantity ?? 0) < getRemaining(items[stepIndex])"
            class="partial-hint"
          >
            部分收货：差额 {{ Number((getRemaining(items[stepIndex]) - (items[stepIndex].quantity ?? 0)).toFixed(2)) }} 将计为拒收
          </div>

          <!-- 接收 / 拒收 -->
          <div class="accept-group">
            <button
              class="accept-btn accept-btn--receive"
              :class="{ 'is-active': items[stepIndex].accept === 'receive' }"
              @click="setAccept(items[stepIndex], 'receive')"
            >
              <span class="accept-btn__icon">✓</span>接收
            </button>
            <button
              class="accept-btn accept-btn--reject"
              :class="{ 'is-active': items[stepIndex].accept === 'reject' }"
              @click="setAccept(items[stepIndex], 'reject')"
            >
              <span class="accept-btn__icon">✗</span>拒收
            </button>
          </div>

          <!-- 接收商品：批次信息 + 质检结果 -->
          <div v-if="items[stepIndex].accept === 'receive'" class="detail-section">
            <div class="detail-row">
<label>批次号<span class="req-mark">*</span></label>
              <div class="batch-row">
                <el-input
                  v-model="items[stepIndex].batchNo"
                  placeholder="扫码/OCR/手动输入批次号"
                  class="flex-input"
                  :class="{ 'field-error': (items[stepIndex].quantity ?? 0) > 0 && !items[stepIndex].batchNo?.trim() }"
                />
                <label class="scan-btn" title="扫码识别该商品批次号（相机实时识别）" @click.prevent="setBatchTarget(stepIndex); scanBatchBarcode()">
                  <el-icon :size="16"><Camera /></el-icon>
                </label>
                <label class="scan-btn scan-btn--ghost" for="batch-file-input" title="相册识别该商品批次号" @click="setBatchTarget(stepIndex)">
                  <el-icon :size="16"><Picture /></el-icon>
                </label>
              </div>
            </div>
            <div class="detail-row">
              <label>生产日期</label>
              <input v-model="items[stepIndex].productionDate" type="date" class="native-date" />
            </div>
            <div class="detail-row">
              <label>有效期至</label>
              <input v-model="items[stepIndex].expiryDate" type="date" class="native-date" />
            </div>
            <div class="detail-row">
              <label>质检<span class="req-mark">*</span></label>
              <div class="qc-group">
                <button
                  v-for="opt in QualityCheckResultOptions"
                  :key="opt.value"
                  class="qc-btn"
                  :class="{ 'is-active': items[stepIndex].qualityCheckResult === opt.value }"
                  @click="items[stepIndex].qualityCheckResult = opt.value"
                >
                  {{ opt.label }}
                </button>
              </div>
            </div>
          </div>

          <!-- 拒收商品：原因 + 现场拍照留证 -->
          <div v-if="items[stepIndex].accept === 'reject'" class="detail-section">
            <div class="detail-row" :class="{ 'field-error': (items[stepIndex].quantity ?? 0) > 0 && !items[stepIndex].remark?.trim() }">
              <label>原因<span class="req-mark">*</span></label>
              <el-input v-model="items[stepIndex].remark" placeholder="必填（拒收必须填写原因）" class="flex-input" @focus="scrollInputIntoView" />
            </div>
            <div class="detail-row">
              <label>留证</label>
              <label
                class="evidence-btn"
                :class="{ 'evidence-btn--done': items[stepIndex].evidence?.length }"
                @click.prevent="startRejectEvidence(stepIndex); takePhoto(async (file) => { const f = { target: { files: [file] } } as unknown as Event; await onRejectEvidenceFileChange(f) })"
              >
                <el-icon :size="16"><Camera /></el-icon>
                {{ items[stepIndex].evidence?.length ? '已留证' : '拍照留证（拒收必拍）' }}
              </label>
            </div>
            <div v-if="items[stepIndex].evidence?.length" class="evidence-thumbs">
              <div v-for="(ev, evIdx) in items[stepIndex].evidence" :key="evIdx" class="evidence-thumb">
                <img :src="ev.localDataUrl || resolveUploadUrl(ev.photoUrl)" alt="留证照片" />
                <span class="evidence-thumb__badge" :class="{ 'is-pending': ev.pendingUpload }">
                  {{ ev.pendingUpload ? '待上传' : ev.evidenceType === 'MANUAL_MATCH' ? '关联' : '拒收' }}
                </span>
                <button class="evidence-thumb__remove" @click="removeEvidence(items[stepIndex], evIdx)">×</button>
              </div>
            </div>
            <div v-if="items[stepIndex].evidence?.some(e => e.pendingUpload)" class="pending-upload-tip">
              照片暂存本地（网络恢复后自动上传）
            </div>
          </div>
        </div>
      </div>

      <!-- 底部 -->
      <div class="bottom-bar">
        <div class="nav-controls">
          <button class="nav-btn" :disabled="!hasPrevPending" @click="gotoPending(-1)">
            ⏪ 跳转至待处理
          </button>
          <span class="nav-center">当前: <strong>{{ items[stepIndex]?.materialName || '-' }}</strong> ({{ stepIndex + 1 }}/{{ items.length }})</span>
          <button class="nav-btn nav-btn--primary" :disabled="!hasNextPending" @click="gotoPending(1)">
            跳转至待处理 ⏩
          </button>
        </div>
        <div class="submit-row">
          <div class="summary-mini">
            <div class="summary-mini__itm">
              <span class="summary-mini__lbl">已确认</span>
              <span class="summary-mini__val summary-mini__val--ok">{{ totalConfirmed }}</span>
            </div>
            <div class="summary-mini__itm">
              <span class="summary-mini__lbl">拒收</span>
              <span class="summary-mini__val summary-mini__val--rej">{{ totalRejected }}</span>
            </div>
          </div>
          <button class="submit-btn" :disabled="submitting" @click="handleSubmit">
            {{ submitting ? '提交中...' : allRejected ? '确认拒收' : '确认收货' }}
          </button>
        </div>
      </div>
      <!-- 图片条码识别隐藏容器 -->
      <div id="batch-file-scanner-region" style="display: none" />
      <!-- 相册选图（label 原生关联触发，iOS 可靠拉起） -->
      <input
        id="batch-file-input"
        type="file"
        accept="image/*"
        class="hidden-file-input"
        @change="handleBatchFileChange"
      />
      <!-- 拍照识别（capture=environment 直接调系统相机，无需 HTTPS 摄像头权限） -->
      <input
        id="batch-camera-input"
        type="file"
        accept="image/*"
        capture="environment"
        class="hidden-file-input"
        @change="handleBatchFileChange"
      />
      <!-- 拒收留证拍照（强制相机现场拍摄，禁止相册） -->
      <input
        id="reject-evidence-input"
        type="file"
        accept="image/*"
        capture="environment"
        class="hidden-file-input"
        @change="onRejectEvidenceFileChange"
      />
      <!-- 人工关联留证拍照（强制相机现场拍摄） -->
      <input
        id="manual-evidence-input"
        type="file"
        accept="image/*"
        capture="environment"
        class="hidden-file-input"
        @change="onManualCameraChange"
      />

      <!-- 裁剪重试：框选条码区域 -->
      <div v-if="cropVisible" class="crop-overlay">
        <div class="crop-overlay__bar">
          <span>框选条码区域</span>
          <button class="icon-btn" @click="closeCrop"><el-icon :size="20"><Close /></el-icon></button>
        </div>
        <div
          ref="cropAreaRef"
          class="crop-area"
          @pointermove="onCropPointerMove"
          @pointerup="onCropPointerUp"
        >
          <img :src="cropImageUrl" alt="待裁剪图片" class="crop-area__img" />
          <div
            class="crop-rect"
            :style="{ left: cropRect.x + 'px', top: cropRect.y + 'px', width: cropRect.w + 'px', height: cropRect.h + 'px' }"
            @pointerdown="onCropPointerDown($event, 'move')"
          >
            <span class="crop-rect__handle" @pointerdown.stop="onCropPointerDown($event, 'resize')" />
          </div>
        </div>
        <div class="crop-overlay__tip">拖动框选条码（四周留少量空白），拖动右下角调整大小</div>
        <div v-if="lastDiag" class="crop-overlay__diag">诊断：{{ lastDiag }}</div>
        <div class="crop-overlay__actions">
          <button class="crop-btn crop-btn--ghost" @click="closeCrop">取消</button>
          <button class="crop-btn" @click="confirmCropAndRecognize">确认识别</button>
        </div>
      </div>

      <!-- 摄像头实时扫码遮罩 -->
      <div v-if="liveScanVisible" class="live-scan">
        <div class="live-scan__bar">
          <span>摄像头扫码</span>
          <button class="icon-btn" @click="closeLiveScanner"><el-icon :size="20"><Close /></el-icon></button>
        </div>
        <div v-if="!liveScanError" id="live-scanner-region" class="live-scan__video" />
        <div v-else class="live-scan__error">{{ liveScanError }}</div>
        <div class="live-scan__tip">{{ liveScanError || '将外箱条码对准画面，自动识别后填入' }}</div>
      </div>

      <!-- OCR 识别结果确认：预填提取字段（可编辑）+ 全文复制 -->
      <div v-if="ocrConfirmVisible" class="ocr-confirm">
        <div class="ocr-confirm__bar">
          <span>识别结果确认</span>
          <button class="icon-btn" @click="ocrConfirmVisible = false"><el-icon :size="20"><Close /></el-icon></button>
        </div>
        <div class="ocr-confirm__body">
          <div class="ocr-confirm__fields-tip">
            应用到：第 {{ batchTargetIndex + 1 }} 项「{{ items[batchTargetIndex]?.materialName }}」· 可直接修改下方字段
          </div>
          <div class="field-row">
            <span class="field-label">批次号</span>
            <el-input v-model="ocrBatch" placeholder="未识别到批次号，可手动输入" class="flex-input" />
          </div>
          <div class="field-row">
            <span class="field-label">生产日期</span>
            <input v-model="ocrProdDate" type="date" class="native-date" />
          </div>
          <div class="field-row">
            <span class="field-label">有效期至</span>
            <input v-model="ocrExpiryDate" type="date" class="native-date" />
          </div>
          <div v-if="ocrNote" class="ocr-confirm__note">{{ ocrNote }}</div>
          <div class="ocr-confirm__text-label">识别到的文本（可编辑，用于重新提取）</div>
          <textarea v-model="ocrConfirmText" class="ocr-confirm__text" rows="6" />
          <button class="ocr-confirm__copy" @click="copyOcrText">复制识别文本</button>
        </div>
        <div class="ocr-confirm__actions">
          <button class="crop-btn crop-btn--ghost" @click="ocrConfirmVisible = false">放弃</button>
          <button class="crop-btn" @click="applyOcrConfirm">应用识别结果</button>
        </div>
      </div>

      <!-- 人工关联：原因 + 现场拍照留证（防货不对码滥用） -->
      <div v-if="manualMatchDialog" class="manual-match">
        <div class="manual-match__bar">
          <span>人工关联物料</span>
          <button class="icon-btn" @click="manualMatchDialog = false"><el-icon :size="20"><Close /></el-icon></button>
        </div>
        <div class="manual-match__body">
          <div class="manual-match__target">
            关联到：第 {{ manualMatchIndex + 1 }} 项「{{ items[manualMatchIndex]?.materialName || '-' }}」
            <span v-if="barcodeNo" class="manual-match__barcode">条码：{{ barcodeNo }}</span>
          </div>
          <div class="manual-match__warn">条码与货物不一致时人工确认，必须现场拍照留证（仅调用相机，禁止相册照片）</div>
          <div class="field-row">
            <span class="field-label">关联原因</span>
            <el-input
              v-model="manualMatchReason"
              type="textarea"
              :rows="2"
              placeholder="必填，如：货不对码，目视确认为该物料"
              class="flex-input"
            />
          </div>
          <div class="manual-match__photo">
            <template v-if="!manualMatchPhoto">
              <label class="evidence-btn evidence-btn--large" :class="{ 'is-loading': manualMatchUploading }" @click.prevent="takePhoto(async (file) => { const f = { target: { files: [file] } } as unknown as Event; await onManualCameraChange(f) })">
                <el-icon :size="18"><Camera /></el-icon>
                {{ manualMatchUploading ? '上传中...' : '现场拍照（必拍）' }}
              </label>
              <div class="manual-match__hint">拍摄货物实物/外包装，作为人工关联证据</div>
            </template>
            <template v-else>
              <div class="manual-match__photo-done">
                <img :src="manualMatchPhoto.localDataUrl || resolveUploadUrl(manualMatchPhoto.photoUrl)" alt="留证照片" />
                <div class="manual-match__meta">
                  <span v-if="manualMatchPhoto.pendingUpload" class="manual-match__meta-miss">照片暂存本地，提交时自动上传</span>
                  <span v-if="manualMatchPhoto.photoTakenAt">拍摄时间：{{ manualMatchPhoto.photoTakenAt }}</span>
                  <span v-if="manualMatchPhoto.deviceModel">设备：{{ manualMatchPhoto.deviceModel }}</span>
                  <span v-if="manualMatchPhoto.gps">定位：{{ manualMatchPhoto.gps }}</span>
                  <span v-if="!manualMatchPhoto.photoTakenAt && !manualMatchPhoto.deviceModel" class="manual-match__meta-miss">元数据缺失（照片可能非现场拍摄）</span>
                </div>
                <button class="manual-match__retake" @click="manualMatchPhoto = null">重新拍摄</button>
              </div>
            </template>
          </div>
        </div>
        <div class="manual-match__actions">
          <button class="crop-btn crop-btn--ghost" @click="manualMatchDialog = false">取消</button>
          <button class="crop-btn" @click="confirmManualMatch">确认关联</button>
        </div>
      </div>
      </template>
    </template>

    <!-- 全屏图片查看器（双指缩放/单指拖动/单击关闭/双击缩放） -->
    <div v-if="photoViewer" class="photo-viewer" @touchstart.prevent="onPhotoViewerTouchStart" @touchmove.prevent="onPhotoViewerTouchMove" @touchend="onPhotoViewerTouchEnd" @touchend.capture.prevent>
      <img
        :src="photoViewer.url"
        class="photo-viewer__img"
        :style="{ transform: `translate(${photoViewer.translateX}px, ${photoViewer.translateY}px) scale(${photoViewer.scale})` }"
      />
    </div>

    <!-- 横屏书写覆盖层（旋转 90° 模拟横屏：absolute+translate 居中保证满屏，不依赖系统旋转 API） -->
    <!-- 横屏书写覆盖层（全屏横向签名区：竖屏坐标直排布局，按钮正常可见，签名区最大化） -->
    <div v-if="landscapePad" class="landscape-pad">
      <!-- 旋转 90° 横屏签名区（视觉横屏：宽=屏高、高=屏宽） -->
      <div class="landscape-pad__rotate">
        <div class="landscape-pad__bar">
          <span class="landscape-pad__title">{{ landscapePad === 'receiver' ? '收货方签名' : '司机签名' }}</span>
          <button class="landscape-pad__close" type="button" @click="closeLandscapePad">✕</button>
        </div>
        <div class="landscape-pad__area">
          <div class="landscape-pad__watermark">{{ landscapePad === 'receiver' ? '收货方签名' : '司机签名' }}</div>
          <SignaturePad
            v-if="landscapePad === 'receiver'"
            v-model="signReceiverSig"
            :height="landscapePadHeight"
            placeholder="在此横向手写签名"
            ref="landscapePadRef"
          />
          <SignaturePad
            v-else
            v-model="signSupplierSig"
            :height="landscapePadHeight"
            placeholder="在此横向手写签名"
            ref="landscapePadRef"
          />
        </div>
      </div>
      <!-- 操作按钮：竖屏坐标底部固定（旋转不挤压，始终可见） -->
      <div class="landscape-pad__actions">
        <button class="sign-btn sign-btn--ghost" type="button" @click="closeLandscapePad">取消</button>
        <button
          class="sign-btn"
          type="button"
          :disabled="landscapePad === 'receiver' ? !signReceiverSig : !signSupplierSig"
          @click="confirmLandscapePad"
        >完成签名</button>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.mobile-page {
  max-width: 560px;
  margin: 0 auto;
  min-height: 100vh;
  min-height: 100dvh; /* iOS Safari 地址栏伸缩时视口高度稳定 */
  background: var(--fts-bg-page);
  padding-bottom: calc(24px + max(max(env(safe-area-inset-bottom, 0px), var(--fts-safe-bottom, 0px)), var(--fts-safe-bottom, 0px)));
  -webkit-text-size-adjust: 100%; /* 禁止 iOS 自动调整文字大小 */

  /* 验收视图：底部固定栏占位，避免内容被遮挡 */
  &.has-bottom-bar {
    padding-bottom: calc(120px + max(max(env(safe-area-inset-bottom, 0px), var(--fts-safe-bottom, 0px)), var(--fts-safe-bottom, 0px)));
  }
}

/* 签字存证步骤（收货确认成功后） */
.sign-step {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 14px;
  padding-bottom: calc(24px + max(max(env(safe-area-inset-bottom, 0px), var(--fts-safe-bottom, 0px)), var(--fts-safe-bottom, 0px)));

  &__intro {
    padding: 16px 14px;
    border-radius: var(--fts-radius-base);
    border: 1px solid rgba(0, 128, 0, 0.35);
    background: rgba(0, 128, 0, 0.07);
  }

  &__title {
    font-size: 16px;
    font-weight: 800;
    color: var(--fts-text-primary);
  }

  &__sub {
    margin-top: 4px;
    font-size: 12px;
    color: var(--fts-text-secondary);
  }

  &__footer {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    padding-top: 4px;
  }

  &__tip {
    font-size: 12px;
    color: var(--fts-text-tertiary);
  }
}

.sign-panel {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px;
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-base);
  background: var(--fts-bg-card);

  &__label {
    font-size: 13px;
    font-weight: 700;
    color: var(--fts-text-primary);
  }

  &__name {
    width: 100%;
  }

  &__actions {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
  }

  &__hint {
    font-size: 12px;
    color: var(--fts-text-tertiary);

    &.is-done {
      color: var(--fts-success);
      font-weight: 600;
    }
  }

  /* 横置签名区：画布随容器宽自适应，高度 150px 保证横屏书写空间 */
  &__pad :deep(.signature-pad__canvas) {
    height: 150px;
  }
}

/* 双签两步导航 */
.sign-tabs {
  display: flex;
  gap: 8px;
}

.sign-tab {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 0;
  border: 1px solid var(--fts-border-primary);
  border-radius: 10px;
  background: var(--fts-bg-card);
  color: var(--fts-text-secondary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;

  &__dot {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 20px;
    height: 20px;
    border-radius: 50%;
    background: var(--fts-bg-subtle);
    color: var(--fts-text-secondary);
    font-size: 12px;
    font-weight: 700;
  }

  &.is-active {
    border-color: var(--fts-primary);
    background: var(--fts-primary-bg);
    color: var(--fts-primary);
  }

  &.is-done .sign-tab__dot {
    background: var(--fts-success);
    color: #fff;
  }
}

.sign-btn {
  flex-shrink: 0;
  padding: 10px 16px;
  border: none;
  border-radius: 10px;
  background: var(--fts-primary);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  &--ghost {
    background: #fff;
    color: var(--fts-primary);
    border: 1px solid var(--fts-primary);
  }
}

.mobile-header {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: calc(12px + max(max(env(safe-area-inset-top, 0px), var(--fts-header-top, var(--fts-safe-top, 0px))), var(--fts-header-top, var(--fts-safe-top, 0px)))) 14px 12px; /* 刘海屏安全区，手机端紧凑 */
  background: var(--fts-bg-card);
  border-bottom: 1px solid var(--fts-border-secondary);

  &__left {
    display: flex;
    align-items: center;
    gap: 10px;
    min-width: 0;
  }

  &__right {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  &__title {
    font-size: 17px;
    font-weight: 700;
    color: var(--fts-text-primary);
  }

  &__sub {
    font-size: 13px;
    color: var(--fts-text-secondary);
  }

  &__info {
    min-width: 0;
  }

  &__code {
    font-size: 14px;
    font-weight: 700;
    color: var(--fts-text-primary);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__sup {
    font-size: 12px;
    color: var(--fts-text-secondary);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__ver {
    padding: 1px 6px;
    border-radius: 4px;
    background: var(--fts-bg-subtle);
    color: var(--fts-text-tertiary);
    font-size: 10px;
  }
}

/* iOS Safari：输入控件字号 ≥16px 才能避免聚焦自动放大页面 */
:deep(.el-input__inner),
:deep(.el-input-number .el-input__inner),
:deep(.el-date-editor .el-input__inner),
:deep(.el-textarea__inner),
:deep(.el-select__wrapper) {
  font-size: 16px;
}

:deep(.el-input-number .el-input__inner) {
  text-align: right;
  font-weight: 600;
  font-size: 17px;
}

/* 头部状态徽章（收货中） */
.mobile-header__badge {
  flex-shrink: 0;
  padding: 3px 12px;
  border-radius: 999px;
  background: var(--fts-warning-bg);
  color: var(--fts-warning);
  font-size: 12px;
  font-weight: 600;
}

/* 草稿恢复横幅（断点继续录入） */
.restore-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin: 8px 12px 0;
  padding: 10px 12px;
  border: 1px solid rgba(26, 95, 180, 0.4);
  border-radius: 12px;
  background: var(--fts-primary-bg);

  &__text {
    flex: 1;
    min-width: 0;
    font-size: 12px;
    line-height: 1.5;
    color: var(--fts-primary);
  }

  &__actions {
    display: flex;
    gap: 8px;
    flex-shrink: 0;
  }
}

.restore-btn {
  padding: 7px 14px;
  border: 1px solid var(--fts-primary);
  border-radius: 8px;
  background: var(--fts-primary);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;

  &--ghost {
    background: var(--fts-bg-card);
    color: var(--fts-text-secondary);
    border-color: var(--fts-border-primary);
  }
}

.pending-upload-tip {
  padding: 6px 10px;
  border-radius: 8px;
  background: var(--fts-warning-bg);
  color: var(--fts-warning);
  font-size: 12px;
}

/* ========== 顶部折叠面板（参考 PDA 手持收货 V4） ========== */
.collapsible {
  margin: 8px 12px 0;
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: 12px;
  overflow: hidden;

  &--locator {
    border-color: var(--fts-primary);
    box-shadow: 0 2px 8px rgba(37, 99, 235, 0.06);

    .collapsible__head {
      background: var(--fts-primary-bg);
      color: var(--fts-primary);
    }
  }

  &__head {
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 10px 14px;
    border: none;
    background: var(--fts-bg-subtle);
    font-size: 13px;
    font-weight: 600;
    color: var(--fts-text-secondary);
    cursor: pointer;
    user-select: none;
    -webkit-tap-highlight-color: transparent;
  }

  &__arrow {
    font-size: 11px;
    transition: transform 0.2s;

    &.is-open {
      transform: rotate(180deg);
    }
  }

  &__body {
    padding: 10px 12px 12px;
    border-top: 1px solid var(--fts-border-secondary);
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  &__row {
    display: flex;
    align-items: center;
    gap: 8px;

    > label {
      flex-shrink: 0;
      width: 64px;
      font-size: 12px;
      color: var(--fts-text-secondary);
    }

    .flex-input,
    .native-date {
      flex: 1;
      min-width: 0;
    }
  }

  &__apply-btn {
    width: 100%;
    height: 38px;
    border: none;
    border-radius: 10px;
    background: var(--fts-primary);
    color: #fff;
    font-weight: 600;
    font-size: 14px;
    cursor: pointer;

    &:disabled {
      opacity: 0.5;
      pointer-events: none;
    }
  }

  &__tip {
    font-size: 11px;
    color: var(--fts-text-tertiary);
    line-height: 1.5;
  }
}

/* 快速定位：扫码 + 搜索 */
.search-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.search-result-hint {
  padding: 6px 12px;
  border-radius: 8px;
  background: var(--fts-bg-subtle);
  font-size: 13px;
  color: var(--fts-text-secondary);

  &--ok {
    background: var(--fts-success-bg);
    color: var(--fts-success);
  }
}

.search-results {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.search-result-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border: 1px solid var(--fts-primary);
  border-radius: 12px;
  background: var(--fts-primary-bg);
  font-size: 13px;
  color: var(--fts-primary);
  cursor: pointer;

  &__dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    flex-shrink: 0;

    &.is-pending {
      background: var(--fts-warning);
    }

    &.is-done {
      background: var(--fts-success);
    }

    &.is-reject {
      background: var(--fts-error);
    }
  }

  &__name {
    max-width: 160px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__state {
    opacity: 0.85;
  }
}

/* ========== 底部固定栏（参考 PDA 手持收货 V4）：导航 + 汇总 + 确认 ========== */
/* 手机端（375–430px）紧凑布局：两行紧凑、各元素略缩；fixed 恒定吸附视口底端 */
.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9;
  max-width: 560px; /* 与 .mobile-page 同宽，桌面居中 */
  margin: 0 auto;
  background: var(--fts-bg-card);
  border-top: 1px solid var(--fts-border-secondary);
  padding: 8px 12px calc(8px + max(max(env(safe-area-inset-bottom, 0px), var(--fts-safe-bottom, 0px)), var(--fts-safe-bottom, 0px)));
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.06);
}

.nav-controls {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  margin-bottom: 8px;
}

.submit-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.nav-btn {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  height: 34px;
  padding: 0 10px;
  border: 1px solid var(--fts-border-primary);
  border-radius: 8px;
  background: var(--fts-bg-card);
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
  color: var(--fts-text-primary);
  cursor: pointer;
  flex-shrink: 0;

  &:disabled {
    opacity: 0.3;
    pointer-events: none;
  }

  &--primary {
    background: var(--fts-primary);
    border-color: var(--fts-primary);
    color: #fff;
  }
}

.nav-center {
  flex: 1;
  min-width: 0;
  text-align: center;
  font-size: 12px;
  color: var(--fts-text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;

  strong {
    color: var(--fts-text-primary);
  }
}

.summary-mini {
  display: flex;
  align-items: center;
  gap: 14px;
  flex: 1;

  &__itm {
    display: flex;
    align-items: baseline;
    gap: 4px;
    white-space: nowrap;
  }

  &__lbl {
    font-size: 12px;
    color: var(--fts-text-secondary);
  }

  &__val {
    font-size: 18px;
    font-weight: 800;
    font-variant-numeric: tabular-nums;

    &--ok {
      color: var(--fts-success);
    }

    &--rej {
      color: var(--fts-error);
    }
  }
}

.icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 8px;
  background: var(--fts-bg-subtle);
  color: var(--fts-text-primary);
  cursor: pointer;

  &.is-loading .el-icon {
    animation: spin 0.8s linear infinite;
  }
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.arrival-list {
  padding: 12px;
  padding-bottom: calc(78px + max(max(env(safe-area-inset-bottom, 0px), var(--fts-safe-bottom, 0px)), var(--fts-safe-bottom, 0px)));
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* 历史收货：日期区间过滤 */
.history-panel {
  display: flex;
  flex-direction: column;
}

.history-filter {
  margin: 10px 12px 0;
  padding: 10px 12px;
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-radius: 12px;

  &__toggle {
    display: flex;
    align-items: center;
    gap: 6px;
    width: 100%;
    padding: 0;
    border: none;
    background: transparent;
    color: var(--fts-text-primary);
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
  }

  &__badge {
    min-width: 18px;
    height: 18px;
    padding: 0 5px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border-radius: 9px;
    background: var(--fts-primary);
    color: #fff;
    font-size: 11px;
    font-weight: 700;
  }

  &__arrow {
    margin-left: auto;
    color: var(--fts-text-secondary);
    transition: transform 0.2s ease;

    &.is-open {
      transform: rotate(180deg);
    }
  }

  &__body {
    margin-top: 10px;
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  &__row {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  &__label {
    flex-shrink: 0;
    width: 44px;
    color: var(--fts-text-secondary);
    font-size: 13px;
  }

  &__dates {
    flex: 1;
    min-width: 0;
    display: flex;
    align-items: center;
    gap: 6px;
  }

  &__sep {
    flex-shrink: 0;
    color: var(--fts-text-secondary);
    font-size: 13px;
  }

  &__actions {
    display: flex;
    justify-content: flex-end;
  }

  &__reset {
    padding: 6px 16px;
    border: 1px solid var(--fts-border-primary);
    border-radius: 8px;
    background: var(--fts-bg-page);
    color: var(--fts-text-secondary);
    font-size: 13px;
    cursor: pointer;
  }

  .native-date {
    flex: 1;
    min-width: 0;
    height: 34px;
    padding: 0 8px;
    border: 1px solid var(--fts-border-primary);
    border-radius: 8px;
    background: var(--fts-bg-page);
    color: var(--fts-text-primary);
    font-size: 14px;
  }

  .native-select {
    flex: 1;
    min-width: 0;
    height: 34px;
    padding: 0 8px;
    border: 1px solid var(--fts-border-primary);
    border-radius: 8px;
    background: var(--fts-bg-page);
    color: var(--fts-text-primary);
    font-size: 14px;
  }

  &__sep {
    flex-shrink: 0;
    color: var(--fts-text-secondary);
    font-size: 13px;
  }
}

/* 列表页签：待收货 / 历史 */
.list-tabs {
  position: sticky;
  top: calc(64px + max(env(safe-area-inset-top, 0px), var(--fts-header-top, var(--fts-safe-top, 0px))));
  z-index: 9;
  display: flex;
  gap: 8px;
  padding: 10px 14px;
  background: var(--fts-bg-page);
  border-bottom: 1px solid var(--fts-border-secondary);
}

.list-tab {
  flex-shrink: 0;
  padding: 7px 16px;
  border: 1px solid var(--fts-border-primary);
  border-radius: 999px;
  background: var(--fts-bg-card);
  color: var(--fts-text-secondary);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;

  &.is-active {
    border-color: var(--fts-primary);
    background: color-mix(in srgb, var(--fts-primary) 12%, var(--fts-bg-card));
    color: var(--fts-primary);
  }

  &__count {
    margin-left: 2px;
    font-size: 12px;
    opacity: 0.7;
  }
}

/* ============ 我的（清除缓存 / 退出登录） ============ */
.mine-view {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px;
  padding-bottom: calc(92px + max(max(env(safe-area-inset-bottom, 0px), var(--fts-safe-bottom, 0px)), var(--fts-safe-bottom, 0px)));

  &__account {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 16px 14px;
    background: linear-gradient(135deg, #1a5fb4, #4c8dff);
    border-radius: 14px;
    color: #fff;
  }
}

.mine-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  flex-shrink: 0;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.22);
  border: 1.5px solid rgba(255, 255, 255, 0.6);
  font-size: 22px;
  font-weight: 700;
}

.mine-user {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;

  &__name {
    font-size: 18px;
    font-weight: 700;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__role {
    font-size: 13px;
    opacity: 0.9;
  }
}

.mine-list {
  display: flex;
  flex-direction: column;
  background: var(--fts-bg-card);
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid var(--fts-border-secondary);
}

.mine-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;

  &[onclick],
  &[class~='is-clickable'] {
    cursor: pointer;
    transition: background 0.15s;
    &:active {
      background: var(--fts-bg-subtle);
    }
  }

  & + & {
    border-top: 1px solid var(--fts-border-secondary);
  }

  &__icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    flex-shrink: 0;
    border-radius: 10px;
    background: color-mix(in srgb, var(--fts-primary) 10%, var(--fts-bg-card));
    color: var(--fts-primary);
  }

  &__body {
    flex: 1;
    min-width: 0;
  }

  &__title {
    font-size: 15px;
    font-weight: 600;
    color: var(--fts-text-primary);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__desc {
    margin-top: 2px;
    font-size: 12px;
    color: var(--fts-text-secondary);
  }

  &__value {
    flex-shrink: 0;
    font-size: 13px;
    color: var(--fts-text-secondary);
  }

  &__action {
    flex-shrink: 0;
    padding: 6px 14px;
    border: none;
    border-radius: 8px;
    background: var(--fts-primary);
    color: #fff;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;

    &:disabled {
      opacity: 0.6;
    }
  }
}

/* ============ 底部 Tab 导航（Android Material 风格） ============ */
.tab-nav {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 20;
  display: flex;
  max-width: 560px;
  margin: 0 auto;
  background: var(--fts-bg-card);
  border-top: 1px solid var(--fts-border-secondary);
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.06);
  padding-bottom: max(max(env(safe-area-inset-bottom, 0px), var(--fts-safe-bottom, 0px)), var(--fts-safe-bottom, 0px));

  &__item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 3px;
    padding: 9px 0 8px;
    border: none;
    background: transparent;
    color: var(--fts-text-secondary);
    font-size: 12px;
    font-weight: 600;
    cursor: pointer;
    -webkit-tap-highlight-color: transparent;

    &.is-active {
      color: var(--fts-primary);
    }
  }

  &__icon {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 28px;
    height: 24px;
  }

  &__badge {
    position: absolute;
    top: -3px;
    right: -8px;
    min-width: 16px;
    height: 16px;
    padding: 0 4px;
    border-radius: 999px;
    background: var(--fts-danger, #f56c6c);
    color: #fff;
    font-size: 10px;
    line-height: 16px;
    text-align: center;
  }
}

.mine-logout {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 13px 16px;
  border: none;
  border-radius: 14px;
  background: var(--fts-bg-card);
  border: 1px solid color-mix(in srgb, #f56c6c 40%, var(--fts-border-primary));
  color: #f56c6c;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
}

.history-status {
  padding: 2px 10px;
  border-radius: 999px;
  background: var(--fts-success-bg);
  color: var(--fts-success);
  font-size: 12px;

  &.is-2 {
    background: var(--fts-error-bg);
    color: var(--fts-error);
  }
}

.load-more-btn {
  margin: 4px auto;
  padding: 10px 0;
  width: 100%;
  max-width: 320px;
  border: 1px solid var(--fts-border-primary);
  border-radius: 10px;
  background: var(--fts-bg-card);
  color: var(--fts-text-secondary);
  font-size: 14px;
  cursor: pointer;
}

/* 历史收货详情（移动端只读） */
.history-detail {
  position: fixed;
  inset: 0;
  z-index: 60;
  display: flex;
  flex-direction: column;
  background: var(--fts-bg-page);
  max-width: 560px;
  margin: 0 auto;

  &__bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: calc(12px + max(max(env(safe-area-inset-top, 0px), var(--fts-header-top, var(--fts-safe-top, 0px))), var(--fts-header-top, var(--fts-safe-top, 0px)))) 14px 12px;
    background: var(--fts-bg-card);
    border-bottom: 1px solid var(--fts-border-secondary);
    font-size: 15px;
    font-weight: 700;
    color: var(--fts-text-primary);
  }

  &__body {
    flex: 1;
    overflow-y: auto;
    padding: 14px;
    padding-bottom: calc(24px + max(max(env(safe-area-inset-bottom, 0px), var(--fts-safe-bottom, 0px)), var(--fts-safe-bottom, 0px)));
    display: flex;
    flex-direction: column;
    gap: 14px;
  }

  &__info {
    padding: 12px;
    border: 1px solid var(--fts-border-primary);
    border-radius: 12px;
    background: var(--fts-bg-card);
    font-size: 13px;
    color: var(--fts-text-secondary);
    line-height: 1.8;
  }

  .info-row {
    display: flex;
    align-items: baseline;
    gap: 10px;

    &__label {
      flex-shrink: 0;
      min-width: 58px;
      font-size: 12px;
      color: var(--fts-text-tertiary);
    }

    &__value {
      font-size: 14px;
      font-weight: 600;
      color: var(--fts-text-primary);
      word-break: break-all;

      &--supplier {
        font-weight: 700;
        color: #1a5fb4;
      }
    }

    & + .info-row {
      margin-top: 6px;
    }
  }

  .info-grid {
    display: flex;
    gap: 10px;
    margin-top: 12px;

    &__cell {
      flex: 1;
      padding: 10px 12px;
      border-radius: 10px;
      background: #f3f7fc;
      text-align: center;
    }

    &__num {
      font-size: 20px;
      font-weight: 800;
      line-height: 1.3;
      letter-spacing: 0.5px;
      font-variant-numeric: tabular-nums;

      &--qty {
        color: #1a5fb4;
      }

      &--amount {
        color: #c0392b;
      }
    }

    &__cap {
      margin-top: 2px;
      font-size: 11px;
      color: var(--fts-text-tertiary);
    }
  }

  &__items {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  &__sign-title {
    display: flex;
    align-items: center;
    gap: 8px;
    padding-top: 4px;
    font-size: 14px;
    font-weight: 700;
    color: var(--fts-text-primary);
  }

  &__sign-miss {
    font-size: 12px;
    font-weight: 400;
    color: #c0392b;
  }

  .re-sign-btn {
    margin-left: auto;
    border: none;
    border-radius: 8px;
    padding: 5px 12px;
    background: #1a5fb4;
    color: #fff;
    font-size: 12px;
    font-weight: 600;
    cursor: pointer;
  }

  &__hint {
    padding: 12px 0;
    font-size: 13px;
    color: var(--fts-text-tertiary);
  }

  &__sign {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px;
    border: 1px solid var(--fts-border-primary);
    border-radius: 10px;
    background: var(--fts-bg-card);
  }

  &__sign-img {
    width: 96px;
    height: 48px;
    object-fit: contain;
    border-radius: 6px;
    background: var(--fts-bg-secondary);
  }

  &__sign-meta {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  &__sign-name {
    font-size: 14px;
    font-weight: 600;
    color: var(--fts-text-primary);
  }

  &__sign-tag {
    margin-left: 6px;
    padding: 1px 8px;
    border-radius: 999px;
    background: var(--fts-success-bg);
    color: var(--fts-success);
    font-size: 12px;
  }

  &__sign-time {
    font-size: 12px;
    color: var(--fts-text-tertiary);
  }
}

.history-item {
  padding: 12px;
  border: 1px solid var(--fts-border-primary);
  border-radius: 10px;
  background: var(--fts-bg-card);

  &__name {
    font-size: 14px;
    font-weight: 700;
    color: var(--fts-text-primary);
  }

  &__meta {
    margin-top: 4px;
    font-size: 12px;
    color: var(--fts-text-secondary);
    line-height: 1.6;
  }

  &__evi {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-top: 8px;
  }

  &__img {
    width: 64px;
    height: 64px;
    object-fit: cover;
    border-radius: 8px;
    border: 1px solid var(--fts-border-primary);
  }
}

.arrival-card {
  padding: 14px;
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: 12px;
  cursor: pointer;

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;
  }

  &__code {
    font-size: 15px;
    font-weight: 700;
    color: var(--fts-text-primary);
  }

  &__status {
    padding: 2px 10px;
    border-radius: 999px;
    background: var(--fts-warning-bg);
    color: var(--fts-warning);
    font-size: 12px;
  }

  &__row {
    font-size: 13px;
    color: var(--fts-text-secondary);
    line-height: 1.7;
  }
}

.empty-tip {
  padding: 48px 0;
  text-align: center;
  color: var(--fts-text-tertiary);
  font-size: 14px;
}

/* ========== 进度 + 智能目录（sticky，参考 PDA 手持收货 V4 精修版） ========== */
.nav-sticky {
  position: sticky;
  top: calc(64px + max(env(safe-area-inset-top, 0px), var(--fts-header-top, var(--fts-safe-top, 0px)))); /* 吸附在 mobile-header 下方 */
  z-index: 15;
  background: var(--fts-bg-card);
  padding: 8px 12px 6px;
  border-bottom: 1px solid var(--fts-border-secondary);
  margin-top: 8px;
}

.progress-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;

  &__label {
    flex-shrink: 0;
    font-size: 12px;
    font-weight: 600;
    color: var(--fts-text-primary);
  }
}

.progress-track {
  flex: 1;
  height: 6px;
  background: var(--fts-bg-subtle);
  border-radius: 6px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: var(--fts-primary);
  border-radius: 6px;
  transition: width 0.3s ease;
}

.progress-text {
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 700;
  color: var(--fts-primary);
  font-variant-numeric: tabular-nums;
}

.chip-scroll {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
  padding-bottom: 6px;

  &::-webkit-scrollbar {
    display: none;
  }
}

.chip {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 3px 12px;
  border: 1px solid var(--fts-border-primary);
  border-radius: 20px;
  background: var(--fts-bg-card);
  font-size: 12px;
  color: var(--fts-text-secondary);
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.15s;

  &--active {
    border-color: var(--fts-primary);
    background: var(--fts-primary-bg);
    color: var(--fts-primary);
    font-weight: 600;
  }

  &--match {
    border-color: var(--fts-primary);
    background: #dbeafe;
    color: var(--fts-primary);
    box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.2);
  }

  &--pending .chip__dot {
    background: var(--fts-warning);
  }

  &--done .chip__dot {
    background: var(--fts-success);
  }

  &--reject .chip__dot {
    background: var(--fts-error);
  }

  &__dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    display: inline-block;
    background: var(--fts-text-tertiary);
  }

  &__name {
    max-width: 120px;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__tag {
    font-size: 10px;
    background: var(--fts-bg-subtle);
    padding: 0 6px;
    border-radius: 4px;

    &--match {
      background: var(--fts-primary);
      color: #fff;
    }
  }
}

/* ========== 卡片区：单卡聚焦 ========== */
.card-area {
  padding: 10px 12px;
}

.item-card {
  background: var(--fts-bg-card);
  border-radius: 14px;
  padding: 12px;
  border: 1px solid var(--fts-border-primary);
  scroll-margin-top: calc(128px + max(env(safe-area-inset-top, 0px), var(--fts-header-top, var(--fts-safe-top, 0px)))); /* sticky 头+目录导航高度 */

  &.is-done {
    border-color: rgba(22, 163, 74, 0.35);
  }

  &.is-reject {
    border-color: rgba(220, 38, 38, 0.35);
  }

  &.is-matched {
    border-color: var(--fts-primary);
    box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.15);
  }

  &.is-invalid {
    border-color: var(--fts-error);
    box-shadow: 0 0 0 2px rgba(220, 38, 38, 0.2);
    animation: invalid-flash 1.1s ease-in-out 2;
  }

  @keyframes invalid-flash {
    0%,
    100% {
      box-shadow: 0 0 0 2px rgba(220, 38, 38, 0.2);
    }
    50% {
      box-shadow: 0 0 0 6px rgba(220, 38, 38, 0.3);
    }
  }

  .card-top {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 10px;

    &__name {
      font-size: 16px;
      font-weight: 800;
      color: var(--fts-text-primary);
      line-height: 1.3;
    }

    &__spec {
      margin-top: 2px;
      font-size: 12px;
      color: var(--fts-text-secondary);
    }

    &__order {
      flex-shrink: 0;
      text-align: right;
      background: var(--fts-bg-subtle);
      padding: 3px 10px;
      border-radius: 8px;

      &-lbl {
        display: inline-block;
        padding: 1px 7px;
        border-radius: 5px;
        background: var(--fts-warning-bg);
        color: var(--fts-warning);
        font-size: 12px;
        font-weight: 800;
        line-height: 1.5;
      }

      &-num {
        font-size: 16px;
        font-weight: 700;
        color: var(--fts-primary);
        font-variant-numeric: tabular-nums;
      }

      &-unit {
        margin-left: 2px;
        font-size: 12px;
        color: var(--fts-text-secondary);
      }
    }
  }
}

.match-btn {
  margin: -4px 0 10px;
  padding: 6px 14px;
  border: 1px solid var(--fts-primary);
  border-radius: 8px;
  background: var(--fts-primary-bg);
  color: var(--fts-primary);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

/* 实收行：标签 + 输入框同一行 */
.qty-row {
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--fts-bg-subtle);
  padding: 4px 10px;
  border-radius: 10px;
  margin-bottom: 10px;

  > label {
    flex-shrink: 0;
    font-weight: 600;
    font-size: 14px;
    color: var(--fts-text-primary);
  }

  &__unit {
    flex-shrink: 0;
    font-size: 13px;
    color: var(--fts-text-secondary);
    padding-left: 4px;
  }

  .reject-all-btn {
    flex-shrink: 0;
    border: none;
    border-radius: 8px;
    padding: 5px 10px;
    background: var(--fts-error-bg, #fde8e8);
    color: var(--fts-error, #c0392b);
    font-size: 12px;
    font-weight: 600;
    cursor: pointer;
  }

  :deep(.qty-input) {
    flex: 1;
    min-width: 0;

    .el-input__wrapper,
    .el-input-number__wrap,
    .el-input-number .el-input__wrapper {
      box-shadow: 0 0 0 1px var(--fts-border-primary) inset;
      background: var(--fts-bg-card);
      border-radius: 8px;
      padding: 0 8px;
      height: 34px;
      min-height: 34px;
      transition: box-shadow 0.15s;

      /* 聚焦态：主色描边，明确可输入 */
      &.is-focus {
        box-shadow: 0 0 0 2px var(--fts-primary) inset;
      }
    }

    .el-input__inner {
      font-size: 20px;
      font-weight: 700;
      text-align: right;
      color: var(--fts-text-primary);
    }
  }
}

/* 接收 / 拒收（✓/✗） */
.accept-group {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
}

.accept-btn {
  flex: 1;
  height: 44px;
  border-radius: 10px;
  border: 2px solid var(--fts-border-primary);
  background: var(--fts-bg-card);
  font-weight: 700;
  font-size: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  cursor: pointer;
  transition: all 0.15s;
  color: var(--fts-text-secondary);

  &__icon {
    font-size: 16px;
    line-height: 1;
  }

  &--receive.is-active {
    border-color: var(--fts-success);
    background: var(--fts-success);
    color: #fff;
  }

  &--reject.is-active {
    border-color: var(--fts-error);
    background: var(--fts-error);
    color: #fff;
  }
}

/* 详情区：批次 / 质检 / 拒收 */
.detail-section {
  background: var(--fts-bg-subtle);
  border-radius: 10px;
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-row {
  display: flex;
  align-items: center;
  gap: 8px;

  > label:not(.evidence-btn) {
    flex-shrink: 0;
    width: 62px;
    font-size: 12px;
    color: var(--fts-text-secondary);
  }

  .native-date,
  .flex-input,
  .batch-row {
    flex: 1;
    min-width: 0;
  }
}

.qc-group {
  display: flex;
  gap: 6px;
  flex: 1;
  flex-wrap: wrap;

  .qc-btn {
    flex: 1;
    height: 30px;
    min-width: 50px;
    border: 1px solid var(--fts-border-primary);
    border-radius: 6px;
    background: var(--fts-bg-card);
    font-size: 12px;
    cursor: pointer;

    &.is-active {
      border-color: var(--fts-primary);
      background: var(--fts-primary-bg);
      color: var(--fts-primary);
      font-weight: 700;
    }
  }
}

.item-card__fields {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.field-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.field-label {
  flex-shrink: 0;
  width: 70px;
  font-size: 15px;
  font-weight: 500;
  color: var(--fts-text-primary);
}

.field-note {
  margin: -6px 0 10px 80px;
  font-size: 12px;
  color: var(--fts-text-tertiary);
  line-height: 1.5;
}

.field-error {
  :deep(.el-input__wrapper) {
    box-shadow: 0 0 0 1px var(--fts-error) inset;
  }
}

.req-mark {
  margin-left: 2px;
  font-size: 14px;
  font-weight: 700;
  color: var(--fts-error);
  line-height: 1;
}

.evidence-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 2px;
}

.evidence-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border: 1px dashed var(--fts-primary);
  border-radius: 8px;
  background: var(--fts-primary-bg);
  color: var(--fts-primary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;

  &--done {
    border-style: solid;
    background: var(--fts-success-bg);
    border-color: var(--fts-success);
    color: var(--fts-success);
  }

  &--large {
    padding: 12px 24px;
    font-size: 16px;
  }

  &.is-loading {
    opacity: 0.6;
  }
}

.evidence-thumbs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.evidence-thumb {
  position: relative;
  width: 56px;
  height: 56px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid var(--fts-border-primary);

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  &__badge {
    position: absolute;
    left: 0;
    bottom: 0;
    padding: 1px 5px;
    font-size: 10px;
    background: rgba(0, 0, 0, 0.55);
    color: #fff;
    border-radius: 0 6px 0 0;

    &.is-pending {
      background: var(--fts-warning);
      color: #fff;
    }
  }

  &__remove {
    position: absolute;
    right: 0;
    top: 0;
    width: 18px;
    height: 18px;
    border: none;
    background: rgba(0, 0, 0, 0.55);
    color: #fff;
    font-size: 13px;
    line-height: 18px;
    border-radius: 0 0 0 6px;
    cursor: pointer;
  }
}

.manual-match {
  position: fixed;
  inset: 0;
  z-index: 40;
  display: flex;
  flex-direction: column;
  background: var(--fts-bg-page);

  &__bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 14px 16px;
    background: var(--fts-bg-card);
    border-bottom: 1px solid var(--fts-border-primary);
    font-size: 17px;
    font-weight: 700;
    color: var(--fts-text-primary);
  }

  &__body {
    flex: 1;
    overflow-y: auto;
    padding: 16px;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  &__target {
    font-size: 15px;
    font-weight: 600;
    color: var(--fts-text-primary);
  }

  &__barcode {
    display: block;
    margin-top: 4px;
    font-size: 13px;
    font-weight: 400;
    color: var(--fts-text-secondary);
    word-break: break-all;
  }

  &__warn {
    padding: 10px 12px;
    border-radius: 8px;
    background: var(--fts-warning-bg);
    color: var(--fts-warning);
    font-size: 13px;
    line-height: 1.6;
  }

  &__photo {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  &__hint {
    font-size: 12px;
    color: var(--fts-text-tertiary);
  }

  &__photo-done {
    display: flex;
    align-items: flex-start;
    gap: 12px;

    img {
      width: 96px;
      height: 96px;
      border-radius: 8px;
      object-fit: cover;
      border: 1px solid var(--fts-border-primary);
    }
  }

  &__meta {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 4px;
    font-size: 12px;
    color: var(--fts-text-secondary);
    word-break: break-all;

    &-miss {
      color: var(--fts-error);
    }
  }

  &__retake {
    padding: 6px 12px;
    border: 1px solid var(--fts-border-primary);
    border-radius: 8px;
    background: var(--fts-bg-card);
    color: var(--fts-text-secondary);
    font-size: 13px;
    cursor: pointer;
  }

  &__actions {
    display: flex;
    gap: 12px;
    padding: 12px 16px calc(12px + max(max(env(safe-area-inset-bottom, 0px), var(--fts-safe-bottom, 0px)), var(--fts-safe-bottom, 0px)));
    background: var(--fts-bg-card);
    border-top: 1px solid var(--fts-border-primary);

    .crop-btn {
      flex: 1;
    }
  }
}

.qty-input {
  flex: 1;
  min-width: 0;

  :deep(.el-input__inner) {
    text-align: right;
    font-size: 18px;
    font-weight: 700;
    height: 20px;
  }

  :deep(.el-input__wrapper) {
    height: 32px;
    min-height: 32px;
    padding: 0 8px;
  }
}

.flex-input {
  flex: 1;
}

.native-date {
  flex: 1;
  height: 32px;
  padding: 0 12px;
  border: 1px solid var(--fts-border-primary);
  border-radius: 6px;
  background: var(--fts-bg-card);
  color: var(--fts-text-primary);
  font-size: 14px;
  -webkit-appearance: none;
  appearance: none;
}

.partial-hint {
  padding: 6px 10px;
  border-radius: 8px;
  background: var(--fts-warning-bg);
  color: var(--fts-warning);
  font-size: 12px;
}

.submit-panel {
  margin: 12px;
  padding: 14px;
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 700;
  color: var(--fts-text-primary);

  &::before {
    content: '';
    width: 4px;
    height: 16px;
    border-radius: 2px;
    background: var(--fts-primary);
    flex-shrink: 0;
  }

  &:not(:first-child) {
    margin-top: 18px;
    padding-top: 14px;
    border-top: 1px solid var(--fts-border-secondary);
  }
}

.barcode-match {
  padding: 8px 12px;
  border-radius: 8px;
  background: var(--fts-warning-bg);
  color: var(--fts-warning);
  font-size: 13px;
  line-height: 1.5;
  margin-bottom: 10px;

  &--ok {
    background: var(--fts-success-bg);
    color: var(--fts-success);
  }
}

.item-card__section {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px;
  margin-top: 12px;
  border: 1px solid var(--fts-border-primary);
  border-radius: 10px;
  background: var(--fts-bg-subtle);

  &-title {
    font-size: 13px;
    font-weight: 600;
    color: var(--fts-text-primary);
  }
}

.qc-btns--card {
  flex: 1;
  display: flex;
  gap: 8px;
}

.quick-apply-btn {
  width: 100%;
  height: 42px;
  margin-top: 4px;
  border: 1px solid var(--fts-primary);
  border-radius: 10px;
  background: var(--fts-primary-bg);
  color: var(--fts-primary);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.qc-btns {
  flex: 1;
  display: flex;
  gap: 8px;
}

.qc-btn {
  flex: 1;
  height: 40px;
  border: 1px solid var(--fts-border-primary);
  border-radius: 8px;
  background: var(--fts-bg-card);
  color: var(--fts-text-secondary);
  font-size: 13px;
  cursor: pointer;

  &.is-active {
    border-color: var(--fts-primary);
    background: var(--fts-primary-bg);
    color: var(--fts-primary);
    font-weight: 600;
  }
}

.summary-cards {
  display: flex;
  gap: 10px;
}

.summary-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 12px 8px;
  border-radius: 10px;
  background: var(--fts-bg-subtle);

  &__label {
    font-size: 12px;
    color: var(--fts-text-secondary);
  }

  &__value {
    font-size: 20px;
    font-weight: 700;
    color: var(--fts-text-primary);
    font-variant-numeric: tabular-nums;
  }

  &--ok {
    background: var(--fts-success-bg);

    .summary-card__value {
      color: var(--fts-success);
    }
  }

  &--reject {
    background: var(--fts-error-bg);

    .summary-card__value {
      color: var(--fts-error);
    }
  }

  &--partial {
    background: var(--fts-warning-bg);

    .summary-card__value {
      color: var(--fts-warning);
    }
  }

  &--empty {
    opacity: 0.6;
  }
}

/* 底部吸附栏的确认按钮（覆盖旧全宽面板样式，手机端紧凑） */
.bottom-bar .submit-btn {
  width: auto;
  margin: 0;
  height: 38px;
  padding: 0 18px;
  border-radius: 10px;
  font-size: 15px;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
  flex-shrink: 0;
}

.submit-btn {
  width: 66.66%;
  margin: 4px auto 0;
  height: 50px;
  border: none;
  border-radius: 10px;
  background: var(--fts-primary);
  color: #fff;
  font-size: 17px;
  font-weight: 700;
  cursor: pointer;

  &:disabled {
    opacity: 0.6;
  }
}

.batch-row {
  flex: 1;
  display: flex;
  gap: 8px;
}

.scan-btn {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 8px;
  background: var(--fts-primary);
  color: #fff;
  cursor: pointer;

  &--ghost {
    background: var(--fts-bg-subtle);
    color: var(--fts-text-primary);
    border: 1px solid var(--fts-border-primary);
  }
}

/* iOS Safari：file input 不能用 display:none（click() 会被忽略），
   用视觉隐藏但保持可交互的方式 */
.hidden-file-input {
  position: fixed;
  left: -9999px;
  top: 0;
  width: 1px;
  height: 1px;
  opacity: 0;
  overflow: hidden;
}

.scan-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  flex-direction: column;
  background: #000;

  &__bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: calc(14px + max(max(env(safe-area-inset-top, 0px), var(--fts-header-top, var(--fts-safe-top, 0px))), var(--fts-header-top, var(--fts-safe-top, 0px)))) 16px 14px;
    color: #fff;
    font-size: 16px;
    font-weight: 600;
  }

  &__video {
    flex: 1;
    width: 100%;

    :deep(video) {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  &__tip {
    padding: 16px;
    text-align: center;
    color: #bbb;
    font-size: 14px;
    padding-bottom: calc(16px + max(max(env(safe-area-inset-bottom, 0px), var(--fts-safe-bottom, 0px)), var(--fts-safe-bottom, 0px)));
  }
}

/* 裁剪重试层 */
.crop-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  flex-direction: column;
  background: #000;

  &__bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: calc(14px + max(max(env(safe-area-inset-top, 0px), var(--fts-header-top, var(--fts-safe-top, 0px))), var(--fts-header-top, var(--fts-safe-top, 0px)))) 16px 14px;
    color: #fff;
    font-size: 16px;
    font-weight: 600;
  }

  &__tip {
    padding: 12px 16px;
    text-align: center;
    color: #bbb;
    font-size: 13px;
  }

  &__diag {
    padding: 0 16px 8px;
    text-align: center;
    color: #888;
    font-size: 11px;
    word-break: break-all;
  }

  &__actions {
    display: flex;
    gap: 12px;
    padding: 12px 16px calc(16px + max(max(env(safe-area-inset-bottom, 0px), var(--fts-safe-bottom, 0px)), var(--fts-safe-bottom, 0px)));
  }
}

.crop-area {
  position: relative;
  flex: 1;
  min-height: 0;
  overflow: hidden;
  touch-action: none;

  &__img {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
    object-fit: contain;
  }
}

.crop-rect {
  position: absolute;
  border: 2px solid var(--fts-primary);
  background: rgba(64, 158, 255, 0.12);
  box-shadow: 0 0 0 9999px rgba(0, 0, 0, 0.45);
  touch-action: none;

  &__handle {
    position: absolute;
    right: -6px;
    bottom: -6px;
    width: 20px;
    height: 20px;
    border-radius: 50%;
    background: var(--fts-primary);
    border: 2px solid #fff;
    touch-action: none;
  }
}

.crop-btn {
  flex: 1;
  height: 46px;
  border: none;
  border-radius: 10px;
  background: var(--fts-primary);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;

  &--ghost {
    background: transparent;
    border: 1px solid rgba(255, 255, 255, 0.4);
    color: #fff;
  }
}

/* 摄像头实时扫码遮罩 */
.live-scan {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  flex-direction: column;
  background: #000;

  &__bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: calc(14px + max(max(env(safe-area-inset-top, 0px), var(--fts-header-top, var(--fts-safe-top, 0px))), var(--fts-header-top, var(--fts-safe-top, 0px)))) 16px 14px;
    color: #fff;
    font-size: 16px;
    font-weight: 600;
  }

  &__video {
    flex: 1;
    width: 100%;
    min-height: 0;
    overflow: hidden;

    :deep(video) {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  &__error {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 24px;
    color: #ffb3b3;
    font-size: 14px;
    line-height: 1.8;
    text-align: center;
    white-space: pre-line;
  }

  &__tip {
    padding: 16px;
    text-align: center;
    color: #bbb;
    font-size: 14px;
    padding-bottom: calc(16px + max(max(env(safe-area-inset-bottom, 0px), var(--fts-safe-bottom, 0px)), var(--fts-safe-bottom, 0px)));
  }
}

/* OCR 识别结果确认弹层 */
.ocr-confirm {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  flex-direction: column;
  background: var(--fts-bg-card);

  &__bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: calc(14px + max(max(env(safe-area-inset-top, 0px), var(--fts-header-top, var(--fts-safe-top, 0px))), var(--fts-header-top, var(--fts-safe-top, 0px)))) 16px 14px;
    font-size: 16px;
    font-weight: 700;
    color: var(--fts-text-primary);
    border-bottom: 1px solid var(--fts-border-secondary);
  }

  &__body {
    flex: 1;
    overflow-y: auto;
    padding: 16px;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  &__fields-tip {
    font-size: 12px;
    color: var(--fts-text-tertiary);
  }

  &__text-label {
    margin-top: 8px;
    font-size: 13px;
    color: var(--fts-text-secondary);
  }

  &__text {
    width: 100%;
    padding: 10px 12px;
    border: 1px solid var(--fts-border-primary);
    border-radius: 8px;
    background: var(--fts-bg-card);
    font-size: 14px;
    line-height: 1.8;
    color: var(--fts-text-primary);
    word-break: break-all;
    resize: vertical;
    min-height: 120px;
    font-family: inherit;
  }

  &__copy {
    align-self: flex-start;
    padding: 8px 16px;
    border: 1px solid var(--fts-primary);
    border-radius: 8px;
    background: var(--fts-primary-bg);
    color: var(--fts-primary);
    font-size: 14px;
    cursor: pointer;
  }

  &__note {
    padding: 8px 12px;
    border-radius: 8px;
    background: var(--fts-warning-bg);
    color: var(--fts-warning);
    font-size: 13px;
    line-height: 1.6;
  }

  &__actions {
    display: flex;
    gap: 12px;
    padding: 12px 16px calc(16px + max(max(env(safe-area-inset-bottom, 0px), var(--fts-safe-bottom, 0px)), var(--fts-safe-bottom, 0px)));
  }
}

/* ===== 全屏图片查看器（触摸缩放/双击/关闭） ===== */
.photo-viewer {
  position: fixed;
  inset: 0;
  z-index: 99999;
  background: rgba(0, 0, 0, 0.94);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;

  &__img {
    max-width: 96vw;
    max-height: 92vh;
    object-fit: contain;
    touch-action: none;
    user-select: none;
  }

  &__close {
    position: absolute;
    bottom: calc(24px + env(safe-area-inset-bottom, 0px));
    left: 50%;
    transform: translateX(-50%);
    padding: 8px 18px;
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.16);
    color: #fff;
    font-size: 13px;
  }
}

/* ===== 横屏书写覆盖层（全屏横向签名区，不依赖系统旋转） ===== */
.landscape-pad {
  position: fixed;
  inset: 0;
  z-index: 99998;
  background: #000;
  overflow: hidden;

  /* 旋转 90° 横屏签名区：视觉宽=屏高、高=屏宽（扣底部按钮区），居中显示 */
  &__rotate {
    position: absolute;
    left: 50%;
    top: 50%;
    width: calc(100vh - 70px);
    height: calc(100vw - 70px);
    background: #fff;
    display: flex;
    flex-direction: column;
    transform: translate(-50%, -50%) rotate(90deg);
  }

  &__bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 10px 16px;
    border-bottom: 1px solid var(--fts-border-secondary);
    background: var(--fts-bg-card);
  }

  &__title {
    font-size: 15px;
    font-weight: 700;
    color: var(--fts-text-primary);
  }

  &__close {
    border: none;
    background: none;
    color: var(--fts-text-secondary);
    font-size: 18px;
    padding: 2px 6px;
  }

  &__area {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
    padding: 10px 16px;
  }

  /* 签名区水印（不占标题面积，作为背景提示） */
  &__watermark {
    position: absolute;
    left: 50%;
    top: 50%;
    transform: translate(-50%, -50%);
    font-size: 30px;
    font-weight: 800;
    letter-spacing: 6px;
    color: rgba(120, 140, 160, 0.12);
    pointer-events: none;
    white-space: nowrap;
  }

  /* 操作按钮：底部固定（竖屏坐标，旋转不挤压） */
  &__actions {
    position: absolute;
    left: 0;
    right: 0;
    bottom: 0;
    display: flex;
    justify-content: flex-end;
    gap: 10px;
    padding: 10px 16px calc(12px + max(max(env(safe-area-inset-bottom, 0px), var(--fts-safe-bottom, 0px)), var(--fts-safe-bottom, 0px)));
    background: rgba(0, 0, 0, 0.85);
  }
}

.landscape-btn {
  flex-shrink: 0;
  border: 1px solid #1a5fb4;
  border-radius: 8px;
  padding: 8px 14px;
  background: #fff;
  color: #1a5fb4;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

/* ===== tab 切换左右翻页动画 ===== */
.tab-slide-left-enter-active,
.tab-slide-left-leave-active,
.tab-slide-right-enter-active,
.tab-slide-right-leave-active {
  transition: transform 0.25s ease, opacity 0.25s ease;
}
.tab-slide-left-enter-from {
  transform: translateX(100%);
  opacity: 0;
}
.tab-slide-left-leave-to {
  transform: translateX(-30%);
  opacity: 0;
}
.tab-slide-right-enter-from {
  transform: translateX(-100%);
  opacity: 0;
}
.tab-slide-right-leave-to {
  transform: translateX(30%);
  opacity: 0;
}
</style>
